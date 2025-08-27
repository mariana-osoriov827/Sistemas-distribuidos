#!/usr/bin/perl
#**************************************************************
#         		Pontificia Universidad Javeriana
#     Autor: Mariana Osorio
#     Fecha: 15/08/2025
#     Materia: Sistemas distribuidos
#     Tema: Taller de EvaluaciÃ³n de Rendimiento
#     Fichero: script automatizaciÃ³n ejecuciÃ³n por lotes 
#****************************************************************/

# Obtiene la ruta del directorio de trabajo actual y elimina el salto de línea al final.
$Path = `pwd`;
chomp($Path);

# Define el nombre del archivo ejecutable del programa de multiplicación de matrices.
$Nombre_Ejecutable = "mmClasicaOpenMP";
# Arreglo con los diferentes tamaños de matriz a probar.
@Size_Matriz = ("240", "1120", "2960", "7520", "10640", "10240", "11840", "13760", "16720", "17120", "18080", "19760");
# Arreglo con el número de hilos a usar en cada prueba.
@Num_Hilos = (1,2,4,8,16,20);
# Define la cantidad de veces que se repetirá cada prueba.
$Repeticiones = 30;
# Bucle principal que itera sobre cada tamaño de matriz.
foreach $size (@Size_Matriz){
    # Bucle anidado que itera sobre cada número de hilos.
    foreach $hilo (@Num_Hilos) {
        # Construye el nombre del archivo de salida para la combinación actual de tamaño e hilos.
		$file = "$Path/$Nombre_Ejecutable-".$size."-Hilos-".$hilo.".dat";
		# Bucle para ejecutar el programa el número de veces definido en $Repeticiones.
		for ($i=0; $i<$Repeticiones; $i++) {
			# Ejecuta el programa y redirige su salida estándar al archivo de datos,
            # agregando el resultado al final (>>).
			system("$Path/$Nombre_Ejecutable $size $hilo  >> $file");
			#printf("$Path/$Nombre_Ejecutable $size $hilo \n");
		}
		# Cierra el archivo de salida (aunque system() ya lo gestiona, es buena práctica).
		close($file);
	$p=$p+1;
	}
}
