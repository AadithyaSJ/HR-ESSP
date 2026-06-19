<script setup>
import { ref, computed, onMounted } from 'vue';
import { useAuthStore } from '../stores/authStore.js';
import { useHrStore } from '../stores/hrStore.js';
import IconHelper from '../components/IconHelper.vue';

const authStore = useAuthStore();
const hrStore = useHrStore();

const activeTab = ref('my-requests'); // 'my-requests', 'approvals', 'booking-desk'
const isManager = computed(() => ['MANAGER', 'HR_ADMIN'].includes(authStore.activeRole));
const isAdminOrFinance = computed(() => ['HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN'].includes(authStore.activeRole));

// Travel Request Form State
const travelDestination = ref('');
const travelPurpose = ref('');
const travelStartDate = ref('');
const travelEndDate = ref('');
const travelNeedsTicket = ref(false);
const travelNeedsAccommodation = ref(false);
const travelAccommodationDetails = ref('');
const travelEstimatedCost = ref(0);
const travelManagerId = ref('');

// Rejection modal
const showRejectModal = ref(false);
const activeRequest = ref(null);
const rejectionReason = ref('');

const isLoading = ref(false);

onMounted(async () => {
  isLoading.value = true;
  try {
    await hrStore.fetchEmployees();
    if (authStore.user) {
      await hrStore.fetchTravelRequests(authStore.user.id);
    }
  } finally {
    isLoading.value = false;
  }
});

// Managers dropdown list
const managersList = computed(() => {
  return hrStore.employees.filter(e => ['MANAGER', 'HR_ADMIN'].includes(e.role));
});

// Requests categorization
const myRequests = computed(() => {
  if (!authStore.user) return [];
  return hrStore.travelRequests.filter(r => r.employeeId === authStore.user.id);
});

const pendingApprovals = computed(() => {
  // Requests assigned to me as a manager
  if (!authStore.user) return [];
  return hrStore.travelRequests.filter(r => r.managerId === authStore.user.id && r.status === 'PENDING');
});

const bookingDeskRequests = computed(() => {
  // Approved requests awaiting booking confirmation
  return hrStore.travelRequests.filter(r => r.status === 'APPROVED');
});

const allRequests = computed(() => {
  return hrStore.travelRequests;
});

// Stats
const travelStats = computed(() => {
  const list = isAdminOrFinance.value ? allRequests.value : (isManager.value ? [...myRequests.value, ...pendingApprovals.value] : myRequests.value);
  return {
    total: list.length,
    pending: list.filter(r => r.status === 'PENDING').length,
    approved: list.filter(r => r.status === 'APPROVED').length,
    booked: list.filter(r => r.status === 'BOOKED').length
  };
});

function getEmployeeName(empId) {
  const emp = hrStore.employees.find(e => e.id === empId);
  return emp ? emp.fullName : 'Employee';
}

function getStatusBadgeClass(status) {
  const map = {
    PENDING: 'badge-warning',
    APPROVED: 'badge-info',
    BOOKED: 'badge-success',
    REJECTED: 'badge-danger'
  };
  return map[status] || 'badge-primary';
}

async function handleCreateRequest() {
  if (!travelDestination.value.trim() || !travelPurpose.value.trim() || !travelStartDate.value || !travelEndDate.value || !travelManagerId.value) {
    window.showPortalToast('Please fill out all mandatory fields', 'error');
    return;
  }

  const start = new Date(travelStartDate.value);
  const end = new Date(travelEndDate.value);
  if (end < start) {
    window.showPortalToast('End date cannot be before start date', 'error');
    return;
  }

  const payload = {
    employeeId: authStore.user.id,
    purpose: travelPurpose.value.trim(),
    destination: travelDestination.value.trim(),
    startDate: travelStartDate.value,
    endDate: travelEndDate.value,
    needsTicket: travelNeedsTicket.value,
    needsAccommodation: travelNeedsAccommodation.value,
    accommodationDetails: travelNeedsAccommodation.value ? travelAccommodationDetails.value.trim() : '',
    estimatedCost: parseFloat(travelEstimatedCost.value) || 0.0,
    managerId: travelManagerId.value,
    status: 'PENDING'
  };

  try {
    isLoading.value = true;
    await hrStore.createTravelRequest(payload, authStore.user.email);
    window.showPortalToast('Travel request submitted successfully', 'success');

    // Reset Form
    travelDestination.value = '';
    travelPurpose.value = '';
    travelStartDate.value = '';
    travelEndDate.value = '';
    travelNeedsTicket.value = false;
    travelNeedsAccommodation.value = false;
    travelAccommodationDetails.value = '';
    travelEstimatedCost.value = 0;
    travelManagerId.value = '';
    
    // Refresh
    await hrStore.fetchTravelRequests(authStore.user.id);
  } catch (err) {
    window.showPortalToast(err.message || 'Failed to submit travel request', 'error');
  } finally {
    isLoading.value = false;
  }
}

async function handleApprove(reqId) {
  try {
    isLoading.value = true;
    await hrStore.updateTravelRequestStatus(reqId, 'APPROVED', null, authStore.user.email);
    window.showPortalToast('Travel request approved', 'success');
    await hrStore.fetchTravelRequests(authStore.user.id);
  } catch (err) {
    window.showPortalToast(err.message || 'Failed to approve request', 'error');
  } finally {
    isLoading.value = false;
  }
}

function openRejectModal(request) {
  activeRequest.value = request;
  rejectionReason.value = '';
  showRejectModal.value = true;
}

async function handleConfirmReject() {
  if (!activeRequest.value) return;
  if (!rejectionReason.value.trim()) {
    window.showPortalToast('Rejection reason is mandatory', 'error');
    return;
  }

  try {
    isLoading.value = true;
    await hrStore.updateTravelRequestStatus(activeRequest.value.id, 'REJECTED', rejectionReason.value.trim(), authStore.user.email);
    window.showPortalToast('Travel request rejected', 'warning');
    showRejectModal.value = false;
    activeRequest.value = null;
    await hrStore.fetchTravelRequests(authStore.user.id);
  } catch (err) {
    window.showPortalToast(err.message || 'Failed to reject request', 'error');
  } finally {
    isLoading.value = false;
  }
}

async function handleBook(reqId) {
  try {
    isLoading.value = true;
    await hrStore.updateTravelRequestStatus(reqId, 'BOOKED', null, authStore.user.email);
    window.showPortalToast('Travel booking completed. E-tickets generated.', 'success');
    await hrStore.fetchTravelRequests(authStore.user.id);
  } catch (err) {
    window.showPortalToast(err.message || 'Failed to confirm booking', 'error');
  } finally {
    isLoading.value = false;
  }
}
</script>

<template>
  <div class="travel-desk container-fluid py-4">
    <!-- Header -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h1 class="page-title">Corporate Travel Desk</h1>
        <p class="text-secondary text-sm">Submit business travel requests, request pre-arranged tickets, and book accommodation.</p>
      </div>

      <!-- Navigation Tabs -->
      <div class="flex gap-2 bg-sidebar rounded p-1 border">
        <button 
          class="btn btn-sm px-4 py-2" 
          :class="activeTab === 'my-requests' ? 'btn-primary' : 'btn-ghost'"
          @click="activeTab = 'my-requests'"
        >
          My Requests
        </button>
        <button 
          v-if="isManager"
          class="btn btn-sm px-4 py-2" 
          :class="activeTab === 'approvals' ? 'btn-primary' : 'btn-ghost'"
          @click="activeTab = 'approvals'"
        >
          Approvals Desk
        </button>
        <button 
          v-if="isAdminOrFinance"
          class="btn btn-sm px-4 py-2" 
          :class="activeTab === 'booking-desk' ? 'btn-primary' : 'btn-ghost'"
          @click="activeTab = 'booking-desk'"
        >
          Booking Desk Console
        </button>
      </div>
    </div>

    <!-- Quick Stats Grid -->
    <div class="grid-12 mb-6">
      <div class="col-3">
        <div class="glass-card flex items-center gap-4">
          <div class="badge badge-primary p-3" style="border-radius: var(--radius-sm)">
            <IconHelper name="file-text" size="24" />
          </div>
          <div>
            <div class="text-secondary text-xs font-semibold">Total Claims</div>
            <div class="text-lg font-bold">{{ travelStats.total }}</div>
          </div>
        </div>
      </div>
      <div class="col-3">
        <div class="glass-card flex items-center gap-4">
          <div class="badge badge-warning p-3" style="border-radius: var(--radius-sm)">
            <IconHelper name="clock" size="24" />
          </div>
          <div>
            <div class="text-secondary text-xs font-semibold">Pending Approval</div>
            <div class="text-lg font-bold">{{ travelStats.pending }}</div>
          </div>
        </div>
      </div>
      <div class="col-3">
        <div class="glass-card flex items-center gap-4">
          <div class="badge badge-info p-3" style="border-radius: var(--radius-sm)">
            <IconHelper name="check" size="24" />
          </div>
          <div>
            <div class="text-secondary text-xs font-semibold">Approved by Mgr</div>
            <div class="text-lg font-bold">{{ travelStats.approved }}</div>
          </div>
        </div>
      </div>
      <div class="col-3">
        <div class="glass-card flex items-center gap-4">
          <div class="badge badge-success p-3" style="border-radius: var(--radius-sm)">
            <IconHelper name="check-circle" size="24" />
          </div>
          <div>
            <div class="text-secondary text-xs font-semibold">Booked & Confirmed</div>
            <div class="text-lg font-bold">{{ travelStats.booked }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- main content workspace -->
    <div class="grid-12">
      <!-- Create request Form -->
      <div class="col-4" v-if="activeTab === 'my-requests'">
        <div class="glass-card">
          <h3 class="mb-4">Raise Travel Request</h3>
          <form @submit.prevent="handleCreateRequest">
            <div class="form-group mb-4">
              <label class="form-label">Destination City / Country</label>
              <input 
                type="text" 
                v-model="travelDestination" 
                class="form-control" 
                placeholder="e.g. London, UK or Bengaluru, India"
                required 
              />
            </div>

            <div class="form-group mb-4">
              <label class="form-label">Business Purpose</label>
              <input 
                type="text" 
                v-model="travelPurpose" 
                class="form-control" 
                placeholder="e.g. Q3 Client presentation"
                required 
              />
            </div>

            <div class="grid-12 gap-2 mb-4">
              <div class="col-6" style="padding:0">
                <label class="form-label">Start Date</label>
                <input type="date" v-model="travelStartDate" class="form-control" required />
              </div>
              <div class="col-6" style="padding:0">
                <label class="form-label">End Date</label>
                <input type="date" v-model="travelEndDate" class="form-control" required />
              </div>
            </div>

            <div class="form-group mb-4">
              <label class="form-label">Reporting Manager (for approval)</label>
              <select v-model="travelManagerId" class="form-control" required>
                <option value="" disabled>Select Manager</option>
                <option v-for="mgr in managersList" :key="mgr.id" :value="mgr.id">
                  {{ mgr.fullName }} ({{ mgr.designation }})
                </option>
              </select>
            </div>

            <div class="form-group mb-4 flex gap-4">
              <label class="flex items-center gap-2 cursor-pointer text-xs">
                <input type="checkbox" v-model="travelNeedsTicket" />
                Needs Travel Ticket (Flight/Train)
              </label>
              <label class="flex items-center gap-2 cursor-pointer text-xs">
                <input type="checkbox" v-model="travelNeedsAccommodation" />
                Needs Accommodation (Hotel)
              </label>
            </div>

            <div class="form-group mb-4" v-if="travelNeedsAccommodation">
              <label class="form-label">Accommodation Details / Preferences</label>
              <textarea 
                v-model="travelAccommodationDetails" 
                class="form-control" 
                rows="3"
                placeholder="e.g. Single room, near London headquarters, quiet zone..."
              ></textarea>
            </div>

            <div class="form-group mb-4">
              <label class="form-label">Estimated Cost / Pre-paid Budget limit (USD / INR)</label>
              <input 
                type="number" 
                v-model="travelEstimatedCost" 
                class="form-control" 
                placeholder="Estimated total cost"
                min="0"
                step="0.01"
              />
            </div>

            <button type="submit" class="btn btn-primary btn-full" :disabled="isLoading">
              <IconHelper name="plus" size="16" class="mr-1" />
              Submit Request
            </button>
          </form>
        </div>
      </div>

      <!-- Requests queue -->
      <div :class="activeTab === 'my-requests' ? 'col-8' : 'col-12'">
        <div class="glass-card">
          <div class="flex justify-between items-center mb-4">
            <h3 v-if="activeTab === 'my-requests'">My Travel Requests</h3>
            <h3 v-else-if="activeTab === 'approvals'">Pending Manager Approvals</h3>
            <h3 v-else>Corporate Booking Queue (Finance/Admin Desk)</h3>
            <button class="btn btn-ghost btn-sm" @click="hrStore.fetchTravelRequests(authStore.user.id)">
              <IconHelper name="refresh-cw" size="14" /> Refresh
            </button>
          </div>

          <div class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>Destination</th>
                  <th>Dates</th>
                  <th>Flight / Hotel</th>
                  <th>Estimated Cost</th>
                  <th>Status</th>
                  <th>Actions / Details</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="req in (activeTab === 'my-requests' ? myRequests : activeTab === 'approvals' ? pendingApprovals : bookingDeskRequests)" :key="req.id">
                  <td>
                    <div class="font-semibold text-sm">{{ getEmployeeName(req.employeeId) }}</div>
                    <div class="text-xs text-muted">Purpose: {{ req.purpose }}</div>
                  </td>
                  <td>
                    <div class="font-semibold text-sm">{{ req.destination }}</div>
                  </td>
                  <td class="text-xs font-mono">
                    {{ req.startDate }} to {{ req.endDate }}
                  </td>
                  <td class="text-xs">
                    <div>Ticket: {{ req.needsTicket ? 'Yes' : 'No' }}</div>
                    <div>Hotel: {{ req.needsAccommodation ? 'Yes' : 'No' }}</div>
                    <div v-if="req.needsAccommodation && req.accommodationDetails" class="text-muted text-xs text-truncate max-width-200">
                      {{ req.accommodationDetails }}
                    </div>
                  </td>
                  <td class="font-mono text-xs">${{ req.estimatedCost }}</td>
                  <td>
                    <span class="badge text-xs" :class="getStatusBadgeClass(req.status)">
                      {{ req.status }}
                    </span>
                    <div v-if="req.status === 'REJECTED' && req.rejectionReason" class="text-xs text-danger mt-1">
                      Reason: {{ req.rejectionReason }}
                    </div>
                  </td>
                  <td>
                    <div class="flex gap-2" v-if="activeTab === 'approvals'">
                      <button class="btn btn-success btn-sm py-1 px-3" @click="handleApprove(req.id)">
                        Approve
                      </button>
                      <button class="btn btn-danger btn-sm py-1 px-3" @click="openRejectModal(req)">
                        Reject
                      </button>
                    </div>
                    <div v-else-if="activeTab === 'booking-desk'">
                      <button class="btn btn-success btn-sm py-1 px-3" @click="handleBook(req.id)">
                        <IconHelper name="check-circle" size="12" /> Book Ticket
                      </button>
                    </div>
                    <div v-else class="text-xs text-muted">
                      No actions
                    </div>
                  </td>
                </tr>
                <tr v-if="(activeTab === 'my-requests' ? myRequests : activeTab === 'approvals' ? pendingApprovals : bookingDeskRequests).length === 0">
                  <td colspan="7" class="text-center text-secondary p-6">No travel requests found in this queue.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- REJECTION MODAL -->
    <div class="modal-overlay" v-if="showRejectModal && activeRequest">
      <div class="modal-content">
        <div class="modal-header">
          <h3>Reject Travel Request</h3>
          <button class="btn btn-ghost btn-sm" @click="showRejectModal = false">
            <IconHelper name="plus" style="transform: rotate(45deg);" size="14" />
          </button>
        </div>
        <div class="modal-body">
          <p class="mb-4">Provide a reason for rejecting the travel request to <b>{{ activeRequest.destination }}</b>.</p>
          <div class="form-group mb-4">
            <label class="form-label">Rejection Reason *</label>
            <textarea 
              v-model="rejectionReason" 
              class="form-control" 
              rows="3" 
              placeholder="e.g. Budget limitations, client visit rescheduled..."
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
.travel-desk {
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
