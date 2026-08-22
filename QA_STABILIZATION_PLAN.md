# Plan de Estabilización y Pruebas Funcionales: KiloMenos (KmSafe) 🚗⛽

**Versión:** 2.0 (Completo y Preparado para Automatización)  
**Rol:** QA Lead Funcional / QA Automation Specialist  
**Estado:** Documento Base para Pruebas Automatizadas  

Este documento define el **Plan de Pruebas Funcionales** y la **Matriz de Pruebas de Alta Eficiencia** para la totalidad de la aplicación **KiloMenos (KmSafe)**. Su estructura y terminología están diseñadas para servir como especificación directa para el desarrollo de pruebas automatizadas (Compose UI Tests, Espresso y Firebase App Testing Agent).

---

## 1. 🔍 Alcance del Plan de Pruebas (Functional Scope)

Para garantizar la estabilidad general y cumplir con las directrices de arquitectura limpia definidas en el proyecto, el plan de pruebas cubre los siguientes seis módulos de negocio:

```
┌────────────────────────────────────────────────────────────────────────┐
│                          KiloMenos (KmSafe)                            │
├───────────────────┬───────────────────┬────────────────────────────────┤
│ 1. AUTH & GDPR    │ 2. CONTRACTS      │ 3. TRACKING & CALCS            │
│  - Email/Google   │  - Setup Wizard   │  - GPS Tracking Service        │
│  - Self-Healing   │  - Edit Contract  │  - Additive calculations       │
│  - Account Delete │  - Fleet Switcher │  - Single Source of Truth      │
├───────────────────┼───────────────────┼────────────────────────────────┤
│ 4. FUEL EXPENSES  │ 5. SYNC & ROOM    │ 6. BILLING & PORTABILITY       │
│  - Station CRUD   │  - WorkManager    │  - Play Billing integration    │
│  - A+C Algorithm  │  - ID Swap        │  - CSV/JSON backup/restore     │
│  - Geofencing     │  - Client-side ID │  - AI-driven projections       │
└───────────────────┴───────────────────┴────────────────────────────────┘
```

1. **Identidad, Sesión y Privacidad**: Flujo de alta/acceso, validación de políticas de privacidad, autodetección y autorrecuperación de sesiones corruptas (Self-Healing), y eliminación total de datos según regulaciones de privacidad (RGPD).
2. **Gestión de Contratos de Renting**: Creación de contratos mediante el asistente secuencial (Setup Wizard), edición de parámetros contractuales, selección de dispositivo Bluetooth de control, recorte de imagen del vehículo y límites de flota.
3. **Lectura de Odometer y Rastreo GPS**: Registro manual de kilometraje, servicio en primer plano para rastreo GPS (Foreground Service), resiliencia de estado en disco del trayecto y cálculos SSOT (DBB, RKC, TK, UB, Odometer Actual).
4. **Gastos de Combustible y Geofencing**: Registro de gastos de repostaje (normal, modo Waze), algoritmo A+C (Consumo de Litros/100km), visualización de volatilidad de precios, geolocalización de estaciones con favoritos y notificaciones proactivas con Deep Links.
5. **Resiliencia de Datos y Sincronización**: Almacenamiento local-first offline en Room, WorkManager para sincronización en red recuperada, resolución de conflictos con identificadores generados por el cliente (UUID v4) e intercambio de identificadores (ID Swap / SyncIdHandler).
6. **Módulo de Monetización y Portabilidad**: Pasarela de pago Premium (Google Play Billing), persistencia cifrada de Entitlements en sesión, exportación selectiva (CSV para Free, JSON completo para Premium) y proyecciones financieras AI.

---

## 2. 📋 Casos de Pruebas Funcionales por Módulo

A continuación, se detallan las verificaciones unitarias y de integración funcional por cada área clave de la aplicación:

### Módulo A: Autenticación, Sesión y Privacidad
*   **TC-A-01 (Registro Nuevo)**: Registro de usuario exitoso ingresando Nombre, Email válido y Contraseña fuerte. Comprobar redirección al WelcomeDiscovery o Dashboard.
*   **TC-A-02 (Registro Duplicado)**: Intento de registro con un email ya existente. Debe bloquear la operación con un banner rojo informativo.
*   **TC-A-03 (Acceso Inválido)**: Login fallido con credenciales erróneas. El sistema muestra un mensaje claro de error sin revelar detalles del fallo de seguridad.
*   **TC-A-04 (Autocorrección - Self-Healing)**: Cold start de la aplicación con sesión inconsistente (ej. credenciales huérfanas en keystore descifrado sin perfil de usuario en base de datos local). La aplicación debe gatillar un `SignOut` automático y redirigir al usuario al login.
*   **TC-A-05 (Derecho al Olvido - RGPD)**: Cierre y borrado de cuenta en Perfil > Privacidad. Confirmación del usuario desencadena la limpieza atómica local (Room DB, preferencias, caché de imágenes) y borrado en servidor, con redirección al Login y banner de éxito.
*   **TC-A-06 (Consentimiento UMP)**: Lanzamiento inicial en la UE muestra el cuadro de diálogo de AdMob User Messaging Platform (UMP). El rechazo de publicidad personalizada debe ocultar anuncios y su aceptación debe cargarlos correctamente.
*   **TC-A-07 (Cambio de Conductor y Conflicto de Identidad)**: Con datos de un Usuario A almacenados localmente, intentar iniciar sesión con un Usuario B diferente. Debe aparecer el cuadro de diálogo "Otro conductor detectado". Al confirmarlo, la base de datos local (Room) y las preferencias se purgan completamente antes de iniciar la sincronización de los datos del Usuario B.
*   **TC-A-08 (Cierre de Sesión Seguro)**: Cierre de sesión manual desde Perfil. Verificación de que los tokens de sesión se invalidan en AuthKit, las bases de datos locales y preferencias de usuario se limpian por completo, y se redirige a la pantalla de Login deshabilitando volver atrás.

### Módulo B: Gestión de Contratos de Renting (Vehículos)
*   **TC-B-01 (Setup Wizard - Flujo Feliz)**: Registro del primer vehículo mediante SetupWizard. Introducción de nombre de vehículo, tipo combustible, fechas, km totales, odómetro inicial, precio de km excedido y dispositivo bluetooth para vinculación. Guardado exitoso y visualización en el Dashboard.
*   **TC-B-02 (Recorte de Imagen)**: Carga de imagen del vehículo desde la galería, navegación a `ImageCropper`, validación de coordenadas de recorte, confirmación y persistencia de la URI resultante en el contrato de renting.
*   **TC-B-03 (Límite de Vehículos - FREE)**: Usuario sin suscripción activa (FREE) intenta agregar un segundo vehículo a través del listado de vehículos. Debe aparecer el cuadro de diálogo Premium bloqueando la acción.
*   **TC-B-04 (Modificación del Contrato)**: Edición de un contrato activo modificando la holgura de cortesía (km) y precio por km excedido. El Dashboard debe reflejar los nuevos límites de manera instantánea.
*   **TC-B-05 (Eliminación de Contrato)**: Eliminar un contrato/vehículo de la flota. Si el vehículo posee registros históricos de kilómetros o gastos de combustible asociados, debe aplicarse una de las siguientes reglas de negocio:
    *   Vehículo seleccionado activo: Bloquear la eliminación mostrando un banner de error.
    *   Vehículo secundario: Borrado exitoso, eliminando en cascada sus OdometerRecords locales/remotos.

### Módulo C: Odometer Records & GPS Tracking
*   **TC-C-01 (Lectura de Odómetro Incremental)**: Registro de nueva lectura del odómetro manual (ej. añadir 150 km). El sistema calcula el incremento y recalculación de los balances (`Theoretical Km`, `Real Km Consumed`, `Updated Balance`).
*   **TC-C-02 (Validación Odométrica)**: Intento de registrar un odómetro menor o igual al último valor válido. Debe bloquear la operación con mensaje de error explicativo.
*   **TC-C-03 (Servicio GPS en Primer Plano)**: Inicio de viaje desde el Dashboard usando el servicio de rastreo GPS asistido. Confirmación de visualización de notificación del servicio en primer plano y persistencia de la geolocalización.
*   **TC-C-04 (Resiliencia ante Process Death en GPS)**: Durante un rastreo activo, simular muerte súbita del proceso del sistema operativo. Al reabrir la app, el repositorio sin estado recupera la información desde DataStore y el servicio continúa rastreando el trayecto sin pérdida de km acumulados.
*   **TC-C-05 (Consistencia de Métrica SSOT)**: Validación de cálculos financieros y de kilometraje a través de la agregación de `OdometerRecords` históricos utilizando el UseCase centralizado (`GetOverviewDataUseCase`). La app nunca lee caches informativos desactualizados del servidor para lógica crítica.

### Módulo D: Módulo de Combustible, Estaciones y Geofencing
*   **TC-D-01 (CRUD Estaciones)**: Creación manual de una estación de servicio con nombre, ubicación capturada por GPS y combustible disponible. Modificación de datos y guardado.
*   **TC-D-02 (Favoritos y Filtro de Búsqueda)**: Añadir estación a favoritos. Uso de la barra de búsqueda aplicando el filtro de favoritos para reducir la lista de estaciones disponibles.
*   **TC-D-03 (Registro de Gasto Normal y Consumo A+C)**: Registro de gasto seleccionando estación favorita, volumen de litros, precio unitario y odómetro actual. El sistema muestra automáticamente en la lista de gastos el cálculo de litros/100km basado en la diferencia del odómetro con el repostaje previo lleno.
*   **TC-D-04 (Repostaje Parcial vs Lleno)**: Registrar un repostaje marcado como parcial. El cálculo de consumo A+C debe suspenderse hasta el próximo repostaje completo (lleno) para evitar desvíos métricos.
*   **TC-D-05 (Modo Waze - Solo Precio)**: Gasto guardado con Volumen = 0 y Precio > 0. El gasto no cuenta para el cálculo de consumo promedio (A+C) del vehículo, pero la lectura actualiza el punto del gráfico de volatilidad de la estación.
*   **TC-D-06 (Geofencing y Deep Link de Notificación)**: Disparar ubicación simulada de geocerca en emulador sobre la latitud/longitud de una estación favorita. El usuario recibe la notificación proactiva de KmSafe. Al pulsar la notificación, accede mediante Deep Link `/expenses?stationId={id}&autoOpenAdd=true` directamente a la pantalla de gastos con la estación pre-seleccionada.

### Módulo E: Motor de Sincronización y Resiliencia Offline
*   **TC-E-01 (Escritura en Modo Avión)**: Crear un `OdometerRecord` y un `FuelExpense` con el modo avión activado. Las filas se guardan localmente en Room con estado `PENDING` y se muestra el indicador visual de sincronización pendiente en la barra superior.
*   **TC-E-02 (WorkManager y Sincronización Diferida)**: Desactivar modo avión. El observador de conectividad de red detecta el estado `Available`, espera 1000ms y activa el sync diferido por WorkManager. Las transacciones suben a la base de datos remota cambiando su estado local a `SYNCED`.
*   **TC-E-03 (Idempotencia y UUID Cliente)**: El registro local nace con un UUID v4 autogenerado en cliente. La subida a API envía el UUID como clave primaria. Una re-ejecución del worker por fallo de red no genera duplicidades en el backend.
*   **TC-E-04 (Swap de IDs - SyncIdHandler)**: Comprobación de que la resolución de identidades locales no altera la estabilidad visual en la UI. Al recibir la confirmación de guardado con swap de ID del servidor, la lista `LazyColumn` se actualiza de forma atómica sin perder la posición ni hacer parpadeos en pantalla.

### Módulo F: Compras In-App, Portabilidad y Proyecciones AI
*   **TC-F-01 (Restricción de Características Premium)**: Usuario con membresía FREE intenta acceder a: AI Projections, Fleet Management (Mis vehículos > Añadir más de un vehículo), JSON Backup/Restore. Debe mostrarse la pantalla de Paywall (`PremiumPaywallRoute`).
*   **TC-F-02 (Compra Exitosa Google Play Billing)**: Ejecución de pasarela de facturación simulada en Paywall. Transición exitosa a Premium, recarga de entitlements en la sesión cifrada y desbloqueo inmediato de las características restringidas sin reiniciar la app.
*   **TC-F-03 (Promoción de Datos al Actualizar)**: Al comprar Premium, gatillar la promoción automática de datos almacenados localmente en Room (que tenían estado `PENDING` de sync por ser FREE) hacia la base de datos en la nube.
*   **TC-F-04 (Exportación CSV vs JSON)**:
    *   Modo FREE: Exportación a CSV permitida, JSON bloqueada.
    *   Modo Premium: Exportaciones a CSV y JSON habilitadas, restauración de backup desde archivo JSON validando la consistencia e integridad relacional.
*   **TC-F-05 (Análisis Financiero Proyecciones AI)**: Cargar la pantalla de proyecciones para un contrato Premium. Visualización correcta de proyecciones matemáticas sobre penalizaciones financieras por exceso de kilometraje con base en tendencias de consumo.

---

## 3. 🎯 Matriz de Pruebas de Alta Eficiencia (E2E Integration Scenarios)

Esta matriz agrupa los flujos individuales en **7 grandes escenarios integrados de extremo a extremo (E2E)**. El objetivo es maximizar la cobertura funcional con la mínima cantidad de ejecuciones, replicando flujos reales de usuario.

| ID | Nombre de Escenario | Módulos Cubiertos | Pasos Secuenciales (Flujo de Acción) | Criterio de Aceptación / Resultado Esperado |
| :--- | :--- | :---: | :--- | :--- |
| **E2E-01** | **Funnel de Onboarding y Configuración Inicial (FREE)** | A, B, C | 1. Lanzamiento inicial con UMP Consent.<br>2. Registrar nuevo usuario con Email.<br>3. WelcomeDiscovery -> SetupWizard.<br>4. Crear contrato para un vehículo gratuito.<br>5. Recortar foto de vehículo con `ImageCropper`. | Aterrizaje en el Dashboard. El vehículo creado es visible, el odómetro inicial está fijado y el balance de kilómetros está correctamente calculado. |
| **E2E-02** | **Registro de Kilómetros, SSOT y Balance** | C, E | 1. Estar logueado (FREE) con 1 vehículo.<br>2. Registrar lectura de odómetro (+100 km) manual.<br>3. Intentar registrar una lectura menor al odómetro actual (Validar error).<br>4. Verificar cálculos de balance en Dashboard. | El balance de km disminuye por el valor incremental real. Los campos DBB, RKC, TK, UB y Odometer Actual se actualizan mediante el agregador central de UseCase sin duplicaciones. |
| **E2E-03** | **Funnel de Venta Premium y Promoción de Datos** | F, E, B | 1. Desde el Dashboard FREE, abrir el menú lateral o Perfil.<br>2. Ir a Premium Paywall y completar compra simulada.<br>3. Volver al Dashboard.<br>4. Ir a Perfil > Mis Vehículos.<br>5. Añadir un segundo coche a la flota y marcarlo como ACTIVO. | La compra actualiza los Entitlements en sesión. El límite de un solo vehículo desaparece. La base de datos local inicia sync en background para promover los registros existentes a la nube. |
| **E2E-04** | **Resiliencia de Sincronización Diferida Offline** | E, C, D | 1. Activar Modo Avión.<br>2. Agregar un registro de odómetro y un gasto de combustible normal.<br>3. Comprobar que en Dashboard se muestra la nube `PENDING`.<br>4. Desactivar Modo Avión (conectar red).<br>5. Observar actualización del estado en Room. | El observador de red detecta la reconexión y dispara el WorkManager tras 1s. Las transacciones se envían usando UUID v4 y cambian a `SYNCED` sin interferir con la interactividad de la UI. |
| **E2E-05** | **Servicio GPS de Trayecto en Segundo Plano** | C | 1. Conceder permisos de ubicación requeridos.<br>2. Iniciar rastreo de viaje en Dashboard.<br>3. Enviar app a segundo plano y simular movimiento de ubicación GPS.<br>4. Forzar "Process Death" en el SO.<br>5. Reabrir app y terminar el viaje de forma manual. | El viaje se mantiene rastreando gracias al Foreground Service y persistencia stateless. Al detenerse el viaje, se genera automáticamente el `OdometerRecord` incremental correspondiente. |
| **E2E-06** | **Repostajes A+C, Geocerca y Notificación con Deep Link** | D | 1. Agregar Estación "A" y marcarla como favorita.<br>2. Simular geolocalización dentro del radio de Estación "A".<br>3. Pulsar en la notificación de KmSafe recibida.<br>4. En el form pre-completado, agregar Gasto Normal (Lleno).<br>5. Repetir pasos para registrar un segundo repostaje lleno posterior. | La notificación se abre vía Deep Link seleccionando la estación. El segundo gasto muestra de forma dinámica el consumo calculado (litros/100km). La estación muestra su curva de volatilidad. |
| **E2E-07** | **Seguridad de Sesión Cifrada y Derecho al Olvido** | A, E | 1. Alterar sesión de usuario simulando inconsistencias en keystore.<br>2. Abrir la aplicación (Cold Start).<br>3. Tras auto-logout, loguearse con credenciales válidas.<br>4. Navegar a Perfil > Privacidad.<br>5. Confirmar "Eliminar mi cuenta". | En el cold start inconsistente, el mecanismo auto-heals de sesión limpia y fuerza logout. En el flujo de eliminación, se eliminan todos los datos locales de Room y SharedPrefs, y se redirige a Login. |
| **E2E-08** | **Cambio de Usuario y Prevención de Fugas de Datos** | A, B, E | 1. Iniciar sesión como Usuario A (Free).<br>2. Configurar vehículo y añadir 3 registros de odómetro.<br>3. Cerrar sesión.<br>4. Iniciar sesión como Usuario B (Premium).<br>5. Confirmar en el diálogo "Otro conductor detectado" para limpiar base local.<br>6. Sincronizar datos de B. | Al aceptar el diálogo, la base local Room se vacía atómicamente. Al completarse la sincronización de B, no hay registros cruzados del Usuario A en pantalla (se evita contaminación de datos). |

---

## 4. 🛠️ Especificación Técnica para Automatización (UI Automation Guidelines)

Para mapear directamente este plan de pruebas funcionales en código ejecutable, los desarrolladores de pruebas deben seguir estas especificaciones técnicas:

### A. Mapeo de Compose UI Elements (Semantics & testTags)
Para que los frameworks de prueba (Compose Testing, Espresso, UI Automator) localicen los elementos interactivos sin depender de textos traducidos, se deben usar identificadores semánticos estandarizados (`testTag`):

```kotlin
// Ejemplos de implementación en Compose UI
Modifier.testTag("google_login_button")
Modifier.testTag("add_odometer_fab")
Modifier.testTag("setup_wizard_vehicle_name")
Modifier.testTag("setup_wizard_save_button")
Modifier.testTag("premium_paywall_buy_button")
Modifier.testTag("sync_pending_indicator")
```

| Pantalla / Componente | Elemento de UI | `testTag` Asignado |
| :--- | :--- | :--- |
| **Login** | Botón de Login de Google | `google_login_button` |
| **Login** | Enlace para registro | `signup_link` |
| **Signup** | Input de Email | `signup_email_input` |
| **Signup** | Botón Registrarse | `signup_submit_button` |
| **Dashboard** | Botón Flotante Añadir Odométro | `add_odometer_fab` |
| **Dashboard** | Indicador Sincronización Pendiente | `sync_pending_indicator` |
| **SetupWizard** | Input Nombre de Vehículo | `setup_vehicle_name_input` |
| **SetupWizard** | Botón Guardar Vehículo | `setup_save_vehicle_button` |
| **PremiumPaywall**| Botón Comprar / Suscribirse | `premium_buy_now_button` |
| **Profile** | Botón Eliminar Cuenta | `profile_delete_account_button` |
| **Expenses** | Botón Guardar Gasto | `expense_save_button` |

### B. Estructura de Automatización con Firebase App Testing Agent (Robo Script YAML)
En la carpeta [firebase-tests/](file:///c:/Users/josh_/AndroidStudioProjects/KmSafe/firebase-tests), las pruebas se ejecutan utilizando el agente de pruebas de Firebase. Se deben seguir las siguientes convenciones sintácticas en los archivos `.yaml` para mantener la compatibilidad con el motor de IA del agente de pruebas:

1.  **displayName**: Título descriptivo e identificable del flujo.
2.  **steps**: Lista ordenada de acciones semánticas.
3.  **goal**: La intención de la acción descrita para el agente inteligente.
4.  **hint** (opcional): Guía de interacción en el canvas (ej. identificar el testTag o cadena de texto).
5.  **finalScreenAssertion**: La verificación visual que debe realizar el agente para dar el paso por aprobado.

*Ejemplo de declaración del flujo de Sincronización Diferida (Offline)*:
```yaml
tests:
  - displayName: "E2E-04: Sincronización Diferida Offline"
    steps:
      - goal: "Colocar el dispositivo en modo offline."
        hint: "Desconectar Wifi y Datos Móviles en el entorno del emulador."
        finalScreenAssertion: "La barra superior debe reflejar un icono de advertencia."
      - goal: "Añadir una lectura de kilometraje incremental."
        hint: "Pulsar sobre add_odometer_fab, escribir un incremento numérico válido y pulsar Guardar."
        finalScreenAssertion: "El icono sync_pending_indicator debe ser visible en el Dashboard."
      - goal: "Reconectar la red del emulador."
        hint: "Activar Wifi en los ajustes simulados."
        finalScreenAssertion: "El icono sync_pending_indicator debe desaparecer tras un tiempo de espera."
```

---

## 5. ⚠️ Casos de Borde y Escenarios Críticos a Validar

Cualquier esfuerzo de automatización o suite de control debe priorizar la robustez ante los siguientes casos de borde identificados en KmSafe:

1.  **Inconsistencia de Sesión Keystore**: Si el token persiste pero el perfil de usuario se borra localmente (debido a restauraciones parciales de copias de seguridad del sistema operativo), el `CheckSessionUseCase` debe detectar el desalineamiento y forzar el SignOut limpio sin provocar bucles infinitos de reinicio.
2.  **Exclusión de Copia de Seguridad Automática**: Comprobar que los archivos de configuración y bases de datos cifradas estén listados en el `xml/backup_rules.xml` como excluidos (`exclude`). Esto previene el error crítico donde las claves de Keystore se borran al desinstalar pero los archivos cifrados se restauran, imposibilitando su desencriptación y corrompiendo el estado de la app en instalaciones limpias.
3.  **Idempotencia del SyncIdHandler**: Al realizar la sincronización de un registro que falló a nivel de red pero se procesó en servidor, validar que el backend rechaza la duplicación usando el UUID de cliente y que el frontend gestiona de manera correcta el ID Swap para mantener vinculada la colección reactiva de la UI.
4.  **Agregación SSOT de Odométros**: Ningún elemento de la UI debe obtener la distancia recorrida del campo informativo plano `rentingContract.currentOdometer` devuelto por el servidor si hay registros de odómetro en la BD. El sistema debe sumar los incrementos de los `OdometerRecords` individuales como única fuente de verdad funcional de distancias.
5.  **Pérdida de Conexión en Sincronización Intermedia**: Simular el corte de red justo a la mitad de la sincronización de una lista de 50 registros pendientes. Comprobar que la base de datos se mantiene en un estado consistente (los procesados cambian a `SYNCED` y los fallidos permanecen en `PENDING` para la siguiente ronda de sincronización del WorkManager).
6.  **Contaminación de Datos en Cambio de Usuario**: Verificar que la limpieza de bases de datos desencadenada por el conflicto de identidad ("Otro conductor detectado") purgue atómicamente todas las tablas de Room (RentingContract, OdometerRecord, ServiceStation, FuelExpense) y la caché. El ID del conductor anterior en preferencias debe ser destruido completamente para prevenir que el WorkManager intente asociar registros huérfanas al nuevo usuario.
