/**************************************************************************************
* Fecha: 05/09/2025
* Autor: Mariana Osorio Vasquez, Gabriel Jaramillo, Roberth Méndez, Juan Esteban Vera
* Tema: 
* - Programación de Sockets
* Descripción:
* - Este programa es un servidor TCP que escucha en el puerto 6001, acepta una conexión de un cliente
* y lee los mensajes que este le envíe, mostrándolos en la consola.
***************************************************************************************/

import java.net.*;
import java.io.*;

// La clase principal del programa, que actúa como el servidor.
public class sockettcpser {
    public static void main(String argv[]) {
      
      // Imprime un mensaje en la consola para indicar que el servidor se ha iniciado.
      System.out.println("Prueba de sockets TCP (servidor)");
      
      ServerSocket socket;
      boolean fin = false;

      // Se utiliza un bloque try-catch para manejar cualquier excepción que pueda ocurrir.
      try {
         // Se crea un objeto ServerSocket, que escucha las peticiones de conexión en el puerto 6001.
         socket = new ServerSocket(6001);
         
         // El servidor se queda "escuchando" (bloqueado) en la línea 'accept()' hasta que un cliente se conecta.
         // Cuando un cliente se conecta, se crea un nuevo objeto Socket_cli para manejar la comunicación con él.
         Socket socket_cli = socket.accept();
         
         // Se crea un flujo de entrada para leer los datos que el cliente envíe a través del socket.
         DataInputStream in =
            new DataInputStream(socket_cli.getInputStream());
         
         // Se inicia un bucle infinito que continuará ejecutándose indefinidamente (ya que 1>0 siempre es verdadero).
         do {
            // Se declara e inicializa una variable para almacenar el mensaje.
            String mensaje ="";
            
            // El servidor espera a recibir un mensaje (bloqueado en esta línea) y lo lee como una cadena de texto.
            mensaje = in.readUTF();
            
            // Muestra el mensaje recibido del cliente en la consola.
            System.out.println(mensaje);
         } while (1>0);
      }
      // Este bloque captura cualquier excepción (como errores de red o I/O) y maneja el error.
      catch (Exception e) {
         // Imprime el mensaje de la excepción en la salida de error estándar.
         System.err.println(e.getMessage());
         // Termina el programa con un código de salida de error.
         System.exit(1);
      }
   }
}