import Gestor_carga.*;

/**************************************************************************************
 * Fecha: 17/11/2025
 * Autor: Gabriel Jaramillo, Roberth Méndez, Mariana Osorio Vasquez, Juan Esteban Vera
 * Tema: Proyecto préstamo de libros (Sistema Distribuido)
 * Descripción:
 * - Cliente interactivo mejorado
 * - Genera ID de usuario automáticamente
 * - Solicita confirmación mostrando el nombre del libro
 * - Usa REQ/REP para todas las operaciones
 ***************************************************************************************/
import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import java.io.*;
import java.util.Scanner;
import java.util.UUID;

public class ClienteInteractivo_ZMQ {
    
    private static String userId;
    private static Scanner scanner = new Scanner(System.in);
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java ClienteInteractivo_ZMQ <gcHost:repPort>");
            System.out.println("Ejemplo: java ClienteInteractivo_ZMQ localhost:5556");
            System.exit(1);
        }
        
        String[] gcParts = args[0].split(":");
        String gcHost = gcParts[0];
        int gcRepPort = Integer.parseInt(gcParts[1]);
        
        // Generar ID de usuario único
        userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
        
        System.out.println("======================================");
        System.out.println("  SISTEMA DE PRÉSTAMO DE LIBROS      ");
        System.out.println("======================================");
        System.out.println("Usuario: " + userId);
        System.out.println("Conectado a: " + gcHost + ":" + gcRepPort);
        System.out.println("======================================\n");
        
        try (ZContext context = new ZContext()) {
            ZMQ.Socket requester = context.createSocket(ZMQ.REQ);
            requester.connect("tcp://" + gcHost + ":" + gcRepPort);
            
            while (true) {
                System.out.println("\n¿Qué operación desea realizar?");
                System.out.println("  1) Préstamo");
                System.out.println("  2) Devolución");
                System.out.println("  3) Renovación");
                System.out.println("  4) Salir");
                System.out.print("\nSeleccione opción: ");
                
                String opcion = scanner.nextLine().trim();
                
                if (opcion.equals("4")) {
                    System.out.println("\n¡Hasta luego!");
                    break;
                }
                
                System.out.print("Ingrese código del libro: ");
                String codigoLibro = scanner.nextLine().trim();
                
                // Solicitar información del libro antes de confirmar
                String infoRequest = "INFO|" + codigoLibro;
                requester.send(infoRequest);
                String infoResponse = requester.recvStr();
                if (infoResponse.startsWith("ERROR") || infoResponse.contains("FAILED|Libro no encontrado")) {
                    System.out.println("\n[ERROR] " + infoResponse);
                    continue;
                }
                // Mostrar información del libro
                System.out.println("\n" + infoResponse);
                System.out.print("¿Desea continuar con esta operación? (s/n): ");
                String confirmarOperacion = scanner.nextLine().trim().toLowerCase();
                if (!confirmarOperacion.equals("s") && !confirmarOperacion.equals("si")) {
                    System.out.println("Operación cancelada.");
                    // Consumir respuesta pendiente
                    requester.send("CANCEL");
                    requester.recvStr();
                    continue;
                }
                
                // Enviar operación real
                String operacion = "";
                switch (opcion) {
                    case "1":
                        operacion = "PRESTAMO|" + codigoLibro + "|" + userId;
                        break;
                    case "2":
                        operacion = "DEVOLUCION|" + codigoLibro + "|" + userId;
                        break;
                    case "3":
                        operacion = "RENOVACION|" + codigoLibro + "|" + userId;
                        break;
                    default:
                        System.out.println("[ERROR] Opción inválida");
                        // Consumir respuesta pendiente
                        requester.send("CANCEL");
                        requester.recvStr();
                        continue;
                }
                
                requester.send(operacion);
                String response = requester.recvStr();
                System.out.println("\n" + formatResponse(response));
                // Aquí puedes agregar lógica para esperar y consultar estado si es necesario
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static String formatResponse(String response) {
        if (response.startsWith("OK|")) {
            // Si la respuesta tiene más de un campo, solo mostrar el mensaje principal
            String[] parts = response.split("\\|", 3);
            if (parts.length >= 2) {
                // Si hay un tercer campo (como UUID), lo ignoramos
                return "[OK] " + parts[1];
            } else {
                return "[OK] " + response.substring(3);
            }
        } else if (response.startsWith("ERROR") || response.startsWith("FAILED")) {
            return "[ERROR] " + response.replaceFirst("(ERROR|FAILED)\\|", "");
        } else {
            return "[INFO] " + response;
        }
    }
}
