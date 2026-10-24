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
| Cloud Run desplegado / carga cloud | **Alternativa no desplegada; se eligió Azure** |
| Azure Container Apps desplegado / carga cloud | **Correcto: 1.000 consultas sintéticas, 0 errores; p95 883 ms** |
| Autoescalado real Azure | **46 muestras: 1 → 2 réplicas (ambas listas) → 0 al quedar sin tráfico** |
| Android con backend real | **APK debug compilado con URL real; interacción manual en teléfono pendiente** |
| Imagen Docker ejecutada localmente | **No realizada:** Docker no está disponible en este equipo |

La compilación, pruebas Android y lint se repitieron con un origen HTTPS
`azurecontainerapps.io` de prueba tras ampliar la validación de URL. No se
contactó ese origen. La revisión inicial del beneficio Azure para Estudiantes
mostró USD 100 y consumo USD 0 antes de desplegar; no es el costo final.
Ver [revisión y costos](azure/02-configuracion-cloud.md).

El workflow de GitHub automatiza pruebas del backend, emuladores, auditoría de
producción, carga sintética, construcción/smoke test del contenedor y validación
de Terraform. Su resultado remoto debe revisarse por separado; esta tabla
informa ejecuciones locales, no presupone que CI terminó correctamente.

**Verificación remota adicional:** el [workflow del commit 1fdc630](https://github.com/IgnacioMaidana7/finanzas-app-android/actions/runs/39868542859)
terminó correctamente en GitHub, incluidos los dos trabajos (backend e
infraestructura), construcción Docker y smoke test del contenedor con health
check y rechazo 401 sin token. No corresponde a un despliegue Google Cloud.

La [ejecución de la adaptación Azure, commit a8fa4f7](https://github.com/IgnacioMaidana7/finanzas-app-android/actions/runs/39874500756)
también terminó correctamente: backend de 19 pruebas, integración de ambos
adaptadores, carga, auditoría, imagen Docker, smoke tests Admin y REST sin ADC,
y validación/pruebas de las dos variantes Terraform.

**Despliegue y verificación reales:** se aplicó Terraform en Azure para Estudiantes
con aprobación explícita del presupuesto de control USD 5 durante 24 h. El
[workflow 123f407](https://github.com/IgnacioMaidana7/finanzas-app-android/actions/runs/39877268933)
pasó todas las verificaciones y produjo la imagen transferida al registro privado.
Se comprobaron hashes, digest y retiro de la credencial temporal del registro.
ACR Tasks rechazó la oferta estudiantil; no se entregaron credenciales Azure a CI.

La [prueba cloud](evidence/cloud-validation.json) verificó health 200, resumen
autenticado 200, falta de sesión y sesión revocada 401, inyección de UID 400 y
lectura cruzada Firestore 403. Usó dos cuentas nuevas y diez documentos sintéticos,
eliminados al finalizar, sin leer datos financieros reales. Ejecutó 1.000 resúmenes
con hasta 32 solicitudes en vuelo y planificación de 25/s: cero errores, p50
753 ms, p95 883 ms y p99 1.258 ms. No son un SLA ni una prueba de capacidad máxima.

Las [muestras de réplicas](evidence/azure-scale-observed.json) provienen de la API
Azure y muestran crecimiento de una a dos instancias, ambas listas, y posterior
reducción a cero. El [gráfico](azure/autoescalado-observado.png) conserva los puntos
medidos. No permite atribuir cada solicitud a una réplica. La consulta inicial de
métricas `Requests` no devolvió series; no se interpreta como ausencia de tráfico.

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
real. La firma de producción queda a cargo del SDK oficial. La prueba cloud
posterior sí emitió tokens reales para las dos cuentas sintéticas nuevas y
verificó su uso y revocación contra el servicio desplegado.

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
La variante elegida es Azure para Estudiantes; se revisaron rol de propietario,
límite de gasto activado, política de regiones y cuotas. El runtime tiene sólo
`AcrPull` sobre el registro. Los archivos Cloud Run se conservan como alternativa.
La base Node y la imagen desplegada están fijadas por digest; la auditoría npm
no equivale a una auditoría completa del sistema operativo. Los máximos de
réplicas y tiempos de espera no son topes absolutos de gasto ni garantizan
cancelar una consulta ya iniciada al proveedor. No hay limitador distribuido por usuario ni protección
dedicada contra abuso en este TP. Ver las etapas de crecimiento en la estrategia.

La lectura del capítulo 16 y la discusión del grupo no se simularon. Las
capturas, configuración efectiva y observaciones cloud se agregaron después de
la autorización. Cost Management no admitió la oferta para consultar el gasto
acumulado: revisar Education, considerando la demora de publicación de consumo.
La interacción completa en un teléfono sigue pendiente; la API y el APK compilado
no se presentan como una prueba manual del flujo móvil.

El retiro Azure se completó el 23/10/2026 a las 22:29 de Buenos Aires, con seis eliminaciones de un plan
nuevo revisado. Se verificaron grupo ausente, estado Terraform vacío y API fuera
de servicio; Firebase y los datos existentes se conservaron. Se eliminó la tarea
programada. Ver [evidencia del retiro](evidence/azure-retirement.json).
