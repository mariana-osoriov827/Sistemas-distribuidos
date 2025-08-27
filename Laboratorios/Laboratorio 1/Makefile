# Define el compilador de C que se va a utilizar.
GCC = gcc

# Define las banderas generales del compilador. 
CFLAGS = -lm

# Define las banderas de OpenMP. '-fopenmp' habilita el soporte para OpenMP,
# y '-O3' aplica un alto nivel de optimización al código generado.
FOPENMP = -fopenmp -O3

# Define las banderas para hilos POSIX. Está incluida pero no se usa, ya que
# el modelo de hilos elegido es OpenMP.
POSIX = -lpthread

modulo = inter.c

# Define el nombre del programa ejecutable que se va a construir.
PROGRAMAS = mmClasicaOpenMP 

# El objetivo 'ALL' es el objetivo por defecto. Cuando se ejecuta 'make',
# se construirán todos los programas listados en la variable 'PROGRAMAS'.
ALL: $(PROGRAMAS)

# Este es el objetivo para compilar el programa 'mmClasicaOpenMP'.
mmClasicaOpenMP:

	$(GCC) $(CLFAGS) $@.c -o $@ $(FOPENMP)

# El objetivo 'clean' se utiliza para eliminar el programa compilado.
clean:
	$(RM) $(PROGRAMAS) 
