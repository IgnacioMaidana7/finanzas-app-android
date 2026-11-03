# Informe integrado - TP3 v2 sobre FinanzasApp TP7

UTN - Facultad Regional San Francisco. Arquitecturas Móviles, 2026.
Ignacio Maidana y José Santino Arroyo. Docente: Juan Pablo Bono. Revisión: 03/11/2026.

## Estado y alcance

**Recursos retirados y verificados.** Se conserva la aplicación TP7 y se realiza el TP3 mediante estrategia, configuración cloud real y diagrama actualizado. 38000 solicitudes de carga completadas y 0 errores registrados. No se declara completada una etapa sin evidencia.

## 1. Estrategia de escalabilidad

Ingreso HTTPS y balanceo administrado, procesos Node.js sin estado, escalado HTTP 0-2, límite de datos y cuotas explícitas. Firebase sigue siendo dependencia compartida. DolarAPI/ArgentinaDatos tienen caché en Android y no se cargan masivamente; ML Kit ejecuta OCR local. No se implementó redundancia zonal/regional ni se aumentó a seis. [Estrategia](01-estrategia.md).

## 2. Configuración y mediciones

La aplicación conserva Firebase Auth/Firestore, DolarAPI, ArgentinaDatos y ML Kit del TP7. La carga apunta exclusivamente al resumen del backend propio y usa un movimiento ficticio por consulta para respetar Spark. La comprobación funcional usa diez (ingresos 500, gastos 150, neto 350). No hubo carga masiva a DolarAPI ni ArgentinaDatos.

La imagen de backend es idéntica en todas las mediciones: commit `8e5aeedfac0d35527e2c9bfb2d38a19123a14d30`, digest `sha256:b4e785288de3580f721699fde89e6692f0726e1a694f511c006458c8b121ecbd`. Baseline se midió en el primer despliegue; media/alta/pico en un segundo despliegue con la misma región, recursos por réplica, límites y datos. No se afirma que los cuatro compartieran el mismo entorno físico. El verificador del pico agrega marcas UTC de inicio/fin para correlacionar las sondas; no cambia la carga programada ni el backend.

| Perfil | Completadas / meta | RPS programado / real | Máximo en curso configurado / real | p50 / p95 / p99 ms | Errores |
|---|---:|---:|---:|---:|---:|
| baseline | 1000 / 1000 | 25 / 24.56 | 32 / 24 | 749 / 783 / 887 | 0 |
| media | 5000 / 5000 | 60 / 59.48 | 64 / 64 | 750 / 1115 / 1949 | 0 |
| alta | 12000 / 12000 | 100 / 99.39 | 128 / 128 | 816 / 2024 / 3347 | 0 |
| pico | 20000 / 20000 | 160 / 127.64 | 256 / 256 | 1604 / 4853 / 6796 | 0 |

Cada fase repite salud, contrato autenticado, aislamiento, revocación y limpieza de fixtures. El presupuesto se creó antes de ACR/entorno. La autorización de las cargas superiores se recibió expresamente y se verificó cada resultado antes de avanzar. El máximo siguió en 2; se observaron las réplicas realmente asignadas/listas antes, durante y después.

Se completaron 38,000 solicitudes de carga con 0 errores. Los percentiles aumentaron con la carga; no se identifica un cuello de botella de CPU, red o Firebase con la telemetria disponible. La comparacion comprende dos despliegues separados con la misma configuracion e imagen. Una sonda de lectura dentro de una replica durante el final del pico puede agregar sobrecarga; no se descuenta ni se estima su efecto. El pico alcanzo 127.64 req/s, solo 79.8% de los 160 req/s programados; no se acredita esa capacidad sostenida con dos replicas.

La linea base pertenece al primer despliegue; los tres niveles siguientes al segundo, con la misma imagen, region, CPU/memoria y limite de replicas. No son cuatro ensayos sobre el mismo entorno fisico. La segunda replica del baseline aparecio despues de terminar su carga, por lo que no se atribuye ese p95 a dos procesos. Las series no se interpolan.

| Fuente / métrica | Resultado |
|---|---|
| CPU Usage | Consulta Azure Monitor sin valores CPU verificables. |
| WorkingSetBytes | Consulta Azure Monitor sin valores de memoria verificables. |
| Replicas / Monitor | Maximo devuelto 0; comparar con API directa. |
| Replicas / API directa | Maximo realmente observado 2; series por perfil, antes y despues de carga. |
| Requests / Monitor | Suma visible 0, con el intervalo y dimensiones originales. No se sustituye por el contador cliente. |
| Microsoft.Insights | Se verifico NotRegistered y se registro antes del segundo despliegue; no garantiza datos retroactivos. |
| Sonda directa: CPU | Un intervalo bajo carga: 0.263 CPU-s/s en el cgroup de una replica, incluido el observador; no es Azure Monitor. |
| Sonda directa: memoria | 170.40 MiB RSS del servidor Node en 2 muestras bajo carga de una replica. No es el working set total. |

El presupuesto es una alarma, no un corte duro. No se declara costo final cero ni se interpreta la ausencia de filas como gratuidad. El saldo, los presupuestos y las consultas de costos se conservaron localmente porque el repositorio es publico. El registro del proveedor Microsoft.Insights no creo recursos de computo o almacenamiento. Firebase siguio en Spark.

Terraform retiro los siete recursos autorizados del segundo despliegue; verificacion Azure a las 2026-11-03T05:37:03.5891270Z. Se confirmaron grupo ausente, cero recursos propios y presupuesto propio ausente. Tambien se conserva el retiro del primer despliegue. Firebase habitual se mantiene; los fixtures sinteticos se limpiaron por perfil.

[Configuración aplicada](02-configuracion-y-evidencias.md) · [JSON, tiempos y límites](07-resultados-cloud.md).

## 3. Diagrama actualizado

El esquema incluye Android, ingreso y balanceo Azure, escalado HTTP, réplicas bajo demanda, identidad AcrPull, ACR privado, Firebase e integraciones TP7. Distingue límite configurado de observación y registra el retiro. [Arquitectura](03-arquitectura.md).

## Límites de la entrega

CPU y memoria de Azure Monitor siguen sin valores verificables. El costo final contabilizado aun no esta disponible. La prueba manual Android no esta acreditada: no habia un dispositivo conectado. La carga representa un movimiento por resumen. No se valido redundancia zonal/regional ni se aumento el maximo a seis. La sonda directa cubre solo una replica y una ventana corta del pico; el observador puede agregar sobrecarga. No completa la telemetria de Azure Monitor.

Los PDF y capturas originales se entregan localmente. Los planes completos, estado y credenciales no integran Git ni el paquete. Fuentes técnicas oficiales: [Azure](https://learn.microsoft.com/en-us/azure/container-apps/scale-app), [métricas](https://learn.microsoft.com/en-us/azure/container-apps/metrics), [cuotas Firestore](https://firebase.google.com/docs/firestore/quotas), [cgroup](https://docs.kernel.org/admin-guide/cgroup-v2.html) y [proc](https://docs.kernel.org/filesystems/proc.html).
