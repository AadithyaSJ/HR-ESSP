variable "aws_region" {
  type        = string
  description = "AWS Region to deploy resources"
  default     = "ap-south-1"
}

variable "app_name" {
  type        = string
  description = "Application name prefix"
  default     = "hrportal"
}

variable "environment" {
  type        = string
  description = "Deployment environment"
  default     = "production"
}

variable "db_name" {
  type        = string
  description = "Database name"
  default     = "dot_solution"
}

variable "db_user" {
  type        = string
  description = "Database administrator username"
  default     = "springuser"
}

variable "db_password" {
  type        = string
  description = "Database administrator password"
  sensitive   = true
  default     = "SpringPassAWS123!"
}

variable "db_instance_class" {
  type        = string
  description = "RDS instance class"
  default     = "db.t4g.micro"
}

variable "s3_bucket_name" {
  type        = string
  description = "S3 bucket for document storage (must be globally unique)"
  default     = "hrportal-prod-storage-bucket"
}

variable "backend_cpu" {
  type        = string
  description = "CPU units for backend service (1 vCPU = 1024)"
  default     = "512"
}

variable "backend_memory" {
  type        = string
  description = "Memory (in MB) for backend service"
  default     = "1024"
}

variable "frontend_cpu" {
  type        = string
  description = "CPU units for frontend service"
  default     = "256"
}

variable "frontend_memory" {
  type        = string
  description = "Memory (in MB) for frontend service"
  default     = "512"
}
