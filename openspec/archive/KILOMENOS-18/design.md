# Technical Design: Catálogo Canónico de Errores Backend y Mapeo MVI
**Change ID**: `KILOMENOS-18`

## 1. MVI State & Error Model Architecture

### 1.1 Dominio (`:core:domain`)
`KmError` se amplía con las 5 categorías oficiales:
- **Entitlements**: `PremiumRequired`, `DeviceTrialLimit`, `AlreadyPremium`, `PurchaseVerificationFailed`.
- **Autenticación**: `AuthUnauthorized`, `AuthTokenExpired`, `ApiKeyInvalid`.
- **Validación**: `InvalidJsonBody`, `ValidationRequiredField(val field: String)`, `InvalidFormat(val field: String, val reason: String?)`.
- **Recursos**: `NotificationNotFound`, `ResourceNotFound`, `Conflict`.
- **Servicios Externos**: `DatabaseError(val code: Int)`, `FcmDispatchFailed`, `ServiceUnavailable(val retryAfterSeconds: Int?)`.

### 1.2 Infraestructura (`:core:infrastructure`)
`ErrorMapper.mapApiResponse` extrae:
1. `code`: De nodo raíz o nodo hijo `error.code` / string `error`.
2. `field`: Nombre del campo para errores de validación.
3. `retry_after_seconds`: Para `ServiceUnavailable` y Rate Limits.

### 1.3 Presentación (`:core:ui`)
`ErrorExtensions.toText()` transforma de forma exhaustiva cada `KmError` en un `TextProvider.Resource` con argumentos parametrizados (`%1$s`), evitando concatenaciones de texto en la vista y facilitando la accesibilidad EAA.

## 2. Unidirectional Data Flow Mapping
- **Efectos Navegacionales**: `UiEffect` canalizado en `LaunchedEffect(Unit)` en la `Route` de Compose.
- **Estado de Pantalla**: Inmutable (`@Immutable data class ...UiState`), proyecciones directas de errores en campos específicos.
- **Acciones Silenciosas**: Eliminación reactiva en Room o reintentos asíncronos en WorkManager.
