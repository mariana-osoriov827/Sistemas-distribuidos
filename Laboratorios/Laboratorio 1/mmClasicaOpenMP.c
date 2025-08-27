/*#######################################################################################
 #* Fecha: 15/08/2025
 #* Autor: Mariana Osorio Vasquez
 #* Tema: 
 #* 	- Programa Multiplicación de Matrices algoritmo clásico
 #* 	- Paralelismo con OpenMP
######################################################################################*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <sys/time.h>
#include <omp.h>


struct timeval inicio, fin; 

// Función que marca el inicio del tiempo.
void InicioMuestra(){
	gettimeofday(&inicio, (void *)0); // Captura el tiempo actual y lo guarda en 'inicio'.
}

// Función que calcula y muestra el tiempo transcurrido.
void FinMuestra(){
	gettimeofday(&fin, (void *)0); // Captura el tiempo actual y lo guarda en 'fin'.
	fin.tv_usec -= inicio.tv_usec;
	fin.tv_sec  -= inicio.tv_sec;// Resta los segundos.
    // Calcula el tiempo total en microsegundos para una medida precisa.
	double tiempo = (double) (fin.tv_sec*1000000 + fin.tv_usec); 
	printf("%9.0f \n", tiempo); // Imprime el tiempo total.
}

// Función para imprimir una matriz.
void impMatrix(double *matrix, int D){
	printf("\n");
	if(D < 9){ // Solo imprime matrices de dimensión menor a 9
		for(int i=0; i<D*D; i++){
			if(i%D==0) printf("\n");
			printf("%f ", matrix[i]);
		}
		printf("\n**-----------------------------**\n");
	}
}

// Función para inicializar dos matrices con valores aleatorios.
void iniMatrix(double *m1, double *m2, int D){
	// Recorre todos los elementos de las matrices usando punteros.
	for(int i=0; i<D*D; i++, m1++, m2++){
		*m1 = rand()%100;	// Asigna un valor aleatorio a la matriz 1 (0-99)
		*m2 = rand()%100;	// Asigna un valor aleatorio a la matriz 2 (0-99)
	}
}

// Función para multiplicar dos matrices usando OpenMP.
void multiMatrix(double *mA, double *mB, double *mC, int D){
	double Suma, *pA, *pB;
	// Directiva de OpenMP para crear un equipo de hilos.
    #pragma omp parallel
    {
    // Directiva de OpenMP para distribuir las iteraciones del bucle for entre los hilos.
    #pragma omp for
	for(int i=0; i<D; i++){ // Bucle para recorrer las filas de la matriz A.
        for(int j=0; j<D; j++){ // Bucle para recorrer las columnas de la matriz B.
            pA = mA+i*D;        // Puntero que apunta al inicio de la fila 'i' de mA.
            pB = mB+j;          // Puntero que apunta al inicio de la columna 'j' de mB.
            Suma = 0.0;
            // Bucle para calcular el producto punto.
            for(int k=0; k<D; k++, pA++, pB+=D){
                Suma += *pA * *pB; // Multiplica y suma los elementos correspondientes.
            }
            mC[i*D+j] = Suma; // Asigna el resultado del producto punto a la matriz C.
        }
    }
    }
}

// Función principal del programa.
int main(int argc, char *argv[]){
	if(argc < 3){
		printf("\n Use: $./clasicaOpenMP SIZE Hilos \n\n");
		exit(0);
	}

	// Convierte los argumentos de cadena a enteros.
    int N = atoi(argv[1]);  // Dimensión de la matriz (SIZE).
    int TH = atoi(argv[2]); // Número de hilos.
    // Asigna memoria dinámicamente para las matrices, inicializándolas con ceros.
    double *matrixA  = (double *)calloc(N*N, sizeof(double));
    double *matrixB  = (double *)calloc(N*N, sizeof(double));
    double *matrixC  = (double *)calloc(N*N, sizeof(double));
    srand(time(NULL)); // Inicializa la semilla para generar números aleatorios.

    omp_set_num_threads(TH); // Establece el número de hilos de OpenMP.

    iniMatrix(matrixA, matrixB, N); // Llama a la función para inicializar las matrices.

	impMatrix(matrixA, N); // Imprime la matriz A si es pequeña
	impMatrix(matrixB, N); // Imprime la matriz B si es pequeña

	InicioMuestra();
	multiMatrix(matrixA, matrixB, matrixC, N);
	FinMuestra();

	impMatrix(matrixC, N); // Imprime la matriz C si es pequeña

	/*Liberación de Memoria*/
	free(matrixA);
	free(matrixB);
	free(matrixC);
	
	return 0;
}
