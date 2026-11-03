# Informe integrado - TP3 v2 sobre FinanzasApp TP7

**UTN - Facultad Regional San Francisco**
Arquitecturas Móviles, 2026
Ignacio Maidana y José Santino Arroyo
Docente: Juan Pablo Bono
Revisión: 03/11/2026

## Estado real

Se realizó el despliegue Azure v2, se ejecutó el perfil inicial con datos ficticios y se documentaron resultados auténticos. Recursos **retirados y verificados**. **La validación completa no está cerrada:** faltan los perfiles superiores, métricas CPU/memoria y costo final contabilizado. La prueba manual móvil tampoco está acreditada en esta ejecución.

## 1. Estrategia de escalabilidad

Se conserva Kotlin, Jetpack Compose, MVVM, Firebase Auth/Firestore, DolarAPI, ArgentinaDatos y Google ML Kit del TP7. El servicio escalable es el resumen mensual autenticado, con procesos Node.js 22 sin estado, límites de datos, ingreso HTTPS y escalado HTTP administrado. No se escalan ni se cargan masivamente los servicios públicos de indicadores; OCR permanece local.

El rango aplicado fue 0-2 réplicas de 0,25 vCPU / 0,5 GiB, regla HTTP 16. El portal mostró el entorno como Express/Rápido. No se amplió a seis ni se desplegó redundancia zonal/regional. [Estrategia](01-estrategia.md).

## 2. Configuración cloud con evidencias

Terraform creó grupo, presupuesto, ACR Basic privado en Brazil South, identidad AcrPull, entorno y aplicación en Chile Central. Imagen por digest SHA-256; HTTPS obligatorio; `/healthz` 200 y anónimo 401. El usuario autorizó gasto y retiro posterior. El presupuesto USD 5 es una alerta, no un corte duro. No se introdujeron credenciales administrativas Google.

El verificador comprobó diez movimientos, aislamiento, revocación y limpieza. La carga usó uno por resumen para respetar Spark: **1.000/1.000 HTTP 200, cero errores, 24,56 req/s, máximo real en curso 24 de 32, p50/p95/p99 749/783/887 ms**. Son mediciones Azure nuevas; no son las 400 llamadas locales ni las del TP3 retirado en octubre.

La API de Azure registró **0 -> 1 -> 2 -> 0** en 43 muestras. La segunda réplica apareció luego de terminar la carga, por lo que no se afirma mejora por dos procesos ni balanceo por instancia. Primera respuesta en una sonda separada desde cero: 3.956 ms incluida red/TLS/arranque.

Azure Monitor no entregó CPU/memoria y devolvió cero para Requests/Replicas; se conserva esa discrepancia. El costo visible antes del retiro era USD 0, sin factura final disponible. [Configuración](02-configuracion-y-evidencias.md) y [resultados verificables](07-resultados-cloud.md).

## 3. Arquitectura actualizada

El diagrama muestra Android, ingreso y balanceo HTTPS Azure, regla HTTP, procesos sin estado, ACR e identidad, Firebase y las integraciones TP7. Distingue el rango configurado del número observado y registra el estado final de los recursos. [Diagrama y explicación](03-arquitectura.md), [fuente Mermaid](arquitectura.mmd).

## Conclusión y pendientes

El despliegue y el primer perfil dejaron evidencia funcional y de escala real. El ensayo corto no demuestra capacidad sostenida de dos réplicas; para eso se necesitan perfiles mayores autorizados, telemetría utilizable y comparación controlada. Media, alta y pico no se ejecutaron por falta de autorización adicional. Firebase Spark condiciona el conjunto a un movimiento por consulta. No se extrapola el resultado a toda la experiencia Android ni a otras regiones.

El informe PDF local acompaña estos tres entregables con capturas, gráficos y archivos originales. Los planes completos, estado y datos de cuenta no se publican en este repositorio público. El PR permanece borrador.

## Fuentes

- [Azure Container Apps: escalado](https://learn.microsoft.com/en-us/azure/container-apps/scale-app)
- [Container Apps Express](https://learn.microsoft.com/en-us/azure/container-apps/express-overview)
- [Métricas](https://learn.microsoft.com/en-us/azure/container-apps/metrics)
- [Presupuestos](https://learn.microsoft.com/en-us/azure/cost-management-billing/costs/tutorial-acm-create-budgets)
- [Cuotas Firestore](https://firebase.google.com/docs/firestore/quotas)
- Guía TP3 y TP7 de Arquitecturas Móviles; Lean Mobile App Development, capítulo 16.
