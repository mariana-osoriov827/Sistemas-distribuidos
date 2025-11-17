/**************************************************************************************
 * Fecha: 17/11/2025
 * Autor: Gabriel Jaramillo, Roberth Méndez, Mariana Osorio Vasquez, Juan Esteban Vera
 * Tema: Proyecto préstamo de libros (Sistema Distribuido)
 * Descripción:
 * - Gestor de Carga usando ZeroMQ (cumple requisitos del enunciado)
 * - PUB socket para publicar mensajes de Devolución/Renovación (asíncrono)
 * - REP socket para atender solicitudes de Préstamo (síncrono)
 ***************************************************************************************/

import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class ServidorGC_ZMQ {
    
    private final int sede;
    private final String pubPort;
    private final String repPort;
    private final String gaHost;
    private final int gaPort;
    
    // Mapas para seguimiento de mensajes
    private final Map<String, String> messageStatus = new ConcurrentHashMap<>();
    private final DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
    
    public ServidorGC_ZMQ(int sede, String pubPort, String repPort, String gaHost, int gaPort) {
        this.sede = sede;
        this.pubPort = pubPort;
        this.repPort = repPort;
        this.gaHost = gaHost;
        this.gaPort = gaPort;
    }
    
    public void iniciar() {
        try (ZContext context = new ZContext()) {
            
            // Socket PUB para publicar mensajes asíncronos (Devolución/Renovación)
            ZMQ.Socket publisher = context.createSocket(ZMQ.PUB);
            publisher.bind("tcp://*:" + pubPort);
            System.out.println("GC Sede " + sede + " - Publisher listo en puerto " + pubPort);
            
            // Socket REP para atender solicitudes síncronas (Préstamo)
            ZMQ.Socket replier = context.createSocket(ZMQ.REP);
            replier.bind("tcp://*:" + repPort);
            System.out.println("GC Sede " + sede + " - Replier listo en puerto " + repPort);
            
            // IMPORTANTE: No creamos socket REQ aquí para evitar bloqueos.
            // Los préstamos síncronos serán manejados por un Actor dedicado que
            // se comunica directamente con el GA usando patrón REQ/REP
            
            // Pequeña pausa para que los suscriptores se conecten
            Thread.sleep(1000);
            
            // Poller para manejar múltiples sockets
            ZMQ.Poller poller = context.createPoller(1);
            poller.register(replier, ZMQ.Poller.POLLIN);
            
            System.out.println("ServidorGC_ZMQ Sede " + sede + " corriendo...\n");
            
            while (!Thread.currentThread().isInterrupted()) {
                
                // Poll con timeout de 100ms
                poller.poll(100);
                
                // Manejar todas las solicitudes (REP socket)
                if (poller.pollin(0)) {
                    String request = replier.recvStr();
                    System.out.println("GC recibió solicitud: " + request);
                    
                    // Formato: TIPO|codigoLibro|usuarioId
                    String[] parts = request.split("\\|");
                    if (parts.length >= 3) {
                        String tipo = parts[0].toUpperCase();
                        String codigoLibro = parts[1];
                        String usuarioId = parts[2];
                        
                        if ("PRESTAMO".equals(tipo)) {
                            // PRESTAMO: Síncrono - publicar para que Actor Préstamo lo procese
                            // y esperar respuesta mediante un canal de vuelta
                            String id = UUID.randomUUID().toString();
                            String fecha = LocalDate.now().format(fmt);
                            
                            // Publicar solicitud de préstamo
                            String mensaje = String.format("%s|%s|%s|%s|%s|%s", 
                                tipo, id, codigoLibro, usuarioId, fecha, "null");
                            publisher.send(mensaje);
                            System.out.println("GC publicó PRESTAMO: " + mensaje);
                            
                            // El Actor de Préstamo procesará esto de forma síncrona con el GA
                            // Por ahora, respondemos que se está procesando
                            // En una implementación completa, necesitaríamos un canal de respuesta
                            replier.send("OK|Préstamo en proceso|" + id);
                            System.out.println("GC respondió préstamo: en proceso");
                            
                        } else if ("DEVOLUCION".equals(tipo) || "RENOVACION".equals(tipo)) {
                            // DEVOLUCION/RENOVACION: Asíncrono - responder inmediatamente y publicar
                            String id = UUID.randomUUID().toString();
                            String fecha = LocalDate.now().format(fmt);
                            String nuevaFecha = "RENOVACION".equals(tipo) ? 
                                LocalDate.now().plusWeeks(1).format(fmt) : "null";
                            
                            // Responder al cliente inmediatamente
                            replier.send("OK|Aceptado|" + id);
                            System.out.println("GC aceptó " + tipo + " inmediatamente");
                            
                            // Publicar mensaje a los actores
                            String mensaje = String.format("%s|%s|%s|%s|%s|%s", 
                                tipo, id, codigoLibro, usuarioId, fecha, nuevaFecha);
                            publisher.send(mensaje);
                            messageStatus.put(id, "PENDING");
                            System.out.println("GC publicó " + tipo + ": " + mensaje);
                            
                        } else {
                            replier.send("ERROR|Tipo de operación desconocido");
                        }
                    } else {
                        replier.send("ERROR|Formato inválido");
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Publica mensaje asíncrono (Devolución o Renovación)
     */
    public void publicarMensajeAsync(ZMQ.Socket publisher, String tipo, String codigoLibro, String usuarioId) {
        String id = UUID.randomUUID().toString();
        String fecha = LocalDate.now().format(fmt);
        String nuevaFecha = null;
        
        if ("RENOVACION".equalsIgnoreCase(tipo)) {
            nuevaFecha = LocalDate.now().plusWeeks(1).format(fmt);
        }
        
        // Formato: TIPO|id|codigoLibro|usuarioId|fecha|nuevaFecha
        String mensaje = String.format("%s|%s|%s|%s|%s|%s", 
            tipo.toUpperCase(), id, codigoLibro, usuarioId, fecha, 
            nuevaFecha != null ? nuevaFecha : "null");
        
        publisher.send(mensaje);
        messageStatus.put(id, "PENDING");
        System.out.println("GC publicó " + tipo + ": " + mensaje);
    }
    
    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Uso: java ServidorGC_ZMQ <sede> <pubPort> <repPort> <gaHost> <gaPort>");
            System.out.println("Ejemplo Sede 1: java ServidorGC_ZMQ 1 5555 5556 localhost 5560");
            System.out.println("Ejemplo Sede 2: java ServidorGC_ZMQ 2 6555 6556 localhost 6560");
            System.exit(1);
        }
        
        int sede = Integer.parseInt(args[0]);
        String pubPort = args[1];
        String repPort = args[2];
        String gaHost = args[3];
        int gaPort = Integer.parseInt(args[4]);
        
        ServidorGC_ZMQ servidor = new ServidorGC_ZMQ(sede, pubPort, repPort, gaHost, gaPort);
        servidor.iniciar();
    }
}
