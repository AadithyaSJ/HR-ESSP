<script setup>
import { ref, computed, onMounted } from 'vue';
import { useAuthStore } from '../stores/authStore.js';
import { useHrStore } from '../stores/hrStore.js';
import IconHelper from '../components/IconHelper.vue';

const authStore = useAuthStore();
const hrStore = useHrStore();

const activeTab = ref('my-resignation'); // 'my-resignation' or 'hr-review'
const isHrOrSysAdmin = computed(() => ['HR_ADMIN', 'SYSTEM_ADMIN'].includes(authStore.activeRole));

// Resignation Form State
const requestedLwd = ref('');
const resignationReason = ref('');

// Review Modals State
const showApproveModal = ref(false);
const showRejectModal = ref(false);
const activeResignation = ref(null);
const adminApprovedLwd = ref('');
const adminRejectionReason = ref('');

const isLoading = ref(false);

onMounted(async () => {
  isLoading.value = true;
  try {
    await hrStore.fetchEmployees();
    if (authStore.user) {
      await hrStore.fetchResignations(authStore.user.id);
    }
  } finally {
    isLoading.value = false;
  }
});

// Find active user profile in employees list to check status (e.g. NOTICE_PERIOD)
const myProfile = computed(() => {
  if (!authStore.user) return null;
  return hrStore.employees.find(e => e.id === authStore.user.id);
});

const myResignations = computed(() => {
  if (!authStore.user) return [];
  return hrStore.resignations.filter(r => r.employeeId === authStore.user.id);
});

const myActiveResignation = computed(() => {
  return myResignations.value[0] || null; // Latest submission
});

const allResignations = computed(() => {
  return hrStore.resignations;
});

// Notice Period countdown calculation
const countdownDays = computed(() => {
  const activeRes = myActiveResignation.value;
  if (!activeRes || activeRes.status !== 'APPROVED' || !activeRes.approvedLastWorkingDay) {
    return null;
  }
  const lwd = new Date(activeRes.approvedLastWorkingDay);
  const today = new Date();
  // Clear time components
  lwd.setHours(0, 0, 0, 0);
  today.setHours(0, 0, 0, 0);
  const diffTime = lwd.getTime() - today.getTime();
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  return diffDays >= 0 ? diffDays : 0;
});

// Stats
const resignationStats = computed(() => {
  const list = allResignations.value;
  return {
    total: list.length,
    pending: list.filter(r => r.status === 'PENDING').length,
    approved: list.filter(r => r.status === 'APPROVED').length,
    rejected: list.filter(r => r.status === 'REJECTED').length
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

async function handleSubmitResignation() {
  if (!requestedLwd.value || !resignationReason.value.trim()) {
    window.showPortalToast('Please select a requested last working day and provide a reason', 'error');
    return;
  }

  const requestedDate = new Date(requestedLwd.value);
  const today = new Date();
  if (requestedDate < today) {
    window.showPortalToast('Requested last working day cannot be in the past', 'error');
    return;
  }

  const payload = {
    employeeId: authStore.user.id,
    submissionDate: today.toISOString().split('T')[0],
    requestedLastWorkingDay: requestedLwd.value,
    reason: resignationReason.value.trim(),
    status: 'PENDING'
  };

  try {
    isLoading.value = true;
    await hrStore.submitResignation(payload, authStore.user.email);
    window.showPortalToast('Resignation request submitted to HR successfully', 'success');
    requestedLwd.value = '';
    resignationReason.value = '';
    await hrStore.fetchResignations(authStore.user.id);
  } catch (err) {
    window.showPortalToast(err.message || 'Failed to submit resignation request', 'error');
  } finally {
    isLoading.value = false;
  }
}

function openApproveModal(res) {
  activeResignation.value = res;
  adminApprovedLwd.value = res.requestedLastWorkingDay;
  showApproveModal.value = true;
}

async function handleConfirmApprove() {
  if (!activeResignation.value) return;
  if (!adminApprovedLwd.value) {
    window.showPortalToast('Please specify an approved Last Working Day', 'error');
    return;
  }

  try {
    isLoading.value = true;
    await hrStore.updateResignationStatus(
      activeResignation.value.id,
      'APPROVED',
      adminApprovedLwd.value,
      null,
      authStore.user.email
    );
    window.showPortalToast('Resignation approved. Employee is now serving notice period.', 'success');
    showApproveModal.value = false;
    activeResignation.value = null;
    await hrStore.fetchResignations(authStore.user.id);
    await hrStore.fetchEmployees(); // Update profiles to sync notice state
  } catch (err) {
    window.showPortalToast(err.message || 'Failed to approve resignation', 'error');
  } finally {
    isLoading.value = false;
  }
}

function openRejectModal(res) {
  activeResignation.value = res;
  adminRejectionReason.value = '';
  showRejectModal.value = true;
}

async function handleConfirmReject() {
  if (!activeResignation.value) return;
  if (!adminRejectionReason.value.trim()) {
    window.showPortalToast('Rejection reason is mandatory', 'error');
    return;
  }

  try {
    isLoading.value = true;
    await hrStore.updateResignationStatus(
      activeResignation.value.id,
      'REJECTED',
      null,
      adminRejectionReason.value.trim(),
      authStore.user.email
    );
    window.showPortalToast('Resignation rejected successfully', 'warning');
    showRejectModal.value = false;
    activeResignation.value = null;
    await hrStore.fetchResignations(authStore.user.id);
  } catch (err) {
    window.showPortalToast(err.message || 'Failed to reject resignation', 'error');
  } finally {
    isLoading.value = false;
  }
}
</script>

<template>
  <div class="resignation container-fluid py-4">
    <!-- Header -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h1 class="page-title">Resignation Desk & Exit Management</h1>
        <p class="text-secondary text-sm">Submit resignation applications, review exit notices, and monitor notice period constraints.</p>
      </div>

      <!-- Navigation Tabs -->
      <div v-if="isHrOrSysAdmin" class="flex gap-2 bg-sidebar rounded p-1 border">
        <button 
          class="btn btn-sm px-4 py-2" 
          :class="activeTab === 'my-resignation' ? 'btn-primary' : 'btn-ghost'"
          @click="activeTab = 'my-resignation'"
        >
          My Exit Desk
        </button>
        <button 
          class="btn btn-sm px-4 py-2" 
          :class="activeTab === 'hr-review' ? 'btn-primary' : 'btn-ghost'"
          @click="activeTab = 'hr-review'"
        >
          Exit Desk Admin Console
        </button>
      </div>
    </div>

    <!-- Quick Stats Grid (Admin only) -->
    <div class="grid-12 mb-6" v-if="activeTab === 'hr-review'">
      <div class="col-3">
        <div class="glass-card flex items-center gap-4">
          <div class="badge badge-primary p-3" style="border-radius: var(--radius-sm)">
            <IconHelper name="file-text" size="24" />
          </div>
          <div>
            <div class="text-secondary text-xs font-semibold">Total exit files</div>
            <div class="text-lg font-bold">{{ resignationStats.total }}</div>
          </div>
        </div>
      </div>
      <div class="col-3">
        <div class="glass-card flex items-center gap-4">
          <div class="badge badge-warning p-3" style="border-radius: var(--radius-sm)">
            <IconHelper name="clock" size="24" />
          </div>
          <div>
            <div class="text-secondary text-xs font-semibold">Awaiting HR Review</div>
            <div class="text-lg font-bold">{{ resignationStats.pending }}</div>
          </div>
        </div>
      </div>
      <div class="col-3">
        <div class="glass-card flex items-center gap-4">
          <div class="badge badge-success p-3" style="border-radius: var(--radius-sm)">
            <IconHelper name="check-circle" size="24" />
          </div>
          <div>
            <div class="text-secondary text-xs font-semibold">Approved Notices</div>
            <div class="text-lg font-bold">{{ resignationStats.approved }}</div>
          </div>
        </div>
      </div>
      <div class="col-3">
        <div class="glass-card flex items-center gap-4">
          <div class="badge badge-danger p-3" style="border-radius: var(--radius-sm)">
            <IconHelper name="log-out" size="24" />
          </div>
          <div>
            <div class="text-secondary text-xs font-semibold">Rejected Requests</div>
            <div class="text-lg font-bold">{{ resignationStats.rejected }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Main Workspace -->
    <div class="grid-12">
      <!-- Notice Serving Countdown Block (Employee active notice) -->
      <div class="col-12 mb-6" v-if="activeTab === 'my-resignation' && myProfile?.status === 'NOTICE_PERIOD' && countdownDays !== null">
        <div class="glass-card bg-glow-notice flex justify-between items-center p-6 border-notice">
          <div>
            <span class="badge badge-danger text-xs mb-2">Notice Period Active</span>
            <h2 class="text-xl font-bold mb-2">Serving Resignation Notice Period</h2>
            <p class="text-secondary text-sm max-width-600">
              Notice Status: Your exit notice is active. According to company policy, you are serving your notice period. 
              <br/>
              <b>Policy restriction:</b> Leaves during the notice period are restricted to <b>SICK</b> leave types only. Other leave applications will be blocked.
            </p>
          </div>
          <div class="text-center p-4 rounded bg-sidebar border" style="min-width: 180px;">
            <div class="text-muted text-xs uppercase font-bold">Days Remaining</div>
            <div class="text-lg font-bold text-danger" style="font-size: 36px; line-height: 1;">{{ countdownDays }}</div>
            <div class="text-xs text-secondary mt-1">LWD: {{ myActiveResignation?.approvedLastWorkingDay }}</div>
          </div>
        </div>
      </div>

      <!-- Application Form for resignations -->
      <div class="col-5" v-if="activeTab === 'my-resignation'">
        <div class="glass-card">
          <h3 class="mb-4">Submit Resignation Notice</h3>
          
          <div v-if="myProfile?.status === 'NOTICE_PERIOD'" class="alert alert-info mb-4" style="min-width: 100%">
            <span>You are currently serving notice period and cannot file another resignation.</span>
          </div>
          <div v-else-if="myActiveResignation?.status === 'PENDING'" class="alert alert-warning mb-4" style="min-width: 100%">
            <span>Your resignation request is pending HR review.</span>
          </div>

          <form v-else @submit.prevent="handleSubmitResignation">
            <div class="form-group mb-4">
              <label class="form-label">Requested Last Working Day (LWD)</label>
              <input type="date" v-model="requestedLwd" class="form-control" required />
              <span class="text-muted text-xs mt-1 block">Subject to standard notice period requirements (typically 30-60 days).</span>
            </div>

            <div class="form-group mb-4">
              <label class="form-label">Detailed Reason for Resignation / Remarks</label>
              <textarea 
                v-model="resignationReason" 
                class="form-control" 
                rows="6"
                placeholder="Please explain exit reason, transition handovers, etc..."
                required
              ></textarea>
            </div>

            <button type="submit" class="btn btn-danger btn-full" :disabled="isLoading">
              <IconHelper name="log-out" size="16" class="mr-1" />
              File Resignation
            </button>
          </form>
        </div>
      </div>

      <!-- Exit notice history / HR Queue -->
      <div :class="activeTab === 'my-resignation' ? 'col-7' : 'col-12'">
        <div class="glass-card">
          <div class="flex justify-between items-center mb-4">
            <h3 v-if="activeTab === 'my-resignation'">My Resignation Record</h3>
            <h3 v-else>Active Resignations Desk (HR Exit Pipeline)</h3>
            <button class="btn btn-ghost btn-sm" @click="hrStore.fetchResignations(authStore.user.id)">
              <IconHelper name="refresh-cw" size="14" /> Refresh
            </button>
          </div>

          <div class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>Submission Date</th>
                  <th>Requested LWD</th>
                  <th>Approved LWD</th>
                  <th>Status</th>
                  <th>Details / HR remarks</th>
                  <th v-if="activeTab === 'hr-review'">Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="res in (activeTab === 'my-resignation' ? myResignations : allResignations)" :key="res.id">
                  <td>
                    <div class="font-semibold text-sm">{{ getEmployeeName(res.employeeId) }}</div>
                    <div class="text-xs text-muted font-mono">{{ getEmployeeCode(res.employeeId) }}</div>
                  </td>
                  <td class="text-xs font-mono">{{ res.submissionDate }}</td>
                  <td class="text-xs font-mono">{{ res.requestedLastWorkingDay }}</td>
                  <td class="text-xs font-mono font-bold text-success">{{ res.approvedLastWorkingDay || 'Not determined' }}</td>
                  <td>
                    <span class="badge text-xs" :class="getStatusBadgeClass(res.status)">
                      {{ res.status }}
                    </span>
                  </td>
                  <td class="text-xs">
                    <div class="text-truncate max-width-200" :title="res.reason">Reason: {{ res.reason }}</div>
                    <div v-if="res.rejectionReason" class="text-danger text-truncate max-width-200" :title="res.rejectionReason">
                      Rejection: {{ res.rejectionReason }}
                    </div>
                  </td>
                  <td v-if="activeTab === 'hr-review'">
                    <div class="flex gap-2" v-if="res.status === 'PENDING'">
                      <button class="btn btn-success btn-sm py-1 px-3" @click="openApproveModal(res)">
                        Approve
                      </button>
                      <button class="btn btn-danger btn-sm py-1 px-3" @click="openRejectModal(res)">
                        Reject
                      </button>
                    </div>
                    <div v-else class="text-xs text-muted">Reviewed</div>
                  </td>
                </tr>
                <tr v-if="(activeTab === 'my-resignation' ? myResignations : allResignations).length === 0">
                  <td colspan="7" class="text-center text-secondary p-6">No resignation records found.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- HR APPROVE MODAL -->
    <div class="modal-overlay" v-if="showApproveModal && activeResignation">
      <div class="modal-content">
        <div class="modal-header">
          <h3>Approve Exit Resignation</h3>
          <button class="btn btn-ghost btn-sm" @click="showApproveModal = false">
            <IconHelper name="plus" style="transform: rotate(45deg);" size="14" />
          </button>
        </div>
        <div class="modal-body">
          <p class="mb-4">Confirm resignation of <b>{{ getEmployeeName(activeResignation.employeeId) }}</b>.</p>
          <div class="form-group mb-4">
            <label class="form-label">Approved Last Working Day (LWD)</label>
            <input type="date" v-model="adminApprovedLwd" class="form-control" required />
            <span class="text-xs text-muted block mt-1">Default set to requested date: {{ activeResignation.requestedLastWorkingDay }}</span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showApproveModal = false">Cancel</button>
          <button class="btn btn-success" @click="handleConfirmApprove" :disabled="isLoading">
            Confirm & Release notice
          </button>
        </div>
      </div>
    </div>

    <!-- HR REJECT MODAL -->
    <div class="modal-overlay" v-if="showRejectModal && activeResignation">
      <div class="modal-content">
        <div class="modal-header">
          <h3>Reject Exit Request</h3>
          <button class="btn btn-ghost btn-sm" @click="showRejectModal = false">
            <IconHelper name="plus" style="transform: rotate(45deg);" size="14" />
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group mb-4">
            <label class="form-label">HR rejection remarks / reasons *</label>
            <textarea 
              v-model="adminRejectionReason" 
              class="form-control" 
              rows="3" 
              placeholder="e.g. Exit terms disputed, insufficient handover planning..."
              required
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showRejectModal = false">Cancel</button>
          <button class="btn btn-danger" @click="handleConfirmReject" :disabled="isLoading">
            Confirm Rejection
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.resignation {
  animation: fadeIn 0.4s ease;
}

.max-width-200 {
  max-width: 200px;
}
.max-width-600 {
  max-width: 600px;
}

.text-truncate {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bg-glow-notice {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.08) 0%, rgba(239, 68, 68, 0.02) 100%);
}
.border-notice {
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.font-semibold { font-weight: 600; }
.font-bold { font-weight: 700; }
.font-mono { font-family: monospace; }
.text-xs { font-size: 12px; }
.text-sm { font-size: 13px; }
.text-lg { font-size: 18px; }
.text-xl { font-size: 20px; }

.flex { display: flex; }
.items-center { align-items: center; }
.justify-between { justify-content: space-between; }
.gap-2 { gap: 8px; }
.gap-4 { gap: 16px; }

.rounded { border-radius: var(--radius-sm); }
.border { border: 1px solid var(--border-color); }

.mb-2 { margin-bottom: 8px; }
.mb-4 { margin-bottom: 16px; }
.mb-6 { margin-bottom: 24px; }
.py-1 { padding-top: 4px; padding-bottom: 4px; }
.py-2 { padding-top: 8px; padding-bottom: 8px; }
.py-4 { padding-top: 16px; padding-bottom: 16px; }
.p-1 { padding: 4px; }
.p-3 { padding: 12px; }
.p-4 { padding: 16px; }
.p-6 { padding: 24px; }

.btn-full { width: 100%; }
.italic { font-style: italic; }
.block { display: block; }
</style>
