---
name: new-feature
description: >-
  Pipeline de diseÃ±o e implementaciÃ³n modular de una nueva feature en KmSafe. GuÃ­a la creaciÃ³n paso a paso cumpliendo estrictamente la arquitectura en AGENTS.md: especificaciÃ³n en 4 capas, separaciÃ³n Route/Screen, mandato UseCase First en :core:domain, tokens CanvasKit y tests unitarios con Turbine.
---

# New Feature â Pipeline de ConstrucciÃ³n Modular (KmSafe)

Este comando guÃ­a la *especificaciÃ³n, diseÃ±o e implementaciÃ³n de una nueva funcionalidad* en KmSafe, garantizando el cumplimiento riguroso de `AGENTS.md` y evitando atajos arquitectÃ³nicos.

Se apoya en `android-staff-engineer-compose`, `android-testing` y la plantilla `.agents/templates/component-spec-template.md`.

---

## CuÃ¡ndo usar este comando
- Para crear una nueva pantalla, pestaÃ±a, diÃ¡logo o flujo dentro de un mÃ³dulo `:feature:*`.
- Para aÃ±adir un nuevo caso de uso con su interfaz de usuario y pruebas unitarias correspondientes.

---

## Protocolo de EjecuciÃ³n (Paso a Paso)

Cuando el usuario invoque `/new-feature [nombre o descripciÃ³n de la feature]`:

### Fase 1: EspecificaciÃ³n del Componente (Component Spec)
Genera la especificaciÃ³n usando `.agents/templates/component-spec-template.md`:
1. *Mapeo en 4 Capas Visuales*:
   - *Layer 1 (The Pulse)*: MÃ©trica hero de alta visibilidad (< 2s).
   - *Layer 2 (Decision Radar)*: Tarjetas glanceables de insights / comparaciÃ³n.
   - *Layer 3 (Zero-Friction Action)*: Chips de 1-tap, FAB auditable, presets.
   - *Layer 4 (Diagnostic Feed)*: Feed histÃ³rico contextual por ciclos.
2. *Contratos MVI*:
   - `UiState`: Data class inmutable con `@Immutable`.
   - `UiAction`: Sealed interface con los intents del usuario.
   - `UiEffect`: (Opcional) Canal de efectos de un solo disparo (navegaciÃ³n, snackbar).

### Fase 2: Mandato "UseCase First" (Dominio)
1. Identifica o define los UseCases en `::core:domain` (`pluginkit.jvm.library`).
2. *Regla de Oro*: Prohibido inyectar `*Repository` directamente en ViewModels. Todo pasa por UseCases especializados (`FlowUseCase` o `UseCase` de FoundationKit).
3. Asegura pureza Kotlin/JVM (cero dependencias de Android SDK en dominio).

### Fase 3: ImplementaciÃ³n de PresentaciÃ³n (Coordinator / Route)
1. *e[Feature]Route.kt*:
   - Inyecta `hiltWiewModel(key = scopingKey)` con clave determinista si es entidad o sesiÃ³n.
   - Observa `collectAsStateWithLifecycle()`.
   - Maneja callbacks de navegaciÃ³n y `savedStateHandle`.
2. *e[Feature]Screen.kt*:
   - Composable puramente stateless.
   - Utiliza exclusivamente tokens de `CanvasKitTheme` (tipografÃ­a, colores, espaciados).
   - Elementos interactivos con touch targets >= 48dp y `contentDescription` accesible.
   - Previews obligatorias para Loading, Content y Error.
3. *e[Feature+ViewModel.kt*:
   - MVI con `StateFlow<UiState>`.in
   - InyecciÃ³n de `DispatcherProvider` (main-safety).
   - Cero cÃ¡lculos matemáticos de negocio en el ViewModel (delegados a dominio).

### Fase 4: Pruebas Automatizadas
1. Genera el test unitario `[Feature]ViewModelTest.kt`:
   - Usa *Turbine* (`viewModel.uiState.test { ... }`).
   - Usa *MockK* para mockear exclusivamente los UseCases inyectados.
   - Usa `StandardTestDispatcher` para control determinista de corrutinas.
