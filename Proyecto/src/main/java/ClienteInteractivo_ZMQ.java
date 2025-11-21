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
                
                if (infoResponse.startsWith("ERROR")) {
                    System.out.println("\n[ERROR] " + infoResponse);
                    continue;
                }
                
                // Mostrar información del libro
                System.out.println("\n" + infoResponse);
                System.out.print("¿Desea continuar con esta operación? (s/n): ");
                String confirmacion = scanner.nextLine().trim().toLowerCase();
                
                if (!confirmacion.equals("s") && !confirmacion.equals("si")) {
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
                
                // Si hay error, mostrarlo y no continuar
                if (response.startsWith("ERROR")) {
                    System.out.println("\n" + formatResponse(response));
                    continue;
                }
                
                System.out.println("\n" + formatResponse(response));
                
                // Si es operación asíncrona (DEVOLUCION o RENOVACION), esperar y consultar estado
                if ((opcion.equals("2") || opcion.equals("3")) && response.contains("Aceptado|")) {
                    String[] responseParts = response.split("\\|");
                    if (responseParts.length >= 3) {
                        String messageId = responseParts[2];
                        System.out.println("\nEsperando resultado de la operación...");
                        
                        // Esperar 2 segundos para que el actor procese
                        Thread.sleep(2000);
                        
                        // Consultar estado
                        requester.send("STATUS|" + messageId);
                        String statusResponse = requester.recvStr();
                        
                        if (statusResponse.startsWith("STATUS|")) {
                            String status = statusResponse.substring(7);
                            if ("SUCCESS".equals(status)) {
                                System.out.println("[ÉXITO] La operación se completó exitosamente");
                            } else if ("FAILED".equals(status)) {
                                System.out.println("[FALLÓ] La operación no se pudo completar");
                            } else {
                                System.out.println("[INFO] Estado: " + status);
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static String formatResponse(String response) {
        if (response.startsWith("OK")) {
            return "[OK] " + response.substring(3);
        } else if (response.startsWith("ERROR") || response.startsWith("FAILED")) {
            return "[ERROR] " + response;
        } else {
            return "[INFO] " + response;
        }
    }
}