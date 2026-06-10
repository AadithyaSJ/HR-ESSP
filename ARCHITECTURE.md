# System Architecture

Comprehensive architecture documentation for the HR Employee Self-Service Portal.

## Table of Contents

- [High-Level Overview](#high-level-overview)
- [System Components](#system-components)
- [Data Flow](#data-flow)
- [Security Architecture](#security-architecture)
- [Database Design](#database-design)
- [API Layers](#api-layers)
- [Scalability & Performance](#scalability--performance)
- [Deployment Architecture](#deployment-architecture)

---

## High-Level Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        End Users                                 │
│              (Employees, Managers, HR Admins)                   │
└──────────┬──────────────────────────────────────────────────┬──┘
           │                                                  │
           │ HTTPS                                            │
           ▼                                                  ▼
┌─────────────────────────────┐              ┌──────────────────────────┐
│   Vue 3 Frontend            │              │   Static Assets (CDN)    │
│   - TypeScript              │              │   - Images, CSS, JS      │
│   - Vuetify UI              │              │   - CloudFront           │
│   - Pinia Store             │              │   - Cache Strategy       │
│   - Real-time (SSE)         │              └──────────────────────────┘
│   Port: 5173 (dev)          │
│   Hosted: CloudFront (prod) │
└──────────┬──────────────────┘
           │
           │ API Calls (JWT in header)
           │
           ▼
┌─────────────────────────────────────────────────────────────────┐
│              API Gateway / Load Balancer (ALB)                   │
│              - SSL/TLS termination                               │
│              - Request throttling & rate limiting               │
│              - CORS configuration                               │
└──────────┬──────────────────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────────┐
│           Spring Boot Backend (ECS Fargate)                      │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ REST Controllers (v1)                                      │ │
│  ├────────────────────────────────────────────────────────────┤ │
│  │ Service Layer (Business Logic)                             │ │
│  ├────────────────────────────────────────────────────────────┤ │
│  │ Repository Layer (Data Access)                             │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  Middleware:                                                     │
│  - JWT Validation (from Keycloak)                              │
│  - Request Logging & Tracing                                   │
│  - Exception Handling                                          │
│  - CORS Filter                                                 │
└──────────┬─────────────────────────┬──────────────┬────────────┘
           │                         │              │
   Flyway  │                         │              │
Migrations │                         │              │
           │                         │              │
     (SQL) │                    (JWT)│ (File Upload)│
           │                         │              │
           ▼                         ▼              ▼
    ┌─────────────┐        ┌────────────────┐  ┌────────────┐
    │ PostgreSQL  │        │   Keycloak     │  │  AWS S3    │
    │ (RDS)       │        │ (OAuth2/OIDC)  │  │(File Store)│
    │             │        │                │  │            │
    │ - Users     │        │ - SAML Login   │  │ - Payslips │
    │ - Employees │        │ - JWT Issue    │  │ - Receipts │
    │ - Leaves    │        │ - User Mgmt    │  │ - Docs     │
    │ - Expenses  │        │                │  │            │
    │ - Payroll   │        │ Port: 8081     │  │ Signed URLs│
    └─────────────┘        └────────────────┘  └────────────┘
```

---

## System Components

### Frontend (Vue 3)

**Responsibilities:**
- User interface rendering
- Form validation (client-side)
- State management (Pinia)
- Real-time updates (Server-Sent Events)
- Local caching & offline support

**Key Modules:**
- **Auth**: Login, token refresh, OAuth redirect handling
- **Employee**: Profile, directory, onboarding
- **Leave**: Request submission, balance view, approval workflow
- **Expense**: Claim submission, receipt upload, tracking
- **Payroll**: Payslip download, tax documents
- **Reports**: Analytics dashboards, exports
- **Admin**: User management, settings, role configuration

**API Client Pattern:**
```typescript
// services/apiClient.ts
export class ApiClient {
  private http = axios.create({
    baseURL: API_BASE_URL,
    headers: {
      'Content-Type': 'application/json'
    }
  });

  // Intercept requests to add JWT
  this.http.interceptors.request.use((config) => {
    const token = getAuthToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  // Handle 401 responses (token expired)
  this.http.interceptors.response.use(
    response => response,
    error => {
      if (error.response?.status === 401) {
        refreshToken().then(() => retryRequest(error.config));
      }
      return Promise.reject(error);
    }
  );
}
```

---

### Backend (Spring Boot 3)

**Responsibilities:**
- REST API implementation
- Business logic execution
- Data validation & persistence
- Authentication & authorization
- Workflow orchestration
- Email & notification dispatch

**Core Layers:**

#### 1. Controller Layer
```
@RestController("/api/v1/leave")
├── GET /balance
├── GET /{id}
├── POST /
├── PUT /{id}
└── DELETE /{id}
```

**Features:**
- Request validation (@Validated)
- JWT extraction from Authorization header
- Response wrapping with success/error format
- HTTP status codes (200, 201, 400, 401, 403, 404, 500)

#### 2. Service Layer
- Business logic implementation
- Transactional operations (@Transactional)
- Cross-module orchestration
- Error handling & logging

**Services:**
- `LeaveService` — Leave calculations, approvals
- `ExpenseService` — Claim processing, receipt handling
- `EmployeeService` — Profile management, hierarchy
- `PayrollService` — Salary calculations, payslip generation
- `NotificationService` — Email & SSE dispatch
- `WorkflowService` — Approval routing, history tracking

#### 3. Repository Layer (Data Access)
- Spring Data JPA interfaces
- Custom queries (@Query)
- Pagination & sorting
- Soft delete support

#### 4. Entity Layer
- JPA annotations (@Entity, @Table, @Column)
- Relationships (OneToMany, ManyToOne, ManyToMany)
- Audit fields (createdAt, updatedAt, createdBy)
- Validation annotations (@NotNull, @Email, etc.)

**Key Entities:**
```
User ──┬─→ Employee ──┬─→ LeaveRequest
       │              ├─→ LeaveBalance
       │              ├─→ ExpenseClaim
       │              └─→ Payslip
       │
       └─→ Role ──┬─→ Permission
                  └─→ WorkflowAction
```

---

### Authentication & Authorization (Keycloak + JWT)

**Authentication Flow:**

```
1. User clicks "Login"
   ↓
2. Frontend redirects to Keycloak
   (http://localhost:8081/auth/realms/hrportal/protocol/openid-connect/auth?...)
   ↓
3. User enters SAML credentials (via IdP)
   ↓
4. Keycloak validates & redirects back with authorization code
   ↓
5. Frontend exchanges code for tokens (access_token, refresh_token, id_token)
   ↓
6. Frontend stores tokens (localStorage/sessionStorage)
   ↓
7. Frontend includes access_token in Authorization header for API calls
   ↓
8. Backend validates JWT signature & claims
   ↓
9. Backend extracts user info (sub, email, roles) from JWT
   ↓
10. Backend returns protected resource
```

**JWT Token Structure:**
```json
{
  "alg": "RS256",
  "typ": "JWT"
}
{
  "exp": 1687856400,
  "iat": 1687856100,
  "auth_time": 1687856050,
  "jti": "3abc...def",
  "iss": "http://localhost:8081/realms/hrportal",
  "aud": "hrportal-web",
  "sub": "12345-abcde",
  "typ": "Bearer",
  "azp": "hrportal-web",
  "session_state": "xyz...",
  "name": "John Doe",
  "email": "john.doe@company.com",
  "email_verified": true,
  "realm_access": {
    "roles": ["employee", "leave-manager"]
  },
  "client_access": {
    "roles": ["manage-profile", "view-payslips"]
  }
}
```

**Authorization Levels:**

```
Admin Panel
└── Manage Users & Roles
    └── Manage Settings
    └── View Audit Logs

HR Manager Dashboard
└── Approve/Reject Leave & Expenses
└── Process Payroll
└── View Department Reports
└── Upload Documents

Manager Dashboard (Team Lead)
└── Approve team's Leave & Expenses
└── View team metrics
└── Manage direct reports

Employee Portal
└── View Own Profile
└── Submit Leave & Expense Requests
└── Download Payslips
└── View Notifications
```

---

### Data Persistence (PostgreSQL)

**Database Characteristics:**
- **RDBMS**: PostgreSQL 16 (ACID compliance, JSON support)
- **ORM**: Spring Data JPA with Hibernate
- **Migrations**: Flyway (versioned SQL scripts)
- **Timezone**: Asia/Kolkata (UTC+5:30)

**Entity Relationships:**

```sql
-- Core Tables

CREATE TABLE "user" (
  id UUID PRIMARY KEY,
  employee_id VARCHAR(50) UNIQUE,
  email VARCHAR(255) UNIQUE,
  keycloak_id VARCHAR(255) UNIQUE,  -- Linked to Keycloak
  status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED'),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE employee (
  id UUID PRIMARY KEY,
  user_id UUID UNIQUE REFERENCES "user",
  employee_code VARCHAR(50) UNIQUE,
  full_name VARCHAR(255),
  department VARCHAR(100),
  designation VARCHAR(100),
  manager_id UUID REFERENCES employee,  -- Self-join for hierarchy
  joining_date DATE,
  salary DECIMAL(12,2),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE leave_type (
  id UUID PRIMARY KEY,
  name VARCHAR(100) UNIQUE,
  max_days_per_year INT,
  requires_manager_approval BOOLEAN,
  accrual_frequency ENUM('MONTHLY', 'QUARTERLY', 'ANNUAL'),
  created_at TIMESTAMP
);

CREATE TABLE leave_balance (
  id UUID PRIMARY KEY,
  employee_id UUID REFERENCES employee,
  leave_type_id UUID REFERENCES leave_type,
  total_days INT,
  used_days INT,
  balance_days INT,
  carry_forward_allowed INT,
  updated_at TIMESTAMP,
  UNIQUE(employee_id, leave_type_id)
);

CREATE TABLE leave_request (
  id UUID PRIMARY KEY,
  employee_id UUID REFERENCES employee,
  leave_type_id UUID REFERENCES leave_type,
  from_date DATE,
  to_date DATE,
  days_requested INT,
  reason TEXT,
  status ENUM('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'CANCELLED'),
  approved_by UUID REFERENCES employee,
  approval_comments TEXT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE expense_claim (
  id UUID PRIMARY KEY,
  employee_id UUID REFERENCES employee,
  amount DECIMAL(12,2),
  category VARCHAR(50),
  description TEXT,
  status ENUM('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'PAID'),
  submitted_by UUID REFERENCES employee,
  approved_by UUID REFERENCES employee,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE receipt (
  id UUID PRIMARY KEY,
  expense_claim_id UUID REFERENCES expense_claim ON DELETE CASCADE,
  s3_key VARCHAR(500),  -- e.g., "receipts/2024/06/claim-123-receipt.pdf"
  file_name VARCHAR(255),
  file_size BIGINT,
  uploaded_at TIMESTAMP
);

CREATE TABLE payslip (
  id UUID PRIMARY KEY,
  employee_id UUID REFERENCES employee,
  month DATE,
  basic DECIMAL(12,2),
  allowances DECIMAL(12,2),
  deductions DECIMAL(12,2),
  net_salary DECIMAL(12,2),
  s3_key VARCHAR(500),  -- S3 location of PDF
  status ENUM('DRAFT', 'GENERATED', 'PUBLISHED', 'DELIVERED'),
  published_at TIMESTAMP,
  created_at TIMESTAMP
);

CREATE TABLE notification (
  id UUID PRIMARY KEY,
  recipient_id UUID REFERENCES "user",
  title VARCHAR(255),
  message TEXT,
  type ENUM('LEAVE_APPROVED', 'EXPENSE_APPROVED', 'PAYSLIP_READY', 'ALERT'),
  is_read BOOLEAN DEFAULT FALSE,
  action_url VARCHAR(500),
  created_at TIMESTAMP
);

CREATE TABLE audit_log (
  id UUID PRIMARY KEY,
  entity_type VARCHAR(100),
  entity_id UUID,
  action ENUM('CREATE', 'UPDATE', 'DELETE'),
  changed_by UUID REFERENCES "user",
  changes JSONB,  -- Old and new values
  created_at TIMESTAMP
);
```

**Indexing Strategy:**

```sql
-- Frequently queried columns
CREATE INDEX idx_user_keycloak_id ON "user"(keycloak_id);
CREATE INDEX idx_employee_department ON employee(department);
CREATE INDEX idx_leave_request_status ON leave_request(status);
CREATE INDEX idx_leave_request_employee_date ON leave_request(employee_id, from_date);
CREATE INDEX idx_expense_claim_status ON expense_claim(status);
CREATE INDEX idx_notification_recipient_read ON notification(recipient_id, is_read);

-- Composite indexes for common queries
CREATE INDEX idx_leave_balance_lookup ON leave_balance(employee_id, leave_type_id);
```

---

### File Storage (AWS S3 / MinIO)

**S3 Bucket Structure:**

```
hrportal-prod/
├── payslips/
│   └── 2024/06/
│       └── emp-123-2024-06.pdf
├── receipts/
│   └── 2024/06/
│       └── expense-456-receipt.pdf
├── documents/
│   └── employee-789/
│       ├── resume.pdf
│       ├── offer_letter.pdf
│       └── id_proof.pdf
└── archives/  # Processed/archived files
```

**Access Pattern:**

```
1. User uploads file (Frontend)
   ↓
2. Backend generates presigned POST URL from S3
   ↓
3. Frontend uploads directly to S3 (CORS enabled)
   ↓
4. S3 stores file & returns key
   ↓
5. Backend stores reference (s3_key, file_name, size) in database
   ↓
6. User requests download
   ↓
7. Backend generates presigned GET URL (time-limited)
   ↓
8. Frontend redirects to signed URL
   ↓
9. User downloads directly from S3
```

**S3 Configuration:**

```properties
# Signed URL expiration: 15 minutes (900 seconds)
spring.aws.s3.expiration=900

# Bucket policy: Block public access
# Enable versioning for audit trail
# Enable encryption at rest (SSE-S3)
# Enable MFA delete (for production)
```

---

### Email & Notifications

**Notification Flow:**

```
Event Triggered (e.g., Leave Approved)
  ↓
NotificationService queues message (async)
  ↓
Message Worker processes queue
  ↓
├─ Send Email via AWS SES
├─ Create In-App Notification (DB)
└─ Push to SSE stream (real-time)
  ↓
User receives:
  1. Real-time SSE update (immediate)
  2. Email notification (within minutes)
  3. In-app badge/toast (on refresh)
```

**Email Templates:**

```
templates/
├── leave_approved.html
├── leave_rejected.html
├── expense_approved.html
├── expense_rejected.html
├── payslip_ready.html
└── onboarding_welcome.html
```

---

### Real-Time Updates (Server-Sent Events)

**SSE Endpoint:**

```
GET /api/v1/events/subscribe

Headers:
  - Authorization: Bearer <JWT>

Response (streaming):
  event: leave_approved
  data: {"leaveId": "123", "status": "APPROVED"}

  event: expense_updated
  data: {"expenseId": "456", "status": "APPROVED"}
```

**Frontend Subscription:**

```typescript
const eventSource = new EventSource('/api/v1/events/subscribe', {
  headers: { 'Authorization': `Bearer ${token}` }
});

eventSource.addEventListener('leave_approved', (event) => {
  const data = JSON.parse(event.data);
  showNotification(`Leave ${data.leaveId} approved!`);
});

eventSource.addEventListener('error', () => {
  eventSource.close();
  // Reconnect with exponential backoff
});
```

---

## Data Flow

### Leave Request Workflow

```
Employee fills form & submits
  ↓
Frontend validates (required fields, dates)
  ↓
Frontend sends POST /api/v1/leave to Backend
  ↓
Backend validates (JWT, business rules)
  ├─ Check leave balance > days_requested
  ├─ Check no overlapping requests
  └─ Check date range valid
  ↓
Backend creates LeaveRequest entity (status: SUBMITTED)
  ↓
Backend posts WorkflowAction (route to manager)
  ↓
Backend queues notification event (async)
  ↓
NotificationService sends:
  1. Email to manager: "Approval needed for John Doe's leave"
  2. In-app notification
  3. SSE event to manager's browser
  ↓
Manager receives real-time notification
  ↓
Manager opens app, reviews, clicks "Approve"
  ↓
Frontend sends PATCH /api/v1/leave/{id} with approval
  ↓
Backend updates LeaveRequest (status: APPROVED, approved_by, approval_comments)
  ↓
Backend updates LeaveBalance (used_days += days_requested)
  ↓
Backend posts WorkflowAction (approval recorded)
  ↓
Backend queues notification event
  ↓
NotificationService sends confirmation to employee
  ↓
Employee receives real-time notification: "Your leave request approved!"
```

---

## Security Architecture

### Authentication & Authorization

**Layers:**

1. **Transport**: HTTPS/TLS 1.3 (enforced)
2. **Authentication**: OAuth2 / OpenID Connect (Keycloak)
3. **Token**: JWT with RS256 signature
4. **Authorization**: Role-based access control (RBAC)

**JWT Validation:**

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

    String bearerToken = extractBearerToken(request);

    if (bearerToken != null) {
      try {
        // Verify signature using Keycloak's public key
        DecodedJWT decodedJWT = jwtVerifier.verify(bearerToken);

        // Extract claims
        String userId = decodedJWT.getSubject();
        List<String> roles = decodedJWT.getClaim("roles").asList(String.class);

        // Set authentication context
        SecurityContextHolder.setContext(new UserContext(userId, roles));
      } catch (JWTVerificationException e) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
      }
    }

    filterChain.doFilter(request, response);
  }
}
```

**Authorization Rules:**

```java
@RestController
@RequestMapping("/api/v1/leave")
public class LeaveController {

  @GetMapping
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR')")
  public List<LeaveRequest> getLeaves() {
    // ...
  }

  @PostMapping("/{id}/approve")
  @PreAuthorize("hasRole('MANAGER')")
  public LeaveRequest approveLeave(@PathVariable UUID id) {
    // Only managers can approve
  }
}
```

### Input Validation

**Frontend:**
- Form validation (Vuetify)
- Data type checking
- Required field enforcement

**Backend:**
- Bean Validation annotations (@NotNull, @Email, @Min, @Max)
- Custom validators
- SQL injection protection (parameterized queries)

**Example:**

```java
@Entity
@Table(name = "leave_request")
public class LeaveRequest {

  @NotNull
  @DateRange(from = "from_date", to = "to_date")
  private LocalDate fromDate;

  @NotNull
  private LocalDate toDate;

  @Min(1)
  @Max(30)
  private Integer daysRequested;

  @NotBlank
  @Size(min = 10, max = 500)
  private String reason;
}
```

### Secrets Management

**Development:**
- `.env` file (gitignored)
- Docker Compose environment variables

**Production:**
- AWS Secrets Manager / Parameter Store
- Environment variables (ECS task definition)
- No secrets in code, images, or VCS

### CORS Configuration

```java
@Configuration
public class CorsConfig {

  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
          .allowedOrigins("https://hrportal.company.com")
          .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
          .allowedHeaders("Authorization", "Content-Type")
          .allowCredentials(true)
          .maxAge(3600);
      }
    };
  }
}
```

---

## Scalability & Performance

### Caching Strategy

```
Layer 1: Frontend (Pinia store)
  - Cached user profile
  - Cached employee list
  - TTL: 5 minutes or manual refresh

Layer 2: Redis (Backend)
  - Cached leave balances
  - Cached org structure
  - TTL: 1 hour (invalidated on update)

Layer 3: Database
  - Source of truth
  - Indexed queries
  - Connection pooling (HikariCP, pool size: 20)

Layer 4: CDN (CloudFront)
  - Static assets
  - TTL: 24 hours
  - Cache invalidation on deploy
```

### Database Optimization

**Query Performance:**
- Use pagination: `?page=0&size=50`
- Lazy loading for relationships
- N+1 query prevention (JPA EntityGraph)
- Query result caching (Spring Cache @Cacheable)

**Index Strategy:**
- Index on foreign keys (automatic)
- Index on status fields (filtering)
- Composite index on frequent where clauses
- Regular ANALYZE command (PostgreSQL optimizer)

### Load Balancing

**Horizontal Scaling:**
```
Load Balancer (ALB)
├── Backend Instance 1 (ECS task)
├── Backend Instance 2 (ECS task)
└── Backend Instance 3 (ECS task)

Auto-scaling policy:
- Min: 2 instances
- Max: 10 instances
- Scale up if CPU > 70%
- Scale down if CPU < 30%
```

---

## Deployment Architecture

### Development

```
Developer Machine
├── Docker Compose
│   ├── PostgreSQL
│   ├── Keycloak
│   ├── MinIO
│   └── Redis
├── Spring Boot (IDE)
└── Vue Dev Server
```

### Staging

```
AWS Account (Staging)
├── VPC (10.0.0.0/16)
│   ├── Public Subnet (ALB)
│   └── Private Subnet
│       ├── ECS Cluster (2 tasks)
│       ├── RDS PostgreSQL
│       ├── ElastiCache Redis
│       └── Keycloak (ECS)
├── S3 (artifacts)
├── ECR (images)
├── CloudWatch (logs)
└── Route53 (DNS)
```

### Production

```
AWS Account (Production)
├── Multi-AZ VPC
│   ├── ALB (cross-AZ)
│   ├── ECS Fargate (4-10 tasks)
│   ├── RDS PostgreSQL (Multi-AZ)
│   ├── ElastiCache (cluster mode)
│   └── Keycloak HA
├── CloudFront (CDN)
├── WAF (API protection)
├── S3 (versioning, encryption)
├── CloudWatch (metrics, alarms)
├── Backup (automated, tested)
└── Disaster Recovery
```

---

## Performance Targets

| Metric | Target |
|--------|--------|
| API Response Time (p95) | <500ms |
| API Response Time (p99) | <1s |
| Database Query Time (p95) | <100ms |
| Frontend Load Time | <2s |
| Login to Portal | <3s |
| Concurrent Users | 5,000+ |
| Uptime | 99.9% |
| Recovery Time (RTO) | <30 min |
| Data Loss (RPO) | <5 min |

---

**Last Updated**: June 2024  
**Architecture Version**: 1.0
