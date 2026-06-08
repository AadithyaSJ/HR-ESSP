# Developer Quick Start Guide

Quick reference for common development tasks.

## Getting Started

### 1. Clone & Setup Environment

```bash
# Clone the repository
git clone https://github.com/yourusername/hrportal.git
cd hrportal

# Copy environment template
cp .env.example .env

# Edit .env with local values
# DB_USER, DB_PASSWORD, KEYCLOAK credentials, etc.
```

### 2. Start Local Services

```bash
# Start all Docker services (Postgres, Keycloak, MinIO, Redis)
docker-compose up -d

# Verify services are running
docker-compose ps

# View logs for a service
docker-compose logs -f postgres    # or keycloak, minio, redis

# Stop services
docker-compose down
```

### 3. Backend Development

```bash
cd backend

# Build the project
mvn clean install

# Run tests
mvn test
mvn verify  # includes integration tests

# Start development server
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Application will be available at: http://localhost:8080
# API docs: http://localhost:8080/swagger-ui.html
```

### 4. Frontend Development

```bash
cd frontend

# Install dependencies
npm install

# Start dev server with hot reload
npm run dev

# Application will be available at: http://localhost:5173
```

### 5. Verify Everything Works

```
✓ Frontend: http://localhost:5173
✓ Backend API: http://localhost:8080/api/v1
✓ Keycloak: http://localhost:8081 (admin / admin)
✓ MinIO: http://localhost:9001 (minioadmin / minioadmin)
✓ PostgreSQL: localhost:5432 (springuser / spring123)
```

---

## Git Workflow

### Creating a Feature Branch

```bash
# Update develop with latest code
git checkout develop
git pull origin develop

# Create feature branch
git checkout -b feature/your-feature-name

# Make changes and commit
git add .
git commit -m "feat(module): description of feature"

# Push to remote
git push origin feature/your-feature-name
```

### Creating a Pull Request

1. Go to GitHub
2. Create PR from `feature/your-feature-name` → `develop`
3. Add title: `[feat] Your feature description`
4. Add description of changes
5. Request 2 reviewers
6. Wait for CI checks to pass
7. Once approved, squash & merge

### Merging to Main (Release Process)

```bash
# Create release branch
git checkout -b release/1.0.0

# Update version numbers, changelogs

# Create PR: release/1.0.0 → main
# Once approved and deployed, merge with conventional merge (not squash)

# Tag the release
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

---

## Common Development Tasks

### Add a New Backend Feature

1. Create entity in `backend/src/main/java/com/dotsolution/hrportal/<module>/entity/`
2. Create DTO in `backend/src/main/java/com/dotsolution/hrportal/<module>/dto/`
3. Create repository in `backend/src/main/java/com/dotsolution/hrportal/<module>/repository/`
4. Create service in `backend/src/main/java/com/dotsolution/hrportal/<module>/service/`
5. Create controller in `backend/src/main/java/com/dotsolution/hrportal/<module>/controller/`
6. Create tests in `backend/src/test/java/com/dotsolution/hrportal/<module>/`
7. Add Flyway migration if needed in `backend/src/main/resources/db/migration/`

### Add a New Frontend Component

1. Create Vue component in `frontend/src/modules/<feature>/components/`
2. Export from module's `index.js`
3. Register in appropriate page/layout
4. Add route to `frontend/src/router/index.js` if needed
5. Add Pinia store to `frontend/src/stores/` if needed

### Run Tests

```bash
# Backend unit tests
cd backend && mvn test

# Backend integration tests
cd backend && mvn verify

# Frontend tests
cd frontend && npm run test

# Frontend linting
cd frontend && npm run lint

# Frontend type checking
cd frontend && npm run type-check
```

### Database Operations

```bash
# Access PostgreSQL
psql -h localhost -U springuser -d dot_solution

# View migrations that have run
SELECT * FROM flyway_schema_history;

# Reset database (WARNING: deletes all data)
docker-compose down -v
docker-compose up -d postgres

# Seed test data
# Scripts in: backend/src/main/resources/db/seed/
```

### Debugging

```bash
# View backend logs
docker-compose logs -f postgres
docker-compose logs -f keycloak

# Check network connectivity
docker exec hrportal-postgres psql -U postgres -c "SELECT 1;"

# View container internals
docker exec -it hrportal-postgres bash

# Inspect API request/response
curl -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  http://localhost:8080/api/v1/leave
```

---

## Keycloak Setup (One-Time)

The Keycloak container auto-starts, but you may need to:

1. Access admin console: http://localhost:8081
2. Login: admin / admin
3. Create realm: hrportal
4. Create client: hrportal-web
5. Set client secret & redirect URIs
6. Create test users for development

---

## Code Review Checklist

Before submitting PR, ensure:

- [ ] Code follows [CONVENTIONS.md](CONVENTIONS.md)
- [ ] All tests pass locally
- [ ] No console errors/warnings
- [ ] No hardcoded secrets
- [ ] Documentation updated
- [ ] Database migrations added (if needed)
- [ ] No breaking changes to API
- [ ] PR title follows conventional commits

---

## Performance Tips

- Use pagination on list endpoints: `?page=0&size=50`
- Lazy load large datasets
- Cache frequently accessed data (Pinia store)
- Use indexes on frequently filtered columns
- Enable query logging only in dev: `spring.jpa.show-sql=true`

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Port 8080 already in use | `lsof -i :8080` then `kill -9 <PID>` |
| Keycloak won't start | Check Docker: `docker-compose ps keycloak` |
| Database connection error | Verify credentials in `.env` match `docker-compose.yml` |
| Frontend can't reach API | Ensure CORS enabled, check `VITE_API_BASE_URL` in `.env` |
| JWT token expired | Token auto-refreshes; check Keycloak is running |
| Flyway migration fails | Check SQL syntax, ensure table names unique |

---

## Resources

- [CONVENTIONS.md](CONVENTIONS.md) — Coding standards
- [ARCHITECTURE.md](ARCHITECTURE.md) — System design
- [README.md](README.md) — Full documentation
- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [Vue 3 Docs](https://vuejs.org/)
- [Keycloak Docs](https://www.keycloak.org/documentation)

---

**Last Updated**: June 2024
