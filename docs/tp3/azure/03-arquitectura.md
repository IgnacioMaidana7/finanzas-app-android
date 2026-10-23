# TP3 · Arquitectura actualizada

Arquitectura Azure + Firebase desplegada y verificada, 22/10/2026 (Argentina).

![Diagrama Azure y Firebase](arquitectura.png)

```mermaid
flowchart LR
  A[Android Kotlin / Compose] -->|Inicio de sesión| F[Firebase Auth existente]
  F -->|ID token| A
  A -->|HTTPS · ID token + mes| I
  subgraph Z[Azure Container Apps Express · Chile Central]
    I[Ingress HTTPS administrado / balanceo] --> S[Escalador HTTP · umbral 16 · mínimo 0 / máximo 2]
    S --> R1[API Node · réplica 1 · sin estado]
    S --> R2[API Node · réplica 2 bajo demanda]
    R1 --> V[Firma / proyecto / expiración · estado y revocación · UID verificado]
    R2 --> V
  end
  V -->|ID token del solicitante · accounts:lookup| F
  V -->|ID token + Security Rules · consulta users/UID/transactions| D[Firestore existente · Santiago regional]
  A -->|Operaciones TP2 · ID token + Security Rules| D
  C[ACR privado Basic · Brazil South · imagen por digest] -.->|Identidad administrada · AcrPull| R1
  C -.->|Misma imagen| R2
  T[Terraform · pruebas sintéticas · métricas] -.-> Z
```

Cada réplica tiene 0,25 vCPU / 0,5 GiB. El umbral HTTP es una señal de escalado,
no un límite de conexiones. Se observó crecimiento automático de una a dos
réplicas, ambas listas, y posterior reducción a cero; el diagrama no implica dos instancias permanentes.
Se acepta arranque en frío con mínimo
cero. No se configura redundancia entre zonas/regiones. El registro tiene costo
independiente de la cantidad de réplicas.

El backend no usa una cuenta administrativa Firebase: verifica la sesión y
consulta Firestore con el mismo ID token del usuario. La identidad Azure se
limita a descargar imágenes privadas; no accede a datos Google. Las operaciones
del TP2 permanecen en Firebase. El resumen agrega enteros exactos de un mes UTC y
no persiste datos, sesiones ni resultados financieros.

Fuente editable: este Mermaid y `scripts/render-architecture.py --azure --deployed` para PNG.
La variante Cloud Run previa se conserva en `docs/tp3/03-arquitectura.md`.

![Autoescalado observado mediante la API de Azure](autoescalado-observado.png)

La curva usa [muestras reales](../evidence/azure-scale-observed.json); las réplicas
asignadas y las listas pueden diferir durante el arranque. La carga autenticada
fue de 1.000 consultas sintéticas sin errores (p95 883 ms). El ingress reparte
solicitudes entre réplicas saludables; esta curva por sí sola no mide cuántas
solicitudes atendió cada réplica.
