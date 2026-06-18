data "aws_availability_zones" "available" {
  state = "available"
}

# 1. VPC Definition
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name        = "${var.app_name}-${var.environment}-vpc"
    Environment = var.environment
  }
}

# 2. Internet Gateway
resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name        = "${var.app_name}-${var.environment}-igw"
    Environment = var.environment
  }
}

# 3. Public Subnets (ALB & NAT Gateway)
resource "aws_subnet" "public" {
  count                   = 2
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.${count.index}.0/24"
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name        = "${var.app_name}-${var.environment}-public-subnet-${count.index + 1}"
    Environment = var.environment
  }
}

# 4. Private App Subnets (ECS Tasks)
resource "aws_subnet" "private_app" {
  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.${count.index + 10}.0/24"
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name        = "${var.app_name}-${var.environment}-private-app-${count.index + 1}"
    Environment = var.environment
  }
}

# 5. Private DB Subnets (RDS DB Instances)
resource "aws_subnet" "private_db" {
  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.${count.index + 20}.0/24"
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name        = "${var.app_name}-${var.environment}-private-db-${count.index + 1}"
    Environment = var.environment
  }
}

# 6. Elastic IP for NAT Gateway
resource "aws_eip" "nat" {
  domain     = "vpc"
  depends_on = [aws_internet_gateway.igw]

  tags = {
    Name        = "${var.app_name}-${var.environment}-nat-eip"
    Environment = var.environment
  }
}

# 7. NAT Gateway (Single Gateway to reduce costs)
resource "aws_nat_gateway" "nat" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public[0].id

  tags = {
    Name        = "${var.app_name}-${var.environment}-nat-gw"
    Environment = var.environment
  }
}

# 8. Route Tables
# Route Table for Public Subnets
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw.id
  }

  tags = {
    Name        = "${var.app_name}-${var.environment}-public-rt"
    Environment = var.environment
  }
}

# Route Table for Private Subnets (App & DB)
resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.nat.id
  }

  tags = {
    Name        = "${var.app_name}-${var.environment}-private-rt"
    Environment = var.environment
  }
}

# 9. Route Table Associations
# Associate Public Subnets with Public Route Table
resource "aws_route_table_association" "public" {
  count          = 2
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# Associate Private App Subnets with Private Route Table
resource "aws_route_table_association" "private_app" {
  count          = 2
  subnet_id      = aws_subnet.private_app[count.index].id
  route_table_id = aws_route_table.private.id
}

# Associate Private DB Subnets with Private Route Table (No public access)
resource "aws_route_table_association" "private_db" {
  count          = 2
  subnet_id      = aws_subnet.private_db[count.index].id
  route_table_id = aws_route_table.private.id
}

# 10. Database Subnet Group (Required for RDS)
resource "aws_db_subnet_group" "db_subnet_group" {
  name        = "${var.app_name}-${var.environment}-db-subnet-group"
  description = "Database Subnet Group for RDS"
  subnet_ids  = aws_subnet.private_db[*].id

  tags = {
    Name        = "${var.app_name}-${var.environment}-db-subnet-group"
    Environment = var.environment
  }
}
