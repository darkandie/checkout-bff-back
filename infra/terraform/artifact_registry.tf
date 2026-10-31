resource "google_artifact_registry_repository" "app" {
  location      = var.region
  repository_id = var.service_name
  description   = "Container images of the checkout BFF"
  format        = "DOCKER"

  cleanup_policies {
    id     = "keep-recent-versions"
    action = "KEEP"

    most_recent_versions {
      keep_count = 10
    }
  }

  cleanup_policies {
    id     = "delete-older-than-30-days"
    action = "DELETE"

    condition {
      older_than = "2592000s"
    }
  }

  depends_on = [google_project_service.required]
}
