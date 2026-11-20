# TP6 Notificaciones push

FinanzasApp recibe campañas remotas mediante Firebase Cloud Messaging. Desde la campana en Inicio se puede conceder el permiso de Android y administrar el canal Recordatorios.

## Configuración de una campaña

Usar el registro Android `com.maidana.finanzasapp` del proyecto Firebase asociado al APK. En Firebase Messaging, crear una notificación con título y cuerpo, elegir la audiencia de ensayo y agregar el dato personalizado `tp6_campaign`.

| Valor de `tp6_campaign` | Destino |
| --- | --- |
| `registro_movimientos` | Nuevo movimiento |
| `revision_metas` | Metas de ahorro |

Usar texto general para que el aviso no revele información de la cuenta en la bandeja de Android. Si la sesión está cerrada, la app guarda el destino hasta que el usuario inicie sesión. Desde un destino válido se registra el evento `tp6_push_open` con el tipo de campaña.

## Pruebas

Compilar y comprobar el cliente:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

La prueba instrumentada `RetentionNotificationsTest` comprueba la creación del canal. Para realizar además el registro y la suscripción de prueba, instalar ambos APK en un emulador con Google Play y ejecutar la prueba con un tema único cuyo nombre empiece por `tp6-prueba-`:

```powershell
adb shell am instrument -w -e tp6TestTopic tp6-prueba-mi-ensayo -e class com.maidana.finanzasapp.RetentionNotificationsTest com.maidana.finanzasapp.test/androidx.test.runner.AndroidJUnitRunner
```

En la consola, usar ese tema como audiencia de las dos campañas. Probar avisos con la app abierta y en segundo plano, y la navegación con y sin sesión.
