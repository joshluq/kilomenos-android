# Product Requirements Document: Optimización y Aceleración del Arranque de la Aplicación (Zero-Network Cold Start)

**Feature ID**: KILOMENOS-15  
**Jira Issue**: KILOMENOS-15  
**Version**: 1.0.0  
**Status**: APPROVED  
**Author**: Product Owner  
**Date**: 2026-09-26  
**Target Release**: v1.2.2  

---

## 1. Executive Summary & Problem Statement

### 1.1 Problem Statement
Actualmente, los usuarios con sesión activa (especialmente los usuarios PREMIUM con múltiples vehículos, cientos de registros de odómetro y gastos de combustible) experimentan un tiempo de espera inaceptablemente prolongado en la pantalla de inicio (`LaunchScreen`). 

Según el análisis exhaustivo de los logs de telemetría extraídos de producción (página de Confluence *"Logs de Arranque"*), el arranque en frío bloquea la interfaz durante **11.88 segundos** antes de mostrar el `Dashboard`:
1. **00:57:31.706**: Inicio del proceso de la aplicación.
2. **00:57:31.861**: La sesión del usuario y los derechos de suscripción (`PREMIUM`) ya se leen localmente desde el almacenamiento cifrado (`TokenRepository` + Tink AEAD) en **~150ms**.
3. **00:57:32.280**: `CheckSessionUseCase` confirma `ActiveSession`. A partir de este momento, `LaunchViewModel` secuestra el hilo de ejecución llamando a `fetchInitialData()`.
4. **Cuello de Botella 1 (Sincronización forzada de Entitlements)**: `getEntitlementsUseCase(forceRefresh = true)` ejecuta una renovación forzada de token (`AuthTokenRefresher`, 2.05s) y consulta remota a Supabase, bloqueando la pantalla durante **6.87 segundos** (hasta 00:57:39.152), a pesar de que los derechos ya estaban en caché local.
5. **Cuello de Botella 2 (Full Deep Sync bloqueante de Contratos, Historial y Gastos)**: `syncContractsUseCase(SyncContractsUseCase.Input)` descarga de forma síncrona en cascada:
   - Contratos y Estaciones de servicio en paralelo (1.78s).
   - Historial completo del contrato activo: **203 registros remotos** (1.70s).
   - Gastos de combustible: **9 registros remotos** (2.63s).
   - Duración total del Deep Sync bloqueante: **4.46 segundos** (hasta 00:57:43.616).
6. **Cuello de Botella 3 (Retardo artificial)**: `delay(500.milliseconds)` arbitrario en `LaunchViewModel` antes de navegar.
7. **00:57:44.163**: Navegación final a `Dashboard` (casi 13 segundos tras el arranque).

### 1.2 Diagnóstico de Redundancia y Anti-patrón
- **Violación de Arquitectura Cache-First / Offline-First**: KmSafe ya cuenta con Room Database como Single Source of Truth. Cuando el `Dashboard` y `OverviewViewModel` se cargan a las 00:57:44.5, leen todos sus datos (contrato activo, métricas, proyecciones y últimos 203 registros) directamente de las tablas locales de Room en menos de **15ms**.
- **Duplicidad de Procesos en Background**: A las 00:57:32.811, `MainActivity` ya encola `SyncWorker` (WorkManager) para ejecutar en segundo plano la sincronización y migración (`MigrateLocalData`). Bloquear la UI en `LaunchViewModel` duplica innecesariamente el tráfico de red y la contención de base de datos.
- **Incapacidad de uso Offline**: Un usuario con sesión activa en un garaje o zona con baja cobertura sufre bloqueos o pantallas congeladas durante el arranque, a pesar de tener todos sus datos en el almacenamiento local.

### 1.3 Value Proposition
- **Aceleración Radical del Arranque (Zero-Network Cold Start)**: Reducir el Time-to-Interactive (TTI) de **~12 segundos a < 800ms**.
- **Desacoplamiento Estricto de Responsabilidades**:
  - `LaunchScreen`: Limitar su responsabilidad única a validar la sesión local en dispositivo (`CheckSessionUseCase`). Si la sesión es activa (`ActiveSession`), navegar de inmediato al `Dashboard` sin esperar respuestas HTTP.
  - Sincronización Remota: Mover la sincronización de contratos, estaciones, historial y gastos a segundo plano no bloqueante (`SyncWorker` / Job asíncrono) y/o carga a demanda (lazy loading) al acceder a las pestañas correspondientes (`HistoryScreen`, `ExpensesScreen`).
  - Sincronización de Entitlements: Validación en segundo plano (silent refresh) que actualiza reactivamente el `UserSessionDataSource` sin congelar la experiencia visual.

---

## 2. Target Personas
- **Primary Persona**: Conductor y usuario recurrente (Free y Premium) de KmSafe que abre la aplicación diariamente para registrar kilometraje al subir o bajar del vehículo.
- **User Pain Point**: Frustración ante una pantalla de carga inmóvil de más de 10 segundos cada vez que abre la app, transmitiendo sensación de pesadez y lentitud.
- **Usage Frequency / Environment**: Apertura rápida antes o después de conducir; frecuentemente en parkings subterráneos, zonas rurales o situaciones con cobertura intermitente o nula.

---

## 3. User Stories (INVEST)

- **US-01 (Arranque Instantáneo Cache-First)**: Como usuario con sesión activa, quiero que la aplicación abra el Dashboard de forma casi instantánea al pulsar el icono, para poder consultar mis datos y registrar kilometraje sin esperar a que se descargue todo mi historial por internet.
- **US-02 (Sincronización Transparente en Segundo Plano)**: Como usuario Premium con múltiples vehículos y cientos de registros, quiero que la sincronización con la nube ocurra en segundo plano sin interrumpir mi navegación, para que mis datos se mantengan al día sin congelar la pantalla.
- **US-03 (Acceso Confiable Sin Conexión)**: Como conductor en un garaje subterráneo sin cobertura, quiero que la aplicación inicie y me permita operar con mis datos locales sin mostrar pantallas de carga infinitas ni errores bloqueantes.
- **US-04 (Carga a Demanda de Histórico y Gastos)**: Como usuario que navega a las pantallas de Historial o Gastos, quiero que los datos locales se muestren al instante y se actualicen incrementalmente mediante pull-to-refresh o refresco en segundo plano, para optimizar el consumo de batería y datos móviles.

---

## 4. Functional Requirements

- **FR-01**: [Eliminación de Sincronización Bloqueante en Launch] — `LaunchViewModel` no debe invocar `syncContractsUseCase` ni ninguna operación de sincronización de red profunda antes de disparar `LaunchEffect.NavigateToDashboard`.
- **FR-02**: [Eliminación de Refresco Forzado de Entitlements en Launch] — `LaunchViewModel` debe confiar en la sesión y derechos de suscripción ya persistidos en el almacenamiento local cifrado (`UserSessionDataSource`), eliminando el parámetro `forceRefresh = true` en la ruta crítica de arranque.
- **FR-03**: [Navegación Inmediata con Sesión Activa] — Al emitirse `CheckSessionUseCase.Output.ActiveSession`, el sistema debe transicionar inmediatamente a `NavigateToDashboard`, eliminando retardos artificiales arbitrarios (`delay`).
- **FR-04**: [Delegación de Sincronización a Background Worker] — Toda la sincronización remota de contratos, estaciones, historial y gastos debe ser orquestada de forma no bloqueante por `SyncWorker` (WorkManager) con restricciones de conectividad de red adecuadas.
- **FR-05**: [Refresco a Demanda en Pestañas Detalle] — Las vistas de Historial (`HistoryScreen`) y Gastos (`ExpensesScreen`) deben implementar sincronización incremental / a demanda (e.g. `pull-to-refresh`) permitiendo al usuario forzar una recarga sin penalizar el inicio global.
- **FR-06**: [Preservación del Manejo de Sesión Inconsistente] — Si `CheckSessionUseCase` detecta `InconsistentSession` o `IdleSession`, se mantendrá el flujo de cierre forzado de sesión y redirección segura a `NavigateToLogin`.
- **FR-07**: [Notificación Unificada ante Degradación PREMIUM -> FREE] — Cuando el sistema detecte la transición de suscripción de PREMIUM a FREE, ya sea mediante la sincronización asíncrona de Entitlements en segundo plano o notificada en caliente a través de una notificación push (FCM), el sistema debe comunicarlo inmediatamente al usuario utilizando el mismo mecanismo de feedback unificado: aprovechando el componente `AppOverlay` (HUD no destructivo o diálogo informativo) o un mensaje en pantalla, explicando la transición a FREE y las características ajustadas.

### 4.1 Reglas de Negocio con Consecuencia Explícita (BR-xx)

| ID Regla | Enunciado de Regla | Condición de Fallo / Violación | Consecuencia en Sistema / Feedback UI |
|---|---|---|---|
| **BR-01** | La navegación a `Dashboard` no puede depender del estado ni latencia de la red si existe sesión local válida. | Red no disponible, latencia > 500ms o error 5xx en backend. | El sistema navega al Dashboard utilizando Room DB; no se muestra error ni spinner de red. |
| **BR-02** | Los derechos de suscripción (Entitlements) se leen del caché local durante el inicio. El feedback visual solo se activa ante la degradación exclusiva de PREMIUM a FREE. | Notificación push en caliente o sincronización en background detecta cambio a FREE. | La app arranca con el último nivel persistido (`PREMIUM`); al confirmar la degradación a `FREE` (vía push o sync), se activa de forma idéntica el `AppOverlay` o mensaje informativo sin cerrar la sesión. |
| **BR-03** | La sincronización remota de WorkManager no debe competir con el renderizado inicial del Dashboard. | Ejecución inmediata en el hilo principal o contención de Room DB. | `SyncWorker` se ejecuta en `Dispatchers.IO` con prioridad diferida para no provocar frame drops en el arranque. |
| **BR-04** | Los registros remotos (historial y gastos) no se borran si falla la sincronización en background. | Excepción de red (HTTP 500, SocketTimeout). | Los registros locales permanecen intactos; se encola un reintento exponencial en WorkManager. |

---

## 5. Acceptance Criteria (Given / When / Then)

### AC-01: Cold Start Instantáneo con Sesión Activa
- **Given** un usuario autenticado con sesión activa y datos previamente almacenados en Room DB,
- **When** el usuario abre la aplicación desde un estado detenido (Cold Start),
- **Then** el sistema evalúa la sesión localmente y emite `LaunchEffect.NavigateToDashboard` en un tiempo total de ejecución inferior a **800ms**, sin realizar peticiones de red bloqueantes en `LaunchScreen`.

### AC-02: Disponibilidad y Arranque en Modo Offline
- **Given** un usuario con sesión activa en un dispositivo en Modo Avión o sin conectividad de red,
- **When** se inicia la aplicación,
- **Then** la aplicación carga exitosamente el `Dashboard` mostrando los vehículos, métricas y contratos locales sin mostrar diálogos de error de red ni quedarse en bucle de carga.

### AC-03: Sincronización en Segundo Plano Sin Afectar Fluidez UI
- **Given** el usuario ya ha navegado al `Dashboard`,
- **When** `SyncWorker` ejecuta la sincronización de contratos, estaciones, historial y gastos en segundo plano,
- **Then** la UI mantiene una tasa de refresco constante (>= 60 fps, frames <= 16ms) sin congelamiento ni tirones (jank).

### AC-04: Carga de Datos Reactiva en Overview
- **Given** que `SyncWorker` descarga nuevos registros de odómetro o gastos mientras el usuario está en el `Dashboard`,
- **When** Room DB actualiza las tablas locales,
- **Then** las métricas de `OverviewScreen` se actualizan reactivamente a través de los `Flow` de Room sin requerir reinicio de la app.

### AC-05: Redirección Correcta ante Sesión Inválida o Ausente
- **Given** un usuario sin sesión activa (`IdleSession`) o con tokens corruptos (`InconsistentSession`),
- **When** se inicia la aplicación,
- **Then** el sistema no intenta sincronizar datos y redirige limpiamente a `NavigateToLogin`.

### AC-06: Feedback al Usuario ante Degradación PREMIUM -> FREE en Background Sync
- **Given** un usuario que inició la aplicación con derechos de suscripción cacheados localmente como `PREMIUM`,
- **When** la sincronización asíncrona de Entitlements en segundo plano detecta que el estado de suscripción ha pasado a `FREE`,
- **Then** el sistema actualiza de forma reactiva el estado de la sesión y muestra un mensaje contextual o aprovecha el componente `AppOverlay` (HUD no destructivo o diálogo informativo) informando al usuario sobre la actualización al nivel FREE y sus características sin forzar el cierre de la aplicación.

### AC-07: Feedback al Usuario ante Notificación Push en Caliente (PREMIUM -> FREE)
- **Given** un usuario activo interactuando con la aplicación con suscripción `PREMIUM` en primer plano (foreground),
- **When** se recibe en caliente una notificación push (FCM) informando la expiración o revocación de la suscripción hacia `FREE`,
- **Then** el sistema procesa el evento, actualiza el estado de la sesión y despliega exactamente el mismo mecanismo (`AppOverlay` o diálogo informativo de suscripción) presentado en AC-06 para notificar la transición al nivel FREE sin forzar el reinicio de la app.

---

## 6. Non-Functional Requirements (Android Constraints)

- **Min SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 35 (Android 15)
- **Offline Capability**: **MANDATORY / REQUIRED** (la app debe operar 100% con datos locales para usuarios con sesión activa previa).
- **Performance Budget**:
  - **Cold Start Time-to-Interactive (TTI)**: <= 800ms (reducción del 93% frente a los 11.88s actuales).
  - **Network Calls on Startup Path**: **0 llamadas bloqueantes** en `LaunchScreen`.
  - **Rendering Budget**: 60 fps (frames <= 16ms) en transiciones de navegación.
  - **Memory Footprint**: Reducción de pico de memoria al arranque en frío al evitar instanciar y mapear colecciones masivas de DTOs en memoria simultáneamente.
- **Accessibility Standards**:
  - Mantener soporte TalkBack y touch targets >= 48x48dp en `LaunchScreen` y `DashboardScreen`.
- **Security & Privacy**:
  - Las lecturas de sesión en arranque continúan utilizando cifrado simétrico mediante Google Tink AEAD sin degradación criptográfica.
  - Cero tokens de autenticación o identificadores personales expuestos en Logcat.

---

## 7. Dudas Abiertas y Decisiones Acordadas (HITL Gate)

| ID Duda | Pregunta / Aspecto Evaluado | Decisión Final Acordada | Estado |
|---|---|---|---|
| **D-01** | ¿Qué ocurre si un usuario fue degradado de PREMIUM a FREE en la nube mientras la app estaba cerrada o mientras la app está abierta en primer plano? | Se asume el estado local en el arranque para no bloquear al usuario. Se confirma que **exclusivamente para la transición PREMIUM -> FREE**, tanto si se detecta en la sincronización en segundo plano de Entitlements como si se recibe en caliente vía notificación Push (FCM), se utilizará el mismo mecanismo unificado: mostrar un mensaje informativo o aprovechar `AppOverlay` para informar con claridad al usuario del cambio a FREE y sus nuevas condiciones. | **RESUELTA** |
| **D-02** | ¿Se deben descargar todos los registros de historial al arrancar la app? | No. El Dashboard solo necesita el último registro de odómetro para el kilometraje actual y la agregación mensual (que ya está calculada en Room). La descarga masiva de históricos de viajes debe ser en background o paginada/on-demand al entrar en `HistoryScreen`. | **RESUELTA** |
| **D-03** | ¿Es viable eliminar completamente el retardo artificial de 500ms en `LaunchViewModel`? | Sí, el splash screen del sistema Android 12+ (`SplashScreen` API) ya gestiona la transición visual; añadir un `delay()` manual de 500ms solo penaliza la experiencia de usuario. | **RESUELTA** |

---

## 8. Out of Scope
- Rediseño visual o gráfico del Splash Screen o LaunchScreen.
- Modificación de los esquemas de tablas en Room Database o Supabase.
- Cambios en las reglas de cálculo matemático de proyecciones o consumos en `OverviewViewModel`.
