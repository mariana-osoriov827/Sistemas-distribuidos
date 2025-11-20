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
package Gestor_carga;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServidorGC {
    public static void main(String[] args) {
        try {
            BibliotecaGCImpl impl = new BibliotecaGCImpl();
            try {
                LocateRegistry.createRegistry(3000);
            } catch (Exception ex) {
                // Silenciado: no mostrar mensaje de RMI registry
            }
            Registry registry = LocateRegistry.getRegistry(3000);
            registry.rebind("BibliotecaGCService", impl);
            System.out.println("BibliotecaGCService listo.");
            // Solo mensaje de inicio exitoso
        } catch (Exception e) {
            // Silenciado
        }
    }
}
