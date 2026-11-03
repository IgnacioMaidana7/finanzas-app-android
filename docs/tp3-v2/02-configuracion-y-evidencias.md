# Entregable 2 - Configuración cloud con evidencia real

**Recursos retirados y verificados.** Se ejecutaron los perfiles de carga con aprobación y revisión individual; sus datos originales, límites y fuentes están en [07-resultados-cloud.md](07-resultados-cloud.md).

| Elemento | Configuración aplicada |
|---|---|
| Grupo / región | rg-tp3-finanzas / Chile Central |
| Entorno | cae-tp3-finanzas, interfaz Express/Rápido |
| Aplicación | ca-tp3-finanzas-summary |
| Registro | tp3finanzasv2im20261102, Basic, Brazil South, admin deshabilitado |
| Identidad | id-tp3-summary; AcrPull restringido al ACR |
| Backend | Node.js 22, 0,25 vCPU / 0,5 GiB por réplica, puerto 8080 |
| Ingreso | HTTPS obligatorio, /healthz, sin token 401 |
| Escala | Mínimo 0, máximo 2; regla HTTP 16; no se aumentó a seis |
| Presupuesto | USD 5/mes, alertas 50/80/100 %, no corte duro |

No se instalaron credenciales administrativas Google en Azure. La API verifica JWT y estado de sesión y consulta Firestore REST con el token del usuario bajo Security Rules. Terraform se aplicó en dos fases y conserva estado fuera de OneDrive/Git.

La aplicación conserva Firebase Auth/Firestore, DolarAPI, ArgentinaDatos y ML Kit del TP7. La carga apunta exclusivamente al resumen del backend propio y usa un movimiento ficticio por consulta para respetar Spark. La comprobación funcional usa diez (ingresos 500, gastos 150, neto 350). No hubo carga masiva a DolarAPI ni ArgentinaDatos.

La imagen de backend es idéntica en todas las mediciones: commit `8e5aeedfac0d35527e2c9bfb2d38a19123a14d30`, digest `sha256:b4e785288de3580f721699fde89e6692f0726e1a694f511c006458c8b121ecbd`. Baseline se midió en el primer despliegue; media/alta/pico en un segundo despliegue con la misma región, recursos por réplica, límites y datos. No se afirma que los cuatro compartieran el mismo entorno físico. El verificador del pico agrega marcas UTC de inicio/fin para correlacionar las sondas; no cambia la carga programada ni el backend.

## Resultados

| Perfil | Completadas / meta | RPS programado / real | Máximo en curso configurado / real | p50 / p95 / p99 ms | Errores |
|---|---:|---:|---:|---:|---:|
| baseline | 1000 / 1000 | 25 / 24.56 | 32 / 24 | 749 / 783 / 887 | 0 |
| media | 5000 / 5000 | 60 / 59.48 | 64 / 64 | 750 / 1115 / 1949 | 0 |
| alta | 12000 / 12000 | 100 / 99.39 | 128 / 128 | 816 / 2024 / 3347 | 0 |
| pico | 20000 / 20000 | 160 / 127.64 | 256 / 256 | 1604 / 4853 / 6796 | 0 |

Autenticación, aislamiento, revocación y limpieza se acreditan por perfil. Las series de réplicas, consultas originales de Monitor, registro del proveedor y sondas directas se conservan separados. No se rellenan huecos ni se confunden fuentes de CPU/memoria.

## Cierre y pendientes

Terraform retiro los siete recursos autorizados del segundo despliegue; verificacion Azure a las 2026-11-03T05:37:03.5891270Z. Se confirmaron grupo ausente, cero recursos propios y presupuesto propio ausente. Tambien se conserva el retiro del primer despliegue. Firebase habitual se mantiene; los fixtures sinteticos se limpiaron por perfil.

CPU y memoria de Azure Monitor siguen sin valores verificables. El costo final contabilizado aun no esta disponible. La prueba manual Android no esta acreditada: no habia un dispositivo conectado. La carga representa un movimiento por resumen. No se valido redundancia zonal/regional ni se aumento el maximo a seis. La sonda directa cubre solo una replica y una ventana corta del pico; el observador puede agregar sobrecarga. No completa la telemetria de Azure Monitor.

Las cuatro cargas autorizadas se revisaron individualmente. La evidencia directa de CPU/RSS, cuando existe, pertenece a contadores del contenedor/proceso y no se presenta como Azure Monitor. El costo final contabilizado y la prueba manual Android permanecen pendientes; no había dispositivo conectado. La observabilidad de plataforma se informa tal como fue devuelta. No se declara cerrado un punto sin evidencia.

El informe PDF y las capturas completas se entregan localmente. Los planes, estado y datos de cuenta no se publican en este repositorio público. El PR sigue borrador mientras existan puntos no acreditados.
