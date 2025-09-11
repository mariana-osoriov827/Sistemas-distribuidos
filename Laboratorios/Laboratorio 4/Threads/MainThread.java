package threadsJarroba;

/**
 * 
 * @author Richard
 */
public class MainThread { // Clase principal para probar con hilos
	
	public static void main(String[] args) { // Método main, punto de entrada
		
		Cliente cliente1 = new Cliente("Cliente 1", new int[] { 2, 2, 1, 5, 2, 3 }); // Se crea cliente 1
		Cliente cliente2 = new Cliente("Cliente 2", new int[] { 1, 3, 5, 1, 1 }); // Se crea cliente 2
		
		// Tiempo inicial de referencia
		long initialTime = System.currentTimeMillis();
		
		// Se crean dos cajeras en hilos distintos
		CajeraThread cajera1 = new CajeraThread("Cajera 1", cliente1, initialTime);
		CajeraThread cajera2 = new CajeraThread("Cajera 2", cliente2, initialTime);
		
		// Se inician los hilos (cada cajera procesa al mismo tiempo)
		cajera1.start();
		cajera2.start();
	}
}
