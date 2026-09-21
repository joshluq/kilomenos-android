# Atlassian Cloud Integration (Jira & Confluence) - KmSafe

Guía de integración y referencia completa de comandos para conectar **KmSafe** con **Atlassian Jira Software** (tableros Kanban/Scrum, tickets, State Guards, reporte automatizado de bugs) y **Atlassian Confluence** (sincronización de Living Architecture Specs).

---

## 1. Configuración Rápida y Credenciales

La configuración se gestiona mediante el archivo local **`.atlassian_config.json`** en la raíz de `KmSafe`. Este archivo está incluido en `.gitignore` para proteger tus claves y credenciales privadas.

### 1.1 Cómo Obtener tu Atlassian API Token
1. Accede a: [Atlassian API Tokens Management](https://id.atlassian.com/manage-profile/security/api-tokens).
2. Haz clic en **Crear token de API** (`Create API token`).
3. Asigna una etiqueta (ej. `kmsafe-agentic-token`).
4. Copia el token generado y pégalo en el campo `"api_token"` de `.atlassian_config.json`.

### 1.2 Archivo de Configuración Local (`.atlassian_config.json`)
El archivo ya está creado en la raíz de tu proyecto con tus datos preconfigurados:
```json
{
  "url": "https://joshluq-dev.atlassian.net",
  "email": "josh.luque@gmail.com",
  "api_token": "PEGA_AQUI_TU_API_TOKEN",
  "project_key": "KILOMENOS",
  "board_id": "1",
  "confluence_space": "KILOMENOS"
}
```

---

## 2. Referencia Completa de Comandos (`scripts/atlassian_bridge.py`)

Todos los comandos admiten el flag `--simulate` para realizar pruebas en seco (mock) sin realizar peticiones de red reales.

### 2.1 Diagnóstico de Conexión (`check`)
Verifica la autenticación, comprueba el estado del usuario y confirma la conectividad con la API REST v3 de Jira y Confluence.
```bash
# Verificación en vivo (tras pegar el token)
python scripts/atlassian_bridge.py check

# Modo simulación (offline)
python scripts/atlassian_bridge.py --simulate check
```

---

### 2.2 Consulta del Tablero Kanban (`board`)
Consulta los tickets activos en tu tablero Kanban de KmSafe (Board ID: 1) con soporte de filtros por columna/estado.
```bash
# Listar todos los tickets del tablero configurado (Board 1)
python scripts/atlassian_bridge.py board

# Filtrar tickets en la columna lista para desarrollo ("Ready for Dev")
python scripts/atlassian_bridge.py board --status "Ready for Dev"

# Filtrar por otra columna (ej. "In Progress", "Backlog")
python scripts/atlassian_bridge.py board --status "In Progress"

# Modo simulación
python scripts/atlassian_bridge.py --simulate board --status "Ready for Dev"
```

---

### 2.3 Obtener Ticket y Andamiaje OpenSpec (`fetch`)
Descarga los detalles de un ticket de Jira (resumen, descripción, criterios de aceptación, prioridad). Permite aplicar **State Guards** y andamiar automáticamente una propuesta OpenSpec.
```bash
# Descarga básica
python scripts/atlassian_bridge.py fetch KILOMENOS-101

# Descarga con State Guard (Bloquea si el ticket no está en estado "Ready for Dev")
python scripts/atlassian_bridge.py fetch KILOMENOS-101 --require-status "Ready for Dev"

# Descarga, valida estado y crea la propuesta OpenSpec (/openspec) automáticamente:
python scripts/atlassian_bridge.py fetch KILOMENOS-101 --require-status "Ready for Dev" --scaffold-openspec

# Modo simulación
python scripts/atlassian_bridge.py --simulate fetch KILOMENOS-101 --require-status "Ready for Dev"
```

---

### 2.4 Refinamiento por el Agente PO (`refine`)
Permite al **agente Product Owner** enriquecer un ticket preliminar con criterios de aceptación formales (formato Gherkin/OpenSpec) y avanzar su estado.
```bash
# Refinar usando un archivo de especificación markdown existente
python scripts/atlassian_bridge.py refine KILOMENOS-101 openspec/changes/FEAT-001/proposal.md --status "Ready for Dev"

# Refinar directamente con texto desde la terminal
python scripts/atlassian_bridge.py refine KILOMENOS-101 "Acceptance Criteria:\n- AC-1: Given user has vehicles, When list is rendered, Then show cards." --status "Ready for Dev"

# Modo simulación
python scripts/atlassian_bridge.py --simulate refine KILOMENOS-101 "Criterios refinados" --status "Ready for Dev"
```

---

### 2.5 Transición de Estados en Jira (`transition`)
Mueve un ticket a través de las columnas del flujo de trabajo de Jira.
```bash
# Mover ticket a En Progreso
python scripts/atlassian_bridge.py transition KILOMENOS-101 "In Progress"

# Mover ticket a Revisión / QA
python scripts/atlassian_bridge.py transition KILOMENOS-101 "Code Review"

# Mover ticket a Finalizado / Done
python scripts/atlassian_bridge.py transition KILOMENOS-101 "Done"

# Modo simulación
python scripts/atlassian_bridge.py --simulate transition KILOMENOS-101 "Done"
```

---

### 2.6 Comentarios y Feedback Humano (`comment`)
Publica preguntas de clarificación, decisiones arquitectónicas o actualizaciones de estado directamente en el hilo del ticket para feedback interactivo.
```bash
# Solicitar clarificación humana (HITL)
python scripts/atlassian_bridge.py comment KILOMENOS-101 "Architect Agent: ¿El módulo de exportación a PDF debe incluir desglose de IVA o solo total neto?"

# Publicar reporte de implementación
python scripts/atlassian_bridge.py comment KILOMENOS-101 "Staff Engineer: Implementación completada bajo MVI Contract. 100% tests unitarios pasando."

# Modo simulación
python scripts/atlassian_bridge.py --simulate comment KILOMENOS-101 "Comentario de prueba simulado"
```

---

### 2.7 Reporte Automatizado de Bugs (`report-crash`)
Crea automáticamente un ticket de tipo **Bug** con alta prioridad en Jira a partir de un JSON de defecto generado por QA o el Senior Debugging Engineer.
```bash
# Crear Bug en Jira desde defecto de prueba
python scripts/atlassian_bridge.py report-crash KILOMENOS walkthrough/artifacts/DEFECT-001.json

# Modo simulación
python scripts/atlassian_bridge.py --simulate report-crash KILOMENOS walkthrough/artifacts/DEFECT-001.json
```

---

### 2.8 Sincronización de Living Specs con Confluence (`sync-confluence`)
Publica todas las especificaciones vivas desde `openspec/specs/` directamente en el Espacio de Confluence (`KILOMENOS`) como páginas de documentación permanente.
```bash
# Publicar especificaciones al espacio configurado (KILOMENOS)
python scripts/atlassian_bridge.py sync-confluence

# Modo simulación
python scripts/atlassian_bridge.py --simulate sync-confluence
```

---

## 3. Flujo Integrado de Desarrollo

```
1. Consultar Kanban:
   python scripts/atlassian_bridge.py board --status "Ready for Dev"

2. Iniciar Desarrollo con OpenSpec:
   python scripts/atlassian_bridge.py fetch KILOMENOS-101 --require-status "Ready for Dev" --scaffold-openspec
   python scripts/atlassian_bridge.py transition KILOMENOS-101 "In Progress"

3. Si hay dudas arquitectónicas:
   python scripts/atlassian_bridge.py comment KILOMENOS-101 "¿Pregunta de diseño?"

4. Al finalizar implementación y QA (100% tests OK):
   python scripts/atlassian_bridge.py transition KILOMENOS-101 "Done"
   python scripts/atlassian_bridge.py sync-confluence
```
