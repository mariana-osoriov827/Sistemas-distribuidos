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
    
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Uso: java ActorClient_ZMQ <gcHost:pubPort> <gaHost:gaPort> <topic>");
            System.out.println("Ejemplo: java ActorClient_ZMQ localhost:5555 localhost:5560 DEVOLUCION");
            System.exit(1);
        }
        
        String[] gcParts = args[0].split(":");
        String gcHost = gcParts[0];
        int gcPubPort = Integer.parseInt(gcParts[1]);
        
        String[] gaParts = args[1].split(":");
        String gaHost = gaParts[0];
        int gaPort = Integer.parseInt(gaParts[1]);
        
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
            System.out.println("Actor conectado al GA en " + gaHost + ":" + gaPort);
            
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
                
                // Procesar operación en el GA
                boolean ok = false;
                try (Socket gaSocket = new Socket(gaHost, gaPort);
                     PrintWriter out = new PrintWriter(gaSocket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(gaSocket.getInputStream()))) {
                    
                    String request;
                    if ("DEVOLUCION".equals(tipo)) {
                        request = "DEVOLUCION|" + codigoLibro + "|" + usuarioId;
                    } else { // RENOVACION
                        request = "RENOVACION|" + codigoLibro + "|" + usuarioId + "|" + nuevaFecha;
                    }
                    
                    out.println(request);
                    String response = in.readLine();
                    ok = response != null && response.startsWith("OK");
                    
                    System.out.println("Actor: operación " + tipo + " -> " + (ok ? "ÉXITO" : "FALLÓ"));
                    
                } catch (Exception e) {
                    System.err.println("Error conectando al GA: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
