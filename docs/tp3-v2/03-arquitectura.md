# Entregable 3 - Arquitectura actualizada

FinanzasApp TP3 v2 sobre la base TP7. Despliegue real del 03/11/2026, recursos **retirados y verificados**. Las dos réplicas son procesos bajo demanda: se observaron 0, 1 y 2 por API, no dos procesos permanentes. El portal identificó el entorno como Express/Rápido.

```mermaid
flowchart TB
  subgraph PHONE["Android / FinanzasApp TP7"]
    APP["Kotlin / Jetpack Compose / MVVM"]
    CACHE["Indicadores con caché local"]
    OCR["Google ML Kit / OCR local"]
  end
  subgraph CLOUD["Azure Container Apps / Chile Central / despliegue real 03-11-2026"]
    INGRESS["Ingreso HTTPS y balanceo administrado"]
    SCALE["Escalado HTTP / regla 16 / mínimo 0 / máximo 2"]
    R1["Réplica Node.js sin estado 1"]
    R2["Réplica Node.js sin estado 2"]
    INGRESS --> R1
    INGRESS --> R2
    SCALE -.-> R1
    SCALE -.-> R2
  end
  ACR["ACR Basic privado / Brazil South / digest SHA-256"]
  ID["Identidad administrada / AcrPull"]
  AUTH["Firebase Authentication / validación y revocación"]
  DB[("Firestore / Security Rules por UID / Spark")]
  DOLAR["DolarAPI / cotizaciones"]
  INFL["ArgentinaDatos / inflación"]
  APP -->|"Inicio de sesión"| AUTH
  APP -->|"HTTPS + Firebase ID token"| INGRESS
  R1 & R2 -->|"Verificar JWT y sesión"| AUTH
  R1 & R2 -->|"REST con token del usuario"| DB
  APP <-->|"Operaciones habituales"| DB
  APP --> CACHE
  CACHE -->|"HTTPS acotado; sin carga masiva"| DOLAR
  CACHE -->|"HTTPS acotado; sin carga masiva"| INFL
  APP --> OCR
  OCR -->|"Borrador editable y confirmación"| APP
  ID -->|"Lectura de imagen"| ACR
  ACR -.-> R1
  ACR -.-> R2
```

## Recorrido y alcance real

Android conserva Firebase Authentication, persistencia Firestore y las integraciones TP7. Para el resumen envía por HTTPS un ID token y mes. Node.js verifica JWT y vigencia de sesión, deriva el UID del token y consulta Firestore REST con ese mismo token bajo Security Rules. No utiliza credenciales administrativas Google en Azure.

El ingreso/balanceo y escalado son administrados por Azure Container Apps. La regla HTTP declarada tiene umbral 16 y rango 0-2. Se observó 0 -> 1 -> 2 -> 0, con la segunda réplica posterior a terminar la carga inicial. No se acreditó balanceo por instancia ni mejora de latencia con dos réplicas.

DolarAPI y ArgentinaDatos se consultan desde Android con caché y no recibieron carga masiva. ML Kit procesa OCR local, propone un borrador editable y requiere confirmación para guardar.

No se implementó redundancia zonal/regional ni se amplió a seis réplicas. Las cuotas Firebase no aumentan al escalar Azure. [Resultados reales y límites](07-resultados-cloud.md).
