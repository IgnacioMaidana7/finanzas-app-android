locals {
  budget_start = formatdate("YYYY-MM-01'T'00:00:00'Z'", plantimestamp())
}

# Alerts only: Azure Cost Management does not stop resources at this amount.
resource "azurerm_consumption_budget_subscription" "tp3" {
  name            = "budget-tp3-finanzas-v2"
  subscription_id = "/subscriptions/${var.subscription_id}"
  amount          = 5
  time_grain      = "Monthly"

  time_period {
    start_date = local.budget_start
    end_date   = timeadd(local.budget_start, "8760h")
  }

  filter {
    dimension {
      name   = "ResourceGroupName"
      values = [azurerm_resource_group.tp3.name]
    }
  }

  dynamic "notification" {
    for_each = toset([50, 80, 100])
    content {
      enabled        = true
      operator       = "GreaterThanOrEqualTo"
      threshold      = notification.value
      threshold_type = "Actual"
      contact_roles  = ["Owner"]
    }
  }
}
