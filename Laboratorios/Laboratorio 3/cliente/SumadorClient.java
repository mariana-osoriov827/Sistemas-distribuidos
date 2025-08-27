import java.rmi.*; // Permite ejecutar métodos de objetos ubicados en otra máquina con Remote Method Invocation

public class SumadorClient {

  //Los argumentos que se pasen por consola estarán en el arreglo 'args'. Se espera que el primer argumento sea la dirección del servidor RMI.
  public static void main(String args[]) {

    // Variable local donde se guardará el resultado de la operación remota.
    int res = 0; 

    try {

      // Mensaje para saber que el cliente está buscando el objeto remoto.
      System.out.println("Buscando Objeto "); 

      /*  
       * Se hace un casting a la interfaz "Sumador" para poder usar sus métodos.
       * Se utiliza la clase Naming para localizar el objeto remoto.
       * "rmi://" indica que se usará RMI.
       */
      Sumador misuma = (Sumador)Naming.lookup("rmi://" + args[0] + "/" +"MiSumador");

      // Aquí se suman los números 5 y 2 en el servidor, y el resultado se devuelve al cliente.
      res = misuma.sumar(5, 2); 

      //Aqui se imprime el resultado de la operacion obtenido desde el servidor
      System.out.println("5 + 2 = " + res); 

    } catch(Exception e) {
        // Si ocurre cualquier excepción se captura aquí.
        System.err.println(" System exception");
    }

    //Finaliza la ejecucion del programa
    System.exit(0);

  }

}