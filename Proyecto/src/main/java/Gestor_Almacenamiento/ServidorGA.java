/**************************************************************************************
* Fecha: 10/10/2025
* Autor: Gabriel Jaramillo, Roberth Méndez, Mariana Osorio Vasquez, Juan Esteban Vera
* Tema: 
* - Proyecto préstamo de libros (Sistema Distribuido)
* Descripción:
* - Clase Servidor Principal (ServidorGA):
* - Programa de arranque para el Gestor de Almacenamiento (GA) en una sede.
* - Inicializa la BaseDatos, cargándola desde libros.txt.
* - Expone la implementación del GA (`GestorAlmacenamientoImpl`) como un servicio RMI.
* - Determina su rol (Primary o Replica/Follower) basado en argumentos de línea de comandos 
* y registra el servicio RMI con el nombre correspondiente (GestorAlmacenamientoPrimary o GestorAlmacenamientoReplica).
* - Incluye un Shutdown Hook para asegurar la persistencia de los datos en el archivo 
* al cerrar el servidor.
***************************************************************************************/
package Gestor_Almacenamiento;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServidorGA {
    public static void main(String[] args) {

        // Se define el rol del servidor (primary o replica) según los argumentos recibidos.
        String role = (args.length > 0) ? args[0] : "primary";
        String archivoBD = "libros.txt";

        try {
            BaseDatos bd = new BaseDatos();
            try {
                bd.cargarDesdeArchivo(archivoBD);
            } catch (Exception e) {
                // Silenciado: no mostrar mensaje de error de carga
            }
            GestorAlmacenamientoImpl impl = new GestorAlmacenamientoImpl(bd, role);
            try {
                LocateRegistry.createRegistry(1099);
            } catch (Exception ex) {
                // Silenciado: no mostrar mensaje de RMI registry
            }
            Registry registry = LocateRegistry.getRegistry(1099);
            if ("primary".equalsIgnoreCase(role)) {
                registry.rebind("GestorAlmacenamientoPrimary", impl);
                System.out.println("GestorAlmacenamientoPrimary listo.");
            } else {
                registry.rebind("GestorAlmacenamientoReplica", impl);
                System.out.println("GestorAlmacenamientoReplica listo.");
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    bd.guardarEnArchivo(archivoBD);
                } catch (Exception e) {
                    // Silenciado
                }
            }));
            // Solo mensaje de inicio exitoso
        } catch (Exception e) {
            // Silenciado
        }
    }
}
