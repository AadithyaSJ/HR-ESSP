import { defineStore } from 'pinia';
import { ref } from 'vue';
import { apiRequest } from '../utils/api';

export const useHrStore = defineStore('hr', () => {
  // --- SEED EMPLOYEES ---
  const employees = ref([
    {
      id: 'emp-101',
      employeeCode: 'EMP2026101',
      fullName: 'Jane Doe',
      email: 'jane.doe@company.com',
      phone: '+91 98765 43210',
      address: '123 Cyber Towers, Hitec City, Hyderabad, India',
      department: 'Engineering',
      designation: 'Senior Frontend Developer',
      managerId: 'emp-102',
      managerName: 'Sarah Jenkins',
      managerContact: 'sarah.j@company.com',
      joiningDate: '2024-03-15',
      employmentType: 'Full-time',
      role: 'EMPLOYEE',
      salary: 1400000,
      salaryBand: 'Band 3 (L3-L4)',
      status: 'ACTIVE',
      photo: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150',
      onboardingPercent: 100,
      onboardingTasks: [
        { id: 1, title: 'Acknowledge company policy documents', done: true },
        { id: 2, title: 'Upload identity proof (Aadhaar / Passport)', done: true },
        { id: 3, title: 'Fill personal details form', done: true },
        { id: 4, title: 'Submit bank account details', done: true },
        { id: 5, title: 'Add emergency contact', done: true }
      ],
      bankDetails: {
        accountNo: '30948573620',
        ifsc: 'SBIN0003049',
        bankName: 'State Bank of India'
      },
      emergencyContact: {
        name: 'John Doe Sr.',
        relation: 'Father',
        phone: '+91 98765 00001'
      },
      documents: [
        { name: 'Offer_Letter.pdf', size: '420 KB', date: '2024-03-01', type: 'HR' },
        { name: 'Aadhaar_Card.pdf', size: '210 KB', date: '2024-03-15', type: 'Identity' },
        { name: 'Degree_Certificate.pdf', size: '1.2 MB', date: '2024-03-15', type: 'Education' }
      ]
    },
    {
      id: 'emp-102',
      employeeCode: 'EMP2023102',
      fullName: 'Sarah Jenkins',
      email: 'sarah.j@company.com',
      phone: '+91 98765 43211',
      address: '456 Jubilee Hills, Hyderabad, India',
      department: 'Engineering',
      designation: 'Engineering Manager',
      managerId: 'emp-103',
      managerName: 'David Vance',
      managerContact: 'david.v@company.com',
      joiningDate: '2023-01-10',
      employmentType: 'Full-time',
      role: 'MANAGER',
      salary: 2800000,
      salaryBand: 'Band 4 (L5-L6)',
      status: 'ACTIVE',
      photo: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150',
      onboardingPercent: 100,
      onboardingTasks: [],
      bankDetails: {
        accountNo: '48375620194',
        ifsc: 'HDFC0000201',
        bankName: 'HDFC Bank'
      },
      emergencyContact: {
        name: 'Robert Jenkins',
        relation: 'Spouse',
        phone: '+91 98765 11112'
      },
      documents: [
        { name: 'Offer_Letter.pdf', size: '430 KB', date: '2023-01-01', type: 'HR' }
      ]
    },
    {
      id: 'emp-103',
      employeeCode: 'EMP2020103',
      fullName: 'David Vance',
      email: 'david.v@company.com',
      phone: '+91 98765 43212',
      address: '789 Gachibowli, Hyderabad, India',
      department: 'Executive',
      designation: 'VP of Engineering',
      managerId: null,
      managerName: null,
      managerContact: null,
      joiningDate: '2020-08-01',
      employmentType: 'Full-time',
      role: 'MANAGER',
      salary: 4500000,
      salaryBand: 'Band 5 (L7+)',
      status: 'ACTIVE',
      photo: 'https://images.unsplash.com/photo-1560250097-0b93528c311a?w=150',
      onboardingPercent: 100,
      onboardingTasks: [],
      bankDetails: {
        accountNo: '10928374650',
        ifsc: 'ICIC0000001',
        bankName: 'ICICI Bank'
      },
      emergencyContact: {
        name: 'Elena Vance',
        relation: 'Spouse',
        phone: '+91 98765 22223'
      },
      documents: [
        { name: 'Employment_Agreement.pdf', size: '1.5 MB', date: '2020-07-15', type: 'HR' }
      ]
    },
    {
      id: 'emp-104',
      employeeCode: 'EMP2025104',
      fullName: 'Vikram Mehta',
      email: 'vikram.m@company.com',
      phone: '+91 98765 43213',
      address: 'Secunderabad, India',
      department: 'HR',
      designation: 'HR Lead',
      managerId: 'emp-103',
      managerName: 'David Vance',
      managerContact: 'david.v@company.com',
      joiningDate: '2025-05-01',
      employmentType: 'Full-time',
      role: 'HR_ADMIN',
      salary: 1500000,
      salaryBand: 'Band 3 (L3-L4)',
      status: 'ACTIVE',
      photo: 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150',
      onboardingPercent: 80,
      onboardingTasks: [
        { id: 1, title: 'Acknowledge company policy documents', done: true },
        { id: 2, title: 'Upload identity proof (Aadhaar / Passport)', done: true },
        { id: 3, title: 'Fill personal details form', done: true },
        { id: 4, title: 'Submit bank account details', done: true },
        { id: 5, title: 'Add emergency contact', done: false }
      ],
      bankDetails: {
        accountNo: '99887766554',
        ifsc: 'KKBK0000123',
        bankName: 'Kotak Mahindra'
      },
      emergencyContact: {
        name: 'Kavita Mehta',
        relation: 'Spouse',
        phone: '+91 98765 33334'
      },
      documents: [
        { name: 'Offer_Letter.pdf', size: '410 KB', date: '2025-04-10', type: 'HR' }
      ]
    },
    {
      id: 'emp-105',
      employeeCode: 'EMP2024105',
      fullName: 'Alice Sterling',
      email: 'alice.s@company.com',
      phone: '+91 98765 43214',
      address: 'Financial District, Hyderabad, India',
      department: 'Finance',
      designation: 'Finance Controller',
      managerId: 'emp-103',
      managerName: 'David Vance',
      managerContact: 'david.v@company.com',
      joiningDate: '2024-11-01',
      employmentType: 'Full-time',
      role: 'FINANCE_ADMIN',
      salary: 1800000,
      salaryBand: 'Band 3 (L3-L4)',
      status: 'ACTIVE',
      photo: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150',
      onboardingPercent: 100,
      onboardingTasks: [],
      bankDetails: {
        accountNo: '88776655443',
        ifsc: 'AXIS0000101',
        bankName: 'Axis Bank'
      },
      emergencyContact: {
        name: 'George Sterling',
        relation: 'Father',
        phone: '+91 98765 44445'
      },
      documents: [
        { name: 'Offer_Letter.pdf', size: '425 KB', date: '2024-10-15', type: 'HR' }
      ]
    }
  ]);

  // --- SEED LEAVES ---
  const leaveRequests = ref([
    {
      id: 'lv-1',
      employeeCode: 'EMP2026101',
      fullName: 'Jane Doe',
      department: 'Engineering',
      leaveType: 'Annual',
      fromDate: '2026-06-15',
      toDate: '2026-06-18',
      daysRequested: 4,
      reason: 'Family trip to Munnar.',
      status: 'PENDING',
      approvedBy: null,
      comments: null,
      createdAt: '2026-06-08T10:15:30Z'
    },
    {
      id: 'lv-2',
      employeeCode: 'EMP2026101',
      fullName: 'Jane Doe',
      department: 'Engineering',
      leaveType: 'Sick',
      fromDate: '2026-05-10',
      toDate: '2026-05-11',
      daysRequested: 2,
      reason: 'Fever and cold. Bed rest recommended.',
      status: 'APPROVED',
      approvedBy: 'Sarah Jenkins',
      comments: 'Take care and recover well!',
      createdAt: '2026-05-10T08:00:00Z'
    },
    {
      id: 'lv-3',
      employeeCode: 'EMP2025104',
      fullName: 'Vikram Mehta',
      department: 'HR',
      leaveType: 'Casual',
      fromDate: '2026-06-25',
      toDate: '2026-06-25',
      daysRequested: 1,
      reason: 'Personal work at the bank.',
      status: 'PENDING',
      approvedBy: null,
      comments: null,
      createdAt: '2026-06-09T09:30:00Z'
    }
  ]);

  const leaveBalances = ref({
    'EMP2026101': [
      { type: 'Annual', accrued: 20, used: 4, remaining: 16, carryForward: 5 },
      { type: 'Casual', accrued: 12, used: 2, remaining: 10, carryForward: 0 },
      { type: 'Sick', accrued: 10, used: 2, remaining: 8, carryForward: 2 },
      { type: 'Maternity', accrued: 180, used: 0, remaining: 180, carryForward: 0 },
      { type: 'Paternity', accrued: 15, used: 0, remaining: 15, carryForward: 0 }
    ],
    'EMP2023102': [
      { type: 'Annual', accrued: 25, used: 5, remaining: 20, carryForward: 8 },
      { type: 'Casual', accrued: 12, used: 0, remaining: 12, carryForward: 0 },
      { type: 'Sick', accrued: 10, used: 1, remaining: 9, carryForward: 3 }
    ],
    'EMP2025104': [
      { type: 'Annual', accrued: 20, used: 0, remaining: 20, carryForward: 0 },
      { type: 'Casual', accrued: 12, used: 1, remaining: 11, carryForward: 0 },
      { type: 'Sick', accrued: 10, used: 0, remaining: 10, carryForward: 0 }
    ]
  });

  const leaveTypes = ref([
    { id: 'lt-1', name: 'Annual', accrualFrequency: 'MONTHLY', maxBalance: 30, carryForwardLimit: 10, eligibility: 'All Employees' },
    { id: 'lt-2', name: 'Casual', accrualFrequency: 'QUARTERLY', maxBalance: 12, carryForwardLimit: 0, eligibility: 'All Employees' },
    { id: 'lt-3', name: 'Sick', accrualFrequency: 'MONTHLY', maxBalance: 12, carryForwardLimit: 5, eligibility: 'All Employees' },
    { id: 'lt-4', name: 'Maternity', accrualFrequency: 'ANNUAL', maxBalance: 180, carryForwardLimit: 0, eligibility: 'Female Employees' },
    { id: 'lt-5', name: 'Paternity', accrualFrequency: 'ANNUAL', maxBalance: 15, carryForwardLimit: 0, eligibility: 'Male Employees' }
  ]);

  const publicHolidays = ref([
    { id: 'h-1', country: 'India', name: 'New Year', date: '2026-01-01', mandatory: true },
    { id: 'h-2', country: 'India', name: 'Republic Day', date: '2026-01-26', mandatory: true },
    { id: 'h-3', country: 'India', name: 'Independence Day', date: '2026-08-15', mandatory: true },
    { id: 'h-4', country: 'India', name: 'Gandhi Jayanti', date: '2026-10-02', mandatory: true },
    { id: 'h-5', country: 'India', name: 'Diwali', date: '2026-11-09', mandatory: true },
    { id: 'h-6', country: 'USA', name: 'Independence Day', date: '2026-07-04', mandatory: true },
    { id: 'h-7', country: 'UK', name: 'Christmas', date: '2026-12-25', mandatory: true }
  ]);

  // --- SEED EXPENSES ---
  const expenseClaims = ref([
    {
      id: 'exp-1',
      employeeCode: 'EMP2026101',
      fullName: 'Jane Doe',
      department: 'Engineering',
      category: 'Meals',
      amount: 1500,
      currency: 'INR',
      date: '2026-06-01',
      description: 'Client lunch with DotSolution technical architect.',
      receiptName: 'receipt_meals_0601.pdf',
      status: 'PENDING',
      timeline: [
        { status: 'SUBMITTED', title: 'Submitted by Jane Doe', timestamp: '2026-06-01T14:20:00Z' }
      ],
      paymentRef: null
    },
    {
      id: 'exp-2',
      employeeCode: 'EMP2026101',
      fullName: 'Jane Doe',
      department: 'Engineering',
      category: 'Travel',
      amount: 8500,
      currency: 'INR',
      date: '2026-05-12',
      description: 'Flight tickets to Bengaluru for internal tech conference.',
      receiptName: 'ticket_bgl_0512.pdf',
      status: 'PAID',
      timeline: [
        { status: 'SUBMITTED', title: 'Submitted by Jane Doe', timestamp: '2026-05-12T10:00:00Z' },
        { status: 'APPROVED_MANAGER', title: 'Approved by Sarah Jenkins (Manager)', timestamp: '2026-05-13T09:15:00Z' },
        { status: 'APPROVED_FINANCE', title: 'Approved by Alice Sterling (Finance)', timestamp: '2026-05-14T11:30:00Z' },
        { status: 'PAID', title: 'Marked Paid by Alice Sterling', timestamp: '2026-05-15T16:00:00Z' }
      ],
      paymentRef: 'TXN-9847582049'
    },
    {
      id: 'exp-3',
      employeeCode: 'EMP2023102',
      fullName: 'Sarah Jenkins',
      department: 'Engineering',
      category: 'Accommodation',
      amount: 4500,
      currency: 'INR',
      date: '2026-06-02',
      description: 'Hotel stay during Bengaluru conference.',
      receiptName: 'hotel_bill_0602.pdf',
      status: 'APPROVED_MANAGER',
      timeline: [
        { status: 'SUBMITTED', title: 'Submitted by Sarah Jenkins', timestamp: '2026-06-02T18:00:00Z' },
        { status: 'APPROVED_MANAGER', title: 'Approved by David Vance (VP)', timestamp: '2026-06-03T11:00:00Z' }
      ],
      paymentRef: null
    }
  ]);

  const expenseLimits = ref([
    { id: 'lim-1', category: 'Meals', maxAmount: 2000, frequency: 'PER_DAY', grade: 'All Grades' },
    { id: 'lim-2', category: 'Travel', maxAmount: 15000, frequency: 'PER_TRIP', grade: 'All Grades' },
    { id: 'lim-3', category: 'Accommodation', maxAmount: 8000, frequency: 'PER_NIGHT', grade: 'Grade L5+' },
    { id: 'lim-4', category: 'Accommodation', maxAmount: 5000, frequency: 'PER_NIGHT', grade: 'Grade L1-L4' }
  ]);

  const currencyRates = ref([
    { code: 'INR', rate: 1.0, symbol: '₹' },
    { code: 'USD', rate: 83.5, symbol: '$' },
    { code: 'EUR', rate: 89.8, symbol: '€' },
    { code: 'GBP', rate: 106.2, symbol: '£' }
  ]);

  // --- SEED PAYSLIPS ---
  const payslips = ref([
    {
      id: 'ps-1',
      employeeCode: 'EMP2026101',
      month: '2026-05',
      grossPay: 116666,
      allowances: 16666,
      deductions: 12500,
      netSalary: 120832,
      basic: 70000,
      pf: 8400,
      tax: 4100,
      status: 'PUBLISHED',
      publishedAt: '2026-05-31T18:00:00Z'
    },
    {
      id: 'ps-2',
      employeeCode: 'EMP2026101',
      month: '2026-04',
      grossPay: 116666,
      allowances: 16666,
      deductions: 12500,
      netSalary: 120832,
      basic: 70000,
      pf: 8400,
      tax: 4100,
      status: 'PUBLISHED',
      publishedAt: '2026-04-30T18:00:00Z'
    },
    {
      id: 'ps-3',
      employeeCode: 'EMP2023102',
      month: '2026-05',
      grossPay: 233333,
      allowances: 33333,
      deductions: 25000,
      netSalary: 241666,
      basic: 140000,
      pf: 16800,
      tax: 8200,
      status: 'PUBLISHED',
      publishedAt: '2026-05-31T18:00:00Z'
    }
  ]);

  const payrollBatches = ref([
    { id: 'b-1', month: '2026-05', uploadDate: '2026-05-30', totalEmployees: 5, status: 'PUBLISHED', csvName: 'payroll_may_2026.csv' },
    { id: 'b-2', month: '2026-04', uploadDate: '2026-04-29', totalEmployees: 5, status: 'PUBLISHED', csvName: 'payroll_april_2026.csv' }
  ]);

  // --- SEED NOTIFICATIONS ---
  const notifications = ref([
    { id: 'n-1', title: 'Welcome to ESSP!', message: 'Acknowledge company policy in your onboarding checklist.', type: 'System', isRead: false, createdAt: '2026-06-08T09:00:00Z', url: '/onboarding' },
    { id: 'n-2', title: 'Payslip Published', message: 'Your payslip for May 2026 is now available.', type: 'Payslip', isRead: true, createdAt: '2026-05-31T18:05:00Z', url: '/payslip' },
    { id: 'n-3', title: 'Leave Approved', message: 'Your Sick leave request for 2026-05-10 has been approved.', type: 'Leave', isRead: true, createdAt: '2026-05-13T09:16:00Z', url: '/leave' }
  ]);

  const notificationPreferences = ref({
    emailEnabled: { Leave: true, Expense: true, Payslip: true, System: true },
    inAppEnabled: { Leave: true, Expense: true, Payslip: true, System: true },
    digestFrequency: 'immediate'
  });

  // --- SEED ADMIN DATA ---
  const systemSettings = ref({
    sessionTimeoutMin: 20,
    samlProviderUrl: 'https://identity.okta.com/app/exchrportal/sso/saml',
    samlCertificate: 'MIIDojCCAoqgAwIBAgIGAX7q... [Okta cert]',
    expenseModuleEnabled: true,
    leaveModuleEnabled: true,
    payrollModuleEnabled: true,
    version: 'v1.4.2-RELEASE',
    environment: 'Staging (AWS ECS Fargate)'
  });

  const emailTemplates = ref([
    { id: 't-1', name: 'Leave Approved Notification', subject: 'Leave Request Approved - {{employee_name}}', body: '<p>Dear {{employee_name}},</p><p>Your request for {{leave_type}} leave from {{from_date}} to {{to_date}} has been <b>APPROVED</b> by {{manager_name}}.</p><p>Comments: {{comments}}</p>' },
    { id: 't-2', name: 'Leave Rejected Notification', subject: 'Leave Request Update - {{employee_name}}', body: '<p>Dear {{employee_name}},</p><p>Your request for {{leave_type}} leave from {{from_date}} to {{to_date}} has been <b>REJECTED</b> by {{manager_name}}.</p><p>Reason: {{reason}}</p>' },
    { id: 't-3', name: 'Onboarding Welcome Email', subject: 'Welcome to the Team, {{employee_name}}!', body: '<h2>Welcome {{employee_name}}!</h2><p>We are thrilled to welcome you. Please log in to the HR ESSP Portal and complete your onboarding checklist.</p>' }
  ]);

  const schedulerJobs = ref([
    { id: 'job-1', name: 'LeaveAccrualJob', cron: '0 0 1 * *', lastRun: '2026-06-01 00:00:05', duration: '1.2s', status: 'SUCCESS', active: true },
    { id: 'job-2', name: 'SessionCleanupJob', cron: '0 */4 * * *', lastRun: '2026-06-09 16:00:01', duration: '0.8s', status: 'SUCCESS', active: true },
    { id: 'job-3', name: 'DigestEmailJob', cron: '0 18 * * 5', lastRun: '2026-06-05 18:00:12', duration: '14.5s', status: 'SUCCESS', active: true }
  ]);

  const auditLogs = ref([
    { id: 'l-1', timestamp: '2026-06-09T17:30:10Z', user: 'jane.doe@company.com', module: 'Auth', action: 'LOGIN', details: 'Successful local login' },
    { id: 'l-2', timestamp: '2026-06-08T10:15:30Z', user: 'jane.doe@company.com', module: 'Leave', action: 'APPLY_LEAVE', details: 'Applied for Annual Leave: 2026-06-15 to 2026-06-18' },
    { id: 'l-3', timestamp: '2026-05-31T18:00:00Z', user: 'vikram.m@company.com', module: 'Payroll', action: 'PUBLISH_BATCH', details: 'Published batch for month May 2026' }
  ]);

  // --- ACTIONS (MUTATIONS) ---
  function addLog(userEmail, module, action, details) {
    auditLogs.value.unshift({
      id: `l-${Date.now()}`,
      timestamp: new Date().toISOString(),
      user: userEmail,
      module,
      action,
      details
    });
  }

  function addNotification(title, message, type, url = '') {
    notifications.value.unshift({
      id: `n-${Date.now()}`,
      title,
      message,
      type,
      isRead: false,
      createdAt: new Date().toISOString(),
      url
    });
  }

  // Employee actions
  function updateEmployeeProfile(empId, data, activeUserEmail) {
    const idx = employees.value.findIndex(e => e.id === empId);
    if (idx !== -1) {
      employees.value[idx] = {
        ...employees.value[idx],
        phone: data.phone || employees.value[idx].phone,
        address: data.address || employees.value[idx].address,
        emergencyContact: {
          ...employees.value[idx].emergencyContact,
          ...data.emergencyContact
        },
        bankDetails: {
          ...employees.value[idx].bankDetails,
          ...data.bankDetails
        }
      };
      
      // Document upload
      if (data.newDocument) {
        employees.value[idx].documents.push(data.newDocument);
      }
      
      addLog(activeUserEmail, 'Employee', 'UPDATE_PROFILE', `Updated profile details for employee ID ${empId}`);
      return true;
    }
    return false;
  }

  function toggleOnboardingTask(empId, taskId, activeUserEmail) {
    const emp = employees.value.find(e => e.id === empId);
    if (emp && emp.onboardingTasks) {
      const t = emp.onboardingTasks.find(x => x.id === taskId);
      if (t) {
        t.done = !t.done;
        
        // Recalculate percent
        const doneCount = emp.onboardingTasks.filter(x => x.done).length;
        emp.onboardingPercent = Math.round((doneCount / emp.onboardingTasks.length) * 100);
        
        addLog(activeUserEmail, 'Onboarding', 'TOGGLE_TASK', `Toggled onboarding task "${t.title}" to ${t.done}`);
      }
    }
  }

  // Leave actions
  function applyLeave(data, activeUserEmail) {
    const newReq = {
      id: `lv-${Date.now()}`,
      employeeCode: data.employeeCode,
      fullName: data.fullName,
      department: data.department,
      leaveType: data.leaveType,
      fromDate: data.fromDate,
      toDate: data.toDate,
      daysRequested: parseInt(data.daysRequested),
      reason: data.reason,
      status: 'PENDING',
      approvedBy: null,
      comments: null,
      createdAt: new Date().toISOString()
    };
    
    leaveRequests.value.unshift(newReq);
    
    addLog(activeUserEmail, 'Leave', 'APPLY_LEAVE', `Applied for ${data.leaveType} leave: ${data.fromDate} to ${data.toDate}`);
    addNotification('Leave Submitted', `Leave request for ${data.daysRequested} days has been submitted.`, 'Leave', '/leave');
    return newReq;
  }

  function cancelLeaveRequest(reqId, activeUserEmail) {
    const req = leaveRequests.value.find(r => r.id === reqId);
    if (req && req.status === 'PENDING') {
      req.status = 'CANCELLED';
      addLog(activeUserEmail, 'Leave', 'CANCEL_LEAVE', `Cancelled leave request ID ${reqId}`);
      return true;
    }
    return false;
  }

  function approveLeaveRequest(reqId, managerName, comment, activeUserEmail) {
    const req = leaveRequests.value.find(r => r.id === reqId);
    if (req && req.status === 'PENDING') {
      req.status = 'APPROVED';
      req.approvedBy = managerName;
      req.comments = comment;
      
      // Deduct balance
      const empBalances = leaveBalances.value[req.employeeCode];
      if (empBalances) {
        const bal = empBalances.find(b => b.type === req.leaveType);
        if (bal) {
          bal.used += req.daysRequested;
          bal.remaining = Math.max(0, bal.remaining - req.daysRequested);
        }
      }
      
      addLog(activeUserEmail, 'Leave', 'APPROVE_LEAVE', `Approved leave request ID ${reqId}`);
      
      // Create notification for employee
      addNotification('Leave Approved', `Your leave request from ${req.fromDate} has been APPROVED.`, 'Leave', '/leave');
      return true;
    }
    return false;
  }

  function rejectLeaveRequest(reqId, managerName, reason, activeUserEmail) {
    const req = leaveRequests.value.find(r => r.id === reqId);
    if (req && req.status === 'PENDING') {
      req.status = 'REJECTED';
      req.approvedBy = managerName;
      req.comments = reason;
      
      addLog(activeUserEmail, 'Leave', 'REJECT_LEAVE', `Rejected leave request ID ${reqId}. Reason: ${reason}`);
      addNotification('Leave Rejected', `Your leave request from ${req.fromDate} has been REJECTED.`, 'Leave', '/leave');
      return true;
    }
    return false;
  }

  // Expense actions
  function submitExpenseClaim(data, activeUserEmail) {
    const newClaim = {
      id: `exp-${Date.now()}`,
      employeeCode: data.employeeCode,
      fullName: data.fullName,
      department: data.department,
      category: data.category,
      amount: parseFloat(data.amount),
      currency: data.currency,
      date: data.date,
      description: data.description,
      receiptName: data.receiptName || 'uploaded_receipt.pdf',
      status: 'PENDING',
      timeline: [
        { status: 'SUBMITTED', title: `Submitted by ${data.fullName}`, timestamp: new Date().toISOString() }
      ],
      paymentRef: null
    };

    expenseClaims.value.unshift(newClaim);
    
    addLog(activeUserEmail, 'Expense', 'SUBMIT_CLAIM', `Submitted expense claim of ${data.currency} ${data.amount} for ${data.category}`);
    addNotification('Expense Submitted', `Expense claim for ${data.currency} ${data.amount} submitted.`, 'Expense', '/expense');
    return newClaim;
  }

  function cancelExpenseClaim(claimId, activeUserEmail) {
    const c = expenseClaims.value.find(cl => cl.id === claimId);
    if (c && c.status === 'PENDING') {
      expenseClaims.value = expenseClaims.value.filter(cl => cl.id !== claimId);
      addLog(activeUserEmail, 'Expense', 'CANCEL_CLAIM', `Cancelled pending claim ID ${claimId}`);
      return true;
    }
    return false;
  }

  function approveExpenseManager(claimId, managerName, activeUserEmail) {
    const c = expenseClaims.value.find(cl => cl.id === claimId);
    if (c && c.status === 'PENDING') {
      c.status = 'APPROVED_MANAGER';
      c.timeline.push({
        status: 'APPROVED_MANAGER',
        title: `Approved by ${managerName} (Manager)`,
        timestamp: new Date().toISOString()
      });
      addLog(activeUserEmail, 'Expense', 'APPROVE_EXPENSE_MANAGER', `Manager approved expense ID ${claimId}`);
      addNotification('Expense Manager Approved', `Your expense claim ID ${claimId} was approved by manager.`, 'Expense', '/expense');
      return true;
    }
    return false;
  }

  function approveExpenseFinance(claimId, financeName, payRef, activeUserEmail) {
    const c = expenseClaims.value.find(cl => cl.id === claimId);
    if (c && c.status === 'APPROVED_MANAGER') {
      c.status = 'APPROVED_FINANCE';
      c.paymentRef = payRef;
      c.timeline.push({
        status: 'APPROVED_FINANCE',
        title: `Approved by ${financeName} (Finance Admin)`,
        timestamp: new Date().toISOString()
      });
      addLog(activeUserEmail, 'Expense', 'APPROVE_EXPENSE_FINANCE', `Finance approved expense ID ${claimId}. Pay Ref: ${payRef}`);
      return true;
    }
    return false;
  }

  function rejectExpense(claimId, reviewerName, reason, activeUserEmail) {
    const c = expenseClaims.value.find(cl => cl.id === claimId);
    if (c && (c.status === 'PENDING' || c.status === 'APPROVED_MANAGER')) {
      c.status = 'REJECTED';
      c.timeline.push({
        status: 'REJECTED',
        title: `Rejected by ${reviewerName}. Reason: ${reason}`,
        timestamp: new Date().toISOString()
      });
      addLog(activeUserEmail, 'Expense', 'REJECT_EXPENSE', `Rejected claim ID ${claimId}. Reason: ${reason}`);
      addNotification('Expense Rejected', `Your expense claim ID ${claimId} was rejected.`, 'Expense', '/expense');
      return true;
    }
    return false;
  }

  function markExpensePaid(claimId, activeUserEmail) {
    const c = expenseClaims.value.find(cl => cl.id === claimId);
    if (c && c.status === 'APPROVED_FINANCE') {
      c.status = 'PAID';
      c.timeline.push({
        status: 'PAID',
        title: `Marked Paid`,
        timestamp: new Date().toISOString()
      });
      addLog(activeUserEmail, 'Expense', 'PAY_EXPENSE', `Marked claim ID ${claimId} as Paid`);
      addNotification('Expense Paid', `Your expense claim ID ${claimId} has been Paid.`, 'Expense', '/expense');
      return true;
    }
    return false;
  }

  // Payroll / Payslip Actions
  function processPayrollUpload(csvContent, month, activeUserEmail) {
    // Basic CSV mock parser
    // Check headers
    const lines = csvContent.split('\n').map(l => l.trim()).filter(Boolean);
    if (lines.length < 2) return { success: false, errors: ['CSV contains no data rows'] };
    
    const headers = lines[0].split(',').map(h => h.trim().toLowerCase());
    const reqHeaders = ['employee_id', 'basic', 'allowances', 'deductions'];
    const missing = reqHeaders.filter(h => !headers.includes(h));
    
    if (missing.length > 0) {
      return { success: false, errors: [`Missing required columns: ${missing.join(', ')}`] };
    }
    
    const empIdIdx = headers.indexOf('employee_id');
    const basicIdx = headers.indexOf('basic');
    const allowancesIdx = headers.indexOf('allowances');
    const deductionsIdx = headers.indexOf('deductions');
    
    const errors = [];
    const validRows = [];
    
    for (let i = 1; i < lines.length; i++) {
      const parts = lines[i].split(',').map(p => p.trim());
      if (parts.length < headers.length) {
        errors.push(`Row ${i + 1}: Incomplete data`);
        continue;
      }
      
      const empCode = parts[empIdIdx];
      const basic = parseFloat(parts[basicIdx]);
      const allowances = parseFloat(parts[allowancesIdx]);
      const deductions = parseFloat(parts[deductionsIdx]);
      
      const emp = employees.value.find(e => e.employeeCode === empCode);
      if (!emp) {
        errors.push(`Row ${i + 1}: Employee code "${empCode}" not found in system`);
        continue;
      }
      
      if (isNaN(basic) || basic <= 0) errors.push(`Row ${i + 1}: Invalid basic salary`);
      if (isNaN(allowances) || allowances < 0) errors.push(`Row ${i + 1}: Invalid allowances`);
      if (isNaN(deductions) || deductions < 0) errors.push(`Row ${i + 1}: Invalid deductions`);
      
      if (errors.length === 0) {
        validRows.push({ empCode, basic, allowances, deductions });
      }
    }
    
    if (errors.length > 0) {
      return { success: false, errors };
    }
    
    // Write draft payslips
    const batchId = `b-${Date.now()}`;
    const newBatch = {
      id: batchId,
      month,
      uploadDate: new Date().toISOString().split('T')[0],
      totalEmployees: validRows.length,
      status: 'DRAFT',
      csvName: `payroll_uploaded_${month}.csv`
    };
    
    payrollBatches.value.unshift(newBatch);
    
    validRows.forEach(row => {
      const grossPay = row.basic + row.allowances;
      const netSalary = grossPay - row.deductions;
      
      payslips.value.push({
        id: `ps-${Date.now()}-${row.empCode}`,
        employeeCode: row.empCode,
        month,
        grossPay,
        allowances: row.allowances,
        deductions: row.deductions,
        netSalary,
        basic: row.basic,
        pf: Math.round(row.basic * 0.12),
        tax: Math.round(row.basic * 0.05),
        status: 'DRAFT',
        publishedAt: null
      });
    });
    
    addLog(activeUserEmail, 'Payroll', 'UPLOAD_PAYROLL', `Uploaded payroll CSV for month ${month}`);
    return { success: true, batch: newBatch };
  }

  function publishPayrollBatch(batchId, month, activeUserEmail) {
    const batch = payrollBatches.value.find(b => b.id === batchId);
    if (batch && batch.status === 'DRAFT') {
      batch.status = 'PUBLISHED';
      
      // Update matching payslips status
      payslips.value.forEach(ps => {
        if (ps.month === month && ps.status === 'DRAFT') {
          ps.status = 'PUBLISHED';
          ps.publishedAt = new Date().toISOString();
          
          // Send notification to employee
          const emp = employees.value.find(e => e.employeeCode === ps.employeeCode);
          if (emp) {
            addNotification('Payslip Published', `Your payslip for ${month} has been published.`, 'Payslip', '/payslip');
          }
        }
      });
      
      addLog(activeUserEmail, 'Payroll', 'PUBLISH_BATCH', `Published payroll batch ID ${batchId} for ${month}`);
      return true;
    }
    return false;
  }

  // HR employee management
  function addNewEmployee(data, activeUserEmail) {
    const newEmp = {
      id: `emp-${Date.now()}`,
      employeeCode: data.employeeCode,
      fullName: data.fullName,
      email: data.email,
      phone: data.phone || '',
      address: data.address || '',
      department: data.department,
      designation: data.designation,
      managerId: data.managerId,
      managerName: data.managerName || 'Sarah Jenkins',
      managerContact: 'sarah.j@company.com',
      joiningDate: data.joiningDate,
      employmentType: data.employmentType || 'Full-time',
      role: data.role || 'EMPLOYEE',
      salary: parseFloat(data.salary) || 800000,
      salaryBand: data.salaryBand || 'Band 1 (L1-L2)',
      status: 'ACTIVE',
      photo: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150',
      onboardingPercent: 0,
      onboardingTasks: [
        { id: 1, title: 'Acknowledge company policy documents', done: false },
        { id: 2, title: 'Upload identity proof (Aadhaar / Passport)', done: false },
        { id: 3, title: 'Fill personal details form', done: false },
        { id: 4, title: 'Submit bank account details', done: false },
        { id: 5, title: 'Add emergency contact', done: false }
      ],
      bankDetails: { accountNo: '', ifsc: '', bankName: '' },
      emergencyContact: { name: '', relation: '', phone: '' },
      documents: []
    };

    employees.value.push(newEmp);
    
    // Setup initial leave balances
    leaveBalances.value[newEmp.employeeCode] = [
      { type: 'Annual', accrued: 20, used: 0, remaining: 20, carryForward: 0 },
      { type: 'Casual', accrued: 12, used: 0, remaining: 12, carryForward: 0 },
      { type: 'Sick', accrued: 10, used: 0, remaining: 10, carryForward: 0 }
    ];

    addLog(activeUserEmail, 'Employee', 'CREATE_EMPLOYEE', `Created new employee profile: ${data.fullName} (${data.employeeCode})`);
    
    // Simulate sending welcome email
    addNotification('Welcome Email Sent', `Onboarding checklist triggered and welcome email dispatched to ${data.email} via SES.`, 'System');
    return newEmp;
  }

  function updateEmployeeHrView(empId, data, activeUserEmail) {
    const emp = employees.value.find(e => e.id === empId);
    if (emp) {
      emp.department = data.department;
      emp.designation = data.designation;
      emp.managerId = data.managerId;
      const mgr = employees.value.find(e => e.id === data.managerId);
      emp.managerName = mgr ? mgr.fullName : 'None';
      emp.employmentType = data.employmentType;
      emp.status = data.status;
      if (data.salary) {
        emp.salary = parseFloat(data.salary);
      }
      
      addLog(activeUserEmail, 'Employee', 'UPDATE_HR_DETAILS', `HR updated employment details for employee ${emp.fullName}`);
      return true;
    }
    return false;
  }

  // System Scheduler Jobs
  function triggerSchedulerJob(jobId, activeUserEmail) {
    const job = schedulerJobs.value.find(j => j.id === jobId);
    if (job) {
      const startTime = Date.now();
      job.status = 'RUNNING';
      
      setTimeout(() => {
        job.lastRun = new Date().toISOString().replace('T', ' ').substring(0, 19);
        job.duration = `${((Date.now() - startTime) / 1000).toFixed(2)}s`;
        job.status = 'SUCCESS';
        
        // Custom side effects
        if (job.name === 'LeaveAccrualJob') {
          // Add 1.5 days to Annual leave remaining for all employees
          Object.values(leaveBalances.value).forEach(bList => {
            const annual = bList.find(b => b.type === 'Annual');
            if (annual) {
              annual.accrued += 2;
              annual.remaining += 2;
            }
          });
          addNotification('Leave Accrual Run', 'Scheduler job successfully processed monthly leave accruals for all active employees.', 'System');
        }
        
        addLog(activeUserEmail, 'Scheduler', 'RUN_JOB', `Manually triggered job ${job.name}`);
      }, 500);
      return true;
    }
    return false;
  }

  function toggleSchedulerJob(jobId, activeUserEmail) {
    const job = schedulerJobs.value.find(j => j.id === jobId);
    if (job) {
      job.active = !job.active;
      addLog(activeUserEmail, 'Scheduler', 'TOGGLE_JOB', `Toggled scheduled job ${job.name} to ${job.active ? 'ENABLED' : 'DISABLED'}`);
      return true;
    }
    return false;
  }

  const backendConnected = ref(false);

  async function fetchEmployees() {
    try {
      const data = await apiRequest('/api/v1/employees');
      if (data && data.length > 0) {
        employees.value = data.map(emp => ({
          id: emp.id,
          employeeCode: emp.employeeCode,
          fullName: emp.name,
          email: emp.email,
          phone: emp.phone,
          address: emp.address,
          department: emp.department,
          designation: emp.designation,
          managerId: emp.managerId,
          managerName: emp.managerId ? 'Sarah Jenkins' : null,
          joiningDate: emp.joiningDate,
          employmentType: 'Full-time',
          role: emp.employeeCode === 'EMP001' ? 'HR_ADMIN' : emp.employeeCode === 'EMP002' ? 'MANAGER' : emp.employeeCode === 'EMP003' ? 'FINANCE_ADMIN' : emp.employeeCode === 'EMP005' ? 'SYSTEM_ADMIN' : 'EMPLOYEE',
          salary: emp.salary,
          salaryBand: emp.salaryBand,
          status: emp.status,
          onboardingPercent: emp.onboardingPercent,
          bankDetails: {
            accountNo: emp.bankAccountNo,
            ifsc: emp.bankIfsc,
            bankName: emp.bankName
          },
          emergencyContact: {
            name: emp.emergencyName,
            relation: emp.emergencyRelation,
            phone: emp.emergencyPhone
          },
          documents: []
        }));
        backendConnected.value = true;
      }
    } catch (e) {
      console.warn('API error fetching employees:', e.message);
    }
  }

  async function fetchLeaves(employeeId) {
    try {
      const balances = await apiRequest(`/api/v1/leaves/balances?employeeId=${employeeId}`);
      const emp = employees.value.find(e => e.id === employeeId);
      if (emp && balances) {
        leaveBalances.value[emp.employeeCode] = balances.map(b => ({
          type: b.leaveType === 'ANNUAL' ? 'Annual' : b.leaveType === 'SICK' ? 'Sick' : b.leaveType === 'CASUAL' ? 'Casual' : b.leaveType,
          accrued: b.leaveType === 'ANNUAL' ? 15 : b.leaveType === 'SICK' ? 10 : 7,
          used: (b.leaveType === 'ANNUAL' ? 15 : b.leaveType === 'SICK' ? 10 : 7) - b.balance,
          remaining: b.balance,
          carryForward: 0
        }));
      }
      
      const requests = await apiRequest(`/api/v1/leaves/requests?employeeId=${employeeId}`);
      if (requests) {
        leaveRequests.value = requests.map(req => ({
          id: req.id,
          employeeCode: emp?.employeeCode || 'EMP',
          fullName: emp?.fullName || 'User',
          department: emp?.department || 'Department',
          leaveType: req.leaveType === 'ANNUAL' ? 'Annual' : req.leaveType === 'SICK' ? 'Sick' : req.leaveType === 'CASUAL' ? 'Casual' : req.leaveType,
          fromDate: req.startDate,
          toDate: req.endDate,
          daysRequested: Math.round((new Date(req.endDate) - new Date(req.startDate)) / (1000 * 60 * 60 * 24)) + 1,
          reason: req.reason,
          status: req.status,
          approvedBy: req.managerComment ? 'Manager' : null,
          comments: req.managerComment,
          createdAt: req.createdAt
        }));
      }
    } catch (e) {
      console.warn('API error fetching leaves:', e.message);
    }
  }

  async function fetchExpenses(employeeId) {
    try {
      const claims = await apiRequest(`/api/v1/expenses?employeeId=${employeeId}`);
      const emp = employees.value.find(e => e.id === employeeId);
      if (claims) {
        expenseClaims.value = claims.map(c => ({
          id: c.id,
          employeeCode: emp?.employeeCode || 'EMP',
          fullName: emp?.fullName || 'User',
          department: emp?.department || 'Department',
          category: c.category,
          amount: c.amount,
          currency: c.currency,
          date: c.createdAt?.split('T')[0] || new Date().toISOString().split('T')[0],
          description: c.description,
          receiptName: 'receipt.pdf',
          status: c.status,
          timeline: [
            { status: 'SUBMITTED', title: 'Submitted', timestamp: c.createdAt }
          ],
          paymentRef: null
        }));
      }
    } catch (e) {
      console.warn('API error fetching expenses:', e.message);
    }
  }

  async function fetchPayslips(employeeId) {
    try {
      const list = await apiRequest(`/api/v1/payroll/employee/${employeeId}`);
      const emp = employees.value.find(e => e.id === employeeId);
      if (list) {
        payslips.value = list.map(p => ({
          id: p.id,
          employeeCode: emp?.employeeCode || 'EMP',
          month: p.month + '-' + p.year,
          grossPay: p.grossPay,
          allowances: 0,
          deductions: p.deduction,
          netSalary: p.netPay,
          basic: p.grossPay,
          pf: 0,
          tax: 0,
          status: p.published ? 'PUBLISHED' : 'DRAFT',
          publishedAt: p.createdAt
        }));
      }
    } catch (e) {
      console.warn('API error fetching payslips:', e.message);
    }
  }

  async function fetchNotifications(userId) {
    try {
      const list = await apiRequest(`/api/v1/notifications?userId=${userId}`);
      if (list) {
        notifications.value = list.map(n => ({
          id: n.id,
          title: n.title,
          message: n.message,
          type: 'System',
          isRead: n.isRead,
          createdAt: n.createdAt,
          url: ''
        }));
      }
    } catch (e) {
      console.warn('API error fetching notifications:', e.message);
    }
  }

  async function syncAll(userId, employeeId) {
    if (!userId || !employeeId) return;
    await fetchEmployees();
    await fetchLeaves(employeeId);
    await fetchExpenses(employeeId);
    await fetchPayslips(employeeId);
    await fetchNotifications(userId);
  }

  return {
    employees,
    leaveRequests,
    leaveBalances,
    leaveTypes,
    publicHolidays,
    expenseClaims,
    expenseLimits,
    currencyRates,
    payslips,
    payrollBatches,
    notifications,
    notificationPreferences,
    systemSettings,
    emailTemplates,
    schedulerJobs,
    auditLogs,
    backendConnected,
    // Methods
    addLog,
    addNotification,
    updateEmployeeProfile,
    toggleOnboardingTask,
    applyLeave,
    cancelLeaveRequest,
    approveLeaveRequest,
    rejectLeaveRequest,
    submitExpenseClaim,
    cancelExpenseClaim,
    approveExpenseManager,
    approveExpenseFinance,
    rejectExpense,
    markExpensePaid,
    processPayrollUpload,
    publishPayrollBatch,
    addNewEmployee,
    updateEmployeeHrView,
    triggerSchedulerJob,
    toggleSchedulerJob,
    fetchEmployees,
    fetchLeaves,
    fetchExpenses,
    fetchPayslips,
    fetchNotifications,
    syncAll
  };
});
