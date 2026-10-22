# TP3 · Estrategia de escalabilidad

Fecha: 22/10/2026. Variante elegida: Azure para Estudiantes + Firebase existente.
Estado: implementación y pruebas locales completas; despliegue pendiente de
aprobar consumo de crédito. El capítulo 16 del libro no fue proporcionado; este
documento describe decisiones del proyecto y no atribuye contenido al libro.

## Problema y funcionalidad

El TP2 usa Kotlin/Jetpack Compose, ViewModels, Firebase Auth y Firestore. Mantiene
operaciones directas desde Android protegidas por las reglas del propietario.
El TP3 agrega una consulta manual de ingresos, egresos y neto mensual, con enteros
exactos, sin trasladar movimientos completos al teléfono. La pantalla permite
cambiar de mes, reintentar y limpia resultados cuando cambia la sesión.

La API Node 22 recibe mes e ID token Firebase. Verifica criptográficamente la
sesión, consulta su estado/revocación y obtiene el UID exclusivamente del token.
Consulta sólo `users/{uid}/transactions`, con proyección de tres campos, rango
mensual UTC y límite de 1.001 documentos. Devuelve 422 si supera 1.000, evitando
totales parciales. No agrega una API de escritura ni una clave administrativa.

## Balanceo, redundancia y autoescalado

El ingress HTTPS administrado de Azure Container Apps reparte solicitudes entre
réplicas saludables de una única revisión activa. El proceso es sin estado:
no almacena sesiones o resultados financieros entre solicitudes. Cada réplica
puede atender cualquier usuario con el mismo contrato y validación.

Terraform propone perfil **Consumption**, mínimo **0**, máximo **2**, **0,25 vCPU
y 0,5 GiB por réplica**, y regla HTTP con umbral **16**. Ese umbral orienta al
escalador; no impone un máximo de 16 solicitudes simultáneas por proceso. La
plataforma usa KEDA. [Referencia oficial](https://learn.microsoft.com/azure/container-apps/scale-app).

Dos réplicas permiten distribuir carga y reemplazar una instancia fallida; con
mínimo cero puede haber arranque en frío. No se promete disponibilidad continua,
redundancia entre zonas ni recuperación entre regiones. Firestore continúa
regional en Santiago, sin copias programadas observadas. No hay migración de datos.

## Seguridad y cuellos de botella

Azure verifica tokens del proyecto `finanzasapp-1f76a`. La consulta de estado usa
`accounts:lookup` con el token del propio usuario y la clave **cliente** Firebase.
Se rechaza deshabilitación y `auth_time < validSince`; si falta información se
rechaza la consulta. Firestore REST recibe ese mismo ID token, de modo que las
reglas del propietario siguen aplicando. La identidad administrada Azure sólo
posee `AcrPull` sobre su registro; no obtiene acceso administrativo Google.

El crecimiento del cómputo no elimina cuotas Firebase ni acelera consultas sin
índice. El rango utiliza el índice simple de `date` observado en Firestore. Cada
resumen puede consumir hasta 1.001 lecturas; la consulta es manual para contener
lecturas repetidas. El límite de respuesta externa es 2 MiB, con tiempo de espera
de 8 s por llamada. No se registra token, UID ni importes; respuestas `no-store`.

## Evolución y medición

1. Demostración: capturar configuración real, health/401, resultado de un usuario
   sintético, métricas de solicitudes y réplicas antes/durante/después de carga.
2. Crecimiento: medir p95, errores 401/422/503, memoria, duración y lecturas por
   consulta. Establecer un objetivo de latencia después de medir arranque en frío
   y comunicación Azure–Firebase; no inferirlo del ensayo local.
3. Si el volumen mensual supera el límite, materializar resúmenes por usuario/mes
   mediante procesamiento idempotente, con permisos y reconciliación propios.
4. Antes de producción: limitación distribuida por usuario, protección contra
   abuso, copias/restauración ensayadas y revisión de necesidad de zonas/regiones.
   Ajustar mínimos/máximos sólo con presupuesto y mediciones aprobados.

La carga local es sintética y no demuestra autoescalado. El ensayo cloud propuesto
se limita a 1.000 consultas de un usuario de prueba con hasta 10 movimientos, sin
importes reales ni tokens en evidencias. Si no aparece una segunda réplica dentro
del ensayo acotado se documentará ese resultado; no se fabricará evidencia ni se
ampliará carga automáticamente. El máximo de réplicas y un presupuesto de control
no constituyen un límite monetario absoluto.
