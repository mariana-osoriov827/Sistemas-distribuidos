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
    private static String[] gaHosts;
    private static int[] gaPorts;
    private static int currentGaIndex = 0;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java ActorPrestamo_ZMQ <gcHost:pubPort> <gaHost1:gaPort1[,gaHost2:gaPort2,...]>");
            System.out.println("Ejemplo: java ActorPrestamo_ZMQ localhost:5555 localhost:5560,10.43.102.177:6560");
            System.exit(1);
        }

        String[] gcParts = args[0].split(":");
        String gcHost = gcParts[0];
        int gcPubPort = Integer.parseInt(gcParts[1]);

        // Parsear lista de GAs
        String[] gaServers = args[1].split(",");
        gaHosts = new String[gaServers.length];
        gaPorts = new int[gaServers.length];
        for (int i = 0; i < gaServers.length; i++) {
            String[] parts = gaServers[i].trim().split(":");
            gaHosts[i] = parts[0];
            gaPorts[i] = Integer.parseInt(parts[1]);
        }

        try (ZContext context = new ZContext()) {
            ZMQ.Socket subscriber = context.createSocket(ZMQ.SUB);
            subscriber.connect("tcp://" + gcHost + ":" + gcPubPort);
            subscriber.subscribe("PRESTAMO".getBytes());
            System.out.println("ActorPrestamo suscrito a tópico PRESTAMO en " + gcHost + ":" + gcPubPort);
            System.out.print("ActorPrestamo configurado con GAs: ");
            for (int i = 0; i < gaHosts.length; i++) {
                System.out.print(gaHosts[i] + ":" + gaPorts[i]);
                if (i < gaHosts.length - 1) System.out.print(", ");
            }
            System.out.println();

            while (!Thread.currentThread().isInterrupted()) {
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

                boolean prestamoConcedido = false;
                String respuestaCompleta = "";
                int intentos = 0;
                int maxIntentos = gaHosts.length * 2;
                while (intentos < maxIntentos) {
                    String gaHost = gaHosts[currentGaIndex];
                    int gaPort = gaPorts[currentGaIndex];
                    try (Socket gaSocket = new Socket(gaHost, gaPort);
                         PrintWriter out = new PrintWriter(new OutputStreamWriter(gaSocket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true);
                         BufferedReader in = new BufferedReader(new InputStreamReader(gaSocket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                        String request = "PRESTAMO|" + codigoLibro + "|" + usuarioId;
                        out.println(request);
                        String response = in.readLine();
                        prestamoConcedido = response != null && response.startsWith("OK");
                        respuestaCompleta = response;
                        if (prestamoConcedido) {
                            System.out.println("[OK] ActorPrestamo: PRÉSTAMO OTORGADO para libro " + codigoLibro + " en GA " + gaHost + ":" + gaPort);
                            break;
                        } else {
                            System.out.println("[FAIL] ActorPrestamo: PRÉSTAMO DENEGADO para libro " + codigoLibro + " en GA " + gaHost + ":" + gaPort + " - " + respuestaCompleta);
                        }
                    } catch (Exception e) {
                        System.err.println("Error conectando al GA " + gaHost + ":" + gaPort + ": " + e.getMessage());
                    }
                    currentGaIndex = (currentGaIndex + 1) % gaHosts.length;
                    intentos++;
                }
                if (!prestamoConcedido) {
                    System.out.println("[ERROR] ActorPrestamo: PRÉSTAMO FALLÓ en todos los GAs");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}