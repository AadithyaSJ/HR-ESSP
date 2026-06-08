# HR Employee Self-Service Portal

A comprehensive, scalable HR management system featuring employee onboarding, leave management, expense reimbursement, payroll processing, and automated workflows.

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Quick Start](#quick-start)
  - [Prerequisites](#prerequisites)
  - [Local Development Setup](#local-development-setup)
  - [Running Services](#running-services)
- [Development Workflow](#development-workflow)
- [Architecture](#architecture)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Deployment](#deployment)
- [Contributing](#contributing)
- [Troubleshooting](#troubleshooting)

---

## Overview

The HR Employee Self-Service Portal is a modern, enterprise-grade HR platform that enables:

- **Employee Onboarding** — Automated workflows for new hires
- **Leave Management** — Request, approve, and track leave with balance calculations
- **Expense Management** — Submit, track, and approve expense claims with S3-attached receipts
- **Payroll Processing** — Automated payroll calculation, payslip generation, and delivery
- **Real-time Notifications** — SSE-based notifications for approvals, policy changes
- **Comprehensive Reporting** — Department analytics, leave trends, payroll reports
- **Workflow & Audit Trail** — Complete workflow history with change tracking

---

## Tech Stack

### Backend

- **Java 21** with Spring Boot 3.x
- **PostgreSQL 16** (primary database)
- **Keycloak 24** (identity & access management)
- **AWS S3** (file storage)
- **AWS SES** (email notifications)
- **Spring Data JPA** (ORM)
- **Flyway** (database migrations)
- **Maven 3.9+** (build tool)

### Frontend

- **Vue 3** with TypeScript
- **Vuetify 3** (material design components)
- **Pinia** (state management)
- **Axios** (HTTP client)
- **Vite** (build tool)
- **Vue Router** (client-side routing)

### DevOps & Deployment

- **Docker** (containerization)
- **Docker Compose** (local development orchestration)
- **GitHub Actions** (CI/CD)
- **AWS ECR** (container registry)
- **AWS ECS Fargate** (orchestration)
- **AWS CloudWatch** (monitoring & logging)
- **Terraform/CloudFormation** (IaC)

---

## Project Structure

```
excellathon/
├── backend/                          # Spring Boot application
│   ├── src/
│   │   ├── main/java/com/dotsolution/dot/
│   │   │   ├── auth/                 # Authentication & security
│   │   │   ├── employee/             # Employee module
│   │   │   ├── leave/                # Leave management
│   │   │   ├── expense/              # Expense claims
│   │   │   ├── payroll/              # Payroll processing
│   │   │   ├── notification/         # Email & notifications
│   │   │   ├── workflow/             # Workflows & approvals
│   │   │   ├── common/               # Utilities & exceptions
│   │   │   └── config/               # Spring configs
│   │   ├── resources/
│   │   │   ├── application.properties
│   │   │   ├── application-dev.properties
│   │   │   ├── application-prod.properties
│   │   │   └── db/migration/         # Flyway migrations
│   │   └── test/java/                # Unit & integration tests
│   └── pom.xml
│
├── frontend/                         # Vue 3 application
│   ├── src/
│   │   ├── modules/                  # Feature modules
│   │   ├── components/               # Shared components
│   │   ├── services/                 # API clients
│   │   ├── stores/                   # Pinia stores
│   │   ├── types/                    # TypeScript types
│   │   ├── router/                   # Vue Router config
│   │   └── main.js
│   ├── package.json
│   ├── vite.config.js
│   └── tsconfig.json
│
├── docker-compose.yml                # Local dev orchestration
├── .env.example                      # Environment variables template
├── CONVENTIONS.md                    # Coding standards
├── ARCHITECTURE.md                   # System architecture
├── PROJECT_TODO.md                   # Implementation roadmap
└── README.md                         # This file
```

---

## Quick Start

### Prerequisites

- **Java 21** or higher: [Download](https://www.oracle.com/java/technologies/downloads/#java21)
- **Node.js 18+**: [Download](https://nodejs.org/)
- **Docker Desktop**: [Download](https://www.docker.com/products/docker-desktop)
- **PostgreSQL 16** (optional, use Docker instead)
- **Git**: [Download](https://git-scm.com/)

### Local Development Setup

#### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/hrportal.git
cd hrportal
```

#### 2. Create Environment File

```bash
cp .env.example .env
```

Edit `.env` and update values for your local setup:

```env
DB_USER=springuser
DB_PASSWORD=spring123
DB_NAME=dot_solution
KEYCLOAK_ADMIN_PASSWORD=admin
MINIO_ROOT_PASSWORD=minioadmin
```

#### 3. Start Services with Docker Compose

```bash
docker-compose up -d
```

This starts:

- PostgreSQL (port 5432)
- Keycloak (port 8081)
- MinIO (ports 9000, 9001)
- Redis (port 6379)

Verify services are running:

```bash
docker-compose ps
```

#### 4. Backend Setup

```bash
cd backend

# Build the project
mvn clean install

# Run the application (Spring Boot 3.x)
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

Backend runs on: `http://localhost:8080`

#### 5. Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

Frontend runs on: `http://localhost:5173`

#### 6. Access the Application

- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080/api/v1
- **Keycloak Admin**: http://localhost:8081 (admin / admin)
- **MinIO Console**: http://localhost:9001 (minioadmin / minioadmin)
- **PostgreSQL**: localhost:5432 (springuser / spring123)

---

## Running Services

### Start All Services

```bash
docker-compose up -d
```

### Stop All Services

```bash
docker-compose down
```

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f postgres
docker-compose logs -f keycloak
```

### Reset Database

```bash
docker-compose down -v
docker-compose up -d postgres
```

---

## Development Workflow

### 1. Create Feature Branch

```bash
git checkout develop
git pull origin develop
git checkout -b feature/your-feature-name
```

### 2. Make Changes

- Follow [CONVENTIONS.md](CONVENTIONS.md) for code style
- Write tests for new features
- Update documentation

### 3. Test Locally

**Backend:**

```bash
mvn test
mvn verify  # includes integration tests
```

**Frontend:**

```bash
npm run test
npm run lint
```

### 4. Commit & Push

```bash
git add .
git commit -m "feat(module): description of changes"
git push origin feature/your-feature-name
```

### 5. Create Pull Request

- Title: `[feat] Feature description`
- Description: Include issue reference, changes, testing
- Request 2+ reviewers
- Ensure all checks pass before merging

---

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed system design:

- Component overview
- Data flow diagrams
- Integration points
- Security architecture
- Scalability considerations

---

## API Documentation

API endpoints follow RESTful conventions. See [API_DOCS.md](API_DOCS.md) for:

- Authentication flow (Keycloak + JWT)
- Endpoint specifications
- Request/response examples
- Error codes & handling

### Example: Get Leave Balance

```bash
curl -X GET http://localhost:8080/api/v1/leave/balance \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

Response:

```json
{
  "success": true,
  "data": {
    "employeeId": "emp-123",
    "leaveType": "ANNUAL",
    "totalDays": 20,
    "usedDays": 5,
    "balanceDays": 15,
    "lastUpdated": "2024-06-01T10:30:00Z"
  }
}
```

---

## Database Schema

PostgreSQL tables include:

- `user` — User accounts linked to Keycloak
- `employee` — Employee profiles
- `role` — Role definitions (HR, Manager, Employee, Admin)
- `leave_type` — Leave categories (Annual, Sick, Maternity, etc.)
- `leave_request` — Leave requests & history
- `leave_balance` — Current balance per employee
- `expense_claim` — Expense requests
- `payslip` — Generated payslips
- `notification` — In-app & email notifications
- `audit_log` — Complete change history
- `workflow_action` — Workflow step history

See [DATABASE.md](DATABASE.md) for full schema documentation & ER diagram.

---

## Deployment

### Development (Local)

```bash
docker-compose up -d
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
npm run dev
```

### Staging (Docker + AWS)

```bash
# Build Docker images
docker build -t hrportal-backend:latest ./backend
docker build -t hrportal-frontend:latest ./frontend

# Push to ECR
aws ecr get-login-password --region ap-south-1 | docker login --username AWS --password-stdin <YOUR_ECR_URL>
docker tag hrportal-backend:latest <YOUR_ECR_URL>/hrportal-backend:latest
docker push <YOUR_ECR_URL>/hrportal-backend:latest

# Deploy via ECS
aws ecs update-service --cluster hrportal-staging --service hrportal-backend --force-new-deployment
```

### Production (ECS Fargate + RDS)

- Use managed PostgreSQL (RDS)
- Use managed Keycloak (or self-hosted with HA)
- Enable CloudWatch monitoring & alarms
- Use CloudFront for CDN
- Enable WAF for API protection

See [DEPLOYMENT.md](DEPLOYMENT.md) for complete CI/CD pipeline setup.

---

## Contributing

1. Follow [CONVENTIONS.md](CONVENTIONS.md)
2. Write tests with >80% coverage
3. Update documentation
4. Submit PR with 2+ approvals
5. Ensure all CI checks pass

---

## Troubleshooting

### Backend Won't Start

```bash
# Check if port 8080 is in use
lsof -i :8080

# Check database connection
mvn test -Dtest=DatabaseConnectionTest

# View full logs
mvn spring-boot:run -X
```

### Keycloak Not Accessible

```bash
# Check if container is running
docker-compose ps keycloak

# View logs
docker-compose logs keycloak

# Restart
docker-compose restart keycloak
```

### Database Connection Errors

```bash
# Verify PostgreSQL is running
docker-compose ps postgres

# Check timezone setting
docker exec hrportal-postgres psql -U postgres -c "SHOW TimeZone"

# Connect directly to test
psql -h localhost -U springuser -d dot_solution
```

### Frontend Build Issues

```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install

# Clear Vite cache
rm -rf dist .vite
npm run build
```

---

## Support & Documentation

- **Issues**: GitHub Issues
- **Discussions**: GitHub Discussions
- **Slack**: #hrportal channel
- **Wiki**: [Project Wiki](https://github.com/yourusername/hrportal/wiki)

---

## License

Proprietary. All rights reserved.

---

## Team

- **Project Lead**: [Name]
- **Architects**: [Names]
- **Contributors**: [Names]

---

**Last Updated**: June 2024  
**Maintained By**: HR Portal Team
