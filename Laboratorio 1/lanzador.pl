#!/usr/bin/perl
#**************************************************************
#         		Pontificia Universidad Javeriana
#     Autor: Mariana Osorio
#     Fecha: 15/08/2025
#     Materia: Sistemas distribuidos
#     Tema: Taller de EvaluaciÃ³n de Rendimiento
#     Fichero: script automatizaciÃ³n ejecuciÃ³n por lotes 
#****************************************************************/

$Path = `pwd`;
chomp($Path);

$Nombre_Ejecutable = "mmClasicaOpenMP";
@Size_Matriz = ("240", "1120", "2960", "7520", "10640", "10240", "11840", "13760", "16720", "17120", "18080", "19760");
@Num_Hilos = (1,2,4,8,16,20);
$Repeticiones = 30;

foreach $size (@Size_Matriz){
	foreach $hilo (@Num_Hilos) {
		$file = "$Path/$Nombre_Ejecutable-".$size."-Hilos-".$hilo.".dat";
		for ($i=0; $i<$Repeticiones; $i++) {
			system("$Path/$Nombre_Ejecutable $size $hilo  >> $file");
			#printf("$Path/$Nombre_Ejecutable $size $hilo \n");
		}
		close($file);
	$p=$p+1;
	}
}
