<script setup>
import { ref, computed, onMounted } from 'vue';
import { useAuthStore } from '../stores/authStore.js';
import { useHrStore } from '../stores/hrStore.js';
import IconHelper from '../components/IconHelper.vue';

const authStore = useAuthStore();
const hrStore = useHrStore();

const activeTab = ref('my-overtime'); // 'my-overtime' or 'manager-approvals'
const isManagerOrAdmin = computed(() => ['MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN'].includes(authStore.activeRole));

// Overtime Form State
const otDate = ref('');
const otHours = ref(0);
const otPurpose = ref('');

const isLoading = ref(false);

onMounted(async () => {
  isLoading.value = true;
  try {
    await hrStore.fetchEmployees();
    if (authStore.user) {
      await hrStore.fetchOvertimeRecords(authStore.user.id);
    }
  } finally {
    isLoading.value = false;
  }
});

// Logs filters
const myLogs = computed(() => {
  if (!authStore.user) return [];
  return hrStore.overtimeRecords.filter(r => r.employeeId === authStore.user.id);
});

const pendingApprovals = computed(() => {
  // If active user is manager or admin, show pending approvals
  return hrStore.overtimeRecords.filter(r => r.status === 'PENDING');
});

// Total approved hours for the current month (for current user)
const monthlyApprovedHours = computed(() => {
  const currentMonth = new Date().getMonth(); // 0-11
  const currentYear = new Date().getFullYear();
  
  return myLogs.value
    .filter(r => {
      if (r.status !== 'APPROVED') return false;
      const d = new Date(r.date);
      return d.getMonth() === currentMonth && d.getFullYear() === currentYear;
    })
    .reduce((sum, r) => sum + r.hours, 0);
});

// Stats
const overtimeStats = computed(() => {
  const list = isManagerOrAdmin.value ? hrStore.overtimeRecords : myLogs.value;
  return {
    totalHours: list.filter(r => r.status === 'APPROVED').reduce((sum, r) => sum + r.hours, 0),
    pendingCount: list.filter(r => r.status === 'PENDING').length,
    approvedCount: list.filter(r => r.status === 'APPROVED').length,
    rejectedCount: list.filter(r => r.status === 'REJECTED').length
  };
});

function getEmployeeName(empId) {
  const emp = hrStore.employees.find(e => e.id === empId);
  return emp ? emp.fullName : 'Employee';
}

function getEmployeeCode(empId) {
  const emp = hrStore.employees.find(e => e.id === empId);
  return emp ? emp.employeeCode : '';
}

function getStatusBadgeClass(status) {
  const map = {
    PENDING: 'badge-warning',
    APPROVED: 'badge-success',
    REJECTED: 'badge-danger'
  };
  return map[status] || 'badge-primary';
}

async function handleLogOvertime() {
  if (!otDate.value || otHours.value <= 0 || !otPurpose.value.trim()) {
    window.showPortalToast('Please fill out all fields with valid values', 'error');
    return;
  }

  if (otHours.value > 12) {
    window.showPortalToast('Overtime limit per log is capped at 12 hours', 'error');
    return;
  }

  const payload = {
    employeeId: authStore.user.id,
    date: otDate.value,
    hours: parseFloat(otHours.value),
    purpose: otPurpose.value.trim(),
    status: 'PENDING'
  };

  try {
    isLoading.value = true;
    await hrStore.logOvertimeRecord(payload, authStore.user.email);
    window.showPortalToast('Overtime hours logged successfully for approval', 'success');
    
    // Reset form
    otDate.value = '';
    otHours.value = 0;
    otPurpose.value = '';
    
    // Refresh
    await hrStore.fetchOvertimeRecords(authStore.user.id);
  } catch (err) {
    window.showPortalToast(err.message || 'Failed to log overtime', 'error');
  } finally {
    isLoading.value = false;
  }
}

async function handleApprove(recordId) {
  try {
    isLoading.value = true;
    await hrStore.updateOvertimeRecordStatus(recordId, 'APPROVED', authStore.user.id, authStore.user.email);
    window.showPortalToast('Overtime record approved successfully', 'success');
    await hrStore.fetchOvertimeRecords(authStore.user.id);
  } catch (err) {
    window.showPortalToast(err.message || 'Failed to approve overtime', 'error');
  } finally {
    isLoading.value = false;
  }
}

async function handleReject(recordId) {
  try {
    isLoading.value = true;
    await hrStore.updateOvertimeRecordStatus(recordId, 'REJECTED', authStore.user.id, authStore.user.email);
    window.showPortalToast('Overtime record rejected', 'warning');
    await hrStore.fetchOvertimeRecords(authStore.user.id);
  } catch (err) {
    window.showPortalToast(err.message || 'Failed to reject overtime', 'error');
  } finally {
    isLoading.value = false;
  }
}
</script>

<template>
  <div class="overtime container-fluid py-4">
    <!-- Header -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h1 class="page-title">Overtime Hours Tracker</h1>
        <p class="text-secondary text-sm">Log extra working hours for 1.5x compensation pay rates on automated payslips.</p>
      </div>

      <!-- Navigation Tabs -->
      <div v-if="isManagerOrAdmin" class="flex gap-2 bg-sidebar rounded p-1 border">
        <button 
          class="btn btn-sm px-4 py-2" 
          :class="activeTab === 'my-overtime' ? 'btn-primary' : 'btn-ghost'"
          @click="activeTab = 'my-overtime'"
        >
          My Overtime Logs
        </button>
        <button 
          class="btn btn-sm px-4 py-2" 
          :class="activeTab === 'manager-approvals' ? 'btn-primary' : 'btn-ghost'"
          @click="activeTab = 'manager-approvals'"
        >
          Pending Verification
        </button>
      </div>
    </div>

    <!-- Quick Stats Grid -->
    <div class="grid-12 mb-6">
      <div class="col-3" v-if="activeTab === 'my-overtime'">
        <div class="glass-card flex items-center gap-4">
          <div class="badge badge-success p-3" style="border-radius: var(--radius-sm)">
            <IconHelper name="clock" size="24" />
          </div>
          <div>
            <div class="text-secondary text-xs font-semibold">Approved Hours (This Month)</div>
            <div class="text-lg font-bold text-success">{{ monthlyApprovedHours }} hrs</div>
          </div>
        </div>
      </div>
      <div class="col-3" v-else>
        <div class="glass-card flex items-center gap-4">
          <div class="badge badge-success p-3" style="border-radius: var(--radius-sm)">
            <IconHelper name="clock" size="24" />
          </div>
          <div>
            <div class="text-secondary text-xs font-semibold">Total Approved Hours</div>
            <div class="text-lg font-bold text-success">{{ overtimeStats.totalHours }} hrs</div>
          </div>
        </div>
      </div>

      <div class="col-3">
        <div class="glass-card flex items-center gap-4">
          <div class="badge badge-warning p-3" style="border-radius: var(--radius-sm)">
            <IconHelper name="clock" size="24" />
          </div>
          <div>
            <div class="text-secondary text-xs font-semibold">Awaiting Verification</div>
            <div class="text-lg font-bold text-warning">{{ overtimeStats.pendingCount }} logs</div>
          </div>
        </div>
      </div>
      <div class="col-3">
        <div class="glass-card flex items-center gap-4">
          <div class="badge badge-primary p-3" style="border-radius: var(--radius-sm)">
            <IconHelper name="check-circle" size="24" />
          </div>
          <div>
            <div class="text-secondary text-xs font-semibold">Total Approved Logs</div>
            <div class="text-lg font-bold">{{ overtimeStats.approvedCount }} logs</div>
          </div>
        </div>
      </div>
      <div class="col-3">
        <div class="glass-card flex items-center gap-4">
          <div class="badge badge-danger p-3" style="border-radius: var(--radius-sm)">
            <IconHelper name="trash" size="24" />
          </div>
          <div>
            <div class="text-secondary text-xs font-semibold">Total Rejected Logs</div>
            <div class="text-lg font-bold text-danger">{{ overtimeStats.rejectedCount }} logs</div>
          </div>
        </div>
      </div>
    </div>

    <!-- main content workspace -->
    <div class="grid-12">
      <!-- Logging Form (For standard users) -->
      <div class="col-4" v-if="activeTab === 'my-overtime'">
        <div class="glass-card">
          <h3 class="mb-4">Log Overtime Hours</h3>
          <form @submit.prevent="handleLogOvertime">
            <div class="form-group mb-4">
              <label class="form-label">Date Worked</label>
              <input type="date" v-model="otDate" class="form-control" required />
            </div>

            <div class="form-group mb-4">
              <label class="form-label">Overtime Hours (Max 12 per request)</label>
              <input 
                type="number" 
                v-model="otHours" 
                class="form-control" 
                min="0.5" 
                max="12"
                step="0.5"
                placeholder="e.g. 3.5 or 4"
                required 
              />
            </div>

            <div class="form-group mb-4">
              <label class="form-label">Purpose / Deliverables Completed</label>
              <textarea 
                v-model="otPurpose" 
                class="form-control" 
                rows="4"
                placeholder="Describe critical system upgrades, deployment supports, or client meetings..."
                required
              ></textarea>
            </div>

            <button type="submit" class="btn btn-primary btn-full" :disabled="isLoading">
              <IconHelper name="plus" size="16" class="mr-1" />
              Log Hours
            </button>
          </form>
        </div>
      </div>

      <!-- Logs history / Manager approvals table -->
      <div :class="activeTab === 'my-overtime' ? 'col-8' : 'col-12'">
        <div class="glass-card">
          <div class="flex justify-between items-center mb-4">
            <h3 v-if="activeTab === 'my-overtime'">My Overtime Logs</h3>
            <h3 v-else>Overtime Approvals Pipeline</h3>
            <button class="btn btn-ghost btn-sm" @click="hrStore.fetchOvertimeRecords(authStore.user.id)">
              <IconHelper name="refresh-cw" size="14" /> Refresh
            </button>
          </div>

          <div class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>Date Worked</th>
                  <th>Overtime Hours</th>
                  <th>Task Purpose</th>
                  <th>Status</th>
                  <th>Reviewer / Details</th>
                  <th v-if="activeTab === 'manager-approvals'">Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="record in (activeTab === 'my-overtime' ? myLogs : pendingApprovals)" :key="record.id">
                  <td>
                    <div class="font-semibold text-sm">{{ getEmployeeName(record.employeeId) }}</div>
                    <div class="text-xs text-muted font-mono">{{ getEmployeeCode(record.employeeId) }}</div>
                  </td>
                  <td class="text-xs font-mono">{{ record.date }}</td>
                  <td class="font-mono text-sm font-bold">{{ record.hours }} hrs</td>
                  <td class="text-xs text-muted max-width-200 text-truncate" :title="record.purpose">
                    {{ record.purpose }}
                  </td>
                  <td>
                    <span class="badge text-xs" :class="getStatusBadgeClass(record.status)">
                      {{ record.status }}
                    </span>
                  </td>
                  <td class="text-xs italic">
                    {{ record.approvedBy ? getEmployeeName(record.approvedBy) : 'Awaiting reviewer' }}
                  </td>
                  <td v-if="activeTab === 'manager-approvals'">
                    <div class="flex gap-2" v-if="record.status === 'PENDING'">
                      <button class="btn btn-success btn-sm py-1 px-3" @click="handleApprove(record.id)">
                        Approve
                      </button>
                      <button class="btn btn-danger btn-sm py-1 px-3" @click="handleReject(record.id)">
                        Reject
                      </button>
                    </div>
                    <div v-else class="text-xs text-muted">Verified</div>
                  </td>
                </tr>
                <tr v-if="(activeTab === 'my-overtime' ? myLogs : pendingApprovals).length === 0">
                  <td colspan="7" class="text-center text-secondary p-6">No overtime logs found in this queue.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.overtime {
  animation: fadeIn 0.4s ease;
}

.max-width-200 {
  max-width: 200px;
}

.text-truncate {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.font-semibold { font-weight: 600; }
.font-bold { font-weight: 700; }
.font-mono { font-family: monospace; }
.text-xs { font-size: 12px; }
.text-sm { font-size: 13px; }
.text-lg { font-size: 18px; }

.flex { display: flex; }
.items-center { align-items: center; }
.justify-between { justify-content: space-between; }
.gap-2 { gap: 8px; }
.gap-4 { gap: 16px; }

.rounded { border-radius: var(--radius-sm); }
.border { border: 1px solid var(--border-color); }

.mb-4 { margin-bottom: 16px; }
.mb-6 { margin-bottom: 24px; }
.py-1 { padding-top: 4px; padding-bottom: 4px; }
.py-2 { padding-top: 8px; padding-bottom: 8px; }
.py-4 { padding-top: 16px; padding-bottom: 16px; }
.p-1 { padding: 4px; }
.p-3 { padding: 12px; }
.p-6 { padding: 24px; }

.btn-full { width: 100%; }
.italic { font-style: italic; }
</style>
