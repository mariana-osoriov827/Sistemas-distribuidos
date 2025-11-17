#!/bin/bash
# Script de inicio para Sede 1 - Linux
# Sistema Distribuido de Préstamo de Libros

echo "======================================"
echo "  SISTEMA DE PRÉSTAMO DE LIBROS      "
echo "           SEDE 1                     "
echo "======================================"

# Configuración de puertos Sede 1
SEDE=1
PUB_PORT=5555
REP_PORT=5556
GA_PORT=5560
GA_HOST="localhost"

# IP de la réplica (ajustar según tu red)
REPLICA_HOST="10.43.102.177"  # Cambiar por IP de Sede 2
REPLICA_PORT=6560

echo ""
echo "[1/4] Iniciando Gestor de Almacenamiento (GA)..."
gnome-terminal -- bash -c "cd $(pwd) && java -cp target/classes Gestor_Almacenamiento.ServidorGA_TCP primary $GA_PORT $REPLICA_HOST $REPLICA_PORT; exec bash" &
sleep 3

echo "[2/4] Iniciando Gestor de Carga (GC)..."
gnome-terminal -- bash -c "cd $(pwd) && java -cp target/classes Gestor_carga.ServidorGC_ZMQ $SEDE $PUB_PORT $REP_PORT $GA_HOST $GA_PORT; exec bash" &
sleep 3

echo "[3/4] Iniciando Actores..."

# Actor Devolución
gnome-terminal -- bash -c "cd $(pwd) && java -cp target/classes Gestor_carga.ActorClient_ZMQ ${GA_HOST}:${PUB_PORT} ${GA_HOST}:${GA_PORT} DEVOLUCION; exec bash" &
sleep 1

# Actor Renovación
gnome-terminal -- bash -c "cd $(pwd) && java -cp target/classes Gestor_carga.ActorClient_ZMQ ${GA_HOST}:${PUB_PORT} ${GA_HOST}:${GA_PORT} RENOVACION; exec bash" &
sleep 1

# Actor Préstamo
gnome-terminal -- bash -c "cd $(pwd) && java -cp target/classes Gestor_carga.ActorPrestamo_ZMQ ${GA_HOST}:${PUB_PORT} ${GA_HOST}:${GA_PORT}; exec bash" &
sleep 1

echo ""
echo "[4/4] Sistema iniciado correctamente!"
echo ""
echo "Puertos configurados:"
echo "  - Publisher (PUB): $PUB_PORT"
echo "  - Replier (REP): $REP_PORT"
echo "  - GA (TCP): $GA_PORT"
echo ""
echo "Para enviar peticiones desde clientes:"
echo "  java -cp target/classes ClienteBatch_ZMQ src/peticiones.txt ${GA_HOST}:${REP_PORT}"
echo ""
