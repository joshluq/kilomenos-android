# Product Requirements Document: Formato Canónico UUID v4 y Deduplicación Semántica de Notificaciones

**Feature ID**: FEAT-004  
**Version**: 1.0.0  
**Status**: APPROVED  
**Author**: Product Owner  
**Date**: 2026-09-23  
**Target Release**: v1.2.0  

---

## 1. Executive Summary & Problem Statement
- **Problem**: Al marcar notificaciones como leídas mediante la petición HTTP `PATCH /v1/notifications/{id}/read`, o durante la sincronización por lotes en `POST /v1/notifications/sync`, la aplicación Android KmSafe está enviando identificadores compuestos generados localmente (por ejemplo: `proj_5a4637c8-9ddb-40b8-af7c-b15073f8090b_20719` o `bt_missing_...`). En la base de datos PostgreSQL de Supabase (`public.user_notifications`), la columna `id` está definida con tipo estricto `UUID`. Al recibir cadenas no conformes con la sintaxis canónica de UUID, PostgreSQL falla con el error `invalid input syntax for type uuid` (HTTP 500 Internal Server Error), provocando que las alertas no puedan marcarse como leídas ni sincronizarse en la nube.
- **Value Proposition**: Estandarizar la clave primaria de todas las notificaciones creadas en Android al formato canónico UUID v4 (`UUID.randomUUID().toString()`) y trasladar las claves compuestas de negocio (`proj_${contractId}_${todayEpochDay}`, etc.) al payload de metadatos `data` (`projection_key`). Esto garantiza compatibilidad 100% con la API REST de Supabase, elimina los errores HTTP 500 y preserva la deduplicación local de alertas en Room sin degradar la experiencia offline.

## 2. Target Personas
- **Primary Persona**: Conductor de renting y gestor de flota vehicular que utiliza KmSafe para monitorizar el cumplimiento de kilometraje y el estado de sus vehículos.
- **User Pain Point**: El usuario pulsa en "Marcar como leída" en una notificación de proyección o sincroniza en segundo plano y la operación remota falla silenciosamente o con error 500 en el servidor, manteniendo estados inconsistentes entre clientes.
- **Usage Frequency / Environment**: Uso diario móvil, transiciones frecuentes entre modo offline (túneles, garajes, zonas sin cobertura) y conectividad Wi-Fi/4G/5G.

## 3. User Stories
- **US-01**: Como conductor que revisa una alerta de proyección en el centro de notificaciones, quiero pulsar en "Marcar como leída" para que la notificación se marque como leída inmediatamente en mi dispositivo y en el servidor remoto sin fallos HTTP 500.
- **US-02**: Como usuario offline que genera alertas de telemetría y proyección en local, quiero que dichas alertas se guarden con identificadores UUID v4 canónicos válidos para que, al recuperar la conectividad, se sincronicen de forma transparente con el backend sin ser rechazadas por la base de datos.
- **US-03**: Como conductor que consulta sus proyecciones periódicamente, quiero que las alertas de proyección de un mismo contrato y día no se dupliquen visualmente en mi historial, deduplicándose mediante su clave semántica (`projection_key`).
- **US-04**: Como usuario con notificaciones heredadas en base de datos local que contienen identificadores no-UUID, quiero que el sistema maneje estas alertas con seguridad sin intentar enviar identificadores malformados a los endpoints REST remotos.

## 4. Functional Requirements
- **FR-01**: [Identificador Canónico UUID v4] — Toda notificación generada en la plataforma Android (originada localmente por proyecciones, alertas de Bluetooth o recibida remotamente) debe poseer como clave primaria `id` un UUID v4 canónico (`UUID.randomUUID().toString()`).
- **FR-02**: [Preservación de Clave Semántica en Data] — Los identificadores compuestos de negocio (tales como `proj_${contractId}_${todayEpochDay}` o `bt_missing_${contractId}`) deben viajar dentro del mapa/JSON de metadatos `data` bajo la clave `projection_key` o `deduplication_key`, nunca en el campo `id`.
- **FR-03**: [Estrategia de Deduplicación Local en Room] — La persistencia local en Room debe evitar la inserción redundante de alertas que compartan la misma clave semántica (`projection_key`) para un mismo contrato y estado, actualizando la entidad existente o ignorando duplicados.
- **FR-04**: [Conformidad de Contrato en Rutas y Cuerpos de Red] — Todas las peticiones HTTP que interactúan con endpoints de notificación (`PATCH /v1/notifications/{id}/read`, `POST /v1/notifications/sync`, `DELETE /v1/notifications/{id}`) deben enviar exclusivamente valores UUID válidos y propagar el mapa `data` en el DTO de sincronización.
- **FR-05**: [Tratamiento Defensivo de IDs Heredados] — Al interactuar con el repositorio o el cliente de red, si una notificación local preexistente posee un `id` que no cumple con el formato UUID canónico, el cliente de red debe evitar invocar el endpoint remoto con dicho parámetro o regenerar el identificador de forma segura para no provocar excepciones HTTP 500 en PostgreSQL.

## 5. Acceptance Criteria (Given / When / Then)

### AC-01: Creación de Alerta de Proyección con UUID v4 Canónico
- **Given** que el usuario experimenta una transición de proyección de kilometraje (por ejemplo, superando el límite contratado),
- **When** `OverviewViewModel` detecta la condición y publica la notificación mediante `PublishNotificationUseCase`,
- **Then** la notificación generada tiene como `id` un UUID v4 válido (`UUID.randomUUID().toString()`) y el mapa `data` contiene `"projection_key": "proj_${contract.id}_${todayEpochDay}"` junto con los metadatos relevantes (`contract_id`, `balance_km`).

### AC-02: Marcado como Leída con UUID v4 en Endpoint REST
- **Given** una notificación de proyección generada con UUID v4 almacenada en Room,
- **When** el usuario pulsa en "Marcar como leída" en `NotificationsListScreen`,
- **Then** la interfaz actualiza el estado inmediatamente a `READ` (Optimistic UI) y la llamada de red ejecuta `PATCH /v1/notifications/{uuid}/read` utilizando el UUID canónico en el path, respondiendo el backend Supabase con HTTP 200 OK sin errores de sintaxis en PostgreSQL.

### AC-03: Sincronización Remota de Lotes con Formato de ID Válido
- **Given** notificaciones locales pendientes de sincronización (`syncStatus = PENDING`),
- **When** se ejecuta la sincronización mediante `SyncNotificationsWorker` o `NotificationRepository.syncPending()`,
- **Then** el payload enviado a `POST /v1/notifications/sync` contiene exclusivamente elementos con `id` en formato UUID canónico y su respectivo objeto `data`, recibiendo respuesta HTTP 200 OK y actualizándose su estado en Room a `SYNCED`.

### AC-04: Deduplicación Semántica en Base de Datos Local
- **Given** que ya existe en Room una notificación de proyección activa para un contrato en el día actual con una clave semántica específica,
- **When** el motor de proyección vuelve a evaluar la misma condición y emite una alerta con la misma `projection_key`,
- **Then** el repositorio o DAO detecta la existencia de la alerta previa y no genera una tarjeta duplicada en la lista de notificaciones.

### AC-05: Tratamiento Resiliente de Identificadores Heredados No-UUID
- **Given** una notificación almacenada localmente en una versión previa con `id = "proj_..."` o `id = "bt_missing_..."`,
- **When** el usuario interactúa con ella (marcar como leída o eliminar),
- **Then** la operación local en Room se completa de inmediato y el cliente HTTP defensivo valida el formato antes de invocar la API remota, evitando enviar cadenas malformadas a PostgreSQL y previniendo el error HTTP 500.

### AC-06: Creación de Alerta de Configuración Bluetooth con UUID v4
- **Given** que un contrato tiene activado el auto-tracking pero no tiene dirección Bluetooth configurada,
- **When** el sistema genera la alerta de configuración de Bluetooth,
- **Then** la notificación se instancia con un UUID v4 en `id` y la clave semántica `bt_missing_${contract.id}` se almacena en `data["deduplication_key"]`.

---

## 6. Non-Functional Requirements (Android Constraints)
- **Min SDK**: 24 (Android 7.0 Nougat).
- **Target SDK**: 35 (Android 15).
- **Offline Capability**: **REQUIRED**. Persistencia Local-First íntegra en SQLite Room v21/v22.
- **Performance Budget**:
  - Validación de formato UUID e inserción local < 5ms.
  - Render inicial de la lista de notificaciones < 16ms (60 fps sin saltos de frame).
  - Cero bloqueo de hilo principal (`Dispatchers.Main`).
- **Accessibility Standards**:
  - Touch targets de acciones de marcado y borrado >= 48x48dp.
  - Etiquetas TalkBack explícitas y accesibles.
- **Security & Privacy**:
  - Los UUIDs generados deben ser v4 criptográficamente seguros mediante `java.util.UUID.randomUUID()`.
  - Cero exposición de datos personales sensibles en logs.

---

## 7. Out of Scope
- Modificación del esquema de base de datos PostgreSQL en Supabase (`public.user_notifications.id UUID` permanece inalterado).
- Modificación visual de las pantallas de notificaciones o de overview.
- Cambios en el sistema de mensajería push FCM más allá del formato de identificadores.
