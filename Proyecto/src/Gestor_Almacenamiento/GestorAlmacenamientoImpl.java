/**************************************************************************************
 * Fecha: 10/10/2025
 * Autor: Gabriel Jaramillo, Roberth Méndez, Mariana Osorio Vasquez, Juan Esteban Vera
 * Tema:
 * - Proyecto préstamo de libros (Sistema Distribuido)
 * Descripción:
 * - Implementación RMI del Gestor de Almacenamiento (GA).
 * - Hace de fachada remota que delega en BaseDatos las operaciones reales.
 * - Registra resultados y proporciona punto de extensión para replicación.
 ***************************************************************************************/
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * - Cada método remoto está sincronizado a nivel de instancia (synchronized) para evitar
 *   condiciones de carrera en la capa GA. Internamente la BaseDatos también usa sincronización.
 * - Las llamadas devuelven booleanos indicando éxito/fracaso de la operación.
 * - Se implementa replicación asíncrona cuando el GA es primario hacia la réplica.
 */
public class GestorAlmacenamientoImpl extends UnicastRemoteObject implements GestorAlmacenamiento {

    private static final long serialVersionUID = 1L;
    private final BaseDatos baseDatos;
    private final String rol;
    private String replicaHost; // Host de la réplica
    private final BlockingQueue<String[]> colaReplicacion; // Cola de operaciones pendientes de replicar
    private final ScheduledExecutorService replicaScheduler;
    private final AtomicBoolean replicaDisponible;

    public GestorAlmacenamientoImpl(BaseDatos bd, String rol) throws RemoteException {
        super();
        this.baseDatos = bd;
        this.rol = rol;
        this.colaReplicacion = new LinkedBlockingQueue<>();
        this.replicaScheduler = Executors.newScheduledThreadPool(1);
        this.replicaDisponible = new AtomicBoolean(false);
        
        // Si es primario, iniciar thread de replicación asíncrona cada 3 segundos
        if ("primary".equalsIgnoreCase(rol)) {
            iniciarReplicacionAsincrona();
        }
    }

    /**
     * - Aplicar Devolución
     * - Método sincronizado para proporcionar una capa adicional de exclusión mutua.
     * - Registra en consola el resultado.
     * - Si es primario, encola la operación para replicación asíncrona.
     */
    @Override
    public synchronized boolean aplicarDevolucionEnBD(String codigoLibro, String usuarioId) throws RemoteException {
        boolean exito = baseDatos.devolverEjemplar(codigoLibro);
        System.out.println("GA (" + rol + "): devolución de " + codigoLibro + " -> " + exito);
        
        // Si es primario y tuvo éxito, encolar para replicación
        if ("primary".equalsIgnoreCase(rol) && exito) {
            colaReplicacion.offer(new String[]{"DEVOLUCION", codigoLibro, usuarioId, ""});
        }
        
        return exito;
    }

    /**
     * - Aplicar renovación
     * - El parámetro nuevaFechaEntrega se transmite para que la BD pueda usarlo o ignorarlo.
     * - Si es primario, encola para replicación.
     */
    @Override
    public synchronized boolean aplicarRenovacionEnBD(String codigoLibro, String usuarioId, String nuevaFechaEntrega) throws RemoteException {
        boolean exito = baseDatos.renovarPrestamo(codigoLibro, usuarioId, nuevaFechaEntrega);
        System.out.println("GA (" + rol + "): renovación de " + codigoLibro + " -> " + exito);
        
        // Si es primario y tuvo éxito, encolar para replicación
        if ("primary".equalsIgnoreCase(rol) && exito) {
            colaReplicacion.offer(new String[]{"RENOVACION", codigoLibro, usuarioId, nuevaFechaEntrega});
        }
        
        return exito;
    }

    /**
     * - Aplicar préstamo:
     * - Retorna true si se pudo asignar un ejemplar disponible, false si no hay ejemplares libres.
     * - Si es primario, encola para replicación.
     */
    @Override
    public synchronized boolean aplicarPrestamoEnBD(String codigoLibro, String usuarioId) throws RemoteException {
        boolean exito = baseDatos.prestarEjemplar(codigoLibro);
        System.out.println("GA (" + rol + "): préstamo de " + codigoLibro + " -> " + exito);
        
        // Si es primario y tuvo éxito, encolar para replicación
        if ("primary".equalsIgnoreCase(rol) && exito) {
            colaReplicacion.offer(new String[]{"PRESTAMO", codigoLibro, usuarioId, ""});
        }
        
        return exito;
    }

    /**
     * Devuelve un resumen textual del estado de la BD.
     */
    @Override
    public String dumpDB() throws RemoteException {
        return baseDatos.dumpResumen();
    }

    /**
     * Punto de entrada para la replicación de operaciones desde el primario.
     * La réplica recibe y aplica las operaciones en su BD local.
     */
    @Override
    public void replicarOperacion(String op, String codigoLibro, String usuarioId, String fecha) throws RemoteException {
        System.out.println("GA (" + rol + ") replicando operación: " + op + " - " + codigoLibro + " - " + usuarioId);
        
        try {
            switch (op.toUpperCase()) {
                case "DEVOLUCION":
                    baseDatos.devolverEjemplar(codigoLibro);
                    break;
                case "RENOVACION":
                    baseDatos.renovarPrestamo(codigoLibro, usuarioId, fecha);
                    break;
                case "PRESTAMO":
                    baseDatos.prestarEjemplar(codigoLibro);
                    break;
                default:
                    System.err.println("Operación desconocida para replicar: " + op);
            }
        } catch (Exception e) {
            System.err.println("Error al replicar operación: " + e.getMessage());
        }
    }
    
    /**
     * Método para configurar el host de la réplica (desde ServidorGA).
     */
    public void setReplicaHost(String host) {
        this.replicaHost = host;
        System.out.println("Host de réplica configurado: " + host);
    }
    
    /**
     * Health Check: permite verificar si el GA está activo.
     */
    @Override
    public boolean healthCheck() throws RemoteException {
        return true;
    }
    
    /**
     * Inicia el thread de replicación asíncrona (solo para primario).
     * Cada 3 segundos procesa la cola de operaciones y las envía a la réplica.
     */
    private void iniciarReplicacionAsincrona() {
        replicaScheduler.scheduleWithFixedDelay(() -> {
            try {
                procesarColaReplicacion();
            } catch (Exception e) {
                System.err.println("Error en replicación asíncrona: " + e.getMessage());
            }
        }, 3, 3, TimeUnit.SECONDS);
        
        System.out.println("Replicación asíncrona iniciada (cada 3 segundos)");
    }
    
    /**
     * Procesa todas las operaciones pendientes en la cola y las envía a la réplica.
     */
    private void procesarColaReplicacion() {
        if (replicaHost == null || replicaHost.isEmpty()) {
            return; // No hay réplica configurada
        }
        
        // Primero verificar health check de la réplica
        if (!verificarHealthCheckReplica()) {
            return;
        }
        
        int operacionesReplicadas = 0;
        String[] operacion;
        
        while ((operacion = colaReplicacion.poll()) != null) {
            try {
                Registry regReplica = LocateRegistry.getRegistry(replicaHost, 1099);
                GestorAlmacenamiento stubReplica = (GestorAlmacenamiento) regReplica.lookup("GestorAlmacenamientoReplica");
                
                stubReplica.replicarOperacion(operacion[0], operacion[1], operacion[2], operacion[3]);
                operacionesReplicadas++;
                
            } catch (Exception e) {
                System.err.println("Error al replicar operación a réplica: " + e.getMessage());
                // Volver a encolar para reintento
                colaReplicacion.offer(operacion);
                replicaDisponible.set(false);
                break;
            }
        }
        
        if (operacionesReplicadas > 0) {
            System.out.println("Replicadas " + operacionesReplicadas + " operaciones a la réplica");
        }
    }
    
    /**
     * Verifica si la réplica está disponible mediante health check.
     */
    private boolean verificarHealthCheckReplica() {
        try {
            Registry regReplica = LocateRegistry.getRegistry(replicaHost, 1099);
            GestorAlmacenamiento stubReplica = (GestorAlmacenamiento) regReplica.lookup("GestorAlmacenamientoReplica");
            boolean alive = stubReplica.healthCheck();
            replicaDisponible.set(alive);
            return alive;
        } catch (Exception e) {
            replicaDisponible.set(false);
            System.err.println("Réplica no disponible: " + e.getMessage());
            return false;
        }
    }
}
