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

El script `backend/scripts/verify-cloud.mjs` conserva las verificaciones de autorización y limpieza de usuarios sintéticos y admite `TP3_LOAD_PROFILE`. Perfiles mayores que baseline requieren `TP3_APPROVE_SYNTHETIC_CLOUD=1` y `TP3_APPROVE_HIGH_LOAD=1`; esos indicadores **no sustituyen la aprobación humana de crédito, cuota, región, servicio y permisos antes del despliegue**. En condiciones de errores sostenidos el ensayo se corta anticipadamente. El script registra evidencias en el JSON existente; guardar una copia identificada por perfil luego de cada prueba antes de ejecutar la siguiente.

**Nota**: a mayor concurrencia puede aumentar el costo de lecturas Firestore, verificaciones Firebase y CPU. La infraestructura tiene `max_replicas=2` por defecto como límite seguro; para evaluar 3–6 réplicas deben comprobarse cuotas, costos y aprobación explícita. El umbral HTTP 16 es una referencia para la decisión de escalado, **no un tope estricto de 16 solicitudes por instancia**.

## Métricas y comparación

Registrar por perfil: requests programadas/realizadas, RPS reales, latencia p50/p95/p99, errores HTTP por categoría, solicitudes en vuelo observadas, CPU/memoria, réplicas asignadas/listas (serie temporal real), lecturas Firestore y costo. Tomar muestras antes, durante y después de carga. Comparar al menos configuración de 2 réplicas vs configuración ampliada (sólo si la cuenta permite), sin cambiar otras variables entre comparaciones.

Además ejecutar pruebas de regresión de Firebase, DolarAPI, ArgentinaDatos, caché y ML Kit a través de mocks/dispositivo: no afirmar que una carga de API demuestra rendimiento de toda la app Android. Documentar pruebas manuales pendientes de OCR y emulador; no declararlas aprobadas sin ejecutarlas.

## Estado

- Código histórico TP3 recuperado sobre base TP7; no se ha vuelto a desplegar.
- No se han ejecutado nuevas pruebas cloud; ningún resultado de carga de este documento es medido.
- Falta incorporar la tarjeta de resumen mensual a la UI del TP7 conservando las nuevas integraciones, ejecutar tests de integración y actualizar el diagrama e informe final con evidencias reales.
