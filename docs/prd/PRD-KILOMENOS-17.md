# Product Requirements Document: Sincronización Segura de Entitlements, Notificaciones Push FCM HTTP v1 y Gestión Contextual de Permisos

**Feature ID**: KILOMENOS-17  
**Version**: 1.0.0  
**Status**: APPROVED  
**Author**: Product Owner  
**Date**: 2026-09-27  
**Target Release**: v1.4.0  

---

## 1. Executive Summary & Problem Statement

- **Problem**: Tras el despliegue del webhook RTDN (Real-Time Developer Notifications) y el despachador FCM HTTP v1 en Supabase Edge Functions (versión 15/50), se detectó que los dispositivos Android no visualizaban las alertas de expiración/cancelación de suscripción cuando los usuarios no habían concedido el permiso `POST_NOTIFICATIONS` en Android 13+. Asimismo, en el cliente Android faltaba el registro del canal del sistema `subscription_alerts`, lo que provocaba que el sistema operativo descartara alertas críticas. A nivel de seguridad, si un usuario opera sin conexión o sin permisos push, existe una ventana de vulnerabilidad local donde la app podría continuar ofreciendo capacidades Premium si no cuenta con una estrategia de validación de triple capa (Zero Trust en backend + defensa local reactiva).
- **Value Proposition**: Implementar una solución integral de sincronización de *entitlements* y ciclo de vida de notificaciones push en KmSafe Android que combine:
  1. Configuración autoritativa del canal del sistema `subscription_alerts` (`IMPORTANCE_HIGH`).
  2. Ingesta desacoplada de payloads FCM HTTP v1 tanto en segundo plano como en primer plano.
  3. Estrategia contextual de solicitud de permisos `POST_NOTIFICATIONS` (*Pre-Permission Primer / In-App Rationale*) basada en psicología del conductor y protección vehicular.
  4. Escudo de seguridad de triple capa para derechos de suscripción: push reactivo, revalidación activa en `onResume` (`GET /v1/user/entitlements`), e interceptor de red para respuestas `HTTP 403 PREMIUM_REQUIRED`, eliminando cualquier riesgo de brecha offline.

---

## 2. Target Personas

- **Primary Persona**: Conductor particular o de flota con suscripción activa o en periodo de prueba (Laura, 29 años).
- **User Pain Point**: Su suscripción de Google Play expira o no se renueva por un fallo de tarjeta. Si no recibe aviso contextual y la app se bloquea de forma abrupta mientras conduce o intenta registrar un repostaje, experimenta frustración y desconfianza.
- **Secondary Persona**: Conductor en Android 13+ que por defecto rechaza los permisos de notificación del sistema porque teme recibir spam comercial en lugar de alertas críticas de su vehículo.

---

## 3. User Stories

- **US-01**: Como conductor en Android 8.0+, quiero que las alertas de cambio de plan y facturación se entreguen en un canal de notificación dedicado y de alta prioridad, para no perderme avisos críticos sobre la cobertura de mis viajes.
- **US-02**: Como conductor en Android 13+, quiero comprender mediante una explicación contextual clara por qué KmSafe necesita enviarme notificaciones antes de que el sistema operativo me pida el permiso, para otorgarlo con confianza y sin fricción.
- **US-03**: Como conductor con la app abierta en primer plano, cuando mi plan cambie en Google Play, quiero recibir una notificación visible no intrusiva y una actualización reactiva de mis derechos sin que la aplicación se congele ni pierda mis datos en curso.
- **US-04**: Como operador de plataforma / Product Owner, quiero que la app móvil verifique la validez de mis derechos de suscripción cada vez que se reanuda la sesión o cuando la API retorne un error de acceso `403 PREMIUM_REQUIRED`, para asegurar que las funcionalidades de pago estén estrictamente protegidas bajo un modelo Zero Trust.

---

## 4. Functional Requirements

- **FR-01: Inicialización del Canal de Notificaciones**: La aplicación inicializará obligatoriamente el canal de notificación del sistema `subscription_alerts` en API 26+ (`NotificationManager.IMPORTANCE_HIGH`) con luces, vibración y descripción comprensible antes de procesar cualquier mensaje FCM.
- **FR-02: Procesamiento de Payload FCM HTTP v1**: `KmFirebaseMessagingService` procesará el payload FCM HTTP v1 identificando `action_code == "REFRESH_ENTITLEMENTS"` o `event_type == "SUBSCRIPTION_DOWNGRADED"`, forzando la invalidación reactiva de la sesión local en Room y emitiendo el estado a la UI.
- **FR-03: Despacho Dual en Foreground / Background**:
  - En **Background**, el sistema operativo Android mostrará el banner del sistema directamente con el deep link `kmsafe://notifications?id=subscription_downgrade`.
  - En **Foreground**, el servicio persistirá la notificación en Room (`origin = REMOTE`) y construirá una notificación visible con `NotificationCompat.Builder` en el canal `subscription_alerts`.
- **FR-04: Pre-Permission Primer para POST_NOTIFICATIONS**: En dispositivos con API 33+ (Android 13+), la aplicación presentará una pantalla o diálogo contextual de sensibilización antes de disparar el diálogo del sistema operativo, destacando la protección contra penalizaciones de kilometraje.
- **FR-05: Revalidación Activa en onResume**: Al pasar a primer plano (`onResume`), el repositorio de sesión consultará de forma asíncrona y no bloqueante los derechos autoritativos al backend (`GET /v1/user/entitlements`), actualizando el almacenamiento local en Room/DataStore.
- **FR-06: Interceptor de Red para HTTP 403 (PREMIUM_REQUIRED)**: Un interceptor de red en OkHttp inspeccionará las respuestas HTTP: ante un código `403` con cuerpo o cabecera `PREMIUM_REQUIRED`, forzará de inmediato la degradación local a nivel `FREE` y notificará a la capa de presentación para mostrar el Paywall / Soft Landing.
- **FR-07: TTL de Caché Offline (24 Horas)**: La vigencia del estado Premium en caché local tendrá una duración máxima de 24 horas sin conexión validada; transcurrido ese tiempo, las acciones premium locales requerirán verificación online.

---

### 4.1 Reglas de Negocio con Consecuencia Explícita (BR-xx)

| ID Regla | Enunciado de Regla | Condición de Fallo / Violación | Consecuencia en Sistema / Feedback UI |
|---|---|---|---|
| **BR-01** | El canal de notificación `subscription_alerts` debe existir antes de recibir cualquier notificación push. | Dispositivo en Android 8.0+ sin canal registrado. | El SO descarta la notificación silenciosamente; el canal se registra en el arranque de la app en `Application.onCreate` o `Initializer`. |
| **BR-02** | Todo evento push con `action_code == "REFRESH_ENTITLEMENTS"` debe invalidar la sesión local de inmediato. | Retraso o fallo de persistencia en `UserSessionDataSource`. | Invocación asíncrona protegida en `serviceScope`; actualiza Room y emite a `StateFlow` sin bloquear el hilo principal. |
| **BR-03** | Una respuesta HTTP 403 con `PREMIUM_REQUIRED` tiene precedencia absoluta sobre el estado local. | Dispositivo en caché local `PREMIUM` recibe `403` en petición remota. | El interceptor degrada inmediatamente el perfil local a `FREE` y emite evento de degradación a la UI para mostrar el diálogo de Soft Landing. |
| **BR-04** | La solicitud de permisos `POST_NOTIFICATIONS` no debe ser bloqueante ni intrusiva en el primer arranque. | Usuario abre la app por primera vez tras la instalación. | El sistema posterga la solicitud contextual hasta que el usuario configura su primer vehículo o interactúa con las alertas. |

---

## 5. Acceptance Criteria (Given / When / Then)

### AC-01: Creación del Canal `subscription_alerts` al Iniciar la App
- **Given** un dispositivo con Android 8.0 o superior (API >= 26),
- **When** se inicializa la aplicación KmSafe (`KmApplication`),
- **Then** el sistema verifica la existencia del canal `"subscription_alerts"` y, si no existe, lo crea con importancia `IMPORTANCE_HIGH`, vibración activada y título `"Alertas de Suscripción"`.

### AC-02: Procesamiento de Push FCM HTTP v1 en Foreground con Alerta Visible
- **Given** la aplicación abierta en primer plano por el usuario,
- **When** `KmFirebaseMessagingService` recibe un mensaje FCM con `action_code == "REFRESH_ENTITLEMENTS"`, `title` y `body`,
- **Then** el servicio actualiza los *entitlements* locales a `FREE`, persiste la alerta en Room DB con `origin = REMOTE`, y emite una notificación en la barra de estado mediante `NotificationCompat.Builder` utilizando el canal `"subscription_alerts"`.

### AC-03: Navegación por Deep Link al Pulsar Notificación de Suscripción
- **Given** una notificación de degradación de suscripción en la bandeja del sistema con deep link `kmsafe://notifications?id=subscription_downgrade`,
- **When** el usuario pulsa sobre la notificación,
- **Then** el sistema abre la aplicación y navega directamente a la pantalla de detalle de notificación o Paywall correspondiente.

### AC-04: Revalidación Activa de Entitlements en onResume
- **Given** un usuario que reanuda la aplicación pasando de segundo plano a primer plano,
- **When** se ejecuta el evento de ciclo de vida `onResume`,
- **Then** el repositorio de sesión consulta de forma asíncrona `GET /v1/user/entitlements`; si el backend retorna `subscription_level: "FREE"`, el estado local se sincroniza inmediatamente sin interrumpir la interacción del usuario.

### AC-05: Degradación Automática por Interceptor ante HTTP 403 PREMIUM_REQUIRED
- **Given** un usuario con estado en caché local que intenta acceder a una función remota de pago,
- **When** el servidor Supabase responde con `HTTP 403 Forbidden` y código de error `PREMIUM_REQUIRED`,
- **Then** el interceptor de red de OkHttp captura la respuesta, actualiza el nivel de suscripción local a `FREE` en Room/DataStore y emite la acción correspondiente para desplegar el diálogo de Soft Landing en la UI.

### AC-06: Pre-Permission Primer para POST_NOTIFICATIONS en Android 13+
- **Given** un dispositivo con Android 13 o superior (API >= 33) que aún no tiene concedido el permiso `POST_NOTIFICATIONS`,
- **When** el usuario interactúa con la configuración de alertas o finaliza la configuración de su vehículo,
- **Then** se muestra un diálogo contextual informativo explicando los beneficios de seguridad vehicular antes de lanzar la solicitud nativa del sistema.

---

## 6. Non-Functional Requirements (Android Constraints)

- **Min SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 35 (Android 15)
- **Offline Capability**: **SUPPORTED** (la app opera en modo Free de forma local-first; la revalidación remota y el push fallan limpiamente sin crashear).
- **Performance Budget**:
  - Tiempo de procesamiento en `onMessageReceived`: <= 20ms en `Dispatchers.IO`.
  - Revalidación en `onResume`: Asíncrona, 0ms de bloqueo en el hilo principal (`Dispatchers.Main`).
- **Accessibility Standards**:
  - Diálogos y primers accesibles para TalkBack con contraste según Material 3 y botones con área táctil >= 48x48dp.
- **Security & Privacy**:
  - Modelo Zero Trust: La autorización reside en el backend de Supabase.
  - Ningún token FCM o identificador personal expuesto en texto plano en logs de logcat en builds de release.

---

## 7. Out of Scope

- Modificación de la lógica interna de facturación en Google Play Console (manejada exclusivamente por backend RTDN).
- Pasarelas de pago alternativas a Google Play Billing (ej. Stripe o PayPal en la app móvil).
- Personalización de tonos de notificación específicos por usuario más allá de los provistos por el sistema operativo.
