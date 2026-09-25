# Épica KILOMENOS-3: Gestión de Notificaciones — Arquitectura, Decisiones Técnicas y Conclusiones

**Clave de la Épica**: [KILOMENOS-3](https://joshluq-dev.atlassian.net/browse/KILOMENOS-3)  
**Título**: Gestión de Notificaciones y Sincronización de Entitlements  
**Estado Final**: Finalizada / Listo (10/10 tickets cerrados con veredicto PASS)  
**Fecha de Cierre**: 24 de Septiembre de 2026  
**Módulos Afectados**: `:feature:notifications`, `:core:domain`, `:core:infrastructure`, `:feature:overview`, `:feature:profile`, `:core:navigation`, `:app`, Backend Supabase (PostgreSQL, RLS, Edge Functions, Hono REST API), Firebase Cloud Messaging (FCM HTTP v1).

---

## 1. Resumen Ejecutivo y Alcance

La Épica **KILOMENOS-3** abordó una de las capacidades transversales más críticas de KmSafe: establecer un sistema unificado, robusto y resiliente para la recepción, persistencia, renderizado y sincronización de notificaciones y alertas en la aplicación móvil, resolviendo simultáneamente una brecha crítica de monetización.

### Problemas Fundamentales Resueltos:
1. **Brecha de Entitlements & Monetización (Background Gap)**: La aplicación únicamente refrescaba los derechos de suscripción (*entitlements*) en el arranque en frío (*cold start*). Si un usuario pasaba de Premium a Free (por expiración o fallo de cobro) mientras la app permanecía en segundo plano, retenía indebidamente el acceso Premium. Se implementó una invalidación push reactiva vía Firebase Cloud Messaging (FCM HTTP v1) disparada automáticamente desde el backend de Supabase.
2. **Ausencia de Canal Unificado de Alertas**: Coexistían componentes visuales fragmentados (`StatusCapsule` y banners efímeros en memoria) que no persistían en Room, impidiendo que el conductor tuviera un historial auditable de excesos de kilometraje o alertas de Bluetooth. Se construyó el módulo autónomo `:feature:notifications` con la píldora unificada `NotificationPill` y la bandeja de histórico `NotificationsListScreen`.
3. **Persistencia Híbrida Local-First**: Las notificaciones generadas localmente (proyecciones, odómetro, Bluetooth) operan al 100% sin conexión en SQLite (Room). Para usuarios Premium, se sincronizan bidireccionalmente hacia Supabase mediante un background worker idempotente.

---

## 2. Decisiones Arquitectónicas Clave (ADRs)

### ADR-1: Estrategia Híbrida Offline-First vs. Push-Only Remoto
* **Contexto**: Se evaluó si las alertas de exceso de kilometraje debían enviarse primero al backend para que este disparara un push FCM a la app.
* **Decisión**: Se adoptó la arquitectura **Offline-First**. Si un conductor circula en túneles o zonas rurales sin cobertura y supera el límite de kilometraje contratado, la alerta debe generarse de inmediato en la base de datos local (Room) con latencia cero (0ms). Las alertas locales se sincronizan al backend en segundo plano cuando hay conectividad. El canal remoto FCM se reserva para eventos originados en el servidor (facturación, suscripciones, avisos globales de flota).

### ADR-2: Optimistic UI y Navegación Asíncrona Inmediata (< 16ms)
* **Contexto**: Al pulsar una notificación en la píldora o en la lista, la app realizaba la llamada suspendida de marcado en red (`markNotificationAsReadUseCase`), bloqueando la navegación en la UI durante 1-2 segundos.
* **Decisión**: La navegación hacia `ProjectionAnalysisScreen` o `EditContractScreen` se despacha de forma **inmediata y optimista** en el hilo principal (`Dispatchers.Main`) cumpliendo el presupuesto de 60fps (< 16ms). La persistencia en Room y la llamada de red a la API de Supabase se ejecutan en segundo plano (`Dispatchers.IO`), asegurando que caídas de red o latencia nunca congelen la interacción.

### ADR-3: Deduplicación Semántica (`PublishNotificationIfUnreadUseCase`)
* **Contexto**: Los recálculos automáticos de telemetría en `OverviewViewModel` recreaban notificaciones idénticas que el usuario ya había marcado como leídas, resucitándolas en la cabecera.
* **Decisión**: Se introdujo el caso de uso `PublishNotificationIfUnreadUseCase` en `:core:domain`. Antes de insertar, verifica en Room si existe un registro previo con la misma clave semántica (`projection_key` / `deduplication_key`). Si ya fue leído (`isRead == true`), la publicación se omite silenciosamente.

### ADR-4: Scoping por Vehículo y Aislamiento de Navegación
* **Contexto**: Las notificaciones pertenecen al usuario pero tienen contexto de un vehículo específico (`contract_id`). Al pulsar una alerta de proyección perteneciente a un vehículo secundario mientras se visualizaba el vehículo activo, la app navegaba incorrectamente a la proyección del vehículo erróneo.
* **Decisión**: Se incorporó el badge visual del vehículo en `NotificationsListScreen`. Si la notificación pertenece a un vehículo distinto del actualmente seleccionado, se oculta el chevron de navegación y se bloquea el click, evitando inconsistencias analíticas.

### ADR-5: Desacoplamiento Estricto de Clean Architecture
* **Contexto**: La implementación del caso de uso residía erróneamente en `:core:infrastructure` debido a una dependencia directa sobre `NotificationDao`.
* **Decisión**: Se amplió el contrato de dominio `NotificationRepository` con el método `getNotificationBySemanticKey`, y se trasladó la implementación del caso de uso a `:core:domain`, garantizando que el dominio dependa únicamente de abstracciones puras de Kotlin sin ataduras a librerías de persistencia.

---

## 3. Matriz de Tickets Resueltos y Conclusiones Específicas

A continuación se detalla la trazabilidad completa de los 10 tickets integrados en la épica:

| Ticket | Tipo | Resumen del Alcance | Causa Raíz / Desafío Técnico | Solución Aplicada & Conclusión |
|---|---|---|---|---|
| **KILOMENOS-2** | Feature | Módulo Base de Notificaciones | Necesidad de canal unificado de alertas y sincronización de entitlements por FCM. | Creación del módulo autónomo `:feature:notifications`, componente interactivo `NotificationPill` en Overview, auto-read, bandeja `NotificationsListScreen` y recepción push en `KmFirebaseMessagingService`. |
| **KILOMENOS-4** | Feature | Credenciales Firebase Admin en Supabase | Desconexión en background ante downgrades de PREMIUM a FREE. | Configuración de Service Account de Google en backend, creación de tabla `user_fcm_tokens` con RLS, dispatcher FCM HTTP v1 idempotente con payload estructurado `data` + `notification`. |
| **KILOMENOS-5** | Error / Mejora | EntryPoint en Profile y Unificación UI | Coexistencia de `StatusCapsule` y `NotificationPill` desordenaba la cabecera; alertas locales eran efímeras. | Eliminación definitiva de `StatusCapsule`. Incorporación de acceso a Notificaciones en `ProfileScreen`. Formalización del ADR Offline-First para telemetría local. |
| **KILOMENOS-8** | Error | Navegación en Lista de Notificaciones | Clic en alertas de proyección abría `NotificationDetailScreen` vacía; bloqueo de red en navegación. | Enrutamiento contextual directo: alertas de proyección van a `ProjectionAnalysisScreen` y alertas de Bluetooth a `EditContractScreen`. Optimistic UI inmediata en `Dispatchers.Main`. |
| **KILOMENOS-9** | Feature | Backend CRUD en Supabase | Falta de persistencia cloud para historial de notificaciones de usuarios Premium. | Creación de tabla `user_notifications` con RLS estricto (`auth.uid() = user_id`), endpoints REST Hono para listado paginado, sync en lote (`/v1/notifications/sync`), marcado de lectura y guard de suscripción Premium. |
| **KILOMENOS-10** | Feature | Integración Local-First Mobile con Backend | Conectar la persistencia Room con los endpoints REST de Supabase. | Implementación de `NotificationRepositoryImpl` con soporte de `syncStatus` (`PENDING` vs `SYNCED`), `SyncNotificationsWorker` en WorkManager y DTOs remotos. |
| **KILOMENOS-11** | Error | Error al Leer Notificación | Fallos en la serialización y discrepancias en el estado de lectura entre Room y Supabase. | Corrección de mapeadores DTO, manejo resiliente de errores HTTP sin revertir la lectura optimista local en Room. |
| **KILOMENOS-12** | Feature | UX de NotificationPill y Prevención de Resurrección | El botón "Ver más" reducía el área táctil; los recálculos revivían alertas leídas. | Eliminación de "Ver más" haciendo la píldora un único target $\ge 48$dp. Creación de `PublishNotificationIfUnreadUseCase` con clave semántica en Room. Acceso al buzón vía campana en `OverviewTopBar`. Deprecación de flags obsoletos en Preferences. |
| **KILOMENOS-13** | Error | Lectura Asíncrona y syncStatus | Píldora de proyección no desaparecía al click; `syncStatus` se quedaba en `PENDING` tras HTTP 200. | Eliminación de fallback a notificaciones leídas en `OverviewViewModel`. Actualización atómica de `syncStatus = 'SYNCED'` en Room tras respuesta exitosa de red. Desacoplamiento total de la navegación respecto a llamadas HTTP. |
| **KILOMENOS-14** | Error | Pantalla en Blanco en Projection y Scoping de Coche | `Destination.ProjectionAnalysis` abría pantalla en blanco desde la pila raíz; falta de scoping por vehículo; violación de Clean Architecture. | Sincronización de `Destination.ProjectionAnalysis` con el tab `PROJECTION` de `DashboardNavigation`. Inclusión de badges de vehículo y bloqueo de navegación si la alerta es de otro coche. Reubicación de `PublishNotificationIfUnreadUseCaseImpl` a `:core:domain` desacoplado de DAO. |

---

## 4. Arquitectura del Flujo de Notificaciones

```
┌────────────────────────────────────────────────────────────────────────┐
│                          DISPARADORES DE ALERTAS                       │
├───────────────────────────────────┬────────────────────────────────────┤
│         LOCALES (Offline)         │          REMOTAS (Cloud)           │
│  - Proyección de Kilometraje      │  - Downgrade Suscripción           │
│  - Bluetooth Desvinculado         │  - Avisos Globales de Flota        │
│  - Telemetría de Odómetro         │  - Facturación / Pagos             │
└─────────────────┬─────────────────┴──────────────────┬─────────────────┘
                  │                                    │
                  ▼                                    ▼
       PublishNotificationIfUnread           KmFirebaseMessagingService
       (Deduplicación semántica)             (FCM HTTP v1 Payload)
                  │                                    │
                  └─────────────────┬──────────────────┘
                                    ▼
                       ┌─────────────────────────┐
                       │   NotificationRepository │
                       │       (Local-First)      │
                       └────────────┬────────────┘
                                    │
            ┌───────────────────────┴───────────────────────┐
            ▼                                               ▼
   Persistencia Local                             Sincronización Cloud
   SQLite (Room Database)                         SyncNotificationsWorker
   Estado: PENDING / SYNCED                       (Supabase REST / Solo Premium)
            │
            ▼
   ┌──────────────────────────────────────────────────────────────┐
   │                    CAPA DE PRESENTACIÓN                      │
   ├──────────────────────────────┬───────────────────────────────┤
   │ OverviewScreen               │ NotificationsListScreen       │
   │ (NotificationPill única)     │ (Historial cronológico)       │
   │ - Click $\to$ Optimistic Read│ - Badging por vehículo        │
   │ - Navegación directa < 16ms  │ - Chevron scoped a auto activo│
   └──────────────────────────────┴───────────────────────────────┘
```

---

## 5. Lecciones Aprendidas y Buenas Prácticas Establecidas

1. **La UI nunca debe esperar por la Red (Optimistic UI Innegociable)**: Toda acción iniciada por el usuario (marcar como leído, descartar, navegar) debe actualizar el estado local en SQLite y disparar el efecto de pantalla en `< 16ms`. La sincronización remota debe ser un efecto colateral tolerante a fallos en segundo plano.
2. **Las Claves Semánticas son Vitales en Sistemas Reactivos**: Cuando un ViewModel observa flujos de datos continuos (odómetros, cálculos de kilometraje diario), recalculará el estado con frecuencia. Sin una clave semántica (`semantic_key` / `deduplication_key`), el sistema caerá en el antipatrón de recrear notificaciones previamente descartadas o leídas.
3. **El Contexto de Entidad (Vehículo) debe Gobernarse desde el Dominio**: En aplicaciones multi-vehículo, las notificaciones no pueden tratarse como texto plano global; deben portar su asociación con el vehículo (`contract_id`) para evitar navegaciones cruzadas inconsistentes.
4. **Vigilancia Continua de Clean Architecture**: La necesidad de rapidez en bugfixes puede tentar a inyectar DAOs de persistencia directamente en los casos de uso. Mantener la regla de que el caso de uso solo conoce interfaces de repositorio en `:core:domain` garantiza que el código sea testeable con JUnit estándar sin emuladores ni bases de datos en memoria.

---

## 6. Estado de Certificación y Calidad

* **Suites de Tests Unitarios**: 100% aprobados en `:core:domain`, `:core:infrastructure`, `:feature:notifications` (14/14 tests) y `:feature:overview` (35/35 tests).
* **Verificación de Compilación**: Compilación limpia del binario Android `./gradlew :app:assembleDevDebug` con **0 errores**.
* **Estado en Jira**: Todos los tickets asociados a la Épica **`KILOMENOS-3`** se encuentran en estado `Finalizada / Listo`.
