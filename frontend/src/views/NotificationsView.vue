<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/authStore.js';
import { useHrStore } from '../stores/hrStore.js';
import IconHelper from '../components/IconHelper.vue';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const hrStore = useHrStore();

const activeTab = ref(route.meta.tab || 'inbox');

// Inbox filters
const filterType = ref('All');

// Preferences state local copies
const emailLeave = ref(true);
const emailExpense = ref(true);
const emailPayslip = ref(true);
const emailSystem = ref(true);

const inAppLeave = ref(true);
const inAppExpense = ref(true);
const inAppPayslip = ref(true);
const inAppSystem = ref(true);

const digestFreq = ref('immediate');

onMounted(() => {
  activeTab.value = route.meta.tab || 'inbox';
  
  // Seed preferences from store
  emailLeave.value = hrStore.notificationPreferences.emailEnabled.Leave;
  emailExpense.value = hrStore.notificationPreferences.emailEnabled.Expense;
  emailPayslip.value = hrStore.notificationPreferences.emailEnabled.Payslip;
  emailSystem.value = hrStore.notificationPreferences.emailEnabled.System;
  
  inAppLeave.value = hrStore.notificationPreferences.inAppEnabled.Leave;
  inAppExpense.value = hrStore.notificationPreferences.inAppEnabled.Expense;
  inAppPayslip.value = hrStore.notificationPreferences.inAppEnabled.Payslip;
  inAppSystem.value = hrStore.notificationPreferences.inAppEnabled.System;
  
  digestFreq.value = hrStore.notificationPreferences.digestFrequency;
});

function changeTab(tab) {
  activeTab.value = tab;
  router.push(tab === 'inbox' ? '/notifications' : '/notifications/preferences');
}

// Filtered notifications list
const filteredNotifications = computed(() => {
  if (filterType.value === 'All') return hrStore.notifications;
  return hrStore.notifications.filter(n => n.type === filterType.value);
});

function markRead(notif) {
  notif.isRead = true;
  hrStore.addLog(authStore.user.email, 'Notifications', 'MARK_READ', `Marked notification ID ${notif.id} as read`);
  if (notif.url) {
    router.push(notif.url);
  }
}

function markAllRead() {
  hrStore.notifications.forEach(n => n.isRead = true);
  hrStore.addLog(authStore.user.email, 'Notifications', 'MARK_ALL_READ', 'Marked all notifications as read');
  window.showPortalToast('All notifications marked as read', 'success');
}

// Save preferences
function handleSavePreferences() {
  hrStore.notificationPreferences.emailEnabled.Leave = emailLeave.value;
  hrStore.notificationPreferences.emailEnabled.Expense = emailExpense.value;
  hrStore.notificationPreferences.emailEnabled.Payslip = emailPayslip.value;
  hrStore.notificationPreferences.emailEnabled.System = emailSystem.value;

  hrStore.notificationPreferences.inAppEnabled.Leave = inAppLeave.value;
  hrStore.notificationPreferences.inAppEnabled.Expense = inAppExpense.value;
  hrStore.notificationPreferences.inAppEnabled.Payslip = inAppPayslip.value;
  hrStore.notificationPreferences.inAppEnabled.System = inAppSystem.value;
  
  hrStore.notificationPreferences.digestFrequency = digestFreq.value;

  hrStore.addLog(authStore.user.email, 'Notifications', 'UPDATE_PREFERENCES', 'Updated notification delivery settings');
  window.showPortalToast('Notification delivery preferences updated', 'success');
}

// SSE Real-time Simulation logs
const sseSimEvents = ref([
  { time: '17:40:12', text: 'SSE connection established securely' },
  { time: '17:41:05', text: 'Ping heartbeat received from Keycloak session' }
]);

function triggerMockSSE(type) {
  const timestamp = new Date().toLocaleTimeString();
  let text;
  
  if (type === 'leave') {
    text = 'Real-time Event: Leave submitted by employee EMP2026101';
    hrStore.addNotification('Leave Submitted', 'Jane Doe applied for CASUAL leave from 2026-06-25.', 'Leave', '/leave');
  } else if (type === 'expense') {
    text = 'Real-time Event: Expense claim status updated to APPROVED';
    hrStore.addNotification('Expense Manager Approved', 'Your Meals claim of INR 1500 was approved by Sarah Jenkins.', 'Expense', '/expense');
  } else {
    text = 'Real-time Event: Scheduler triggered job complete';
    hrStore.addNotification('System Alert', 'Scheduler SessionCleanupJob completed successfully.', 'System', '/admin/scheduler');
  }
  
  sseSimEvents.value.unshift({ time: timestamp, text });
  window.showPortalToast('Simulated real-time Server-Sent Event (SSE) received!', 'info');
}
</script>

<template>
  <div class="notifications-view">
    <!-- SUB-TABS -->
    <div class="tabs-container">
      <div class="tabs">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'inbox' }"
          @click="changeTab('inbox')"
        >
          Notifications Centre
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'preferences' }"
          @click="changeTab('preferences')"
        >
          Delivery Preferences
        </button>
      </div>
    </div>

    <!-- TAB 1: NOTIFICATION INBOX -->
    <template v-if="activeTab === 'inbox'">
      <div class="grid-12">
        <!-- left: list logs -->
        <div class="col-8">
          <div class="glass-card">
            <div class="flex justify-between items-center mb-6">
              <div class="flex gap-2 items-center">
                <h3>Notifications Inbox</h3>
                <span class="badge badge-danger text-xs font-mono" v-if="hrStore.notifications.some(n => !n.isRead)">
                  {{ hrStore.notifications.filter(n => !n.isRead).length }} New
                </span>
              </div>
              
              <div class="flex gap-2">
                <button class="btn btn-ghost btn-xs px-2" @click="markAllRead">
                  Mark all read
                </button>
                <select v-model="filterType" class="form-control text-xs py-1 w-120">
                  <option value="All">All Alerts</option>
                  <option value="Leave">Leaves</option>
                  <option value="Expense">Expenses</option>
                  <option value="Payslip">Payslips</option>
                  <option value="System">System</option>
                </select>
              </div>
            </div>

            <!-- List entries -->
            <div class="notif-list-wrapper">
              <div
                v-for="notif in filteredNotifications"
                :key="notif.id"
                class="notif-row clickable"
                :class="{ unread: !notif.isRead }"
                @click="markRead(notif)"
              >
                <div class="notif-icon-col" :class="notif.type.toLowerCase()">
                  <IconHelper :name="notif.type === 'Leave' ? 'calendar' : notif.type === 'Expense' ? 'credit-card' : 'bell'" size="16" />
                </div>
                <div class="notif-body-col">
                  <div class="notif-row-title">
                    {{ notif.title }}
                    <span class="badge badge-muted text-xs font-mono py-0 ml-2">{{ notif.type }}</span>
                  </div>
                  <div class="notif-row-msg text-secondary text-xs mt-1">{{ notif.message }}</div>
                  <div class="notif-row-time text-muted text-xs mt-2">{{ new Date(notif.createdAt).toLocaleString() }}</div>
                </div>
                <div class="notif-action-col text-muted">
                  <IconHelper name="arrow-left" size="14" style="transform: rotate(180deg);" />
                </div>
              </div>
              <div v-if="filteredNotifications.length === 0" class="text-center p-8 text-secondary">
                Inbox clear. No notifications.
              </div>
            </div>
          </div>
        </div>

        <!-- right: sse real time testing simulator -->
        <div class="col-4">
          <div class="glass-card">
            <h3>SSE Stream Test Console</h3>
            <p class="text-xs text-secondary mb-4 mt-1">
              Simulate server-triggered Server-Sent Events pushes. Pushes appear immediately in alerts.
            </p>
            
            <div class="flex flex-col gap-2 mb-6">
              <button class="btn btn-secondary btn-sm" @click="triggerMockSSE('leave')">
                Push Leave Submitted SSE
              </button>
              <button class="btn btn-secondary btn-sm" @click="triggerMockSSE('expense')">
                Push Expense Approved SSE
              </button>
              <button class="btn btn-secondary btn-sm" @click="triggerMockSSE('system')">
                Push System Batch Job SSE
              </button>
            </div>

            <div class="console-box p-3 rounded font-mono text-xs" style="height: 180px;">
              <div v-for="(ev, idx) in sseSimEvents" :key="idx" class="console-line">
                <span class="text-muted">[{{ ev.time }}]</span> {{ ev.text }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- TAB 2: PREFERENCES -->
    <template v-if="activeTab === 'preferences'">
      <div class="glass-card max-width-600">
        <h3 class="mb-6">Alert Delivery Configuration</h3>
        <form @submit.prevent="handleSavePreferences">
          
          <!-- Email toggles -->
          <h4 class="section-header">Email Alerts Setup (via AWS SES)</h4>
          <div class="pref-row mb-3">
            <span class="text-sm">Dispatches for Leave updates (Submissions, approvals)</span>
            <input type="checkbox" v-model="emailLeave" />
          </div>
          <div class="pref-row mb-3">
            <span class="text-sm">Dispatches for Expense claims billing releases</span>
            <input type="checkbox" v-model="emailExpense" />
          </div>
          <div class="pref-row mb-3">
            <span class="text-sm">Dispatches when Payslip PDFs are published</span>
            <input type="checkbox" v-model="emailPayslip" />
          </div>
          <div class="pref-row mb-6">
            <span class="text-sm">Global system changes and password resets</span>
            <input type="checkbox" v-model="emailSystem" />
          </div>

          <!-- In App toggles -->
          <h4 class="section-header mt-6">In-App Banner Notifications</h4>
          <div class="pref-row mb-3">
            <span class="text-sm">Banners for Leave approvals</span>
            <input type="checkbox" v-model="inAppLeave" />
          </div>
          <div class="pref-row mb-3">
            <span class="text-sm">Banners for Expense claims workflow tracking</span>
            <input type="checkbox" v-model="inAppExpense" />
          </div>
          <div class="pref-row mb-3">
            <span class="text-sm">Banners when new Payslips are published</span>
            <input type="checkbox" v-model="inAppPayslip" />
          </div>
          <div class="pref-row mb-6">
            <span class="text-sm">System reminders and onboarding checklists</span>
            <input type="checkbox" v-model="inAppSystem" />
          </div>

          <!-- Frequency -->
          <h4 class="section-header mt-6">Email Digest Frequency</h4>
          <div class="form-group mb-6">
            <select v-model="digestFreq" class="form-control">
              <option value="immediate">Real-time alerts (Immediate dispatch)</option>
              <option value="daily">Daily digest summary emails</option>
              <option value="weekly">Weekly digest summary emails</option>
            </select>
          </div>

          <div class="flex justify-end pt-4 border-t">
            <button type="submit" class="btn btn-primary">Save Preferences</button>
          </div>
        </form>
      </div>
    </template>
  </div>
</template>

<style scoped>
.notifications-view {
  animation: fadeIn 0.4s ease;
}

.max-width-600 {
  max-width: 600px;
  margin: 0 auto;
}

.clickable { cursor: pointer; }

/* Inbox rows list */
.notif-list-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.notif-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: var(--radius-sm);
  background-color: rgba(255, 255, 255, 0.02);
  border: 1px solid var(--border-color);
  transition: all var(--transition-fast);
}
.notif-row:hover {
  border-color: rgba(255, 255, 255, 0.15);
  background-color: rgba(255, 255, 255, 0.04);
}
.notif-row.unread {
  background-color: rgba(99, 102, 241, 0.03);
  border-left: 3px solid var(--primary-color);
}
.notif-icon-col {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.notif-icon-col.leave { background-color: var(--info-light); color: var(--info); }
.notif-icon-col.expense { background-color: var(--warning-light); color: var(--warning); }
.notif-icon-col.payslip { background-color: var(--success-light); color: var(--success); }
.notif-icon-col.system { background-color: var(--primary-light); color: var(--primary-color); }
.notif-body-col {
  flex: 1;
}
.notif-row-title {
  font-weight: 600;
  font-size: 14px;
}
.notif-action-col {
  opacity: 0.3;
}

.section-header {
  font-size: 13px;
  font-weight: 700;
  color: var(--primary-color);
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 6px;
  margin-top: 20px;
  margin-bottom: 16px;
}

.pref-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* Console logs */
.console-box {
  background-color: #020617;
  border: 1px solid var(--border-color);
  overflow-y: auto;
  line-height: 1.5;
}
.console-line {
  margin-bottom: 4px;
}
.text-info { color: var(--info); }
.text-success { color: var(--success); }
.text-muted { color: var(--text-muted); }

.w-120 { width: 120px; }
.font-mono { font-family: monospace; }
.text-xs { font-size: 12px; }
.text-sm { font-size: 13px; }

.flex { display: flex; }
.flex-col { display: flex; flex-direction: column; }
.items-center { align-items: center; }
.justify-between { justify-content: space-between; }
.gap-2 { gap: 8px; }

.mb-3 { margin-bottom: 12px; }
.mb-4 { margin-bottom: 16px; }
.mb-6 { margin-bottom: 24px; }
.mt-1 { margin-top: 4px; }
.mt-2 { margin-top: 8px; }
.mt-6 { margin-top: 24px; }
.ml-2 { margin-left: 8px; }
.pt-4 { padding-top: 16px; }
.py-0 { padding-top: 0; padding-bottom: 0; }
.py-1 { padding-top: 4px; padding-bottom: 4px; }
.p-3 { padding: 12px; }
.p-8 { padding: 32px; }
.border-t { border-top: 1px solid var(--border-color); }
.text-center { text-align: center; }
</style>
