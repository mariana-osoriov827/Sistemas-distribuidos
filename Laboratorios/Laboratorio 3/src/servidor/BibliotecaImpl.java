/**************************************************************************************
* Fecha: 05/09/2025
* Autores: Mariana Osorio Vasquez, Gabriel Jaramillo, Roberth Méndez, Juan Esteban Vera
* Tema:
* - Implementación del servicio remoto de biblioteca en Java RMI
* Descripción:
* - Implementa la interfaz remota Biblioteca y extiende UnicastRemoteObject para que
*   pueda exportarse como objeto RMI.
* - Administra los procesos de negocio de la biblioteca: préstamo, devolución, renovación,
*   generación de reportes y control de clientes conectados.
* - Guarda el estado de la base de datos antes de finalizar el servidor.
**************************************************************************************/
import java.rmi.server.UnicastRemoteObject; 
import java.rmi.RemoteException;            
import java.util.List;

// Clase que implementa la interfaz remota Biblioteca y define el comportamiento del servidor
public class BibliotecaImpl extends UnicastRemoteObject implements Biblioteca {
    
    // Objeto que gestiona la base de datos de libros
    private BaseDatos bd;

    /**
     * Constructor de BibliotecaImpl.
     * Inicializa la base de datos cargando la información de un archivo.
     * @param archivoLibros Ruta del archivo donde están registrados los libros.
     * @throws RemoteException Si ocurre un error en la comunicación RMI.
     */
    public BibliotecaImpl(String archivoLibros) throws RemoteException {

      // Se llama al constructor de UnicastRemoteObject que registra el objeto en la infraestructura RMI para que pueda recibir llamadas remotas.
      super(); 
      // Se crea una nueva instancia de la base de datos
      bd = new BaseDatos(); 

      try {
        bd.cargarBD(archivoLibros); // Se cargan los datos de los libros desde el archivo
      } catch (Exception e) {
        System.err.println("Error cargando BD: " + e.getMessage());// Si ocurre un error al cargar la BD, se muestra el mensaje en consola
      }

    }

    /**
     * Permite a un cliente realizar un préstamo de libros.
     * @param nombre Nombre del libro.
     * @param isbn Código ISBN del libro.
     * @return Respuesta con el resultado del préstamo.
     */
    @Override
    public String prestar(String nombre, int isbn) throws RemoteException {

      String resp = bd.procesarPrestamo(nombre, isbn);
      System.out.println("[Servidor] Préstamo: " + nombre + " (" + isbn + ") -> " + resp);
      return resp;

    }

    /**
     * Permite a un cliente devolver un libro previamente prestado.
     * @param nombre Nombre del libro.
     * @param isbn Código ISBN del libro.
     * @return Respuesta con el resultado de la devolución.
     */
    @Override
    public String devolver(String nombre, int isbn) throws RemoteException {

      String resp = bd.procesarDevolucion(nombre, isbn);
      System.out.println("[Servidor] Devolución: " + nombre + " (" + isbn + ") -> " + resp);
      return resp;

    }

    /**
     * Permite a un cliente renovar un préstamo de libro.
     * @param nombre Nombre del libro.
     * @param isbn Código ISBN del libro.
     * @return Respuesta con el resultado de la renovación.
     */
    @Override
    public String renovar(String nombre, int isbn) throws RemoteException {

      String resp = bd.procesarRenovacion(nombre, isbn);
      System.out.println("[Servidor] Renovación: " + nombre + " (" + isbn + ") -> " + resp);
      return resp;

    }

    /**
     * Devuelve un reporte del estado de la biblioteca (libros disponibles, prestados, etc.).
     * @return Lista de cadenas con la información del reporte.
     */
    @Override
    public List<String> reporte() throws RemoteException {

      System.out.println("[Servidor] Reporte solicitado.");
      return bd.mostrarReporte();

    }

    /**
     * Registra la conexión de un cliente al servidor.
     * @param clienteId Identificador del cliente (ej. nombre de usuario).
     */
    @Override
    public void conectar(String clienteId) throws RemoteException {
      System.out.println("[Servidor] Cliente conectado: " + clienteId);
    }

    /**
     * Registra la desconexión de un cliente del servidor.
     * @param clienteId Identificador del cliente (ej. nombre de usuario).
     */
    @Override
    public void desconectar(String clienteId) throws RemoteException {
        System.out.println("[Servidor] Cliente desconectado: " + clienteId);
    }

    /**
     * Guarda la base de datos de la biblioteca en un archivo antes de finalizar la ejecución.
     * @param archivoSalida Ruta del archivo donde se guardará la información.
     */
    public void guardar(String archivoSalida) {
        try {
            bd.guardarBD(archivoSalida);
        } catch (Exception e) {
            System.err.println("Error guardando BD: " + e.getMessage());
        }
    }
}
