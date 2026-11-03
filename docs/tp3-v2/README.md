# TP3 v2 - Escalabilidad de FinanzasApp sobre TP7

**Estado al 03/11/2026:** despliegue real y perfil inicial ejecutados en Azure. Recursos retirados y verificados. **La validación solicitada completa sigue pendiente:** media, alta y pico no tienen una nueva autorización; CPU/memoria de Azure Monitor y costo final facturado no están acreditados.

| Entregable | Archivo y alcance |
|---|---|
| 1. Estrategia de escalabilidad | [01-estrategia.md](01-estrategia.md), actualizada con límites observados |
| 2. Configuración cloud y evidencias | [02-configuracion-y-evidencias.md](02-configuracion-y-evidencias.md) y [resultados reales](07-resultados-cloud.md) |
| 3. Arquitectura actualizada | [03-arquitectura.md](03-arquitectura.md) y [Mermaid](arquitectura.mmd) |

**Resultado cloud inicial:** 1.000/1.000 respuestas de carga HTTP 200, cero errores; 24,56 req/s; máximo observado en curso 24 de 32; p50/p95/p99 749/783/887 ms; ciclo de réplicas **0 -> 1 -> 2 -> 0** por API Azure. Autenticación, aislamiento, revocación y limpieza sintética pasaron.

La carga resume **un movimiento ficticio por consulta** para respetar Firebase Spark; la comprobación funcional conserva diez. No se cargaron DolarAPI ni ArgentinaDatos. No se reutilizaron las 400 llamadas locales ni las métricas históricas de octubre.

Azure Monitor devolvió CPU/memoria sin datos y réplicas/solicitudes en cero. Esa limitación se conserva; el ciclo de escala se acredita con la API directa. El segundo proceso fue observado después de terminar la carga, por lo que este ensayo no prueba una mejora de latencia por dos réplicas.

El repositorio es público: capturas financieras/de cuenta, planes y estado completos se conservan en la entrega local, fuera de Git. El PR permanece borrador.

[Informe integrado](05-informe-integrado.md) · [Procedimiento Azure](04-procedimiento-azure.md) · [Plan de pruebas](plan-pruebas.md) · [Preparación](06-preparacion-cloud-2026-11-02.md)
