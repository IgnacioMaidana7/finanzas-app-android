# TP3 renovado sobre el TP7 — plan de implementación y pruebas

**Base**: rama `tp7/integraciones-apis-sdk` de FinanzasApp (Kotlin, Firebase Auth, Firestore, DolarAPI, ArgentinaDatos y ML Kit OCR).

## Alcance

Se mantiene el objetivo y los tres entregables del TP3: estrategia de escalabilidad, infraestructura cloud real y diagrama actualizado. El TP7 es la nueva base funcional, no un cambio de consigna. Se recuperó el backend Node.js autenticado del TP3 anterior como código de referencia y la infraestructura Azure declarativa; **esto no significa que Azure esté desplegado ahora**.

DolarAPI y ArgentinaDatos se consultan por el móvil con caché y límites del TP7. **No someter servicios públicos de terceros a pruebas intensivas.** ML Kit funciona localmente y no se prueba con concurrencia cloud. La carga autorizada se concentra exclusivamente en el backend propio de resumen mensual, con cuentas y documentos sintéticos bajo control.

## Plan experimental propuesto (no son resultados)

| Perfil | Total de solicitudes programadas | Máximo configurado de operaciones en curso | Ritmo programado |
| --- | ---: | ---: | ---: |
| baseline | 1.000 | 32 | 25/s |
| media | 5.000 | 64 | 60/s |
| alta | 12.000 | 128 | 100/s |
| pico | 20.000 | 256 | 160/s |

Las operaciones en curso **observadas** pueden ser menores que el máximo del generador. Separar métricas de concurrencia, RPS programadas y RPS efectivas. No equiparar 20.000 solicitudes totales con 20.000 solicitudes simultáneas.

**Tamaño del conjunto de carga:** el usuario confirmó Firebase Spark. La validación funcional comprueba diez movimientos; la carga usa uno por consulta para que los cuatro perfiles sumen unas 38.000 lecturas más controles, sujetas al consumo previo del proyecto y a la cuota compartida de 50.000 lecturas/día. No mide capacidad de resúmenes de diez o mil movimientos. Se conserva el mismo tamaño en los cuatro perfiles.

El script `backend/scripts/verify-cloud.mjs` conserva las verificaciones de autorización y limpieza de usuarios sintéticos y admite `TP3_LOAD_PROFILE`. Perfiles mayores que baseline requieren `TP3_APPROVE_SYNTHETIC_CLOUD=1` y `TP3_APPROVE_HIGH_LOAD=1`; esos indicadores **no sustituyen la aprobación humana de crédito, cuota, región, servicio y permisos antes del despliegue**. En condiciones de errores sostenidos el ensayo se corta anticipadamente. El script registra evidencias en el JSON existente; guardar una copia identificada por perfil luego de cada prueba antes de ejecutar la siguiente.

**Nota**: a mayor concurrencia puede aumentar el costo de lecturas Firestore, verificaciones Firebase y CPU. La infraestructura tiene `max_replicas=2` por defecto como límite seguro; para evaluar 3–6 réplicas deben comprobarse cuotas, costos y aprobación explícita. El umbral HTTP 16 es una referencia para la decisión de escalado, **no un tope estricto de 16 solicitudes por instancia**.

## Métricas y comparación

Registrar por perfil: requests programadas/realizadas, RPS reales, latencia p50/p95/p99, errores HTTP por categoría, solicitudes en vuelo observadas, CPU/memoria, réplicas asignadas/listas (serie temporal real), lecturas Firestore y costo. Tomar muestras antes, durante y después de carga. Comparar al menos configuración de 2 réplicas vs configuración ampliada (sólo si la cuenta permite), sin cambiar otras variables entre comparaciones.

Además ejecutar pruebas de regresión de Firebase, DolarAPI, ArgentinaDatos, caché y ML Kit a través de mocks/dispositivo: no afirmar que una carga de API demuestra rendimiento de toda la app Android. Documentar pruebas manuales pendientes de OCR y emulador; no declararlas aprobadas sin ejecutarlas.

## Estado

- Código TP3 recuperado sobre TP7; despliegue Azure v2 y perfil inicial ejecutados el 03/11/2026.
- Baseline medido: 1.000 consultas sin errores, 24,56 req/s, p95 783 ms; ciclo real 0 -> 1 -> 2 -> 0. Los perfiles media/alta/pico siguen como plan no ejecutado y requieren autorización adicional.
- La tarjeta de resumen mensual ya se recuperó y las comprobaciones automáticas existentes pasaron. Faltan la prueba real en dispositivo, la integración con Azure v2 y el diagrama e informe final con evidencias reales.
