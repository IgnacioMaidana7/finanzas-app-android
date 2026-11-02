# TP3 v2 - Escalabilidad de FinanzasApp sobre TP7

**Objetivo:** completar el Trabajo Práctico 3 respetando sus tres entregables y utilizando la versión TP7 de la aplicación Android como nueva base.

| Entregable | Archivo |
|---|---|
| 1. Estrategia de escalabilidad | [01-estrategia.md](01-estrategia.md) |
| 2. Configuración cloud y evidencias | [02-configuracion-y-evidencias.md](02-configuracion-y-evidencias.md) |
| 3. Arquitectura actualizada | [03-arquitectura.md](03-arquitectura.md) / [Mermaid](arquitectura.mmd) |

**Documentación adicional:** [Informe integrado](05-informe-integrado.md), [Procedimiento Azure](04-procedimiento-azure.md), [Plan de pruebas](plan-pruebas.md).

**Estado real (02/11/2026):** backend TP3 recuperado, tarjeta del resumen en Android TP7 e infraestructura Terraform actualizada. Pruebas de backend y Terraform correctas en CI; infraestructura Azure del TP3 anterior retirada. La carga cloud v2 y las capturas del portal no están realizadas. No confundir el gráfico histórico 1→2→0 con una nueva medición.

**CI verificada:** [ejecución 41540892011](https://github.com/IgnacioMaidana7/finanzas-app-android/actions/runs/41540892011): 19 tests backend, 1 test Firebase emulado, 2 tests Terraform, Android BUILD SUCCESSFUL y 400 llamadas sintéticas locales sin errores. Estas pruebas no miden la nube.

**Seguridad y costos:** el script de carga cloud usa solo cuentas y datos ficticios, con validación de autenticación, aislamiento y limpieza; no cargar DolarAPI/ArgentinaDatos. La creación de Azure y la subida del contenedor pueden generar costos y requieren revisar crédito, cuotas y autorización. La prueba debe detenerse ante cuotas o tasas de errores anormales.
