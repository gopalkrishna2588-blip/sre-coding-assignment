variable "service_name" {}
variable "team_name"    {}
variable "ecr_arn"      {}

data "aws_caller_identity" "current" {}

resource "aws_iam_role" "this" {
  name = "${var.team_name}-${var.service_name}-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = "sts:AssumeRole"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })

  tags = {
    Service = var.service_name
    Team    = var.team_name
  }
}

resource "aws_iam_role_policy" "ecr_pull" {
  name = "${var.service_name}-ecr-pull"
  role = aws_iam_role.this.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetAuthorizationToken"
      ]
      Resource = [var.ecr_arn, "*"]
    }]
  })
}

output "role_arn"  { value = aws_iam_role.this.arn }
output "role_name" { value = aws_iam_role.this.name }