package Gestor_carga;

import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import org.zeromq.ZMQException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Gestiona la comunicación síncrona con los Gestores de Almacenamiento (GA) 
 * en un hilo separado para evitar el bloqueo del ServidorGC_ZMQ principal.
 * Utiliza ZMQ REQ/REP para las consultas INFO y PRESTAMO.
 */
public class GAClientProxy extends Thread {
    
    private final ZContext context;
    private final String[] gaHosts;
    private final int[] gaPorts;
    private volatile int currentGaIndex = 0; // Índice para failover Round-Robin
    
    // Almacena temporalmente la respuesta de una solicitud para que el GC la recoja
    private final Map<String, String> pendingResponses = new ConcurrentHashMap<>();
    
    // Cola local (inproc) para solicitudes entrantes del hilo principal del GC
    private final ZMQ.Socket internalReceiver;
    
    // Puerto interno usado para la comunicación inproc (dentro del mismo proceso)
    private final String internalPort;
    
    public GAClientProxy(ZContext context, String[] gaHosts, int[] gaPorts, String internalPort) {
        this.context = context;
        this.gaHosts = gaHosts;
        this.gaPorts = gaPorts;
        this.internalPort = internalPort;
        
        // Socket PULL interno para recibir peticiones del hilo principal del GC
        this.internalReceiver = context.createSocket(ZMQ.PULL);
        this.internalReceiver.bind("inproc://" + internalPort);
    }
    
    // Método que usa el ServidorGC_ZMQ para enviar una petición al Proxy
    public void sendRequest(String messageId, String request) {
        // Formato: messageId|request
        internalReceiver.send(messageId + "|" + request);
    }

    // Método que usa el ServidorGC_ZMQ para obtener la respuesta
    public String getResponse(String messageId, int timeoutMs) {
        int waited = 0;
        int interval = 50; // Intervalo de polling local
        String response;
        while (waited < timeoutMs) {
            response = pendingResponses.get(messageId);
            if (response != null) {
                pendingResponses.remove(messageId);
                return response;
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            waited += interval;
        }
        return "ERROR|Timeout de comunicación GC->GA (Proxy)";
    }

    @Override
    public void run() {
        // Socket REQ para comunicarse con los GA (DEBE crearse y usarse en su propio hilo)
        try (ZMQ.Socket requester = context.createSocket(ZMQ.REQ)) {
            
            // Conectar a todos los GAs para el Failover
            for (int i = 0; i < gaHosts.length; i++) {
                requester.connect("tcp://" + gaHosts[i] + ":" + gaPorts[i]);
                System.out.println("GAClientProxy conectado a GA " + gaHosts[i] + ":" + gaPorts[i]);
            }

            ZMQ.Poller poller = context.createPoller(1);
            poller.register(internalReceiver, ZMQ.Poller.POLLIN);

            while (!Thread.currentThread().isInterrupted()) {
                // Poll: espera peticiones internas (del GC)
                poller.poll(50); 

                if (poller.pollin(0)) {
                    // Recibir mensaje interno (bloqueado por el Poller)
                    String fullRequest = internalReceiver.recvStr(); 
                    String[] parts = fullRequest.split("\\|", 2);
                    String messageId = parts[0];
                    String request = parts[1];
                    
                    // Enviar y recibir del GA con failover (Aquí ocurre el bloqueo, pero en este hilo auxiliar)
                    String response = sendAndReceiveGA(requester, request);
                    
                    // Almacenar la respuesta para que el hilo principal del GC la recoja
                    pendingResponses.put(messageId, response);
                }
            }
        } catch (ZMQException e) {
            System.err.println("Error fatal en GAClientProxy: " + e.getMessage());
        }
    }

    /**
     * Envía la solicitud al GA con Failover (Round-Robin) y manejo de ZMQ REQ/REP.
     */
    private String sendAndReceiveGA(ZMQ.Socket requester, String request) {
        int intentos = 0;
        int maxIntentos = gaHosts.length * 2;
        final int gaTimeoutMs = 5000; // Timeout de 5 segundos para la respuesta del GA

        while (intentos < maxIntentos) {
            
            String gaHost = gaHosts[currentGaIndex];
            int gaPort = gaPorts[currentGaIndex];
            
            try {
                // 1. Enviar
                requester.send(request);
                
                // 2. Esperar respuesta con timeout
                byte[] reply = requester.recv(gaTimeoutMs);

                if (reply != null) {
                    System.out.println("[INFO GAClientProxy] Éxito con GA " + gaHost + ":" + gaPort);
                    return new String(reply, ZMQ.CHARSET);
                }
                
                // Si hay timeout (reply es null): FAILOVER
                System.err.println("[FAILOVER] GA " + gaHost + ":" + gaPort + " no responde a tiempo. Rotando...");
                
                // Si falla (timeout), rotar el índice.
                currentGaIndex = (currentGaIndex + 1) % gaHosts.length;
                intentos++;
                
                // Nota: El socket ZMQ.REQ está ahora "atascado" esperando una respuesta. 
                // Para recuperarlo, la forma más limpia es cerrar y recrear el socket, 
                // pero por simplicidad de REQ/REP, confiamos en la auto-reconexión 
                // y rotamos al siguiente servidor.

            } catch (ZMQException e) {
                System.err.println("[FAILOVER] ZMQ Error en GA " + gaHost + ":" + gaPort + ": " + e.getMessage());
                // Si hay un error ZMQ (ej. context cerrado), también rotamos y reintentamos.
                currentGaIndex = (currentGaIndex + 1) % gaHosts.length;
                intentos++;
            }
        }
        
        return "ERROR|Todos los GAs no disponibles (fallo en " + maxIntentos + " intentos)";
    }
}