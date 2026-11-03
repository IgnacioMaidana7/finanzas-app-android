# Estrategia de escalabilidad

## Servicio y alcance funcional

Android solicita **GET /v1/summary?month=AAAA-MM** con un Firebase ID token. La API deriva el UID del token verificado, consulta los movimientos de ese usuario y devuelve ingresos, gastos, neto y cantidad. Utiliza importes enteros ARS y meses UTC. Limita la consulta a 1.000 movimientos y rechaza resultados parciales.

## Balanceo de carga

El ingreso HTTPS de Azure Container Apps recibe las solicitudes y las dirige a réplicas disponibles. Los procesos Node.js son sin estado: no almacenan sesiones ni resultados que obliguen a enviar un usuario a una instancia específica. El balanceo forma parte del servicio administrado; no se crea un recurso Azure Load Balancer independiente.

## Autoescalado y uso de recursos

La regla HTTP tiene umbral 16, mínimo 0 y máximo 2 réplicas. Cada réplica dispone de 0,25 vCPU y 0,5 GiB. El umbral guía al escalador; no constituye un tope estricto de solicitudes por proceso. El mínimo cero reduce consumo sin tráfico y admite una primera respuesta con arranque en frío.

## Redundancia y continuidad

Varias réplicas permiten atender solicitudes mediante procesos reemplazables. La configuración medida no garantiza redundancia entre zonas o regiones. Un requisito de continuidad más exigente necesitaría un diseño y una evaluación adicionales; no se atribuye esa garantía a dos procesos del mismo entorno.

Las capturas originales se incluyen en el PDF y en el paquete local de evidencias.

Ante crecimiento sostenido, evaluar consultas agregadas, caché o resúmenes precomputados. Agregar réplicas no amplía las cuotas de Firebase.
