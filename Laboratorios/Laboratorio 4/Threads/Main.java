package threadsJarroba;

/**************************************************************************************
* Fecha: 05/09/2025
* Autor: Mariana Osorio Vasquez, Gabriel Jaramillo, Roberth Méndez, Juan Esteban Vera
* Tema: 
* - Programación de Sockets
* Descripción:
* - // Este programa se encarga crear las instancias de cliente y cajero, de manera que un cajero atienda a un cliente,
		en esta situacion el programa funciona de manera secuencial es decir, se atiende primero un cliente y despues el siguiente.
***************************************************************************************/
public class Main { // Clase principal
	
	public static void main(String[] args) { // Método main, punto de entrada del programa
		
		Cliente cliente1 = new Cliente("Cliente 1", new int[] { 2, 2, 1, 5, 2, 3 }); // Se crea un cliente con tiempos de compra
		Cliente cliente2 = new Cliente("Cliente 2", new int[] { 1, 3, 5, 1, 1 }); // Otro cliente
		
		Cajera cajera1 = new Cajera("Cajera 1"); // Se crea la cajera 1
		Cajera cajera2 = new Cajera("Cajera 2"); // Se crea la cajera 2
		
		// Tiempo inicial de la simulación
		long initialTime = System.currentTimeMillis();
		
		// Se procesan los clientes de manera secuencial (sin hilos)
		cajera1.procesarCompra(cliente1, initialTime);
		cajera2.procesarCompra(cliente2, initialTime);
	}
}
