# TP3 renovado — Entregable 1: Estrategia de escalabilidad

**Materia:** Arquitecturas Móviles · **Aplicación:** FinanzasApp Android · **Base funcional:** TP7 · **Estado:** desarrollo y pruebas locales pendientes de CI; ensayos cloud v2 aún no ejecutados.

## 1. Objetivo y contexto

La guía de trabajos prácticos (TP3, p. 3) pide diseñar una estrategia para escalar una aplicación móvil, considerando **balanceo de carga, redundancia y autoescalado**. Esta versión integra las funcionalidades existentes del TP7 (guía p. 10): Firebase Authentication y Cloud Firestore; cotizaciones en DolarAPI; inflación en ArgentinaDatos; y reconocimiento local de tickets mediante Google ML Kit. Las integraciones del TP7 no modifican los tres entregables exigidos para el TP3.

## 2. Alcance a escalar

La aplicación Android conserva sus operaciones directas con Firebase. Un backend Node.js 22 de resumen mensual se encarga de comprobar la sesión, consultar exclusivamente movimientos del usuario autenticado en Firestore y devolver ingresos, gastos, saldo neto y cantidad. La consulta limita el volumen a 1.000 movimientos por mes y rechaza totales parciales; no almacena sesión ni resultados en memoria compartida.

Las otras integraciones tienen comportamientos distintos:
- **DolarAPI y ArgentinaDatos:** consultas HTTPS desde Android con caché y reintentos acotados; su escalabilidad depende de terceros y NO son objetivos de carga masiva.
- **ML Kit:** OCR ejecutado localmente en Android; su desempeño se estudia con pruebas en dispositivos, no aumentando réplicas Azure.
- **Firebase Auth/Firestore:** dependencias compartidas; su tasa de operaciones, latencia, índices y cuotas pueden transformarse en cuello de botella incluso si se agregan instancias.

## 3. Escalado horizontal, balanceo y redundancia

Se utiliza Azure Container Apps, perfil Consumption, ingreso HTTPS administrado y una regla HTTP (umbral 16) para aumentar o reducir el número de réplicas. Se mantiene `min_replicas=0` para ahorrar cómputo sin tráfico y se parametriza `max_replicas`: **2 por defecto**, opción experimental de hasta **6** únicamente tras revisión de cuotas, capacidad y presupuesto.

El umbral HTTP orienta el escalador pero **no garantiza 16 solicitudes simultáneas como límite por réplica**. Las réplicas son sin estado para que cualquiera pueda responder; esto facilita balanceo y reemplazo de procesos. Un mínimo de cero implica arranque en frío; dos réplicas no prueban por sí solas continuidad ante fallo regional o de zona. La base Firestore continúa fuera de Azure.

## 4. Seguridad y privacidad

La API comprueba el ID token de Firebase y obtiene el UID del token verificado, nunca del parámetro enviado por la app. En Azure el token del usuario se utiliza en Firestore REST para conservar Security Rules. No se incorporan credenciales administrativas Google al contenedor ni se guardan tokens, UID o importes en los logs. Los ensayos usan **solo cuentas y transacciones sintéticas del propio proyecto**.

## 5. Comparación de capacidad

Comparar:
1. Línea base histórica de octubre 2026: 1.000 consultas, 25 solicitudes/s programadas, 32 máximo en curso, cero errores, p95=883 ms y réplicas 1→2→0. Estas mediciones corresponden a un **despliegue anterior retirado**; no son resultados de esta versión.
2. Carga actual progresiva sobre la base TP7: baseline (1.000/32), media (5.000/64), alta (12.000/128) y pico (20.000/256). Son parámetros del generador, no cargas realmente alcanzadas hasta que se ejecuten y midan.
3. De ser viable, repetir la misma carga con máximo 2 vs. un máximo mayor de réplicas; variar **solo un factor** y controlar tráfico, datos sintéticos, región y configuración del contenedor.

Métricas: RPS programados y reales, solicitudes simultáneas observadas, latencia p50/p95/p99, errores por HTTP, réplicas asignadas/listas en una misma escala temporal, CPU/memoria, número de lecturas Firestore, costos y arranque en frío. Se detiene la prueba ante fallos sostenidos o presupuesto/ cuotas en riesgo.

## 6. Interpretación y límites

El aumento de instancias sin mejoras en latencia puede señalar cuello de botella en Firebase o en red, no necesariamente en CPU. Evitar presentar el total de solicitudes como sinónimo de simultaneidad. Evaluar caché, consultas agregadas y resúmenes precomputados antes de seguir elevando el número de réplicas.

**Pendiente**: agregar aquí los resultados reales de la nueva ejecución (tablas y gráficos con fecha, perfil y configuración), con comparaciones medibles y conclusiones; no anticipar valores.

**Referencias:** Guía de TP3 (p. 3), TP7 (p. 10); *Lean Mobile App Development*, capítulo 16; documentación oficial Azure Container Apps https://learn.microsoft.com/azure/container-apps/scale-app; repositorio `docs/tp7/README.md`.
