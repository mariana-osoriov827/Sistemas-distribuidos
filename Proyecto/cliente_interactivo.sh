#!/bin/bash
# Cliente interactivo para probar operaciones manualmente

if [ $# -lt 1 ]; then
    echo "Uso: ./cliente_interactivo.sh <IP_SEDE1>"
    echo "Ejemplo: ./cliente_interactivo.sh 10.43.103.49"
    exit 1
fi

SEDE1_IP=$1
REP_PORT=5556

echo "======================================"
echo "  CLIENTE INTERACTIVO                 "
echo "======================================"
echo "Conectando a Sede 1: $SEDE1_IP:$REP_PORT"
echo ""

while true; do
    echo ""
    echo "Seleccione operación:"
    echo "1) PRESTAMO"
    echo "2) DEVOLUCION"
    echo "3) RENOVACION"
    echo "4) Salir"
    read -p "Opción: " opcion
    
    case $opcion in
        1)
            read -p "ID Usuario: " usuario
            read -p "Código Libro: " libro
            echo "Enviando: PRESTAMO;$usuario;$libro"
            echo "PRESTAMO;$usuario;$libro" | java -cp "target/classes:$HOME/.m2/repository/org/zeromq/jeromq/0.6.0/jeromq-0.6.0.jar" ClienteBatch_ZMQ /dev/stdin $SEDE1_IP:$REP_PORT
            ;;
        2)
            read -p "ID Usuario: " usuario
            read -p "Código Libro: " libro
            echo "Enviando: DEVOLUCION;$usuario;$libro"
            echo "DEVOLUCION;$usuario;$libro" | java -cp "target/classes:$HOME/.m2/repository/org/zeromq/jeromq/0.6.0/jeromq-0.6.0.jar" ClienteBatch_ZMQ /dev/stdin $SEDE1_IP:$REP_PORT
            ;;
        3)
            read -p "ID Usuario: " usuario
            read -p "Código Libro: " libro
            echo "Enviando: RENOVACION;$usuario;$libro"
            echo "RENOVACION;$usuario;$libro" | java -cp "target/classes:$HOME/.m2/repository/org/zeromq/jeromq/0.6.0/jeromq-0.6.0.jar" ClienteBatch_ZMQ /dev/stdin $SEDE1_IP:$REP_PORT
            ;;
        4)
            echo "Saliendo..."
            exit 0
            ;;
        *)
            echo "Opción inválida"
            ;;
    esac
done