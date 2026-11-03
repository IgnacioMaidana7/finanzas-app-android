# TP3 v2 | Operación de Azure y captura de evidencias (procedimiento de ejecución)

**ESTADO:** procedimiento reproducible. La ejecución v2 del 03/11/2026 sí se desplegó y se midieron los perfiles progresivos; ver [resultados](07-resultados-cloud.md). Las instrucciones por sí solas no son evidencia. La prueba anterior de octubre está retirada.

## Objetivo y requisitos

El TP3 de la guía solicita estrategia de escalabilidad, **configuración en la nube** y diagrama actualizado. Partimos del TP7 de FinanzasApp y mantenemos la API de resumen financiero del TP3 anterior, sin modificar DolarAPI, ArgentinaDatos ni Google ML Kit. El ensayo de carga se realizará exclusivamente sobre la API propia y solo con usuarios/transacciones sintéticas.

El procedimiento presupone Windows PowerShell, Terraform, Node 22, acceso a GitHub Actions, Azure CLI, una suscripción Azure para Estudiantes **vigente** y permisos efectivos para crear Container Apps, ACR, entornos e identidad. No hay conexión automatizada a Azure desde este chat. Los comandos que generan gasto requieren revisar precios, cuotas, permisos y aprobación antes de ejecutarse.

## Paso 1. Verificaciones sin costo de despliegue

```powershell
az login
az account list --output table
az account set --subscription "<ID_DE_TU_SUSCRIPCION>"
az account show --query "{id:id,name:name,state:state}" --output table
terraform -chdir=infra/azure fmt -check -recursive
terraform -chdir=infra/azure init -backend=false
terraform -chdir=infra/azure validate
terraform -chdir=infra/azure test
```

Verificar en Azure Portal que la suscripción mantiene crédito estudiantil y límite de gasto; cotejar región Chile Central para el cómputo, Brazil South para el registro privado, cuotas por región, precio de ACR Basic y costos por operaciones Firebase. **Un presupuesto de 5 USD es una alarma de control, no un corte duro exacto**. No colocar claves en GitHub, en comandos compartidos ni en informes.

En la preparación del 02–03/11/2026 se verificaron los proveedores necesarios individualmente. El catálogo completo de proveedores agotó el tiempo de espera de AzureRM 4.81.0. Para esa sesión se usó `$env:ARM_PROVIDER_ENHANCED_VALIDATION_RESOURCE_PROVIDERS = 'false'`, conservando `resource_provider_registrations="none"`: evita cargar ese catálogo para validación de nombres; no cambia autenticación, HTTPS, RBAC ni validación del servicio Azure. Los registros necesarios deben seguir comprobándose antes de aplicar.

`infra/azure/budget.tf` prepara un presupuesto mensual de USD 5, filtrado al grupo del TP3, con alertas al 50 %, 80 % y 100 % al rol Propietario. No asumir que está creado hasta verificar la aplicación y la API de presupuestos.

## Paso 2. Preparación de las variables locales

Copiar `infra/azure/terraform.tfvars.example` a `infra/azure/terraform.tfvars` y completar `subscription_id`, `location`, `registry_name` y `registry_location`. El archivo real está excluido de Git. Mantener `deploy_app=false` hasta disponer de imagen probada.

Antes de planificar un despliegue real, inicializar el backend local con una ruta fuera del repositorio y de OneDrive, por ejemplo `terraform -chdir=infra/azure init -backend-config="path=C:/ruta-privada/tp3-v2/terraform.tfstate"`. `init -backend=false` sirve solo para validación sin estado. No confiar en `-state` del plan para elegir la ruta del posterior apply; usar el backend explícito y comprobarla.

La fase de creación de recursos solo debe iniciarse tras autorización explícita del crédito. `approve_credit_usage=true` habilita la precondición Terraform, pero no sustituye esa autorización. No incorporar tokens ni secretos a variables de línea de comandos o capturas públicas.

## Paso 3. Infraestructura y contenedor (genera potenciales costos)

1. Revisar `terraform -chdir=infra/azure plan -out=plan.tfplan` y **verificar** que no afecta recursos ajenos; luego aplicar el plan aprobado con `terraform -chdir=infra/azure apply plan.tfplan`.
2. En GitHub Actions ejecutar el workflow `TP3 backend and infrastructure` sobre esta rama con `export_image=true`. El job debe completar tests Node, emuladores Firebase, smoke test, análisis de dependencias y contenedor. Descargar el artefacto privado y su checksum; no hay Docker local obligatorio.
3. Transferir el contenedor al ACR privado mediante `scripts/Push-TestedImage.ps1` y herramienta `crane` instalada de fuente oficial con hash verificado. Conservar el digest `sha256:...` de la imagen.
4. Cambiar `deploy_app=true`, agregar `image="<REGISTRY>.azurecr.io/summary@sha256:<DIGEST_REAL>"` y mantener `max_replicas=2` en el primer ensayo.
5. Facilitar la clave **cliente** de Firebase como variable de entorno local `TF_VAR_firebase_client_api_key` (no es una clave de cuenta de servicio) y no mostrar su valor. Revisar `terraform plan` antes de aplicar el despliegue. Comprobar la URL HTTPS asignada, el puerto 8080, `/healthz`, HTTPS obligatorio y el ingreso restringido al contrato esperado.

**Nota:** la variante Azure utiliza `FIREBASE_DATA_ACCESS=user-rest`, token ID verificado y Firestore REST bajo las Security Rules existentes. La identidad administrada Azure únicamente puede obtener la imagen del registro (`AcrPull`).

## Paso 4. Verificación funcional y carga por fases

Ejecutar primero una prueba de salud y aislamiento. El script `backend/scripts/verify-cloud.mjs` utiliza exclusivamente cuentas y datos sintéticos de prueba; valida token, separación entre usuarios y limpieza. Antes de la primera ejecución: revisar que Firebase permita crear cuentas de prueba y que la aplicación tenga la cuota necesaria. Usar un terminal de monitoreo aparte y registrar la cantidad de réplicas en cada fase.

**Firebase Spark confirmado por el usuario:** la comprobación funcional usa diez movimientos y, antes de la carga, elimina nueve fixtures. Cada consulta de carga procesa un solo movimiento. Los cuatro perfiles requieren aproximadamente 38.000 lecturas más controles, dentro de las 50.000 lecturas diarias solo si el resto del consumo lo permite. Consultar el uso previo; detener ante agotamiento. No habilitar Blaze automáticamente. No comparar estos resultados como si fueran consultas de diez movimientos. El JSON registra este tamaño de carga, commit y hash del verificador.

```powershell
# Solo tras revisar el origen HTTPS y aprobar el gasto/uso.
$env:SUMMARY_BASE_URL = "https://<TU-CONTAINER-APP>.azurecontainerapps.io"
$env:TP3_APPROVE_SYNTHETIC_CLOUD = "1"
$env:TP3_LOAD_PROFILE = "baseline"
node backend/scripts/verify-cloud.mjs
# Una fase siguiente exige autorización adicional explícita:
$env:TP3_APPROVE_HIGH_LOAD = "1"
$env:TP3_LOAD_PROFILE = "media"
node backend/scripts/verify-cloud.mjs
```

No ejecutar simultáneamente perfiles desde varios equipos; no automatizar el salto al perfil siguiente. Cada nivel usa una nueva ejecución aislada y conserva `docs/tp3-v2/evidencias/cloud-validation-<perfil>.json`. Los resultados **solo existen** si se ejecutó ese perfil y se verificó que los fixtures se limpiaron. Si un ensayo retorna errores sostenidos, cuota agotada, saturación no controlada o riesgo de costos: detener y analizar.

En una terminal separada, registrar réplicas de Azure usando:
```powershell
.\scripts\Measure-CloudScale.ps1 -Profile baseline -DurationSeconds 480 -IntervalSeconds 5
```

Los archivos `azure-scale-<perfil>.json` deben conservar series de tiempo realmente observadas: réplicas asignadas y listas. No completar muestras faltantes por interpolación. Complementar con Azure Monitor (métricas Requests, Replicas, CPU, memoria si disponibles) y capturas de portal.

## Paso 5. Comparación de escenarios

| Perfil | Total programado | Operaciones en curso máximas configuradas | Tasa programada | Resultado |
|---|---:|---:|---:|---|
| baseline | 1.000 | 32 | 25/s | Ejecutado; ver resultados originales |
| media | 5.000 | 64 | 60/s | Ejecutado; ver resultados originales |
| alta | 12.000 | 128 | 100/s | Ejecutado; ver resultados originales |
| pico | 20.000 | 256 | 160/s | Ejecutado; ver resultados originales |

La prueba debe informar **RPS efectivos** y concurrencia **real observada**, no asumir que los límites configurados se alcanzaron. Registrar p50, p95, p99, fallos 4xx/5xx, CPU, memoria, réplicas listas y asignadas, lecturas a Firestore, tiempos de arranque y costo aproximado/real si está visible.

Solo si el ensayo de 2 réplicas es estable y se comprueban cuotas/costos, considerar repetir un nivel idéntico con `max_replicas` mayor (hasta 6). Esta variable es un límite, no garantiza que se creen todas las instancias. No realizar pruebas de carga sobre DolarAPI ni ArgentinaDatos.

## Paso 6. Evidencia y cierre

Capturar: (a) recurso Container App y su regla HTTP; (b) 0..max réplicas y revisión efectiva; (c) comprobación HTTPS/health; (d) pruebas de aislamiento Auth/Firestore con cuentas sintéticas; (e) serie temporal de réplicas; (f) salida JSON de cada fase con tasas y percentiles; (g) factura/crédito si visible y límites; (h) validación Terraform; (i) APK probado en Android.

Luego de respaldar evidencias, dejar los recursos activos o retirarlos **solo según autorización explícita del usuario**. Si autoriza retiro, revisar `terraform plan -destroy`, confirmar que el grupo es exclusivo del TP3 y ejecutar la eliminación únicamente tras revisar la lista. Comprobar ausencia de recursos Azure, conservar Firebase intacto salvo los fixtures sintéticos autorizados y dejar constancia de fecha/hora. No atribuir costos cero a herramientas que no entregaron datos.

## Fuentes oficiales

- Escalado Azure: https://learn.microsoft.com/es-es/azure/container-apps/scale-app
- Métricas Azure: https://learn.microsoft.com/es-es/azure/container-apps/metrics
- Facturación Azure: https://learn.microsoft.com/es-es/azure/container-apps/billing
- Firebase REST: https://firebase.google.com/docs/firestore/use-rest-api

## Registro de Monitor comprobado

Antes de medir, comprobar `az provider show -n Microsoft.Insights --query registrationState`. En esta continuación estaba NotRegistered y se registró mediante `az provider register -n Microsoft.Insights --wait`. Registrar un proveedor no garantiza datos: conservar consultas y distinguir Azure Monitor de sondas directas del contenedor. No crear almacenamiento/Log Analytics de pago sin revisar alcance y costo.
