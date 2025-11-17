/**************************************************************************************
* Fecha: 10/10/2025
* Autor: Gabriel Jaramillo, Roberth Méndez, Mariana Osorio Vasquez, Juan Esteban Vera
* Tema: 
* - Proyecto préstamo de libros (Sistema Distribuido)
* Descripción:
* - Interfaz de Servicio Remoto (GestorAlmacenamiento):
* - Define el contrato RMI (`Remote`) para que los Actores (Renovación/Devolución) 
* puedan invocar remotamente las operaciones de modificación de datos sobre la Base de Datos local.
* - Incluye métodos para la aplicación de Devolución y Renovación, así como 
* para la replicación de cambios con el GA de la otra sede.
***************************************************************************************/
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GestorAlmacenamiento extends Remote {

    /**
     * - Aplicar Devolución
     * - Método sincronizado para proporcionar una capa adicional de exclusión mutua.
     * - Registra en consola el resultado.
     */
    boolean aplicarDevolucionEnBD(String codigoLibro, String usuarioId) throws RemoteException;
    
    /**
     * - Aplicar renovación
     * - El parámetro nuevaFechaEntrega se transmite para que la BD pueda usarlo o ignorarlo.
     */
    boolean aplicarRenovacionEnBD(String codigoLibro, String usuarioId, String nuevaFechaEntrega) throws RemoteException;
    
    /**
     * - Aplicar préstamo:
     * - Retorna true si se pudo asignar un ejemplar disponible, false si no hay ejemplares libres.
     */
    boolean aplicarPrestamoEnBD(String codigoLibro, String usuarioId) throws RemoteException;

    /**
     * Devuelve un resumen textual del estado de la BD.
     */
    String dumpDB() throws RemoteException;

    /**
     * Punto de entrada para la replicación de operaciones.
     */
    void replicarOperacion(String op, String codigoLibro, String usuarioId, String fecha) throws RemoteException;

}
