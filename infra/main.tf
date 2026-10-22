# No database creation, migration, Firebase config replacement, or billing enablement.
# APIs and an existing Artifact Registry image are preflight prerequisites.
resource "google_service_account" "summary" {
  account_id   = "tp3-summary-runtime"
  display_name = "TP3 financial summary (read only)"
  lifecycle {
    precondition {
      condition     = var.approve_cloud_changes
      error_message = "Falta revisar y autorizar los cambios cloud y sus costos."
    }
  }
}

resource "google_project_iam_custom_role" "summary" {
  role_id     = "tp3SummaryReader"
  title       = "TP3 summary read only"
  description = "Read Firestore and check Firebase token revocation; no writes."
  permissions = [
    "datastore.databases.get",
    "datastore.entities.get",
    "datastore.entities.list",
    "firebaseauth.users.get",
  ]
}

resource "google_project_iam_member" "summary" {
  project = var.project_id
  role    = google_project_iam_custom_role.summary.name
  member  = "serviceAccount:${google_service_account.summary.email}"
}

resource "google_cloud_run_v2_service" "summary" {
  name                = "tp3-finanzas-summary"
  location            = var.region
  deletion_protection = true
  ingress             = "INGRESS_TRAFFIC_ALL"
  labels              = { purpose = "tp3", app = "finanzas" }

  scaling {
    min_instance_count = 0
    max_instance_count = var.max_instances
  }

  template {
    service_account                  = google_service_account.summary.email
    timeout                          = "15s"
    max_instance_request_concurrency = 16
    containers {
      image = var.image
      ports { container_port = 8080 }
      resources {
        limits            = { cpu = "1", memory = "256Mi" }
        cpu_idle          = true
        startup_cpu_boost = false
      }
      env {
        name  = "FIREBASE_PROJECT_ID"
        value = var.project_id
      }
      startup_probe {
        initial_delay_seconds = 0
        period_seconds        = 3
        timeout_seconds       = 1
        failure_threshold     = 10
        http_get { path = "/healthz" }
      }
    }
  }
  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }
  depends_on = [google_project_iam_member.summary]
}

# Firebase ID tokens are verified by the application, not by Cloud Run IAM.
# Public transport is required for Android. /v1/summary still requires Firebase auth.
resource "google_cloud_run_v2_service_iam_member" "transport" {
  project  = var.project_id
  location = google_cloud_run_v2_service.summary.location
  name     = google_cloud_run_v2_service.summary.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}
