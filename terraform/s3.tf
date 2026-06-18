resource "random_id" "bucket_suffix" {
  byte_length = 4
}

# 1. S3 Bucket
resource "aws_s3_bucket" "storage" {
  bucket        = "${var.s3_bucket_name}-${random_id.bucket_suffix.hex}"
  force_destroy = true # Permits bucket deletion during terraform destroy even if it has files inside

  tags = {
    Name        = "${var.app_name}-${var.environment}-storage"
    Environment = var.environment
  }
}

# 2. Block Public Access
resource "aws_s3_bucket_public_access_block" "storage_public_block" {
  bucket = aws_s3_bucket.storage.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# 3. CORS Configuration
resource "aws_s3_bucket_cors_configuration" "storage_cors" {
  bucket = aws_s3_bucket.storage.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "PUT", "POST", "DELETE", "HEAD"]
    allowed_origins = ["*"] # Adjust to specific domain/ALB URL in strict production
    expose_headers  = ["ETag"]
    max_age_seconds = 3000
  }
}

# 4. Server Side Encryption
resource "aws_s3_bucket_server_side_encryption_configuration" "storage_encryption" {
  bucket = aws_s3_bucket.storage.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}
