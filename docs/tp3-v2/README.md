# Trabajo Práctico 3 - Escalabilidad de FinanzasApp

Arquitecturas Móviles. Ignacio Maidana y José Santino Arroyo. Docente: Juan Pablo Bono. Fecha: 03/11/2026.

FinanzasApp registra ingresos y gastos, genera resúmenes mensuales y ofrece indicadores y OCR de tickets. Este trabajo explica la estrategia de escalabilidad y su evaluación mediante un backend propio desplegado en Azure.

| Entregable | Documento |
|---|---|
| Estrategia de escalabilidad | [Balanceo, redundancia y autoescalado](01-estrategia.md) |
| Configuración cloud con evidencias | [Infraestructura y mediciones](02-configuracion-y-evidencias.md) |
| Diagrama de arquitectura | [Iconografía oficial Azure](03-arquitectura.md), [PNG](arquitectura-azure.png), [SVG editable](arquitectura-azure.svg) |

[Informe integrado](05-informe-integrado.md) · [Resultados y trazabilidad](07-resultados-cloud.md) · [Procedimiento](04-procedimiento-azure.md) · [Metodología de pruebas](plan-pruebas.md)

Se completaron 38.000 solicitudes sin errores; máximo observado 2 réplicas. El perfil pico alcanzó 127,64/s frente a 160/s programadas. La infraestructura se retiró al finalizar las evidencias. El costo final, la telemetría completa de plataforma y la prueba manual Android/OCR no están acreditados. Los PDF y las capturas de cuenta se entregan localmente; los JSON técnicos identifican los ensayos originales.
