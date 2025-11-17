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

/**
 * - Cada método remoto está sincronizado a nivel de instancia (synchronized) para evitar
 *   condiciones de carrera en la capa GA. Internamente la BaseDatos también usa sincronización.
 * - Las llamadas devuelven booleanos indicando éxito/fracaso de la operación.
 */
public class GestorAlmacenamientoImpl extends UnicastRemoteObject implements GestorAlmacenamiento {

    private static final long serialVersionUID = 1L;
    private final BaseDatos baseDatos;
    private final String rol;

    public GestorAlmacenamientoImpl(BaseDatos bd, String rol) throws RemoteException {
        super();
        this.baseDatos = bd;
        this.rol = rol;
    }

    /**
     * - Aplicar Devolución
     * - Método sincronizado para proporcionar una capa adicional de exclusión mutua.
     * - Registra en consola el resultado.
     */
    @Override
    public synchronized boolean aplicarDevolucionEnBD(String codigoLibro, String usuarioId) throws RemoteException {
        boolean exito = baseDatos.devolverEjemplar(codigoLibro);
        System.out.println("GA (" + rol + "): devolución de " + codigoLibro + " -> " + exito);
        return exito;
    }

    /**
     * - Aplicar renovación
     * - El parámetro nuevaFechaEntrega se transmite para que la BD pueda usarlo o ignorarlo.
     */
    @Override
    public synchronized boolean aplicarRenovacionEnBD(String codigoLibro, String usuarioId, String nuevaFechaEntrega) throws RemoteException {
        boolean exito = baseDatos.renovarPrestamo(codigoLibro, usuarioId, nuevaFechaEntrega);
        System.out.println("GA (" + rol + "): renovación de " + codigoLibro + " -> " + exito);
        return exito;
    }

    /**
     * - Aplicar préstamo:
     * - Retorna true si se pudo asignar un ejemplar disponible, false si no hay ejemplares libres.
     */
    @Override
    public synchronized boolean aplicarPrestamoEnBD(String codigoLibro, String usuarioId) throws RemoteException {
        boolean exito = baseDatos.prestarEjemplar(codigoLibro);
        System.out.println("GA (" + rol + "): préstamo de " + codigoLibro + " -> " + exito);
        // Aquí se podría llamar a replicarOperacion("PRESTAMO", codigoLibro, usuarioId, fecha)
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
     * Punto de entrada para la replicación de operaciones.
     */
    @Override
    public void replicarOperacion(String op, String codigoLibro, String usuarioId, String fecha) throws RemoteException {
        System.out.println("GA (" + rol + ") replicando operación: " + op + " - " + codigoLibro + " - " + usuarioId);
    }
    
}
