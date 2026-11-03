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

Android envía mes y token Firebase por HTTPS. Node verifica JWT y vigencia, deriva UID del token y consulta Firestore REST bajo Security Rules. DolarAPI/ArgentinaDatos quedan en Android con caché y ML Kit es local.

La linea base pertenece al primer despliegue; los tres niveles siguientes al segundo, con la misma imagen, region, CPU/memoria y limite de replicas. No son cuatro ensayos sobre el mismo entorno fisico. La segunda replica del baseline aparecio despues de terminar su carga, por lo que no se atribuye ese p95 a dos procesos. Las series no se interpolan.

Recursos retirados y verificados. Retiro verificado en `2026-11-03T05:37:03.5891270Z`. [Resultados y límites](07-resultados-cloud.md).
