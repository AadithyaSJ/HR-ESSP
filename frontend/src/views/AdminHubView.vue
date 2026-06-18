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

const activeTab = ref(route.meta.tab || 'settings');

// System Settings local copy
const timeoutInput = ref(20);
const samlUrlInput = ref('');
const samlCertInput = ref('');
const moduleExpense = ref(true);
const moduleLeave = ref(true);
const modulePayroll = ref(true);

const emailJsServiceId = ref('');
const emailJsTemplateId = ref('');
const emailJsPublicKey = ref('');
const emailJsPrivateKey = ref('');

// Email Template State
const activeTemplate = ref(null);
const templateSubject = ref('');
const templateBody = ref('');

// Audit filter state
const auditUserQuery = ref('');
const auditModuleFilter = ref('All');

// User manager state
const userSearchQuery = ref('');

onMounted(() => {
  activeTab.value = route.meta.tab || 'settings';
  // Seed local settings state
  timeoutInput.value = hrStore.systemSettings.sessionTimeoutMin;
  samlUrlInput.value = hrStore.systemSettings.samlProviderUrl;
  samlCertInput.value = hrStore.systemSettings.samlCertificate;
  moduleExpense.value = hrStore.systemSettings.expenseModuleEnabled;
  moduleLeave.value = hrStore.systemSettings.leaveModuleEnabled;
  modulePayroll.value = hrStore.systemSettings.payrollModuleEnabled;
  
  emailJsServiceId.value = hrStore.systemSettings.emailJsServiceId || 'service_z913fuu';
  emailJsTemplateId.value = hrStore.systemSettings.emailJsTemplateId || 'template_2mgjka9';
  emailJsPublicKey.value = hrStore.systemSettings.emailJsPublicKey || 'rA7PvZze2DIvgX8WR';
  emailJsPrivateKey.value = hrStore.systemSettings.emailJsPrivateKey || 'uRt0bEDyyVvuKCVnzAjET';
  
  if (hrStore.emailTemplates.length > 0) {
    selectTemplate(hrStore.emailTemplates[0]);
  }
});

function changeTab(tab) {
  activeTab.value = tab;
  router.push(tab === 'settings' ? '/admin/settings' : tab === 'users' ? '/admin/users' : tab === 'emails' ? '/admin/email-templates' : tab === 'audit' ? '/admin/audit' : '/admin/scheduler');
}

// Save Global Settings
function handleSaveSettings() {
  hrStore.systemSettings.sessionTimeoutMin = timeoutInput.value;
  hrStore.systemSettings.samlProviderUrl = samlUrlInput.value;
  hrStore.systemSettings.samlCertificate = samlCertInput.value;
  hrStore.systemSettings.expenseModuleEnabled = moduleExpense.value;
  hrStore.systemSettings.leaveModuleEnabled = moduleLeave.value;
  hrStore.systemSettings.payrollModuleEnabled = modulePayroll.value;
  
  hrStore.systemSettings.emailJsServiceId = emailJsServiceId.value;
  hrStore.systemSettings.emailJsTemplateId = emailJsTemplateId.value;
  hrStore.systemSettings.emailJsPublicKey = emailJsPublicKey.value;
  hrStore.systemSettings.emailJsPrivateKey = emailJsPrivateKey.value;
  
  // Update actual timeout limits
  authStore.timeLimitSeconds = timeoutInput.value * 60;
  authStore.warningLimitSeconds = (timeoutInput.value - 2) * 60;

  hrStore.addLog(authStore.user.email, 'Admin', 'SAVE_SETTINGS', 'Updated global system settings and module toggles');
  window.showPortalToast('Global system configurations saved', 'success');
}

// Email template select
function selectTemplate(temp) {
  activeTemplate.value = temp;
  templateSubject.value = temp.subject;
  templateBody.value = temp.body;
}

function handleSaveTemplate() {
  if (activeTemplate.value) {
    activeTemplate.value.subject = templateSubject.value;
    activeTemplate.value.body = templateBody.value;
    
    hrStore.addLog(authStore.user.email, 'Admin', 'SAVE_EMAIL_TEMPLATE', `Saved email template: ${activeTemplate.value.name}`);
    window.showPortalToast('Email notification template updated', 'success');
  }
}

// Render dynamic preview
const renderedTemplateBody = computed(() => {
  if (!templateBody.value) return '';
  return templateBody.value
    .replace(/\{\{employee_name\}\}/g, 'Jane Doe')
    .replace(/\{\{leave_type\}\}/g, 'Annual')
    .replace(/\{\{from_date\}\}/g, '2026-06-15')
    .replace(/\{\{to_date\}\}/g, '2026-06-18')
    .replace(/\{\{manager_name\}\}/g, 'Sarah Jenkins')
    .replace(/\{\{comments\}\}/g, 'Enjoy your trip!')
    .replace(/\{\{reason\}\}/g, 'Personal emergencies');
});

// Audit Trail filter
const filteredAuditLogs = computed(() => {
  return hrStore.auditLogs.filter(l => {
    const matchUser = !auditUserQuery.value || l.user.toLowerCase().includes(auditUserQuery.value.toLowerCase());
    const matchModule = auditModuleFilter.value === 'All' || l.module === auditModuleFilter.value;
    return matchUser && matchModule;
  });
});

function exportAuditLogs() {
  let csv = 'data:text/csv;charset=utf-8,';
  csv += 'Timestamp,User Email,Module,Action,Details\r\n';
  filteredAuditLogs.value.forEach(l => {
    csv += `"${l.timestamp}","${l.user}","${l.module}","${l.action}","${l.details}"\r\n`;
  });
  
  const uri = encodeURI(csv);
  const link = document.createElement('a');
  link.setAttribute('href', uri);
  link.setAttribute('download', `security_audit_${new Date().toISOString().split('T')[0]}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  window.showPortalToast('Audit trail exported successfully', 'success');
}

// User role management list
const filteredUsers = computed(() => {
  return hrStore.employees.filter(e => {
    return !userSearchQuery.value || 
           e.fullName.toLowerCase().includes(userSearchQuery.value.toLowerCase()) ||
           e.email.toLowerCase().includes(userSearchQuery.value.toLowerCase());
  });
});

function handlePromoteUser(emp, newRole) {
  emp.role = newRole;
  hrStore.addLog(authStore.user.email, 'UserAdmin', 'PROMOTE_ROLE', `Promoted ${emp.fullName} role to ${newRole}`);
  window.showPortalToast(`User role updated for ${emp.fullName}`, 'success');
}

function handleResetPassword(email) {
  hrStore.addNotification('Reset Password Triggered', `Admin triggered password reset email for ${email}.`, 'System');
  window.showPortalToast(`Reset notification dispatched to ${email}`, 'info');
}

// Scheduler Jobs Actions
function triggerJob(jobId) {
  hrStore.triggerSchedulerJob(jobId, authStore.user.email);
  window.showPortalToast('Spring Batch job thread initiated', 'info');
}

function toggleJob(jobId) {
  hrStore.toggleSchedulerJob(jobId, authStore.user.email);
}
</script>

<template>
  <div class="admin-hub">
    <!-- SUB-TABS -->
    <div class="tabs-container">
      <div class="tabs">
        <button
          v-if="authStore.activeRole === 'SYSTEM_ADMIN'"
          class="tab-btn"
          :class="{ active: activeTab === 'settings' }"
          @click="changeTab('settings')"
        >
          System Settings
        </button>
        <button
          v-if="authStore.activeRole === 'SYSTEM_ADMIN'"
          class="tab-btn"
          :class="{ active: activeTab === 'users' }"
          @click="changeTab('users')"
        >
          User & Role Mgmt
        </button>
        <button
          v-if="['SYSTEM_ADMIN', 'HR_ADMIN'].includes(authStore.activeRole)"
          class="tab-btn"
          :class="{ active: activeTab === 'emails' }"
          @click="changeTab('emails')"
        >
          SES Email Templates
        </button>
        <button
          v-if="authStore.activeRole === 'SYSTEM_ADMIN'"
          class="tab-btn"
          :class="{ active: activeTab === 'audit' }"
          @click="changeTab('audit')"
        >
          Security Audit Logs
        </button>
        <button
          v-if="authStore.activeRole === 'SYSTEM_ADMIN'"
          class="tab-btn"
          :class="{ active: activeTab === 'scheduler' }"
          @click="changeTab('scheduler')"
        >
          Batch Scheduler
        </button>
      </div>
    </div>

    <!-- TAB 1: SYSTEM SETTINGS -->
    <template v-if="activeTab === 'settings' && authStore.activeRole === 'SYSTEM_ADMIN'">
      <div class="grid-12">
        <!-- Toggles & configs -->
        <div class="col-8">
          <div class="glass-card">
            <h3 class="mb-6">Global Application Configuration</h3>
            <form @submit.prevent="handleSaveSettings">
              <div class="grid-12">
                <div class="col-6 form-group">
                  <label class="form-label">Default Session Idle Timeout (Minutes)</label>
                  <input type="number" v-model="timeoutInput" class="form-control" min="5" max="60" required />
                </div>
                <div class="col-6 form-group">
                  <label class="form-label">SAML SSO Identity Provider (Okta / AD)</label>
                  <input type="text" v-model="samlUrlInput" class="form-control" />
                </div>
                <div class="col-12 form-group">
                  <label class="form-label">SAML Provider Public X.509 Certificate</label>
                  <textarea v-model="samlCertInput" class="form-control font-mono text-xs" rows="4"></textarea>
                </div>

                <h4 class="col-12 section-header">EmailJS Integration (Onboarding Invitations)</h4>
                <div class="col-6 form-group">
                  <label class="form-label">EmailJS Service ID</label>
                  <input type="text" v-model="emailJsServiceId" class="form-control" />
                </div>
                <div class="col-6 form-group">
                  <label class="form-label">EmailJS Template ID</label>
                  <input type="text" v-model="emailJsTemplateId" class="form-control" placeholder="e.g. template_2mgjka9" />
                </div>
                <div class="col-6 form-group">
                  <label class="form-label">EmailJS Public Key (API Key)</label>
                  <input type="text" v-model="emailJsPublicKey" class="form-control" />
                </div>
                <div class="col-6 form-group">
                  <label class="form-label">EmailJS Private Key (Access Token)</label>
                  <input type="password" v-model="emailJsPrivateKey" class="form-control" />
                </div>

                <h4 class="col-12 section-header">Active Modules & Feature Flags</h4>
                <div class="col-4 flex items-center gap-2">
                  <input type="checkbox" v-model="moduleExpense" id="chk-exp" />
                  <label for="chk-exp" class="text-sm">Expense Claims Hub</label>
                </div>
                <div class="col-4 flex items-center gap-2">
                  <input type="checkbox" v-model="moduleLeave" id="chk-lv" />
                  <label for="chk-lv" class="text-sm">Leave Management Hub</label>
                </div>
                <div class="col-4 flex items-center gap-2">
                  <input type="checkbox" v-model="modulePayroll" id="chk-pr" />
                  <label for="chk-pr" class="text-sm">Payroll Processing Hub</label>
                </div>
              </div>

              <div class="flex justify-end gap-3 mt-6">
                <button type="submit" class="btn btn-primary">Save Settings</button>
              </div>
            </form>
          </div>
        </div>

        <!-- System env details -->
        <div class="col-4">
          <div class="glass-card">
            <h3 class="mb-4">Version & Environments</h3>
            <div class="font-mono text-xs">
              <div class="flex justify-between pb-2 mb-2 border-b">
                <span class="text-secondary">Release Version:</span>
                <span class="font-bold">{{ hrStore.systemSettings.version }}</span>
              </div>
              <div class="flex justify-between pb-2 mb-2 border-b">
                <span class="text-secondary">Active Environment:</span>
                <span class="font-bold text-success">{{ hrStore.systemSettings.environment }}</span>
              </div>
              <div class="flex justify-between pb-2 mb-2 border-b">
                <span class="text-secondary">Build Engine:</span>
                <span>Spring Boot 3.3.4, Java 22</span>
              </div>
              <div class="flex justify-between">
                <span class="text-secondary">CORS Domain:</span>
                <span>*.company.com</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- TAB 2: USER & ROLES -->
    <template v-if="activeTab === 'users' && authStore.activeRole === 'SYSTEM_ADMIN'">
      <div class="glass-card">
        <div class="flex justify-between items-center mb-6">
          <h3>Registered System Credentials</h3>
          <div class="form-group m-0 w-260">
            <input type="text" v-model="userSearchQuery" class="form-control" placeholder="Search by name or email..." />
          </div>
        </div>

        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>Profile Name</th>
                <th>Email Address</th>
                <th>Designation</th>
                <th>Default Role Authorization</th>
                <th class="text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in filteredUsers" :key="u.id">
                <td class="font-bold">{{ u.fullName }}</td>
                <td class="font-mono text-xs">{{ u.email }}</td>
                <td>{{ u.designation }}</td>
                <td>
                  <select :value="u.role" @change="handlePromoteUser(u, $event.target.value)" class="form-control text-xs py-1 w-160">
                    <option value="EMPLOYEE">Employee</option>
                    <option value="MANAGER">Manager / Team Lead</option>
                    <option value="HR_ADMIN">HR Admin</option>
                    <option value="FINANCE_ADMIN">Finance Admin</option>
                    <option value="SYSTEM_ADMIN">System Admin</option>
                  </select>
                </td>
                <td class="text-right">
                  <button class="btn btn-secondary btn-sm" @click="handleResetPassword(u.email)">
                    Reset Password
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- TAB 3: EMAIL TEMPLATES -->
    <template v-if="activeTab === 'emails' && ['SYSTEM_ADMIN', 'HR_ADMIN'].includes(authStore.activeRole)">
      <div class="grid-12">
        <!-- template picker / editor -->
        <div class="col-7">
          <div class="glass-card">
            <h3 class="mb-4">AWS SES Template Editor</h3>
            <div class="flex gap-2 overflow-x-auto mb-6 pb-2 border-b">
              <button
                v-for="t in hrStore.emailTemplates"
                :key="t.id"
                class="btn btn-ghost btn-sm"
                :class="{ 'btn-secondary border': activeTemplate?.id === t.id }"
                @click="selectTemplate(t)"
              >
                {{ t.name }}
              </button>
            </div>

            <div v-if="activeTemplate">
              <div class="form-group">
                <label class="form-label">Email Subject Line</label>
                <input type="text" v-model="templateSubject" class="form-control" />
              </div>
              <div class="form-group">
                <label class="form-label">Email Body (HTML Supported)</label>
                <textarea v-model="templateBody" class="form-control font-mono text-xs" rows="8"></textarea>
              </div>
              <p class="text-xs text-muted mb-4">
                Supported dynamic placeholders: <code>\{{employee_name}}</code>, <code>\{{leave_type}}</code>, <code>\{{from_date}}</code>, <code>\{{to_date}}</code>, <code>\{{manager_name}}</code>.
              </p>
              <div class="flex justify-end">
                <button type="button" class="btn btn-primary btn-sm" @click="handleSaveTemplate">Save Template</button>
              </div>
            </div>
          </div>
        </div>

        <!-- template preview -->
        <div class="col-5">
          <div class="glass-card h-full">
            <h3>Live Rendered Template Preview</h3>
            <div class="email-preview-frame mt-6 p-4 rounded bg-surface">
              <div class="preview-subject font-bold border-b pb-2 mb-4 text-sm">
                Subject: {{ templateSubject }}
              </div>
              <div class="preview-body text-xs" v-html="renderedTemplateBody"></div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- TAB 4: AUDIT LOGS -->
    <template v-if="activeTab === 'audit' && authStore.activeRole === 'SYSTEM_ADMIN'">
      <div class="glass-card">
        <div class="flex justify-between items-center mb-6">
          <h3>Security Audit Logs</h3>
          
          <div class="flex gap-3">
            <div class="form-group m-0 w-160">
              <input type="text" v-model="auditUserQuery" class="form-control" placeholder="Filter by user email..." />
            </div>

            <div class="form-group m-0 w-160">
              <select v-model="auditModuleFilter" class="form-control">
                <option value="All">All Modules</option>
                <option value="Auth">Auth Module</option>
                <option value="Leave">Leave Module</option>
                <option value="Expense">Expense Module</option>
                <option value="Payroll">Payroll Module</option>
                <option value="Employee">Employee Module</option>
                <option value="Scheduler">Scheduler Module</option>
                <option value="Admin">Admin Module</option>
              </select>
            </div>

            <button class="btn btn-secondary btn-sm" @click="exportAuditLogs">
              <IconHelper name="download" size="14" />
              Export CSV
            </button>
          </div>
        </div>

        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>Timestamp</th>
                <th>Operator User</th>
                <th>Component Module</th>
                <th>Action Code</th>
                <th>Modification Details</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="l in filteredAuditLogs" :key="l.id">
                <td class="font-mono text-xs text-secondary">{{ new Date(l.timestamp).toLocaleString() }}</td>
                <td class="font-bold text-xs">{{ l.user }}</td>
                <td><span class="badge badge-muted">{{ l.module }}</span></td>
                <td><span class="badge badge-info">{{ l.action }}</span></td>
                <td class="text-xs text-secondary">{{ l.details }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- TAB 5: BATCH SCHEDULER -->
    <template v-if="activeTab === 'scheduler' && authStore.activeRole === 'SYSTEM_ADMIN'">
      <div class="grid-12">
        <!-- Spring jobs list -->
        <div class="col-8">
          <div class="glass-card">
            <h3>Registered Spring Batch Cron Jobs</h3>
            <div class="table-container mt-4">
              <table class="table">
                <thead>
                  <tr>
                    <th>Job Name</th>
                    <th>Cron Expression</th>
                    <th>Last Execution</th>
                    <th>Duration</th>
                    <th>Status</th>
                    <th class="text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="job in hrStore.schedulerJobs" :key="job.id">
                    <td class="font-bold text-sm">{{ job.name }}</td>
                    <td class="font-mono text-xs text-secondary">{{ job.cron }}</td>
                    <td class="font-mono text-xs">{{ job.lastRun }}</td>
                    <td class="text-xs">{{ job.duration }}</td>
                    <td>
                      <span class="badge" :class="job.status === 'SUCCESS' ? 'badge-success' : job.status === 'RUNNING' ? 'badge-warning' : 'badge-danger'">
                        {{ job.status }}
                      </span>
                    </td>
                    <td class="text-right">
                      <div class="flex gap-2 justify-end">
                        <button class="btn btn-secondary btn-sm px-2 py-4" @click="toggleJob(job.id)" :title="job.active ? 'Disable job' : 'Enable job'">
                          {{ job.active ? 'Disable' : 'Enable' }}
                        </button>
                        <button class="btn btn-primary btn-sm px-2 py-4" @click="triggerJob(job.id)" :disabled="job.status === 'RUNNING'">
                          Run Now
                        </button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Logs console console -->
        <div class="col-4">
          <div class="glass-card h-full">
            <h3>Scheduler Daemon Logs</h3>
            <div class="console-box mt-4 p-3 rounded font-mono text-xs">
              <div class="console-line text-muted">[2026-06-09 00:00:00] INFO SchedulerDaemon started.</div>
              <div class="console-line text-muted">[2026-06-09 16:00:01] INFO Job [SessionCleanupJob] completed in 0.8s.</div>
              <div class="console-line text-info" v-if="hrStore.schedulerJobs.some(j => j.status === 'RUNNING')">
                [{{ new Date().toLocaleTimeString() }}] INFO Initiated thread for Job...
              </div>
              <div class="console-line text-success" v-else>
                [{{ new Date().toLocaleTimeString() }}] INFO Scheduler listener IDLE. Awaiting crontab matches...
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.admin-hub {
  animation: fadeIn 0.4s ease;
}

.font-normal { font-weight: 400; }
.font-semibold { font-weight: 600; }
.font-bold { font-weight: 700; }
.font-mono { font-family: monospace; }
.text-xs { font-size: 11px; }
.text-sm { font-size: 13px; }
.text-lg { font-size: 18px; }

.flex { display: flex; }
.flex-col { display: flex; flex-direction: column; }
.items-center { align-items: center; }
.justify-between { justify-content: space-between; }
.justify-end { justify-content: flex-end; }
.gap-2 { gap: 8px; }
.gap-3 { gap: 12px; }
.text-right { text-align: right; }
.btn-full { width: 100%; }
.w-120 { width: 120px; }
.w-160 { width: 160px; }
.w-260 { width: 260px; }
.p-3 { padding: 12px; }
.p-4 { padding: 16px; }

.mb-2 { margin-bottom: 8px; }
.mb-4 { margin-bottom: 16px; }
.mb-6 { margin-bottom: 24px; }
.mt-4 { margin-top: 16px; }
.mt-6 { margin-top: 24px; }
.pb-2 { padding-bottom: 8px; }
.py-1 { padding-top: 4px; padding-bottom: 4px; }
.py-4 { padding-top: 4px; padding-bottom: 4px; }
.bg-surface { background-color: rgba(15, 23, 42, 0.3); }
.rounded { border-radius: var(--radius-sm); }
.border-b { border-bottom: 1px solid var(--border-color); }
.list-disc { list-style-type: disc; }

.section-header {
  font-size: 13px;
  font-weight: 700;
  color: var(--primary-color);
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 6px;
  margin-top: 16px;
  margin-bottom: 12px;
}

/* Console logs styling */
.console-box {
  background-color: #020617;
  border: 1px solid var(--border-color);
  height: 220px;
  overflow-y: auto;
  line-height: 1.5;
}
.console-line {
  margin-bottom: 4px;
}
.text-info { color: var(--info); }
.text-success { color: var(--success); }
.text-muted { color: var(--text-muted); }
</style>
