# Living Specification: KILOMENOS-11
# Delta Specification: Formato Canónico UUID v4 y Deduplicación de Notificaciones
**Domain**: `KILOMENOS-11` / `FEAT-004`

## Added Requirements

### REQ-KILOMENOS-11-001: Identificador Canónico UUID v4 en Generación de Notificaciones
Toda notificación instanciada localmente (alertas de proyección, Bluetooth, recordatorios) debe asignar a su propiedad `id` un UUID v4 canónico (`UUID.randomUUID().toString()`).
- **Given** que el sistema evalúa una condición que dispara una alerta funcional en primer plano o en background
- **When** se crea la entidad de notificación
- **Then** el `id` asignado cumple la expresión regular `^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$`.

### REQ-KILOMENOS-11-002: Encapsulación de Claves Semánticas en Data Payload
Los identificadores compuestos de negocio (tales como `proj_${contractId}_${todayEpochDay}` o `bt_missing_${contractId}`) deben almacenarse dentro del mapa `data` bajo `projection_key` o `deduplication_key`.
- **Given** una alerta generada por cambio de ritmo de proyección de kilometraje
- **When** se construye el modelo `Notification`
- **Then** `data["projection_key"]` contiene el identificador compuesto y `data["contract_id"]` contiene el identificador de contrato.

### REQ-KILOMENOS-11-003: Deduplicación Semántica en Base de Datos Local
La persistencia en Room debe garantizar que la publicación recurrente de una alerta para el mismo contrato y día no genere registros duplicados en la interfaz del usuario.
- **Given** una notificación existente en Room con una `projection_key` específica
- **When** el motor de cálculo intenta publicar una nueva alerta con la misma clave semántica
- **Then** la base de datos actualiza el registro existente o descarta la inserción duplicada.

### REQ-KILOMENOS-11-004: Conformidad de Rutas y Lotes en Endpoints REST
Las llamadas a `PATCH /v1/notifications/{id}/read`, `POST /v1/notifications/sync` y `DELETE /v1/notifications/{id}` deben utilizar exclusivamente valores UUID v4 válidos en las variables de ruta y cuerpos de petición, propagando el mapa `data`.
- **Given** una notificación con UUID canónico
- **When** se ejecuta el marcado como leída o la sincronización en background
- **Then** el backend PostgreSQL de Supabase responde con HTTP 200 sin errores de tipo de dato en columna UUID.

### REQ-KILOMENOS-11-005: Tratamiento Resiliente de Identificadores Heredados
El cliente de red debe validar defensivamente que el parámetro `id` sea un UUID canónico antes de despachar llamadas remotas.
- **Given** una notificación preexistente en base de datos con un ID no-UUID (ej: `proj_...`)
- **When** el usuario realiza una acción de lectura o borrado
- **Then** la operación local se completa y el cliente de red evita invocar el endpoint remoto con un ID sintácticamente inválido.
