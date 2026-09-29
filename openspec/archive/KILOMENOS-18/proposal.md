# Change Proposal: Catálogo Canónico de Errores Backend y Mapeo MVI
**Change ID**: `KILOMENOS-18`
**Status**: Proposed
**Created At**: 2026-09-29 08:21:29

## 1. Intent & Business Value
Migrar el sistema de gestión de errores del frontal Android desde un emparejamiento heurístico de cadenas hacia el consumo del Catálogo Canónico de Errores emitido por Backend. Esto garantiza determinismo en la interacción con el usuario, evita falsos positivos/negativos en validaciones, habilita la renovación silenciosa de tokens y previene roturas de interfaz ante cambios de internacionalización en el servidor.

## 2. Scope of Changes
- **Target Modules**: `:core:domain`, `:core:infrastructure`, `:core:ui`
- **Key Capabilities**:
  - Incorporar los nuevos tipos de error del catálogo en [KmError.kt].
  - Implementar el extractor y deserializador de `code` canónico en [ErrorMapper.kt].
  - Mapear mensajes localizados y accesibles en `:core:ui` ([ErrorExtensions.kt], `strings.xml`).
  - Habilitar acciones predecibles en la capa de presentación (MVI `UiEffect` y `UiState`).

## 3. Dependencies & Compatibility
- **Dependencies**: Jackson ObjectMapper, Retrofit 2, CanvasKit TextProvider.
- **Breaking Changes**: Ninguna. Se preserva compatibilidad retroactiva total con todos los subtipos preexistentes de `KmError`.

## 4. Reglas de Negocio con Consecuencia de Fallo (BR-xx)
| ID Regla | Enunciado de Regla | Condición de Fallo | Consecuencia en Sistema / Error |
|---|---|---|---|
| **BR-01** | La app debe mapear códigos de error backend a través de su identificador `code` canónico. | Si no existe `code` ni coincidencia exacta, se recurre a código HTTP de fallback. | `KmError.ServerError` o `KmError.UnknownError`. |
| **BR-02** | Respuestas `AUTH_TOKEN_EXPIRED` (401) deben ser procesadas silenciosamente en la capa de red. | Si el refresh falla o es irrecuperable. | Emisión de `KmError.AuthUnauthorized`, cierre de sesión y redirección a `Destination.Login`. |
| **BR-03** | Errores `PREMIUM_REQUIRED` (403) deben guiar al usuario a la pantalla de Paywall. | Intento de consumir recurso o sincronización exclusiva de suscriptores. | Emisión de `UiEffect.NavigateTo(Destination.PremiumPaywall)`. |
| **BR-04** | Los errores de validación de campo deben reflejarse bajo el campo visual correspondiente. | Falta un parámetro obligatorio o formato inválido. | Proyección en `UiState` sin alertas modales intrusivas. |

## 5. Dudas Abiertas Bloqueantes (HITL Halt Gate)
- [x] No existen dudas abiertas bloqueantes pendientes.
