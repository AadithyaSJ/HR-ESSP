# Project Conventions

This document outlines the coding standards, naming conventions, and development practices for the HR Employee Self-Service Portal.

## Table of Contents

- [Git Workflow](#git-workflow)
- [Java/Backend Conventions](#javabackend-conventions)
- [Frontend (Vue/TypeScript) Conventions](#frontend-vuetypescript-conventions)
- [Database Conventions](#database-conventions)
- [API Conventions](#api-conventions)
- [Code Review & Quality](#code-review--quality)

---

## Git Workflow

### Branching Strategy

- **`main`** — Production-ready code. Deployments happen from this branch.
- **`develop`** — Integration branch for features. This is the base for PRs.
- **`feature/*`** — Feature branches. Example: `feature/leave-module`, `feature/s3-integration`.
- **`bugfix/*`** — Bug fix branches. Example: `bugfix/jwt-expiry-issue`.
- **`release/*`** — Release preparation branches. Example: `release/1.0.0`.

### Commit Messages

Follow conventional commits:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: A new feature
- `fix`: A bug fix
- `docs`: Documentation changes
- `refactor`: Code refactoring without changing functionality
- `test`: Adding or updating tests
- `chore`: Build, dependencies, or configuration changes
- `perf`: Performance improvements

**Example:**
```
feat(leave): implement leave balance calculation

Calculate leave balance based on accrual frequency and carry-forward rules.
Add unit tests for monthly and quarterly accrual scenarios.

Closes #123
```

### Pull Requests

- Title: `[<type>] <description>` (e.g., `[feat] Add leave module`)
- Description: Include issue reference, changes made, testing performed
- Minimum reviewers: 2
- All checks must pass (tests, linting, coverage)
- Squash & merge on main; conventional merge on develop

---

## Java/Backend Conventions

### Package Structure

```
com.dotsolution.hrportal
├── auth           # Authentication & security config
├── employee       # Employee profile & management
├── leave          # Leave management module
├── expense        # Expense claims module
├── payroll        # Payroll & payslips
├── notification   # Email & in-app notifications
├── workflow       # Workflow & audit trail
├── report         # Reports & analytics
├── admin          # Admin panel & settings
├── common         # Common utilities, enums, exceptions
└── config         # Spring configs (security, web, db)
```

### Naming Conventions

- **Classes**: `PascalCase` (e.g., `LeaveRequest`, `EmployeeService`)
- **Interfaces**: `IPascalCase` or just `PascalCase` (prefer no 'I' prefix)
- **Methods**: `camelCase` (e.g., `calculateLeaveBalance()`)
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `MAX_LEAVE_DAYS`)
- **Properties**: `camelCase` (e.g., `employeeId`, `leaveType`)

### File Organization

```
com.dotsolution.hrportal.leave
├── entity/         # JPA entities
├── dto/            # Request/response DTOs (Request, Response, Details suffixes)
├── repository/     # Spring Data JPA repositories
├── service/        # Business logic services
├── controller/     # REST controllers
├── exception/      # Custom exceptions
└── mapper/         # Entity <-> DTO mappers
```

### Annotations & Best Practices

- Use `@Transactional` only on service methods (not controllers)
- Use `@Validated` on controllers to enable method-level validation
- Use `@CreatedDate`, `@LastModifiedDate` for audit timestamps
- Use `@JsonIgnore` for sensitive fields in responses
- Add `@Slf4j` for logging: `log.info()`, `log.error()`
- Entity methods: use `equals()` and `hashCode()` based on `id`

### Error Handling

- Create custom exceptions extending `RuntimeException` in `exception/` package
- Example: `EmployeeNotFoundException`, `InvalidLeaveRequestException`
- Controllers catch exceptions and return appropriate HTTP status codes

### DTOs

- **Request DTOs**: `*Request` (e.g., `LeaveRequestDTO` or `LeaveRequest`)
- **Response DTOs**: `*Response` or `*Details` (e.g., `LeaveResponse`, `EmployeeDetails`)
- Use Bean Validation annotations: `@NotNull`, `@NotBlank`, `@Min`, `@Max`, etc.

---

## Frontend (Vue/TypeScript) Conventions

### Project Structure

```
frontend/src
├── modules/
│   ├── auth/           # Login, OIDC redirect, token refresh
│   ├── employee/       # Employee profile & onboarding
│   ├── leave/          # Leave management
│   ├── expense/        # Expense claims
│   ├── payroll/        # Payslips
│   ├── reports/        # Reports & analytics
│   └── admin/          # Admin panel
├── components/         # Shared components (forms, tables, cards)
├── layouts/            # Page layouts (AppLayout, AuthLayout)
├── services/           # API clients & utilities (axios instances)
├── stores/             # Pinia stores (auth, user, ui)
├── router/             # Vue Router config
├── types/              # TypeScript types & interfaces
└── assets/             # Static assets, images, styles
```

### Naming Conventions

- **Components**: `PascalCase` file names (e.g., `LeaveForm.vue`, `EmployeeTable.vue`)
- **Files**: kebab-case for non-components (e.g., `auth-service.ts`, `api-client.ts`)
- **Functions**: `camelCase` (e.g., `fetchEmployeeList()`)
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `API_BASE_URL`, `SESSION_TIMEOUT_MS`)
- **Store modules**: `camelCase` (e.g., `authStore`, `userStore`)

### TypeScript Strict Mode

- Enable `strict: true` in `tsconfig.json`
- All function parameters and return types must be typed
- Use `interface` for API responses, `type` for unions/tuples

### Component Best Practices

- Single responsibility: one feature per component
- Props should be typed and documented
- Use `<script setup>` syntax (modern Vue 3)
- Emit custom events with type safety: `defineEmits<{ 'update:modelValue': [value: string] }>()`
- Use `computed()` and `watch()` for reactivity

### API Service Pattern

```typescript
// services/apiClient.ts
export class ApiClient {
  private http = axios.create({ baseURL: API_BASE_URL });

  async get<T>(url: string): Promise<T> {
    const response = await this.http.get<T>(url);
    return response.data;
  }

  // ... other methods
}

// services/leaveService.ts
export class LeaveService {
  constructor(private api: ApiClient) {}

  async getLeaveBalance(): Promise<LeaveBalance> {
    return this.api.get('/leave/balance');
  }
}
```

### State Management (Pinia)

```typescript
export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null);
  const isAuthenticated = computed(() => !!user.value);

  const login = async (credentials) => {
    // ...
  };

  return { user, isAuthenticated, login };
});
```

---

## Database Conventions

### Table Naming

- `snake_case` for table and column names
- Singular names for tables (e.g., `user`, `employee`, `leave_request`, NOT `users`)
- Foreign keys: `<table>_id` (e.g., `employee_id`, `manager_id`)

### Column Conventions

- `id UUID PRIMARY KEY` for all tables
- `created_at TIMESTAMP` for creation time
- `updated_at TIMESTAMP` for last modification
- Boolean columns: `is_<property>` (e.g., `is_active`, `is_approved`)
- Status enums: use `status VARCHAR(50)` with CHECK constraint or separate enum type

### Example Schema

```sql
CREATE TABLE "user" (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  employee_id VARCHAR(50) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL UNIQUE,
  role_id UUID NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (role_id) REFERENCES role(id)
);

CREATE TABLE employee (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  employee_code VARCHAR(50) NOT NULL UNIQUE,
  full_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  phone VARCHAR(20),
  department VARCHAR(100),
  designation VARCHAR(100),
  manager_id UUID,
  joining_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (manager_id) REFERENCES employee(id)
);
```

### Indexes

- Add index on frequently queried columns (e.g., `status`, `employee_id`)
- Foreign keys are automatically indexed in PostgreSQL
- Partial indexes for soft deletes: `CREATE INDEX ON table(id) WHERE deleted_at IS NULL;`

---

## API Conventions

### RESTful Endpoints

- **GET** `/api/v1/leave` — List all leaves
- **GET** `/api/v1/leave/{id}` — Get single leave
- **POST** `/api/v1/leave` — Create new leave
- **PUT** `/api/v1/leave/{id}` — Update leave (full replacement)
- **PATCH** `/api/v1/leave/{id}` — Partial update
- **DELETE** `/api/v1/leave/{id}` — Delete leave

### Request/Response Format

```json
// Success Response (200, 201)
{
  "success": true,
  "data": { ... },
  "message": "Leave request approved successfully"
}

// Error Response (4xx, 5xx)
{
  "success": false,
  "error": "INVALID_REQUEST",
  "message": "Leave type is required",
  "timestamp": "2026-06-08T10:30:00Z"
}
```

### HTTP Status Codes

- **200** — OK (GET, PUT, PATCH)
- **201** — Created (POST)
- **204** — No Content (DELETE)
- **400** — Bad Request (validation error)
- **401** — Unauthorized (no/invalid token)
- **403** — Forbidden (no permission)
- **404** — Not Found
- **409** — Conflict (duplicate entry, state mismatch)
- **500** — Internal Server Error

### Query Parameters

- **Pagination**: `/api/v1/leave?page=1&size=10`
- **Filtering**: `/api/v1/leave?status=PENDING&employee_id=abc123`
- **Sorting**: `/api/v1/leave?sort=created_at:desc,status:asc`

---

## Code Review & Quality

### Before Submitting PR

- [ ] Code compiles/builds without errors
- [ ] All tests pass (unit + integration)
- [ ] Code coverage > 80% for new code
- [ ] No secrets/credentials in code
- [ ] Follows naming conventions
- [ ] Documentation/comments added
- [ ] No dead code or commented-out code
- [ ] Linting passes (ESLint, CheckStyle/Spotbugs)

### Code Review Checklist

- [ ] Logic is correct and handles edge cases
- [ ] No hardcoded values; use constants/config
- [ ] Proper error handling and logging
- [ ] SQL queries are optimized (use indexes, avoid N+1)
- [ ] Security considerations (auth, input validation, CORS)
- [ ] Performance impact assessed
- [ ] Documentation updated

### Testing

- **Unit Tests**: Mock external dependencies, test business logic
- **Integration Tests**: Use Testcontainers for DB/services
- **E2E Tests**: Frontend flow testing with Cypress/Playwright
- Target: >80% code coverage on core modules

---

## Environment Variables

All sensitive config should use environment variables. See `.env.example` for template.

**Do NOT commit `.env`, only `.env.example`.**

---

## Questions?

Refer to this document during code review. When in doubt, ask on Slack or raise a discussion issue.
