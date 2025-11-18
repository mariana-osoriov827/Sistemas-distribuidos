/**************************************************************************************
 * Fecha: 17/11/2025
 * Autor: Gabriel Jaramillo, Roberth Méndez, Mariana Osorio Vasquez, Juan Esteban Vera
 * Tema: Proyecto préstamo de libros (Sistema Distribuido)
 * Descripción:
 * - Gestor de Almacenamiento usando sockets TCP (no RMI)
 * - Atiende solicitudes de Actores y GC
 * - Mantiene replicación asíncrona hacia GA réplica
 ***************************************************************************************/
package Gestor_Almacenamiento;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ServidorGA_TCP {
    
    private final BaseDatos bd;
    private final String role;
    private final int port;
    private final String replicaHost;
    private final int replicaPort;
    
    // Replicación asíncrona
    private final BlockingQueue<String> colaReplicacion = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService replicaScheduler = Executors.newScheduledThreadPool(1);
    private boolean replicaDisponible = true;
    private int intentosFallidos = 0;
    
    public ServidorGA_TCP(String role, int port, String replicaHost, Integer replicaPort) {
        this.role = role;
        this.port = port;
        this.replicaHost = replicaHost;
        this.replicaPort = (replicaPort != null) ? replicaPort : 0;
        
        // Cargar BD
        this.bd = new BaseDatos();
        try {
            bd.cargarDesdeArchivo("src/libros.txt");
        } catch (Exception e) {
            System.out.println("No se pudo cargar libros.txt, BD vacía.");
        }
        
        // Iniciar replicación si es primario
        if ("primary".equalsIgnoreCase(role) && replicaHost != null) {
            iniciarReplicacionAsincrona();
        }
    }
    
    public void iniciar() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("GA (" + role + ") escuchando en puerto " + port);
            
            // Guardar BD al cerrar
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    bd.guardarEnArchivo("src/libros.txt");
                    System.out.println("BD guardada en libros.txt");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> manejarCliente(clientSocket)).start();
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void manejarCliente(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            
            String request = in.readLine();
            if (request == null) return;
            
            System.out.println("GA recibió: " + request);
            
            // Formato: TIPO|codigoLibro|usuarioId[|nuevaFecha]
            String[] parts = request.split("\\|");
            if (parts.length < 2) {
                out.println("ERROR|Formato inválido");
                return;
            }
            
            String tipo = parts[0];
            String codigoLibro = parts[1];
            String usuarioId = parts.length > 2 ? parts[2] : "system";
            
            boolean resultado = false;
            String respuesta;
            
            switch (tipo.toUpperCase()) {
                case "INFO":
                    String nombreLibro = bd.obtenerNombreLibro(codigoLibro);
                    respuesta = nombreLibro != null ? "OK|" + nombreLibro : "FAILED|Libro no encontrado";
                    break;
                    
                case "PRESTAMO":
                    resultado = bd.prestarEjemplar(codigoLibro);
                    respuesta = resultado ? "OK|Préstamo otorgado" : "FAILED|No disponible";
                    if (resultado) encolarReplicacion("PRESTAMO", codigoLibro, usuarioId, null);
                    break;
                    
                case "DEVOLUCION":
                    resultado = bd.devolverEjemplar(codigoLibro);
                    respuesta = resultado ? "OK|Devolución registrada" : "FAILED|Error en devolución";
                    if (resultado) encolarReplicacion("DEVOLUCION", codigoLibro, usuarioId, null);
                    break;
                    
                case "RENOVACION":
                    String nuevaFecha = parts.length > 3 ? parts[3] : null;
                    resultado = bd.renovarPrestamo(codigoLibro, usuarioId, nuevaFecha);
                    respuesta = resultado ? "OK|Renovación exitosa" : "FAILED|Máximo renovaciones alcanzado";
                    if (resultado) encolarReplicacion("RENOVACION", codigoLibro, usuarioId, nuevaFecha);
                    break;
                    
                default:
                    respuesta = "ERROR|Operación desconocida";
            }
            
            out.println(respuesta);
            System.out.println("GA respondió: " + respuesta);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void encolarReplicacion(String tipo, String codigo, String usuario, String fecha) {
        if ("primary".equalsIgnoreCase(role) && replicaHost != null) {
            String operacion = tipo + "|" + codigo + "|" + usuario + "|" + fecha;
            colaReplicacion.offer(operacion);
        }
    }
    
    private void iniciarReplicacionAsincrona() {
        replicaScheduler.scheduleAtFixedRate(() -> {
            if (!colaReplicacion.isEmpty()) {
                procesarColaReplicacion();
            }
        }, 3, 3, TimeUnit.SECONDS);
    }
    
    private void procesarColaReplicacion() {
        int replicadas = 0;
        try (Socket socket = new Socket(replicaHost, replicaPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            String operacion;
            while ((operacion = colaReplicacion.poll()) != null) {
                out.println(operacion);
                String ack = in.readLine();
                replicadas++;
            }
            
            if (replicadas > 0) {
                System.out.println("Replicadas " + replicadas + " operaciones a la réplica");
            }
            
            // Resetear contador de fallos si tuvo éxito
            if (!replicaDisponible && replicadas > 0) {
                System.out.println("Réplica reconectada exitosamente");
            }
            replicaDisponible = true;
            intentosFallidos = 0;
            
        } catch (Exception e) {
            intentosFallidos++;
            // Solo mostrar error cada 10 intentos fallidos
            if (replicaDisponible || intentosFallidos % 10 == 0) {
                System.err.println("Réplica no disponible (intento " + intentosFallidos + "): " + e.getMessage());
            }
            replicaDisponible = false;
        }
    }
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java ServidorGA_TCP <role> <port> [replicaHost] [replicaPort]");
            System.out.println("Ejemplo Primary: java ServidorGA_TCP primary 5560 10.43.102.177 6560");
            System.out.println("Ejemplo Replica: java ServidorGA_TCP replica 6560");
            System.exit(1);
        }
        
        String role = args[0];
        int port = Integer.parseInt(args[1]);
        String replicaHost = args.length > 2 ? args[2] : null;
        Integer replicaPort = args.length > 3 ? Integer.parseInt(args[3]) : null;
        
        ServidorGA_TCP servidor = new ServidorGA_TCP(role, port, replicaHost, replicaPort);
        servidor.iniciar();
    }
}
