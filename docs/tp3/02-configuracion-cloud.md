# TP3 · Configuración cloud y evidencia

**Estado al 22/10/2026: preparado localmente, sin despliegue cloud.**
Proyecto Firebase confirmado por el usuario y por Android: `finanzasapp-1f76a`.
La revisión de consola confirmó Firestore regional en Santiago (`southamerica-west1`),
sin cuenta de facturación vinculada. Las APIs de Run, Cloud Build y Artifact
Registry no aparecen habilitadas. Las reglas activas exigen UID propietario;
se revisaron IAM e índices existentes. Los permisos efectivos del futuro backend
todavía deben verificarse. No hay URL real de la API ni
evidencia de autoescalado cloud. Ver [revisión y capturas](revision-cloud.md).

## Configuración reproducible

La carpeta `infra/` define Terraform para Cloud Run v2, cuenta de servicio,
rol personalizado de lectura e IAM de invocación. La imagen debe usar digest
SHA256 de Artifact Registry. El proyecto y la región son entradas obligatorias;
el archivo de ejemplo no es una configuración aplicada.

| Parámetro | Valor propuesto |
|---|---|
| Servicio | `tp3-finanzas-summary` |
| Región propuesta | `southamerica-west1`, junto a Firestore existente |
| Escalado automático | mínimo 0, máximo 2 al nivel del servicio |
| Contenedor | Node 22; usuario sin privilegios |
| CPU / memoria | 1 vCPU / 256 MiB |
| Concurrencia | 16 solicitudes por instancia |
| Tiempo máximo Cloud Run | 15 segundos |
| Asignación CPU | Durante solicitudes (`cpu_idle=true`) |
| Probe | `/healthz`, sólo proceso, no readiness de Firestore |
| Identidad runtime | `tp3-summary-runtime` |
| Datos | Firestore `(default)` existente, sin creación ni migración |
| Autenticación HTTP | ID token Firebase validado dentro de la API |
| Tráfico | 100% a la última revisión; sin afinidad de sesión |

Cloud Run reparte las solicitudes a sus instancias mediante su infraestructura
administrada. No se contrata un Application Load Balancer adicional. El servicio
regional permite redundancia administrada dentro de la región; aún se debe
revisar el alcance de redundancia de la base existente.
Fuentes: [Cloud Run](https://docs.cloud.google.com/run/docs/overview/what-is-cloud-run),
[límites de instancias](https://docs.cloud.google.com/run/docs/configuring/max-instances).

## Inspección previa, sólo lectura

Vincular la cuenta Google que administra el proyecto mediante la consola o
Google Cloud CLI oficial. No compartir tokens ni claves por chat. No confundir
el conector Google Drive con permisos de Google Cloud: son accesos diferentes.
Firebase ya está asociado a un proyecto Google Cloud, sin que esto confirme
facturación habilitada. [Relación de proyectos](https://firebase.google.com/docs/projects/learn-more).

Desde Cloud Shell, o desde PowerShell con Google Cloud CLI autenticada:

```powershell
.\scripts\Inspect-Cloud.ps1 -ProjectId finanzasapp-1f76a
```

El script no habilita APIs, no inicia facturación y no despliega. Requiere permisos
de lectura de proyecto, facturación, bases y servicios. Guarda una selección de
metadatos sin credenciales. Si falta un permiso, solicita a un administrador
esa lectura; no asumir que un error significa facturación deshabilitada.

Antes de continuar registrar:

- Identidad y proyecto correctos; Firestore `(default)` y ubicación actual.
- Facturación efectiva y presupuesto elegido por el usuario.
- APIs necesarias: Run, Artifact Registry, Cloud Build, Firestore, Identity Toolkit,
  IAM y Service Usage; habilitar sólo las que falten después de aprobación.
- Reglas activas, índices existentes y restricciones de organización para
  invocación pública. No ampliar permisos para eludir una política de organización.
- Revisión de permisos del operador: creación de servicio/cuenta/rol y asignación
  de IAM; usar permisos por función en vez de Owner/Editor permanentes.
- Cuenta de compilación, permisos de Artifact Registry, costo de almacenamiento,
  retención de imágenes y registros. La cuenta runtime no es la de compilación.

## IAM previsto

| Permiso runtime | Uso |
|---|---|
| `datastore.databases.get` | Acceso a metadatos de base |
| `datastore.entities.get` | Lectura de documentos |
| `datastore.entities.list` | Consultas de documentos |
| `firebaseauth.users.get` | Estado/revocación del usuario |

El rol no concede escritura. Aun así, su lectura tiene alcance de proyecto y no
se restringe por UID en IAM. El aislamiento depende de la API. La entrada pública
`allUsers → roles/run.invoker` abre transporte; `/v1/summary` exige el ID token.
`/healthz` sólo devuelve un estado genérico. Revisar esta decisión con el plan
antes del apply. [Verificación Firebase](https://firebase.google.com/docs/auth/admin/verify-id-tokens),
[IAM Firestore](https://docs.cloud.google.com/firestore/native/docs/security/iam).

## Construcción y aplicación, después de aprobación

Los siguientes pasos generan recursos y pueden generar cargos. No se ejecutaron
en esta entrega local. Primero aprobar proyecto, región, gasto de demostración,
presupuesto/alertas y configuración pública de transporte.

1. Habilitar sólo las APIs faltantes y crear un repositorio Docker de Artifact
   Registry en la región aprobada. Configurar identidad de compilación.
2. Construir `backend/Dockerfile` con Cloud Build, revisar vulnerabilidades y
   obtener el digest real. No enviar la raíz del repositorio como contexto.
   Ejemplo en Cloud Shell, sustituyendo `REGION` con la aprobada:

   ```sh
   gcloud builds submit backend --project=finanzasapp-1f76a --region=REGION --tag=REGION-docker.pkg.dev/finanzasapp-1f76a/tp3/summary:tp3
   gcloud artifacts docker images describe REGION-docker.pkg.dev/finanzasapp-1f76a/tp3/summary:tp3 --project=finanzasapp-1f76a --format='value(image_summary.digest)'
   ```

3. Copiar `infra/terraform.tfvars.example` a `infra/terraform.tfvars`, completar
   región/digest y habilitar `approve_cloud_changes` sólo con autorización.
   El valor por defecto bloquea el plan para evitar una aplicación accidental.

   ```sh
   terraform -chdir=infra init
   terraform -chdir=infra plan -out=tp3.tfplan
   # Revisar que sólo cree recursos nuevos del TP3. Guardar el plan en lugar privado.
   terraform -chdir=infra apply tp3.tfplan
   terraform -chdir=infra output summary_url
   ```

4. No aplicar `firestore.rules` ni `firestore.indexes.json` automáticamente.
   Comparar/exportar la configuración vigente, validar compatibilidad y aprobar
   cualquier cambio. Las reglas propuestas se prueban localmente; publicar
   índices vacíos podría borrar índices existentes.
5. Consultar `/healthz`; confirmar 401 sin token y resumen correcto con una
   cuenta sintética. Revisar IAM efectivo del runtime, no sólo Terraform.
6. Configurar el origen real en `SUMMARY_BASE_URL` y compilar Android. Probar
   consulta, cambio de mes, desconexión y cierre/cambio de sesión en un dispositivo.

La infraestructura declara protección contra eliminación. Para una limpieza
posterior hay que autorizar y cambiar esa protección expresamente. Evitar un
`destroy` general: no borrar el proyecto Firebase, la base ni recursos del TP2.

## Evidencias pendientes de capturar

| Evidencia | Qué debe demostrar | Estado |
|---|---|---|
| Preflight | Proyecto, ubicación, facturación y APIs | Revisado en consola; sin cambios |
| Plan Terraform | Recursos, imagen, IAM y parámetros aprobados | Pendiente de acceso |
| Servicio desplegado | Revisión lista, origen real, CPU/memoria y cuenta runtime | Pendiente |
| Escalado | Min 0, max 2, concurrencia 16 | Configurado en código; no aplicado |
| Reparto y redundancia | Infraestructura Cloud Run y revisiones saludables | Diseño; no observado |
| Seguridad | 401 sin token y aislamiento entre cuentas sintéticas | Verificado localmente; cloud pendiente |
| Métricas de carga cloud | Solicitudes, latencias, errores, instancias, CPU y lecturas | Pendiente |
| Costo | Presupuesto y alertas, estimación de carga, gasto al terminar | Pendiente |

Después de desplegar, ejecutar `Inspect-Cloud.ps1` también con `-Region` para
guardar configuración del servicio, revisión e IAM de invocación. Revisar y
anonimizar capturas antes de incorporarlas al informe: ocultar correos personales,
identificadores de facturación, tokens y cualquier movimiento real.

La carga local en `evidence/load-local.json` está identificada explícitamente
como local y usa 100 filas sintéticas por solicitud. **No sirve como captura
de configuración ni prueba de autoescalado real.** Para cerrar este entregable
es necesario el acceso cloud y la autorización del usuario para cargos.
