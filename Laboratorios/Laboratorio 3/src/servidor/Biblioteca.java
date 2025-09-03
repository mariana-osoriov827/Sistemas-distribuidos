/**************************************************************************************
* Fecha: 05/09/2025
* Autores: Mariana Osorio Vasquez, Gabriel Jaramillo, Roberth Méndez, Juan Esteban Vera
* Tema:
* - Interfaz remota RMI en Java
* Descripción:
* - Define la interfaz remota Biblioteca que extiende de Remote.
* - Especifica las operaciones que el cliente puede invocar en el servidor de manera remota:
*   prestar, devolver, renovar libros, generar reportes, y gestionar la conexión/desconexión
*   de clientes.
* - Todas las operaciones declaran RemoteException para manejar errores en la comunicación RMI.
**************************************************************************************/
import java.rmi.Remote;          
import java.rmi.RemoteException; 
import java.util.List;

/**
 * Interfaz remota Biblioteca.
 * Define las operaciones que los clientes pueden invocar en el servidor vía RMI.
 * Cada método lanza RemoteException, ya que la comunicación puede fallar.
 */
public interface Biblioteca extends Remote {

    /**
     * Permite a un cliente solicitar un préstamo de libro.
     * @param nombre Nombre del libro.
     * @param isbn Código ISBN del libro.
     * @return Respuesta del servidor con el resultado de la operación.
     * @throws RemoteException Si ocurre un error en la comunicación remota.
     */
    String prestar(String nombre, int isbn) throws RemoteException;

    /**
     * Permite a un cliente devolver un libro prestado.
     * @param nombre Nombre del libro.
     * @param isbn Código ISBN del libro.
     * @return Respuesta del servidor con el resultado de la operación.
     * @throws RemoteException Si ocurre un error en la comunicación remota.
     */
    String devolver(String nombre, int isbn) throws RemoteException;

    /**
     * Permite a un cliente renovar un préstamo de libro.
     * @param nombre Nombre del libro.
     * @param isbn Código ISBN del libro.
     * @return Respuesta del servidor con el resultado de la operación.
     * @throws RemoteException Si ocurre un error en la comunicación remota.
     */
    String renovar(String nombre, int isbn) throws RemoteException;

    /**
     * Devuelve un reporte del estado actual de la biblioteca.
     * @return Lista de cadenas con información de los libros (ej. disponibles, prestados, etc.).
     * @throws RemoteException Si ocurre un error en la comunicación remota.
     */
    List<String> reporte() throws RemoteException;

    /**
     * Notifica al servidor que un cliente se ha conectado.
     * @param clienteId Identificador del cliente (ej. nombre de usuario del sistema).
     * @throws RemoteException Si ocurre un error en la comunicación remota.
     */
    void conectar(String clienteId) throws RemoteException;

    /**
     * Notifica al servidor que un cliente se ha desconectado.
     * @param clienteId Identificador del cliente (ej. nombre de usuario del sistema).
     * @throws RemoteException Si ocurre un error en la comunicación remota.
     */
    void desconectar(String clienteId) throws RemoteException;

}

