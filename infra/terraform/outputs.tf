output "service_url" {
  description = "Public URL of the checkout BFF."
  value       = google_cloud_run_v2_service.app.uri
}

output "artifact_registry" {
  description = "Docker repository used by the CD pipeline (GCP_ARTIFACT_REGISTRY)."
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.app.repository_id}"
}

output "workload_identity_provider" {
  description = "Workload Identity provider resource name (GCP_WORKLOAD_IDENTITY_PROVIDER)."
  value       = google_iam_workload_identity_pool_provider.github.name
}

output "deployer_service_account" {
  description = "Service account impersonated by GitHub Actions (GCP_DEPLOYER_SERVICE_ACCOUNT)."
  value       = google_service_account.deployer.email
}

output "cloud_sql_connection_name" {
  description = "Cloud SQL instance connection name."
  value       = google_sql_database_instance.main.connection_name
}
