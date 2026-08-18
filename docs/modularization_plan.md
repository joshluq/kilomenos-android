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

## Fase 2: Módulo de Infraestructura Centralizada (Core)
*   **Objetivo**: Extraer toda la "fontanería" de la aplicación del módulo `:app` para dejarlo como un cascarón vacío de orquestación.
*   **Módulo a crear**:
    *   `:core:infrastructure`: Un único módulo que contendrá:
        *   **Local Data**: `AppDatabase`, DAOs y Entidades de Room.
        *   **Remote Data**: Cliente Retrofit, `ApiService` e Interceptores.
        *   **Common Utils**: Mappers globales, extensiones de contexto y lógica de seguridad (Tink).
*   **Ventaja**: Acelera la migración inicial y simplifica la gestión de dependencias en esta etapa crítica.

---

## Fase 3: Vertical Slicing (Domain & Data)
*   **Objetivo**: Una vez estabilizada la infraestructura, separaremos la lógica por contextos. Aquí es donde evaluaremos si `:core:infrastructure` debe dividirse en `:core:database` y `:core:network` si el tamaño lo justifica.

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
