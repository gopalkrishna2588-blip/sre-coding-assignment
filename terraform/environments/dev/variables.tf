variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-south-1"
}

variable "service_name" {
  description = "Microservice name"
  type        = string
}

variable "team_name" {
  description = "Owning team name"
  type        = string
}