package threadsJarroba;

/**
 * 
 * @author Richard
 */
public class Cliente { // Clase que representa a un cliente del supermercado
	
	private String nombre; // Nombre del cliente
	private int[] carroCompra; // Lista de tiempos que representa cada producto
	
	public Cliente(String nombre, int[] carroCompra) {
		this.nombre = nombre; // Se asigna el nombre del cliente
		this.carroCompra = carroCompra; // Se asigna el carrito de compras
	}
	
	public String getNombre() { // Retorna el nombre del cliente
		return nombre;
	}
	
	public int[] getCarroCompra() { // Retorna los productos del carrito
		return carroCompra;
	}
}
