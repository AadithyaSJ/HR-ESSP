import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/authStore.js'

import LoginView from '../views/LoginView.vue'
import ForgotPasswordView from '../views/ForgotPasswordView.vue'
import SessionExpiredView from '../views/SessionExpiredView.vue'
import ProfileView from '../views/ProfileView.vue'
import EmployeeDirectoryView from '../views/EmployeeDirectoryView.vue'
import LeaveHubView from '../views/LeaveHubView.vue'
import ExpenseHubView from '../views/ExpenseHubView.vue'
import PayrollHubView from '../views/PayrollHubView.vue'
import ManagerHubView from '../views/ManagerHubView.vue'
import ReportsHubView from '../views/ReportsHubView.vue'
import AdminHubView from '../views/AdminHubView.vue'
import NotificationsView from '../views/NotificationsView.vue'

const routes = [
  // --- AUTH ROUTES ---
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { public: true }
  },
  {
    path: '/forgot-password',
    name: 'forgot-password',
    component: ForgotPasswordView,
    meta: { public: true }
  },
  {
    path: '/session-expired',
    name: 'session-expired',
    component: SessionExpiredView,
    meta: { public: true }
  },

  // --- MAIN LAYOUT ROUTES ---
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('../views/DashboardView.vue')
  },
  {
    path: '/profile',
    name: 'profile',
    component: ProfileView,
    meta: { tab: 'details' }
  },
  {
    path: '/onboarding',
    name: 'onboarding',
    component: ProfileView,
    meta: { tab: 'onboarding' }
  },
  {
    path: '/onboarding-setup',
    name: 'onboarding-setup',
    component: () => import('../views/OnboardingSetupView.vue')
  },
  {
    path: '/documents',
    name: 'documents',
    component: ProfileView,
    meta: { tab: 'documents' }
  },

  // --- HR / EMPLOYEE MANAGEMENT ---
  {
    path: '/hr/employees',
    name: 'hr-employees',
    component: EmployeeDirectoryView,
    meta: { tab: 'directory', roles: ['HR_ADMIN', 'MANAGER'] }
  },
  {
    path: '/hr/employees/new',
    name: 'hr-employees-new',
    component: EmployeeDirectoryView,
    meta: { tab: 'new-employee', roles: ['HR_ADMIN'] }
  },
  {
    path: '/hr/employees/onboarding',
    name: 'hr-employees-onboarding',
    component: EmployeeDirectoryView,
    meta: { tab: 'onboarding', roles: ['HR_ADMIN'] }
  },
  {
    path: '/hr/employees/:id',
    name: 'hr-employee-detail',
    component: EmployeeDirectoryView,
    meta: { tab: 'edit-employee', roles: ['HR_ADMIN'] }
  },
  {
    path: '/hr/documents',
    name: 'hr-documents',
    component: EmployeeDirectoryView,
    meta: { tab: 'documents', roles: ['HR_ADMIN'] }
  },

  // --- LEAVE MANAGEMENT ---
  {
    path: '/leave',
    name: 'leave',
    component: LeaveHubView,
    meta: { tab: 'my-leaves' }
  },
  {
    path: '/leave/requests',
    name: 'leave-requests',
    component: LeaveHubView,
    meta: { tab: 'approvals', roles: ['MANAGER', 'HR_ADMIN'] }
  },
  {
    path: '/leave/policies',
    name: 'leave-policies',
    component: LeaveHubView,
    meta: { tab: 'policy', roles: ['HR_ADMIN'] }
  },
  {
    path: '/leave/holidays',
    name: 'leave-holidays',
    component: LeaveHubView,
    meta: { tab: 'calendar' }
  },
  // Legacy redirects
  {
    path: '/leave/calendar',
    redirect: '/leave/holidays'
  },
  {
    path: '/manager/leave',
    redirect: '/leave/requests'
  },
  {
    path: '/admin/leave-policy',
    redirect: '/leave/policies'
  },

  // --- EXPENSE CLAIMS ---
  {
    path: '/expense',
    name: 'expense',
    component: ExpenseHubView,
    meta: { tab: 'my-expenses' }
  },
  {
    path: '/manager/expense',
    name: 'manager-expense',
    component: ExpenseHubView,
    meta: { tab: 'approvals' }
  },
  {
    path: '/finance/expense',
    name: 'finance-expense',
    component: ExpenseHubView,
    meta: { tab: 'finance-queue', roles: ['FINANCE_ADMIN'] }
  },
  {
    path: '/admin/expense-limits',
    name: 'admin-expense-limits',
    component: ExpenseHubView,
    meta: { tab: 'limits' }
  },
  {
    path: '/admin/currency-rates',
    name: 'admin-currency-rates',
    component: ExpenseHubView,
    meta: { tab: 'currencies' }
  },

  // --- PAYSLIP & PAYROLL ---
  {
    path: '/payslip',
    name: 'payslip',
    component: PayrollHubView,
    meta: { tab: 'my-payslips' }
  },
  {
    path: '/hr/payroll/upload',
    name: 'payroll-upload',
    component: PayrollHubView,
    meta: { tab: 'upload', roles: ['HR_ADMIN'] }
  },
  {
    path: '/hr/payroll/publish',
    name: 'payroll-publish',
    component: PayrollHubView,
    meta: { tab: 'publish', roles: ['HR_ADMIN'] }
  },
  {
    path: '/hr/payroll/history',
    name: 'payroll-history',
    component: PayrollHubView,
    meta: { tab: 'history', roles: ['HR_ADMIN', 'FINANCE_ADMIN'] }
  },

  // --- MANAGER SELF SERVICE ---
  {
    path: '/manager/dashboard',
    name: 'manager-dashboard',
    component: ManagerHubView,
    meta: { tab: 'dashboard', roles: ['MANAGER'] }
  },
  {
    path: '/manager/team',
    name: 'manager-team',
    component: ManagerHubView,
    meta: { tab: 'team', roles: ['MANAGER'] }
  },
  {
    path: '/manager/orgchart',
    name: 'manager-orgchart',
    component: ManagerHubView,
    meta: { tab: 'orgchart', roles: ['MANAGER', 'HR_ADMIN'] }
  },

  // --- REPORTS ---
  {
    path: '/reports/headcount',
    name: 'report-headcount',
    component: ReportsHubView,
    meta: { tab: 'headcount', roles: ['HR_ADMIN'] }
  },
  {
    path: '/reports/leave',
    name: 'report-leave',
    component: ReportsHubView,
    meta: { tab: 'leave', roles: ['HR_ADMIN'] }
  },
  {
    path: '/reports/expense',
    name: 'report-expense',
    component: ReportsHubView,
    meta: { tab: 'expense', roles: ['HR_ADMIN', 'FINANCE_ADMIN'] }
  },

  // --- ADMIN CONFIGURATION ---
  {
    path: '/admin/settings',
    name: 'admin-settings',
    component: AdminHubView,
    meta: { tab: 'settings', roles: ['SYSTEM_ADMIN'] }
  },
  {
    path: '/admin/users',
    name: 'admin-users',
    component: AdminHubView,
    meta: { tab: 'users', roles: ['SYSTEM_ADMIN'] }
  },
  {
    path: '/admin/email-templates',
    name: 'admin-email-templates',
    component: AdminHubView,
    meta: { tab: 'emails', roles: ['HR_ADMIN', 'SYSTEM_ADMIN'] }
  },
  {
    path: '/admin/audit',
    name: 'admin-audit',
    component: AdminHubView,
    meta: { tab: 'audit', roles: ['SYSTEM_ADMIN'] }
  },
  {
    path: '/admin/scheduler',
    name: 'admin-scheduler',
    component: AdminHubView,
    meta: { tab: 'scheduler', roles: ['SYSTEM_ADMIN'] }
  },

  // --- NOTIFICATIONS ---
  {
    path: '/notifications',
    name: 'notifications',
    component: NotificationsView,
    meta: { tab: 'inbox' }
  },
  {
    path: '/notifications/preferences',
    name: 'notification-preferences',
    component: NotificationsView,
    meta: { tab: 'preferences' }
  },

  // Catch-all redirect to dashboard
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// Route guard to check auth & active role clearance
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  // If not logged in and going to non-public page, redirect to login
  if (!authStore.isAuthenticated && !to.meta.public) {
    next('/login')
    return
  } 

  // If logged in and onboarding is not approved, restrict them strictly to /onboarding-setup
  if (authStore.isAuthenticated && authStore.user.onboardingStatus && authStore.user.onboardingStatus !== 'APPROVED') {
    if (to.name !== 'onboarding-setup' && to.name !== 'session-expired') {
      next('/onboarding-setup')
      return
    }
  }

  // If logged in and going to login/forgot password, redirect to dashboard
  if (authStore.isAuthenticated && to.meta.public && to.name !== 'session-expired') {
    next('/dashboard')
  }
  // Role checks
  else if (to.meta.roles && !to.meta.roles.includes(authStore.activeRole)) {
    // Redirect to dashboard if active role is not cleared
    next('/dashboard')
  }
  else {
    next()
  }
})

export default router
