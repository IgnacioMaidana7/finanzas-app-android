# Resultados cloud v2 - Ejecución real del 03/11/2026

**Estado: perfil inicial ejecutado y recursos retirados y verificados. Validación completa pendiente.**

## Identificación

- Imagen backend: commit `8e5aeedfac0d35527e2c9bfb2d38a19123a14d30`, digest `sha256:b4e785288de3580f721699fde89e6692f0726e1a694f511c006458c8b121ecbd`.
- Verificador: commit `66562605b5810fb78c3bcf1cb102d9622ca79a05`; SHA-256 `b01b8d0e006451265e08b11dd81a75613c019d9a3d0a65b8cefada5d8c892fdb`.
- Fase funcional/carga/limpieza: 04:10:44,750 a 04:11:41,965 UTC (01:10 a 01:11 Buenos Aires). El tiempo de carga por separado fue 40.719 ms.
- Solo cuentas y movimientos ficticios del proyecto propio; la carga nunca apuntó a DolarAPI ni ArgentinaDatos.

## Carga

| Perfil | Programadas / completadas | RPS programado / real | Máximo en curso configurado / real | p50 / p95 / p99 ms | Errores |
|---|---:|---:|---:|---:|---:|
| baseline | 1.000 / 1.000 | 25 / 24,56 | 32 / 24 | 749 / 783 / 887 | 0 |
| media | 5.000 / no ejecutado | 60 / sin dato | 64 / sin dato | sin dato | sin dato |
| alta | 12.000 / no ejecutado | 100 / sin dato | 128 / sin dato | sin dato | sin dato |
| pico | 20.000 / no ejecutado | 160 / sin dato | 256 / sin dato | sin dato | sin dato |

Todas las respuestas de carga completadas fueron HTTP 200 y su JSON coincidió con el resumen esperado. Percentiles sobre todas las completadas, incluidos fallos si los hubiera; no hubo fallos. La tasa efectiva usa la duración real de la fase.

La validación funcional usa diez movimientos (500 ingresos, 150 gastos, 350 neto). Antes de medir, elimina nueve y carga un movimiento por resumen. Se conserva Firebase Spark. Esta prueba no acredita resúmenes de diez o mil movimientos.

## Autenticación y limpieza

HTTPS 200, anónimo 401, UID inyectado 400, acceso cruzado Firestore 403, segundo usuario sin movimientos, token revocado 401 y sesión nueva 200. Limpieza exitosa y cero errores de eliminación; no quedó archivo de recuperación de fixtures. [JSON original](evidencias/cloud-validation-baseline.json).

## Réplicas y arranque

| Primera observación UTC | Asignadas | Listas |
|---|---:|---:|
| 04:10:34,681 | 0 | 0 |
| 04:10:55,442 | 1 | 0 |
| 04:12:06,284 | 2 | 1 |
| 04:17:07,621 | 0 | 0 |

Son transiciones por réplicas asignadas; hay 43 muestras originales con listas/asignadas, sin interpolación. El máximo listo fue 2. La segunda réplica se observó **después de terminar y limpiar el ensayo**, de modo que no se atribuye la latencia a dos réplicas ni se afirma una comparación de capacidad. [Serie](evidencias/azure-scale-baseline.json) y [resumen](evidencias/azure-scale-baseline-summary.json).

Una sonda separada confirmó cero por API y obtuvo HTTPS 200 en 3.956 ms; incluye red, TLS y arranque, no tiempo puro de inicio. [Sonda](evidencias/cold-response.json).

## Telemetría y costos

Azure Monitor fue consultado por API y portal. CPU y memoria no tenían valores; Requests y Replicas devolvieron cero a pesar de las respuestas y observaciones directas. También se consultaron las métricas preview. No se fabricaron datos ni se reemplazó esta respuesta. [Consultas originales](evidencias/azure-monitor-metrics.json).

Se creó primero un presupuesto de alerta USD 5/mes, umbrales 50/80/100 % al Propietario y filtro al grupo. ACR Basic: tarifa consultada USD 0,1666/día; dos réplicas activas una hora serían USD 0,0756 de cómputo antes de franquicias. El presupuesto mostraba USD 0 antes del retiro: dato demorado, **no factura final cero**. Las capturas de saldo y cuenta quedan en la entrega local porque el repositorio es público.

## Retiro y estado de entrega

Usuario: «Autorizo; retirar los recursos al terminar». Se revisó un plan exclusivo de siete recursos del TP3 y se respaldaron evidencias antes de aplicar el retiro. Recursos **retirados y verificados**. Verificación UTC: 2026-11-03T04:31:04.5955416Z. [Comprobación](evidencias/retirement-verification.json).

Pendientes: autorización para media/alta/pico; CPU y memoria de Azure verificables; costo final contabilizado; experiencia manual Android (no había dispositivo conectado). No se declaró completado todo el trabajo. Los recursos de octubre y las 400 llamadas locales de CI son antecedentes separados.
