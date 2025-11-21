#!/bin/bash
# Cliente para enviar peticiones al sistema distribuido

if [ $# -lt 1 ]; then
    echo "======================================"
    echo "  CLIENTE SISTEMA DE PRÉSTAMOS       "
    echo "======================================"
    echo ""
    echo "Uso:"
    echo "  ./cliente.sh <IP_SEDE1> [archivo_peticiones]"
    echo ""
    echo "Ejemplos:"
    echo "  ./cliente.sh 10.43.103.49                    # Interactivo"
    echo "  ./cliente.sh 10.43.103.49 src/peticiones.txt # Batch"
    echo ""
    exit 1
fi

SEDE1_IP=$1
REP_PORT=5556
ARCHIVO=${2:-""}

CP="target/classes:$HOME/.m2/repository/org/zeromq/jeromq/0.6.0/jeromq-0.6.0.jar"

if [ -z "$ARCHIVO" ]; then
    echo "======================================"
    echo "  CLIENTE INTERACTIVO                 "
    echo "======================================"
    echo "Conectando a: $SEDE1_IP:$REP_PORT"
    echo ""
    
    # Usar el nuevo cliente interactivo mejorado
    java -cp "$CP" ClienteInteractivo_ZMQ $SEDE1_IP:$REP_PORT
else
    echo "======================================"
    echo "  CLIENTE BATCH                       "
    echo "======================================"
    echo "Conectando a: $SEDE1_IP:$REP_PORT"
    echo "Archivo: $ARCHIVO"
    echo ""
    
    if [ ! -f "$ARCHIVO" ]; then
        echo "[ERROR] Archivo '$ARCHIVO' no encontrado"
        exit 1
    fi
    
    java -cp "$CP" ClienteBatch_ZMQ $ARCHIVO $SEDE1_IP:$REP_PORT
fi