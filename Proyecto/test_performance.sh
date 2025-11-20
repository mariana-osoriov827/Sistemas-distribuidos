#!/bin/bash
# Script para medir desempeño sin JMeter
# Simula 4 PS con 20 solicitudes cada uno

echo "======================================"
echo "  PRUEBA DE DESEMPEÑO - PS-01"
echo "  4 Procesos Solicitantes"
echo "  20 solicitudes por proceso"
echo "======================================"

# Crear archivo de peticiones
cat > /tmp/test_requests.txt << 'EOF'
PRESTAMO|978-0134685991|testuser1
INFO|978-0134685991|testuser1
DEVOLUCION|978-0134685991|testuser1
PRESTAMO|978-0596009205|testuser2
INFO|978-0596009205|testuser2
DEVOLUCION|978-0596009205|testuser2
PRESTAMO|978-0321573513|testuser3
INFO|978-0321573513|testuser3
DEVOLUCION|978-0321573513|testuser3
PRESTAMO|978-1449355739|testuser4
INFO|978-1449355739|testuser4
DEVOLUCION|978-1449355739|testuser4
PRESTAMO|978-0134685991|testuser5
RENOVACION|978-0134685991|testuser5
DEVOLUCION|978-0134685991|testuser5
PRESTAMO|978-0596009205|testuser6
RENOVACION|978-0596009205|testuser6
DEVOLUCION|978-0596009205|testuser6
INFO|978-0321573513|testuser7
INFO|978-1449355739|testuser8
EOF

# Lanzar 4 procesos en paralelo
START=$(date +%s%3N)

for i in {1..4}; do
  (
    ./cliente.sh localhost /tmp/test_requests.txt > /tmp/log_ps_$i.txt 2>&1
  ) &
done

# Esperar que terminen todos
wait

END=$(date +%s%3N)
TOTAL_TIME=$((END - START))

echo ""
echo "======================================"
echo "  RESULTADOS"
echo "======================================"
echo "Tiempo total: ${TOTAL_TIME}ms"
echo "Total solicitudes: 80 (4 PS × 20)"
echo "Tiempo promedio por solicitud: $((TOTAL_TIME / 80))ms"
echo ""
echo "Logs individuales:"
for i in {1..4}; do
  echo "  PS-$i: /tmp/log_ps_$i.txt"
done
echo ""
echo "Ver errores: grep -i 'error\|fallo' /tmp/log_ps_*.txt"
echo "======================================"
