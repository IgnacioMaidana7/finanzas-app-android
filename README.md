# Finanzas App · TP2 y TP3

Aplicación Android con Kotlin, Jetpack Compose, Firebase Auth y Firestore.
El TP3 incorpora una API Node.js desplegable en Azure Container Apps para consultar un
resumen mensual autenticado, sin cambiar las operaciones existentes del TP2.

## Entregables del TP3

1. [Estrategia de escalabilidad](docs/tp3/azure/01-estrategia.md).
2. [Configuración y evidencia cloud](docs/tp3/azure/02-configuracion-cloud.md).
3. [Diagrama actualizado](docs/tp3/azure/03-arquitectura.md).

[Estado de validación](docs/tp3/validacion.md) · [Backend y contrato HTTP](backend/README.md).

**Estado:** API desplegada en Azure para Estudiantes con aprobación de consumo
del crédito (presupuesto de control USD 5, ventana de demostración de 24 h).
Se verificaron 1.000 consultas sintéticas sin errores, aislamiento y revocación,
y se observó crecimiento automático de una a dos réplicas. APK debug compilado
con la URL real; la interacción en un teléfono queda para verificación manual.
La [variante Cloud Run](docs/tp3/02-configuracion-cloud.md) se conserva como
alternativa; Google Cloud no tiene facturación vinculada.

Servicio: `https://ca-tp3-finanzas-summary.icybay-96cbe924.chilecentral.azurecontainerapps.io`.
`/healthz` es público; el resumen requiere sesión Firebase. Los usuarios y
movimientos sintéticos se eliminaron al terminar la prueba.

El retiro de los recursos Azure está programado para el **23/10/2026 a las 21:55
(Buenos Aires)**, aproximadamente 24 horas después de la aprobación. La tarea
local necesita la computadora encendida, Codex abierto y la sesión Azure vigente.
Al ejecutarse, la API dejará de responder; Firebase y estos entregables se conservan.
Ver [programación y alcance](docs/tp3/evidence/azure-demo-window.json).

## Android

Requisitos del proyecto existente: SDK Android 37, JDK 25 (criterio del daemon
Gradle del TP2), Gradle Wrapper. `app/google-services.json` corresponde al cliente
Firebase existente; no contiene credenciales del backend.

```powershell
.\gradlew.bat assembleDebug
# Después de desplegar y verificar el servicio real:
.\gradlew.bat assembleDebug '-PSUMMARY_BASE_URL=https://URL-REAL.azurecontainerapps.io'
```

Usar exactamente el origen HTTPS devuelto por Azure (también admite Cloud Run), sin ruta ni barra final.
Si no se configura la propiedad, la tarjeta nueva no aparece y continúa la interfaz
del TP2. Con la URL configurada, aparece en Inicio y permite consultar el mes
seleccionado manualmente. El período se indica en UTC y el resultado mensual
no incluye saldos iniciales. El token se obtiene de Firebase Auth, no se guarda
en archivos y no se envía a destinos HTTP ni mediante redirecciones.

## Pruebas sin cuenta cloud

```powershell
cd backend
npm ci
npm test
npm run load:local
# Requiere Java 21 o superior; todos los datos son sintéticos y locales.
npx firebase emulators:exec --config ../firebase.json --project demo-finanzas-tp3 --only auth,firestore "npm run test:emulator"
```

La integración aborta si no detecta exactamente los emuladores locales y el
proyecto `demo-finanzas-tp3`. No usar el ID real en estas pruebas.
