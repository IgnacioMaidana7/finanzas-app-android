# Preparación del despliegue real - 02-03/11/2026

La sesión Azure del usuario, permisos de Propietario, oferta de Estudiantes, límite de gasto y saldo suficiente fueron verificados. Las capturas financieras y de cuenta se conservan únicamente en la entrega local porque GitHub informa que este repositorio es público.

Terraform validó dos pruebas con proveedor simulado y preparó seis creaciones sin cambios ni eliminaciones: grupo, presupuesto mensual de alerta USD 5, ACR Basic, identidad administrada, AcrPull y entorno Consumption. ACR y entorno dependen del presupuesto para crearse después de él. La API se despliega en segunda fase por digest, con HTTPS obligatorio, 0-2 réplicas, 0,25 vCPU y 0,5 GiB por réplica.

El usuario autorizó crear estos recursos, consumir crédito, ejecutar el perfil inicial y retirar recursos después de respaldar las evidencias. Los perfiles siguientes y aumentos de réplicas se revisan por separado. El presupuesto de alerta no detiene el gasto.

## Firebase Spark

El usuario confirmó Spark: 50.000 lecturas Firestore diarias, compartidas con toda la aplicación. La prueba funcional conserva diez movimientos. Después elimina nueve fixtures y usa un movimiento por consulta en todos los perfiles. Los cuatro niveles requieren aproximadamente 38.000 lecturas más comprobaciones, condicionadas al consumo previo del día. No se cambia el plan a Blaze.

Este conjunto representa un mes pequeño, no capacidad con diez o mil movimientos. El generador registra commit, hash del verificador, códigos HTTP, RPS efectivos y concurrencia observada. Se detiene ante HTTP 429, diez fallos consecutivos o diez fallos entre las últimas cien respuestas; nunca inicia el siguiente perfil automáticamente.

## Preparación comprobable

- Cuota Chile Central: 0 de 1 entornos administrados en la consulta previa; núcleos del entorno pendientes de comprobar tras crearlo.
- Proveedores Microsoft.App, Microsoft.ContainerRegistry, Microsoft.ManagedIdentity, Microsoft.CostManagement y Microsoft.Consumption registrados.
- ACR previsto: tp3finanzasv2im20261102 en Brazil South; nombre disponible en la consulta previa.
- Contenedor fuente: commit 8e5aeedfac0d35527e2c9bfb2d38a19123a14d30; [empaquetado 41574591148](https://github.com/IgnacioMaidana7/finanzas-app-android/actions/runs/41574591148). Los resultados de CI no se presentan como rendimiento Azure.
- [Preparación de la imagen](evidencias/tested-image-preparation.json): hashes ZIP y TAR comprobados localmente, sin usar el contenedor histórico de octubre.
- [Resumen del plan previo](evidencias/azure-proposed-plan.json): propuesta sin aplicar en el momento de la captura; no es constancia de recursos creados.

La descarga inicial del proveedor no completó y se reutilizó la copia local existente de AzureRM 4.81.0, compatible con el bloqueo. La consulta del catálogo completo de proveedores agotó el tiempo de espera. Se usó la opción oficial ARM_PROVIDER_ENHANCED_VALIDATION_RESOURCE_PROVIDERS=false tras verificar individualmente los registros necesarios. Esto evita esa validación de nombres por catálogo; conserva autenticación, HTTPS, RBAC y las validaciones del servicio Azure.

El estado Terraform, planes completos, credenciales locales y datos de cuenta quedan fuera de Git y de OneDrive. Los tres entregables se cerrarán únicamente según los resultados del despliegue y pruebas reales.

## Fuentes

- [Presupuestos Azure](https://learn.microsoft.com/en-us/azure/cost-management-billing/costs/tutorial-acm-create-budgets)
- [Límite de gasto Azure](https://learn.microsoft.com/en-us/azure/cost-management-billing/manage/spending-limit)
- [ACR](https://azure.microsoft.com/en-us/pricing/details/container-registry/)
- [Container Apps](https://azure.microsoft.com/en-us/pricing/details/container-apps/)
- [Cuotas Firestore](https://firebase.google.com/docs/firestore/quotas)
