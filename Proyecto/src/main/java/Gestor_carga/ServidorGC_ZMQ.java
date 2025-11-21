/**************************************************************************************
 * Fecha: 17/11/2025
 * Autor: Gabriel Jaramillo, Roberth Méndez, Mariana Osorio Vasquez, Juan Esteban Vera
 * Tema: Proyecto préstamo de libros (Sistema Distribuido)
 * Descripción:
 * - Gestor de Carga usando ZeroMQ (cumple requisitos del enunciado)
 * - PUB socket para publicar mensajes de Devolución/Renovación (asíncrono)
 * - REP socket para atender solicitudes de Préstamo (síncrono)
 ***************************************************************************************/
package Gestor_carga;

import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;
import org.zeromq.ZMQException;
import java.util.concurrent.TimeUnit;
import java.util.Map;

public class ServidorGC_ZMQ {
    private final int sede;
    private final String pubPort;
    private final String repPort;
    private final String[] gaHosts;
    private final int[] gaPorts;

    private GAClientProxy gaProxy;
    private final String internalProxyPort = "gc_proxy_requests";

    // Mapas para seguimiento de mensajes asíncronos (Devolución/Renovación)
    private final Map<String, String> messageStatus = new ConcurrentHashMap<String, String>();
    public static void main(String[] args) {
        // Debe haber exactamente 4 argumentos: sede, pubPort, repPort, gaList
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
            gc.iniciar(); // Llamar al método que contiene la lógica ZMQ
            
        } catch (NumberFormatException e) {
            System.err.println("Error: La sede debe ser un número entero.");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error al iniciar el Gestor de Carga: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public ServidorGC_ZMQ(int sede, String pubPort, String repPort, String gaAddress) {
        this.sede = sede;
        this.pubPort = pubPort;
        this.repPort = repPort;
        
        // Configuración de GAs para failover
        String[] gaServers = gaAddress.split(",");
        this.gaHosts = new String[gaServers.length];
        this.gaPorts = new int[gaServers.length];
        for (int i = 0; i < gaServers.length; i++) {
            String[] parts = gaServers[i].trim().split(":");
            this.gaHosts[i] = parts[0];
            this.gaPorts[i] = Integer.parseInt(parts[1]);
        }
        
        System.out.println("GC Sede " + sede + " | GA configurados: " + Arrays.toString(gaServers));
    }
    
    public void iniciar() {
        try (ZContext context = new ZContext()) {
            
            // 1. INICIAR EL PROXY DE COMUNICACIÓN CON EL GA (No bloqueante para este hilo)
            gaProxy = new GAClientProxy(context, gaHosts, gaPorts, internalProxyPort);
            gaProxy.start();
            
            // Socket PUB para publicar mensajes asíncronos (Devolución/Renovación)
            ZMQ.Socket publisher = context.createSocket(ZMQ.PUB);
            publisher.bind("tcp://*:" + pubPort);
            System.out.println("GC Sede " + sede + " | PUB escuchando en puerto " + pubPort);
            
            // Socket REP para recibir peticiones síncronas de Clientes
            ZMQ.Socket replier = context.createSocket(ZMQ.REP);
            replier.bind("tcp://*:" + repPort);
            System.out.println("GC Sede " + sede + " | REP escuchando en puerto " + repPort);
            
            // Socket PULL para recibir resultados asíncronos de los Actores
            ZMQ.Socket resultPuller = context.createSocket(ZMQ.PULL);
            // Asumimos que el PULL se conecta a un puerto conocido, aquí se usa 5557 como ejemplo
            resultPuller.bind("tcp://*:5557"); 
            System.out.println("GC Sede " + sede + " | PULL escuchando en puerto 5557");
            
            // Configuración del Poller
            ZMQ.Poller poller = context.createPoller(2); // Solo 2 sockets principales
            poller.register(replier, ZMQ.Poller.POLLIN); 
            poller.register(resultPuller, ZMQ.Poller.POLLIN);
            
            Thread.sleep(1000); // Pausa para inicialización
            
            // Bucle principal de manejo de eventos
            while (!Thread.currentThread().isInterrupted()) {
                // Espera hasta 100ms por un evento
                poller.poll(100); 
                
                // 1. Manejo de peticiones síncronas (Cliente REQ/REP)
                if (poller.pollin(0)) {
                    String request = replier.recvStr();
                    System.out.println("GC recibió REQ: " + request);
                    
                    String[] parts = request.split("\\|", 3);
                    String tipo = parts[0];
                    
                    if (parts.length >= 1) {
                        String response;
                        String messageId = UUID.randomUUID().toString();
                        
                        switch (tipo) {
                            case "INFO":
                                // >>> MANEJO SÍNCRONO CON EL PROXY (NO BLOQUEA ESTE HILO)
                                if (parts.length >= 2) {
                                    String codigoLibro = parts[1];
                                    
                                    // 1. Enviar la petición al Proxy 
                                    gaProxy.sendRequest(messageId, "INFO|" + codigoLibro + "|system");
                                    
                                    // 2. Esperar la respuesta del Proxy (síncrono para el cliente)
                                    response = gaProxy.getResponse(messageId, 5000); 
                                    // <<< FIN MANEJO SÍNCRONO CON EL PROXY
                                } else {
                                    response = "ERROR|Faltan parámetros para INFO";
                                }
                                break;
                                
                            case "PRESTAMO":
                                if (parts.length >= 3) {
                                    // ... Lógica de PRESTAMO (similar a INFO, usa el GA para validación)
                                    String codigoLibro = parts[1];
                                    String userId = parts[2];
                                    
                                    // 1. Asignar el trabajo a un Actor (Patrón Asíncrono)
                                    String actorRequest = "PRESTAMO|" + codigoLibro + "|" + userId + "|" + messageId;
                                    publisher.sendMore("PRESTAMO");
                                    publisher.send(actorRequest);
                                    
                                    messageStatus.put(messageId, "PENDING"); // Marcar como pendiente
                                    
                                    // 2. Esperar el resultado del Actor (simula REQ/REP síncrono para el cliente)
                                    response = esperarResultadoActor(messageId, 5000); // 5 segundos
                                    
                                } else {
                                    response = "ERROR|Faltan parámetros para PRESTAMO";
                                }
                                break;
                                
                            case "DEVOLUCION":
                            case "RENOVACION":
                                // Respuestas inmediatas y publicación asíncrona
                                if (parts.length >= 3) {
                                    String codigoLibro = parts[1];
                                    String userId = parts[2];
                                    
                                    // 1. Respuesta inmediata al cliente
                                    response = "OK|Operación de " + tipo + " aceptada. Procesando...";
                                    
                                    // 2. Publicar asíncronamente a los Actores
                                    publisher.sendMore(tipo); // Tópico
                                    publisher.send(tipo + "|" + codigoLibro + "|" + userId + "|" + messageId); // Mensaje
                                    
                                } else {
                                    response = "ERROR|Faltan parámetros para " + tipo;
                                }
                                break;
                                
                            case "STATUS":
                                if (parts.length >= 2) {
                                    String statusId = parts[1];
                                    String status = messageStatus.getOrDefault(statusId, "PENDING");
                                    response = "STATUS|" + status;
                                } else {
                                    response = "ERROR|Faltan parámetros para STATUS";
                                }
                                break;
                                
                            case "CANCEL":
                                // Usado por el cliente para liberar el lockstep del REP
                                response = "OK|Cancelación recibida";
                                break;
                                
                            default:
                                response = "ERROR|Operación desconocida: " + tipo;
                                break;
                        }
                        
                        replier.send(response);
                        System.out.println("GC respondió REQ: " + response);
                    }
                }
                
                // 2. Manejo de resultados asíncronos (Actor PUSH/PULL)
                if (poller.pollin(1)) {
                    String result = resultPuller.recvStr();
                    System.out.println("GC recibió PULL (Resultado Actor): " + result);
                    
                    // El resultado debe tener el formato: TIPO|messageId|STATUS_O_MENSAJE
                    String[] resultParts = result.split("\\|", 3);
                    if (resultParts.length >= 3) {
                        String messageId = resultParts[1];
                        String status = resultParts[2];
                        
                        // Almacenar el resultado o mensaje final
                        messageStatus.put(messageId, status);
                    }
                }
            }
        } catch (InterruptedException e) {
            System.err.println("GC interrumpido: " + e.getMessage());
        } finally {
            if (gaProxy != null) {
                gaProxy.interrupt();
            }
        }
    }
    // Método auxiliar para simular la espera síncrona del cliente por una operación asíncrona
    private String esperarResultadoActor(String messageId, int timeoutMs) {
        int waited = 0;
        int interval = 100; // Polling en este hilo
        
        while (waited < timeoutMs) {
            String status = messageStatus.get(messageId);
            if (status != null && !"PENDING".equals(status)) {
                messageStatus.remove(messageId);
                return status;
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            waited += interval;
        }
        
        return "ERROR|No se recibió respuesta del actor|" + messageId;
    }
}
