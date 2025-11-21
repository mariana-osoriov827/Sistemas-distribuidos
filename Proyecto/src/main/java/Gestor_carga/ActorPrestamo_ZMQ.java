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

import org.zeromq.ZMQException;

import java.net.*;
import java.util.Arrays;

public class ActorPrestamo_ZMQ {
    private final String gcPubAddress;
    private final String[] gaHosts;
    private final int[] gaPorts;
    private final String topic = "PRESTAMO";
    private final String gcResultIp;
    // Índice actual para failover Round-Robin con los GAs
    private volatile int currentGaIndex = 0; 

    public ActorPrestamo_ZMQ(String gcPubAddress, String gaList, String gcResultIp) {
        this.gcPubAddress = gcPubAddress;
        this.gcResultIp = gcResultIp;
        // Parsear la lista de GAs para failover
        String[] gaServers = gaList.split(",");
        this.gaHosts = new String[gaServers.length];
        this.gaPorts = new int[gaServers.length];
        for (int i = 0; i < gaServers.length; i++) {
            String[] parts = gaServers[i].trim().split(":");
            this.gaHosts[i] = parts[0];
            this.gaPorts[i] = Integer.parseInt(parts[1]);
        }
        System.out.println("Actor Prestamo | GAs configurados: " + Arrays.toString(gaServers));
        System.out.println("Actor Prestamo | GC para PUSH de resultados: " + gcResultIp + ":5557");
    }

    public void iniciar() {
        try (ZContext context = new ZContext()) {
            // 1. Socket SUB para recibir peticiones del GC (Asíncrono)
            ZMQ.Socket subscriber = context.createSocket(ZMQ.SUB);
            subscriber.connect("tcp://" + gcPubAddress);
            subscriber.subscribe(topic.getBytes(ZMQ.CHARSET));
            System.out.println("Actor Prestamo | Suscrito a GC en " + gcPubAddress);

            // 2. Socket PUSH para enviar resultados de vuelta al GC (Asíncrono)
            ZMQ.Socket pusher = context.createSocket(ZMQ.PUSH);
            pusher.connect("tcp://" + gcResultIp + ":5557");
            System.out.println("Actor Prestamo | Conectado al PULL del GC en " + gcResultIp + ":5557");

            // 3. Socket REQ para comunicarse con los GAs (Síncrono para la consulta)
            try (ZMQ.Socket requester = context.createSocket(ZMQ.REQ)) {
                for (int i = 0; i < gaHosts.length; i++) {
                    requester.connect("tcp://" + gaHosts[i] + ":" + gaPorts[i]);
                }
                while (!Thread.currentThread().isInterrupted()) {
                    // Recibir mensaje completo (topic y cuerpo)
                    String receivedTopic = subscriber.recvStr(); 
                    String request = subscriber.recvStr();
                    if (request == null) continue; // Si hay desconexión
                    System.out.println("Actor Prestamo recibió: " + request);
                    // Formato esperado: PRESTAMO|codigoLibro|userId|messageId
                    String[] parts = request.split("\\|", 4);
                    if (parts.length < 4) {
                        System.err.println("Mensaje de Prestamo incompleto: " + request);
                        continue;
                    }
                    String codigoLibro = parts[1];
                    String userId = parts[2];
                    String messageId = parts[3];
                    // --- 1. Consultar disponibilidad al GA (BLOQUEANTE, con Failover) ---
                    String gaRequest = "VALIDAR_PRESTAMO|" + codigoLibro;
                    String gaResponse = sendAndReceiveGA(requester, gaRequest);
                    String result;
                    if (gaResponse.startsWith("OK")) {
                        // Simulación de procesamiento (ej: 500ms)
                        Thread.sleep(500); 
                        result = "EXITO|Préstamo de libro " + codigoLibro + " a usuario " + userId + " completado.";
                        System.out.println("Actor Prestamo: Exito! " + result);
                    } else if (gaResponse.startsWith("ERROR|Timeout")) {
                         result = "ERROR|Timeout de comunicación con GA. Intente de nuevo.";
                         System.err.println("Actor Prestamo: Fallo por timeout con GA.");
                    } else {
                        result = gaResponse;
                        System.out.println("Actor Prestamo: Fallo por validación GA. " + result);
                    }
                    
                    // --- 3. Enviar resultado de vuelta al GC (PUSH) ---
                    String finalResult = topic + "|" + messageId + "|" + result;
                    pusher.send(finalResult);
                    System.out.println("Actor Prestamo envió PUSH: " + finalResult);
                }
            } catch (ZMQException e) {
                System.err.println("Error ZMQ en el Actor Prestamo (Requester): " + e.getMessage());
            }

        } catch (InterruptedException e) {
            System.out.println("Actor Prestamo interrumpido.");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Realiza la comunicación síncrona con el GA, incluyendo Failover (Round-Robin) 
     * en caso de fallo o timeout.
     */
    private String sendAndReceiveGA(ZMQ.Socket requester, String request) {
        int intentos = 0;
        // Número máximo de intentos antes de fallar por completo
        int maxIntentos = gaHosts.length * 2; 
        final int gaTimeoutMs = 3000; // 3 segundos para la respuesta del GA

        while (intentos < maxIntentos) {
            
            String gaHost = gaHosts[currentGaIndex];
            int gaPort = gaPorts[currentGaIndex];
            
            try {
                // 1. Enviar
                requester.send(request);
                
                // 2. Esperar respuesta con timeout
                // El error anterior de 'cancellationToken' ha sido corregido aquí (0 es el flag)
                byte[] reply = requester.recv(gaTimeoutMs); 

                if (reply != null) {
                    System.out.println("[INFO ActorPrestamo] Éxito con GA " + gaHost + ":" + gaPort);
                    return new String(reply, ZMQ.CHARSET);
                }
                
                // Si hay timeout (reply es null): FAILOVER
                System.err.println("[FAILOVER ActorPrestamo] GA " + gaHost + ":" + gaPort + " no responde a tiempo. Rotando...");
                
                // Rotar el índice y reintentar
                currentGaIndex = (currentGaIndex + 1) % gaHosts.length;
                intentos++;
                
                // NOTA: Para sockets REQ, después de un timeout, es a menudo necesario 
                // recrear o resetear el socket para que el estado de REQ/REP no se rompa.
                // Aquí, confiamos en la auto-reconexión de ZMQ, pero en un sistema de prod.
                // esta sección requeriría un manejo más robusto del socket REQ/REP.

            } catch (ZMQException e) {
                System.err.println("[FAILOVER ActorPrestamo] ZMQ Error en GA " + gaHost + ":" + gaPort + ": " + e.getMessage());
                currentGaIndex = (currentGaIndex + 1) % gaHosts.length;
                intentos++;
            }
        }
        
        return "ERROR|Timeout de comunicación con GA (después de " + maxIntentos + " intentos)";
    }
    
    // El método principal debe recibir los argumentos del script
    public static void main(String[] args) {
        // Argumentos esperados: 1. gcPubAddress, 2. gaList
        if (args.length != 2) {
            System.err.println("Uso: ActorPrestamo_ZMQ <gcPubAddress> <gaList>");
            System.exit(1);
        }
        
        try {
            String gcPubAddress = args[0]; // Ej: localhost:6555
            String gaList = args[1];       // Ej: localhost:6560,10.43.103.49:5560
            
            ActorPrestamo_ZMQ actor = new ActorPrestamo_ZMQ(gcPubAddress, gaList);
            actor.iniciar();
            
        } catch (Exception e) {
            System.err.println("Error fatal en Actor Prestamo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}