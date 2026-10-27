# Validación del TP7

Verificaciones realizadas el 27 de octubre de 2026 sobre la implementación de la rama `tp7/integraciones-apis-sdk`.

| Verificación | Resultado |
| --- | --- |
| Pruebas unitarias y de regresión | 175 ejecutadas, sin fallas ni errores |
| Compilación de desarrollo | APK debug generado correctamente |
| Revisión estática de Android | Sin errores bloqueantes; advertencias registradas en el informe de lint |
| Consulta HTTPS de DolarAPI | HTTP 200; siete cotizaciones |
| Consulta HTTPS de ArgentinaDatos | HTTP 200; 1003 registros mensuales |
| Decodificación de respuestas reales | Ambas respuestas aceptadas por `IndicatorJson`, el mismo decodificador utilizado en producción |

Las consultas reales y la decodificación se verificaron desde el entorno de desarrollo. No representan una prueba completa de interfaz o de red en un teléfono.

Las pruebas nuevas comprueban precisión decimal, selección de la cotización de venta, acumulación de inflación, cobertura mensual, contratos JSON, caché, concurrencia, fallos y cancelación. Para tickets cubren selección de total frente a subtotal y vuelto, ambigüedad, fechas, moneda y centavos. También se comprueban cambio de usuario, rechazo de fracciones sin confirmación, identificadores estables en reintentos y ausencia de duplicación ante confirmaciones repetidas.

Se corrigieron dos pruebas previas de categorías que combinaban movimientos con la fecha del día y presupuestos fijos de septiembre de 2026. Sus movimientos ahora usan una fecha del mismo mes que el presupuesto; la lógica productiva de categorías se conserva.

## Verificaciones pendientes en Android

No se ejecutaron cámara, selector de imágenes, reconocimiento nativo de fotos reales ni escritura completa contra Firestore desde un dispositivo. El intento de iniciar un emulador se detuvo por espacio insuficiente para crear su partición de datos.

Antes de considerar validada la experiencia completa deben recorrerse en Android la captura y selección, cancelación, revisión del OCR, elección de cuenta y categoría, confirmación de un gasto, funcionamiento sin red, cambio de sesión, orientación, tamaño de fuente grande y lector de pantalla. Tampoco se realizaron ensayos de utilidad con participantes.

Los casos pendientes se distinguen de las pruebas automatizadas realizadas; no se presentan como aprobados.
