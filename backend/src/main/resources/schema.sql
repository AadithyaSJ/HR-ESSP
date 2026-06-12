-- PostgreSQL Relational Schema for HR ESSP

-- Drop tables in dependency order if they exist
DROP TABLE IF EXISTS workflow_actions CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS payslips CASCADE;
DROP TABLE IF EXISTS expense_receipts CASCADE;
DROP TABLE IF EXISTS expense_claims CASCADE;
DROP TABLE IF EXISTS leave_requests CASCADE;
DROP TABLE IF EXISTS leave_balances CASCADE;
DROP TABLE IF EXISTS employees CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS roles CASCADE;

-- 1. Roles table
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(100) UNIQUE NOT NULL
);

-- 2. Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id VARCHAR(100),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role_id BIGINT REFERENCES roles(id) ON DELETE RESTRICT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Employees table
CREATE TABLE employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_code VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(50),
    address VARCHAR(1000),
    department VARCHAR(255),
    designation VARCHAR(255),
    bank_account_no VARCHAR(100),
    bank_ifsc VARCHAR(50),
    bank_name VARCHAR(255),
    emergency_name VARCHAR(255),
    emergency_relation VARCHAR(100),
    emergency_phone VARCHAR(50),
    onboarding_percent INT DEFAULT 0,
    manager_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    joining_date DATE NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    salary NUMERIC(15,2),
    salary_band VARCHAR(100)
);

-- 4. Leave Balances table
CREATE TABLE leave_balances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID REFERENCES employees(id) ON DELETE CASCADE,
    leave_type VARCHAR(100) NOT NULL,
    balance INT NOT NULL DEFAULT 0,
    year INT NOT NULL
);

-- 5. Leave Requests table
CREATE TABLE leave_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID REFERENCES employees(id) ON DELETE CASCADE,
    leave_type VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT,
    status VARCHAR(50) DEFAULT 'PENDING',
    manager_comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. Expense Claims table
CREATE TABLE expense_claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID REFERENCES employees(id) ON DELETE CASCADE,
    category VARCHAR(100) NOT NULL,
    amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    description TEXT,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. Expense Receipts table
CREATE TABLE expense_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id UUID REFERENCES expense_claims(id) ON DELETE CASCADE,
    s3_url VARCHAR(1000) NOT NULL,
    file_name VARCHAR(255) NOT NULL
);

-- 8. Payslips table
CREATE TABLE payslips (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID REFERENCES employees(id) ON DELETE CASCADE,
    month VARCHAR(20) NOT NULL,
    year INT NOT NULL,
    gross_pay NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    deduction NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    net_pay NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    pdf_url VARCHAR(1000),
    published BOOLEAN DEFAULT FALSE
);

-- 9. Notifications table
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 10. Workflow Actions table
CREATE TABLE workflow_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    action_by UUID REFERENCES employees(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    comments TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Performance Indexes
CREATE INDEX idx_employees_code ON employees(employee_code);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_leave_requests_employee ON leave_requests(employee_id);
CREATE INDEX idx_expense_claims_employee ON expense_claims(employee_id);
CREATE INDEX idx_payslips_employee_month ON payslips(employee_id, month, year);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read);
