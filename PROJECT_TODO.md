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

Notes:
- Prioritise setting up local dev infra (`docker-compose`) and Keycloak so frontend/backend auth flows can be developed in parallel.
- Use Flyway for DB migrations and seed scripts for dev.
- Prefer Keycloak as IdP for SAML/OIDC to accelerate integration with Azure AD and Okta.
- Keep sensitive config in environment variables / secrets manager and do not commit credentials.

If you want, I can split any high-level item above into smaller tickets with estimates and assign priorities. Also I can add GitHub Issues for each item automatically — tell me how you'd like them grouped (by sprint, by subsystem, by priority).