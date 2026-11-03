# Entregable 2 - Configuración cloud y evidencias reales

**Ejecución v2: 03/11/2026. Recursos retirados y verificados.** Esto documenta un despliegue que existió y fue probado; no un servicio permanente ni el cierre completo de todos los ensayos previstos.

## Configuración aplicada

| Elemento | Valor observado |
|---|---|
| Grupo | rg-tp3-finanzas |
| Entorno | cae-tp3-finanzas, Chile Central; interfaz Express/Rápido |
| Aplicación | ca-tp3-finanzas-summary |
| Registro | tp3finanzasv2im20261102, Basic, Brazil South; administración por claves deshabilitada |
| Identidad | id-tp3-summary; AcrPull sobre ese registro |
| Recursos por réplica | 0,25 vCPU / 0,5 GiB |
| Ingreso | HTTPS obligatorio, puerto interno 8080, sonda /healthz |
| Escala | Mínimo 0 y máximo 2, regla http-demand / 16; interfaz mostró 300 s y 30 s |
| Presupuesto | USD 5 mensuales, filtro de grupo, alertas 50/80/100 % al Propietario; no es corte duro |

Backend de la imagen: `8e5aeedfac0d35527e2c9bfb2d38a19123a14d30`. Verificador del ensayo: `66562605b5810fb78c3bcf1cb102d9622ca79a05` y hash en JSON. Digest desplegado:

`sha256:b4e785288de3580f721699fde89e6692f0726e1a694f511c006458c8b121ecbd`

El contenedor actual se exportó en [41574591148](https://github.com/IgnacioMaidana7/finanzas-app-android/actions/runs/41574591148), se verificaron los hashes ZIP/TAR y se transfirió al ACR. La compilación no se presenta como medición cloud. No se usó la imagen histórica del 22/10.

Origen durante la prueba: `https://ca-tp3-finanzas-summary.gentlerock-685a5a8a.chilecentral.azurecontainerapps.io`.

## Evidencia cloud

| Control | Resultado |
|---|---|
| HTTPS /healthz | 200, validación TLS normal |
| Sin token | 401 |
| HTTP | 301 a HTTPS; nunca se enviaron tokens por HTTP |
| Resumen de diez movimientos | 500 ingresos, 150 gastos, 350 neto; 200 |
| Otro usuario | Mes vacío; acceso cruzado Firestore 403 |
| UID por parámetro | 400 |
| Sesión revocada / nueva | 401 / 200 |
| Limpieza | Dos cuentas y fixtures ficticios eliminados; cero errores reportados |
| Carga baseline | 1.000 completadas, cero errores, 24,56 req/s, máximo en curso 24, p50/p95/p99 749/783/887 ms |
| Escalado | 43 observaciones, 0 -> 1 -> 2 -> 0; máximo listo 2 |

**Tamaño de carga:** un movimiento por resumen después de la prueba funcional de diez. Firebase Spark tiene 50.000 lecturas/día compartidas; los cuatro perfiles completos requerirían unas 38.000 más controles. No se habilitó Blaze.

## Límites y pendientes

- Media (5.000/64/60), alta (12.000/128/100) y pico (20.000/256/160): **no ejecutados; autorización adicional pendiente**. No hay resultados inventados.
- La segunda réplica apareció a las 04:12:06 UTC. El verificador ya había guardado el resultado y limpieza a las 04:11:41 UTC. El ensayo prueba reacción de escala, pero no rendimiento sostenido de dos réplicas ni balanceo por instancia.
- Azure Monitor devolvió CPU/memoria sin valores y réplicas/solicitudes en cero, también en el portal. No se usaron esos ceros para negar los datos directos ni se rellenaron muestras.
- No se aumentó el máximo a seis. La cuota regional de entornos se verificó; la API de cuotas del entorno devolvió lista vacía y no acredita seis réplicas.
- Primera respuesta desde cero: 3.956 ms, incluida red/TLS/arranque; no tiempo puro de inicio del proceso.
- Costo visible de presupuesto USD 0 antes del retiro: **no es costo final facturado**. Se conservaron las tarifas y evidencias locales; hay demora de contabilización.
- Sin dispositivo Android conectado: prueba manual de OCR y tarjeta no acreditada en esta ejecución.

## Retiro y fuentes

Retiro autorizado por el usuario; estado: **retirados y verificados**, verificación UTC: 2026-11-03T04:31:04.5955416Z. Firebase habitual se conserva. Ver [resultados y archivos](07-resultados-cloud.md).

Las evidencias técnicas están en `evidencias/`. Capturas de cuenta/crédito, estado y planes completos quedan fuera de este repositorio público. El informe PDF local incluye las capturas originales disponibles y explicita los pendientes. **No se declara cerrado todo el TP3 por haber pasado un perfil.**
