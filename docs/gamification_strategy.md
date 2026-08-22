# Estrategia de Gamificación: Eco-Pilot System (KmSafe)

Este documento detalla el sistema de refuerzo de hábitos positivos diseñado para KmSafe, con el objetivo de aumentar la retención (stickiness), mejorar la calidad de los datos y fomentar la conversión a Premium.

---

## 1. El Concepto: Eco-Pilot XP
KmSafe deja de ser una calculadora pasiva para convertirse en un asistente de conducción proactivo. El usuario gana "XP de Conducción" (Eco-Points) por cada acción que aporte valor a la precisión de su presupuesto de renting.

### 💎 Economía de XP (Eco-Points)
| Acción | Recompensa | Objetivo de Negocio |
| :--- | :--- | :--- |
| **Registro Manual** | +10 XP | Mantener la base de datos actualizada. |
| **Uso de Auto-tracking** | +50 XP | Fomentar la precisión del GPS y uso de feature Premium. |
| **Día en "Zona Verde"** | +20 XP | Reforzar el cumplimiento del contrato diario. |
| **Registro de Gasto** | +40 XP | Completar perfil de ahorro y datos de electrificación. |
| **Semana Consistente** | +100 XP | Fomentar el uso recurrente de la App (Retención). |
| **Categorización** | +30 XP | Mejorar la calidad de los datos para la IA. |

---

## 2. Dinámicas de Progresión

### Rangos de Conductor
El XP acumulado desbloquea rangos que se muestran en el Perfil y Dashboard:
1. **Novato (Lvl 1-3)**: Visualización básica.
2. **Eco-Driver (Lvl 4-7)**: Desbloquea iconos personalizados para el vehículo.
3. **Master de Renting (Lvl 8-10)**: Reconocimiento visual "Top Tier".

### Rachas de Ahorro (Saving Streaks)
- Un contador de "Fuego" en el Dashboard.
- Se incrementa cada día que el usuario termina con un **Balance Positivo**.
- **Bonus**: Multiplicador de XP (x1.2, x1.5) si la racha supera los 7 días.

---

## 3. Mecánica de Oposición: "Mr. Penalty"
En lugar de mostrar solo números rojos, personificamos el riesgo financiero de exceder los Km.

- **Aparición**: Solo si `TripProjection.isOverLimit` es `true`.
- **Evolución**:
    - **Sombra (Exceso leve)**: Aparece un aviso sutil.
    - **Monstruo (Exceso crítico)**: Un avatar rojo que "se come" el saldo visual de la tarjeta principal.
- **Victoria**: El enemigo solo desaparece si el usuario ajusta su ritmo de conducción en el **IA Simulator** hasta volver a un balance final proyectado positivo.

---

## 4. Recompensas de Gasto: "The Armory" 🛡️⚔️
Para incentivar el uso de la funcionalidad de **Fuel Expenses**, hemos introducido el concepto de equipamiento defensivo.

### 🛡️ Escudo de Gasto (Armor)
El registro recurrente de gastos de combustible otorga al usuario el "Escudo Financiero".
- **Efecto**: Bloquea visualmente los ataques de "Mr. Penalty" cuando el exceso es menor al 2%.
- **Niveles**:
    - **Armadura de Cuero**: 3 gastos registrados (Protección base).
    - **Coraza de Carbono**: 10 gastos registrados (Añade aura de éxito en el Dashboard).

### ⚡ Espada de Electrificación (Weapon)
Especial para usuarios que registran cargas eléctricas (EV).
- **Efecto**: "Corta" la incertidumbre financiera mostrando con mayor énfasis el KPI de ahorros acumulados.
- **Bonus**: Desbloquea un rayo visual sobre el icono del vehículo cuando está cargando.

---

## 5. Puntos de Mejora y Refinamiento (V2)
- **Desafíos Temporales**: *"Este mes recorre menos de 500km y gana el emblema 'Eco-City'"*.
- **Integración con Partners**: Convertir el XP en descuentos reales para cambio de neumáticos o revisiones en talleres asociados.
- **Leaderboard Anónimo**: Comparar tu nivel de eficiencia con la media de conductores del mismo modelo de vehículo.
- **Personalización**: Canjear Eco-Points por skins o temas visuales para la App (Modo Noche exclusivo, colores de marca).

---

## 6. Viabilidad Técnica y Riesgos

### 🛠️ Viabilidad
- **Arquitectura**: Se puede implementar extendiendo el `UserSessionModel` para incluir `totalXp` y `level`.
- **Cálculo**: La lógica de XP debe residir en la capa de **Domain (UseCases)** para asegurar que se calcule igual independientemente de la pantalla.
- **Offline**: Room puede cachear el XP ganado y sincronizarlo mediante `SyncWorker` cuando haya red.

### ⚠️ Riesgos
1. **Fraude (Gaming the system)**: Usuarios introduciendo registros falsos pequeños para farmear XP.
    *   *Mitigación*: Capar el XP diario máximo ganable por registros manuales.
2. **Complejidad de UI**: Sobrecargar el Dashboard con demasiados elementos lúdicos.
    *   *Mitigación*: Usar micro-interacciones sutiles (Lottie animations) en lugar de grandes pop-ups.
3. **Pérdida de Datos**: Si el XP no se persiste en el Backend, el usuario se frustrará al reinstalar.
    *   *Mitigación*: Incluir el XP en el flujo de sincronización atómica de `AuthRepository`.
4. **Frustración por "Mr. Penalty"**: Ver un monstruo rojo puede ser estresante para algunos usuarios.
    *   *Mitigación*: Permitir desactivar la "visualización lúdica de riesgos" en Preferencias.
