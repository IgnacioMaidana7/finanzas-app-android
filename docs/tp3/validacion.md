# TP3 · Validación y límites de la evidencia

Fecha: 22/10/2026. Entorno local Windows, Node 22.14.0, JDK 25 del Android Studio
existente, SDK Android 37. No se usaron cuentas ni movimientos reales en pruebas.

| Comprobación | Resultado local |
|---|---|
| Android `testDebugUnitTest` | **149 pruebas, 0 fallos** |
| Android `assembleDebug` | Correcto, con URL de prueba y sin URL configurada |
| Android `lintDebug` | Correcto; contiene advertencias heredadas, sin errores bloqueantes |
| Backend `npm test` | **19 pruebas, 0 fallos**, incluidos acceso REST sin clave administrativa y revocación |
| Firebase emuladores | **1 integración completa, correcta**, con dos usuarios sintéticos; adaptadores Admin y REST |
| Instalación `npm ci` | Correcta, dependencias fijadas en lockfile |
| Auditoría de producción | **0 vulnerabilidades conocidas** en la fecha de ejecución |
| Terraform formato/validación | Correctos para Google y Azure (azurerm 4.81.0), Terraform 1.14.3 |
| Terraform pruebas con proveedor simulado | **4 pruebas, 0 fallos** (2 por variante); no usan cuenta cloud |
| Carga HTTP local | **400 solicitudes, concurrencia 16, 0 errores** |
| Cloud Run desplegado / carga cloud | **Pendiente** |
| Azure Container Apps desplegado / carga cloud | **Pendiente de aprobación de consumo de crédito** |
| Flujo Android contra backend real | **Pendiente** |
| Imagen Docker ejecutada localmente | **No realizada:** Docker no está disponible en este equipo |

La compilación, pruebas Android y lint se repitieron con un origen HTTPS
`azurecontainerapps.io` de prueba tras ampliar la validación de URL. No se
contactó ese origen. El beneficio Azure para Estudiantes está activo, con USD 100
y consumo observado USD 0; [revisión y costos](azure/02-configuracion-cloud.md).

El workflow de GitHub automatiza pruebas del backend, emuladores, auditoría de
producción, carga sintética, construcción/smoke test del contenedor y validación
de Terraform. Su resultado remoto debe revisarse por separado; esta tabla
informa ejecuciones locales, no presupone que CI terminó correctamente.

**Verificación remota adicional:** el [workflow del commit 1fdc630](https://github.com/IgnacioMaidana7/finanzas-app-android/actions/runs/39868542859)
terminó correctamente en GitHub, incluidos los dos trabajos (backend e
infraestructura), construcción Docker y smoke test del contenedor con health
check y rechazo 401 sin token. No corresponde a un despliegue Google Cloud.

## Qué verifican las pruebas

Las pruebas HTTP comprueban aislamiento por UID, parámetros no admitidos,
sesiones ausentes/incorrectas/vencidas/revocadas/deshabilitadas, errores de
dependencias sin filtrar detalles, ausencia de caché y logs personales, períodos
UTC, precisión superior a `Number.MAX_SAFE_INTEGER`, límites y desborde de Long.
Pruebas adicionales usan Firebase Admin real sin emulador para rechazar tokens
sin firma, de otro proyecto y con emisor incorrecto, sin requerir credenciales.

La integración usa Auth/Firestore locales. Crea dos usuarios sintéticos y
movimientos, obtiene un token del emulador, ejecuta la consulta real del Admin
SDK, verifica rango/proyección y precisión BigInt, comprueba que sólo devuelve
movimientos del usuario autenticado, impide inyección de UID y valida el límite.
Prueba reglas que rechazan lecturas/escrituras ajenas y anónimas y permiten
colecciones legítimas del TP2, incluidos aportes a metas. Después deshabilita al
usuario y comprueba el rechazo. Elimina los fixtures de los emuladores al finalizar.

El adaptador REST repite el resumen contra Firestore con el ID token del usuario,
sin ADC, y comprueba el rechazo de la cuenta deshabilitada. Los fixtures locales
incluyen email/contraseña sintéticos, para que Auth mantenga el estado de
revocación. Pruebas unitarias adicionales cubren estado ausente, `validSince`,
credenciales por solicitud, consultas y respuestas acotadas. Ambos adaptadores
rechazan tokens sin firma/de proyecto ajeno y emuladores en servicios desplegados.

Los tokens del emulador no demuestran validación criptográfica de un ID token
real. La firma de producción queda a cargo del SDK oficial; la prueba de emisión
y uso de tokens reales sigue pendiente del proyecto de prueba cloud.

Las tres pruebas nuevas de Android verifican cancelación/limpieza del resultado
al cambiar de usuario, cierre de sesión, cambio de mes y recuperación por reintento.
Durante la línea base del TP2 fallaron dos pruebas de categorías porque usaban
presupuestos de septiembre de 2026 con movimientos fechados con el reloj actual.
Se fijaron esas dos fechas en septiembre; no se cambió la lógica de categorías.

Las pruebas de Terraform verifican límites de instancias/concurrencia, permisos
de sólo lectura, fijación de proyecto Firebase y bloqueo si no hay aprobación.
Usan `mock_provider`; no prueban cuotas, facturación, permisos efectivos ni que
la API cloud acepte el despliegue real.

## Carga sintética y auditoría

Los valores exactos de latencia, duración y solicitudes por segundo están en
[`load-local.json`](evidence/load-local.json). El servidor local usa el handler
real con verificación sintética y 100 movimientos sintéticos por solicitud,
incluyendo una demora de lectura artificial de 15 ms. Las latencias cambian
según el equipo y la carga de otros procesos. **No son medidas de Firestore,
Cloud Run ni una demostración de autoescalado.**

La auditoría completa conserva **7 avisos de desarrollo (3 altos, 4 moderados)**
en la CLI de Firebase y dependencias relacionadas: braces/chokidar, OpenTelemetry
de Pub/Sub y uuid/gaxios. Se usa la CLI vigente fijada en el lockfile; no se
forzaron downgrades incompatibles para ocultar avisos. Se fijaron versiones
corregidas de gRPC y re2, comprobadas de nuevo con los emuladores. Estos paquetes
de desarrollo no se instalan en el contenedor (`npm ci --omit=dev`) ni se usan en
la API de producción. Ejecutar las herramientas sólo con fixtures locales y
revisar actualizaciones antes de usarlas con un proyecto real.

Evidencias: [auditoría completa](evidence/dependency-audit.json),
[auditoría de producción](evidence/production-audit.json),
[resumen de suites Android](evidence/android-unit-tests.json).

## Limitaciones que quedan abiertas

Se inspeccionaron en consola ubicación/modalidad de Firestore, vinculación de
facturación y lista de APIs. Firestore es regional en Santiago; no hay facturación
vinculada. Se compararon las reglas publicadas, índices y roles existentes.
Siguen pendientes permisos específicos del nuevo runtime, presupuesto y política
organizacional. La variante elegida ahora es Azure para Estudiantes; los archivos
Cloud Run se conservan como alternativa. El
contenedor debe construirse y revisarse antes del despliegue; la imagen base
todavía usa la etiqueta oficial Node 22 y se recomienda fijar su digest tras
esa revisión. El máximo de instancias y el timeout de Cloud Run no son topes
absolutos de gasto; el timeout tampoco garantiza cancelar una consulta ya
iniciada al proveedor. No hay limitador distribuido por usuario ni protección
dedicada contra abuso en este TP. Ver las etapas de crecimiento en la estrategia.

La lectura del capítulo 16 y la discusión del grupo no se simularon. Las
capturas y métricas cloud deben agregarse después del acceso autorizado. No
presentar configuraciones propuestas o resultados locales como recursos reales.
