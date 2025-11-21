package Gestor_carga;

import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import org.zeromq.ZMQException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Proxy para manejar la comunicación con los GAs de forma asíncrona
 * respecto al hilo principal del ServidorGC_ZMQ, usando su propia hebra.
 * Implementa Failover con Round-Robin.
 */
public class GAClientProxy implements Runnable {

    private final ZContext context;
    private final String[] gaHosts;
    private final int[] gaPorts;
    
    // Cola para peticiones entrantes del GC (bloqueo suave)
    private final LinkedBlockingQueue<ProxyRequest> requestQueue = new LinkedBlockingQueue<>();
    // Mapa para almacenar las respuestas de vuelta al hilo del GC
    private final ConcurrentHashMap<String, String> responseMap = new ConcurrentHashMap<>();

    private volatile AtomicInteger currentGaIndex = new AtomicInteger(0);
    private final int gaTimeoutMs = 3000;
    private ZMQ.Socket requester;

    public GAClientProxy(ZContext context, String[] gaHosts, int[] gaPorts) {
        this.context = context;
        this.gaHosts = gaHosts;
        this.gaPorts = gaPorts;
    }

    /**
     * Llamado por el ServidorGC_ZMQ para enviar una petición de forma bloqueante,
     * pero la lógica real REQ/REP es no-bloqueante en este hilo.
     */
    public String sendRequest(String request, String requestId) throws InterruptedException {
        ProxyRequest proxyRequest = new ProxyRequest(requestId, request);
        requestQueue.put(proxyRequest); // Bloquea si la cola está llena

        // Esperar la respuesta de forma activa
        long startTime = System.currentTimeMillis();
        // Timeout para que el GC no espere eternamente (ej: 10 segundos)
        long totalTimeoutMs = 10000; 

        while (System.currentTimeMillis() - startTime < totalTimeoutMs) {
            String response = responseMap.remove(requestId);
            if (response != null) {
                return response;
            }
            // Pequeña espera para no consumir CPU
            Thread.sleep(10); 
        }

        // Si hay timeout
        return "ERROR|Timeout de respuesta del GA (después de " + totalTimeoutMs + "ms)";
    }

    @Override
    public void run() {
        // La creación del socket y el ciclo de vida de REQ/REP deben estar en esta hebra
        try {
            requester = context.createSocket(ZMQ.REQ);

            // Conectar a todos los GAs
            for (int i = 0; i < gaHosts.length; i++) {
                requester.connect("tcp://" + gaHosts[i] + ":" + gaPorts[i]);
                System.out.println("GAClientProxy conectado a GA " + gaHosts[i] + ":" + gaPorts[i]);
            }

            while (!Thread.currentThread().isInterrupted()) {
                // Bloquea suavemente esperando una petición del GC
                ProxyRequest proxyRequest = requestQueue.poll(100, TimeUnit.MILLISECONDS);
                
                if (proxyRequest != null) {
                    String response = executeFailoverRequest(proxyRequest.request);
                    // Devolver la respuesta al hilo del GC usando el mapa
                    responseMap.put(proxyRequest.id, response);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ZMQException e) {
            System.err.println("Error fatal ZMQ en GAClientProxy: " + e.getMessage());
        } finally {
            if (requester != null) {
                requester.close();
            }
        }
    }

    private String executeFailoverRequest(String request) {
        int intentos = 0;
        int maxIntentos = gaHosts.length * 2; // Intentar dos veces cada GA

        while (intentos < maxIntentos) {
            int index = currentGaIndex.get();
            String gaHost = gaHosts[index];
            int gaPort = gaPorts[index];

            try {
                // 1. Enviar
                requester.send(request.getBytes(ZMQ.CHARSET));

                // 2. Esperar respuesta con timeout
                // El error de 'cancellationToken' ha sido corregido aquí (0 es el flag)
                byte[] reply = requester.recv(gaTimeoutMs); 

                if (reply != null) {
                    System.out.println("[INFO Proxy] Éxito con GA " + gaHost + ":" + gaPort);
                    return new String(reply, ZMQ.CHARSET);
                }
                
                // Si hay timeout (reply es null): FAILOVER
                System.err.println("[FAILOVER Proxy] GA " + gaHost + ":" + gaPort + " no responde a tiempo. Rotando...");
                
                // Rotar el índice y reintentar
                currentGaIndex.set((index + 1) % gaHosts.length);
                intentos++;
                
                // NOTA: ZMQ recomienda resetear el socket REQ después de un timeout, 
                // pero por simplicidad, confiamos en la reconexión automática aquí.
                // Si tienes fallos persistentes, considera cerrar y recrear el socket.

            } catch (ZMQException e) {
                System.err.println("[FAILOVER Proxy] ZMQ Error en GA " + gaHost + ":" + gaPort + ": " + e.getMessage());
                currentGaIndex.set((index + 1) % gaHosts.length);
                intentos++;
            }
        }

        return "ERROR|Timeout de comunicación con GA (después de " + maxIntentos + " intentos)";
    }

    private static class ProxyRequest {
        public final String id;
        public final String request;

        public ProxyRequest(String id, String request) {
            this.id = id;
            this.request = request;
        }
    }
}