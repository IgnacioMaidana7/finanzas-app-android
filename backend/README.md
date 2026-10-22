# API de resumen financiero

Node 22, Firebase Admin SDK y HTTP nativo. Contenedor sin privilegios y sin claves
de cuentas de servicio. En Cloud Run usa Application Default Credentials de la
cuenta de ejecución dedicada. `FIREBASE_PROJECT_ID` es obligatorio y fija el
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

`verifyIdToken(token, true)` valida firma, audiencia, emisor, expiración y consulta
revocación/deshabilitación. Las reglas de Firestore protegen al cliente móvil;
el Admin SDK las omite y depende de IAM y de la autorización implementada en la
API. La cuenta de ejecución tiene sólo cuatro permisos de lectura, definidos
en `infra/main.tf`. IAM permite leer datos del proyecto; no impone aislamiento
por UID. Ese aislamiento lo implementa y prueba la aplicación.

Fuentes: [validación de ID tokens](https://firebase.google.com/docs/auth/admin/verify-id-tokens),
[reglas y acceso mediante servidor](https://firebase.google.com/docs/firestore/security/rules-conditions).

## Ejecución y despliegue

```powershell
npm ci
npm test
npm run load:local
```

La carga local arranca un servidor efímero con autenticación y repositorio sintéticos.
No existe modo demo ni bypass de autenticación en el ejecutable de producción.
Las variables de emulador están prohibidas al detectar Cloud Run (`K_SERVICE`).

Para iniciar contra un proyecto real se necesitan ADC y permisos explícitos;
no hacerlo como parte de las pruebas. Ver el [procedimiento cloud](../docs/tp3/02-configuracion-cloud.md).
El Dockerfile sólo copia dependencias de producción y `src/`; no incluye pruebas,
configuración Firebase Android ni archivos personales. Fijar también el digest
de la imagen base tras la revisión de vulnerabilidades previa al despliegue.
