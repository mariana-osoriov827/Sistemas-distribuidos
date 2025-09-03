/**************************************************************************************
* Fecha: 05/09/2025
* Autores: Mariana Osorio Vasquez, Gabriel Jaramillo, Roberth Méndez, Juan Esteban Vera
* Tema:
* - Servidor RMI en Java
* Descripción:
* - Punto de entrada del servidor RMI de la biblioteca.
* - Crea una instancia de BibliotecaImpl y la registra en el Registry en el puerto 1099.
* - Expone el servicio con el nombre "BibliotecaService" para que los clientes puedan acceder.
* - Añade un shutdown hook que asegura el guardado de la base de datos en un archivo de salida
*   antes de que el servidor se apague.
**************************************************************************************/
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Clase principal del servidor RMI.
 * Se encarga de inicializar el servicio de biblioteca, registrar el objeto remoto en el registro RMI y
 * garantizar el guardado de la base de datos al finalizar.
 */
public class ServidorRMI {
  public static void main(String[] args) {

    try {
      // Archivos usados para cargar y guardar la base de datos de libros
      String archivoLibros = "libros.txt";
      String archivoSalida = "salida.txt";

      // Se crea la implementación del objeto remoto con la base de datos inicial
      BibliotecaImpl obj = new BibliotecaImpl(archivoLibros);

      // Se crea y arranca un registro RMI en el puerto 1099
      Registry registry = LocateRegistry.createRegistry(1099);

      // Se asocia el objeto remoto con un nombre para que los clientes puedan encontrarlo
      registry.rebind("BibliotecaService", obj);

      System.out.println("Servidor RMI listo en puerto 1099...");

      // Se añade un hook para que, cuando el servidor se cierre, la base de datos se guarde automáticamente en archivoSalida
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("Guardando base de datos en " + archivoSalida);
        obj.guardar(archivoSalida);
      }));

    } catch (Exception e) { 
        e.printStackTrace(); // Captura y muestra cualquier error que ocurra durante la ejecución del servidor
    }

  }

}
