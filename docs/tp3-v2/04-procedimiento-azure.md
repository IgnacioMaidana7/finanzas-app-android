# Procedimiento de despliegue y evaluación en Azure

## Preparación

Comprobar suscripción activa, rol suficiente, crédito, límite de gasto, proveedores y cuotas de Azure Container Apps y ACR. Definir región, recursos y presupuesto antes de consumir crédito. La configuración evaluada utiliza máximo 2 réplicas; cualquier ampliación requiere autorización y revisión de cuotas y costos.

Firebase utiliza Spark. Revisar consumo y cuota antes de generar carga. Las cuentas y movimientos de ensayo deben ser ficticios, identificables y eliminables. La carga apunta al backend propio; no se aplica a DolarAPI ni ArgentinaDatos.

## Aprovisionamiento

Utilizar Terraform de `infra/azure`. Inicializar el backend local con una ruta privada fuera del repositorio y de OneDrive. Crear primero el presupuesto USD 5 y la infraestructura; publicar la imagen inmutable en ACR privado y aplicar la configuración de la API HTTPS. Comprobar que el plan contiene exclusivamente los recursos del proyecto antes de aplicarlo.

Mantener credenciales fuera de comandos publicados, documentación, estado compartido y capturas. La identidad de ejecución recibe AcrPull solo sobre el ACR. Validar HTTPS, `/healthz`, rechazo de solicitudes sin token y Security Rules antes de cargar el servicio.

## Medición progresiva

Ejecutar `backend/scripts/verify-cloud.mjs` con un único perfil por vez: baseline 1.000/32/25 RPS, media 5.000/64/60 RPS, alta 12.000/128/100 RPS y pico 20.000/256/160 RPS. Los valores representan total, máximo en curso y ritmo programado. Habilitar únicamente el nivel autorizado y conservar su JSON antes de continuar.

Medir réplicas asignadas/listas antes, durante y después mediante `scripts/Measure-CloudScale.ps1`. Revisar errores, limpieza, costos y límite de réplicas entre niveles. Detener el ensayo ante fallos sostenidos o riesgo de cuotas. Registrar RPS efectivos y concurrencia observada; no sustituirlos por metas del generador.

Comprobar el registro de Microsoft.Insights para consultar Azure Monitor. Conservar las respuestas originales incluso cuando no aporten datos. Las sondas directas deben identificar su alcance, intervalo y posible sobrecarga; no se presentan como métricas de plataforma.

## Cierre y evidencias

Respaldar resultados, capturas, configuración sin secretos y hashes. Limpiar cuentas y movimientos ficticios. Cuando el retiro esté autorizado, revisar el plan de eliminación y confirmar que no afecta recursos ajenos; luego verificar ausencia de grupo, recursos propios y presupuesto. Conservar Firebase y sus datos habituales.

El presupuesto es una alerta, no un corte duro. No interpretar una consulta de costos sin filas como costo final cero. Los resultados medidos y sus límites están en [07-resultados-cloud.md](07-resultados-cloud.md).
