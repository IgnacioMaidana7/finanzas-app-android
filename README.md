# Finanzas App Android

Aplicación de finanzas personales desarrollada con Kotlin, Jetpack Compose, Firebase Authentication y Cloud Firestore para Arquitecturas Móviles.

Permite registrar ingresos y gastos, organizar cuentas, definir presupuestos por categoría, seguir metas de ahorro y consultar estadísticas. La arquitectura separa presentación, ViewModels y repositorios.

## Integraciones del TP7

- **Cotizaciones e inflación**, desde Inicio: referencias de DolarAPI, equivalencias en USD y comparaciones por inflación mensual de ArgentinaDatos.
- **Tickets**, desde Nuevo movimiento de tipo Gasto: foto o selección de imagen, OCR local con Google ML Kit, revisión de candidatos y confirmación antes de guardar.

La implementación conserva la base del TP2. No utiliza servicios de backend del TP3.

La descripción de componentes, el diagrama y las fuentes están en [docs/tp7](docs/tp7/README.md).

## Desarrollo

Abrir el proyecto en Android Studio y sincronizar Gradle. Se requiere un JDK compatible con el complemento de Android y el SDK indicado en `app/build.gradle.kts`. Firebase usa la configuración Android del proyecto.

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

El APK de desarrollo se genera en `app/build/outputs/apk/debug/app-debug.apk`. La cámara, el reconocimiento de imágenes y el flujo completo con Firestore deben verificarse en Android; las pruebas locales no sustituyen esa comprobación.
