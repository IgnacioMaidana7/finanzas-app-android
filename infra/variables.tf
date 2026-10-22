variable "project_id" {
  type        = string
  description = "Proyecto Firebase existente; verificar antes del apply."
  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{4,28}[a-z0-9]$", var.project_id))
    error_message = "Se requiere un ID válido de proyecto Google Cloud."
  }
}

variable "region" {
  type        = string
  description = "Elegir cerca de la ubicación real de Firestore, antes de desplegar."
}

variable "image" {
  type        = string
  description = "Imagen de Artifact Registry inmutable, fijada por digest sha256."
  validation {
    condition     = can(regex("^[a-z0-9-]+-docker.pkg.dev/.+@sha256:[a-f0-9]{64}$", var.image))
    error_message = "Usar una imagen de Artifact Registry con digest sha256."
  }
}

variable "max_instances" {
  type        = number
  default     = 2
  description = "Límite al nivel del servicio. No constituye un tope absoluto de gasto."
  validation {
    condition     = var.max_instances >= 1 && var.max_instances <= 3 && floor(var.max_instances) == var.max_instances
    error_message = "El TP permite entre 1 y 3 instancias; revisar costos antes de ampliarlo."
  }
}

variable "approve_cloud_changes" {
  type        = bool
  default     = false
  description = "Pasar a true sólo después de revisar plan, IAM, facturación, región y autorización del usuario."
}
