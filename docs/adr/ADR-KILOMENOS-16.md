# ADR-KILOMENOS-16: Persistencia Local-First Optimista y Desacoplamiento de Sincronización en el Registro de Odómetro

**Feature ID**: KILOMENOS-16  
**Status**: ACCEPTED  
**Deciders**: Mobile Software Architect (Android)  
**Date**: 2026-09-26  
**Technical Stakeholders**: Senior Android Developer, QA/Testing Engineer, Product Owner  

---

## 1. Context and Problem Statement

En KmSafe, la acción transaccional más frecuente para los usuarios es registrar un incremento de kilometraje al finalizar un viaje desde el BottomSheet de `OverviewScreen`.
Hasta ahora, la implementación de `AddOdometerRecordUseCase` y `HistoryRepositoryImpl.saveRecord` realizaba una llamada HTTP síncrona `apiService.addOdometerRecord(...)` dentro de la misma función suspendida antes de retornar a la capa de presentación.

En condiciones reales de movilidad (garajes subterráneos, túneles, parkings bajo nivel o zonas rurales sin cobertura 4G/5G), esto provocaba que la interfaz se congelara en estado de carga (*"Guardando..."*) durante 5 a 15 segundos hasta recibir respuesta del servidor o alcanzar el timeout de socket. Esto degrada severamente la experiencia de usuario, induce a dobles pulsaciones y contradice los principios de una arquitectura *Offline-First*.

Se requiere establecer una arquitectura **Local-First Optimista** donde la escritura en la base de datos local Room DB confirme el éxito inmediatamente a la UI (< 30ms), cerrando el BottomSheet y recalculando métricas en tiempo real, mientras la sincronización remota se ejecuta de forma asíncrona y desacoplada en segundo plano.

---

## 2. Decision Drivers

- **Latencia de Interacción Ultrabaja**: El conductor debe recibir confirmación y ver cerrada la hoja modal en menos de 30ms.
- **Disponibilidad Offline 100% Garantizada**: La persistencia local y el recálculo de balance en el Dashboard deben operar sin importar la presencia o calidad de la red.
- **Idempotencia Garantizada (Client-Generated UUID)**: El backend acepta y persiste el identificador UUID generado en el dispositivo móvil como Primary Key definitiva, eliminando reconciliaciones de ID o riesgos de desvinculación con rutas GPS (`TripRoute`).
- **Resiliencia en Concurrencia y Outbox Local**: Las ediciones o borrados de registros locales en estado `SyncStatus.PENDING` deben gestionarse limpiamente sin provocar llamadas HTTP huérfanas (ej. `DELETE` de un registro inexistente en el servidor).
- **Consistencia Reactiva en Presentación**: `OverviewViewModel` y `CalculateContractMetricsUseCase` deben continuar operando como Single Source of Truth reactivo alimentado por los `Flow` de Room DB.

---

## 3. Considered Architectural Options

1. **Opción 1: Mantenimiento del Guardado Síncrono Bloqueante con Reducción de Timeout**:
   - *Pros*: Código existente sin cambios estructurales en el repositorio.
   - *Cons*: Mantiene el bloqueo del hilo en el BottomSheet; no soluciona el problema de experiencia en parkings subterráneos. Rechazada.

2. **Opción 2: Local-First Optimista con Desacoplamiento Asíncrono de Sincronización Remota (Elegida)**:
   - *Pros*:
     - La escritura en Room DB se completa en < 5ms y emite `Output.Success` a la UI de inmediato.
     - El BottomSheet se cierra instantáneamente (< 30ms).
     - El Dashboard recalcula y renderiza el nuevo odómetro y balance de kilómetros al milisegundo mediante Room `Flow`.
     - La llamada remota a Supabase se despacha en segundo plano (corutina desacoplada en `ProcessLifecycleScope` o encolado en `SyncManager`/WorkManager).
     - Al fallar por desconexión, el registro permanece como `SyncStatus.PENDING` y WorkManager lo sincroniza automáticamente con backoff exponencial.
   - *Cons*: Requiere asegurar que si el usuario elimina un registro `PENDING` antes del sync, se purgue de Room y se cancele el `DELETE` remoto.

3. **Opción 3: Cola de Mensajes Persistente Completa (Outbox Table dedicada)**:
   - *Pros*: Máxima formalidad transaccional.
   - *Cons*: Sobrecarga innecesaria de tablas y complejidad, dado que la entidad `OdometerRecordEntity` ya posee el campo `syncStatus` (`PENDING`, `SYNCED`, `FAILED`).

---

## 4. Decision Outcome

- **Chosen Option**: **Opción 2** — Local-First Optimista con Desacoplamiento Asíncrono de Sincronización.
- **Architecture Pattern**: `CLEAN_ARCHITECTURE` + `LOCAL_FIRST_OPTIMISTIC_UDF`
- **Justification**: Permite la máxima reactividad y fluidez percibida por el conductor, aprovecha la infraestructura ya existente de Room y `SyncStatus`, y respeta el principio de Room como Single Source of Truth.

---

## 5. System Topology & Component Interactions

```
 ┌──────────────────────────────────────────────────────────┐
 │ OverviewViewModel.handleSaveRecord(timestamp)            │
 └────────────────────────────┬─────────────────────────────┘
                              │ Invokes AddOdometerRecordUseCase
                              ▼
 ┌──────────────────────────────────────────────────────────┐
 │ AddOdometerRecordUseCaseImpl.invoke(input)               │
 └────────────────────────────┬─────────────────────────────┘
                              │ Generates UUID & passes to Repository
                              ▼
 ┌──────────────────────────────────────────────────────────┐
 │ HistoryRepositoryImpl.saveRecord(record, route)          │
 │ 1. Inserts into Room DB (syncStatus = PENDING)           │
 │ 2. Inserts TripRoute if present                          │
 └────────────────────────────┬─────────────────────────────┘
                              │
               ┌──────────────┴─────────────────────────────┐
               │ (Immediate return / emit Success)          │ (Dispatched in background)
               ▼                                            ▼
 ┌──────────────────────────────────────────┐ ┌──────────────────────────────────────────┐
 │ OverviewViewModel receives Success       │ │ Remote Sync Dispatcher                   │
 │ - isSaving = false                       │ │ - Checks Cloud Sync & Active Session     │
 │ - showBottomSheet = false                │ │ - Executes apiService.addOdometerRecord  │
 │ - Clears tracking & notifications        │ │ - On Success: updates Room to SYNCED     │
 │ - Room Flow triggers instant metrics     │ │ - On Error: calls syncManager.schedule() │
 └──────────────────────────────────────────┘ └──────────────────────────────────────────┘
```

### Reglas de Eliminación y Edición:
- `deleteRecord(record)`:
  - Si `record.syncStatus == SyncStatus.PENDING`: Se ejecuta `dao.deleteRecord(record.toEntity())` y se retorna inmediatamente sin realizar petición HTTP `DELETE` remota.
  - Si `record.syncStatus == SyncStatus.SYNCED`: Se elimina de Room y se ejecuta la llamada remota `apiService.deleteOdometerRecord(record.id)`.
- `updateRecord(record)`:
  - Si `record.syncStatus == SyncStatus.PENDING`: Se actualizan los datos en Room manteniendo `syncStatus = PENDING`. La posterior sincronización enviará la versión consolidada.

---

## 6. Consequences & Tradeoffs

- **Positive Consequences**:
  - TTI de guardado pasa de ~10s a **< 30ms** en cualquier circunstancia de conectividad.
  - Operación 100% fiable en parkings subterráneos y garajes.
  - Cero dependencias de red en la ruta crítica del usuario en `OverviewScreen`.
  - Mantenimiento estricto de las claves foráneas con `TripRoute` mediante el Client-Generated UUID.
- **Negative Consequences**:
  - El estado local puede adelantarse brevemente al estado del backend hasta que WorkManager complete la sincronización asíncrona.
  - Requiere asegurar que el diálogo de cierre de sesión advierta si existen registros `PENDING` sin sincronizar.
