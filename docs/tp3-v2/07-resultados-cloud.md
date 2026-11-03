# TP3 v2 - Resultados reales Azure, 03/11/2026

**Recursos retirados y verificados. 38000 solicitudes de carga completadas; 0 errores.**

La aplicación conserva Firebase Auth/Firestore, DolarAPI, ArgentinaDatos y ML Kit del TP7. La carga apunta exclusivamente al resumen del backend propio y usa un movimiento ficticio por consulta para respetar Spark. La comprobación funcional usa diez (ingresos 500, gastos 150, neto 350). No hubo carga masiva a DolarAPI ni ArgentinaDatos.

La imagen de backend es idéntica en todas las mediciones: commit `8e5aeedfac0d35527e2c9bfb2d38a19123a14d30`, digest `sha256:b4e785288de3580f721699fde89e6692f0726e1a694f511c006458c8b121ecbd`. Baseline se midió en el primer despliegue; media/alta/pico en un segundo despliegue con la misma región, recursos por réplica, límites y datos. No se afirma que los cuatro compartieran el mismo entorno físico. El verificador del pico agrega marcas UTC de inicio/fin para correlacionar las sondas; no cambia la carga programada ni el backend.

## Perfiles y fuentes

| Perfil | Completadas / meta | RPS programado / real | Máximo en curso configurado / real | p50 / p95 / p99 ms | Errores |
|---|---:|---:|---:|---:|---:|
| baseline | 1000 / 1000 | 25 / 24.56 | 32 / 24 | 749 / 783 / 887 | 0 |
| media | 5000 / 5000 | 60 / 59.48 | 64 / 64 | 750 / 1115 / 1949 | 0 |
| alta | 12000 / 12000 | 100 / 99.39 | 128 / 128 | 816 / 2024 / 3347 | 0 |
| pico | 20000 / 20000 | 160 / 127.64 | 256 / 256 | 1604 / 4853 / 6796 | 0 |

Los RPS efectivos y la concurrencia observada son mediciones, no equivalencias con sus techos. Los percentiles miden hasta completar y validar el JSON e incluyen fallos si existieran. La preparación y limpieza no integran la duración de carga.

Se completaron 38,000 solicitudes de carga con 0 errores. Los percentiles aumentaron con la carga; no se identifica un cuello de botella de CPU, red o Firebase con la telemetria disponible. La comparacion comprende dos despliegues separados con la misma configuracion e imagen. Una sonda de lectura dentro de una replica durante el final del pico puede agregar sobrecarga; no se descuenta ni se estima su efecto. El pico alcanzo 127.64 req/s, solo 79.8% de los 160 req/s programados; no se acredita esa capacidad sostenida con dos replicas.

- **baseline**: 2026-11-03T04:10:44.750Z; commit `66562605b5810fb78c3bcf1cb102d9622ca79a05`; verificador SHA-256 `b01b8d0e006451265e08b11dd81a75613c019d9a3d0a65b8cefada5d8c892fdb`; [JSON original](evidencias/cloud-validation-baseline.json). Limpieza: `True`.
- **media**: 2026-11-03T04:58:24.548Z; commit `bb566ada9f1b5f076ef48f5e18ae50879f75ad75`; verificador SHA-256 `b01b8d0e006451265e08b11dd81a75613c019d9a3d0a65b8cefada5d8c892fdb`; [JSON original](evidencias/cloud-validation-media.json). Limpieza: `True`.
- **alta**: 2026-11-03T05:07:26.313Z; commit `bb566ada9f1b5f076ef48f5e18ae50879f75ad75`; verificador SHA-256 `b01b8d0e006451265e08b11dd81a75613c019d9a3d0a65b8cefada5d8c892fdb`; [JSON original](evidencias/cloud-validation-alta.json). Limpieza: `True`.
- **pico**: 2026-11-03T05:17:17.207Z; commit `36a4a8efcec8b3003638259ba235219fb3169541`; verificador SHA-256 `f28792ab2adb5e7e73c9ba7d8975af5af5a59b9951aab587b55bebdda794d1ea`; [JSON original](evidencias/cloud-validation-pico.json). Limpieza: `True`.

## Seguridad y limpieza

Se verifican HTTPS 200, anónimo 401, segundo usuario sin movimientos, UID inyectado 400, acceso cruzado Firestore 403, token revocado 401 y sesión nueva 200. Cada ejecución registra sus comprobaciones y limpieza en su JSON. Los fixtures son cuentas y movimientos ficticios; no se utiliza información financiera real.

## Réplicas observadas

| Perfil | Transiciones asignadas observadas | Muestras reales |
|---|---|---:|
| baseline | 0 -> 1 -> 2 -> 0 | 43 |
| media | 1 -> 2 -> 0 | 51 |
| alta | 1 -> 2 -> 0 | 56 |
| pico | 1 -> 2 -> 0 | 59 |

El máximo aplicado permaneció en 2. Las listas y asignadas se conservan por separado; no se interpolan muestras faltantes. El baseline observó la segunda réplica después de terminar su carga; su p95 no demuestra mejora por dos procesos. La sonda de primera respuesta desde cero midió 3.956 ms incluida red/TLS/arranque, no inicio puro del proceso.

## CPU, memoria y plataforma

Se detectó `Microsoft.Insights=NotRegistered` y se registró antes del segundo despliegue. Esto no prueba por sí solo que existan métricas ni recupera datos históricos. [Registro](evidencias/monitor-provider-registration.json).

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

Las sondas directas ejecutan lecturas dentro de réplicas propias: `cpu.stat` es CPU acumulada del cgroup en microsegundos; RSS corresponde al proceso servidor Node, identificado entre procesos de ese contenedor. El observador puede sumar una pequeña sobrecarga; RSS no equivale a todo el working set de Azure. No se comparan estos datos como si fueran los contadores de Monitor. Las muestras se correlacionan con la ventana UTC del pico, sin inventar valores.

## Costos, permisos y retiro

Se verificó suscripción Estudiantes activa, límite de gasto habilitado y rol Propietario. Cada despliegue creó primero el presupuesto de alerta USD 5 mensual (50/80/100 % al Propietario), después ACR y entorno. No es corte duro. ACR Basic tiene tarifa consultada USD 0,1666/día; no se declara costo final cero por una pantalla o consulta vacía. Firebase permaneció Spark.

El usuario autorizó los tres niveles restantes con revisión entre cada uno y mantuvo la autorización de retiro. [Revisión media](evidencias/profile-review-media.json), [alta](evidencias/profile-review-alta.json), [pico](evidencias/profile-review-pico.json). No hubo avance automático entre niveles.

Estado final: **Recursos retirados y verificados**, comprobación UTC `2026-11-03T05:37:03.5891270Z`. Se conserva el retiro inicial y el final; se mantienen Firebase y sus datos habituales. [Retiro final](evidencias/retirement-verification.json).

## Límites

CPU y memoria de Azure Monitor siguen sin valores verificables. El costo final contabilizado aun no esta disponible. La prueba manual Android no esta acreditada: no habia un dispositivo conectado. La carga representa un movimiento por resumen. No se valido redundancia zonal/regional ni se aumento el maximo a seis. La sonda directa cubre solo una replica y una ventana corta del pico; el observador puede agregar sobrecarga. No completa la telemetria de Azure Monitor.

Las cuatro cargas autorizadas se revisaron individualmente. La evidencia directa de CPU/RSS, cuando existe, pertenece a contadores del contenedor/proceso y no se presenta como Azure Monitor. El costo final contabilizado y la prueba manual Android permanecen pendientes; no había dispositivo conectado. La observabilidad de plataforma se informa tal como fue devuelta. No se declara cerrado un punto sin evidencia.

El repositorio es público: capturas financieras/de cuenta, planes y estado completos permanecen en la entrega local. Las 400 llamadas locales y los datos del Azure de octubre son antecedentes, no mediciones v2.
