mock_provider "google" {}

variables {
  project_id            = "finanzasapp-1f76a"
  region                = "us-central1"
  image                 = "us-central1-docker.pkg.dev/finanzasapp-1f76a/tp3/summary@sha256:0000000000000000000000000000000000000000000000000000000000000000"
  approve_cloud_changes = true
}

run "scaling_and_identity" {
  command = plan
  assert {
    condition = (
      google_cloud_run_v2_service.summary.scaling[0].min_instance_count == 0 &&
      google_cloud_run_v2_service.summary.scaling[0].max_instance_count == 2 &&
      google_cloud_run_v2_service.summary.template[0].max_instance_request_concurrency == 16
    )
    error_message = "La capacidad del TP debe permanecer acotada a 0-2 instancias y concurrencia 16."
  }
  assert {
    condition = google_project_iam_custom_role.summary.permissions == toset([
      "datastore.databases.get", "datastore.entities.get", "datastore.entities.list", "firebaseauth.users.get"
    ])
    error_message = "La cuenta runtime debe tener sólo los cuatro permisos de lectura previstos."
  }
  assert {
    condition     = one(google_cloud_run_v2_service.summary.template[0].containers[0].env).value == var.project_id
    error_message = "La audiencia Firebase debe quedar fijada al proyecto configurado."
  }
}

run "approval_required" {
  command = plan
  variables { approve_cloud_changes = false }
  expect_failures = [google_service_account.summary]
}
