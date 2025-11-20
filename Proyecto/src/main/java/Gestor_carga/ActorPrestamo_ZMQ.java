/**************************************************************************************
 * Fecha: 17/11/2025
 * Autor: Gabriel Jaramillo, Roberth Méndez, Mariana Osorio Vasquez, Juan Esteban Vera
 * Tema: Proyecto préstamo de libros (Sistema Distribuido)
 * Descripción:
 * - Actor especializado para PRESTAMO usando ZeroMQ
 * - Se suscribe al tópico PRESTAMO del GC (patrón PUB/SUB)
 * - Procesa las solicitudes de forma SÍNCRONA con el GA
 * - Garantiza que el préstamo se valida y registra antes de confirmar al PS
 ***************************************************************************************/
package Gestor_carga;

import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import java.io.*;
import java.net.*;

public class ActorPrestamo_ZMQ {
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java ActorPrestamo_ZMQ <gcHost:pubPort> <gaHost:gaPort>");
            System.out.println("Ejemplo: java ActorPrestamo_ZMQ localhost:5555 localhost:5560");
            System.exit(1);
        }
        
        String[] gcParts = args[0].split(":");
        String gcHost = gcParts[0];
        int gcPubPort = Integer.parseInt(gcParts[1]);
        
        String[] gaParts = args[1].split(":");
        String gaHost = gaParts[0];
        int gaPort = Integer.parseInt(gaParts[1]);
        
        try (ZContext context = new ZContext()) {
            // Socket SUB para recibir mensajes de PRESTAMO del GC
            ZMQ.Socket subscriber = context.createSocket(ZMQ.SUB);
            subscriber.connect("tcp://" + gcHost + ":" + gcPubPort);
            subscriber.subscribe("PRESTAMO".getBytes());
            System.out.println("ActorPrestamo suscrito a tópico PRESTAMO en " + gcHost + ":" + gcPubPort);
            System.out.println("ActorPrestamo conectado al GA en " + gaHost + ":" + gaPort);

            // Socket PUSH para reportar resultados al GC (puerto PUB + 2 = REP + 1)
            int resultPort = gcPubPort + 2;
            ZMQ.Socket resultPusher = context.createSocket(ZMQ.PUSH);
            resultPusher.connect("tcp://" + gcHost + ":" + resultPort);
            System.out.println("ActorPrestamo reportará resultados en puerto " + resultPort);

            while (!Thread.currentThread().isInterrupted()) {
                // Recibir mensaje del GC
                String mensaje = subscriber.recvStr();
                System.out.println(java.time.LocalDateTime.now() + " - ActorPrestamo recibió: " + mensaje);
                // Formato: PRESTAMO|id|codigoLibro|usuarioId|fecha|null
                String[] parts = mensaje.split("\\|");
                if (parts.length < 5) {
                    System.err.println("Formato de mensaje inválido");
                    continue;
                }
                String tipo = parts[0];
                String messageId = parts[1];
                String codigoLibro = parts[2];
                String usuarioId = parts[3];

                // Procesar solicitud de préstamo en el GA de forma SÍNCRONA
                boolean prestamoConcedido = false;
                String respuestaCompleta = "";
                try (Socket gaSocket = new Socket(gaHost, gaPort);
                     PrintWriter out = new PrintWriter(gaSocket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(gaSocket.getInputStream()))) {
                    String request = "PRESTAMO|" + codigoLibro + "|" + usuarioId;
                    out.println(request);
                    String response = in.readLine();
                    prestamoConcedido = response != null && response.startsWith("OK");
                    respuestaCompleta = response;
                    if (prestamoConcedido) {
                        System.out.println("[OK] ActorPrestamo: PRÉSTAMO OTORGADO para libro " + codigoLibro);
                        // Reportar resultado OK al GC
                        String resultMsg = "RESULT|" + messageId + "|OK||PRESTAMO";
                        resultPusher.send(resultMsg);
                        System.out.println("ActorPrestamo reportó: " + resultMsg);
                    } else {
                        System.out.println("[FAIL] ActorPrestamo: PRÉSTAMO DENEGADO para libro " + codigoLibro + " - " + respuestaCompleta);
                        // Reportar resultado FAILED y mensaje de error real al GC
                        String errorMsg = "Error desconocido";
                        if (respuestaCompleta != null) {
                            if (respuestaCompleta.startsWith("FAILED|")) {
                                errorMsg = respuestaCompleta.substring(7); // Solo el mensaje, sin el prefijo
                            } else if (respuestaCompleta.startsWith("ERROR|")) {
                                errorMsg = respuestaCompleta.substring(6);
                            } else {
                                errorMsg = respuestaCompleta;
                            }
                        }
                        String resultMsg = "RESULT|" + messageId + "|FAILED|" + errorMsg + "|PRESTAMO";
                        resultPusher.send(resultMsg);
                        System.out.println("ActorPrestamo reportó: " + resultMsg);
                    }
                } catch (Exception e) {
                    System.err.println("Error conectando al GA: " + e.getMessage());
                    System.out.println("[ERROR] ActorPrestamo: PRÉSTAMO FALLÓ por error de conexión");
                    String resultMsg = "RESULT|" + messageId + "|FAILED|Error de conexión|PRESTAMO";
                    resultPusher.send(resultMsg);
                    System.out.println("ActorPrestamo reportó: " + resultMsg);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
