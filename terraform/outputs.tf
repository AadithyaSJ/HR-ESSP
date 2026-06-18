output "alb_dns_name" {
  value       = aws_lb.main.dns_name
  description = "The public DNS name of the Application Load Balancer"
}

output "backend_ecr_url" {
  value       = aws_ecr_repository.backend.repository_url
  description = "The URL of the backend ECR repository"
}

output "frontend_ecr_url" {
  value       = aws_ecr_repository.frontend.repository_url
  description = "The URL of the frontend ECR repository"
}

output "rds_endpoint" {
  value       = aws_db_instance.postgres.endpoint
  description = "The connection endpoint for the RDS PostgreSQL database"
}

output "s3_bucket_name" {
  value       = aws_s3_bucket.storage.id
  description = "The name of the generated S3 bucket"
}
