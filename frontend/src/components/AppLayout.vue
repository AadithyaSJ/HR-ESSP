<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '../stores/authStore.js';
import { useHrStore } from '../stores/hrStore.js';
import IconHelper from './IconHelper.vue';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const hrStore = useHrStore();

const isSidebarCollapsed = ref(false);
const isNotificationOpen = ref(false);
const isProfileOpen = ref(false);

// Auto-trigger idle warning check
onMounted(() => {
  authStore.startIdleTimer(router);
});

onUnmounted(() => {
  authStore.stopIdleTimer();
});

const activeRoleName = computed(() => {
  const map = {
    EMPLOYEE: 'Employee',
    MANAGER: 'Manager / Lead',
    HR_ADMIN: 'HR Administrator',
    FINANCE_ADMIN: 'Finance Admin',
    SYSTEM_ADMIN: 'System Admin'
  };
  return map[authStore.activeRole] || authStore.activeRole;
});

// Notifications
const unreadNotificationCount = computed(() => {
  return hrStore.notifications.filter(n => !n.isRead).length;
});

function markAllNotificationsRead() {
  hrStore.notifications.forEach(n => n.isRead = true);
  hrStore.addLog(authStore.user.email, 'Notifications', 'MARK_ALL_READ', 'Marked all notifications as read');
}

function selectNotification(notif) {
  notif.isRead = true;
  isNotificationOpen.value = false;
  if (notif.url) {
    router.push(notif.url);
  }
}

// Sidebar links filtering based on active role
const menuGroups = computed(() => {
  const role = authStore.activeRole;
  
  const groups = [
    {
      title: 'General',
      items: [
        { name: 'My Profile', icon: 'user', path: '/profile', roles: ['EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN'] },
        { name: 'Onboarding Checklist', icon: 'clipboard', path: '/onboarding', roles: ['EMPLOYEE', 'HR_ADMIN'] },
        { name: 'My Documents', icon: 'file-text', path: '/documents', roles: ['EMPLOYEE', 'MANAGER'] }
      ]
    },
    {
      title: 'Leave & Expenses',
      items: [
        { name: 'My Leaves', icon: 'calendar', path: '/leave', roles: ['EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN'] },
        { name: 'Leave Calendar', icon: 'calendar', path: '/leave/calendar', roles: ['EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN'] },
        { name: 'Team Leaves', icon: 'check', path: '/manager/leave', roles: ['MANAGER', 'HR_ADMIN'] },
        { name: 'My Expenses', icon: 'credit-card', path: '/expense', roles: ['EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN'] },
        { name: 'Team Expenses', icon: 'check-circle', path: '/manager/expense', roles: ['MANAGER'] },
        { name: 'Finance Expense Queue', icon: 'credit-card', path: '/finance/expense', roles: ['FINANCE_ADMIN'] }
      ]
    },
    {
      title: 'Payslips & Payroll',
      items: [
        { name: 'My Payslips', icon: 'dollar', path: '/payslip', roles: ['EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN'] },
        { name: 'Payroll Processing', icon: 'upload', path: '/hr/payroll/upload', roles: ['HR_ADMIN'] }
      ]
    },
    {
      title: 'HR Employee Hub',
      items: [
        { name: 'Employee Directory', icon: 'users', path: '/hr/employees', roles: ['HR_ADMIN', 'MANAGER'] },
        { name: 'Add Employee', icon: 'plus', path: '/hr/employees/new', roles: ['HR_ADMIN'] },
        { name: 'Org Chart', icon: 'users', path: '/manager/orgchart', roles: ['MANAGER', 'HR_ADMIN'] }
      ]
    },
    {
      title: 'Analytics & Reports',
      items: [
        { name: 'Headcount Report', icon: 'bar-chart', path: '/reports/headcount', roles: ['HR_ADMIN'] },
        { name: 'Leave Utilisation', icon: 'bar-chart', path: '/reports/leave', roles: ['HR_ADMIN'] },
        { name: 'Expense Summary', icon: 'bar-chart', path: '/reports/expense', roles: ['HR_ADMIN', 'FINANCE_ADMIN'] }
      ]
    },
    {
      title: 'System Config',
      items: [
        { name: 'Leave Policy Config', icon: 'settings', path: '/admin/leave-policy', roles: ['HR_ADMIN'] },
        { name: 'Expense Limits Config', icon: 'settings', path: '/admin/expense-limits', roles: ['HR_ADMIN'] },
        { name: 'Currency Rates', icon: 'settings', path: '/admin/currency-rates', roles: ['HR_ADMIN', 'FINANCE_ADMIN'] },
        { name: 'System Settings', icon: 'settings', path: '/admin/settings', roles: ['SYSTEM_ADMIN'] },
        { name: 'User Management', icon: 'users', path: '/admin/users', roles: ['SYSTEM_ADMIN'] },
        { name: 'Email Templates', icon: 'file-text', path: '/admin/email-templates', roles: ['HR_ADMIN', 'SYSTEM_ADMIN'] },
        { name: 'Audit Logs', icon: 'file-text', path: '/admin/audit', roles: ['SYSTEM_ADMIN'] },
        { name: 'Scheduler Jobs', icon: 'clock', path: '/admin/scheduler', roles: ['SYSTEM_ADMIN'] }
      ]
    }
  ];

  return groups.map(g => ({
    ...g,
    items: g.items.filter(item => item.roles.includes(role))
  })).filter(g => g.items.length > 0);
});

// Toast System
const toasts = ref([]);

function addToast(title, type = 'success') {
  const id = Date.now();
  toasts.value.push({ id, title, type });
  setTimeout(() => {
    toasts.value = toasts.value.filter(t => t.id !== id);
  }, 4000);
}

// Expose toast method for inner pages
window.showPortalToast = (title, type) => {
  addToast(title, type);
};

function switchRole(role) {
  authStore.setRole(role);
  addToast(`Switched active workspace role to ${role.replace('_', ' ')}`, 'info');
  isProfileOpen.value = false;
  // If route is restricted, redirect to profile
  router.push('/profile');
}

function handleLogOut() {
  authStore.logout();
  router.push('/login');
}

// Reset idle timer on clicks or key presses
function activityDetected() {
  authStore.resetIdleTimer();
}

// Format date
function formatDate(d) {
  return new Date(d).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}
</script>

<template>
  <div class="app-layout" @click="activityDetected" @keypress="activityDetected">
    <!-- BACKGROUND GLOW EFFECTS -->
    <div class="bg-glow bg-glow-1"></div>
    <div class="bg-glow bg-glow-2"></div>

    <!-- MAIN APP WRAPPER -->
    <div class="app-container">
      
      <!-- COLLAPSIBLE LEFT SIDEBAR -->
      <aside class="sidebar-wrapper" :class="{ collapsed: isSidebarCollapsed }">
        <div class="sidebar-header">
          <div class="logo-area">
            <div class="logo-box">
              <IconHelper name="shield" size="24" color="#818cf8" />
            </div>
            <span class="logo-text" v-if="!isSidebarCollapsed">Excellathon</span>
          </div>
          <button class="btn-collapse" @click="isSidebarCollapsed = !isSidebarCollapsed">
            <IconHelper :name="isSidebarCollapsed ? 'plus' : 'arrow-left'" size="16" />
          </button>
        </div>

        <nav class="sidebar-nav">
          <div class="nav-group" v-for="group in menuGroups" :key="group.title">
            <div class="nav-group-title" v-if="!isSidebarCollapsed">{{ group.title }}</div>
            <div class="nav-items">
              <router-link
                v-for="item in group.items"
                :key="item.path"
                :to="item.path"
                class="nav-item-link"
                :class="{ active: route.path === item.path }"
              >
                <div class="nav-item-icon">
                  <IconHelper :name="item.icon" size="18" />
                </div>
                <span class="nav-item-name" v-if="!isSidebarCollapsed">{{ item.name }}</span>
              </router-link>
            </div>
          </div>
        </nav>

        <div class="sidebar-footer" v-if="!isSidebarCollapsed">
          <div class="user-pill">
            <img :src="hrStore.employees[0].photo" alt="Photo" class="user-avatar-sm" />
            <div class="user-pill-details">
              <div class="user-pill-name">{{ authStore.user.fullName }}</div>
              <div class="user-pill-role">{{ activeRoleName }}</div>
            </div>
          </div>
        </div>
      </aside>

      <!-- MAIN PAGE FRAME -->
      <div class="main-content">
        <!-- TOP TOOLBAR NAVBAR -->
        <header class="navbar-wrapper">
          <div class="navbar-left">
            <h2 class="page-title">{{ route.name ? route.name.toString().toUpperCase().replace('-', ' ') : 'ESSP PORTAL' }}</h2>
          </div>

          <div class="navbar-right">
            <!-- ROLE SWITCHER SELECT -->
            <div class="role-switcher-dropdown">
              <button class="btn btn-secondary btn-sm" @click="isProfileOpen = !isProfileOpen">
                <IconHelper name="shield" size="14" color="#818cf8" />
                <span>Role: {{ activeRoleName }}</span>
              </button>
              <div class="role-dropdown-menu glass-card" v-if="isProfileOpen">
                <div class="role-dropdown-header">Switch Portal Role</div>
                <button class="role-item-btn" :class="{active: authStore.activeRole === 'EMPLOYEE'}" @click="switchRole('EMPLOYEE')">Employee View</button>
                <button class="role-item-btn" :class="{active: authStore.activeRole === 'MANAGER'}" @click="switchRole('MANAGER')">Manager Self-Service</button>
                <button class="role-item-btn" :class="{active: authStore.activeRole === 'HR_ADMIN'}" @click="switchRole('HR_ADMIN')">HR Admin View</button>
                <button class="role-item-btn" :class="{active: authStore.activeRole === 'FINANCE_ADMIN'}" @click="switchRole('FINANCE_ADMIN')">Finance Admin View</button>
                <button class="role-item-btn" :class="{active: authStore.activeRole === 'SYSTEM_ADMIN'}" @click="switchRole('SYSTEM_ADMIN')">System Admin Console</button>
              </div>
            </div>

            <!-- MOCK SESSION TIMEOUT PROGRESS -->
            <div class="session-timer-pill" title="Time remaining until session expires. Toggles warning at 18 mins.">
              <IconHelper name="clock" size="14" color="#94a3b8" />
              <span>{{ Math.max(0, Math.floor((authStore.timeLimitSeconds - authStore.idleTimeSeconds) / 60)) }}m left</span>
            </div>

            <!-- NOTIFICATIONS POPUP BELL -->
            <div class="navbar-icon-btn" @click="isNotificationOpen = !isNotificationOpen">
              <IconHelper name="bell" size="18" />
              <div class="unread-badge" v-if="unreadNotificationCount > 0">{{ unreadNotificationCount }}</div>
              
              <!-- NOTIFICATIONS DRAWER PANEL -->
              <div class="notif-dropdown glass-card" v-if="isNotificationOpen" @click.stop>
                <div class="notif-dropdown-header">
                  <h3>Notifications Center</h3>
                  <button class="btn btn-ghost btn-sm" @click="markAllNotificationsRead">Mark all read</button>
                </div>
                <div class="notif-dropdown-body">
                  <div v-if="hrStore.notifications.length === 0" class="notif-empty">No notifications yet.</div>
                  <div
                    v-for="notif in hrStore.notifications"
                    :key="notif.id"
                    class="notif-item"
                    :class="{ unread: !notif.isRead }"
                    @click="selectNotification(notif)"
                  >
                    <div class="notif-item-icon" :class="notif.type.toLowerCase()">
                      <IconHelper :name="notif.type === 'Leave' ? 'calendar' : notif.type === 'Expense' ? 'credit-card' : 'bell'" size="14" />
                    </div>
                    <div class="notif-item-content">
                      <div class="notif-item-title">{{ notif.title }}</div>
                      <div class="notif-item-msg">{{ notif.message }}</div>
                      <div class="notif-item-time">{{ formatDate(notif.createdAt) }}</div>
                    </div>
                  </div>
                </div>
                <div class="notif-dropdown-footer">
                  <router-link to="/notifications" class="btn btn-ghost btn-sm btn-full" @click="isNotificationOpen = false">View All</router-link>
                </div>
              </div>
            </div>

            <!-- LOGOUT BUTTON -->
            <button class="navbar-icon-btn text-danger-btn" @click="handleLogOut" title="Log Out">
              <IconHelper name="log-out" size="18" />
            </button>
          </div>
        </header>

        <!-- VIEW WRAPPER -->
        <main class="content-body">
          <router-view />
        </main>
      </div>

      <!-- IDLE SESSION WARNING DIALOG MODAL (At 18 min idle / 1080s) -->
      <div class="modal-overlay" v-if="authStore.isAuthenticated && authStore.idleTimeSeconds >= authStore.warningLimitSeconds">
        <div class="modal-content text-center" style="max-width: 400px;">
          <div class="modal-body p-6">
            <div class="mb-4">
              <IconHelper name="clock" size="48" color="#f59e0b" />
            </div>
            <h3 class="mb-2">Session Expiry Warning</h3>
            <p class="text-secondary mb-4">
              You have been inactive for {{ Math.floor(authStore.idleTimeSeconds / 60) }} minutes. Your session will expire in
              <b>{{ Math.max(0, Math.floor((authStore.timeLimitSeconds - authStore.idleTimeSeconds))) }} seconds</b>.
            </p>
            <button class="btn btn-primary btn-full" @click="authStore.resetIdleTimer()">Extend Session</button>
          </div>
        </div>
      </div>

      <!-- ALERT TOAST MESSAGES -->
      <div class="alert-container">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          class="alert"
          :class="`alert-${toast.type}`"
        >
          <div class="flex items-center gap-2">
            <IconHelper :name="toast.type === 'success' ? 'check-circle' : 'info'" size="18" />
            <span>{{ toast.title }}</span>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
/* App Layout Container styles */
.app-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* Sidebar Wrapper */
.sidebar-wrapper {
  width: 260px;
  background-color: var(--bg-sidebar);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  height: 100vh;
  position: sticky;
  top: 0;
  transition: width var(--transition-normal);
  z-index: 100;
}
.sidebar-wrapper.collapsed {
  width: 68px;
}

.sidebar-header {
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-color);
}
.logo-area {
  display: flex;
  align-items: center;
  gap: 12px;
}
.logo-box {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  background-color: var(--primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo-text {
  font-weight: 700;
  font-size: 16px;
  letter-spacing: 0.05em;
  background: linear-gradient(135deg, #fff 0%, #818cf8 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.btn-collapse {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px;
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 16px 8px;
}
.nav-group {
  margin-bottom: 20px;
}
.nav-group-title {
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--text-muted);
  letter-spacing: 0.1em;
  padding: 0 12px 8px;
}
.nav-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.nav-item-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}
.nav-item-link:hover {
  background-color: rgba(255, 255, 255, 0.05);
  color: var(--text-primary);
}
.nav-item-link.active {
  background-color: var(--primary-light);
  color: var(--primary-color);
  font-weight: 500;
}
.nav-item-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid var(--border-color);
}
.user-pill {
  display: flex;
  align-items: center;
  gap: 10px;
  background-color: rgba(255, 255, 255, 0.02);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 8px 10px;
}
.user-avatar-sm {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}
.user-pill-details {
  overflow: hidden;
}
.user-pill-name {
  font-weight: 600;
  font-size: 12px;
  color: var(--text-primary);
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}
.user-pill-role {
  font-size: 10px;
  color: var(--text-muted);
}

/* Navbar */
.navbar-wrapper {
  background-color: var(--bg-main);
  border-bottom: 1px solid var(--border-color);
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  position: sticky;
  top: 0;
  z-index: 90;
}
.navbar-left {
  display: flex;
  align-items: center;
}
.page-title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--text-primary);
}
.navbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* Dropdown styling */
.role-switcher-dropdown {
  position: relative;
}
.role-dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 220px;
  padding: 8px;
  z-index: 150;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.role-dropdown-header {
  font-size: 10px;
  text-transform: uppercase;
  color: var(--text-muted);
  font-weight: 700;
  padding: 8px;
}
.role-item-btn {
  background: none;
  border: none;
  font-family: var(--font-family);
  font-size: 13px;
  padding: 8px;
  text-align: left;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}
.role-item-btn:hover {
  background-color: rgba(255, 255, 255, 0.05);
  color: var(--text-primary);
}
.role-item-btn.active {
  background-color: var(--primary-light);
  color: var(--primary-color);
  font-weight: 500;
}

.session-timer-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  background-color: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  padding: 6px 12px;
  border-radius: 9999px;
  font-size: 12px;
  color: var(--text-secondary);
}

.navbar-icon-btn {
  background: none;
  border: none;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  transition: all var(--transition-fast);
}
.navbar-icon-btn:hover {
  background-color: rgba(255, 255, 255, 0.05);
  color: var(--text-primary);
}
.unread-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background-color: var(--error);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.text-danger-btn:hover {
  border-color: rgba(239, 68, 68, 0.4);
  color: var(--error);
}

/* Notifications Drawer */
.notif-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 360px;
  padding: 0;
  z-index: 150;
  cursor: default;
}
.notif-dropdown-header {
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.notif-dropdown-body {
  max-height: 280px;
  overflow-y: auto;
}
.notif-empty {
  padding: 24px;
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
}
.notif-item {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  transition: background-color var(--transition-fast);
}
.notif-item:hover {
  background-color: rgba(255, 255, 255, 0.02);
}
.notif-item.unread {
  background-color: rgba(99, 102, 241, 0.05);
}
.notif-item-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.notif-item-icon.leave { background-color: var(--info-light); color: var(--info); }
.notif-item-icon.expense { background-color: var(--warning-light); color: var(--warning); }
.notif-item-icon.payslip { background-color: var(--success-light); color: var(--success); }
.notif-item-icon.system { background-color: var(--primary-light); color: var(--primary-color); }
.notif-item-content {
  flex: 1;
}
.notif-item-title {
  font-weight: 600;
  font-size: 13px;
}
.notif-item-msg {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}
.notif-item-time {
  font-size: 10px;
  color: var(--text-muted);
  margin-top: 4px;
}
.notif-dropdown-footer {
  padding: 8px;
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: center;
  background-color: rgba(15, 23, 42, 0.2);
}
.btn-full {
  width: 100%;
}
</style>
