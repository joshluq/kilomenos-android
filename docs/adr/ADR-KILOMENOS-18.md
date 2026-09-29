# ADR-KILOMENOS-18: Adopción del Catálogo Canónico de Errores Backend y Mapeo MVI en KmSafe

**Feature ID**: KILOMENOS-18  
**Status**: ACCEPTED  
**Deciders**: Mobile Software Architect (Android), Staff Android Engineer (Compose)  
**Date**: 2026-09-29  
**Technical Stakeholders**: Senior Android Developer, Backend Engineering Team, QA/Testing Engineer, Product Owner  

---

## 1. Context and Problem Statement

Hasta la fecha, la capa de infraestructura Android de KmSafe ([ErrorMapper.kt](file:///c:/Users/josh_/AndroidStudioProjects/KmSafe/core/infrastructure/src/main/java/es/joshluq/kmsafe/infrastructure/mapper/ErrorMapper.kt)) interpretaba los errores de red mediante una coincidencia heurística basada en subcadenas en lenguaje natural (`errorMsg.contains("periodo de prueba")`, `errorMsg.contains("usuarios PREMIUM")`, etc.).

El equipo de Backend ha publicado formalmente el **Catálogo Oficial de Códigos de Error** estructurado en 5 categorías (Entitlements, Autenticación, Validación, Recursos y Servicios Externos) junto con sus códigos HTTP canónicos y la acción esperada en el cliente.

Se requiere:
1. Reemplazar el emparejamiento frágil por un deserializador fuertemente tipado que consuma el campo `code` canónico del backend.
2. Modelar todos los códigos del catálogo dentro de [KmError.kt](file:///c:/Users/josh_/AndroidStudioProjects/KmSafe/core/domain/src/main/java/es/joshluq/kmsafe/domain/model/KmError.kt) sin introducir regresiones ni romper compatibilidad con llamadas existentes.
3. Proveer soporte de localización en `:core:ui` ([ErrorExtensions.kt](file:///c:/Users/josh_/AndroidStudioProjects/KmSafe/core/ui/src/main/java/es/joshluq/kmsafe/core/ui/util/ErrorExtensions.kt)) y recursos de cadenas (español e inglés).
4. Establecer pautas MVI claras sobre qué errores son gestionados transparentemente en red, cuáles provocan efectos de navegación (`UiEffect`), y cuáles actualizan el estado de pantalla (`UiState`).

---

## 2. Decision Drivers

- **Determinismo y Resiliencia**: El cliente nunca debe fallar por cambios cosméticos en los mensajes de texto del backend; el mapeo debe depender exclusivamente de `code` y el código HTTP de respuesta.
- **Transparencia en Red (Cero Ruido en UI)**: Errores recuperables como `AUTH_TOKEN_EXPIRED` (401) deben ser absorbidos por el `Authenticator` de OkHttp ejecutando un refresh silencioso sin interrumpir al conductor.
- **Unidirectional Data Flow (MVI)**: Errores navegacionales (`PREMIUM_REQUIRED` -> Paywall, `AUTH_UNAUTHORIZED` recurrente -> Login) deben emitirse mediante `UiEffect`, mientras que validaciones de formulario (`INVALID_FORMAT`, `VALIDATION_REQUIRED_FIELD`) se proyectan en el `UiState`.
- **Alineación con Clean Architecture**: `:core:domain` permanece puro (sin librerías de Android ni Retrofit), `:core:infrastructure` efectúa la traducción técnica y `:core:ui` transforma los errores a [TextProvider].

---

## 3. Considered Architectural Options

1. **Opción 1: Mantener inspección por cadenas y añadir los nuevos strings**:
   - *Descartada*: Extremadamente frágil, propensa a roturas silenciosas ante cambios de backend o traducción multilingüe en el servidor.
2. **Opción 2: Reemplazo disruptivo de la jerarquía de errores**:
   - *Descartada*: Modificar las firmas existentes rompería decenas de use cases y pruebas unitarias vigentes.
3. **Opción 3: Expansión de KmError con Códigos Canónicos y Deserialización en ErrorMapper (Elegida)**:
   - Se mantiene la compatibilidad retroactiva completa de [KmError.kt].
   - Se agregan las nuevas ramas para cada código del catálogo oficial (`PremiumRequired`, `DeviceTrialLimit`, `AlreadyPremium`, `PurchaseVerificationFailed`, `AuthUnauthorized`, `AuthTokenExpired`, `ApiKeyInvalid`, `InvalidJsonBody`, `ValidationRequiredField`, `InvalidFormat`, `NotificationNotFound`, `ResourceNotFound`, `Conflict`, `DatabaseError`, `FcmDispatchFailed`, `ServiceUnavailable`).
   - Se implementa un extractor JSON en `ErrorMapper` que inspecciona la propiedad canónica `code` (soportando objetos `{ error: { code: ... } }`, raíz `{ code: ... }` y wrappers `{ error: "CODE" }`).
   - Se actualizan las cadenas localizadas en [strings.xml] y [ErrorExtensions.kt].

---

## 4. Decision Outcome

- **Chosen Option**: **Opción 3**
- **Affected Modules**:
  - `:core:domain`: [KmError.kt] (incorporación de nuevos subtipos del catálogo oficial).
  - `:core:infrastructure`: [ErrorMapper.kt] y su suite de pruebas unitarias [ErrorMapperTest.kt].
  - `:core:ui`: [ErrorExtensions.kt], `strings.xml` (default) y `values-en/strings.xml`.
  - `openspec/changes/KILOMENOS-18/`: Propuesta, diseño y especificación delta.

---

## 5. Error Mapping Matrix

| Código Backend | HTTP Status | Dominio (`KmError`) | Capa de Ejecución / Acción en Frontal |
| :--- | :---: | :--- | :--- |
| `PREMIUM_REQUIRED` | 403 | `KmError.PremiumRequired` | `UiEffect.NavigateTo(Destination.PremiumPaywall)` |
| `TRIAL_ALREADY_USED` | 400 | `KmError.TrialAlreadyUsed` | `UiState.trialAvailable = false` + Banner de actualización |
| `DEVICE_TRIAL_LIMIT` | 400 | `KmError.DeviceTrialLimit` | `UiState.isTrialEligibleOnDevice = false` |
| `ALREADY_PREMIUM` | 400 | `KmError.AlreadyPremium` | `SyncEntitlementsUseCase` reactivo + ocultar banners |
| `PURCHASE_VERIFICATION_FAILED` | 400/402 | `KmError.PurchaseVerificationFailed` | `PaywallUiState.error` + Diálogo con CTA "Restaurar Compras" |
| `AUTH_UNAUTHORIZED` | 401 | `KmError.AuthUnauthorized` | Sesión revocada -> `Logout` + purga de `ViewModelStore` + `Destination.Login` |
| `AUTH_TOKEN_EXPIRED` | 401 | `KmError.AuthTokenExpired` | OkHttp `Authenticator` ejecuta refresh silencioso en red |
| `AUTH_INVALID_CREDENTIALS` | 401 | `KmError.InvalidCredentials` | `LoginState.formError` |
| `API_KEY_INVALID` | 401 | `KmError.ApiKeyInvalid` | Telemetría / Log fatal interno |
| `INVALID_JSON_BODY` | 400 | `KmError.InvalidJsonBody` | Telemetría interna + error genérico no bloqueante |
| `VALIDATION_REQUIRED_FIELD` | 400 | `KmError.ValidationRequiredField(field)` | Error contextual bajo el campo de formulario correspondiente |
| `INVALID_FORMAT` | 400 | `KmError.InvalidFormat(field, reason)` | Error contextual bajo el campo de formulario correspondiente |
| `NOTIFICATION_NOT_FOUND` | 404 | `KmError.NotificationNotFound` | Eliminación reactiva en Room (`NotificationDao.deleteById`) |
| `NOT_FOUND` | 404 | `KmError.ResourceNotFound` | `UiState` a `EmptyState` de CanvasKit |
| `CONFLICT` | 409 | `KmError.Conflict` | Refresco automático silencioso desde servidor (Pull-to-refresh / sync) |
| `DATABASE_ERROR` | 500 | `KmError.DatabaseError(code)` | Snackbar con acción de reintento |
| `FCM_DISPATCH_FAILED` | 502 | `KmError.FcmDispatchFailed` | Encolado en WorkManager en segundo plano con backoff exponencial |
| `INTERNAL_SERVER_ERROR` | 500 | `KmError.ServerError(500)` | Mensaje genérico de servidor + reporte a telemetría |
| `SERVICE_UNAVAILABLE` | 503 | `KmError.ServiceUnavailable(retryAfter)` | Reintento automático con backoff exponencial |
