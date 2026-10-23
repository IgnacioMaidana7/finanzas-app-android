locals {
  tags = { purpose = "tp3", app = "finanzas" }
}

resource "azurerm_resource_group" "tp3" {
  name     = "rg-tp3-finanzas"
  location = var.location
  tags     = local.tags
  lifecycle {
    precondition {
      condition     = var.approve_credit_usage
      error_message = "Falta aprobar el consumo de crédito, región, registro privado y permisos."
    }
  }
}

resource "azurerm_container_registry" "images" {
  name                = var.registry_name
  resource_group_name = azurerm_resource_group.tp3.name
  location            = var.registry_location
  sku                 = "Basic"
  admin_enabled       = false
  tags                = local.tags
}

resource "azurerm_user_assigned_identity" "runtime" {
  name                = "id-tp3-summary"
  resource_group_name = azurerm_resource_group.tp3.name
  location            = var.location
  tags                = local.tags
}

resource "azurerm_role_assignment" "pull" {
  scope                = azurerm_container_registry.images.id
  role_definition_name = "AcrPull"
  principal_id         = azurerm_user_assigned_identity.runtime.principal_id
}

resource "azurerm_container_app_environment" "tp3" {
  name                = "cae-tp3-finanzas"
  resource_group_name = azurerm_resource_group.tp3.name
  location            = var.location
  workload_profile {
    name                  = "Consumption"
    workload_profile_type = "Consumption"
  }
  tags = local.tags
}

resource "azurerm_container_app" "summary" {
  count                        = var.deploy_app ? 1 : 0
  name                         = "ca-tp3-finanzas-summary"
  resource_group_name          = azurerm_resource_group.tp3.name
  container_app_environment_id = azurerm_container_app_environment.tp3.id
  revision_mode                = "Single"
  workload_profile_name        = "Consumption"
  tags                         = local.tags
  identity {
    type         = "UserAssigned"
    identity_ids = [azurerm_user_assigned_identity.runtime.id]
  }
  registry {
    server   = azurerm_container_registry.images.login_server
    identity = azurerm_user_assigned_identity.runtime.id
  }
  ingress {
    external_enabled           = true
    allow_insecure_connections = false
    target_port                = 8080
    transport                  = "http"
    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }
  template {
    min_replicas = 0
    max_replicas = 2
    http_scale_rule {
      name                = "http-demand"
      concurrent_requests = "16"
    }
    container {
      name   = "summary"
      image  = var.image
      cpu    = 0.25
      memory = "0.5Gi"
      env {
        name  = "FIREBASE_PROJECT_ID"
        value = var.firebase_project_id
      }
      env {
        name  = "FIREBASE_DATA_ACCESS"
        value = "user-rest"
      }
      env {
        name  = "FIREBASE_WEB_API_KEY"
        value = var.firebase_client_api_key
      }
      env {
        name  = "PORT"
        value = "8080"
      }
      readiness_probe {
        transport = "HTTP"
        port      = 8080
        path      = "/healthz"
      }
    }
  }
  lifecycle {
    precondition {
      condition     = var.firebase_client_api_key != ""
      error_message = "Se necesita la clave cliente de Firebase para verificar sesiones; no usar claves administrativas."
    }
    precondition {
      condition     = startswith(var.image, "${azurerm_container_registry.images.login_server}/")
      error_message = "La imagen debe pertenecer al registro privado previsto."
    }
  }
  depends_on = [azurerm_role_assignment.pull]
}
