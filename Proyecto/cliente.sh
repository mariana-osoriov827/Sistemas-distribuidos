#!/bin/bash
# Cliente para enviar peticiones al sistema distribuido


if [ $# -lt 1 ]; then
    echo "======================================"
    echo "  CLIENTE SISTEMA DE PRÉSTAMOS       "
    echo "======================================"
    echo ""
    echo "Uso:"
    echo "  ./cliente.sh <IP_SEDE> [PUERTO_REP] [archivo_peticiones]"
    echo ""
    echo "Ejemplos:"
    echo "  ./cliente.sh 10.43.103.49 5556                    # Interactivo sede 1"
    echo "  ./cliente.sh 10.43.102.177 6556                   # Interactivo sede 2"
    echo "  ./cliente.sh 10.43.103.49 5556 src/peticiones.txt  # Batch sede 1"
    echo ""
    exit 1
fi

SEDE_IP=$1
REP_PORT=${2:-6556}
ARCHIVO=${3:-""}

CP="target/classes:$HOME/.m2/repository/org/zeromq/jeromq/0.6.0/jeromq-0.6.0.jar"

if [ -z "$ARCHIVO" ]; then
    echo "======================================"
    echo "  CLIENTE INTERACTIVO                 "
    echo "======================================"
    echo "Conectando a: $SEDE_IP:$REP_PORT"
    echo ""
    # Usar el nuevo cliente interactivo mejorado
    java -cp "$CP" ClienteInteractivo_ZMQ $SEDE_IP:$REP_PORT
else
    echo "======================================"
    echo "  CLIENTE BATCH                       "
    echo "======================================"
    echo "Conectando a: $SEDE_IP:$REP_PORT"
    echo "Archivo: $ARCHIVO"
    echo ""
    if [ ! -f "$ARCHIVO" ]; then
        echo "[ERROR] Archivo '$ARCHIVO' no encontrado"
        exit 1
    fi
    java -cp "$CP" ClienteBatch_ZMQ $ARCHIVO $SEDE_IP:$REP_PORT
fi