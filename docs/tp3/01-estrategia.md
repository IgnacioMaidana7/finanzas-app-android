# TP3 · Estrategia de escalabilidad

**Aplicación:** Finanzas App. **Base:** TP2, commit
`413497b8c56c05497d4d5eb2f6f9bd455ceda465`. **Fecha:** 22/10/2026.
**Rama:** `tp3/cloud-run-financial-summary`; `main` se conserva sin cambios.

## Objetivo y alcance

Mantener una experiencia móvil estable al aumentar usuarios y consultas,
desacoplando el cálculo de un resumen financiero mensual del teléfono. Se
incorpora una API de sólo lectura en Cloud Run que aprovecha Firebase Auth y
Firestore existentes. La funcionalidad produce ingresos, gastos, diferencia
y cantidad de movimientos, con límites explícitos de lectura y precisión.

La estrategia responde a las actividades de balanceo, redundancia y autoescalado
de la consigna proporcionada. El capítulo 16 de *Lean Mobile App Development*
no se facilitó; este documento no afirma haberlo leído ni le atribuye citas.
La discusión grupal y la lectura siguen siendo actividades del grupo.

## Estado inicial y desafíos

El TP2 usa Compose, ViewModels, Flows y repositorios Firebase. Cada usuario tiene
sus colecciones bajo `users/{uid}`. Los repositorios de movimientos usan
listeners que reciben el historial completo ordenado por fecha; el teléfono
calcula vistas y estadísticas. Esto facilita la actualización en tiempo real,
pero al crecer el historial aumenta lecturas, transferencia, memoria y trabajo
en cada dispositivo. Un backend nuevo no elimina por sí mismo esos listeners.

Otros desafíos son aislar los datos personales, absorber picos de consultas,
evitar depender de una instancia, mantener importes exactos y controlar el gasto.
El repositorio no tenía reglas de Firestore versionadas ni infraestructura cloud.
La consola confirmó una base regional en Santiago y ausencia de facturación
vinculada. Las reglas efectivas todavía deben inspeccionarse.

## Diseño propuesto e implementado en código

| Aspecto | Decisión para el TP3 | Motivo y límite |
|---|---|---|
| Backend | Node 22, contenedor sin estado | Cualquier instancia puede atender al mismo usuario |
| Balanceo | URL HTTPS y distribución administrada por Cloud Run | Sin balanceador externo dedicado en esta etapa |
| Autoescalado | Escalado automático, mínimo 0 y máximo 2 al nivel del servicio | Reduce recursos ociosos; el arranque en frío es aceptado |
| Capacidad | 1 vCPU, 256 MiB, concurrencia máxima 16 por instancia | Punto inicial a validar con métricas, no capacidad garantizada |
| Redundancia | Ejecución regional administrada y reemplazo de instancias | No constituye recuperación activa en otra región |
| Datos | Firestore regional existente en Santiago | Sin recuperación multirregional; copias programadas inhabilitadas |
| Seguridad | Firebase ID token, revocación y UID derivado del token | Ningún parámetro del cliente elige el usuario consultado |
| Costo de consulta | Un mes, proyección de tres campos, máximo 1.001 documentos | Hasta 1.000 movimientos; error explícito cuando se supera |

Cloud Run ajusta instancias según demanda y señales de utilización. La
concurrencia elegida permite compartir recursos durante esperas de Firestore.
La distribución entrante es responsabilidad de la plataforma; no se configura
afinidad de sesión. El mínimo cero permite apagar instancias sin tráfico.
Fuentes: [autoescalado](https://docs.cloud.google.com/run/docs/about-instance-autoscaling),
[concurrencia](https://docs.cloud.google.com/run/docs/about-concurrency).

El máximo de instancias es una medida de capacidad y control de costos, con
posibles excesos transitorios; no es un límite monetario. Crear varias revisiones
tampoco debe multiplicar el límite: se usa escalado al nivel del servicio.
Fuente: [máximo de instancias](https://docs.cloud.google.com/run/docs/configuring/max-instances).

## Flujo y consistencia

1. El usuario inicia sesión en Firebase Auth desde Android.
2. Al pulsar Consultar resumen, Android solicita un ID token vigente y envía
   mes y token por HTTPS. No envía el UID ni la lista de movimientos.
3. La API verifica el token para el proyecto configurado y consulta revocación.
4. Lee sólo `users/{uid verificado}/transactions` dentro del período UTC.
5. Calcula totales enteros con BigInt y devuelve cadenas decimales compatibles
   con Long. La respuesta indica período, zona y unidad.

Cada consulta es una lectura actual de Firestore. No hay caché compartida ni
promesa de actualización en tiempo real en la tarjeta; el usuario puede volver
a consultar. No se suman saldos iniciales: el resultado del mes no es el saldo
total de las cuentas. La interfaz conserva las vistas existentes del TP2.

## Seguridad y degradación

La cuenta de ejecución dedicada no tiene permisos de escritura ni claves
descargadas. Permite leer Firestore y verificar estado del usuario. Como Admin
SDK omite Security Rules, la ruta por UID se impone en el backend y se prueba
con dos usuarios. Las reglas versionadas permiten al móvil acceder únicamente
a sus colecciones conocidas, incluidos aportes a metas. Antes de publicarlas
hay que compararlas con las reglas activas para no eliminar permisos legítimos.
Fuentes: [ID tokens](https://firebase.google.com/docs/auth/admin/verify-id-tokens),
[reglas](https://firebase.google.com/docs/firestore/security/rules-conditions).

Los errores de autenticación no consultan movimientos. Un fallo de dependencia
devuelve 503 sin datos internos; datos inválidos o exceso de volumen devuelven
422 y no un total engañoso. No se guardan tokens, UIDs ni importes en los logs
propios. La app muestra carga y reintento; cancela resultados de una sesión
anterior. Cloud Run recibe tráfico público para permitir Firebase Auth a nivel
de aplicación, con el costo residual de solicitudes rechazadas. Antes de abrir
un servicio a público amplio, evaluar App Check y protección contra abuso.

## Crecimiento por etapas

**Etapa actual:** API mensual acotada y Cloud Run regional. Validar latencia,
memoria y lecturas con datos sintéticos antes de ampliar límites.

**Siguiente etapa:** paginar el historial móvil y sustituir listeners completos
por consultas por período. Si aparecen meses con más de 1.000 movimientos,
crear resúmenes por usuario/mes actualizados con operaciones idempotentes y
reconciliación; no aumentar indiscriminadamente el límite de documentos.

**Mayor escala:** fijar objetivos de disponibilidad y recuperación; evaluar
Firestore multirregional y despliegue en varias regiones. Un balanceador global
externo, Cloud Armor o mínimo de instancias mayor que cero sólo se justifican
con demanda medida y presupuesto aprobado. Son ampliaciones propuestas, no
componentes desplegados ni requisitos ya satisfechos.

## Validación y observabilidad

Pruebas de contrato, errores, enteros grandes y límites; integración local
Firebase para aislamiento, reglas y consulta real; carga HTTP sintética local
con concurrencia 16. Ver resultados exactos en [validación](validacion.md).

Después del despliegue, usar una cuenta sintética con datos aislados y aplicar
una rampa breve de solicitudes concurrentes. Registrar p50/p95/p99, errores,
cantidad de instancias, CPU/memoria y lecturas. Una carga demasiado pequeña
puede no producir más de una instancia; no afirmar autoescalado observado si
el gráfico no lo muestra. Detener ante 5xx, latencia sostenida o gasto inesperado.

Objetivos iniciales propuestos para la demostración, aún no SLO medidos:
menos de 1% de errores válidos y p95 menor a 1 s en régimen caliente. Medir
arranque en frío por separado. Si no se cumplen, revisar dependencia, tamaño
de consulta y concurrencia antes de subir el máximo.

## Costos y decisión de despliegue

Se propone Santiago para compartir región con Firestore, cuya ubicación fue
confirmada en consola. Revisar CPU/tiempo,
memoria, solicitudes, lecturas, red, compilación, Artifact Registry y logs.
Una consulta puede leer hasta 1.001 documentos: 400 consultas podrían sumar
hasta 400.400 documentos, además de otros cargos aplicables. Esta multiplicación
es un escenario de carga, no una cotización monetaria.

Configurar presupuesto y alertas antes de la demostración; los presupuestos
notifican y no detienen automáticamente el gasto. No asumir costo cero por
usar capa gratuita. La habilitación de facturación, APIs, creación de recursos
y cargas cloud quedan pendientes de inspección y autorización explícita.
Fuentes: [precios de Cloud Run](https://cloud.google.com/run/pricing),
[precios de Firestore](https://cloud.google.com/firestore/pricing),
[presupuestos](https://docs.cloud.google.com/billing/docs/how-to/budgets).
