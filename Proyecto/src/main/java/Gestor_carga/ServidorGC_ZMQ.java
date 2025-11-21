package Gestor_carga;

import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import org.zeromq.ZMQException;
import java.util.Arrays;

public class ServidorGC_ZMQ {

    private final int sede;
    private final String pubPort;
    private final String repPort;
    private final String[] gaHosts;
    private final int[] gaPorts;
    private final String pullPort = "5557"; // Puerto fijo para recibir PUSH de Actores
    
    private ZContext context;
    private ZMQ.Socket publisher;
    private ZMQ.Socket replier;
    private ZMQ.Socket puller;
    private GAClientProxy gaClientProxy;

    public ServidorGC_ZMQ(int sede, String pubPort, String repPort, String gaList) {
        this.sede = sede;
        this.pubPort = pubPort;
        this.repPort = repPort;

        // Parsear la lista de GAs para el Proxy
        String[] gaServers = gaList.split(",");
        this.gaHosts = new String[gaServers.length];
        this.gaPorts = new int[gaServers.length];
        for (int i = 0; i < gaServers.length; i++) {
            String[] parts = gaServers[i].trim().split(":");
            this.gaHosts[i] = parts[0];
            this.gaPorts[i] = Integer.parseInt(parts[1]);
        }
        
        System.out.println("GC Sede " + sede + " | GA configurados: " + Arrays.toString(gaServers));
    }

    public void iniciar() throws InterruptedException {
        context = new ZContext();
        
        // 1. Socket PUB (para Actores)
        publisher = context.createSocket(ZMQ.PUB);
        publisher.bind("tcp://*:" + pubPort);
        System.out.println("GC Sede " + sede + " | PUB escuchando en puerto " + pubPort);

        // 2. Socket PULL (para Actores - recibir resultados)
        puller = context.createSocket(ZMQ.PULL);
        puller.bind("tcp://*:" + pullPort);
        System.out.println("GC Sede " + sede + " | PULL escuchando en puerto " + pullPort);
        
        // 3. Inicializar y arrancar el Proxy de GA (hilo separado)
        gaClientProxy = new GAClientProxy(context, gaHosts, gaPorts);
        new Thread(gaClientProxy).start();
        
        // 4. Socket REP (para Clientes/PS)
        replier = context.createSocket(ZMQ.REP);
        replier.bind("tcp://*:" + repPort);
        System.out.println("GC Sede " + sede + " | REP escuchando en puerto " + repPort);

        // Configuración de Poller para manejar REP y PULL de forma asíncrona
        ZMQ.Poller items = context.createPoller(2);
        items.register(replier, ZMQ.Poller.POLLIN); // Peticiones del Cliente (REP)
        items.register(puller, ZMQ.Poller.POLLIN);  // Resultados de los Actores (PULL)

        try {
            while (!Thread.currentThread().isInterrupted()) {
                items.poll(); // Bloquea hasta que haya actividad en un socket

                // Manejo de peticiones del Cliente (REP)
                if (items.pollin(0)) {
                    String request = replier.recvStr();
                    if (request == null) continue;

                    String[] parts = request.split("\\|", 3);
                    String operation = parts[0];
                    String messageId = parts.length > 2 ? parts[2] : "N/A"; // Obtener MessageID si existe

                    System.out.println("GC recibió REQ: " + request);

                    if ("INFO".equals(operation)) {
                        // INFO requiere comunicación SÍNCRONA al GA
                        // Se delega al Proxy (que maneja su propia cola)
                        
                        // Generar el ID para esta petición
                        String proxyRequestId = messageId; 
                        
                        // Esperar la respuesta del Proxy de forma bloqueante (con timeout global)
                        String gaResponse = gaClientProxy.sendRequest(proxyRequestId, request);
                        
                        // Responder al cliente (REP)
                        replier.send(gaResponse);
                        
                    } else if ("PRESTAMO".equals(operation) || "DEVOLUCION".equals(operation) || "RENOVACION".equals(operation)) {
                        
                        // Publicar al Actor correspondiente (PUB/SUB)
                        publisher.sendMore(operation); // Tópico
                        publisher.send(request);      // Cuerpo del mensaje
                        replier.send("ACK|Petición aceptada. Procesando...");
                        
                    } else {
                        replier.send("ERROR|Operación no soportada.");
                    }
                }
                
                // Manejo de resultados de los Actores (PULL)
                if (items.pollin(1)) {
                    // Recibir resultado del Actor (ej: DEVOLUCION|messageId|EXITO|...)
                    String result = puller.recvStr();
                    if (result != null) {
                        System.out.println("GC recibió PULL (Resultado Actor): " + result);
                        // Lógica futura: Aquí puedes escribir el resultado a un log o enviarlo a un monitor.
                    }
                }
            }
        } catch (ZMQException e) {
            System.err.println("Error ZMQ en el Servidor GC: " + e.getMessage());
        } finally {
            context.close();
        }
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println("Uso: ServidorGC_ZMQ <sede> <pubPort> <repPort> <gaList>");
            System.exit(1);
        }
        
        try {
            int sede = Integer.parseInt(args[0]);
            String pubPort = args[1];
            String repPort = args[2];
            String gaList = args[3];
            
            ServidorGC_ZMQ gc = new ServidorGC_ZMQ(sede, pubPort, repPort, gaList);
            gc.iniciar();
            
        } catch (NumberFormatException e) {
            System.err.println("Error: La sede y los puertos deben ser números enteros.");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error al iniciar el Gestor de Carga: " + e.getMessage());
            e.printStackTrace();
        }
    }
}