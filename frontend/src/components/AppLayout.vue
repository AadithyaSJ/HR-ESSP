<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
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
const isProfileDropdownOpen = ref(false);
const isMobileOpen = ref(false);
const searchQuery = ref('');

const expandedGroups = ref({
  'Employee Management': true,
  'Leave Management': false,
  'Payroll': false,
  'Expenses': false,
  'Reports': false,
  'Communication': false,
  'Administration': false
});

function toggleGroup(title) {
  expandedGroups.value[title] = !expandedGroups.value[title];
}

function isGroupActive(group) {
  return group.items.some(item => route.path === item.path);
}

function toggleSidebar() {
  if (window.innerWidth <= 768) {
    isMobileOpen.value = !isMobileOpen.value;
  } else {
    isSidebarCollapsed.value = !isSidebarCollapsed.value;
  }
}

const hasReports = computed(() => {
  return hrStore.employees.some(e => e.managerId === authStore.user?.id);
});

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
    HR_ADMIN: 'HR Manager',
    FINANCE_ADMIN: 'Finance Admin',
    SYSTEM_ADMIN: 'System Admin',
    IT_SUPPORT: 'IT Support Specialist'
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
      title: 'Employee Management',
      icon: 'users',
      items: [
        { name: 'Employee Directory', icon: 'users', path: '/hr/employees', roles: ['HR_ADMIN', 'MANAGER'] },
        { name: 'Add Employee', icon: 'plus', path: '/hr/employees/new', roles: ['HR_ADMIN'] },
        { name: 'Onboarding', icon: 'clipboard', path: '/hr/employees/onboarding', roles: ['HR_ADMIN'] },
        { name: 'Org Chart', icon: 'users', path: '/manager/orgchart', roles: ['MANAGER', 'HR_ADMIN'] }
      ]
    },
    {
      title: 'Leave Management',
      icon: 'calendar',
      items: [
        { name: 'My Leaves', icon: 'calendar', path: '/leave', roles: ['EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN'] },
        { name: 'Leave Calendar', icon: 'calendar', path: '/leave/calendar', roles: ['EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN'] },
        { name: 'Team Leaves', icon: 'check', path: '/manager/leave', roles: ['MANAGER', 'HR_ADMIN'] },
        { name: 'Leave Policy Config', icon: 'settings', path: '/admin/leave-policy', roles: ['HR_ADMIN'] },
        { name: 'Resignation Desk', icon: 'log-out', path: '/resignation', roles: ['EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN'] }
      ]
    },
    {
      title: 'Payroll',
      icon: 'dollar',
      items: [
        { name: 'My Payslips', icon: 'dollar', path: '/payslip', roles: ['EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN'] },
        { name: 'Payroll Processing', icon: 'upload', path: '/hr/payroll/upload', roles: ['HR_ADMIN'] },
        { name: 'Overtime Logs', icon: 'clock', path: '/overtime', roles: ['EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN'] }
      ]
    },
    {
      title: 'Expenses',
      icon: 'credit-card',
      items: [
        { name: 'My Expenses', icon: 'credit-card', path: '/expense', roles: ['EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN'] },
        { name: 'Finance Expense Queue', icon: 'credit-card', path: '/finance/expense', roles: ['FINANCE_ADMIN'] },
        { name: 'Expense Limits Config', icon: 'settings', path: '/admin/expense-limits', roles: ['EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN'] },
        { name: 'Currency Rates', icon: 'settings', path: '/admin/currency-rates', roles: ['EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN'] },
        { name: 'Travel Desk', icon: 'credit-card', path: '/travel-desk', roles: ['EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN'] }
      ]
    },
    {
      title: 'Technical Support',
      icon: 'settings',
      items: [
        { name: 'IT Support Kiosk', icon: 'settings', path: '/it-kiosk', roles: ['EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN', 'IT_SUPPORT'] }
      ]
    },
    {
      title: 'Reports',
      icon: 'bar-chart',
      items: [
        { name: 'Headcount Report', icon: 'bar-chart', path: '/reports/headcount', roles: ['HR_ADMIN'] },
        { name: 'Leave Utilisation', icon: 'bar-chart', path: '/reports/leave', roles: ['HR_ADMIN'] },
        { name: 'Expense Summary', icon: 'bar-chart', path: '/reports/expense', roles: ['HR_ADMIN', 'FINANCE_ADMIN'] }
      ]
    },
    {
      title: 'Communication',
      icon: 'bell',
      items: [
        { name: 'Announcements', icon: 'bell', path: '/notifications', roles: ['EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN'] },
        { name: 'Preferences', icon: 'settings', path: '/notifications/preferences', roles: ['EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN'] }
      ]
    },
    {
      title: 'Administration',
      icon: 'settings',
      items: [
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
    items: g.items.filter(item => {
      if (item.path === '/manager/leave' && hasReports.value) return true;
      return item.roles.includes(role);
    })
  })).filter(g => g.items.length > 0);
});

watch(() => route.path, (newPath) => {
  isMobileOpen.value = false;
  isProfileDropdownOpen.value = false;
  isNotificationOpen.value = false;
  
  // Expand group that matches active route
  if (menuGroups.value) {
    menuGroups.value.forEach(g => {
      if (g.items && g.items.some(item => item.path === newPath)) {
        expandedGroups.value[g.title] = true;
      }
    });
  }
}, { immediate: true });

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
      <aside class="sidebar-wrapper" :class="{ collapsed: isSidebarCollapsed, 'mobile-open': isMobileOpen }">
        <div class="sidebar-header">
          <div class="logo-area">
            <div class="logo-box">
              <IconHelper name="shield" size="20" color="#ffffff" />
            </div>
            <span class="logo-text" v-if="!isSidebarCollapsed">HR Admin</span>
          </div>
        </div>

        <nav class="sidebar-nav">
          <!-- Flat Dashboard Link -->
          <router-link to="/dashboard" class="nav-item-link" :class="{ active: route.path === '/dashboard' }">
            <div class="nav-item-icon">
              <IconHelper name="home" size="18" />
            </div>
            <span class="nav-item-name" v-if="!isSidebarCollapsed">Dashboard</span>
          </router-link>

          <!-- Collapsible Groups -->
          <div class="nav-group" v-for="group in menuGroups" :key="group.title">
            <div class="nav-group-header" :class="{ active: isGroupActive(group) }" v-if="!isSidebarCollapsed" @click="toggleGroup(group.title)">
              <div class="flex items-center gap-3">
                <div class="nav-item-icon">
                  <IconHelper :name="group.icon" size="18" />
                </div>
                <span class="nav-item-name">{{ group.title }}</span>
                <span class="badge badge-danger badge-xs ml-1" style="font-size: 8px; padding: 2px 6px; text-transform: none;" v-if="group.title === 'Communication' && unreadNotificationCount > 0">{{ unreadNotificationCount }}</span>
              </div>
              <svg class="group-arrow" :class="{ rotated: expandedGroups[group.title] }" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
            </div>
            <!-- Collapsed state fallback showing only icon -->
            <div class="nav-group-header-collapsed" v-else @click="isSidebarCollapsed = false" :title="group.title" :class="{ active: isGroupActive(group) }">
              <IconHelper :name="group.icon" size="18" />
            </div>
            <div class="nav-items" v-show="!isSidebarCollapsed && expandedGroups[group.title]">
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

          <!-- Flat My Workspace Link -->
          <router-link to="/profile" class="nav-item-link" :class="{ active: route.path === '/profile' }">
            <div class="nav-item-icon">
              <IconHelper name="file-text" size="18" />
            </div>
            <span class="nav-item-name" v-if="!isSidebarCollapsed">My Workspace</span>
          </router-link>
        </nav>

        <div class="sidebar-footer" v-if="!isSidebarCollapsed">
          <div class="user-pill" style="justify-content: space-between; cursor: pointer;" @click="isProfileDropdownOpen = !isProfileDropdownOpen">
            <div class="flex items-center gap-2">
              <img :src="authStore.user.photo || 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150'" alt="Lisa Anderson" class="user-avatar-sm" />
              <div class="user-pill-details">
                <div class="user-pill-name">{{ authStore.user.fullName }}</div>
                <div class="user-pill-role">{{ activeRoleName }}</div>
              </div>
            </div>
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="text-muted"><polyline points="6 9 12 15 18 9"/></svg>
          </div>
        </div>
      </aside>

      <!-- MOBILE OVERLAY BACKDROP -->
      <div class="mobile-overlay" v-if="isMobileOpen" @click="isMobileOpen = false"></div>

      <!-- MAIN PAGE FRAME -->
      <div class="main-content">
        <!-- TOP TOOLBAR NAVBAR -->
        <header class="navbar-wrapper">
          <div class="navbar-left">
            <button class="hamburger-btn" @click="toggleSidebar" aria-label="Open navigation menu">
              <IconHelper name="menu" size="20" />
            </button>
            <div class="logo-area header-logo">
              <div class="logo-box">
                <IconHelper name="shield" size="18" color="var(--primary-color)" />
              </div>
              <span class="logo-text header-logo-text">HR Admin</span>
            </div>
          </div>

          <!-- Search Bar in Center -->
          <div class="navbar-center">
            <div class="search-bar-container">
              <IconHelper name="search" size="14" class="search-icon" />
              <input type="text" v-model="searchQuery" class="search-input" placeholder="Search employees, leaves, expenses..." />
            </div>
          </div>

          <div class="navbar-right">
            <!-- MOCK SESSION TIMEOUT PROGRESS -->
            <div class="session-timer-pill" title="Time remaining until session expires. Toggles warning at 18 mins.">
              <IconHelper name="clock" size="14" color="#94a3b8" />
              <span>{{ Math.max(0, Math.floor((authStore.timeLimitSeconds - authStore.idleTimeSeconds) / 60)) }}m left</span>
            </div>

            <!-- SETTINGS ICON BUTTON -->
            <router-link to="/admin/settings" class="navbar-icon-btn" title="System Settings" aria-label="System Settings">
              <IconHelper name="settings" size="18" />
            </router-link>

            <!-- NOTIFICATIONS POPUP BELL -->
            <div style="position: relative;">
              <button class="navbar-icon-btn" @click="isNotificationOpen = !isNotificationOpen" aria-label="Toggle notifications center" title="Notifications">
                <IconHelper name="bell" size="18" />
                <div class="unread-badge" v-if="unreadNotificationCount > 0">{{ unreadNotificationCount }}</div>
              </button>
                
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

            <!-- PROFILE PIC & DROPDOWN -->
            <div class="profile-dropdown-container" style="position: relative;">
              <button class="profile-pic-btn" @click="isProfileDropdownOpen = !isProfileDropdownOpen" aria-label="Open profile menu" title="Profile">
                <img :src="authStore.user.photo || 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150'" alt="Lisa Anderson" class="user-avatar-sm header-avatar" />
              </button>
              
              <div class="profile-dropdown dropdown-card" v-if="isProfileDropdownOpen" @click.stop>
                <div class="profile-dropdown-header-card">
                  <img :src="authStore.user.photo || 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150'" alt="Lisa Anderson" class="profile-dropdown-avatar" />
                  <div class="profile-dropdown-meta">
                    <h4 class="profile-dropdown-name">{{ authStore.user.fullName }}</h4>
                    <span class="profile-dropdown-role">{{ activeRoleName }}</span>
                  </div>
                </div>
                
                <div class="profile-dropdown-details">
                  <div class="details-item">
                    <span class="details-label">Employee ID:</span>
                    <span class="details-value font-mono">{{ authStore.user.employeeCode }}</span>
                  </div>
                  <div class="details-item">
                    <span class="details-label">Email:</span>
                    <span class="details-value">{{ authStore.user.email }}</span>
                  </div>
                  <div class="details-item">
                    <span class="details-label">Department:</span>
                    <span class="details-value">{{ authStore.user.department }}</span>
                  </div>
                  <div class="details-item">
                    <span class="details-label">Phone:</span>
                    <span class="details-value">{{ authStore.user.phone }}</span>
                  </div>
                  <div class="details-item">
                    <span class="details-label">Last Login:</span>
                    <span class="details-value">{{ authStore.user.lastLogin }}</span>
                  </div>
                </div>
                
                <button class="btn btn-danger btn-sm btn-full mt-4" @click="handleLogOut(); isProfileDropdownOpen = false;">
                  <IconHelper name="log-out" size="14" />
                  Logout
                </button>
              </div>
            </div>
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
  border-radius: 50% !important;
  background-color: var(--primary-color) !important;
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo-text {
  font-weight: 700;
  font-size: 16px;
  color: var(--text-primary) !important;
  letter-spacing: -0.02em;
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
  margin-bottom: 8px;
}
.nav-group-header {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  padding: 10px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  margin-bottom: 4px;
}
.nav-group-header:hover {
  background-color: rgba(0, 0, 0, 0.02);
  color: var(--text-primary);
}
.nav-group-header.active {
  color: var(--primary-color);
  font-weight: 600;
}
.nav-group-header-collapsed {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 0;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  margin-bottom: 4px;
  width: 100%;
}
.nav-group-header-collapsed:hover {
  background-color: rgba(0, 0, 0, 0.02);
  color: var(--text-primary);
}
.nav-group-header-collapsed.active {
  color: var(--primary-color);
}
.nav-items {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding-left: 12px;
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

/* Redesigned Sidebar & Accordion styles */
.group-arrow {
  transition: transform var(--transition-normal);
  color: var(--text-muted);
}
.group-arrow.rotated {
  transform: rotate(180deg);
  color: var(--primary-color);
}
.nav-group-header {
  font-size: 13px;
  padding: 8px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: background-color var(--transition-fast), color var(--transition-fast);
  margin-bottom: 4px;
}
.nav-group-header:hover {
  background-color: var(--secondary-light);
  color: var(--text-primary);
}

/* Redesigned Header search input and logo */
.header-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 12px;
}
.header-logo-text {
  font-weight: 700;
  font-size: 15px;
  color: var(--text-primary);
}
.navbar-center {
  flex: 1;
  max-width: 400px;
  margin: 0 24px;
}
.search-bar-container {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}
.search-icon {
  position: absolute;
  left: 12px;
  color: var(--text-muted);
  pointer-events: none;
}
.search-input {
  width: 100%;
  padding: 8px 12px 8px 36px;
  font-size: 13px;
  font-family: var(--font-family);
  background-color: var(--secondary-light);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  transition: all var(--transition-fast);
}
.search-input:focus {
  outline: none;
  background-color: #ffffff;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

/* Detailed Profile Dropdown styling */
.profile-pic-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px;
  border-radius: 50%;
  border: 2px solid transparent;
  transition: border-color var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
}
.profile-pic-btn:hover, .profile-pic-btn:focus-visible {
  border-color: var(--primary-color);
  outline: none;
}
.header-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}
.profile-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 320px;
  padding: 16px;
  z-index: 160;
  cursor: default;
  box-shadow: var(--shadow-lg);
  background: #ffffff;
  border: 1px solid var(--border-color);
}
.profile-dropdown-header-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 12px;
}
.profile-dropdown-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}
.profile-dropdown-meta {
  display: flex;
  flex-direction: column;
}
.profile-dropdown-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}
.profile-dropdown-role {
  font-size: 11px;
  color: var(--text-muted);
}
.profile-dropdown-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}
.details-item {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  line-height: 1.4;
}
.details-label {
  color: var(--text-secondary);
  font-weight: 500;
}
.details-value {
  color: var(--text-primary);
  font-weight: 600;
  text-align: right;
}
.profile-dropdown-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.role-switcher-section {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}
.role-switcher-title {
  display: block;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--text-muted);
  letter-spacing: 0.05em;
  margin-bottom: 8px;
}
.role-switcher-buttons {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
}
.role-switch-btn {
  background-color: var(--secondary-light);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  font-size: 10px;
  font-weight: 600;
  padding: 6px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
  text-align: center;
  white-space: nowrap;
}
.role-switch-btn:hover {
  background-color: var(--border-color);
  color: var(--text-primary);
}
.role-switch-btn.active {
  background-color: var(--primary-light);
  border-color: var(--primary-color);
  color: var(--primary-color);
  font-weight: 700;
}

.hamburger-btn {
  display: flex !important;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-secondary);
  padding: 8px;
  margin-right: 12px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}
.hamburger-btn:hover {
  background-color: var(--secondary-light);
  color: var(--text-primary);
}

.dropdown-card {
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
}
</style>
