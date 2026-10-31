variable "project_id" {
  description = "GCP project that hosts the checkout BFF."
  type        = string
}

variable "region" {
  description = "Region for Cloud Run, Cloud SQL and Artifact Registry."
  type        = string
  default     = "southamerica-east1"
}

variable "service_name" {
  description = "Base name used for the Cloud Run service and related resources."
  type        = string
  default     = "checkout-bff"
}

variable "github_repository" {
  description = "GitHub repository allowed to deploy, in the owner/name format."
  type        = string
}

variable "image" {
  description = "Initial container image. Later deployments are managed by the CD pipeline."
  type        = string
  default     = "us-docker.pkg.dev/cloudrun/container/hello"
}

variable "db_tier" {
  description = "Cloud SQL machine tier."
  type        = string
  default     = "db-f1-micro"
}

variable "max_instances" {
  description = "Maximum number of Cloud Run instances."
  type        = number
  default     = 3
}

variable "deletion_protection" {
  description = "Protects the database instance from accidental deletion."
  type        = bool
  default     = false
}
