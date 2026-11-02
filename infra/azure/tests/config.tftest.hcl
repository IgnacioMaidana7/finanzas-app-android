mock_provider "azurerm" {
  mock_resource "azurerm_container_registry" {
    defaults = {
      id           = "/subscriptions/00000000-0000-0000-0000-000000000000/resourceGroups/rg-tp3-finanzas/providers/Microsoft.ContainerRegistry/registries/tp3synthetic"
      login_server = "tp3synthetic.azurecr.io"
    }
  }
  mock_resource "azurerm_user_assigned_identity" {
    defaults = {
      id           = "/subscriptions/00000000-0000-0000-0000-000000000000/resourceGroups/rg-tp3-finanzas/providers/Microsoft.ManagedIdentity/userAssignedIdentities/id-tp3-summary"
      principal_id = "00000000-0000-0000-0000-000000000001"
    }
  }
  mock_resource "azurerm_container_app_environment" {
    defaults = {
      id = "/subscriptions/00000000-0000-0000-0000-000000000000/resourceGroups/rg-tp3-finanzas/providers/Microsoft.App/managedEnvironments/cae-tp3-finanzas"
    }
  }
}

variables {
  subscription_id         = "00000000-0000-0000-0000-000000000000"
  location                = "chilecentral"
  registry_name           = "tp3synthetic"
  firebase_client_api_key = "synthetic-client-api-key"
  approve_credit_usage    = true
  deploy_app              = true
  image                   = "tp3synthetic.azurecr.io/summary@sha256:0000000000000000000000000000000000000000000000000000000000000000"
}

run "bounded_keyless_runtime" {
  command = plan
  assert {
    condition = (
      azurerm_container_app.summary[0].template[0].min_replicas == 0 &&
      azurerm_container_app.summary[0].template[0].max_replicas == 2 &&
      one(azurerm_container_app.summary[0].template[0].http_scale_rule).concurrent_requests == "16" &&
      azurerm_container_app.summary[0].template[0].container[0].cpu == 0.25 &&
      azurerm_container_app.summary[0].template[0].container[0].memory == "0.5Gi"
    )
    error_message = "La demostración debe mantener límites de consumo acotados."
  }
  assert {
    condition     = !azurerm_container_registry.images.admin_enabled && azurerm_role_assignment.pull.role_definition_name == "AcrPull"
    error_message = "Usar identidad administrada con lectura del registro; no habilitar claves admin."
  }
  assert {
    condition     = !azurerm_container_app.summary[0].ingress[0].allow_insecure_connections
    error_message = "El token Firebase sólo debe viajar por HTTPS."
  }
}

run "approval_required" {
  command = plan
  variables { approve_credit_usage = false }
  expect_failures = [azurerm_resource_group.tp3]
}
