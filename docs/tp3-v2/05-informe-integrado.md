# Informe integrado | TP3 v2: Estrategias de escalabilidad sobre FinanzasApp TP7

**UTN - Facultad Regional San Francisco**  
**Materia:** Arquitecturas Móviles, 2026  
**Estudiantes:** Ignacio Maidana y José Santino Arroyo  
**Docente:** Juan Pablo Bono  
**Fecha de revisión:** 2 de noviembre de 2026  
**Estado:** código e infraestructura preparada; despliegue y nuevas mediciones cloud pendientes.

## Introducción

El TP3 de la guía requiere diseñar una estrategia de escalabilidad, configurar servicios en la nube y presentar un diagrama actualizado. Esta revisión toma como línea base la aplicación Android del TP7 sin convertir el trabajo en un TP de APIs. FinanzasApp conserva Kotlin, Jetpack Compose, MVVM, Firebase Authentication y Firestore; TP7 suma DolarAPI, ArgentinaDatos y Google ML Kit OCR.

## Entregable 1. Estrategia de escalabilidad

La funcionalidad escalable es un **resumen financiero mensual autenticado**. Android envía un token Firebase a una API Node.js 22 por HTTPS. El proceso verifica la identidad, consulta únicamente `users/{uid}/transactions` vía Firestore REST y devuelve ingresos, gastos, neto y cantidad de movimientos. Los importes usan enteros exactos y un mes UTC; se rechazan totales parciales cuando el mes excede 1.000 movimientos. El backend es **sin estado** para permitir múltiples réplicas.

**Balanceo:** Azure Container Apps proporciona ingreso HTTPS administrado. **Autoescalado:** se define KEDA HTTP con umbral 16, `min_replicas=0` y `max_replicas=2` por defecto; el límite superior puede ampliarse hasta 6 previa validación del presupuesto y cuotas. **Redundancia:** dos o más réplicas independientes ayudan a reemplazar un proceso, pero no equivalen a redundancia entre zonas o regiones. **Dependencias:** Firebase puede convertirse en cuello de botella; DolarAPI y ArgentinaDatos no se someterán a carga masiva, y el OCR funciona localmente.

La estrategia detallada está en [01-estrategia.md](01-estrategia.md).

## Entregable 2. Configuración de nube

La infraestructura se expresa en `infra/azure` mediante Terraform: grupo aislado, Container Apps Environment Consumption, Container App en HTTPS (8080, `/healthz`), ACR Basic privado, identidad con permiso `AcrPull` y contenedor por digest SHA-256. No se agregan credenciales administrativas Google en Azure; el acceso a Firestore respeta la sesión Firebase y sus Security Rules.

Se prepara una prueba escalonada con estas **metas de generación**, todavía **no medidas**:

| Perfil | Total programado | Máximo configurado en vuelo | Tasa programada |
|---|---:|---:|---:|
| Base | 1.000 | 32 | 25/s |
| Media | 5.000 | 64 | 60/s |
| Alta | 12.000 | 128 | 100/s |
| Pico | 20.000 | 256 | 160/s |

Con cada perfil deben registrarse solicitudes completadas, máximo efectivamente en vuelo, tasa real, p50/p95/p99, errores, réplicas asignadas/listas, CPU, memoria, lecturas y costos. No se interpretan 20.000 solicitudes totales como 20.000 simultáneas. Las pruebas se limitarán al backend propio con **usuarios y movimientos sintéticos**; hay controles de parada ante errores sostenidos.

**Histórico no comparable directamente:** el informe de octubre 2026 documentó 1.000 solicitudes, cero errores, p95=883 ms y réplicas 1→2→0; esos recursos fueron retirados el 23/10/2026. La v2 necesita un despliegue y evidencias nuevos.

Consultar [02-configuracion-y-evidencias.md](02-configuracion-y-evidencias.md) y [04-procedimiento-azure.md](04-procedimiento-azure.md). La implementación Terraform y las pruebas unitarias/backend fueron validadas por una ejecución CI verde ([GitHub Actions 41539908250](https://github.com/IgnacioMaidana7/finanzas-app-android/actions/runs/41539908250)); esto **no prueba que la nube esté desplegada**.

## Entregable 3. Diagrama actualizado

El [diagrama](03-arquitectura.md) refleja Android, Firebase, API Node.js, ingreso/balanceo administrado, KEDA y réplicas bajo demanda; incorpora DolarAPI, ArgentinaDatos y OCR local del TP7. El esquema distingue la arquitectura **preparada** de la infraestructura **implementada y observada**, que está pendiente de despliegue.

## Resultados y conclusiones al momento de la revisión

**Verificado:** recuperación del backend TP3 sobre la rama TP7, controles del contrato autenticado, infraestructura versionada, pruebas backend y Terraform en GitHub Actions. **Pendiente:** volver a desplegar Azure con cuenta autorizada, medir cargas progresivas y réplicas de la nueva ejecución, capturar configuración real, costos y probar la tarjeta Android en un dispositivo.

El objetivo del trabajo no es solo mostrar que hay más réplicas, sino determinar si efectivamente mejora la respuesta del servicio, qué límites aparecen en Firebase y cuánto cuesta cada cambio de capacidad. No se introducen resultados inventados ni se reutilizan capturas históricas como evidencia v2.

## Referencias

- Guía de Trabajos Prácticos: TP3, Estrategias de Escalabilidad, y TP7, Integración de APIs y SDKs.
- Mike van Drongelen, *Lean Mobile App Development*, capítulo 16.
- Informe TP3 anterior, octubre 2026 (referencia histórica de infraestructura y mediciones).
- Microsoft: https://learn.microsoft.com/es-es/azure/container-apps/scale-app
- Microsoft: https://learn.microsoft.com/es-es/azure/container-apps/billing
- Firebase: https://firebase.google.com/docs/firestore/use-rest-api
