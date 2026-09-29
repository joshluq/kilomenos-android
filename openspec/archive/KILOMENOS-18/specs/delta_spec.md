# Delta Specification: Catálogo Canónico de Errores Backend y Mapeo MVI
**Domain**: `KILOMENOS-18`

## Added Requirements

### REQ-KILOMENOS-18-001: Canonical Backend Error Code Deserialization
El mapeador de errores de infraestructura debe deserializar el campo `code` canónico tanto en respuestas con status HTTP 4xx/5xx como en payloads con JSON body.
- **Given** una respuesta de la API de backend con `code: "PREMIUM_REQUIRED"` y HTTP 403
- **When** `ErrorMapper.mapApiResponse` procesa la respuesta
- **Then** retorna `KmError.PremiumRequired`.

### REQ-KILOMENOS-18-002: Contextual Validation Error Extraction
Para errores de validación de peticiones (`VALIDATION_REQUIRED_FIELD`, `INVALID_FORMAT`), el mapeador debe extraer el identificador del campo afectado.
- **Given** una respuesta con `code: "VALIDATION_REQUIRED_FIELD"` y `field: "fcm_token"`
- **When** `ErrorMapper.mapApiResponse` procesa la respuesta
- **Then** retorna `KmError.ValidationRequiredField(field = "fcm_token")`.

### REQ-KILOMENOS-18-003: UI Localized Text Provider Mapping
Cada código de error del catálogo debe tener una traducción localizada y accesible en `:core:ui`.
- **Given** un error de dominio `KmError.DeviceTrialLimit`
- **When** se invoca `KmError.toText()`
- **Then** retorna un `TextProvider.Resource` con el recurso de cadena correspondiente en español o inglés según la configuración del dispositivo.
