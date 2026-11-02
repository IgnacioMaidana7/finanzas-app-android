output "registry_name" {
  value = azurerm_container_registry.images.name
}

output "registry_server" {
  value = azurerm_container_registry.images.login_server
}

output "summary_url" {
  value = var.deploy_app ? "https://${azurerm_container_app.summary[0].ingress[0].fqdn}" : null
}
