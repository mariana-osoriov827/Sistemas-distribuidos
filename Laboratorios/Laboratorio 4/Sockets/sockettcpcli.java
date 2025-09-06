/**************************************************************************************
* Fecha: 05/09/2025
* Autor: Mariana Osorio Vasquez, Gabriel Jaramillo, Roberth Méndez, Juan Esteban Vera
* Tema: 
* - Programación de Sockets
* Descripción:
* - Este programa es un cliente TCP que se conecta a un servidor en un puerto específico (6001) para enviar mensajes.
***************************************************************************************/

import java.net.*;
import java.io.*;

// Declaración de la clase principal.
public class sockettcpcli {
   // Método principal, punto de entrada del programa. Recibe argumentos de la línea de comandos.
   public static void main(String argv[]) {

      // Verifica si se ha proporcionado un argumento (la dirección del servidor).
      // Si no hay argumentos, muestra un mensaje de error y termina el programa.
      if (argv.length == 0) {
         System.err.println("java sockettcpcli servidor");
         System.exit(1);
      }

      // Se crea un objeto BufferedReader para leer la entrada del usuario desde la consola.
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

      // Muestra un mensaje en la consola para indicar que el programa se ha iniciado.
      System.out.println("Prueba de sockets TCP (cliente)");
      
      // Se declaran las variables que se utilizarán para el socket y los datos.
      Socket socket;
      InetAddress address;
      byte[] mensaje_bytes = new byte[256];
      String mensaje="";

      // El bloque try-catch maneja las posibles excepciones que puedan ocurrir durante la ejecución.
      try {
         // Intenta obtener la dirección IP del servidor a partir del nombre o la IP proporcionada como argumento.
         System.out.print("Capturando direccion de host... ");
         address=InetAddress.getByName(argv[0]);
         System.out.println("ok");

         // Intenta crear un nuevo socket de cliente y conectarlo al servidor en el puerto 6001.
         System.out.print("Creando socket... ");
         socket = new Socket(address,6001);
         System.out.println("ok");

         // Se crea un flujo de salida para enviar datos a través del socket.
         DataOutputStream out =
            new DataOutputStream(socket.getOutputStream());

         // Le pide al usuario que ingrese los mensajes a enviar.
         System.out.println("Introduce mensajes a enviar:");

         // Inicia un bucle que continuará hasta que el mensaje enviado comience con "fin".
         do {
            // Lee una línea de texto de la consola.
            mensaje = in.readLine();
            // Envía el mensaje al servidor a través del flujo de salida.
            out.writeUTF(mensaje);
         } while (!mensaje.startsWith("fin"));
      }
      // Si ocurre alguna excepción durante la ejecución del bloque try, la captura y la maneja aquí.
      catch (Exception e) {
         // Muestra el mensaje de error en la salida de error estándar.
         System.err.println(e.getMessage());
         // Termina el programa con un código de error.
         System.exit(1);
      }
   }
}