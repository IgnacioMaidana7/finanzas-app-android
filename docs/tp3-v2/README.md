# TP3 v2 - Escalabilidad de FinanzasApp sobre TP7

**03/11/2026: Recursos retirados y verificados.** Perfiles ejecutados sin errores: baseline, media, alta, pico. Solicitudes de carga completadas: **38000**, errores: **0**.

| Entregable | Evidencia |
|---|---|
| 1. Estrategia | [01-estrategia.md](01-estrategia.md) |
| 2. Configuración cloud | [02-configuracion-y-evidencias.md](02-configuracion-y-evidencias.md), [resultados originales](07-resultados-cloud.md) |
| 3. Arquitectura | [03-arquitectura.md](03-arquitectura.md), [Mermaid editable](arquitectura.mmd) |

| Perfil | Completadas / meta | RPS programado / real | Máximo en curso configurado / real | p50 / p95 / p99 ms | Errores |
|---|---:|---:|---:|---:|---:|
| baseline | 1000 / 1000 | 25 / 24.56 | 32 / 24 | 749 / 783 / 887 | 0 |
| media | 5000 / 5000 | 60 / 59.48 | 64 / 64 | 750 / 1115 / 1949 | 0 |
| alta | 12000 / 12000 | 100 / 99.39 | 128 / 128 | 816 / 2024 / 3347 | 0 |
| pico | 20000 / 20000 | 160 / 127.64 | 256 / 256 | 1604 / 4853 / 6796 | 0 |

Se mantuvo máximo 2 réplicas, imagen inmutable, HTTPS y Firebase Spark. La carga usa un movimiento ficticio por resumen; los controles funcionales usan diez. DolarAPI/ArgentinaDatos no recibieron carga masiva.

CPU y memoria de Azure Monitor siguen sin valores verificables. El costo final contabilizado aun no esta disponible. La prueba manual Android no esta acreditada: no habia un dispositivo conectado. La carga representa un movimiento por resumen. No se valido redundancia zonal/regional ni se aumento el maximo a seis. La sonda directa cubre solo una replica y una ventana corta del pico; el observador puede agregar sobrecarga. No completa la telemetria de Azure Monitor.

No confundir las mediciones cloud v2 con las 400 llamadas locales ni los resultados de octubre. El repositorio es público; datos de cuenta/crédito y los PDF con capturas privadas se entregan localmente. El PR permanece borrador y no se fusionó.

[Informe integrado](05-informe-integrado.md) · [Procedimiento](04-procedimiento-azure.md) · [Plan](plan-pruebas.md)
