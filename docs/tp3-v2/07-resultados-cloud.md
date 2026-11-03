# Seguridad y metodología de ensayo

Las capturas originales se incluyen en el PDF y en el paquete local de evidencias.

Cada perfil repite estos controles con dos cuentas ficticias. La comprobación funcional crea diez movimientos y verifica ingresos 500, gastos 150, neto 350 y cantidad diez. Antes de la carga se eliminan nueve movimientos, de modo que cada resumen consulta uno. Los datos financieros reales no forman parte del ensayo.

Los cuatro perfiles requieren aproximadamente 38.000 lecturas más controles. Firebase utiliza Spark, con cuota compartida de 50.000 lecturas/día; el diseño del ensayo considera el consumo del proyecto. Esta carga no representa resúmenes de diez o mil movimientos.

Se revisan errores, limpieza, réplicas y costo entre niveles. El generador se detiene ante 429, diez errores consecutivos o diez fallos entre las últimas cien respuestas. DolarAPI y ArgentinaDatos no reciben carga masiva.

La consola de Firebase mostró 5,6 K lecturas en un intervalo que terminaba antes de alta/pico. Se conserva como lectura parcial con demora, no como total de los cuatro perfiles.

# Resultados de rendimiento

Las capturas originales se incluyen en el PDF y en el paquete local de evidencias.

Los totales programados y completados son 1.000, 5.000, 12.000 y 20.000. El máximo en curso es un límite del generador; se informa la concurrencia realmente observada. Solicitudes totales y usuarios simultáneos son magnitudes distintas.

Los percentiles abarcan desde el envío hasta leer y validar el JSON e incluirían las solicitudes fallidas, si existieran. La tasa efectiva utiliza la duración completa de la carga. Preparación, comprobaciones funcionales y limpieza quedan fuera de esa tasa.

## Interpretación

Se completaron 38.000 solicitudes con cero errores. El aumento de carga elevó los percentiles de latencia. El perfil pico alcanzó 127,64 solicitudes/s, un 79,8 % de las 160 solicitudes/s programadas; esta configuración no acredita sostener esa meta. La telemetría disponible no permite atribuir el límite a CPU, red o Firebase. Una sonda dentro de una réplica durante el final del pico puede agregar sobrecarga; su efecto no se descuenta de las mediciones.

Las mediciones abarcan dos sesiones de aprovisionamiento con la misma imagen y configuración. Los orígenes y tiempos originales permiten identificar cada ensayo; no se supone el mismo entorno físico para todos los perfiles.

La cantidad de solicitudes atendidas por cada instancia no se instrumentó. Las series de réplicas demuestran asignación y disponibilidad observadas; por sí solas no acreditan reparto de tráfico por instancia.

# Comportamiento del autoescalado

Series originales: [baseline](evidencias/azure-scale-baseline.json), [media](evidencias/azure-scale-media.json), [alta](evidencias/azure-scale-alta.json), [pico](evidencias/azure-scale-pico.json).

Puntos verdes: réplicas asignadas. Círculos naranjas: réplicas listas cuando difieren. Cada punto es una respuesta real de la API. El intervalo nominal de 5 s se amplía por la duración de cada consulta; no se completan espacios entre muestras.

Cada punto corresponde a una observación real; no se interpolan intervalos. Las mediciones abarcan dos sesiones de aprovisionamiento con la misma imagen, región, recursos por réplica y límite de escala. Los JSON identifican los orígenes y fechas. En baseline, la segunda réplica apareció después de finalizar la carga: su p95 no demuestra mejora por dos procesos.

# Métricas y observabilidad

Las capturas originales se incluyen en el PDF y en el paquete local de evidencias.

Las respuestas originales de Azure Monitor se conservan con su intervalo y dimensiones. Los valores cero o las series vacías no se reemplazan por los contadores del generador. La sonda directa cubre una réplica y no permite diagnosticar el cuello de botella del conjunto.

Las capturas originales se incluyen en el PDF y en el paquete local de evidencias.

Capturas originales: rango de réplicas y detalle de la regla HTTP. Las imágenes completas integran el paquete de evidencias.

# Costos y cierre operativo

El presupuesto mensual de USD 5 se configura antes de ACR y del entorno. Sus alertas alcanzan 50 %, 80 % y 100 % del importe. El límite general de la oferta estudiantil y el presupuesto son controles distintos: **el presupuesto no es un corte duro.**

Las capturas originales se incluyen en el PDF y en el paquete local de evidencias.

El presupuesto genera alertas y no detiene el consumo. La contabilización puede demorarse: un presupuesto con 0 USD y una consulta ActualCost sin filas no acreditan costo final cero. Las tarifas indicadas son referencias de cálculo, no una factura. Firebase utiliza el plan Spark.

## Cierre de la infraestructura de ensayo

La infraestructura se retiró después de respaldar las evidencias. La comprobación de Azure a las 2026-11-03T05:37:03Z confirmó siete recursos Terraform eliminados, grupo ausente, cero recursos propios y presupuesto propio ausente. Firebase se mantiene; las cuentas y los movimientos ficticios se limpiaron en cada perfil.

Los recursos se retiraron después de observar el retorno a cero y de conservar los resultados. El origen HTTPS registrado identifica el servicio utilizado durante las pruebas, no un endpoint que permanezca activo.

## Trazabilidad de los ensayos

- [baseline](evidencias/cloud-validation-baseline.json): fecha UTC `2026-11-03T04:10:44.750Z`, commit `66562605b5810fb78c3bcf1cb102d9622ca79a05`, SHA-256 del verificador `b01b8d0e006451265e08b11dd81a75613c019d9a3d0a65b8cefada5d8c892fdb`.
- [media](evidencias/cloud-validation-media.json): fecha UTC `2026-11-03T04:58:24.548Z`, commit `bb566ada9f1b5f076ef48f5e18ae50879f75ad75`, SHA-256 del verificador `b01b8d0e006451265e08b11dd81a75613c019d9a3d0a65b8cefada5d8c892fdb`.
- [alta](evidencias/cloud-validation-alta.json): fecha UTC `2026-11-03T05:07:26.313Z`, commit `bb566ada9f1b5f076ef48f5e18ae50879f75ad75`, SHA-256 del verificador `b01b8d0e006451265e08b11dd81a75613c019d9a3d0a65b8cefada5d8c892fdb`.
- [pico](evidencias/cloud-validation-pico.json): fecha UTC `2026-11-03T05:17:17.207Z`, commit `36a4a8efcec8b3003638259ba235219fb3169541`, SHA-256 del verificador `f28792ab2adb5e7e73c9ba7d8975af5af5a59b9951aab587b55bebdda794d1ea`.

[Retiro verificado](evidencias/retirement-verification.json).

Las consultas de Azure Monitor no aportaron CPU ni memoria verificables. La sonda directa solo cubre una réplica y una ventana breve del pico. El costo final contabilizado no está disponible. La carga representa un movimiento por resumen y no acredita meses de mayor volumen. No se comprobó redundancia zonal o regional ni distribución de solicitudes por instancia. El ensayo no incluye una prueba manual de Android u OCR en dispositivo.
