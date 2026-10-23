# API de resumen financiero

Node 22, Firebase Admin SDK y HTTP nativo. Contenedor sin privilegios y sin claves
de cuentas de servicio. En Azure usa la sesión Firebase de cada solicitante;
la alternativa Cloud Run usa Application Default Credentials de una cuenta
de ejecución dedicada. `FIREBASE_PROJECT_ID` es obligatorio y fija el
proyecto esperado para los tokens.

## Contrato

`GET /v1/summary?month=2026-10`

Encabezado `Authorization: Bearer <Firebase ID token>` obtenido con
`FirebaseUser.getIdToken(...)`. No usar custom tokens ni tokens de Google Cloud.

```json
{
  "month": "2026-10",
  "timeZone": "UTC",
  "unit": "ARS_INTEGER",
  "income": "10000",
  "expense": "3000",
  "net": "7000",
  "transactionCount": 2
}
```

Los importes son cadenas decimales exactas para preservar los `Long` de Kotlin.
Corresponden a pesos enteros según el modelo del TP2, sin conversión monetaria.
El rango es `[inicio del mes UTC, inicio del siguiente mes UTC)`; no coincide
necesariamente con el mes en la zona local del teléfono.

El UID proviene sólo del token verificado. La consulta está acotada a
`users/{uid}/transactions`, rango de `date`, proyección `amount/type/date` y
1.001 documentos. Hasta 1.000 movimientos devuelve totales; 1.001 indica que
se superó el límite y devuelve 422, sin totales parciales. No modifica Firestore.
Usa índice simple por `date`, ya necesario en el TP2; confirmar que esté activo
en el proyecto real. No desplegar un archivo vacío de índices sobre índices ajenos.

| HTTP | Casos |
|---|---|
| 200 | Resumen, incluido mes sin movimientos |
| 400 | Mes inválido, duplicado, parámetros extra (incluido UID) o cuerpo |
| 401 | Sesión ausente, inválida, vencida, revocada o usuario deshabilitado |
| 404 / 405 | Ruta inexistente / método distinto de GET |
| 422 | Límite, datos inválidos o desborde de Long |
| 503 | Dependencia Auth/Firestore no disponible o configuración insuficiente |

`GET /healthz` es público y sólo indica que el proceso responde. No comprueba
conectividad con Firestore. La API usa `Cache-Control: no-store`; los logs propios
sólo contienen código, estado y duración. No registrar tokens, UIDs ni movimientos.

## Autenticación y permisos

**Azure, `FIREBASE_DATA_ACCESS=user-rest`:** el SDK oficial valida firma,
audiencia, emisor y expiración con el proyecto fijado, sin ADC. Luego
`accounts:lookup` verifica el estado del propio usuario con ese ID token y la
clave cliente Firebase (`FIREBASE_WEB_API_KEY`, ya distribuida en Android).
Se rechazan usuarios deshabilitados y sesiones con `auth_time < validSince`.
Si no puede comprobar el estado o falta `validSince`, falla de forma cerrada.
Cada consulta REST a Firestore lleva el ID token del mismo solicitante y aplica
Security Rules, además del aislamiento explícito por UID en la API. No se guarda
el token entre solicitudes. Respuestas externas limitadas a 2 MiB y 8 s por llamada.
No hay clave administrativa Google en Azure ni permisos IAM Google nuevos.
Revisar las restricciones de la clave cliente antes del despliegue; no quitarlas
automáticamente si la llamada falla.

**Alternativa Cloud Run, `FIREBASE_DATA_ACCESS=admin` (predeterminado):**
`verifyIdToken(token, true)` valida firma, audiencia, emisor, expiración y consulta
revocación/deshabilitación. Las reglas de Firestore protegen al cliente móvil;
el Admin SDK las omite y depende de IAM y de la autorización implementada en la
API. La cuenta de ejecución tiene sólo cuatro permisos de lectura, definidos
en `infra/main.tf`. IAM permite leer datos del proyecto; no impone aislamiento
por UID. Ese aislamiento lo implementa y prueba la aplicación.

Fuentes: [validación de ID tokens](https://firebase.google.com/docs/auth/admin/verify-id-tokens),
[reglas y acceso mediante servidor](https://firebase.google.com/docs/firestore/security/rules-conditions),
[Firestore REST con ID token](https://firebase.google.com/docs/firestore/use-rest-api),
[consulta de la propia sesión](https://docs.cloud.google.com/identity-platform/docs/reference/rest/v1/accounts/lookup).

## Ejecución y despliegue

```powershell
npm ci
npm test
npm run load:local
```

La carga local arranca un servidor efímero con autenticación y repositorio sintéticos.
No existe modo demo ni bypass de autenticación en el ejecutable de producción.
Las variables de emulador están prohibidas en Cloud Run (`K_SERVICE`) y Azure
(`CONTAINER_APP_NAME`). El adaptador REST sólo admite emuladores en el proyecto
aislado `demo-finanzas-tp3` y puertos locales exactos de la suite.

Las pruebas no usan el proyecto real. Ver el [procedimiento Azure](../docs/tp3/azure/02-configuracion-cloud.md)
y la [alternativa Cloud Run](../docs/tp3/02-configuracion-cloud.md).
El Dockerfile sólo copia dependencias de producción y `src/`; no incluye pruebas,
configuración Firebase Android ni archivos personales. La imagen base Node está
fijada por digest. La auditoría npm de producción pasó; queda pendiente un análisis
completo de vulnerabilidades del sistema operativo de la imagen.
