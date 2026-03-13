terraform {
  required_version = ">= 1.6.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.25"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

module "ecr" {
  source       = "../../modules/ecr"
  service_name = var.service_name
  team_name    = var.team_name
}

module "iam" {
  source       = "../../modules/iam"
  service_name = var.service_name
  team_name    = var.team_name
  ecr_arn      = module.ecr.repository_arn
}

module "k8s" {
  source        = "../../modules/k8s"
  service_name  = var.service_name
  team_name     = var.team_name
  ecr_image_uri = module.ecr.repository_url
}