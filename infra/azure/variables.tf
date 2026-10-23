variable "subscription_id" {
  type        = string
  description = "Azure for Students; completar localmente después de verificar la oferta y límite de gasto."
}

variable "location" {
  type        = string
  description = "Región propuesta chilecentral; verificar cuotas y políticas de la suscripción antes de aplicar."
}

variable "registry_name" {
  type        = string
  description = "Nombre globalmente único, sólo minúsculas y números, 5-50 caracteres."
  validation {
    condition     = can(regex("^[a-z0-9]{5,50}$", var.registry_name))
    error_message = "Usar 5-50 letras minúsculas o números."
  }
}

variable "registry_location" {
  type        = string
  default     = "brazilsouth"
  description = "ACR Tasks no está disponible en Chile Central; registro Basic en Brazil South con la misma tarifa revisada."
}

variable "firebase_project_id" {
  type    = string
  default = "finanzasapp-1f76a"
}

variable "firebase_client_api_key" {
  type        = string
  sensitive   = true
  default     = ""
  description = "Clave de API cliente de Firebase, NO una clave privada de cuenta de servicio."
}

variable "deploy_app" {
  type        = bool
  default     = false
  description = "Fase 1 crea infraestructura; fase 2 despliega imagen ya compilada por digest."
}

variable "image" {
  type    = string
  default = ""
  validation {
    condition     = !var.deploy_app || can(regex("^[a-z0-9]+\\.azurecr\\.io/.+@sha256:[a-f0-9]{64}$", var.image))
    error_message = "La fase 2 necesita una imagen ACR inmutable por digest sha256."
  }
}

variable "approve_credit_usage" {
  type        = bool
  default     = false
  description = "Requiere aprobación explícita del usuario para consumir crédito estudiantil."
}
