# AGENTS.md - SafeKm Architecture & Guidelines 🚗

## 1. Objective
El objetivo de **SafeKm** es proporcionar una herramienta de alta calidad para conductores de renting o leasing, permitiéndoles gestionar su consumo de kilometraje de forma diaria y acumulada. La app elimina la incertidumbre sobre las penalizaciones por exceso de kilómetros mediante un cálculo dinámico de "saldo".

## 2. Architecture
El proyecto sigue una arquitectura de **Clean Architecture** dentro de un único módulo (`:app`). Se prioriza la legibilidad, la testabilidad y el mantenimiento siguiendo los principios SOLID.

### Layers:
- **`data/`**: Implementaciones de repositorios, fuentes de datos (Room/DataStore) y mappers.
- **`domain/`**: Contiene la lógica de negocio pura.
    - `model/`: Entidades de negocio y Value Objects.
    - `usecase/`: Casos de uso que encapsulan la lógica de negocio ejecutable (ej. `GetCurrentBalanceUseCase`).
    - `repository/`: Definiciones de contratos de datos (interfaces).
- **`ui/`**: Implementación de la interfaz de usuario con Jetpack Compose.
    - Sigue el patrón **MVI (Model-View-Intent)** para una gestión de estado predecible (`State`, `Event`, `Effect`).
    - Organizado por features (ej. `dashboard/`, `contract/`).
- **`di/`**: Módulos Hilt para la inyección de dependencias.
- **`designsystem/`**: Componentes de UI reutilizables y tokens de diseño (colores, tipografía).

## 3. Business Logic (El Algoritmo SafeKm)
El núcleo de la aplicación debe implementar estrictamente estos cálculos en la capa `Domain`:

1.  **Presupuesto Diario Base (PDB):** `Km Totales / Días Totales del Contrato`.
2.  **Kilómetros Teóricos (KT):** `Días Transcurridos * PDB`.
3.  **Saldo Actualizado (SA):** `KT - Kilómetros Reales`. 
    *   *Positivo:* Ahorro (Verde). *Negativo:* Exceso (Rojo).

## 4. Tech Stack
- **Language**: Kotlin 2.0+ (K2 Compiler ready).
- **Build System**: Gradle Kotlin DSL con Version Catalogs.
- **UI Framework**: Jetpack Compose con Material 3.
- **Dependency Injection**: Hilt (Dagger).
- **Asynchrony & Reactivity**: Kotlin Coroutines y Flow.
- **Architecture**: Pure MVI (Model-View-Intent).
- **Foundation & Infrastructure (Pluginkit)**:
    - **FoundationKit**: Abstracciones de `UseCase`, `DispatcherProvider` y utilidades core.
    - **Pluginkit Navigation**: Navegación de Compose type-safe.
    - **Pluginkit Quality**: Análisis estático y estándares de código (Linting).
- **Persistence**: Room para el histórico de lecturas y DataStore para preferencias rápidas.

## 5. Design Patterns and SOLID
- **SRP (Single Responsibility)**: Cada clase tiene una única responsabilidad.
- **DIP (Dependency Inversion)**: Las capas de UI y Data dependen de abstracciones definidas en Domain.
- **Repository Pattern**: Desacoplamiento de las fuentes de datos.
- **Stateless UI**: Separación clara entre `Root` composables (con ViewModel) y `Screen` composables (representación pura).

## 6. Best Practices
- **KDoc**: Documentación obligatoria en componentes críticos y lógica del algoritmo.
- **Immutability**: Uso de `data class` inmutables para UI State y modelos de dominio.
- **Visual Feedback**: Uso de colores semánticos (Verde/Rojo) para comunicar el estado del saldo de un vistazo.
- **Testing**: Tests unitarios rigurosos para el algoritmo de cálculo en la capa de dominio.
