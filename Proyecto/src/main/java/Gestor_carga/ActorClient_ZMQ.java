package Gestor_carga;

import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import org.zeromq.ZMQException;

import java.util.concurrent.atomic.AtomicInteger;

import org.zeromq.ZMQ.Socket;

/**
 * Actor genérico para manejar DEVOLUCION y RENOVACION.
 * - Patrón ZMQ: SUB para recibir peticiones del GC.
 * - Patrón ZMQ: REQ para enviar peticiones al GA (con failover).
 * - Patrón ZMQ: PUSH para enviar el resultado de vuelta al GC.
 */
public class ActorClient_ZMQ {

    private final String gcPubAddress;
    private final String operationType;
    private final String[] gaHosts;
    private final int[] gaPorts;
    private final String gcResultIp;
    private final AtomicInteger currentGaIndex = new AtomicInteger(0); 

    public ActorClient_ZMQ(String gcPubAddress, String gaList, String operationType, String gcResultIp) {
        this.gcPubAddress = gcPubAddress;
        this.operationType = operationType;
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
        System.out.println("Actor configurado con " + gaServers.length + " GA(s):");
        for (int i = 0; i < gaServers.length; i++) {
            System.out.println("  GA" + (i + 1) + ": " + gaServers[i]);
        }
        System.out.println("Actor reportará resultados en tcp://" + gcResultIp + ":5557");
    }

    public void iniciar() {
        try (ZContext context = new ZContext()) {
            // 1. Socket SUB para recibir peticiones del GC
            ZMQ.Socket subscriber = context.createSocket(ZMQ.SUB);
            subscriber.connect("tcp://" + gcPubAddress);
            subscriber.subscribe(operationType.getBytes(ZMQ.CHARSET));
            System.out.println("Actor suscrito a tópico " + operationType + " en " + gcPubAddress);

            // 2. Socket PUSH para enviar resultados de vuelta al GC (Puerto 5557)
            ZMQ.Socket pusher = context.createSocket(ZMQ.PUSH);
            pusher.connect("tcp://" + gcResultIp + ":5557");
            System.out.println("Actor reportará resultados en tcp://" + gcResultIp + ":5557");

            // 3. Socket REQ para comunicarse con los GAs (Failover)
            try (ZMQ.Socket requester = context.createSocket(ZMQ.REQ)) {
                for (int i = 0; i < gaHosts.length; i++) {
                    requester.connect("tcp://" + gaHosts[i] + ":" + gaPorts[i]);
                }
                System.out.println("Actor conectado a " + gaHosts.length + " GAs.");
                while (!Thread.currentThread().isInterrupted()) {
                    // Recibir mensaje completo (topic y cuerpo)
                    String receivedTopic = subscriber.recvStr(); 
                    String request = subscriber.recvStr();
                    if (request == null) continue;
                    System.out.println("Actor " + operationType + " recibió: " + request);
                    // Formato esperado: TOPIC|codigoLibro|userId|messageId
                    String[] parts = request.split("\\|", 4);
                    if (parts.length < 4) {
                        System.err.println("Mensaje incompleto: " + request);
                        continue;
                    }
                    String codigoLibro = parts[1];
                    String userId = parts[2];
                    String messageId = parts[3];
                    // --- 1. Preparar y enviar petición al GA (REQ/REP con Failover) ---
                    String gaRequest = operationType + "|" + codigoLibro + "|" + userId;
                    String gaResponse = sendAndReceiveGA(requester, gaRequest);
                    // --- 2. Procesar respuesta y enviar al GC (PUSH) ---
                    String finalResult = receivedTopic + "|" + messageId + "|" + gaResponse;
                    pusher.send(finalResult);
                    System.out.println("Resultado enviado al GC: " + finalResult);
                }
            } catch (ZMQException e) {
                System.err.println("Error ZMQ en el Actor " + operationType + " (Requester): " + e.getMessage());
            }
        } catch (ZMQException e) {
            System.err.println("Error ZMQ en el Actor " + operationType + ": " + e.getMessage());
        }
    }

    private String sendAndReceiveGA(ZMQ.Socket requester, String request) {
        int intentos = 0;
        int maxIntentos = gaHosts.length * 2;
        final int gaTimeoutMs = 3000;
        while (intentos < maxIntentos) {

            int index = currentGaIndex.get();
            try {
                // Enviar petición por REQ socket
                requester.send(request.getBytes(ZMQ.CHARSET));
                byte[] replyBytes = requester.recv(gaTimeoutMs);

                if (replyBytes != null) {
                    String reply = new String(replyBytes, ZMQ.CHARSET);
                    System.out.println("[INFO Actor " + operationType + "] Éxito con GA " + gaHosts[index] + ":" + gaPorts[index]);
                    // Mover al siguiente GA para la próxima vez (simple round-robin)
                    currentGaIndex.set((index + 1) % gaHosts.length);
                    return reply;
                } else {
                    System.err.println("[WARN Actor " + operationType + "] Sin respuesta de GA " + gaHosts[index] + ":" + gaPorts[index]);
                    // Rotar al siguiente GA y reintentar
                    currentGaIndex.set((index + 1) % gaHosts.length);
                }
            } catch (ZMQException e) {
                System.err.println("[ERROR Actor " + operationType + "] Error ZMQ con GA " + gaHosts[index] + ":" + gaPorts[index] + " - " + e.getMessage());
                // Rotar y contar intento como fallido
                currentGaIndex.set((index + 1) % gaHosts.length);
            }
            intentos++;
        }
        return "ERROR|Timeout de comunicación con GA (después de " + maxIntentos + " intentos)";
    }
    
    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println("Uso: java ActorClient_ZMQ <gcHost:pubPort> <gaHost1:port1[,gaHost2:port2]> <topic> <gcResultIp>");
            System.out.println("Ejemplo: java ActorClient_ZMQ localhost:5555 localhost:5560,10.43.102.177:6560 DEVOLUCION 10.43.103.49");
            System.exit(1);
        }
        try {
            String gcPubAddress = args[0];
            String gaList = args[1];
            String operationType = args[2].toUpperCase();
            String gcResultIp = args[3];
            if (!operationType.equals("DEVOLUCION") && !operationType.equals("RENOVACION")) {
                System.out.println("Error: tópico debe ser DEVOLUCION o RENOVACION");
                System.exit(1);
            }
            ActorClient_ZMQ actor = new ActorClient_ZMQ(gcPubAddress, gaList, operationType, gcResultIp);
            actor.iniciar();
        } catch (Exception e) {
            System.err.println("Error fatal en Actor Client: " + e.getMessage());
            e.printStackTrace(); // Puede comentarse si se desea evitar el warning de lint
        }
    }
}