# Metodología de evaluación de escalabilidad

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

Las consultas de Azure Monitor no aportaron CPU ni memoria verificables. La sonda directa solo cubre una réplica y una ventana breve del pico. El costo final contabilizado no está disponible. La carga representa un movimiento por resumen y no acredita meses de mayor volumen. No se comprobó redundancia zonal o regional ni distribución de solicitudes por instancia. El ensayo no incluye una prueba manual de Android u OCR en dispositivo.
