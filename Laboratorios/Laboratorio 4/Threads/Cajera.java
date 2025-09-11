package threadsJarroba;

/**
 * 
 * @author Richard
 */
public class Cajera { // Clase que representa a una cajera en el supermercado
	
	private String nombre;
	
	public Cajera(String nombre) {
		this.nombre = nombre; // Se asigna el nombre de la cajera
	}
	
	public void procesarCompra(Cliente cliente, long timeStamp) { // Método que simula el proceso de compra
		System.out.println("La cajera " + this.nombre + 
				" comienza a procesar la compra del cliente " + cliente.getNombre() + 
				" en el tiempo: " + (System.currentTimeMillis() - timeStamp) / 1000 + "seg"); // Imprime inicio del proceso
		
		// Recorre los productos del cliente
		for (int i = 0; i < cliente.getCarroCompra().length; i++) { 
			this.esperarXsegundos(cliente.getCarroCompra()[i]); // Simula el tiempo de escaneo
			System.out.println("Procesado el producto " + (i + 1) + 
					" del cliente " + cliente.getNombre() + 
					" ->Tiempo: " + (System.currentTimeMillis() - timeStamp) / 1000 + "seg"); // Imprime progreso
		}
		
		System.out.println("La cajera " + this.nombre + 
				" ha terminado de procesar " + cliente.getNombre() + 
				" en el tiempo: " + (System.currentTimeMillis() - timeStamp) / 1000 + "seg"); // Imprime fin
	}
	
	private void esperarXsegundos(int segundos) { // Método que simula la espera por cada producto
		try {
			Thread.sleep(segundos * 1000); // Simula los segundos de espera
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt(); // Si algo interrumpe el hilo, lo maneja
		}
	}
}
