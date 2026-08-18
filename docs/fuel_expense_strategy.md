# Estrategia: Gestión Inteligente de Gastos y Estaciones (My Stations)

## 1. Visión General
KmSafe evoluciona para ser un gestor financiero del vehículo. El usuario no solo registra cuánto gasta, sino que es dueño de su propia base de datos de estaciones de servicio, permitiéndole optimizar sus repostajes mediante análisis histórico y proactivo.

---

## 2. Componentes del Ecosistema

### 2.1 Entidad: Estación de Servicio (ServiceStation)
Cada gasto se vincula a una estación física o punto de carga.
- **Detección Automática**: Integración con Google Places API para identificar la estación cuando el vehículo se detiene más de 5 minutos en coordenadas de tipo `gas_station`.
- **Favoritos**: El usuario puede marcar sus estaciones preferidas para recibir alertas personalizadas.
- **Contexto**: Almacenamiento de Marca (Repsol, Tesla Supercharger, Iberdrola, etc.), Ubicación exacta y tipo de energía (Combustible fósil / Eléctrico).

### 2.2 Histograma de Volatilidad
Visualización interactiva dentro de la ficha de detalle de cada estación.
- **Análisis Temporal**: Gráfico que muestra la evolución del precio por litro o kWh a lo largo de las semanas/meses.
- **Insight de Ahorro**: KmSafe indica si el precio del día está por encima o por debajo de la media histórica del usuario en esa estación específica.

### 2.3 Modo Carga Mixta (PHEV/EV Ready)
- **Selector de Energía**: Formulario dinámico que cambia según el tipo de vehículo activo.
- **Carga Eléctrica**: Registro de kWh consumidos, tiempo de conexión y coste total.
- **KPI Diferencial**: Visualización del "Ahorro por Electrificación" (Comparativa de coste vs. trayectos equivalentes en gasolina).

---

## 3. Puntos de Mejora y Refinamiento (V2)
- **Geofencing Proactivo**: Notificación push automática al entrar en el radio de una estación favorita: *"Estás en tu gasolinera habitual. El último precio que registraste aquí fue 1.55€/L. ¿Ha cambiado?"*.
- **Reporte Rápido (Waze-Style)**: Permitir que el usuario actualice solo el precio al pasar por delante, sin necesidad de realizar un repostaje completo, para mantener el histograma vivo.
- **Predicción de Precios**: Algoritmo que sugiere el mejor día de la semana para llenar el depósito basado en la tendencia de precios registrada por el usuario.

---

## 4. Viabilidad Técnica y Riesgos

### 🛠️ Viabilidad
- **Arquitectura de Datos**: Room soporta de forma nativa la relación 1:N entre la nueva tabla `service_stations` y la tabla de `expenses`.
- **Geolocalización**: Utilización de `ActivityRecognition` para detectar paradas prolongadas, minimizando el impacto en la batería al no mantener el GPS activo constantemente.
- **Modularidad**: El sistema se puede implementar por fases, empezando por el registro manual y escalando hacia la detección automática.

### ⚠️ Riesgos
- **Costos de Google Places API**: Consultas recurrentes pueden elevar el coste de mantenimiento.
    - *Mitigación*: Implementar un sistema de caché local agresivo. Solo se consulta a Google si la estación no existe previamente en la base de datos local del usuario.
- **Precisión en Zonas Urbanas**: El GPS puede dar falsos positivos en calles con mucha densidad de comercios.
    - *Mitigación*: Nunca crear una estación automáticamente. El sistema debe lanzar una sugerencia y esperar la confirmación del usuario ("¿Has repostado en [Nombre]?").
- **Fragmentación de Datos**: Usuarios con flotas mixtas pueden confundir unidades (litros vs kWh).
    - *Mitigación*: Validaciones en tiempo real en los campos de entrada y estilos visuales diferenciados (Azul para eléctrico, Naranja para combustible).
