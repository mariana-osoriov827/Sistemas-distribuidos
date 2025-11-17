/**************************************************************************************
 * Fecha: 10/10/2025
 * Autor: Gabriel Jaramillo, Roberth Méndez, Mariana Osorio Vasquez, Juan Esteban Vera
 * Tema:
 * - Proyecto préstamo de libros (Sistema Distribuido)
 * Descripción:
 * - Actor especializado (DEVOLUCION / RENOVACION / PRESTAMO).
 * - Consume mensajes del Gestor de Carga (GC) mediante polling RMI y aplica operaciones
 *   en el Gestor de Almacenamiento (GA).
 ***************************************************************************************/
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * 
 * 1. Se conecta al GC (puerto 3000) para recibir mensajes.
 * 2. Intenta conectarse al GA primario; si falla, conecta con la réplica.
 * 3. Va pidiendo mensajes del tópico correspondiente, cuando recibe un mensaje del GC 
 *    ejecuta la operación en el GA y notifica el resultado al GC con un acknowledge.
 *
 */
public class ActorClient {
    public static void main(String[] args) {

        if (args.length < 3) {
            System.out.println("Uso: java ActorClient <hostGC> <hostGA> <topic>");
            System.exit(1);
        }

        String hostGC = args[0];
        String hostGA = args[1];
        String topic = args[2].toUpperCase();

        // Verifica si el topico de creación del actor es válido
        if (!topic.equals("DEVOLUCION") && !topic.equals("RENOVACION") && !topic.equals("PRESTAMO")) {
            System.out.println("Error: tipo de operación no reconocido. Solo se permiten DEVOLUCION, RENOVACION o PRESTAMO.");
            System.exit(1);
        }

        try {
            // Conexión al Gestor de Carga (GC)
            Registry regGc = LocateRegistry.getRegistry(hostGC, 3000);
            BibliotecaGC stubGc = (BibliotecaGC) regGc.lookup("BibliotecaGCService");

            // Conexión al Gestor de Almacenamiento (GA): preferir primario, si falla usar réplica
            GestorAlmacenamiento stubGa = null;
            try {
                Registry regGa = LocateRegistry.getRegistry(hostGA, 1099);
                stubGa = (GestorAlmacenamiento) regGa.lookup("GestorAlmacenamientoPrimary");
                System.out.println("Actor conectado con GestorAlmacenamientoPrimary");
            } catch (Exception ex) {
                Registry regGaReplica = LocateRegistry.getRegistry(hostGA, 1099);
                stubGa = (GestorAlmacenamiento) regGaReplica.lookup("GestorAlmacenamientoReplica");
                System.out.println("Actor conectado con GestorAlmacenamientoReplica");
            }

            System.out.println("ActorClient escuchando el tema " + topic);

            // El actor mantiene escuchando si el Gestor de Carga (GC) tiene un nuevo mensaje disponible para procesar.
            while (true) {

                Message m = stubGc.fetchNextMessage(topic);

                if (m != null) {
                    System.out.println(java.time.LocalDateTime.now() + " - Actor recibió: " + m);
                    boolean ok = false;

                    // Ejecutar la operación correspondiente en el GA
                    if (topic.equals("DEVOLUCION")) {
                        ok = stubGa.aplicarDevolucionEnBD(m.getCodigoLibro(), m.getUsuarioId());
                    } else if (topic.equals("RENOVACION")) {
                        ok = stubGa.aplicarRenovacionEnBD(m.getCodigoLibro(), m.getUsuarioId(), m.getNuevaFechaEntrega());
                    } else if (topic.equals("PRESTAMO")) {
                        ok = stubGa.aplicarPrestamoEnBD(m.getCodigoLibro(), m.getUsuarioId());
                    }

                    // Notificar resultado al GC (Operación fallida o correctamente procesada)
                    if (ok) {
                        stubGc.ackMessage(m.getId(), true);
                        System.out.println("Actor: operación procesada correctamente, ACK enviado al GC");
                    } else {
                        stubGc.ackMessage(m.getId(), false);
                        System.out.println("Actor: operación falló, ACK de error enviado al GC");
                    }
                }

                Thread.sleep(1000);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}