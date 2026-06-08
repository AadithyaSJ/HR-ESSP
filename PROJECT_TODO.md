# Project TODO List — HR Employee Self-Service Portal

This file contains the complete project TODO list covering database, backend, frontend, integrations, CI/CD, staging and production rollout. Use this as the single source of truth for task tracking and planning.

- [x] Define project conventions (naming, code style, branching, PR rules) — CONVENTIONS.md created
- [ ] Initialize git branches (main, develop, feature/*, release/*)
- [x] Create README & docs (architecture, setup, run, environment variables) — README.md, ARCHITECTURE.md, .env.example created
- [x] Setup Docker Compose for local development (Postgres, Keycloak, MinIO/localstack) — docker-compose.yml created
- [ ] Setup Keycloak locally (realm, clients, test users)
- [ ] Setup Postgres dev (docker, seed data)
- [ ] Design DB schema (users, roles, employees, leaves, expenses, payslips, notifications, workflow_actions)
- [ ] Implement Flyway (or Liquibase) migrations and baseline
- [ ] Configure `application.properties` / `application.yml` for profiles (dev/staging/prod)
- [ ] Implement Keycloak / SAML authentication integration in backend
- [ ] Implement JWT issuance and refresh token flow (when using IdP + JWT for APIs)
- [ ] Configure AWS S3 integration (buckets, credentials, environment config)
- [ ] Implement file storage service (upload, signed URLs, delete, lifecycle)
- [ ] Implement user module (registration hooks, role assignment, profile)
- [ ] Implement employee module (profile view/edit, onboarding checklist)
- [ ] Implement leave module (balance, apply, approvals, calendar)
- [ ] Implement expense module (submit claim, receipts, manager approval, finance queue)
- [ ] Implement payroll module (upload, generate payslips, publish)
- [ ] Implement notifications system (in-app, email via SES, preferences)
- [ ] Implement workflow engine (actions, audit trail, notifications)
- [ ] Implement reports module (headcount, leave utilisation, expense summary)
- [ ] Implement admin panel (user/role management, system settings, scheduler, audit logs)
- [ ] Add validation DTOs and request/response models
- [ ] Add JPA entities and repositories
- [ ] Add service layer with business logic and transactional boundaries
- [ ] Add REST controllers (versioned APIs, error handling)
- [ ] Add OpenAPI / Swagger documentation
- [ ] Add backend unit tests (JUnit, Mockito)
- [ ] Add backend integration tests (Testcontainers or embedded services)
- [ ] Scaffold frontend app (Vue 3 + TypeScript + Vite + Vuetify)
- [ ] Setup frontend routing and module structure (auth, employee, leave, expense, payroll, reports, admin)
- [ ] Setup frontend auth (SAML redirect / Keycloak OIDC, JWT handling, route guards)
- [ ] Build frontend modules and components (forms, tables, upload widgets, calendar)
- [ ] Add frontend state management (Pinia) and services (Axios with interceptors)
- [ ] Add frontend E2E tests (Cypress / Playwright)
- [ ] Setup CI workflows (GitHub Actions: build, test, lint, publish image)
- [ ] Add multi-stage Dockerfile for backend and frontend
- [ ] Configure AWS ECR and ECS Fargate deployment manifests (task defs, service, ALB)
- [ ] Add infrastructure as code (Terraform / CloudFormation) for networking, RDS, ECR, ECS, S3, IAM
- [ ] Setup monitoring & logging (CloudWatch/Datadog/Prometheus + Grafana, structured logs)
- [ ] Security hardening (CSP, CORS, secrets management, IAM least-privilege, pen test checklist)
- [ ] Performance testing plan (JMeter / k6 scripts, target throughput, SLOs)
- [ ] Accessibility & UX review (WCAG basics, keyboard nav, color contrast)
- [ ] Provision staging environment (separate AWS account/stack)
- [ ] Run staging integration tests and UAT
- [ ] Prepare production rollout (blue/green or canary strategy)
- [ ] Create runbooks & on-call procedures (incident runbooks, rollback steps)
- [ ] Final QA & acceptance
- [ ] Release, monitoring, and post-release checks
- [ ] Knowledge transfer & handover documentation

---

## ✅ PHASE 1: FOUNDATION (COMPLETED)

All foundational tasks are now complete. The project is ready for backend and database development.

**Completed Tasks:**

1. ✅ **Define project conventions** (CONVENTIONS.md)
   - Package structure, naming conventions, annotation guidelines
   - Git workflow with conventional commits
   - Code review checklist

2. ✅ **Initialize git branches**
   - `main` — Production-ready code
   - `develop` — Integration branch (current working branch)
   - Feature branch templates: `feature/*`, `bugfix/*`, `release/*`

3. ✅ **Create documentation**
   - README.md — Complete project overview and setup instructions
   - ARCHITECTURE.md — Detailed system design, data flows, security
   - QUICKSTART.md — Developer quick reference guide
   - .env.example — Environment variables template

4. ✅ **Setup Docker Compose**
   - PostgreSQL 16 (port 5432)
   - Keycloak 24 (port 8081)
   - MinIO for S3-compatible storage (ports 9000, 9001)
   - Redis for caching (port 6379)

**Current State:**
- Working Branch: `develop` (ready for feature development)
- Latest Commit: e663e5e — "Add quick start guide"
- All services defined in docker-compose.yml and ready to start

---

## 🚀 PHASE 2: BACKEND FOUNDATION (NEXT UP - Items 5-10)

These tasks establish the backend infrastructure for database and business logic.

**Next Tasks to Complete:**

- [ ] **Task 5:** Setup Keycloak locally (realm, clients, test users)
  - Create realm: hrportal
  - Create client: hrportal-web
  - Create test users for development
  - Configure SAML/OIDC settings

- [ ] **Task 6:** Setup Postgres dev environment
  - Start PostgreSQL container from docker-compose
  - Initialize database schema
  - Seed test data

- [ ] **Task 7:** Design complete DB schema (SQL DDL)
  - Create Flyway migration: V1__InitialSchema.sql
  - Tables: user, employee, role, leave_*, expense_*, payslip, notification, audit_log, workflow_*
  - Add indexes and constraints

- [ ] **Task 8:** Implement Flyway migrations
  - V1__InitialSchema.sql — Main tables
  - V2__CreateIndexes.sql — Performance indexes
  - Add seed data migrations for dev environment

- [ ] **Task 9:** Configure application.properties profiles
  - dev profile (local, verbose logging)
  - staging profile (minimal logging, external DB)
  - prod profile (optimized, monitoring enabled)

- [ ] **Task 10:** Implement Keycloak authentication in backend
  - Configure Spring Security with JWT validation
  - Create JWT filter and authentication provider
  - Setup authorization roles and permissions

---

## 📋 REMAINING PHASES (Tasks 11-48)

Refer to full list below. These will be tackled in order following Phase 2 completion.

---

## PROJECT STATUS SUMMARY

**Phase:** Foundation → Backend Foundation (Phase 2 starting now)  
**Progress:** 4/48 tasks complete (8%)  
**Active Branch:** develop  
**Last Updated:** June 2024

### What's Ready

✅ Project structure and git workflow established  
✅ Comprehensive documentation (CONVENTIONS, ARCHITECTURE, README, QUICKSTART)  
✅ Local development environment defined (docker-compose.yml)  
✅ Environment template ready (.env.example)  
✅ Backend code skeleton present (Spring Boot 4.0.6, Java 22, Maven)  
✅ Frontend code skeleton present (Vue 3, Vite, basic structure)

### What's Next

🔄 **PHASE 2 (Backend Foundation — Tasks 5-10):**
1. Start local Docker containers
2. Setup Keycloak with test realm and clients
3. Design and implement database schema
4. Create Flyway migrations
5. Configure Spring profiles (dev/staging/prod)
6. Implement backend authentication (JWT + Keycloak)

**Estimated Duration:** 3-5 days  
**Dependencies:** Docker Desktop, Java 21+, Node.js 18+, Git

### How to Proceed

1. Start services: `docker-compose up -d`
2. Follow QUICKSTART.md for development setup
3. Refer to CONVENTIONS.md for coding standards
4. Reference ARCHITECTURE.md for system design
5. Track progress in this file

### Notes

- Prioritise setting up local dev infra (`docker-compose`) and Keycloak so frontend/backend auth flows can be developed in parallel.
- Use Flyway for DB migrations and seed scripts for dev.
- Prefer Keycloak as IdP for SAML/OIDC to accelerate integration with Azure AD and Okta.
- Keep sensitive config in environment variables / secrets manager and do not commit credentials.