variable "service_name"  {}
variable "team_name"     {}
variable "ecr_image_uri" {}

resource "kubernetes_namespace" "this" {
  metadata {
    name   = var.team_name
    labels = { team = var.team_name }
  }
}

resource "kubernetes_deployment" "this" {
  metadata {
    name      = var.service_name
    namespace = kubernetes_namespace.this.metadata[0].name
    labels    = { app = var.service_name }
  }

  spec {
    replicas = 1
    selector { match_labels = { app = var.service_name } }
    template {
      metadata { labels = { app = var.service_name } }
      spec {
        container {
          name  = var.service_name
          image = "${var.ecr_image_uri}:latest"
          port  { container_port = 8080 }
        }
      }
    }
  }
}

resource "kubernetes_service" "this" {
  metadata {
    name      = var.service_name
    namespace = kubernetes_namespace.this.metadata[0].name
  }
  spec {
    selector = { app = var.service_name }
    port {
      port        = 80
      target_port = 8080
    }
    type = "ClusterIP"
  }
}

output "namespace" { value = kubernetes_namespace.this.metadata[0].name }