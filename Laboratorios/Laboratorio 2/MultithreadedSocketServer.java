/**************************************************************************************
* Fecha: 22/08/2025
* Autor: Mariana Osorio Vasquez, Gabriel Jaramillo, Roberth Méndez, Juan Esteban Vera
* Tema: 
* - Multi-threaded Client/Server in Java
* Descripción:
* - En este proyecto de programación, el código crea un sistema cliente/servidor multihilo en Java. 
* - El servidor escucha en el puerto 8888 y, por cada cliente que se conecta, crea un nuevo hilo para manejar la comunicación.
* - El hilo del cliente (ServerClientThread) se encarga de recibir un número del cliente, calcular su cuadrado y enviar el 
*   resultado de vuelta. Este enfoque permite que múltiples clientes se conecten y sean atendidos simultáneamente sin que las 
*   solicitudes se bloqueen entre sí.
***************************************************************************************/

import java.net.*;
import java.io.*;

public class MultithreadedSocketServer {
  public static void main(String[] args) throws Exception {
    try {
      ServerSocket server = new ServerSocket(8888); // Crea un objeto ServerSocket que recibe conexiones del puerto 888
      int counter = 0;
      System.out.println("Server Started ....");

      // Bucle infinito en el que el servidor acepta nuevas conexiones
      while (true) {
        counter++;
        Socket serverClient = server.accept(); // Bloquear la ejecución hasta que un cliente se conecta
        System.out.println(" >> " + "Client No:" + counter + " started!");
        ServerClientThread sct = new ServerClientThread(serverClient, counter);
        sct.start(); // Inicia el hilo
      }
    } catch (Exception e) {
      System.out.println(e); // Manejo de excepciones
    }
  }
}

class ServerClientThread extends Thread {
  Socket serverClient;
  int clientNo;
  int squre;

  // Constructor que recibe el socket del cliente y su número para la comunicación
  ServerClientThread(Socket inSocket, int counter) {
    serverClient = inSocket;
    clientNo = counter;
  }

  // Contiene el código que se ejecutará en el hilo separado
  public void run() {
    try {
      // Configura los flujos de entrada y salida para la comunicación con el cliente
      DataInputStream inStream = new DataInputStream(serverClient.getInputStream());
      DataOutputStream outStream = new DataOutputStream(serverClient.getOutputStream());
      String clientMessage = "", serverMessage = "";

      // Bucle que procesa los mensajes del cliente
      while (!clientMessage.equals("bye")) {
        clientMessage = inStream.readUTF(); // Lee el mensaje del cliente
        System.out.println("From Client-" + clientNo + ": Number is :" + clientMessage);
        squre = Integer.parseInt(clientMessage) * Integer.parseInt(clientMessage); // Calcula el cuadrado del número recibido.
        serverMessage = "From Server to Client-" + clientNo + " Square of " + clientMessage + " is " + squre; // Prepara el mensaje de respuesta para el cliente
        outStream.writeUTF(serverMessage); // Envía la respuesta al cliente
        outStream.flush(); // Asegura que los datos se envíen inmediatamente
      }
      
      // Cierra cuando el cliente se desconecta
      inStream.close();
      outStream.close();
      serverClient.close();
    } catch (Exception ex) {
      System.out.println(ex);
    } finally {
      System.out.println("Client -" + clientNo + " exit!! "); // Asegura mensaje de desconexión
    }
  }
}