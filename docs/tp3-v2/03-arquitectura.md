# TP3 renovado — Entregable 3: Diagrama de arquitectura

El diagrama editable está en [arquitectura.mmd](arquitectura.mmd). Representa los componentes **propuestos y versionados**; la infraestructura Azure de la nueva versión está pendiente de despliegue, no debe marcarse como activa.

## Flujo financiero

1. Android se autentica con Firebase Auth.
2. El usuario solicita el resumen de un mes y Android remite su ID token Firebase por HTTPS a Azure.
3. El ingreso HTTP administrado envía la solicitud a una réplica disponible; el escalador ajusta el número de réplicas según demanda.
4. La API verifica token, consulta el estado de sesión y lee **solo** los movimientos del UID autenticado mediante Firestore REST y Security Rules.
5. Android recibe únicamente totales y cantidad de movimientos y los muestra en `CloudSummaryCard`.

## Flujos propios del TP7

- Las cotizaciones e inflación se solicitan desde Android a DolarAPI y ArgentinaDatos y pueden guardarse temporalmente en caché local, según `CachedIndicatorRepository`.
- El OCR con Google ML Kit se ejecuta en el dispositivo, genera un borrador editable, y solo tras confirmación explícita puede guardarse el gasto mediante el repositorio existente de Firestore.
- Firebase Auth y Firestore siguen siendo la base de usuarios y persistencia; el backend no reemplaza las integraciones móviles existentes.

## Balanceo, redundancia y restricciones

El control de réplicas y el ingreso HTTPS son responsabilidad de Container Apps. El dibujo muestra **capacidad potencial**, no réplicas permanentemente activas. Con mínimo cero hay arranque en frío. Con máximo 2 a 6, el límite es de capacidad de cómputo, no una garantía de disponibilidad zonal o regional. Firestore y los servicios terceros no se vuelven más escalables solo por agregar procesos Node.js.

## Verificación

Una vez desplegada la nueva revisión, actualizar el diagrama con región, versión, máximo aprobado y estado efectivo; acompañarlo con capturas de configuración y evidencia temporal de autoescalado. Mantener separados arquitectura **diseñada** y arquitectura **verificada**.
