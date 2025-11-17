/**************************************************************************************
* Fecha: 10/10/2025
* Autor: Gabriel Jaramillo, Roberth Méndez, Mariana Osorio Vasquez, Juan Esteban Vera
* Tema: 
* - Proyecto préstamo de libros (Sistema Distribuido)
* Descripción:
* - Clase Servidor Principal (ServidorGC):
* - Programa de arranque para el Gestor de Carga (GC).
* - Inicializa la implementación del servicio (`BibliotecaGCImpl`).
* - Configura y expone el servicio GC a través del RMI Registry en el puerto 3000, 
* bajo el nombre "BibliotecaGCService".
* - Permite que tanto los Clientes como los Actores se conecten a este 
* punto central para el flujo de mensajes asíncronos.
***************************************************************************************/
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServidorGC {
    public static void main(String[] args) {
        try {
          
            BibliotecaGCImpl impl = new BibliotecaGCImpl();

            // Crear o reutilizar el registro RMI en el puerto 3000
            try {
                LocateRegistry.createRegistry(3000);
                System.out.println("RMI registry creado en 3000");
            } catch (Exception ex) {
                System.out.println("RMI registry posiblemente ya existía: " + ex.getMessage());
            }

            // Registrar el servicio remoto bajo el nombre "BibliotecaGCService"
            Registry registry = LocateRegistry.getRegistry(3000);
            registry.rebind("BibliotecaGCService", impl);
            System.out.println("BibliotecaGCService listo.");

            // El servidor queda activo escuchando solicitudes de clientes y actores
            System.out.println("ServidorGC corriendo. Pulse Ctrl+C para terminar.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
