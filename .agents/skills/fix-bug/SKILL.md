---
name: fix-bug
description: >-
  Protocolo de investigaciÃ³n quirÃºggica, triaje de causa raÃ­z y correcciÃ³n anti-regresiÃ³n para KmSafe. Analiza stacktraces, Logcat o comportamientos errÃ³neos, crea un test unitario reproductor, aplica la soluciÃ³n mÃ­nima necesaria y verifica la sanidad sin parches cosmÃ©ticos.
---

# Fix Bug â DiagnÃ³stico QuirÃºrgico & Anti-RegresiÃ³n (KmSafe)

Este comando ejecuta una *investigaciÃ³n sistemÃ¡tica de causa raÃªz* para diagnosticar y erradicar defectos, evitando parches cosmÃ©ticos o supresiones silenciosas de excepciones.

Se apoya en `senior-debugging-engineer`, `android-quality` (`logcat_triage.py`) y `performance-optimization-tips`.

---

## CuÃ¡ndo usar este comando
- Cuando ocurra un crash, ANR o excepciÃ³n inesperada en KmSafe.
- Cuando un cÃ¡lculo de mÃ©tricas (odÃ³metro, balance, gasto de combustible/EV) de un resultado incorrecto.
- Cuando un Composable tenga parpadeos, recomposiciones infinitas o fugas de estado en Navigation 3.

---

## Protocolo de EjecuciÃ³n (Paso a Paso)

Cuando el usuario invoque `/fix-bug [descripciÃ³n, logs o stacktrace]`:

### Fase 1: Triaje de Causa RaÃ­z (Sintoma vs. Causa Real)
1. *Identificar la Invariante Rota*: Â¿QuÃ© contrato de `AGENTS.md` o modelo de dominio se ha violado?
2. *Rastreo de EjecuciÃ³n*:
   - Â¿Se ejecutÃ³ una operaciÃ³n de E/S o cÃ¡lculo pesado en el hilo principal (`Main`)?
   - Â¿Falta main-safety con `dispatchers.io` en el repositorio?
   - Â¿OcurriÃ³ una fuga de instancia en Navigation 3 por omitir la `key` en `hiltViewModel`?
   - Â¿Se intentÃ³ iniciar un Foreground Service fuera del ciclo sÃ­ncrono de `onReceive()`;
3. *Aislamiento*: Distinguir el detonante (ej. un null o un ID vacÃ­o) del fallo arquitectÃ³nico de fondo.

### Fase 2: Escribir el Test de RegresiÃ³n (Red Phase)
1. Antes de modificar el cÃ³digo de producciÃ³n, redacta un test unitario en el mÃ³dulo correspondiente (`ProfileViewModelTest`, `CalculateMetricsUseCaseTest`, etc.).
2. El test debe reproducir exactamente el escenario anÃ³malo y *fallar* (`RED`).

### Fase 3: CorrecciÃ³n QuirÃºggica (Green Phase)
1. Modifica el cÃ³digo aplicando la soluciÃ³n mÃ¡s limpia y robusta posible.
2. Respeta las invariantes: 
   - Prohibido suprimir errores con bloques `try/catch` vacÃ­os.
   - Prohibido aÃ±adir estados mutables dentro de Repositorios (deben permanecer stateless).
   - Prohibido hacer cÃ¡lculos de negocio dentro de ViewModels.

### Fase 4: VerificaciÃ³n & Sanity Check
1. Ejecuta el test de regresiÃ³n para confirmar que ahora pasa en verde (`GREEN`).
2. Verifica que las pruebas preexistentes del mÍÑÕ±¼¹¼Í¡å¸É½Ñ¼¸(Ì¸%¹½Éµ°ÕÍÕÉ¥¼è(´©
ÕÍIµè¥¹½ÍÑ¥¨èE×¤±±É±µ¹ÑäÁ½ÈÅ×¤¸(´©M½±Õ§Í¸Á±¥¨èE×¤µ¥½ÌÍÉ±¥éÉ½¸¸(´©QÍÐIÉÍ§Í¸%¹½ÉÁ½É¼¨è9½µÉäÉÕÑ°ÑÍÐÅÕÁÉ½ÑË°Í¥¼¸°ÕÑÕÉ¼¸(