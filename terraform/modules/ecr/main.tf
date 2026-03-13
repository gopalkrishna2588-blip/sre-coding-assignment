variable "service_name" {}
variable "team_name"    {}

resource "aws_ecr_repository" "this" {
  name                 = "${var.team_name}-${var.service_name}"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Service = var.service_name
    Team    = var.team_name
  }
}

output "repository_url" { value = aws_ecr_repository.this.repository_url }
output "repository_arn" { value = aws_ecr_repository.this.arn }