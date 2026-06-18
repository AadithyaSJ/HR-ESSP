import { defineStore } from 'pinia';
import { ref } from 'vue';
import { apiRequest } from '../utils/api';

function convertMonthToYyyyMm(monthStr, year) {
  if (!monthStr) return '';
  if (monthStr.includes('-') && !isNaN(monthStr.split('-')[0])) {
    return monthStr;
  }
  const months = {
    january: '01', february: '02', march: '03', april: '04', may: '05', june: '06',
    july: '07', august: '08', september: '09', october: '10', november: '11', december: '12'
  };
  const m = months[String(monthStr).trim().toLowerCase()] || '01';
  return `${year}-${m}`;
}

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
    { id: 'lim-2', category: 'Travel', maxAmount: 20000, frequency: 'PER_TRIP', grade: 'All Grades' },
    { id: 'lim-3', category: 'Accommodation', maxAmount: 10000, frequency: 'PER_NIGHT', grade: 'Grade L5+' },
    { id: 'lim-4', category: 'Accommodation', maxAmount: 6000, frequency: 'PER_NIGHT', grade: 'Grade L1-L4' },
    { id: 'lim-5', category: 'Broadband', maxAmount: 2000, frequency: 'PER_MONTH', grade: 'All Grades' },
    { id: 'lim-6', category: 'Mobile', maxAmount: 1200, frequency: 'PER_MONTH', grade: 'All Grades' },
    { id: 'lim-7', category: 'HomeOffice', maxAmount: 15000, frequency: 'PER_YEAR', grade: 'All Grades' },
    { id: 'lim-8', category: 'Wellness', maxAmount: 3000, frequency: 'PER_MONTH', grade: 'All Grades' },
    { id: 'lim-9', category: 'Certification', maxAmount: 40000, frequency: 'PER_YEAR', grade: 'All Grades' }
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
    environment: 'Staging (AWS ECS Fargate)',
    emailJsServiceId: 'service_z913fuu',
    emailJsTemplateId: 'template_2mgjka9',
    emailJsPublicKey: 'rA7PvZze2DIvgX8WR',
    emailJsPrivateKey: 'uRt0bEDyyVvuKCVnzAjET'
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
  async function updateEmployeeProfile(empId, data, activeUserEmail) {
    const idx = employees.value.findIndex(e => e.id === empId);
    if (idx !== -1) {
      const emp = employees.value[idx];
      emp.phone = data.phone || emp.phone;
      emp.address = data.address || emp.address;
      if (data.emergencyContact) {
        emp.emergencyContact = {
          ...emp.emergencyContact,
          ...data.emergencyContact
        };
      }
      if (data.bankDetails) {
        emp.bankDetails = {
          ...emp.bankDetails,
          ...data.bankDetails
        };
      }
      
      // Document upload
      if (data.newDocument) {
        emp.documents.push(data.newDocument);
      }
      
      // Map to backend entity structure
      const backendEmp = {
        id: emp.id,
        employeeCode: emp.employeeCode,
        name: emp.fullName,
        email: emp.email,
        phone: emp.phone,
        address: emp.address,
        department: emp.department,
        designation: emp.designation,
        bankAccountNo: emp.bankDetails.accountNo,
        bankIfsc: emp.bankDetails.ifsc,
        bankName: emp.bankDetails.bankName,
        emergencyName: emp.emergencyContact.name,
        emergencyRelation: emp.emergencyContact.relation,
        emergencyPhone: emp.emergencyContact.phone,
        onboardingPercent: emp.onboardingPercent,
        managerId: emp.managerId,
        joiningDate: emp.joiningDate,
        status: emp.status,
        salary: emp.salary,
        salaryBand: emp.salaryBand
      };

      await apiRequest(`/api/v1/employees/${empId}`, {
        method: 'PUT',
        body: JSON.stringify(backendEmp)
      });

      addLog(activeUserEmail, 'Employee', 'UPDATE_PROFILE', `Updated profile details for employee ID ${empId}`);
      return true;
    }
    return false;
  }

  async function toggleOnboardingTask(empId, taskId, activeUserEmail) {
    const emp = employees.value.find(e => e.id === empId);
    if (emp && emp.onboardingTasks) {
      const t = emp.onboardingTasks.find(x => x.id === taskId);
      if (t) {
        t.done = !t.done;
        
        // Recalculate percent
        const doneCount = emp.onboardingTasks.filter(x => x.done).length;
        emp.onboardingPercent = Math.round((doneCount / emp.onboardingTasks.length) * 100);
        
        // Save update to DB
        const backendEmp = {
          id: emp.id,
          employeeCode: emp.employeeCode,
          name: emp.fullName,
          email: emp.email,
          phone: emp.phone,
          address: emp.address,
          department: emp.department,
          designation: emp.designation,
          bankAccountNo: emp.bankDetails.accountNo,
          bankIfsc: emp.bankDetails.ifsc,
          bankName: emp.bankDetails.bankName,
          emergencyName: emp.emergencyContact.name,
          emergencyRelation: emp.emergencyContact.relation,
          emergencyPhone: emp.emergencyContact.phone,
          onboardingPercent: emp.onboardingPercent,
          managerId: emp.managerId,
          joiningDate: emp.joiningDate,
          status: emp.status,
          salary: emp.salary,
          salaryBand: emp.salaryBand
        };

        await apiRequest(`/api/v1/employees/${empId}`, {
          method: 'PUT',
          body: JSON.stringify(backendEmp)
        });

        addLog(activeUserEmail, 'Onboarding', 'TOGGLE_TASK', `Toggled onboarding task "${t.title}" to ${t.done}`);
      }
    }
  }

  // Leave actions
  async function applyLeave(data, activeUserEmail) {
    const { useAuthStore } = await import('./authStore');
    const authStore = useAuthStore();

    const backendReq = {
      employeeId: authStore.user.id,
      leaveType: data.leaveType.toUpperCase(),
      startDate: data.fromDate,
      endDate: data.toDate,
      reason: data.reason,
      status: 'PENDING',
      attachmentName: data.attachmentName || null,
      attachmentPath: data.attachmentPath || null
    };

    const created = await apiRequest('/api/v1/leaves/requests', {
      method: 'POST',
      body: JSON.stringify(backendReq)
    });

    const daysReq = calculateLeaveDaysFrontend(authStore.user.id, created.startDate, created.endDate);

    const mapped = {
      id: created.id,
      employeeCode: data.employeeCode,
      fullName: data.fullName,
      department: data.department,
      leaveType: created.leaveType === 'ANNUAL' ? 'Annual' : created.leaveType === 'SICK' ? 'Sick' : created.leaveType === 'CASUAL' ? 'Casual' : created.leaveType === 'UNPAID' ? 'Unpaid' : created.leaveType,
      fromDate: created.startDate,
      toDate: created.endDate,
      daysRequested: daysReq,
      reason: created.reason,
      status: created.status,
      approvedBy: null,
      comments: null,
      attachmentName: created.attachmentName || null,
      attachmentPath: created.attachmentPath || null,
      createdAt: created.createdAt
    };

    leaveRequests.value.unshift(mapped);
    addLog(activeUserEmail, 'Leave', 'APPLY_LEAVE', `Applied for ${data.leaveType} leave: ${data.fromDate} to ${data.toDate}`);
    addNotification('Leave Submitted', `Leave request for ${mapped.daysRequested} days has been submitted.`, 'Leave', '/leave');
    return mapped;
  }

  async function cancelLeaveRequest(reqId, activeUserEmail) {
    await apiRequest(`/api/v1/leaves/requests/${reqId}/cancel`, { method: 'POST' });
    const req = leaveRequests.value.find(r => r.id === reqId);
    if (req) {
      req.status = 'CANCELLED';
    }
    addLog(activeUserEmail, 'Leave', 'CANCEL_LEAVE', `Cancelled leave request ID ${reqId}`);
    return true;
  }

  async function approveLeaveRequest(reqId, managerName, comment, activeUserEmail) {
    const url = `/api/v1/leaves/requests/${reqId}/approve` + (comment ? `?comment=${encodeURIComponent(comment)}` : '');
    const approved = await apiRequest(url, { method: 'POST' });
    const req = leaveRequests.value.find(r => r.id === reqId);
    if (req) {
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
    }
    
    addLog(activeUserEmail, 'Leave', 'APPROVE_LEAVE', `Approved leave request ID ${reqId}`);
    addNotification('Leave Approved', `Your leave request from ${req ? req.fromDate : ''} has been APPROVED.`, 'Leave', '/leave');
    return true;
  }

  async function rejectLeaveRequest(reqId, managerName, reason, activeUserEmail) {
    const url = `/api/v1/leaves/requests/${reqId}/reject` + (reason ? `?comment=${encodeURIComponent(reason)}` : '');
    const rejected = await apiRequest(url, { method: 'POST' });
    const req = leaveRequests.value.find(r => r.id === reqId);
    if (req) {
      req.status = 'REJECTED';
      req.approvedBy = managerName;
      req.comments = reason;
    }
    
    addLog(activeUserEmail, 'Leave', 'REJECT_LEAVE', `Rejected leave request ID ${reqId}. Reason: ${reason}`);
    addNotification('Leave Rejected', `Your leave request from ${req ? req.fromDate : ''} has been REJECTED.`, 'Leave', '/leave');
    return true;
  }

  function calculateLeaveDaysFrontend(employeeId, fromDateStr, toDateStr) {
    const emp = employees.value.find(e => e.id === employeeId);
    let country = 'India';
    if (emp && emp.address) {
      const addrUpper = emp.address.toUpperCase();
      if (addrUpper.includes("USA") || addrUpper.includes("UNITED STATES")) {
        country = 'USA';
      } else if (addrUpper.includes("UK") || addrUpper.includes("UNITED KINGDOM")) {
        country = 'UK';
      }
    }
    const countryHolidays = publicHolidays.value
      .filter(h => h.country.toLowerCase() === country.toLowerCase())
      .map(h => h.date);

    let count = 0;
    let start = new Date(fromDateStr);
    let end = new Date(toDateStr);
    let current = new Date(start);
    while (current <= end) {
      const day = current.getDay();
      const isWeekend = (day === 0 || day === 6);
      const yyyy = current.getFullYear();
      const mm = String(current.getMonth() + 1).padStart(2, '0');
      const dd = String(current.getDate()).padStart(2, '0');
      const dateStr = `${yyyy}-${mm}-${dd}`;
      const isHoliday = countryHolidays.includes(dateStr);

      if (!isWeekend && !isHoliday) {
        count++;
      }
      current.setDate(current.getDate() + 1);
    }
    return count;
  }

  async function fetchPublicHolidays() {
    try {
      const data = await apiRequest('/api/v1/leaves/holidays');
      if (data) {
        publicHolidays.value = data.map(h => ({
          id: h.id,
          country: h.country,
          name: h.name,
          date: h.holidayDate,
          mandatory: true
        }));
      }
    } catch (e) {
      console.warn('API error fetching public holidays:', e.message);
    }
  }

  async function addPublicHoliday(holidayData, activeUserEmail) {
    try {
      const backendHoliday = {
        country: holidayData.country,
        name: holidayData.name,
        holidayDate: holidayData.date
      };
      const created = await apiRequest('/api/v1/leaves/holidays', {
        method: 'POST',
        body: JSON.stringify(backendHoliday)
      });
      if (created) {
        publicHolidays.value.push({
          id: created.id,
          country: created.country,
          name: created.name,
          date: created.holidayDate,
          mandatory: true
        });
        addLog(activeUserEmail || 'hr@company.com', 'Leave', 'ADD_HOLIDAY', `Added public holiday: ${created.name} (${created.country}) on ${created.holidayDate}`);
        return created;
      }
    } catch (e) {
      console.error('Error adding public holiday:', e);
      throw e;
    }
  }

  async function uploadLeaveAttachment(file) {
    try {
      const formData = new FormData();
      formData.append('file', file);
      const data = await apiRequest('/api/v1/leaves/upload', {
        method: 'POST',
        body: formData
      });
      return data; // returns { fileName, filePath }
    } catch (e) {
      console.error('Error uploading leave attachment:', e);
      throw e;
    }
  }

  async function uploadExpenseReceipt(file) {
    try {
      const formData = new FormData();
      formData.append('file', file);
      const data = await apiRequest('/api/v1/expenses/upload', {
        method: 'POST',
        body: formData
      });
      return data; // returns { fileName, filePath }
    } catch (e) {
      console.error('Error uploading expense receipt:', e);
      throw e;
    }
  }

  // Expense actions
  async function submitExpenseClaim(data, activeUserEmail) {
    const { useAuthStore } = await import('./authStore');
    const authStore = useAuthStore();

    const claim = {
      employeeId: authStore.user.id,
      category: data.category.toUpperCase(),
      amount: parseFloat(data.amount),
      currency: data.currency,
      description: data.description,
      status: 'PENDING'
    };

    const receipts = data.receiptName ? [{
      s3Url: `http://localhost:9000/receipts/${data.receiptName}`,
      fileName: data.receiptName
    }] : [];

    const payload = { claim, receipts };

    const created = await apiRequest('/api/v1/expenses', {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    const newClaim = {
      id: created.id,
      employeeCode: data.employeeCode,
      fullName: data.fullName,
      department: data.department,
      category: data.category,
      amount: created.amount,
      currency: created.currency,
      date: created.createdAt?.split('T')[0] || new Date().toISOString().split('T')[0],
      description: created.description,
      receiptName: data.receiptName || 'uploaded_receipt.pdf',
      status: created.status,
      timeline: [
        { status: 'SUBMITTED', title: `Submitted by ${data.fullName}`, timestamp: created.createdAt }
      ],
      paymentRef: null
    };

    expenseClaims.value.unshift(newClaim);
    
    addLog(activeUserEmail, 'Expense', 'SUBMIT_CLAIM', `Submitted expense claim of ${data.currency} ${data.amount} for ${data.category}`);
    addNotification('Expense Submitted', `Expense claim for ${data.currency} ${data.amount} submitted.`, 'Expense', '/expense');
    return newClaim;
  }

  async function cancelExpenseClaim(claimId, activeUserEmail) {
    const c = expenseClaims.value.find(cl => cl.id === claimId);
    if (c && ['PENDING', 'PENDING_FINANCE'].includes(c.status)) {
      await apiRequest(`/api/v1/expenses/${claimId}/cancel`, { method: 'POST' });
      c.status = 'CANCELLED';
      addLog(activeUserEmail, 'Expense', 'CANCEL_CLAIM', `Cancelled pending claim ID ${claimId}`);
      return true;
    }
    return false;
  }

  async function approveExpenseManager(claimId, managerName, activeUserEmail) {
    const approved = await apiRequest(`/api/v1/expenses/${claimId}/approve-manager`, { method: 'POST' });
    const c = expenseClaims.value.find(cl => cl.id === claimId);
    if (c) {
      c.status = 'APPROVED_MANAGER';
      c.timeline.push({
        status: 'APPROVED_MANAGER',
        title: `Approved by ${managerName} (Manager)`,
        timestamp: new Date().toISOString()
      });
    }
    addLog(activeUserEmail, 'Expense', 'APPROVE_EXPENSE_MANAGER', `Manager approved expense ID ${claimId}`);
    addNotification('Expense Manager Approved', `Your expense claim ID ${claimId} was approved by manager.`, 'Expense', '/expense');
    return true;
  }

  async function approveExpenseFinance(claimId, financeName, payRef, activeUserEmail) {
    const approved = await apiRequest(`/api/v1/expenses/${claimId}/approve-finance`, { method: 'POST' });
    const c = expenseClaims.value.find(cl => cl.id === claimId);
    if (c) {
      c.status = 'APPROVED_FINANCE';
      c.paymentRef = payRef;
      c.timeline.push({
        status: 'APPROVED_FINANCE',
        title: `Approved by ${financeName} (Finance Admin)`,
        timestamp: new Date().toISOString()
      });
    }
    addLog(activeUserEmail, 'Expense', 'APPROVE_EXPENSE_FINANCE', `Finance approved expense ID ${claimId}. Pay Ref: ${payRef}`);
    return true;
  }

  async function rejectExpense(claimId, reviewerName, reason, activeUserEmail) {
    const { useAuthStore } = await import('./authStore');
    const authStore = useAuthStore();

    const isFinance = authStore.activeRole === 'FINANCE_ADMIN';
    const endpoint = isFinance 
      ? `/api/v1/expenses/${claimId}/reject-finance` 
      : `/api/v1/expenses/${claimId}/reject-manager`;

    const rejected = await apiRequest(endpoint, { method: 'POST' });
    const c = expenseClaims.value.find(cl => cl.id === claimId);
    if (c) {
      c.status = 'REJECTED';
      c.timeline.push({
        status: 'REJECTED',
        title: `Rejected by ${reviewerName}. Reason: ${reason}`,
        timestamp: new Date().toISOString()
      });
    }
    addLog(activeUserEmail, 'Expense', 'REJECT_EXPENSE', `Rejected claim ID ${claimId}. Reason: ${reason}`);
    addNotification('Expense Rejected', `Your expense claim ID ${claimId} was rejected.`, 'Expense', '/expense');
    return true;
  }

  async function markExpensePaid(claimId, activeUserEmail) {
    await apiRequest(`/api/v1/expenses/${claimId}/pay`, { method: 'POST' });
    const c = expenseClaims.value.find(cl => cl.id === claimId);
    if (c) {
      c.status = 'PAID';
      c.timeline.push({
        status: 'PAID',
        title: `Marked Paid`,
        timestamp: new Date().toISOString()
      });
    }
    addLog(activeUserEmail, 'Expense', 'PAY_EXPENSE', `Marked claim ID ${claimId} as Paid`);
    addNotification('Expense Paid', `Your expense claim ID ${claimId} has been Paid.`, 'Expense', '/expense');
    return true;
  }

  // Payroll / Payslip Actions
  async function generatePayrollRun(month, activeUserEmail) {
    try {
      const [yearStr, monthNum] = month.split('-');
      const year = parseInt(yearStr);
      const months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
      const monthName = months[parseInt(monthNum) - 1] || month;

      const generated = await apiRequest(`/api/v1/payroll/generate?month=${monthName}&year=${year}`, {
        method: 'POST'
      });

      const newPayslips = generated.map(p => {
        const emp = employees.value.find(e => e.id === p.employeeId);
        const basic = Math.round((p.grossPay - p.deduction) * 0.6);
        const allowances = p.grossPay - basic;
        const pf = Math.round(p.deduction * 0.5);
        const tax = p.deduction - pf;
        return {
          id: p.id,
          employeeCode: emp ? emp.employeeCode : 'EMP001',
          month: convertMonthToYyyyMm(p.month, p.year),
          grossPay: p.grossPay,
          allowances: allowances,
          deductions: p.deduction,
          netSalary: p.netPay,
          basic: basic,
          pf: pf,
          tax: tax,
          status: p.published ? 'PUBLISHED' : 'DRAFT',
          publishedAt: p.published ? p.createdAt : null
        };
      });

      // Filter out existing drafts
      payslips.value = payslips.value.filter(ps => !(ps.month === month && ps.status === 'DRAFT'));
      payslips.value.push(...newPayslips);

      // Create a batch in local state
      const batchId = `b-${Date.now()}`;
      const newBatch = {
        id: batchId,
        month,
        uploadDate: new Date().toISOString().split('T')[0],
        totalEmployees: generated.length,
        status: 'DRAFT',
        csvName: `Auto-Run Generated (${month})`
      };

      payrollBatches.value = payrollBatches.value.filter(b => !(b.month === month && b.status === 'DRAFT'));
      payrollBatches.value.unshift(newBatch);

      addLog(activeUserEmail, 'Payroll', 'GENERATE_PAYROLL', `Automatically generated payroll run for month ${month}`);
      return { success: true, batch: newBatch };
    } catch (e) {
      console.error(e);
      return { success: false, errors: [e.message] };
    }
  }

  async function processPayrollUpload(csvContent, month, activeUserEmail) {
    try {
      const imported = await apiRequest('/api/v1/payroll/upload', {
        method: 'POST',
        headers: { 'Content-Type': 'text/plain' },
        body: csvContent
      });

      const newPayslips = imported.map(p => {
        const emp = employees.value.find(e => e.id === p.employeeId);
        const basic = Math.round((p.grossPay - p.deduction) * 0.6);
        const allowances = p.grossPay - basic;
        const pf = Math.round(p.deduction * 0.5);
        const tax = p.deduction - pf;
        return {
          id: p.id,
          employeeCode: emp ? emp.employeeCode : 'EMP001',
          month: convertMonthToYyyyMm(p.month, p.year),
          grossPay: p.grossPay,
          allowances: allowances,
          deductions: p.deduction,
          netSalary: p.netPay,
          basic: basic,
          pf: pf,
          tax: tax,
          status: 'DRAFT',
          publishedAt: null
        };
      });

      payslips.value.push(...newPayslips);

      const batchId = `b-${Date.now()}`;
      const newBatch = {
        id: batchId,
        month,
        uploadDate: new Date().toISOString().split('T')[0],
        totalEmployees: imported.length,
        status: 'DRAFT',
        csvName: `payroll_uploaded_${month}.csv`
      };
      
      payrollBatches.value.unshift(newBatch);
      addLog(activeUserEmail, 'Payroll', 'UPLOAD_PAYROLL', `Uploaded payroll CSV for month ${month}`);
      return { success: true, batch: newBatch };
    } catch (e) {
      console.error(e);
      return { success: false, errors: [e.message] };
    }
  }

  async function publishPayrollBatch(batchId, month, activeUserEmail) {
    const batch = payrollBatches.value.find(b => b.id === batchId);
    if (batch && batch.status === 'DRAFT') {
      try {
        const [yearStr, monthNum] = month.split('-');
        const year = parseInt(yearStr);
        const months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
        const monthName = months[parseInt(monthNum) - 1] || month;

        await apiRequest(`/api/v1/payroll/publish?month=${monthName}&year=${year}`, { method: 'POST' });

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
      } catch (e) {
        console.error('Publish failed:', e.message);
        return false;
      }
    }
    return false;
  }

  async function rejectPayrollBatch(batchId, month, activeUserEmail) {
    const batch = payrollBatches.value.find(b => b.id === batchId);
    if (batch && batch.status === 'DRAFT') {
      try {
        const [yearStr, monthNum] = month.split('-');
        const year = parseInt(yearStr);
        const months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
        const monthName = months[parseInt(monthNum) - 1] || month;

        await apiRequest(`/api/v1/payroll/reject?month=${monthName}&year=${year}`, { method: 'POST' });

        // Remove matching batch
        payrollBatches.value = payrollBatches.value.filter(b => b.id !== batchId);
        
        // Remove matching draft payslips
        payslips.value = payslips.value.filter(ps => !(ps.month === month && ps.status === 'DRAFT'));
        
        addLog(activeUserEmail, 'Payroll', 'REJECT_BATCH', `Rejected and deleted draft payroll batch for month ${month}`);
        return true;
      } catch (e) {
        console.error('Reject failed:', e.message);
        return false;
      }
    }
    return false;
  }

  async function updatePayslip(payslipId, data, activeUserEmail) {
    try {
      const payload = {
        grossPay: parseFloat(data.grossPay),
        deduction: parseFloat(data.deductions)
      };
      const updated = await apiRequest(`/api/v1/payroll/${payslipId}`, {
        method: 'PUT',
        body: JSON.stringify(payload)
      });
      // Update local state
      const ps = payslips.value.find(p => p.id === payslipId);
      if (ps) {
        ps.grossPay = updated.grossPay;
        ps.deductions = updated.deduction;
        ps.netSalary = updated.netPay;
        // Also update calculated fields for breakdown
        ps.basic = Math.round((updated.grossPay - updated.deduction) * 0.6);
        ps.allowances = updated.grossPay - ps.basic;
        ps.pf = Math.round(updated.deduction * 0.5);
        ps.tax = updated.deduction - ps.pf;
      }
      addLog(activeUserEmail, 'Payroll', 'UPDATE_PAYSLIP', `Manually updated payslip ID ${payslipId}`);
      return true;
    } catch (e) {
      console.error('Update payslip failed:', e.message);
      return false;
    }
  }

  // HR employee management
  async function addNewEmployee(data, activeUserEmail) {
    const backendEmp = {
      employeeCode: data.employeeCode,
      name: data.fullName,
      email: data.email,
      phone: data.phone || '',
      address: data.address || '',
      department: data.department,
      designation: data.designation,
      managerId: data.managerId || null,
      joiningDate: data.joiningDate,
      salary: parseFloat(data.salary) || 800000,
      salaryBand: data.salaryBand || 'Band 1 (L1-L2)',
      status: 'ACTIVE',
      role: data.role,
      onboardingPercent: 0
    };

    const created = await apiRequest('/api/v1/employees', {
      method: 'POST',
      body: JSON.stringify(backendEmp)
    });

    const welcomeLink = `http://localhost:5173/login?email=${encodeURIComponent(created.email)}&action=create-password`;

    const newEmp = {
      id: created.id,
      employeeCode: created.employeeCode,
      fullName: created.name,
      email: created.email,
      phone: created.phone || '',
      address: created.address || '',
      department: created.department,
      designation: created.designation,
      managerId: created.managerId,
      managerName: created.managerId ? (employees.value.find(e => e.id === created.managerId)?.fullName || 'Sarah Jenkins') : null,
      managerContact: 'sarah.j@company.com',
      joiningDate: created.joiningDate,
      employmentType: 'Full-time',
      role: data.role || 'EMPLOYEE',
      salary: created.salary,
      salaryBand: created.salaryBand,
      status: created.status,
      photo: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150',
      onboardingPercent: created.onboardingPercent,
      onboardingStatus: created.onboardingStatus || 'PENDING_DETAILS',
      school: '',
      college: '',
      experience: '',
      certificates: '',
      welcomeLink: welcomeLink,
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
    addNotification('Welcome Email Sent', `Welcome link: ${welcomeLink}`, 'System');

    // EmailJS Send Integration
    const sId = systemSettings.value.emailJsServiceId;
    const tId = systemSettings.value.emailJsTemplateId;
    const pKey = systemSettings.value.emailJsPublicKey;
    const prKey = systemSettings.value.emailJsPrivateKey;

    if (sId && tId && pKey) {
      const emailPayload = {
        service_id: sId,
        template_id: tId,
        user_id: pKey,
        template_params: {
          to_email: created.email,
          email: created.email,
          to: created.email,
          recipient: created.email,
          recipient_email: created.email,
          employee_name: created.name,
          welcome_link: welcomeLink
        }
      };

      if (prKey) {
        emailPayload.accessToken = prKey;
      }

      fetch('https://api.emailjs.com/api/v1.0/email/send', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(emailPayload)
      })
      .then(res => {
        if (res.ok) {
          console.log('Onboarding invitation email sent successfully via EmailJS');
          addLog(activeUserEmail, 'Employee', 'EMAIL_SENT', `Onboarding invitation sent to ${created.email} via EmailJS`);
        } else {
          res.text().then(text => {
            console.error('EmailJS error response:', text);
            addLog(activeUserEmail, 'Employee', 'EMAIL_SEND_FAILED', `Failed to send onboarding email to ${created.email}: ${text}`);
            if (window.showPortalToast) {
              window.showPortalToast(`EmailJS Error: ${text}. Check Email Settings in Admin Console.`, 'error');
            }
          });
        }
      })
      .catch(err => {
        console.error('EmailJS request failed:', err);
        addLog(activeUserEmail, 'Employee', 'EMAIL_SEND_FAILED', `Failed to send onboarding email to ${created.email}: ${err.message || err}`);
        if (window.showPortalToast) {
          window.showPortalToast(`EmailJS Request Failed: ${err.message || err}`, 'error');
        }
      });
    }

    return newEmp;
  }

  async function updateEmployeeHrView(empId, data, activeUserEmail) {
    const emp = employees.value.find(e => e.id === empId);
    if (emp) {
      emp.department = data.department;
      emp.designation = data.designation;
      emp.managerId = data.managerId;
      const mgr = employees.value.find(e => e.id === data.managerId);
      emp.managerName = mgr ? mgr.fullName : 'None';
      emp.employmentType = data.employmentType || emp.employmentType;
      emp.status = data.status;
      if (data.salary) {
        emp.salary = parseFloat(data.salary);
      }
      
      const backendEmp = {
        id: emp.id,
        employeeCode: emp.employeeCode,
        name: emp.fullName,
        email: emp.email,
        phone: emp.phone,
        address: emp.address,
        department: emp.department,
        designation: emp.designation,
        bankAccountNo: emp.bankDetails.accountNo,
        bankIfsc: emp.bankDetails.ifsc,
        bankName: emp.bankDetails.bankName,
        emergencyName: emp.emergencyContact.name,
        emergencyRelation: emp.emergencyContact.relation,
        emergencyPhone: emp.emergencyContact.phone,
        onboardingPercent: emp.onboardingPercent,
        managerId: emp.managerId,
        joiningDate: emp.joiningDate,
        status: emp.status,
        salary: emp.salary,
        salaryBand: emp.salaryBand
      };

      await apiRequest(`/api/v1/employees/${empId}`, {
        method: 'PUT',
        body: JSON.stringify(backendEmp)
      });
      
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
  const mandatoryDocuments = ref([]);

  async function fetchMandatoryDocuments() {
    try {
      const data = await apiRequest('/api/v1/documents/mandatory');
      if (data) {
        mandatoryDocuments.value = data;
      }
    } catch (e) {
      console.error('Error fetching mandatory documents:', e);
    }
  }

  async function addMandatoryDocument(name, activeUserEmail) {
    try {
      const data = await apiRequest('/api/v1/documents/mandatory', {
        method: 'POST',
        body: JSON.stringify({ name })
      });
      if (data) {
        mandatoryDocuments.value.push(data);
        addLog(activeUserEmail, 'Documents', 'ADD_MANDATORY_DOC', `Added required document type: ${name}`);
        addNotification('New Mandatory Document', `A new mandatory document '${name}' has been required by HR.`, 'System');
        return true;
      }
    } catch (e) {
      console.error('Error adding mandatory document:', e);
    }
    return false;
  }

  async function fetchEmployeeDocuments(employeeId) {
    try {
      const data = await apiRequest(`/api/v1/employees/${employeeId}/documents`);
      const emp = employees.value.find(e => e.id === employeeId);
      if (emp && data) {
        emp.documents = data.map(d => ({
          id: d.id,
          name: d.fileName,
          size: d.fileSize,
          date: d.uploadedAt?.split('T')[0] || new Date().toISOString().split('T')[0],
          type: d.documentType
        }));
      }
      return data;
    } catch (e) {
      console.error('Error fetching employee documents:', e);
      return [];
    }
  }

  async function uploadDocument(employeeId, file, documentType, activeUserEmail) {
    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('documentType', documentType);

      const data = await apiRequest(`/api/v1/employees/${employeeId}/documents`, {
        method: 'POST',
        body: formData
      });

      if (data) {
        await fetchEmployeeDocuments(employeeId);
        addLog(activeUserEmail, 'Documents', 'UPLOAD_DOCUMENT', `Uploaded document type: ${documentType}`);
        return true;
      }
    } catch (e) {
      console.error('Error uploading document:', e);
    }
    return false;
  }

  async function deleteDocument(employeeId, documentId, activeUserEmail) {
    try {
      await apiRequest(`/api/v1/employees/${employeeId}/documents/${documentId}`, {
        method: 'DELETE'
      });
      await fetchEmployeeDocuments(employeeId);
      addLog(activeUserEmail, 'Documents', 'DELETE_DOCUMENT', `Deleted document ID: ${documentId}`);
      return true;
    } catch (e) {
      console.error('Error deleting document:', e);
    }
    return false;
  }

  async function fetchEmployees() {
    try {
      const data = await apiRequest('/api/v1/employees');
      if (data && data.length > 0) {
        employees.value = data.map(emp => {
          const docs = emp.certificates ? emp.certificates.split(',').map(name => ({
            name: name,
            size: '1.2 MB',
            date: new Date().toISOString().split('T')[0],
            type: 'Education'
          })) : [];

          return {
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
            onboardingStatus: emp.onboardingStatus || 'APPROVED',
            school: emp.school || '',
            college: emp.college || '',
            experience: emp.experience || '',
            certificates: emp.certificates || '',
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
            documents: docs
          };
        });
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
      
      const { useAuthStore } = await import('./authStore');
      const authStore = useAuthStore();
      const role = authStore.activeRole;
      
      let requests = [];
      if (role === 'HR_ADMIN' || role === 'SYSTEM_ADMIN') {
        requests = await apiRequest('/api/v1/leaves/requests');
      } else if (role === 'MANAGER') {
        const own = await apiRequest(`/api/v1/leaves/requests?employeeId=${employeeId}`) || [];
        const reports = await apiRequest(`/api/v1/leaves/requests/manager?managerId=${employeeId}`) || [];
        requests = [...own, ...reports];
      } else {
        requests = await apiRequest(`/api/v1/leaves/requests?employeeId=${employeeId}`) || [];
      }
      
      if (requests) {
        leaveRequests.value = requests.map(req => {
          const reqEmp = employees.value.find(e => e.id === req.employeeId);
          const daysReq = calculateLeaveDaysFrontend(req.employeeId, req.startDate, req.endDate);
          return {
            id: req.id,
            employeeCode: reqEmp?.employeeCode || 'EMP',
            fullName: reqEmp?.fullName || 'User',
            department: reqEmp?.department || 'Department',
            leaveType: req.leaveType === 'ANNUAL' ? 'Annual' : req.leaveType === 'SICK' ? 'Sick' : req.leaveType === 'CASUAL' ? 'Casual' : req.leaveType === 'UNPAID' ? 'Unpaid' : req.leaveType,
            fromDate: req.startDate,
            toDate: req.endDate,
            daysRequested: daysReq,
            reason: req.reason,
            status: req.status,
            approvedBy: req.managerComment ? 'Manager' : null,
            comments: req.managerComment,
            attachmentName: req.attachmentName || null,
            attachmentPath: req.attachmentPath || null,
            createdAt: req.createdAt
          };
        });
      }
    } catch (e) {
      console.warn('API error fetching leaves:', e.message);
    }
  }

  async function fetchExpenses(employeeId) {
    try {
      const { useAuthStore } = await import('./authStore');
      const authStore = useAuthStore();
      const role = authStore.activeRole;
      
      let claims = [];
      if (role === 'HR_ADMIN' || role === 'SYSTEM_ADMIN' || role === 'FINANCE_ADMIN') {
        claims = await apiRequest('/api/v1/expenses') || [];
      } else if (role === 'MANAGER') {
        const own = await apiRequest(`/api/v1/expenses?employeeId=${employeeId}`) || [];
        const reports = await apiRequest(`/api/v1/expenses/manager?managerId=${employeeId}`) || [];
        claims = [...own, ...reports];
      } else {
        claims = await apiRequest(`/api/v1/expenses?employeeId=${employeeId}`) || [];
      }

      if (claims) {
        const mappedClaims = await Promise.all(claims.map(async (c) => {
          const reqEmp = employees.value.find(e => e.id === c.employeeId);
          const mappedStatus = c.status === 'APPROVED_BY_MANAGER' ? 'APPROVED_MANAGER'
                             : c.status === 'APPROVED' ? 'APPROVED_FINANCE'
                             : c.status === 'REJECTED_BY_FINANCE' ? 'REJECTED'
                             : c.status;
          
          const timeline = [{ status: 'SUBMITTED', title: `Submitted by ${reqEmp?.fullName || 'Employee'}`, timestamp: c.createdAt }];
          if (mappedStatus === 'APPROVED_MANAGER' || mappedStatus === 'APPROVED_FINANCE' || mappedStatus === 'PAID') {
            timeline.push({ status: 'APPROVED_MANAGER', title: 'Approved by Manager', timestamp: c.createdAt });
          }
          if (mappedStatus === 'APPROVED_FINANCE' || mappedStatus === 'PAID') {
            timeline.push({ status: 'APPROVED_FINANCE', title: 'Approved by Finance', timestamp: c.createdAt });
          }
          if (mappedStatus === 'PAID') {
            timeline.push({ status: 'PAID', title: 'Marked Paid', timestamp: c.createdAt });
          }
          if (mappedStatus === 'REJECTED') {
            timeline.push({ status: 'REJECTED', title: 'Rejected', timestamp: c.createdAt });
          }
          
          let rName = 'receipt.pdf';
          try {
            const receipts = await apiRequest(`/api/v1/expenses/${c.id}/receipts`) || [];
            if (receipts.length > 0) {
              rName = receipts[0].fileName;
            }
          } catch (err) {
            console.warn(`Failed to fetch receipts for claim ${c.id}:`, err.message);
          }

          return {
            id: c.id,
            employeeId: c.employeeId,
            employeeCode: reqEmp?.employeeCode || 'EMP',
            fullName: reqEmp?.fullName || 'User',
            department: reqEmp?.department || 'Department',
            category: c.category.charAt(0).toUpperCase() + c.category.slice(1).toLowerCase(),
            amount: c.amount,
            currency: c.currency,
            date: c.createdAt?.split('T')[0] || new Date().toISOString().split('T')[0],
            description: c.description,
            receiptName: rName,
            status: mappedStatus,
            timeline,
            paymentRef: mappedStatus === 'PAID' ? 'TXN-9847582049' : null
          };
        }));
        expenseClaims.value = mappedClaims;
      }
    } catch (e) {
      console.warn('API error fetching expenses:', e.message);
    }
  }

  async function fetchPayslips(employeeId) {
    try {
      const { useAuthStore } = await import('./authStore');
      const authStore = useAuthStore();
      const role = authStore.activeRole;
      
      let list = [];
      if (role === 'HR_ADMIN' || role === 'FINANCE_ADMIN' || role === 'SYSTEM_ADMIN') {
        list = await apiRequest('/api/v1/payroll/all') || [];
      } else {
        list = await apiRequest(`/api/v1/payroll/employee/${employeeId}`) || [];
      }
      
      if (list) {
        payslips.value = list.map(p => {
          const emp = employees.value.find(e => e.id === p.employeeId);
          const basic = Math.round((p.grossPay - p.deduction) * 0.6);
          const allowances = p.grossPay - basic;
          const pf = Math.round(p.deduction * 0.5);
          const tax = p.deduction - pf;
          return {
            id: p.id,
            employeeCode: emp ? emp.employeeCode : 'EMP',
            month: convertMonthToYyyyMm(p.month, p.year),
            grossPay: p.grossPay,
            allowances: allowances,
            deductions: p.deduction,
            netSalary: p.netPay,
            basic: basic,
            pf: pf,
            tax: tax,
            status: p.published ? 'PUBLISHED' : 'DRAFT',
            publishedAt: p.createdAt
          };
        });
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
    await fetchPublicHolidays();
    await fetchLeaves(employeeId);
    await fetchExpenses(employeeId);
    await fetchPayslips(employeeId);
    await fetchNotifications(userId);
    await fetchMandatoryDocuments();
    await fetchEmployeeDocuments(employeeId);
  }

  async function approveOnboarding(empId) {
    try {
      await apiRequest(`/api/v1/employees/${empId}/approve-onboarding`, {
        method: 'POST'
      });
      const emp = employees.value.find(e => e.id === empId);
      if (emp) {
        emp.onboardingStatus = 'APPROVED';
        emp.onboardingPercent = 100;
      }
      return true;
    } catch (e) {
      console.error('Approval failed:', e);
      return false;
    }
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
    mandatoryDocuments,
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
    rejectPayrollBatch,
    updatePayslip,
    addNewEmployee,
    updateEmployeeHrView,
    triggerSchedulerJob,
    toggleSchedulerJob,
    fetchEmployees,
    fetchLeaves,
    fetchExpenses,
    fetchPayslips,
    fetchNotifications,
    approveOnboarding,
    syncAll,
    fetchMandatoryDocuments,
    addMandatoryDocument,
    fetchEmployeeDocuments,
    uploadDocument,
    deleteDocument,
    fetchPublicHolidays,
    addPublicHoliday,
    uploadLeaveAttachment,
    uploadExpenseReceipt,
    generatePayrollRun,
    calculateLeaveDaysFrontend
  };
});
