resource "google_service_account" "runtime" {
  account_id   = "${var.service_name}-run"
  display_name = "Checkout BFF runtime"
}

resource "google_project_iam_member" "runtime_cloudsql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.runtime.email}"
}

resource "google_secret_manager_secret_iam_member" "runtime_db_password" {
  secret_id = google_secret_manager_secret.db_password.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.runtime.email}"
}

resource "google_cloud_run_v2_service" "app" {
  name                = var.service_name
  location            = var.region
  ingress             = "INGRESS_TRAFFIC_ALL"
  deletion_protection = false

  template {
    service_account                  = google_service_account.runtime.email
    timeout                          = "30s"
    max_instance_request_concurrency = 80

    scaling {
      min_instance_count = 0
      max_instance_count = var.max_instances
    }

    containers {
      image = var.image

      ports {
        container_port = 8080
      }

      resources {
        limits = {
          cpu    = "1"
          memory = "1Gi"
        }
        cpu_idle          = true
        startup_cpu_boost = true
      }

      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "gcp"
      }

      env {
        name  = "CLOUD_SQL_INSTANCE"
        value = google_sql_database_instance.main.connection_name
      }

      env {
        name  = "DATABASE_NAME"
        value = google_sql_database.app.name
      }

      env {
        name  = "DATABASE_USERNAME"
        value = google_sql_user.app.name
      }

      env {
        name = "DATABASE_PASSWORD"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.db_password.secret_id
            version = "latest"
          }
        }
      }

      startup_probe {
        tcp_socket {
          port = 8080
        }
        initial_delay_seconds = 5
        period_seconds        = 5
        timeout_seconds       = 3
        failure_threshold     = 24
      }

      liveness_probe {
        http_get {
          path = "/actuator/health/liveness"
          port = 8080
        }
        period_seconds    = 30
        timeout_seconds   = 3
        failure_threshold = 3
      }
    }
  }

  lifecycle {
    ignore_changes = [
      template[0].containers[0].image,
      client,
      client_version,
    ]
  }

  depends_on = [
    google_project_service.required,
    google_project_iam_member.runtime_cloudsql_client,
    google_secret_manager_secret_iam_member.runtime_db_password,
    google_secret_manager_secret_version.db_password,
  ]
}

resource "google_cloud_run_v2_service_iam_member" "public_invoker" {
  name     = google_cloud_run_v2_service.app.name
  location = google_cloud_run_v2_service.app.location
  role     = "roles/run.invoker"
  member   = "allUsers"
}
