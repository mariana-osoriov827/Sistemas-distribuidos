/*****************************************************************
- Fecha: 26 de septiembre de 2025
- Autor: Mariana Osorio Vásquez
- Materia: sistemas distribuidos
- Tema: comunicación de procesos con MPI
*****************************************************************/

#include <mpi.h>
#include <stdio.h>

int main(int argc, char** argv){
  //Inicializar el entorno ,PI
  MPI_Init(NULL, NULL);
  
  //Numero del proceso
  int world_size;
  MPI_Comm_size(MPI_COMM_WORLD, &world_size);
  
  // Rank del proceso
  int world_rank;
  MPI_Comm_size(MPI_COMM_WORLD, &world_rank);
  
  // Get nombre del proceso
  char processor_name[MPI_MAX_PROCESSOR_NAME];
  int name_len;
  MPI_Get_processor_name(processor_name, &name_len);
  
  // Imprimir hello world
  printf("|==>Hello world <==| from processor %s, rank %d out of %d processor \n", processor_name, world_rank, world_size);
  
  //finalizar el entorno MPI
  MPI_Finalize();
}
