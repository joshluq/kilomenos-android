# Plan de Rediseño: Projection Risk Sentinel & Runway Planner (KmSafe)

Este documento define la especificación funcional, de diseño (UX/UI) y técnica para la renovación completa de la pantalla de **Proyección Contractual** (`ProjectionAnalysisScreen`). La arquitectura implementada corresponde al modelo **Híbrido: Financial Risk Sentinel (Capa 1) + Runway Timeline (Capa 2) + Interactive Sandbox con Ritmo y Planificador de Escapadas (Capa 3) + Remedial Advisory (Capa 4)**, garantizando una separación nítida de valor entre los usuarios **FREE** (proyección diagnóstica con teasers) y los usuarios **PREMIUM** (simulador multivariable, apilador de escapadas ilimitadas con escapadas personalizadas y prescripción de ritmo remedial a 0 €).

---

## 1. Visión de Producto y Propuesta de Valor

Mientras que `Overview` (Tab 1) responde a *"¿Cómo estoy hoy?"*, la pantalla de `Projection` (Tab 4) debe responder a la pregunta que quita el sueño al conductor de renting:
> **"¿Me van a cobrar penalización cuando devuelva el coche? ¿Cuánto exactamente y cómo lo evito?"**

### 1.1 El Dolor del Conductor de Renting / Leasing (Target 30-55 años)
1. **La factura de regularización al vencimiento:** Al entregar el coche tras 36 o 48 meses, un exceso de 10.000 km a 0,08 €/km supone una factura sorpresa de **800 € a 1.500 € + IVA**.
2. **Incertidumbre en vacaciones y cambios de rutina:** Dudas constantes sobre si hacer un viaje largo en verano o un cambio de oficina obligará a renegociar el contrato o pagar penalizaciones.
3. **Falta de acción preventiva:** Saber que uno va "pasado" no sirve de nada si la app no le dice el ritmo diario exacto (`km/día`) al que debe conducir a partir de hoy para llegar a 0 € de penalización.

---

## 2. Matriz de Valor: Nivel FREE vs. Nivel PREMIUM

La estrategia de monetización no oculta la pantalla al usuario gratuito; **muestra el diagnóstico de riesgo básico y despierta el deseo de mitigación mediante teasers de alto valor**:

| Capacidad / Módulo | 🆓 Nivel FREE (Básico / Diagnóstico) | 💎 Nivel PREMIUM (Proactivo / Sandbox) |
| :--- | :--- | :--- |
| **Capa 1: Centinela Financiero (Hero)** | **Balance Final, Penalización y Runway Bar:** Visualización clara de exceso/colchón en km y penalización estimada con la barra compacta `RunwayCeilingBar`. | **Centinela Auditado Completo:** Integrado con la tarifa pactada en contrato (€/km extra) y recálculo instantáneo multivariable. |
| **Capa 2: Horizonte de Agotamiento (Runway Timeline)** | **Teaser Bloqueado:** Candado elegante: *"Tus km se agotarán antes de la entrega. Descubre el mes exacto con KmSafe Premium"*. | **Fecha Crítica Exacta en Calendario:** Visualización de mes/año de colisión (`Marzo 2027`) vs. mes de fin de contrato (`Noviembre 2027`) con barra de desvío temporal. |
| **Capa 3A: Simulador de Ritmo Diario** | **Presets + Slider:** Fila de presets (`-20%`, `-10%`, `Normal`, `+10%`, `+20%`) sincronizados con slider continuo de ritmo diario (0 a 150 km/día). | **Presets + Slider con Recálculo Global:** Permite modelar cambios de hábito de forma interactiva en tiempo real. |
| **Capa 3B: Planificador de Escapadas (Trip Planner)** | **1 Solo Viaje de Prueba:** Permite simular 1 escapada (presets o personalizada). Si intenta apilar un segundo viaje, salta el diálogo de suscripción. | **Multi-Trip Stacking Ilimitado + Modal Personalizado:** Añade y combina múltiples viajes (`Escapada`, `Verano` o `+ Personalizado` con modal para ingresar título y km exactos). |
| **Capa 4: Asesor de Ritmo Remedial (Copilot Advisory)** | **Teaser Persuasivo:** Advierte que existe un ritmo diario exacto para revertir la penalización a 0,00 €, invitando a desbloquearlo. | **Prescripción Quirúrgica a 0 €:** *"Para neutralizar la penalización a 0,00 €, ajusta tu ritmo a 38,4 km/día a partir de mañana"*. |

---

## 3. Jerarquía Visual en 4 Capas (Clean UI Pattern de `AGENTS.md`)

```
┌────────────────────────────────────────────────────────┐
│                      PROYECCIÓN                        │
├────────────────────────────────────────────────────────┤
│  ⚡ CAPA 1: EL CENTINELA HERO (Financial Sentinel)     │
│  ┌──────────────────────────────────────────────────┐  │
│  │  ESTADO AL VENCER (30 Nov 2027)         RIESGO ⚠️│  │
│  │                                                  │  │
│  │  - 2.450 km              196,00 €                │  │
│  │  EXCESO PREVISTO         PENALIZACIÓN ESTIMADA   │  │
│  │                                                  │  │
│  │  0 km              60.000 km (Techo)   62.450 km │  │
│  │  ┌───────────────────────┬────────────┐          │  │
│  │  │ Consumo Contrato (96%)│ DESBORDE 🔴│ ➔ +2.450 │  │
│  │  └───────────────────────┴────────────┘          │  │
│  │                          ▲ Hito Contractual      │  │
│  │  Odómetro final previsto: 62.450 / 60.000 km     │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│  🎯 CAPA 2: RADAR DE AGOTAMIENTO (Runway Timeline)     │
│  ┌──────────────────────────────────────────────────┐  │
│  │  [PREMIUM] FECHA DE COLISIÓN                     │  │
│  │  📅 Agotarás tus km en: MARZO 2027               │  │
│  │  ⚠️ 8 meses antes de que venza tu contrato       │  │
│  │  [═══════════════░░░░░░░░░░] 74% Runway agotado  │  │
│  │  ──────────────────────────────────────────────  │  │
│  │  [FREE: Candado blur con botón "Desbloquear"]    │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│  🎛️ CAPA 3: SANDBOX INTERACTIVO (Ritmo & Escapadas)    │
│  ┌──────────────────────────────────────────────────┐  │
│  │  SIMULADOR DE RITMO DIARIO                       │  │
│  │  [ -20% ]  [ -10% ]  [ Normal ]  [ +10% ]  [ +20% ] │  │
│  │  ──●───────────────────────── 42.5 km/día        │  │
│  ├──────────────────────────────────────────────────┤  │
│  │  PLANIFICADOR DE ESCAPADAS                       │  │
│  │  [ Escapada (350 km) ] [ Verano ] [ + Personalizado]│
│  │  Viajes simulados:                               │  │
│  │  • Vacaciones Agosto: +1.500 km        [ x ]     │  │
│  │  • Viaje a Valencia:  +450 km          [ x ]     │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│  🩺 CAPA 4: ASESOR REMEDIAL (Prescripción Copilot)     │
│  ┌──────────────────────────────────────────────────┐  │
│  │  💡 RECETA DEL COPILOTO PARA 0 € PENALIZACIÓN    │  │
│  │  Para evitar pagar los 196 €, reduce tu ritmo a: │  │
│  │  👉 36,8 km/día (Disponible con KmSafe Premium)  │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

---

## 4. Especificación Detallada de Componentes Visuales

Todos los componentes emplean exclusivamente tokens de **`CanvasKitTheme`** (colores, tipografía y espaciado), con soporte completo para tema claro y oscuro.

### 4.1 Capa 1: `ProjectionSentinelCard` con `RunwayCeilingBar` (Hero Glanceable)
- **Propósito:** Responder en menos de 2 segundos a *"¿gano o pierdo dinero al final?"* con máxima densidad vertical.
- **Sustitución del Gauge Circular:** Se descarta el arco/velocímetro circular de 180° (que consumía 220 dp de altura vertical y empujaba el simulador fuera de pantalla) a favor de la **Runway Ceiling Bar (Barra de Techo Contractual)**:
  - **Altura ultracompacta (44–48 dp):** Permite que la Capa 1 completa y la Capa 2 queden *above the fold*.
  - **Metáfora visual:** Límite presupuestario. El usuario percibe de inmediato si se mantiene dentro del contenedor o si "se desborda por la derecha".
- **Estructura Interna:**
  - **Cabecera:** Fecha de vencimiento (`"Vencimiento: 30 Nov 2027"`) + Badge semántico:
    - 🟢 `ZONA SEGURA` (`CanvasKitTheme.colors.success`) si `simulatedFinalBalance >= 0`.
    - 🔴 `RIESGO PENALIZACIÓN` (`CanvasKitTheme.colors.error`) si `simulatedFinalBalance < 0`.
  - **Métrica Principal Dual:**
    - **Izquierda:** Kilómetros de balance (`+ 1.500 km` en verde o `- 2.450 km` en rojo, tipografía `headingLarge` en negrita).
    - **Derecha:** Cifra en Euros de penalización estimada (`0,00 €` protegido o `196,00 €` en error).
  - **Componente `RunwayCeilingBar`:**
    - **Geometría Unificada (8.dp de radio exterior):** Ambos estados (Safe y Risk) comparten el mismo contenedor exterior con esquinas redondeadas continuas (`clipPath`), eliminando cualquier discontinuidad visual o cortes rectos en los bordes. Incluye borde sutil de alta definición (`borderSubtle`).
    - **Modo Seguro (`balance >= 0`):** El 100% del ancho representa el techo contractual. El progreso se anima hasta el porcentaje consumido en color éxito (`success`), dejando visible el colchón disponible en la pista y el marcador vertical de fin de contrato a la derecha. Leyenda inferior: `[0 km] --- [Colchón: +1.500 km] --- [Techo: 60.000 km]`.
    - **Modo Riesgo / Desbordamiento (`balance < 0`):** El 80% del ancho acoge el 100% contractual delimitado por el marcador vertical de hito de techo. El 20% restante acoge el bloque de desbordamiento en rojo (`error`) animado con el porcentaje de exceso. Leyenda inferior perfectamente alineada con la pista: `[0 km] --- [Techo: 60.000 km] --- [+2.450 km exceso]`.
  - **Resumen Inferior de Odómetro:** `"Odómetro final previsto: XX.XXX / YY.YYY km"`.

### 4.2 Capa 2: `RunwayTimelineCard` (Horizonte de Agotamiento)
- **Propósito:** Mostrar la discrepancia entre el tiempo transcurrido del contrato y el consumo de km.
- **Lógica de Cálculo:**
  $$\text{Días de autonomía de km} = \frac{\text{Km disponibles restantes}}{\text{Ritmo medio diario}}$$
  $$\text{Fecha de agotamiento} = \text{Fecha actual} + \text{Días de autonomía de km}$$
- **Variante Premium:**
  - Icono de calendario (`Icons.Default.DateRange`).
  - Título: *"HORIZONTE DE AGOTAMIENTO"*.
  - Indicador de impacto: *"Agotarás tus km en: Marzo 2027 (8 meses antes de que venza tu contrato)"*.
  - Mini-barra de progreso con porcentaje de Runway agotado.
- **Variante Free (Smart Teaser):**
  - Fondo de tarjeta sutil (`CanvasKitCardVariant.Outlined`).
  - Icono de candado / corona (`brandAccent`).
  - Texto persuasivo: *"Tus hábitos indican que agotarás tus km antes de la entrega. Descubre el mes exacto con KmSafe Premium."*
  - Botón: `[ Desbloquear Fecha de Colisión ]` que dispara `onUpgradeClick()`.

### 4.3 Capa 3: Sandbox Interactivo (`PaceSimulatorCard` + `TripPlannerCard`)
- **Bloque 3A: `PaceSimulatorCard` (Simulador de Ritmo Diario):**
  - Mantiene el diseño ágil y táctil sin campos de texto directos: Fila de chips de ajuste rápido (`-20%`, `-10%`, `Normal`, `+10%`, `+20%`) + Slider continuo de 0 a 150 km/día.
  - Al interactuar con el slider o los presets, se recalcula la simulación instantáneamente.
- **Bloque 3B: `TripPlannerCard` (Planificador de Escapadas con Entrada Personalizada):**
  - Chips rápidos en cabecera: `[ Escapada (350 km) ]`, `[ Verano (1.500 km) ]`, `[ + Personalizado ]`.
  - **Modal de Escapada Personalizada (`CanvasKitDialog`):** Al pulsar `[ + Personalizado ]`, se abre un diálogo modal con:
    - Campo de texto: *"Nombre de la escapada"* (ej. `"Viaje a Valencia"`).
    - Campo numérico: *"Kilómetros estimados"* con sufijo `"km"` y validación de enteros positivos.
    - Botones de *"Cancelar"* y *"Añadir Viaje"*.
  - **Modo Free:** Permite 1 viaje de prueba. Si el usuario intenta apilar más escapadas, se muestra un banner de actualización a Premium con botón `[ PRO ]`.
  - **Modo Premium:** Apilador ilimitado (`PlannedTripRow`) con título, km y botón para eliminar individualmente cada viaje.

### 4.4 Capa 4: `RemedialAdvisoryCard` (Receta Médica del Copiloto)
- **Propósito:** Si el usuario tiene penalización, darle la solución exacta para revertirla.
- **Lógica de Cálculo:**
  $$\text{Ritmo Remedial (Target)} = \frac{\text{Km restantes de contrato}}{\text{Días restantes de contrato}}$$
- **Comportamiento:**
  - **Si el contrato está en verde (`balance >= 0`):** Mensaje de tranquilidad: *"Vas perfecto. Sigue disfrutando del camino sin preocupaciones."*
  - **Si el contrato está en rojo (`balance < 0`):**
    - **Premium:** Muestra en grande: *"Para neutralizar la penalización a 0,00 €, ajusta tu ritmo a: 36,8 km/día a partir de mañana"*.
    - **Free:** Teaser: *"Existe un ritmo diario exacto para revertir esta penalización y devolver el coche a 0,00 €. Desbloquéalo con Premium."* con botón `[ Desbloquear Ritmo Remedial ]`.

---

## 5. Arquitectura Técnica & "UseCase First"

Siguiendo el mandato de **`AGENTS.md`**:
- El ViewModel no realiza cálculos matemáticos directos ni dispersos; orquesta la simulación a través de **`SimulateContractProjectionUseCase`** en `:core:domain`.
- Se expone un `State` inmutable y una interfaz sellada `Event`.

### 5.1 Caso de Uso de Dominio: `SimulateContractProjectionUseCase`
- Ubicado en `:core:domain:usecase:projection:SimulateContractProjectionUseCase`.
- Pureza Kotlin/JVM absoluta (cero dependencias de Android).
- Entrada:
  - `totalContractKms: Double`
  - `startOdometer: Double`
  - `realKmConsumed: Double`
  - `contractDurationDays: Int`
  - `daysElapsed: Int`
  - `penaltyPricePerKm: Float`
  - `simulatedDailyKm: Float`
  - `plannedTrips: List<PlannedTrip>`
- Salida: `ProjectionSimulationResult`:
  - `simulatedTotalKmConsumed: Double`
  - `simulatedProjectedOdometer: Double`
  - `simulatedFinalBalance: Double`
  - `estimatedPenaltyEuro: Double`
  - `exhaustionDateDays: Double?`
  - `remedialDailyKm: Double?`
  - `isExceeded: Boolean`
- **Cobertura de Pruebas:** 100% testeado con JUnit en `SimulateContractProjectionUseCaseTest.kt`.

### 5.2 Estado Inmutable (`State` en `Contract.kt`)

```kotlin
data class State(
    val isLoading: Boolean = true,
    val isPremium: Boolean = false,
    
    // Contrato base
    val totalContractKms: Double = 0.0,
    val startOdometer: Double = 0.0,
    val contractEndDateMillis: Long = 0L,
    val daysRemaining: Long = 0L,
    val penaltyPricePerKm: Float = 0.05f,
    
    // Estado proyectado real (Status Quo)
    val baselineProjection: TripProjection? = null,
    val realDailyAverage: Float = 0f,
    
    // Simulación activa
    val simulatedDailyKm: Float = 0f,
    val paceMultiplier: Float = 1.0f,
    val plannedTrips: List<PlannedTrip> = emptyList(),
    val totalPlannedTripsKm: Int = 0,
    
    // Resultados de la simulación
    val simulatedProjectedTotalKms: Double = 0.0,
    val simulatedFinalBalance: Double = 0.0,
    val estimatedPenalty: Double = 0.0,
    val exhaustionDateMillis: Long? = null,
    val monthsAheadOrBehind: Int = 0,
    val remedialDailyKm: Double? = null,
    
    val error: TextProvider? = null
) : UiState {
    val isOverLimit: Boolean get() = simulatedFinalBalance < 0
}
```

---

## 6. Estado de Implementación & Verificación

- [x] **Dominio (`:core:domain`):**
  - Entidad `PlannedTrip` y resultado `ProjectionSimulationResult`.
  - Caso de uso `SimulateContractProjectionUseCase` implementado y testeado al 100%.
- [x] **Infraestructura (`:core:infrastructure`):**
  - Binding de DI en `UseCaseModule.kt`.
- [x] **UI Foundations (`:core:ui`):**
  - Formateadores `formatShortDate` y `formatMonthYear` en `DateUtils.kt`.
- [x] **Componentes Visuales (`:feature:projection:components`):**
  - `RunwayCeilingBar.kt` (Barra horizontal compacta de 20 dp con hito de techo contractual y zona de desbordamiento).
  - `ProjectionSentinelCard.kt` (Hero centinela financiero con `RunwayCeilingBar`).
  - `RunwayTimelineCard.kt` (Radar temporal con variante Premium y Free Teaser).
  - `PaceSimulatorCard.kt` (Simulador de ritmo diario con 5 presets y slider continuo).
  - `TripPlannerCard.kt` (Planificador de escapadas con presets, modal personalizado `CanvasKitDialog` y multi-trip stacking).
  - `RemedialAdvisoryCard.kt` (Prescripción médica del copiloto a 0,00 €).
- [x] **Pantalla y Navegación (`:feature:projection` & `:feature:dashboard`):**
  - Integración completa de las 4 capas en `ProjectionAnalysisScreen.kt`.
  - ViewModel orquestador con MVI limpio en `ProjectionAnalysisViewModel.kt`.
  - Navegación al paywall (`Destination.SubscriptionPaywall`) desde los teasers.
- [x] **Strings & Localización:**
  - Soporte completo bilingüe en `es` (`values/strings.xml`) y `en` (`values-en/strings.xml`).
- [x] **Verificación:**
  - Tests unitarios ejecutados exitosamente (`:feature:projection:testDebugUnitTest`).
  - Compilación de fuentes completa exitosa (`:app:compileDevDebugSources`).
