<script setup>
import { ref, computed, onMounted } from 'vue';
import { useAuthStore } from '../stores/authStore.js';
import { useHrStore } from '../stores/hrStore.js';
import IconHelper from '../components/IconHelper.vue';

const authStore = useAuthStore();
const hrStore = useHrStore();

const activeTab = ref('my-tickets'); // 'my-tickets' or 'admin-desk'
const isHrOrSysAdmin = computed(() => ['HR_ADMIN', 'SYSTEM_ADMIN'].includes(authStore.activeRole));

// Ticket Form State
const ticketTitle = ref('');
const ticketCategory = ref('TECHNICAL_ISSUE');
const ticketDescription = ref('');

// Admin Update State
const showUpdateModal = ref(false);
const activeTicket = ref(null);
const adminStatus = ref('PENDING');
const adminAssignedTo = ref('');
const adminComment = ref('');

const isLoading = ref(false);

onMounted(async () => {
  isLoading.value = true;
  try {
    await hrStore.fetchEmployees();
    if (authStore.user) {
      await hrStore.fetchItTickets(authStore.user.id);
    }
  } finally {
    isLoading.value = false;
  }
});

function changeTab(tab) {
  activeTab.value = tab;
}

const getCategoryLabel = (category) => {
  const map = {
    TECHNICAL_ISSUE: 'Technical Issue',
    HARDWARE_ISSUE: 'Hardware Issue',
    SOFTWARE_INSTALLATION: 'Software Installation',
    SOFTWARE_UPGRADE: 'Software Upgrade',
    OTHER: 'Other Support'
  };
  return map[category] || category;
};

const getStatusBadgeClass = (status) => {
  const map = {
    PENDING: 'badge-warning',
    IN_PROGRESS: 'badge-info',
    RESOLVED: 'badge-success',
    CLOSED: 'badge-secondary'
  };
  return map[status] || 'badge-primary';
};

// Filtered list of tickets
const myTickets = computed(() => {
  if (!authStore.user) return [];
  return hrStore.itTickets.filter(t => t.employeeId === authStore.user.id);
});

const allTickets = computed(() => {
  return hrStore.itTickets;
});

// Stats
const ticketStats = computed(() => {
  const list = isHrOrSysAdmin.value ? allTickets.value : myTickets.value;
  return {
    total: list.length,
    pending: list.filter(t => t.status === 'PENDING').length,
    inProgress: list.filter(t => t.status === 'IN_PROGRESS').length,
    resolved: list.filter(t => t.status === 'RESOLVED' || t.status === 'CLOSED').length
  };
});

async function handleCreateTicket() {
  if (!ticketTitle.value.trim() || !ticketDescription.value.trim()) {
    window.showPortalToast('Please fill out all fields', 'error');
    return;
  }

  const payload = {
    employeeId: authStore.user.id,
    title: ticketTitle.value.trim(),
    category: ticketCategory.value,
    description: ticketDescription.value.trim(),
    status: 'PENDING'
  };

  try {
    isLoading.value = true;
    await hrStore.createItTicket(payload, authStore.user.email);
    window.showPortalToast('IT Support Ticket raised successfully', 'success');
    
    // Reset Form
    ticketTitle.value = '';
    ticketCategory.value = 'TECHNICAL_ISSUE';
    ticketDescription.value = '';
    
    // Refresh tickets list
    await hrStore.fetchItTickets(authStore.user.id);
  } catch (err) {
    window.showPortalToast(err.message || 'Failed to raise support ticket', 'error');
  } finally {
    isLoading.value = false;
  }
}

function openUpdateModal(ticket) {
  activeTicket.value = ticket;
  adminStatus.value = ticket.status;
  adminAssignedTo.value = ticket.assignedTo || '';
  adminComment.value = ticket.comments || '';
  showUpdateModal.value = true;
}

async function handleUpdateTicket() {
  if (!activeTicket.value) return;

  try {
    isLoading.value = true;
    await hrStore.updateItTicketStatus(
      activeTicket.value.id,
      adminStatus.value,
      adminAssignedTo.value.trim(),
      adminComment.value.trim(),
      authStore.user.email
    );
    window.showPortalToast('Ticket updated successfully', 'success');
    showUpdateModal.value = false;
    activeTicket.value = null;
    
    // Refresh
    await hrStore.fetchItTickets(authStore.user.id);
  } catch (err) {
    window.showPortalToast(err.message || 'Failed to update ticket', 'error');
  } finally {
    isLoading.value = false;
  }
}
</script>

<template>
  <div class="it-kiosk container-fluid py-4">
    <!-- Header -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h1 class="page-title">IT Support Kiosk</h1>
        <p class="text-secondary text-sm">Raise support tickets, request hardware/software installations, or request software packages.</p>
      </div>
      
      <!-- Tabs switching if Admin -->
      <div v-if="isHrOrSysAdmin" class="flex gap-2 bg-sidebar rounded p-1 border">
        <button 
          class="btn btn-sm px-4 py-2" 
          :class="activeTab === 'my-tickets' ? 'btn-primary' : 'btn-ghost'"
          @click="changeTab('my-tickets')"
        >
          My Support Tickets
        </button>
        <button 
          class="btn btn-sm px-4 py-2" 
          :class="activeTab === 'admin-desk' ? 'btn-primary' : 'btn-ghost'"
          @click="changeTab('admin-desk')"
        >
          IT Support Admin Desk
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
            <div class="text-secondary text-xs font-semibold">Total Tickets</div>
            <div class="text-lg font-bold">{{ ticketStats.total }}</div>
          </div>
        </div>
      </div>
      <div class="col-3">
        <div class="glass-card flex items-center gap-4">
          <div class="badge badge-warning p-3" style="border-radius: var(--radius-sm)">
            <IconHelper name="clock" size="24" />
          </div>
          <div>
            <div class="text-secondary text-xs font-semibold">Pending Review</div>
            <div class="text-lg font-bold">{{ ticketStats.pending }}</div>
          </div>
        </div>
      </div>
      <div class="col-3">
        <div class="glass-card flex items-center gap-4">
          <div class="badge badge-info p-3" style="border-radius: var(--radius-sm)">
            <IconHelper name="settings" size="24" />
          </div>
          <div>
            <div class="text-secondary text-xs font-semibold">In Progress</div>
            <div class="text-lg font-bold">{{ ticketStats.inProgress }}</div>
          </div>
        </div>
      </div>
      <div class="col-3">
        <div class="glass-card flex items-center gap-4">
          <div class="badge badge-success p-3" style="border-radius: var(--radius-sm)">
            <IconHelper name="check-circle" size="24" />
          </div>
          <div>
            <div class="text-secondary text-xs font-semibold">Resolved / Closed</div>
            <div class="text-lg font-bold">{{ ticketStats.resolved }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Main Content Workspace -->
    <div class="grid-12">
      <!-- Form Section to raise ticket -->
      <div class="col-4" v-if="activeTab === 'my-tickets'">
        <div class="glass-card">
          <h3 class="mb-4">Raise a Support Ticket</h3>
          <form @submit.prevent="handleCreateTicket">
            <div class="form-group mb-4">
              <label class="form-label">Ticket Title / Short Summary</label>
              <input 
                type="text" 
                v-model="ticketTitle" 
                class="form-control" 
                placeholder="e.g. Broken office WiFi or Docker package upgrade"
                required 
              />
            </div>

            <div class="form-group mb-4">
              <label class="form-label">Support Category</label>
              <select v-model="ticketCategory" class="form-control">
                <option value="TECHNICAL_ISSUE">Technical Issue (WiFi, Network)</option>
                <option value="HARDWARE_ISSUE">Hardware Issue (Laptop replacement, Charger)</option>
                <option value="SOFTWARE_INSTALLATION">Request Software Installation</option>
                <option value="SOFTWARE_UPGRADE">Request Software Package Upgrade</option>
                <option value="OTHER">Other Support Request</option>
              </select>
            </div>

            <div class="form-group mb-4">
              <label class="form-label">Detailed Description</label>
              <textarea 
                v-model="ticketDescription" 
                class="form-control" 
                rows="5"
                placeholder="Please describe your issue, request, or package upgrades in detail..."
                required
              ></textarea>
            </div>

            <button type="submit" class="btn btn-primary btn-full" :disabled="isLoading">
              <IconHelper name="plus" size="16" class="mr-1" />
              Submit Ticket
            </button>
          </form>
        </div>
      </div>

      <!-- Tickets List Section -->
      <div :class="activeTab === 'my-tickets' ? 'col-8' : 'col-12'">
        <div class="glass-card">
          <div class="flex justify-between items-center mb-4">
            <h3 v-if="activeTab === 'my-tickets'">My Support History</h3>
            <h3 v-else>Active IT Support Queue (All Corporate Tickets)</h3>
            <button class="btn btn-ghost btn-sm" @click="hrStore.fetchItTickets(authStore.user.id)">
              <IconHelper name="refresh-cw" size="14" /> Refresh
            </button>
          </div>

          <div class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>Raised Date</th>
                  <th>Title Summary</th>
                  <th>Category</th>
                  <th>Status</th>
                  <th>Assigned Specialist</th>
                  <th>Comments / Feedback</th>
                  <th v-if="activeTab === 'admin-desk'">Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="ticket in (activeTab === 'my-tickets' ? myTickets : allTickets)" :key="ticket.id">
                  <td class="font-mono text-xs">{{ new Date(ticket.createdAt).toLocaleDateString() }}</td>
                  <td>
                    <div class="font-semibold text-sm">{{ ticket.title }}</div>
                    <div class="text-muted text-xs text-truncate max-width-200">{{ ticket.description }}</div>
                  </td>
                  <td class="text-xs">{{ getCategoryLabel(ticket.category) }}</td>
                  <td>
                    <span class="badge text-xs" :class="getStatusBadgeClass(ticket.status)">
                      {{ ticket.status }}
                    </span>
                  </td>
                  <td class="text-xs italic">{{ ticket.assignedTo || 'Unassigned' }}</td>
                  <td class="text-xs text-muted max-width-200 text-truncate">{{ ticket.comments || 'No comments yet' }}</td>
                  <td v-if="activeTab === 'admin-desk'">
                    <button class="btn btn-primary btn-sm py-1 px-3" @click="openUpdateModal(ticket)">
                      <IconHelper name="settings" size="12" /> Update
                    </button>
                  </td>
                </tr>
                <tr v-if="(activeTab === 'my-tickets' ? myTickets : allTickets).length === 0">
                  <td colspan="7" class="text-center text-secondary p-6">No support tickets found in this queue.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- UPDATE MODAL (ADMIN CONTROL) -->
    <div class="modal-overlay" v-if="showUpdateModal && activeTicket">
      <div class="modal-content">
        <div class="modal-header">
          <h3>Manage Support Ticket</h3>
          <button class="btn btn-ghost btn-sm" @click="showUpdateModal = false">
            <IconHelper name="plus" style="transform: rotate(45deg);" size="14" />
          </button>
        </div>
        <div class="modal-body">
          <div class="mb-4">
            <div class="text-xs text-muted">Ticket Subject</div>
            <div class="font-semibold text-sm">{{ activeTicket.title }}</div>
          </div>
          <div class="mb-4">
            <div class="text-xs text-muted">User Description</div>
            <div class="text-sm border p-3 rounded bg-sidebar" style="white-space: pre-line">{{ activeTicket.description }}</div>
          </div>

          <div class="form-group mb-4">
            <label class="form-label">Lifecycle Status</label>
            <select v-model="adminStatus" class="form-control">
              <option value="PENDING">Pending Review</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="RESOLVED">Resolved</option>
              <option value="CLOSED">Closed</option>
            </select>
          </div>

          <div class="form-group mb-4">
            <label class="form-label">Assign Support Engineer</label>
            <input 
              type="text" 
              v-model="adminAssignedTo" 
              class="form-control" 
              placeholder="e.g. IT Helpdesk Specialist A"
            />
          </div>

          <div class="form-group mb-4">
            <label class="form-label">Comments / Engineer Notes</label>
            <textarea 
              v-model="adminComment" 
              class="form-control" 
              rows="3"
              placeholder="Provide solution summary or package dependencies..."
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showUpdateModal = false">Cancel</button>
          <button class="btn btn-primary" @click="handleUpdateTicket" :disabled="isLoading">
            Apply Updates
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.it-kiosk {
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
