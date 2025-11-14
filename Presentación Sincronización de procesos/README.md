# Procesos Distribuidos: Sincronización y Relojes Lógicos de Lamport
Este repositorio contiene una presentación resumida y una implementación en Java sobre **procesos distribuidos**, **sincronización**, **exclusión mutua distribuida** y **relojes lógicos de Lamport**.

Autores: Gabriel Jaramillo Cuberos, Roberth Méndez Rivera, Mariana Osorio Vásquez, Juan Esteban Vera Garzón

## 🧩 1. Introducción a los Procesos Distribuidos
En los sistemas distribuidos, múltiples procesos se ejecutan en máquinas distintas **sin memoria compartida**. Un reto principal es mantener **coherencia y orden** entre eventos distribuidos.

### Problema central
- **No existe un reloj global** que permita ordenar eventos de forma universal.
- Se requieren mecanismos que permitan establecer **orden lógico** entre acciones distribuidas.

## 🕒 2. Relojes Lógicos de Lamport
Los relojes lógicos fueron propuestos para determinar un **orden parcial** entre eventos en sistemas distribuidos.

## 🔐 3. Exclusión Mutua Distribuida (EMD)
La EMD busca garantizar que **solo un proceso acceda a la Sección Crítica (SC)** evitando inconsistencias.
