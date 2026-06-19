# 1. TLS Private Key for SSH
resource "tls_private_key" "bastion_key" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

# 2. AWS Key Pair
resource "aws_key_pair" "bastion" {
  key_name   = "${var.app_name}-${var.environment}-bastion-key"
  public_key = tls_private_key.bastion_key.public_key_openssh
}

# 3. Security Group for Bastion Host
resource "aws_security_group" "bastion" {
  name        = "${var.app_name}-${var.environment}-bastion-sg"
  description = "Allows SSH and SSM connections to Bastion"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "Allow SSH from internet"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "Allow all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "${var.app_name}-${var.environment}-bastion-sg"
    Environment = var.environment
  }
}

# 4. Ingress Rule on RDS Security Group allowing access from Bastion
resource "aws_security_group_rule" "rds_from_bastion" {
  type                     = "ingress"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  security_group_id        = aws_security_group.rds.id
  source_security_group_id = aws_security_group.bastion.id
  description              = "Allow PostgreSQL access from temporary Bastion host"
}

# 5. IAM Role and Instance Profile for Systems Manager (SSM)
resource "aws_iam_role" "bastion_ssm" {
  name = "${var.app_name}-${var.environment}-bastion-ssm-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "bastion_ssm_attach" {
  role       = aws_iam_role.bastion_ssm.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "bastion" {
  name = "${var.app_name}-${var.environment}-bastion-profile"
  role = aws_iam_role.bastion_ssm.name
}

# 6. Fetch the latest Amazon Linux 2023 AMI dynamically
data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }
}

# 7. Bastion EC2 Instance
resource "aws_instance" "bastion" {
  ami                         = data.aws_ami.amazon_linux_2023.id
  instance_type               = "t3.micro"
  subnet_id                   = aws_subnet.public[0].id
  vpc_security_group_ids      = [aws_security_group.bastion.id]
  key_name                    = aws_key_pair.bastion.key_name
  iam_instance_profile        = aws_iam_instance_profile.bastion.name
  associate_public_ip_address = true

  tags = {
    Name        = "${var.app_name}-${var.environment}-bastion"
    Environment = var.environment
  }
}

# 8. Outputs for Bastion
output "bastion_public_ip" {
  value       = aws_instance.bastion.public_ip
  description = "The public IP of the temporary Bastion host"
}

output "bastion_private_key" {
  value       = tls_private_key.bastion_key.private_key_pem
  description = "The generated SSH private key for the Bastion host (sensitive)"
  sensitive   = true
}
