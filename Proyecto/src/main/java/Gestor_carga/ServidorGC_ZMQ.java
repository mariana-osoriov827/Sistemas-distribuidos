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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class ServidorGC_ZMQ {
    
    private final int sede;
    private final String pubPort;
    private final String repPort;
    private final String[] gaHosts;
    private final int[] gaPorts;
    private int currentGaIndex = 0;
    
    // Mapas para seguimiento de mensajes
    private final Map<String, String> messageStatus = new ConcurrentHashMap<>();
    private final DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
    
    public ServidorGC_ZMQ(int sede, String pubPort, String repPort, String gaList) {
        this.sede = sede;
        this.pubPort = pubPort;
        this.repPort = repPort;
        
        // Parsear múltiples GAs
        String[] gaArray = gaList.split(",");
        this.gaHosts = new String[gaArray.length];
        this.gaPorts = new int[gaArray.length];
        
        for (int i = 0; i < gaArray.length; i++) {
            String[] parts = gaArray[i].split(":");
            this.gaHosts[i] = parts[0];
            this.gaPorts[i] = Integer.parseInt(parts[1]);
        }
        
        System.out.println("GC configurado con " + gaArray.length + " GA(s):");
        for (int i = 0; i < gaHosts.length; i++) {
            System.out.println("  GA" + (i+1) + ": " + gaHosts[i] + ":" + gaPorts[i]);
        }
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
            
            // Socket PULL para recibir resultados de actores (REP + 1)
            int resultPort = Integer.parseInt(repPort) + 1;
            ZMQ.Socket resultPuller = context.createSocket(ZMQ.PULL);
            resultPuller.bind("tcp://*:" + resultPort);
            System.out.println("GC Sede " + sede + " - Result Puller listo en puerto " + resultPort);
            
            // IMPORTANTE: No creamos socket REQ aquí para evitar bloqueos.
            // Los préstamos síncronos serán manejados por un Actor dedicado que
            // se comunica directamente con el GA usando patrón REQ/REP
            
            // Pequeña pausa para que los suscriptores se conecten
            Thread.sleep(1000);
            
            // Poller para manejar múltiples sockets
            ZMQ.Poller poller = context.createPoller(2);
            poller.register(replier, ZMQ.Poller.POLLIN);
            poller.register(resultPuller, ZMQ.Poller.POLLIN);
            
            System.out.println("ServidorGC_ZMQ Sede " + sede + " corriendo...\n");
            
            while (!Thread.currentThread().isInterrupted()) {
                
                // Poll con timeout de 100ms
                poller.poll(100);
                
                // Manejar resultados de actores (PULL socket)
                if (poller.pollin(1)) {
                    String resultMsg = resultPuller.recvStr();
                    // Formato: RESULT|messageId|status|mensaje|tipo  (mensaje puede faltar)
                    String[] resultParts = resultMsg.split("\\|", 5);
                    if (resultParts.length >= 4) {
                        String msgId = resultParts[1];
                        String status = resultParts[2];
                        String tipo = resultParts[resultParts.length - 1];
                        String mensajeCompleto;
                        if (resultParts.length == 5) {
                            // Hay mensaje de error real
                            mensajeCompleto = status + "|" + resultParts[3];
                        } else {
                            mensajeCompleto = status;
                        }
                        messageStatus.put(msgId, mensajeCompleto);
                        System.out.println("GC registró resultado " + tipo + " [" + msgId + "]: " + mensajeCompleto);
                    }
                }
                
                // Manejar todas las solicitudes (REP socket)
                if (poller.pollin(0)) {
                    String request = replier.recvStr();
                    System.out.println("GC recibió solicitud: " + request);
                    
                    // Formato: TIPO|codigoLibro|usuarioId
                    String[] parts = request.split("\\|");
                    if (parts.length >= 1) {
                        String tipo = parts[0].toUpperCase();
                        
                        if ("INFO".equals(tipo) && parts.length >= 2) {
                            // INFO: Consultar información del libro
                            String codigoLibro = parts[1];
                            String infoLibro = consultarInfoLibro(codigoLibro);
                            replier.send(infoLibro);
                            System.out.println("GC respondió INFO: " + infoLibro);
                            
                        } else if ("STATUS".equals(tipo) && parts.length >= 2) {
                            // STATUS: Consultar estado de operación asíncrona
                            String messageId = parts[1];
                            String status = messageStatus.getOrDefault(messageId, "PENDING");
                            replier.send("STATUS|" + status);
                            System.out.println("GC respondió STATUS para " + messageId + ": " + status);
                            // Limpiar solo si ya no es PENDING
                            if (!"PENDING".equals(status)) {
                                messageStatus.remove(messageId);
                            }
                        } else if ("CANCEL".equals(tipo)) {
                            // CANCEL: Cliente canceló operación
                            replier.send("OK|Cancelado");
                            System.out.println("GC: operación cancelada por cliente");
                            
                        } else if (parts.length >= 3) {
                            String codigoLibro = parts[1];
                            String usuarioId = parts[2];
                        
                            if ("PRESTAMO".equals(tipo)) {
                                // PRESTAMO: Publicar y esperar respuesta real del actor
                                String id = UUID.randomUUID().toString();
                                String fecha = LocalDate.now().format(fmt);
                                String mensaje = String.format("%s|%s|%s|%s|%s|%s", 
                                    tipo, id, codigoLibro, usuarioId, fecha, "null");
                                messageStatus.put(id, "PENDING"); // Poner antes de enviar
                                publisher.send(mensaje);
                                System.out.println("GC publicó PRESTAMO: " + mensaje);
                                // Esperar resultado real del actor (bloqueante, timeout opcional)
                                String resultado = esperarResultadoActor(id, 10000); // Aumenta timeout a 10s
                                if (resultado == null) {
                                    replier.send("ERROR|No se recibió respuesta del actor|" + id);
                                } else if (resultado.startsWith("OK")) {
                                    replier.send("OK|Préstamo otorgado");
                                } else {
                                    // El actor debe propagar el mensaje de error del GA
                                    replier.send(resultado);
                                }
                                System.out.println("GC respondió préstamo: " + (resultado != null ? resultado : "sin respuesta"));
                                
                            } else if ("DEVOLUCION".equals(tipo) || "RENOVACION".equals(tipo)) {
                                // DEVOLUCION/RENOVACION: Publicar y esperar respuesta real del actor
                                String id = UUID.randomUUID().toString();
                                String fecha = LocalDate.now().format(fmt);
                                String nuevaFecha = "RENOVACION".equals(tipo) ? 
                                    LocalDate.now().plusWeeks(1).format(fmt) : "null";
                                String mensaje = String.format("%s|%s|%s|%s|%s|%s", 
                                    tipo, id, codigoLibro, usuarioId, fecha, nuevaFecha);
                                messageStatus.put(id, "PENDING");
                                publisher.send(mensaje);
                                System.out.println("GC publicó " + tipo + ": " + mensaje);
                                // Esperar resultado real del actor (bloqueante, timeout opcional)
                                String resultado = esperarResultadoActor(id, 5000);
                                if (resultado == null) {
                                    replier.send("ERROR|No se recibió respuesta del actor");
                                } else if (resultado.startsWith("OK")) {
                                    replier.send("OK|" + ("DEVOLUCION".equals(tipo) ? "Devolución registrada" : "Renovación exitosa"));
                                } else {
                                    replier.send(resultado);
                                }
                                System.out.println("GC respondió " + tipo + ": " + (resultado != null ? resultado : "sin respuesta"));
                                
                            } else {
                                replier.send("ERROR|Tipo de operación desconocido");
                            }
                        } else {
                            replier.send("ERROR|Formato inválido - parámetros insuficientes");
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
     * Consulta información del libro en el GA con failover automático
     */
    private String consultarInfoLibro(String codigoLibro) {
        return consultarGA("INFO|" + codigoLibro + "|system");
    }
    
    /**
     * Valida si el libro tiene un préstamo activo con failover
     */
    private boolean validarPrestamo(String codigoLibro) {
        String response = consultarGA("VALIDAR_PRESTAMO|" + codigoLibro + "|system");
        return response != null && response.startsWith("OK|true");
    }
    
    /**
     * Consulta genérica al GA con failover automático
     */
    private String consultarGA(String request) {
        int intentos = 0;
        int maxIntentos = gaHosts.length * 2;
        
        while (intentos < maxIntentos) {
            String gaHost = gaHosts[currentGaIndex];
            int gaPort = gaPorts[currentGaIndex];
            
            try {
                java.net.Socket socket = new java.net.Socket();
                socket.connect(new java.net.InetSocketAddress(gaHost, gaPort), 2000);
                socket.setSoTimeout(3000);
                
                java.io.PrintWriter out = new java.io.PrintWriter(socket.getOutputStream(), true);
                java.io.BufferedReader in = new java.io.BufferedReader(
                    new java.io.InputStreamReader(socket.getInputStream()));
                
                out.println(request);
                String response = in.readLine();
                
                socket.close();
                return response != null ? response : "ERROR|Sin respuesta del GA";
                
            } catch (Exception e) {
                System.err.println("[FAILOVER] GA " + gaHost + ":" + gaPort + " no disponible");
                
                currentGaIndex = (currentGaIndex + 1) % gaHosts.length;
                intentos++;
                
                if (intentos < maxIntentos) {
                    System.out.println("[FAILOVER] GC intentando con GA " + gaHosts[currentGaIndex] + ":" + gaPorts[currentGaIndex]);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        
        return "ERROR|Todos los GAs no disponibles";
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
    
    // --- Agregar función auxiliar para esperar resultado del actor ---
    private String esperarResultadoActor(String id, int timeoutMs) {
        int waited = 0;
        int interval = 50;
        String status;
        while (waited < timeoutMs) {
            status = messageStatus.get(id);
            if (status != null && !"PENDING".equals(status)) {
                messageStatus.remove(id); // Limpiar estado
                if (status.startsWith("FAILED") || status.startsWith("ERROR")) {
                    return status;
                } else if (status.startsWith("OK")) {
                    return status;
                }
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            waited += interval;
        }
        // Última comprobación por si el resultado llegó justo después del timeout
        status = messageStatus.get(id);
        if (status != null && !"PENDING".equals(status)) {
            messageStatus.remove(id);
            return status;
        }
        messageStatus.remove(id);
        return null;
    }
    
    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Uso: java ServidorGC_ZMQ <sede> <pubPort> <repPort> <gaHost1:port1[,gaHost2:port2]>");
            System.out.println("Ejemplo Sede 1: java ServidorGC_ZMQ 1 5555 5556 localhost:5560,10.43.102.177:6560");
            System.out.println("Ejemplo Sede 2: java ServidorGC_ZMQ 2 6555 6556 localhost:6560,10.43.103.49:5560");
            System.exit(1);
        }
        
        int sede = Integer.parseInt(args[0]);
        String pubPort = args[1];
        String repPort = args[2];
        String gaList = args[3];
        
        ServidorGC_ZMQ servidor = new ServidorGC_ZMQ(sede, pubPort, repPort, gaList);
        servidor.iniciar();
    }
}
