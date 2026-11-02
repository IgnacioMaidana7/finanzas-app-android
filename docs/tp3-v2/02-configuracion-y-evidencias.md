# TP3 renovado — Entregable 2: Configuración de nube y evidencias

**Estado actual:** infraestructura Azure del TP3 previo retirada; código e infraestructura versionados, **sin despliegue v2 aprobado**. Este documento no certifica que la API esté disponible en este momento.

## Configuración reproducible preparada

- Terraform en `infra/azure/`: grupo de recursos, Container Apps Environment (Consumption), Container Registry privado, identidad administrada con permiso `AcrPull`, aplicación HTTPS en puerto interno 8080 y sonda `/healthz`.
- Backend Node.js en `backend/`, con tests automatizados, API `GET /v1/summary?month=AAAA-MM` y validación del token Firebase.
- Autoescalado con regla HTTP `http-demand` umbral 16, mínimo 0; `max_replicas=2` por defecto y variable validada para ensayos de capacidad (hasta 6, previa aprobación).
- `backend/scripts/verify-cloud.mjs`: valida salud, seguridad y aislamiento con usuarios ficticios, y contempla perfiles `baseline`, `media`, `alta`, `pico`; el script limpia los datos sintéticos. Los perfiles superiores requieren aprobación explícita adicional; limitar costos y monitorear cuotas.
- `docs/tp3-v2/plan-pruebas.md`: parámetros, métricas y precauciones de ejecución.

## Matriz de pruebas

| Perfil | Solicitudes programadas | Máximo operaciones en curso | Ritmo programado (req/s) | Resultado |
|---|---:|---:|---:|---|
| Base | 1.000 | 32 | 25 | Pendiente (v2) |
| Media | 5.000 | 64 | 60 | Pendiente |
| Alta | 12.000 | 128 | 100 | Pendiente |
| Pico | 20.000 | 256 | 160 | Pendiente |

No confundir operaciones en curso configuradas con simultaneidad observada. Las métricas de carga deben registrarse **por perfil** con evidencia JSON/CSV, fecha y commit. Una prueba local con dependencias sintéticas no acredita escalado en Azure. Las pruebas cloud deben efectuarse solo en infraestructura propia, con datos artificiales y límites aprobados.

## Qué faltará evidenciar para dar por completado este entregable

- [ ] Confirmar cuenta Azure Estudiantes, región, cuotas, costos y permiso para gastar crédito.
- [ ] Ejecutar y guardar `terraform validate`, `plan` y evidencia de recursos aplicados.
- [ ] CI de backend, app Android y tests; imagen de contenedor por digest.
- [ ] Capturas reales de configuración HTTPS, regla HTTP y límite de réplicas de la **nueva** ejecución.
- [ ] Pruebas cloud con fixture sintético aislado: salud 200, sin token 401, aislamiento entre usuarios, revocación y limpieza.
- [ ] Resultados de carga progresiva p50/p95/p99, fallos y RPS efectivo.
- [ ] Serie temporal de réplicas asignadas y listas (antes/durante/después), con marcas de tiempo reales y sin interpolar.
- [ ] Lecturas Firebase, consumo aproximado y costo realmente visible (o declarar costo no disponible).
- [ ] Verificar la experiencia móvil de nueva tarjeta de resumen con una sesión de prueba.
- [ ] Retirar recursos cuando termine la demostración, registrar el retiro y conservar evidencias.

## Distinción con el TP anterior

Las capturas y registros del informe de octubre 2026 **son antecedentes**, no evidencia del despliegue actual. La nueva API de producción no está levantada y no hay resultados v2 de carga medidos.

## Condiciones de parada

Detener ante errores sostenidos, cuotas insuficientes, costos no controlados o fallo de aislamiento/limpieza. No ejecutar cargas sobre DolarAPI, ArgentinaDatos ni otros servicios de terceros.
