/**************************************************************************************
* Fecha: 22/08/2025
* Autor: Mariana Osorio Vasquez, Gabriel Jaramillo, Roberth Méndez, Juan Esteban Vera
* Tema: 
* 	- Multi-threaded Client/Server in Java
* Descripción:
*   - El cliente (TCPClient) simplemente se conecta al servidor, le envía un número que el 
*     usuario ingresa y muestra la respuesta del servidor en la consola.
***************************************************************************************/

import java.net.*;
import java.io.*;

// Clase principal que define al cliente TCP
public class TCPClient {
  
  // Método principal: punto de entrada del programa
  public static void main(String[] args) throws Exception {
    try {
      
      Socket socket = new Socket("127.0.0.1", 8888); // Crear socket del cliente y conectarse al servidor en localhost (127.0.0.1), puerto 8888
      DataInputStream inStream = new DataInputStream(socket.getInputStream()); // Lee mensajes del servidor
      DataOutputStream outStream = new DataOutputStream(socket.getOutputStream()); // Enviar mensajes al servidor
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); // Leer consola para la entrada del usuario
      String clientMessage = "";  // Mensaje de entrada del usuario
      String serverMessage = "";  // Mensaje que recibe el servidor

      while (!clientMessage.equals("bye")) {  
        System.out.println("Enter number :"); // Mostrar en consola instrucción al usuario
        clientMessage = br.readLine(); // Leer el mensaje ingresado por teclado
        outStream.writeUTF(clientMessage); // Enviar el mensaje al servidor
        outStream.flush(); // Vaciar el buffer
        serverMessage = inStream.readUTF(); // Esperar y leer la respuesta del servidor
        System.out.println(serverMessage); // Mostrar respuesta del servidor
      }

      // liberar recursos
      outStream.close();
      outStream.close();
      socket.close();

    } catch (Exception e) {
      System.out.println(e); // Manejo de excepciones
    }
  }
}