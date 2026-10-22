output "summary_url" {
  value = google_cloud_run_v2_service.summary.uri
}

output "runtime_service_account" {
  value = google_service_account.summary.email
}
