# ✅ CHECKLIST DE REQUISITOS DEL PROYECTO

## 📋 Requisitos de la Segunda Entrega (16 Nov)

### 🎯 Funcionalidad Principal

#### Operaciones (3/3)
- [x] **Devolución** - Asíncrona con PUB/SUB
- [x] **Renovación** - Asíncrona con PUB/SUB, máx. 2 renovaciones
- [x] **Préstamo** - Síncrona con validación en BD, 14 días

#### Procesos (4/4)
- [x] **Procesos Solicitantes (PS)** - Leen archivo de peticiones
- [x] **Gestor de Carga (GC)** - Uno por sede con ZeroMQ
- [x] **Actores** - 3 por sede (Devol, Renov, Préstamo)
- [x] **Gestor de Almacenamiento (GA)** - Réplica primaria/secundaria

---

## 🔌 Comunicación y Patrones

### ZeroMQ (Obligatorio)
- [x] **Librería ZeroMQ** utilizada (JeroMQ para Java)
- [x] **Patrón PUB/SUB** para Devolución y Renovación
- [x] **Patrón REQ/REP** para comunicación PS → GC
- [x] **Comunicación síncrona** para préstamos

### Patrones Implementados
- [x] **Publicador/Suscriptor** - GC publica, Actores suscriben
- [x] **Request/Reply** - PS envía solicitudes, GC responde
- [x] **Asíncrono** - Devolución y Renovación
- [x] **Síncrono** - Préstamo con validación

---

## 💾 Base de Datos y Persistencia

### Estructura
- [x] **Entidades**: Libro, Ejemplar
- [x] **Campos mínimos**: código, nombre, ejemplares disponibles
- [x] **Persistencia**: Archivo de texto (libros.txt)
- [x] **Thread-safe**: Sincronización con `synchronized`

### Datos Iniciales
- [ ] **1000 libros** cargados (PENDIENTE - actualmente ~10)
- [ ] **200 prestados** (50 sede 1, 150 sede 2) - PENDIENTE
- [x] **Ejemplares únicos** - Algunos libros con 1 solo ejemplar
- [x] **BDs idénticas** al inicio en ambas sedes

### Operaciones BD
- [x] **Prestar** - Marca ejemplar como 'P', fecha +14 días
- [x] **Devolver** - Marca ejemplar como 'D', resetea renovaciones
- [x] **Renovar** - Extiende fecha +7 días, valida máx. 2

---

## 🔄 Replicación y Tolerancia a Fallos

### Replicación
- [x] **Asíncrona** entre GA primario y réplica
- [x] **Cola de replicación** con procesamiento cada 3 segundos
- [x] **Operaciones replicadas**: PRESTAMO, DEVOLUCION, RENOVACION

### Tolerancia a Fallos
- [ ] **Failover GA** - Cambio automático si falla primario (PENDIENTE)
- [ ] **Reconexión automática** de actores (PENDIENTE)
- [x] **Manejo de errores** en comunicación
- [x] **Operaciones atómicas** con sincronización

---

## 📂 Archivos y Código

### Código Fuente
- [x] **Todos los .java** implementados
- [x] **Código documentado** con comentarios
- [x] **Header con autores y fecha** en todos los archivos
- [x] **Organización en paquetes** (Gestor_carga, Gestor_Almacenamiento)

### Archivos de Configuración
- [x] **pom.xml** con dependencias
- [x] **peticiones.txt** con mínimo 20 operaciones
- [x] **peticiones2.txt** para segunda sede
- [x] **libros.txt** para persistencia

### Documentación
- [x] **README.md** completo
- [x] **GUIA_IMPLEMENTACION.md** detallada
- [x] **RESUMEN_CAMBIOS.md** con cambios
- [x] **Este checklist** (CHECKLIST.md)

### Scripts
- [x] **iniciar_sede1.ps1** - Script automático Sede 1
- [x] **iniciar_sede2.ps1** - Script automático Sede 2

---

## 🖥️ Despliegue y Pruebas

### Distribución en Computadores
- [ ] **Computador 1**: GC + Actores Sede 1 (PENDIENTE)
- [ ] **Computador 2**: GC + Actores Sede 2 (PENDIENTE)
- [ ] **Computador 3**: Procesos Solicitantes (PENDIENTE)

### Pruebas Funcionales
- [x] **Prueba local** en una máquina
- [ ] **Prueba distribuida** en 3 máquinas (PENDIENTE)
- [ ] **Prueba de fallo GA** (PENDIENTE)
- [ ] **Prueba de concurrencia** múltiples PS (PENDIENTE)

---

## 📹 Entregables Segunda Entrega

### Código y Ejecución
- [x] **Archivo .zip** con código fuente
- [x] **README** con instrucciones de ejecución
- [x] **Código compilable** con Maven

### Video (Máx. 10 minutos)
- [ ] **Distribución de componentes** en máquinas (PENDIENTE)
- [ ] **Librerías y patrones** usados (PENDIENTE)
- [ ] **Tratamiento de falla GA/BD** (PENDIENTE)
- [ ] **Generación de carga** (PENDIENTE)

### Informe de Rendimiento (Máx. 5 páginas)
- [ ] **Descripción experimentos** (PENDIENTE)
- [ ] **Especificaciones HW/SW** (PENDIENTE)
- [ ] **Herramientas de medición** (PENDIENTE)
- [ ] **Tablas con resultados** (PENDIENTE)
- [ ] **Gráficos de variables** (PENDIENTE)
- [ ] **Análisis de resultados** (PENDIENTE)

### Documentación Complementaria
- [ ] **Complementar primera entrega** (PENDIENTE)
- [x] **Archivos fuente documentados**

---

## 📊 Medidas de Rendimiento

### Experimentos Requeridos
- [ ] **4 PS** por sede (PENDIENTE)
- [ ] **6 PS** por sede (PENDIENTE)
- [ ] **10 PS** por sede (PENDIENTE)

### Variables a Medir
- [ ] **Tiempo respuesta promedio** préstamos (PENDIENTE)
- [ ] **Desviación estándar** tiempos (PENDIENTE)
- [ ] **Cantidad solicitudes** en 2 minutos (PENDIENTE)

### Comparación (Elegir UNA)
- [ ] **Opción A**: Gestores seriales vs multihilos (PENDIENTE)
- [ ] **Opción B**: Comunicación asíncrona vs síncrona (PENDIENTE)

---

## 🎯 Validaciones Específicas

### Operación Préstamo
- [x] **Duración**: 2 semanas (14 días)
- [x] **Validación síncrona** en BD
- [x] **Solo ejemplares disponibles** ('D')
- [x] **Actualización inmediata** en BD
- [x] **Respuesta al PS** después de validar

### Operación Renovación
- [x] **Duración**: +1 semana (7 días)
- [x] **Máximo 2 renovaciones** por libro
- [x] **Validación de límite** antes de renovar
- [x] **Respuesta inmediata** al PS
- [x] **Actualización asíncrona** en BD

### Operación Devolución
- [x] **Solo libros prestados** ('P')
- [x] **Respuesta inmediata** al PS
- [x] **Actualización asíncrona** en BD
- [x] **Reset contador renovaciones**

---

## 🔍 Revisión de Código

### Calidad
- [x] **Nombres descriptivos** de variables y métodos
- [x] **Comentarios explicativos** en código complejo
- [x] **Headers con información** de autores
- [x] **Manejo de excepciones** apropiado
- [x] **Sin código comentado** innecesario

### Arquitectura
- [x] **Separación de responsabilidades** clara
- [x] **Bajo acoplamiento** entre componentes
- [x] **Alta cohesión** dentro de clases
- [x] **Patrón MVC/Capas** respetado

### Concurrencia
- [x] **Thread-safety** en operaciones críticas
- [x] **Sincronización apropiada** (synchronized)
- [x] **Uso de ConcurrentHashMap** donde corresponde
- [x] **Sin condiciones de carrera** evidentes

---

## 📈 Estado General del Proyecto

### Implementación Técnica
| Aspecto | Completado | Observaciones |
|---------|------------|---------------|
| Arquitectura base | ✅ 100% | Completa y funcional |
| Operaciones CRUD | ✅ 100% | Préstamo, Devol, Renov |
| Comunicación ZeroMQ | ✅ 100% | PUB/SUB y REQ/REP |
| Persistencia | ✅ 100% | Archivo texto |
| Replicación | ✅ 90% | Falta failover automático |
| Validaciones | ✅ 100% | Todas implementadas |
| Documentación | ✅ 100% | Completa |

### Entregables
| Componente | Estado | Prioridad |
|------------|--------|-----------|
| Código funcional | ✅ Listo | ✓ |
| Documentación código | ✅ Listo | ✓ |
| README/Guías | ✅ Listo | ✓ |
| Scripts inicio | ✅ Listo | ✓ |
| Pruebas 3 máquinas | ⏳ Pendiente | 🔴 ALTA |
| Video demostración | ⏳ Pendiente | 🔴 ALTA |
| Informe rendimiento | ⏳ Pendiente | 🔴 ALTA |
| Experimentos | ⏳ Pendiente | 🔴 ALTA |

---

## 🚨 Acciones Urgentes (Para Sustentación)

### Alta Prioridad 🔴
1. [ ] **Generar archivo libros.txt con 1000 libros**
2. [ ] **Probar en 3 computadores físicos/VMs**
3. [ ] **Grabar video de 10 minutos**
4. [ ] **Realizar experimentos de rendimiento**
5. [ ] **Crear informe de 5 páginas**

### Media Prioridad 🟡
6. [ ] Implementar failover automático GA
7. [ ] Agregar heartbeat entre componentes
8. [ ] Mejorar logging y monitoreo
9. [ ] Crear más archivos de peticiones

### Baja Prioridad 🟢
10. [ ] Optimizar rendimiento
11. [ ] Agregar interfaz gráfica
12. [ ] Implementar autenticación
13. [ ] Mejorar manejo de errores

---

## 📞 Contacto y Notas

### Equipo
- Gabriel Jaramillo Cuberos
- Roberth Méndez Rivera
- Mariana Osorio Vásquez
- Juan Esteban Vera Garzón

### Notas Importantes
- ⚠️ Fecha entrega: **16 de noviembre de 2025**
- ⚠️ Sustentación: **Semana 17**
- ⚠️ Presencial: **Todos los integrantes deben estar presentes**
- ⚠️ No plagio: Código y documentos originales

---

## ✅ Resumen Ejecutivo

### ¿Qué está funcionando?
✅ Todas las operaciones implementadas y probadas localmente  
✅ Comunicación ZeroMQ correcta  
✅ Persistencia y replicación funcionando  
✅ Validaciones de negocio correctas  
✅ Documentación completa  

### ¿Qué falta?
⏳ Pruebas en ambiente distribuido (3 máquinas)  
⏳ Video de demostración  
⏳ Informe de rendimiento con métricas  
⏳ Experimentos con carga variable  
⏳ Generar BD completa (1000 libros)  

### ¿Listo para sustentar?
**🟡 CASI** - Falta realizar pruebas distribuidas y generar entregables de documentación

---

**Última actualización**: 17 de noviembre de 2025
