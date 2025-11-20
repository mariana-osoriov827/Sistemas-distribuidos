/**************************************************************************************
 * Fecha: 17/11/2025
 * Autor: Gabriel Jaramillo, Roberth Méndez, Mariana Osorio Vasquez, Juan Esteban Vera
 * Tema: Proyecto préstamo de libros (Sistema Distribuido)
 * Descripción:
 * - Actor usando ZeroMQ SUB para suscribirse a tópicos (Devolución/Renovación)
 * - Se conecta al GC mediante patrón Pub/Sub
 * - Actualiza la BD mediante conexión directa al GA
 ***************************************************************************************/
package Gestor_carga;

import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import java.io.*;
import java.net.*;

public class ActorClient_ZMQ {
    
    private static String[] gaHosts;
    private static int[] gaPorts;
    private static int currentGaIndex = 0;
    
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Uso: java ActorClient_ZMQ <gcHost:pubPort> <gaHost1:port1[,gaHost2:port2]> <topic>");
            System.out.println("Ejemplo: java ActorClient_ZMQ localhost:5555 localhost:5560,10.43.102.177:6560 DEVOLUCION");
            System.exit(1);
        }
        
        String[] gcParts = args[0].split(":");
        String gcHost = gcParts[0];
        int gcPubPort = Integer.parseInt(gcParts[1]);
        
        // Parsear múltiples GAs (primario y backups)
        String[] gaList = args[1].split(",");
        gaHosts = new String[gaList.length];
        gaPorts = new int[gaList.length];
        
        for (int i = 0; i < gaList.length; i++) {
            String[] parts = gaList[i].split(":");
            gaHosts[i] = parts[0];
            gaPorts[i] = Integer.parseInt(parts[1]);
        }
        
        System.out.println("Actor configurado con " + gaList.length + " GA(s):");
        for (int i = 0; i < gaHosts.length; i++) {
            System.out.println("  GA" + (i+1) + ": " + gaHosts[i] + ":" + gaPorts[i]);
        }
        
        String topic = args[2].toUpperCase();
        
        if (!topic.equals("DEVOLUCION") && !topic.equals("RENOVACION")) {
            System.out.println("Error: tópico debe ser DEVOLUCION o RENOVACION");
            System.exit(1);
        }
        
        try (ZContext context = new ZContext()) {
            
            // Socket SUB para recibir mensajes del GC
            ZMQ.Socket subscriber = context.createSocket(ZMQ.SUB);
            subscriber.connect("tcp://" + gcHost + ":" + gcPubPort);
            subscriber.subscribe(topic.getBytes());
            System.out.println("Actor suscrito a tópico " + topic + " en " + gcHost + ":" + gcPubPort);
            System.out.println("Actor conectará al GA activo: " + gaHosts[currentGaIndex] + ":" + gaPorts[currentGaIndex]);
            
            // Socket PUSH para reportar resultados al GC (puerto PUB + 2 = REP + 1)
            // Sede 1: 5555 (PUB) + 2 = 5557 = 5556 (REP) + 1
            ZMQ.Socket resultPusher = context.createSocket(ZMQ.PUSH);
            resultPusher.connect("tcp://" + gcHost + ":" + (gcPubPort + 2));
            System.out.println("Actor reportará resultados en puerto " + (gcPubPort + 2));
            
            while (!Thread.currentThread().isInterrupted()) {
                // Recibir mensaje del GC
                String mensaje = subscriber.recvStr();
                System.out.println(java.time.LocalDateTime.now() + " - Actor recibió: " + mensaje);
                // Formato: TIPO|id|codigoLibro|usuarioId|fecha|nuevaFecha
                String[] parts = mensaje.split("\\|");
                if (parts.length < 5) continue;
                String tipo = parts[0];
                String messageId = parts[1];
                String codigoLibro = parts[2];
                String usuarioId = parts[3];
                String nuevaFecha = parts.length > 5 ? parts[5] : null;
                // Procesar operación en el GA y obtener respuesta completa
                String response = enviarOperacionGAConRespuesta(tipo, codigoLibro, usuarioId, nuevaFecha);
                boolean operacionExitosa = response != null && response.startsWith("OK");
                String resultMsg;
                if (operacionExitosa) {
                    resultMsg = "RESULT|" + messageId + "|OK||" + tipo;
                } else {
                    String errorMsg = "Error desconocido";
                    if (response != null) {
                        if (response.startsWith("FAILED|")) {
                            errorMsg = response.substring(7);
                        } else if (response.startsWith("ERROR|")) {
                            errorMsg = response.substring(6);
                        } else {
                            errorMsg = response;
                        }
                    }
                    resultMsg = "RESULT|" + messageId + "|FAILED|" + errorMsg + "|" + tipo;
                }
                resultPusher.send(resultMsg);
                System.out.println("Actor reportó: " + resultMsg);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Envía operación al GA con failover automático y retorna la respuesta textual (OK|..., FAILED|..., ERROR|...)
     */
    private static String enviarOperacionGAConRespuesta(String tipo, String codigoLibro, String usuarioId, String nuevaFecha) {
        int intentos = 0;
        int maxIntentos = gaHosts.length * 2;
        while (intentos < maxIntentos) {
            String gaHost = gaHosts[currentGaIndex];
            int gaPort = gaPorts[currentGaIndex];
            try (Socket gaSocket = new Socket()) {
                gaSocket.connect(new InetSocketAddress(gaHost, gaPort), 2000);
                gaSocket.setSoTimeout(3000);
                PrintWriter out = new PrintWriter(gaSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(gaSocket.getInputStream()));
                String request;
                if ("DEVOLUCION".equals(tipo)) {
                    request = "DEVOLUCION|" + codigoLibro + "|" + usuarioId;
                } else {
                    request = "RENOVACION|" + codigoLibro + "|" + usuarioId + "|" + nuevaFecha;
                }
                out.println(request);
                String response = in.readLine();
                return response;
            } catch (IOException e) {
                System.err.println("[FAILOVER] GA " + gaHost + ":" + gaPort + " no disponible: " + e.getMessage());
                currentGaIndex = (currentGaIndex + 1) % gaHosts.length;
                intentos++;
                if (intentos < maxIntentos) {
                    System.out.println("[FAILOVER] Intentando con GA " + gaHosts[currentGaIndex] + ":" + gaPorts[currentGaIndex]);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        System.err.println("[FAILOVER] Todos los GAs no disponibles después de " + maxIntentos + " intentos");
        return "FAILED|Todos los GAs no disponibles";
    }

}
