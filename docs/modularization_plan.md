# Plan Maestro de Modularización: KmSafe

Este documento detalla la hoja de ruta técnica para transformar el monolito actual en una arquitectura modular, escalable y testable, aplicando Clean Architecture y DDD.

---

## Fase 1: Estandarización y Version Catalog (En progreso)
*   **Objetivo**: Centralizar la gestión de dependencias para evitar discrepancias entre futuros módulos.
*   **Acciones**:
    *   Implementar un catálogo de versiones local (`gradle/libs.versions.toml`) que complemente al catálogo remoto de los Kits.
    *   Migrar todas las dependencias del archivo `app/build.gradle.kts` a referencias del catálogo.
    *   Preparar los `Convention Plugins` (opcional pero recomendado) para compartir configuraciones de compilación.

---

## Fase 2: Extracción de Infraestructura (Core)
*   **Objetivo**: Aislar las dependencias de bajo nivel del resto de la lógica.
*   **Módulos a crear**:
    *   `:core:foundation`: Logger, Dispatchers, Extensiones de Kotlin.
    *   `:core:design-system`: Temas de CanvasKit, tipografía y componentes atómicos compartidos.
    *   `:core:database`: Definición de Room, DAOs y entidades de base de datos.
    *   `:core:network`: Cliente Retrofit, interceptores y configuración de API.

---

## Fase 3: Vertical Slicing (Domain & Data)
*   **Objetivo**: Aplicar Clean Architecture separando reglas de negocio de implementaciones de datos por contexto.
*   **Módulos a crear**:
    *   `:domain:auth` & `:data:auth`: Gestión de tokens, login y sesión.
    *   `:domain:renting` & `:data:renting`: Gestión de vehículos y contratos.
    *   `:domain:tracking`: Lógica de GPS y cálculos de trayecto.
    *   `:domain:expenses`: Lógica de combustible y estaciones de servicio.

---

## Fase 4: Módulos de Funcionalidad (Features)
*   **Objetivo**: Aislar la UI y los ViewModels para permitir compilación paralela y pruebas aisladas.
*   **Módulos a crear**:
    *   `:feature:auth`: Pantallas de Login y Registro.
    *   `:feature:onboarding`: Setup Wizard.
    *   `:feature:dashboard`: Contenedor del Dashboard y navegación interna.
    *   `:feature:history`: Historial y detalle de registros.

---

## Guía de Dependencias entre Módulos
1.  **Feature** depende de **Domain** (para UseCases) y de **Core** (para UI).
2.  **Data** depende de **Domain** (para implementar Repositorios).
3.  **App (Shell)** depende de todas las **Features** para orquestar la navegación global.
4.  **Domain** NO DEPENDE de nadie (Pureza absoluta).
