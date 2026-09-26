# Product Requirements Document: Optimización Local-First en el Registro de Odómetro

**Feature ID**: KILOMENOS-16  
**Version**: 1.0.0  
**Status**: APPROVED  
**Author**: Product Owner  
**Date**: 2026-09-26  
**Target Release**: v1.4.0  

---

## 1. Executive Summary & Problem Statement

- **Problem**: Actualmente en KmSafe, al registrar kilómetros desde el BottomSheet de `OverviewScreen` o al confirmar un trayecto asistido, el caso de uso `AddOdometerRecordUseCase` ejecuta una llamada HTTP síncrona contra Supabase antes de confirmar el éxito a la interfaz de usuario. En escenarios habituales de conducción (parkings subterráneos, túneles, garajes o zonas rurales con baja o nula cobertura móvil), la UI permanece bloqueada en estado *"Guardando..."* durante 5 a 15 segundos hasta que la red responde o alcanza el timeout, impidiendo cerrar el modal, frustrando al conductor y generando riesgo de abandonos o pulsaciones duplicadas.
- **Value Proposition**: Transformar el registro de odómetro en una experiencia **Local-First optimista pura**. La aplicación validará los datos en cliente, los insertará de forma inmediata en la base de datos local Room DB (< 5ms) y cerrará el BottomSheet instantáneamente (< 30ms). El balance, odómetro actual y proyecciones se recalcularán en tiempo real mediante los `Flow` reactivos de Room, mientras que la sincronización remota con el backend se despacha de forma asíncrona y desacoplada en segundo plano a través de WorkManager / `SyncManager`, garantizando cero fricción y cero bloqueos de red.

---

## 2. Target Personas

- **Primary Persona**: Conductor de renting / particular (Carlos, 34 años).
- **User Pain Point**: Acaba de aparcar en el garaje subterráneo de su casa o trabajo (sin cobertura 4G/5G). Abre KmSafe para apuntar los kilómetros del trayecto, pero el botón "Guardar" se queda en bucle de carga indefinido y no puede cerrar la pantalla hasta que sale a la calle o la app falla por timeout.
- **Usage Frequency / Environment**: Uso diario o semanal, recurrentemente en entornos de conectividad intermitente o nula (sótanos, zonas de montaña, parkings).

---

## 3. User Stories

- **US-01**: Como conductor, quiero registrar mis kilómetros recorridos de forma inmediata y sin esperas, para que el modal se cierre al instante tras pulsar "Guardar" independientemente de la calidad de mi cobertura móvil.
- **US-02**: Como conductor, quiero que mis métricas de balance, kilometraje real y proyección se actualicen al milisegundo en la pantalla principal tras guardar, para verificar inmediatamente mi estado respecto al contrato.
- **US-03**: Como conductor en zonas sin cobertura o modo avión, quiero que mis registros se almacenen con seguridad en mi dispositivo y se sincronicen automáticamente en segundo plano cuando recupere la conexión, sin pérdida de datos.
- **US-04**: Como conductor, si me equivoco y elimino un registro que aún no se ha subido a la nube, quiero que se cancele limpiamente sin generar errores de sincronización ni registros duplicados.

---

## 4. Functional Requirements

- **FR-01: Persistencia Local Inmediata**: El sistema persistirá el nuevo registro de odómetro en la base de datos local Room DB con estado `SyncStatus.PENDING` en un tiempo de ejecución inferior a 10ms.
- **FR-02: Cierre Instantáneo de UI**: El BottomSheet de registro de odómetro se cerrará y emitirá confirmación de éxito a la UI en un tiempo total inferior a 30ms tras la pulsación del usuario, sin esperar a la finalización de llamadas de red.
- **FR-03: Recálculo Reactivo de Métricas**: El Dashboard (`OverviewViewModel`) recalculará y mostrará reactivamente el nuevo kilometraje total, balance contractual y gráfica de proyecciones a través del `Flow` de Room sin requerir recarga manual ni reinicio de la app.
- **FR-04: Sincronización Asíncrona Desacoplada**: El sistema delegará la sincronización remota a un Job asíncrono o a WorkManager (`SyncManager`), ejecutándose en segundo plano en `Dispatchers.IO` sin interferir con la navegación del usuario.
- **FR-05: Idempotencia por UUID de Cliente**: Todo registro se creará con un identificador UUID generado por el cliente móvil (`client-generated UUID`), el cual será respetado por el backend para evitar colisiones, duplicados o swaps de ID que puedan romper la relación con rutas GPS (`TripRoute`).
- **FR-06: Gestión Segura de Borrado y Edición Local**: Si un registro marcado como `SyncStatus.PENDING` es eliminado localmente antes de sincronizarse con el backend, se purgará de Room y se cancelará su sincronización remota sin emitir llamadas `DELETE` huérfanas al servidor.

---

### 4.1 Reglas de Negocio con Consecuencia Explícita (BR-xx)

| ID Regla | Enunciado de Regla | Condición de Fallo / Violación | Consecuencia en Sistema / Feedback UI |
|---|---|---|---|
| **BR-01** | Todo registro de odómetro debe poseer un valor positivo (`> 0`) y una fecha dentro de la vigencia del contrato activo. | Valor `<= 0`, formato no numérico o fecha fuera del rango `[startDate..endDate]`. | El sistema rechaza la operación antes de tocar Room DB; muestra error contextual localizado en el campo correspondiente del formulario. |
| **BR-02** | La confirmación de éxito hacia la UI es independiente de la conectividad y disponibilidad del backend. | Pérdida de conexión, latencia > 500ms o error 5xx en backend. | El registro permanece en Room como `SyncStatus.PENDING`. La UI se cierra con éxito y `SyncManager` encola reintentos con backoff exponencial. |
| **BR-03** | Si un registro local `PENDING` es eliminado por el usuario, no debe generarse tráfico de red al servidor. | Eliminación de registro que nunca fue confirmado como `SYNCED` por el backend. | El registro se elimina físicamente de Room DB y se purga de la cola de sync; no se envía `DELETE` HTTP a Supabase (evita errores `404 Not Found`). |
| **BR-04** | Los registros con estado `PENDING` deben protegerse ante cierres de sesión no intencionados. | Usuario intenta cerrar sesión (`SignOut`) teniendo registros locales no sincronizados. | `EvaluateIdentityConflictUseCase` detecta registros pendientes y muestra diálogo de advertencia informando al usuario del riesgo de pérdida antes de purgar datos. |

---

## 5. Acceptance Criteria (Given / When / Then)

### AC-01: Registro Instantáneo y Cierre de UI (< 30ms)
- **Given** un usuario autenticado con contrato activo en la pantalla `OverviewScreen`,
- **When** introduce un kilometraje válido (ej. "45") y pulsa el botón "Guardar Registro",
- **Then** el sistema inserta el registro en Room DB como `SyncStatus.PENDING`, cierra inmediatamente el BottomSheet en menos de **30ms** y recalcula reactivamente el balance y odómetro en el Dashboard, mientras la sincronización remota se ejecuta en segundo plano.

### AC-02: Disponibilidad y Persistencia en Modo Offline
- **Given** un usuario en Modo Avión o en un garaje subterráneo sin cobertura de red,
- **When** registra un kilometraje válido desde el BottomSheet,
- **Then** el registro se almacena exitosamente en Room DB, la UI se cierra sin mostrar alertas de error de red ni spinners bloqueantes, y el nuevo kilometraje se refleja de inmediato en el Dashboard local.

### AC-03: Sincronización Automática al Recuperar Conectividad
- **Given** uno o más registros de odómetro almacenados localmente con `SyncStatus.PENDING`,
- **When** el dispositivo recupera la conexión a Internet o se dispara el worker en segundo plano,
- **Then** `SyncWorker` / `SyncManager` envía las solicitudes `POST` con los UUIDs locales, el servidor responde `HTTP 200/201` y los registros se actualizan localmente a `SyncStatus.SYNCED`.

### AC-04: Eliminación Limpia de Registro PENDING sin Tráfico de Red
- **Given** un registro de odómetro en Room DB con `SyncStatus.PENDING` que aún no ha sido sincronizado con el servidor,
- **When** el usuario pulsa "Eliminar" desde la pantalla de Historial,
- **Then** el registro se elimina inmediatamente de la base de datos local y el sistema no envía ninguna petición `DELETE` HTTP al backend.

### AC-05: Rechazo de Inputs Inválidos sin Escritura en Base de Datos
- **Given** el BottomSheet de registro abierto,
- **When** el usuario introduce un valor no numérico, menor o igual a cero, o una fecha fuera de contrato,
- **Then** el sistema no ejecuta ninguna inserción en Room DB, mantiene el BottomSheet abierto y muestra el mensaje de error de validación correspondiente.

---

## 6. Non-Functional Requirements (Android Constraints)

- **Min SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 35 (Android 15)
- **Offline Capability**: **MANDATORY / REQUIRED** (la acción de guardar kilometraje debe funcionar 100% desconectada).
- **Performance Budget**:
  - **Latencia de Cierre de UI (Save-to-Dismiss TTI)**: <= 30ms (reducción del 99% frente a los 5-15s actuales).
  - **Tiempo de Escritura Local en Room**: <= 5ms en `Dispatchers.IO`.
  - **Bloqueo en Hilo Principal**: 0ms (cero I/O o cálculo pesado en `Dispatchers.Main`).
  - **Network Payload**: Uso del Client-Generated UUID como Primary Key para máxima eficiencia sin roundtrips de resolución de IDs.
- **Accessibility Standards**:
  - Mantener accesibilidad TalkBack y touch targets >= 48x48dp en botones de guardado y campos del BottomSheet.
- **Security & Privacy**:
  - Cero datos sensibles o PII expuestos en logs durante la sincronización en background.

---

## 7. Dudas Abiertas Resueltas (HITL Gate)

| ID Duda | Pregunta / Aspecto Evaluado | Decisión Final Acordada | Estado |
|---|---|---|---|
| **D-01** | ¿El backend acepta y persiste el UUID generado por el cliente móvil como Primary Key definitiva? | **SÍ**. El usuario confirmó expresamente que el backend acepta el ID generado por el cliente móvil. Esto elimina la necesidad de reconciliación de IDs (`SyncIdHandler`) y previene carreras con rutas GPS (`TripRoute`). | **RESUELTA** |
| **D-02** | ¿Qué ocurre si la llamada remota falla por error de red temporal tras cerrar el BottomSheet? | El registro permanece en Room con `SyncStatus.PENDING`. `SyncManager.scheduleSync()` programa un trabajo en WorkManager con reintentos exponenciales automáticos al recuperar red. | **RESUELTA** |
| **D-03** | ¿Se debe permitir eliminar o editar registros que aún no se han subido a la nube? | Sí. Si el registro está en estado `PENDING`, la edición actualiza el registro localmente, y la eliminación lo purga directamente de Room sin enviar llamadas HTTP al servidor. | **RESUELTA** |

---

## 8. Out of Scope

- Modificación gráfica o rediseño visual del BottomSheet de odómetro.
- Modificación de las fórmulas matemáticas de cálculo de balance en `CalculateContractMetricsUseCase`.
- Cambios en el esquema de tablas SQL en Room Database o Supabase.
