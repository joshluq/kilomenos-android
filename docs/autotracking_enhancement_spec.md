# Especificación de Mejoras y Optimizaciones de Autotracking 🚗⚡
## KiloMenos (KmSafe) - Roadmap Funcional y de Experiencia de Usuario

> **Documento de Referencia Técnica y de Producto**  
> **Fecha de Creación**: Septiembre 2026  
> **Estado**: Propuesta de Arquitectura & Especificación Funcional  
> **Módulos Afectados**: `:core:tracking`, `:core:infrastructure`, `:core:domain`, `:feature:overview`, `:feature:fleet`  
> **Documento Base**: [`autotracking_technical_spec.md`](file:///c:/Users/josh_/AndroidStudioProjects/KmSafe/docs/autotracking_technical_spec.md)

---

## 1. Visión y Resumen Ejecutivo

El sistema de **Autotracking** de KiloMenos ha superado con éxito sus fases críticas de resiliencia en Android 14+ (Fast-Path por hardware Bluetooth, exención de inicio en segundo plano y ventana de cooldown anti-flap de 60s).

Sin embargo, desde una perspectiva puramente **funcional, conductual y de experiencia de usuario (UX)**, el sistema actual opera como un **"registrador GPS semiautomático"**: detecta el inicio y fin del viaje de forma transparente, pero **exige la intervención manual del conductor para consolidar los kilómetros en el odómetro** a través de la interfaz visual (`TripCompletedCard` $\rightarrow$ Confirmar $\rightarrow$ `CanvasKitBottomSheet` $\rightarrow$ Guardar).

### Objetivo Principal:
Transformar el autotracking de KiloMenos en un **Copiloto Automotriz 100% Manos Libres ("Zero-Friction Autonomous Copilot")**, eliminando la necesidad obligatoria de abrir la aplicación tras cada trayecto, resolviendo los casos de borde habituales en la conducción real (paradas breves, repostajes, acumulación involuntaria de trayectos y garajes subterráneos) y maximizando el valor percibido de la suscripción **Premium**.

---

## 2. Matriz de Priorización de Mejoras

| ID | Mejora Funcional | Impacto UX / Negocio | Complejidad Técnica | Fase Sugerida |
| :---: | :--- | :---: | :---: | :---: |
| **F-01** | **Guardado Automático Silencioso (Modo Auto-Save)** | ⭐⭐⭐⭐⭐ (Crítico) | Media | Fase 1 |
| **F-02** | **Timeout de Sesión Inactiva (Anti-Viajes Huérfanos)** | ⭐⭐⭐⭐⭐ (Crítico) | Baja | Fase 1 |
| **F-03** | **Ventana de Gracia ante Paradas Cortas (Debounce 3m)** | ⭐⭐⭐⭐ (Alto) | Media | Fase 2 |
| **F-04** | **Acciones Rápidas en Notificación de Fin de Viaje** | ⭐⭐⭐⭐ (Alto) | Baja | Fase 1 |
| **F-05** | **Claridad Semántica: Odómetro Resultante vs Incremento** | ⭐⭐⭐⭐ (Alto) | Baja | Fase 1 |
| **F-06** | **Detección Multivehículo en Flotas (Auto-Switch por MAC)** | ⭐⭐⭐ (Medio) | Media | Fase 2 |
| **F-07** | **Coordenada SSOT para Detección de Gasolinera** | ⭐⭐⭐ (Medio) | Baja | Fase 1 |
| **F-08** | **Muestreo GPS Adaptativo (Ahorro de Batería en Autovía)** | ⭐⭐⭐ (Medio) | Media | Fase 3 |
| **F-09** | **Clasificación de Trayectos (Laboral vs Personal)** | ⭐⭐⭐ (Medio) | Media | Fase 2 |
| **F-10** | **Tolerancia a Parkings Subterráneos (Extrapolación)** | ⭐⭐ (Bajo) | Alta | Fase 3 |

---

## 3. Especificación Detallada de Mejoras Funcionales

---

### F-01: Modo "Auto-Save" (Guardado Silencioso 100% Manos Libres)

#### Problema Conductual:
KiloMenos promete un *"registro manos libres"* a los suscriptores Premium. Sin embargo, si un conductor realiza 4 trayectos al día y no abre la aplicación después de cada uno, ninguno de esos kilómetros se asienta en el historial ni descuenta saldo del balance de renting. Tener que abrir la app obligatoriamente tras aparcar rompe la promesa de automatización.

#### Solución Funcional:
1. **Preferencia de Usuario**: Añadir en `Ajustes > Telemetría y Copilot` el selector:
   - `Guardado automático de viajes` (Activado por defecto en usuarios Premium con contrato vinculado).
2. **Consolidación en Segundo Plano**: Al recibir `ACTION_ACL_DISCONNECTED` o `IN_VEHICLE EXIT`, si la distancia acumulada es válida ($\ge 500\text{ m}$):
   - Invocar automáticamente `AddOdometerRecordUseCase` con una etiqueta autogenerada (ej. *"Trayecto inteligente · 17 sep, 14:30"*).
   - Asociar la entidad `TripRoute` con la polilínea codificada.
   - Ejecutar `ClearTrackingUseCase` para limpiar el estado persistido y armar el cooldown anti-flap.
3. **Notificación de Confirmación No Intrusiva**:
   - Título: *"Viaje de 14.5 km guardado en tu odómetro"*.
   - Mensaje: *"Balance actualizado: 1.250 km de margen"*.
   - Acciones: `[ Ver Ruta ]` (abre la app en el historial) y `[ Deshacer ]` (elimina el registro recién guardado).

```mermaid
graph TD
    Disconnect[Bluetooth Desconectado / Salida Vehículo] --> DistCheck{¿Distancia >= 500m?}
    DistCheck -- No (<500m) --> Discard[Descartar silencio / Limpiar estado]
    DistCheck -- Sí --> PrefCheck{¿Auto-Save Activado?}
    PrefCheck -- No --> Standby[Mantener en TrackingDataSource y mostrar TripCompletedCard en UI]
    PrefCheck -- Sí --> AutoSave[Ejecutar AddOdometerRecordUseCase en segundo plano]
    AutoSave --> ClearDS[Limpiar TrackingDataSource + Sellar cooldown 60s]
    AutoSave --> Notify[Notificación: 'Viaje guardado (+14.5 km)' con botón 'Deshacer']
```

---

### F-02: Timeout de Sesión Inactiva (Anti-Viajes Huérfanos Acumulativos)

#### Problema Detectado en Código:
En [`TrackingDataSource.kt#L67`](file:///c:/Users/josh_/AndroidStudioProjects/KmSafe/core/infrastructure/src/main/java/es/joshluq/kmsafe/infrastructure/local/datasource/TrackingDataSource.kt#L67):
```kotlin
if (existingDistance <= 0.0 || existingStartTime == null) {
    // Inicia nuevo viaje
} else {
    // Reanuda sesión existente acumulando distancia
}
```
Si el usuario conduce 20 km por la mañana al trabajo, no abre la app, y vuelve a arrancar el coche 8 horas después para volver a casa (otros 20 km), el sistema asume que es el **mismo viaje interrumpido**. El resultado es un único viaje artificial de 40 km con una línea recta incoherente en el mapa que une el trabajo con el punto de arranque.

#### Solución Funcional:
1. **Regla de Expiración de Sesión Inactiva (`SESSION_EXPIRATION_THRESHOLD = 45 minutos`)**:
   - Si `isTracking == false` y `existingDistance > 0`, evaluar el tiempo transcurrido desde la última parada (`lastDisconnectTimestamp`).
   - Si `(currentTime - lastDisconnectTimestamp) > 45 minutos`:
     - **Si Auto-Save está activo**: Consolidar inmediatamente el viaje pendiente anterior antes de iniciar el nuevo.
     - **Si Auto-Save está desactivado**: Archivar el viaje anterior como *"Viaje pendiente de confirmación en Historial"* y resetear los contadores a `0.0 m` para el nuevo trayecto.
2. **Garantía Invariante**: Ningún viaje automático acumulará trayectos separados por más de 45 minutos de motor apagado.

---

### F-03: Ventana de Gracia ante Paradas Cortas (Histeresis / Debounce de 3 Minutos)

#### Problema en Conducción Real:
En vehículos modernos con parada de motor agresiva, al repostar en gasolinera (apagar contacto 2 minutos), en paradas de peaje o al descargar a un pasajero, el Bluetooth se desconecta. Actualmente, `BluetoothConnectionReceiver` dispara de inmediato `ACTION_STOP`, cortando el viaje y generando dos trayectos separados de forma innecesaria.

#### Solución Funcional:
1. **Estado `STANDBY_GRACE_PERIOD` (Ventana de Gracia de 180 segundos)**:
   - Al recibir `ACTION_ACL_DISCONNECTED`, el servicio **no** finaliza el viaje inmediatamente.
   - Entra en modo de bajo consumo: suspende el flujo continuo de GPS y activa un temporizador de cuenta atrás de 3 minutos (`180_000 ms`).
2. **Reconexión Rápida (< 3 minutos)**:
   - Si el Bluetooth del coche vuelve a conectar dentro de la ventana de gracia, el temporizador se cancela y el GPS se reanuda de inmediato sin saltos en el odómetro. El trayecto se mantiene unificado.
3. **Expiración (> 3 minutos)**:
   - Si expira el temporizador sin reconexión, se procede al cierre formal del viaje (Auto-Save o `TripCompletedCard`).

```
[CONDUCIENDO] ──(Apaga contacto)──> [MODO GRACIA (3 min)]
                                         │            │
             (Arranca antes de 3 min) ────┘            └── (Pasan >3 min)
             ▼                                             ▼
       [REANUDA VIAJE]                               [FINALIZA Y GUARDA]
```

---

### F-04: Acciones Rápidas en la Notificación de Fin de Trayecto

#### Problema UX:
La notificación actual [`showTripFinishedNotification`](file:///c:/Users/josh_/AndroidStudioProjects/KmSafe/core/tracking/src/main/java/es/joshluq/kmsafe/core/tracking/LocationTrackingService.kt#L700) solo permite pulsar para abrir la aplicación. Esto obliga al conductor que aparca con prisas a desbloquear el teléfono para validar un viaje.

#### Solución Funcional:
Dotar a la notificación de fin de viaje de dos botones interactivos:
1. **Botón `[ ✅ Guardar (+X.X km) ]`**:
   - Envía un `PendingIntent` a un `BroadcastReceiver` en segundo plano.
   - Guarda el viaje con la fecha/hora exacta y cierra la notificación.
   - Retroalimentación auditiva/háptica sutil de éxito.
2. **Botón `[ 🗑️ Descartar ]`**:
   - Descarta los kilómetros registrados (ej. si conducía otra persona o se trataba de una prueba) y limpia el estado sin abrir la interfaz de usuario.
3. **Compatibilidad Wear OS / Pantalla de Bloqueo**: Las acciones son accesibles directamente desde relojes inteligentes o la pantalla Always-On Display.

---

### F-05: Claridad Semántica en la Confirmación del Odómetro (Total vs Incremento)

#### Problema de Comprensión:
Al pulsar "Confirmar" en la tarjeta `TripCompletedCard`, se despliega el BottomSheet con los km del viaje precargados en el campo `newOdometerValue` (ej. `14.50`). Sin embargo, el texto descriptivo dice *"Odómetro actual"*.
- **Riesgo**: Los conductores habituados a consultar el cuadro de mandos total (ej. `54.200 km`) sienten desconfianza al ver `14.50 km` y a menudo lo sobreescriben manualmente, rompiendo el modelo aditivo de KiloMenos.

#### Solución Visual / UX:
En `UpdateOdometerContent` y `TripCompletedCard`, implementar un bloque de **Transparencia Aritmética Aditiva**:

```
┌────────────────────────────────────────────────────────┐
│  🏁 VIAJE FINALIZADO                                  │
│  + 14,50 km recorridos                                 │
├────────────────────────────────────────────────────────┤
│  Odómetro anterior registrado:          54.230,00 km   │
│  Distancia de este viaje:               +   14,50 km   │
│  ───────────────────────────────────────────────────   │
│  Nuevo odómetro resultante:             54.244,50 km   │
├────────────────────────────────────────────────────────┤
│  [  Descartar  ]             [  Confirmar Odómetro  ]  │
└────────────────────────────────────────────────────────┘
```
- El usuario comprende con absoluta claridad que la app suma el incremento al odómetro anterior, eliminando cualquier duda o error de digitación.

---

### F-06: Detección Multivehículo en Flotas (Auto-Switch de Contrato por MAC)

#### Problema en Cuentas con Múltiples Vehículos:
Los usuarios de flotas o familias que tienen más de un vehículo registrado en KiloMenos sufren una limitación: si el usuario conduce habitualmente el Coche A, pero un fin de semana coge el Coche B, el autotracking no inicia porque `BluetoothConnectionReceiver` solo verifica contra el `linkedMac` del coche seleccionado en ese momento.

#### Solución Funcional:
1. **Caché Multi-MAC en `TrackingDeviceCache`**:
   - Almacenar en SharedPreferences el mapa completo de `Map<MAC, VehicleId>` de todos los vehículos asociados a la cuenta del usuario.
2. **Auto-Detección y Conmutación Inteligente**:
   - Si se detecta un evento `ACTION_ACL_CONNECTED` con una MAC que pertenece al Coche B (y el activo era el Coche A):
     1. Disparar internamente `SelectContractUseCase(vehicleId = B)`.
     2. Iniciar el tracking asignando la ruta y los kilómetros directamente al contrato del Coche B.
     3. Notificar: *"Conectado a Cupra Formentor (Coche B). Cambiando vehículo activo..."*.

---

### F-07: Coordenada SSOT para Detección de Gasolinera (`checkStationArrival`)

#### Problema Técnico-Funcional:
[`BluetoothConnectionReceiver.kt#L410`](file:///c:/Users/josh_/AndroidStudioProjects/KmSafe/core/tracking/src/main/java/es/joshluq/kmsafe/core/tracking/BluetoothConnectionReceiver.kt#L410) invoca `getLastLocation(context)` al desconectar el Bluetooth. Si el coche está bajo la marquesina de hormigón/metal de la estación de servicio, la última ubicación del sistema puede ser nula o tener un error de 200-300 metros de un punto anterior en carretera.

#### Solución Funcional:
- En lugar de consultar `getLastLocation` en frío:
  1. `LocationTrackingService` guarda en `TrackingDataSource` la **última coordenada GPS filtrada y validada** antes de apagarse (`lastValidLatitude`, `lastValidLongitude`).
  2. `checkStationArrival` lee esa coordenada precisa (que fue capturada justo al entrar y aparcar en el surtidor).
  3. La tasa de acierto de la notificación *"¿Has repostado en Repsol?"* aumenta drásticamente.

---

### F-08: Muestreo GPS Adaptativo (Ahorro de Batería en Autovía)

#### Problema de Eficiencia Energética:
Actualmente se solicitan ubicaciones cada 2-3 segundos con precisión máxima continuada. En trayectos largos por autopista (3-5 horas en línea recta), esto provoca un consumo de batería innecesario del 12-18%.

#### Solución Funcional:
Implementar un algoritmo de **Frecuencia Adaptativa en Marcha**:
- **Modo Urbano / Maniobra** (Velocidad $< 60\text{ km/h}$ o cambios de rumbo $\Delta \text{bearing} > 15^\circ$): Muestreo de alta resolución cada **2-3 segundos**.
- **Modo Crucero Autovía** (Velocidad $\ge 80\text{ km/h}$ y rumbo recto durante $> 60\text{ segundos}$): Espaciar el intervalo de solicitud a **6-8 segundos**.
- **Impacto**: Reduce el drenaje de batería hasta un **35%** en viajes interurbanos largos sin degradar la precisión del kilometraje total ni la estética de la polilínea.

---

### F-09: Clasificación de Trayectos (Laboral vs Personal)

#### Valor Añadido para Renting y Autónomos:
Para autónomos y flotas corporativas, la deducción del renting (IRPF e IVA) requiere justificar el porcentaje de uso laboral frente al particular.

#### Solución Funcional:
1. **Reglas Inteligentes por Horario**:
   - Opción en Ajustes: *"Marcar como trayecto profesional de Lunes a Viernes de 08:00 a 19:00"*.
2. **Chip Rápido de 1-Tap**:
   - En la notificación de fin de viaje o en la tarjeta de confirmación, permitir alternar con un toque:
     `[ 💼 Profesional ]` / `[ 🏠 Personal ]`.
   - Este metadato se almacena en el `OdometerRecord` y permite exportar informes certificados para asesorías o empresas de renting.

---

### F-10: Tolerancia a Parkings Subterráneos (Extrapolación de Fin de Ruta)

#### Caso Borde en Garajes:
Al entrar a un parking subterráneo (-1, -2 o -3), la señal GPS se bloquea por completo. El usuario conduce 150 metros dentro del garaje hasta su plaza y apaga el motor. Actualmente, esos últimos metros no se registran.

#### Solución Funcional:
1. **Detección de Pérdida Repentina de Señal en Tránsito**:
   - Si la señal GPS pasa bruscamente de buena precisión a `0 satélites` con velocidad previa $>10\text{ km/h}$, el sistema asume entrada en túnel o parking.
2. **Sellado Seguro de Ruta**:
   - Utilizar la última coordenada válida como punto de cierre.
   - Si el dispositivo cuenta con sensor de pasos/odometría inercial, estimar el metraje adicional o etiquetar el viaje con el aviso informativo: *"Finalizado en zona sin cobertura GPS"*.

---

## 4. Plan de Ejecución por Fases (Roadmap)

```mermaid
gantt
    title Roadmap de Mejoras de Autotracking
    dateFormat  YYYY-MM-DD
    section Fase 1: Resiliencia & Zero-Friction Inmediato
    F-02 Timeout de Sesión Inactiva (Anti-Huérfanos) :done, p1, 2026-10-01, 3d
    F-05 Claridad Odómetro Resultante en UI         :done, p2, after p1, 2d
    F-07 Coordenada SSOT para Gasolinera            :done, p3, after p2, 2d
    F-04 Acciones Rápidas en Notificación           :active, p4, after p3, 4d
    F-01 Modo Auto-Save Silencioso (Premium)        :p5, after p4, 5d

    section Fase 2: Experiencia Continua & Flotas
    F-03 Ventana de Gracia / Histeresis (3 min)     :p6, 2026-10-20, 6d
    F-06 Detección Multivehículo en Flotas          :p7, after p6, 5d
    F-09 Clasificación Laboral vs Personal          :p8, after p7, 4d

    section Fase 3: Optimización Avanzada & Eficiencia
    F-08 Muestreo GPS Adaptativo (Batería)          :p9, 2026-11-10, 6d
    F-10 Tolerancia a Parkings Subterráneos         :p10, after p9, 5d

    section Casos de Borde en Producción
    Defecto 1: Píldora 0.0km & Cooldown 15s         :done, e1, 2026-09-17, 1d
    Defecto 2: Anti-Deriva Peatonal & Heartbeat BT  :done, e2, 2026-09-17, 1d
```

---

## 5. Arquitectura y Reglas de Implementación (Cumplimiento AGENTS.md)

Para garantizar que estas mejoras no introduzcan regresiones técnicas, deben cumplirse estrictamente los siguientes principios arquitectónicos:

1. **Mandato "UseCase First" (Regla 18)**:
   - Todo nuevo flujo (ej. auto-save, cambio de vehículo, descarte rápido) debe encapsularse en un caso de uso puro en `:core:domain` (ej. `AutoSaveTripUseCase`, `DiscardTrackedTripUseCase`).
2. **Repositorios Sin Estado (Regla 8)**:
   - El estado de la ventana de gracia y los timestamps de desconexión deben persistir en `TrackingDataSource` (`StorageProvider`), **nunca** en variables en memoria dentro de `TrackingRepositoryImpl`.
3. **Pureza de `:core:domain` (Regla 15.3)**:
   - Los modelos de datos de dominio no deben contener dependencias de Android SDK (`android.location.*`, `PendingIntent`, etc.).
4. **Resiliencia en Android 14+ (Regla 16.1)**:
   - Cualquier acción originada desde las notificaciones o receivers debe ejecutar servicios o tareas dentro de los límites de ejecución en segundo plano y las APIs de WorkManager o Foreground Services según corresponda.

---

## 6. Diagnósticos de Casos de Borde en Producción (Fases 4 y 5) ✅ COMPLETADO

### 6.1 Defecto 1: Aparición Efímera de la Píldora con 0.0 km al Guardar Registro & Viajes Continuos ✅ RESUELTO

- **Estado**: **Completado & Verificado en Producción**
- **Síntoma Observado**: Al pulsar "Guardar Registro" en el BottomSheet mientras el vehículo sigue conectado por Bluetooth, durante el estado de carga (`isSaving == true`), la píldora de telemetría (`FloatingTelemetryPill`) aparece en pantalla indicando `0.0 km` y desaparece inmediatamente cuando finaliza la sincronización remota.
- **Causa Raíz Técnica**:
  1. En `handleConfirmTrackedTrip()`, se invoca `stopTrackingUseCase()`, el cual establece `KEY_IS_TRACKING = false` en `TrackingDataSource`.
  2. **Vulnerabilidad de Cooldown**: `TrackingDataSource.stopTracking()` **no** escribía `KEY_LAST_TRIP_END_TIME` (el cooldown anti-flap de 60s sólo se armaba en `clear()`).
  3. Mientras el usuario editaba o pulsaba "Guardar" y se realizaba el POST de red, el coche continuaba emparejado a nivel de Bluetooth.
  4. Una re-evaluación del entorno (transición de Activity Recognition `IN_VEHICLE ENTER` o refresco reactivo de estado en `OverviewViewModel`) comprobaba que no había cooldown activo ni tracking grabando, iniciando un viaje espurio con `0.0 km`.
  5. Al terminar el guardado de red en `AddOdometerRecordUseCase.Output.Success`, `OverviewViewModel` llamaba a `clearTrackingUseCase()`, eliminando `KEY_IS_TRACKING` y ocultando abruptamente la píldora.
- **Solución Implementada**:
  - **Armado Universal de Cooldown**: En [`TrackingDataSource.kt`](file:///c:/Users/josh_/AndroidStudioProjects/KmSafe/core/infrastructure/src/main/java/es/joshluq/kmsafe/infrastructure/local/datasource/TrackingDataSource.kt), `stopTracking()` registra siempre `KEY_LAST_TRIP_END_TIME = System.currentTimeMillis()`.
  - **Cooldown Dinámico de 15s con Bypass de Ignición**: En [`LocationTrackingService.kt`](file:///c:/Users/josh_/AndroidStudioProjects/KmSafe/core/tracking/src/main/java/es/joshluq/kmsafe/core/tracking/LocationTrackingService.kt), el cooldown se ajustó a `AUTO_TRACKING_COOLDOWN_MS = 15_000L` (15 segundos) y se implementó un bypass para eventos `ACTION_START_BT_AUTO` (`isBluetoothFastPath == true`), permitiendo el arranque inmediato de viajes continuos al reencender el motor sin esperas artificiales.
  - **Guardia de UI en Compose**: En [`OverviewScreen.kt`](file:///c:/Users/josh_/AndroidStudioProjects/KmSafe/feature/overview/src/main/java/es/joshluq/kmsafe/feature/overview/OverviewScreen.kt), la visibilidad de `FloatingTelemetryPill` está condicionada a `isVisible = state.isTracking && !state.showBottomSheet && !state.isSaving`.
  - **Guardia de ViewModel**: En [`OverviewViewModel.kt`](file:///c:/Users/josh_/AndroidStudioProjects/KmSafe/feature/overview/src/main/java/es/joshluq/kmsafe/feature/overview/OverviewViewModel.kt), `observeTracking()` suprime `isTracking = true` en el estado de la UI mientras el BottomSheet se encuentra abierto o guardando.
  - **Verificación de Tests**: Cubierto con pruebas unitarias en [`TrackingDataSourceTest.kt`](file:///c:/Users/josh_/AndroidStudioProjects/KmSafe/core/infrastructure/src/test/kotlin/es/joshluq/kmsafe/infrastructure/local/datasource/TrackingDataSourceTest.kt) y [`OverviewViewModelTest.kt`](file:///c:/Users/josh_/AndroidStudioProjects/KmSafe/feature/overview/src/test/kotlin/es/joshluq/kmsafe/feature/overview/OverviewViewModelTest.kt).

---

### 6.2 Defecto 2: Deriva Peatonal (0.5 km) por Ausencia de Actividades Antagónicas ✅ RESUELTO

- **Estado**: **Completado & Verificado en Producción**
- **Síntoma Observado por QA**: Tras bajarse del vehículo y caminar, el usuario observaba que el tracking permanecía activo en la barra de estado (`GPS_RECORDING`), acumulando hasta 0.5 km de trayecto a pie, hasta que entraba a la aplicación y cancelaba manualmente en `TripCompletedCard`.
- **Causa Raíz Técnica**:
  1. **Subscripción Incompleta en `AutoTrackingManager`**: Sólo se registraban transiciones de `DetectedActivity.IN_VEHICLE` (ENTER y EXIT).
  2. **Comportamiento Real de Google Play Services**: En dispositivos físicos, al bajarse del coche y caminar, Activity Recognition suele transicionar directamente a `DetectedActivity.WALKING` o `DetectedActivity.ON_FOOT` sin emitir el evento `IN_VEHICLE EXIT` (o demorándolo >10 minutos).
  3. **Evasión del Filtro de Velocidad**: El umbral `MIN_SPEED_THRESHOLD_MPS = 1.5` (~5.4 km/h) era superado a paso ligero, sumando metros peatonales al odómetro.
- **Solución Implementada**:
  - **Registro de Actividades Antagónicas**: En [`AutoTrackingManager.kt`](file:///c:/Users/josh_/AndroidStudioProjects/KmSafe/core/infrastructure/src/main/java/es/joshluq/kmsafe/infrastructure/repository/tracking/AutoTrackingManager.kt), se añadió la suscripción a `DetectedActivity.WALKING` (`ENTER`) y `DetectedActivity.ON_FOOT` (`ENTER`).
  - **Caché Síncrono de Estado Bluetooth**: En [`TrackingDeviceCache.kt`](file:///c:/Users/josh_/AndroidStudioProjects/KmSafe/core/tracking/src/main/java/es/joshluq/kmsafe/core/tracking/TrackingDeviceCache.kt), se implementó `isBluetoothConnected(): Boolean` y `setBluetoothConnected(Boolean)`, sincronizado de inmediato en [`BluetoothConnectionReceiver.kt`](file:///c:/Users/josh_/AndroidStudioProjects/KmSafe/core/tracking/src/main/java/es/joshluq/kmsafe/core/tracking/BluetoothConnectionReceiver.kt) tanto en el `onReceive` síncrono como en corrutina.
  - **Regla de Detención Peatonal Inmediata**: En [`ActivityTransitionReceiver.kt`](file:///c:/Users/josh_/AndroidStudioProjects/KmSafe/core/tracking/src/main/java/es/joshluq/kmsafe/core/tracking/ActivityTransitionReceiver.kt), si se detecta `WALKING ENTER` u `ON_FOOT ENTER` y el Bluetooth del coche no está conectado (o el vehículo no posee Bluetooth), se envía inmediatamente `ACTION_STOP` a `LocationTrackingService`.
  - **Heartbeat de Bluetooth en Servicio**: En [`LocationTrackingService.kt`](file:///c:/Users/josh_/AndroidStudioProjects/KmSafe/core/tracking/src/main/java/es/joshluq/kmsafe/core/tracking/LocationTrackingService.kt), se ejecuta `startBluetoothHeartbeat()` cada 15 segundos durante la grabación activa, validando la presencia del enlace mediante `isBluetoothDeviceConnected()`. Si el enlace se pierde y la velocidad es $< 10\text{ km/h}$, detiene automáticamente la grabación y formaliza el fin del viaje.
