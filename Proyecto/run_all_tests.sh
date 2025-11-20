#!/bin/bash
# Script para ejecutar todos los casos de prueba y guardar la salida en archivos .dat
# Debe ejecutarse desde la raíz del proyecto

# Definir los casos de prueba y sus archivos de entrada
# Puedes ajustar los nombres de los archivos según tu estructura

declare -A pruebas=(
  [PS-01]="src/peticiones.txt"
  [PS-02]="src/archivo_inexistente.txt"
  [DEV-01]="src/dev_01.txt"
  [DEV-02]="src/dev_02.txt"
  [REN-01]="src/ren_01.txt"
  [REN-02]="src/ren_02.txt"
  [PRE-01]="src/pre_01.txt"
  [PRE-02]="src/pre_02.txt"
  [PRE-03]="src/pre_03.txt"
  [PER-01]="src/bd_primaria.txt src/bd_replicada.txt"
  [PER-02]="src/per_02.txt"
  [FAIL-01]="src/fail_01.txt"
  [FAIL-02]="src/fail_02.txt"
  [DES-01]="src/des_01.txt"
  [DES-02]="src/des_02.txt"
  [DES-03]="src/des_03.txt"
  [DES-04]="src/des_04.txt"
)

# Ejecutar cada prueba y guardar la salida

# Configura la IP o hostname del servidor aquí:
SEDE1_IP="localhost"
REP_PORT=5556

for caso in "${!pruebas[@]}"; do
  entrada=${pruebas[$caso]}
  echo "Ejecutando $caso con entrada: $entrada"
  if [[ $caso == PER-01 ]]; then
    # Comparar archivos para PER-01
    diff $entrada > "$caso.dat"
  else
    start=$(date +%s%N)
    ./cliente.sh $SEDE1_IP $entrada > "$caso.dat" 2>&1
    end=$(date +%s%N)
    elapsed=$(( (end - start)/1000000 ))
    echo "Tiempo de ejecución: $elapsed ms" >> "$caso.dat"
  fi
  echo "Salida guardada en $caso.dat"
done

echo "Todas las pruebas han sido ejecutadas."
