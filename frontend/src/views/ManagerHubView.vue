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

const activeTab = ref(route.meta.tab || 'dashboard');

// Org chart search state
const orgSearch = ref('');
const expandedNodes = ref({
  'emp-103': true, // VP Vance
  'emp-102': true  // Mgr Jenkins
});

// Read only profile viewer
const showProfileModal = ref(false);
const activeProfile = ref(null);

onMounted(() => {
  activeTab.value = route.meta.tab || 'dashboard';
});

function changeTab(tab) {
  activeTab.value = tab;
  router.push(tab === 'dashboard' ? '/manager/dashboard' : tab === 'team' ? '/manager/team' : '/manager/orgchart');
}

// Manager's direct reports
const directReports = computed(() => {
  // Sarah Jenkins manages jane.doe (emp-101)
  // David Vance manages Sarah Jenkins (emp-102), Vikram (emp-104), Alice (emp-105)
  // Let's filter employees where managerId matches active user id
  return hrStore.employees.filter(e => e.managerId === authStore.user.id);
});

// Pending items count
const pendingLeavesCount = computed(() => {
  return hrStore.leaveRequests.filter(r => r.status === 'PENDING').length;
});

const pendingExpensesCount = computed(() => {
  return hrStore.expenseClaims.filter(c => c.status === 'PENDING').length;
});

// Employees out today
const outTodayList = computed(() => {
  return hrStore.leaveRequests.filter(r => {
    if (r.status !== 'APPROVED') return false;
    const start = new Date(r.fromDate);
    const end = new Date(r.toDate);
    const today = new Date('2026-06-09'); // Mock current local date
    return today >= start && today <= end;
  });
});

// Direct reports details details
const teamOverviewData = computed(() => {
  return directReports.value.map(emp => {
    // Leave remaining
    const balances = hrStore.leaveBalances[emp.employeeCode] || [];
    const annual = balances.find(b => b.type === 'Annual');
    const remainingLeave = annual ? annual.remaining : 0;
    
    // Open claims
    const claims = hrStore.expenseClaims.filter(c => c.employeeCode === emp.employeeCode && c.status === 'PENDING');
    
    return {
      ...emp,
      remainingLeave,
      openClaimsCount: claims.length
    };
  });
});

function openProfileInspector(emp) {
  activeProfile.value = emp;
  showProfileModal.value = true;
}

// Org chart hierarchy node expand
function toggleNode(nodeId) {
  expandedNodes.value[nodeId] = !expandedNodes.value[nodeId];
}

// Export org chart image
function exportOrgChart() {
  window.showPortalToast('Rendering high-resolution SVG canvas...', 'info');
  setTimeout(() => {
    window.showPortalToast('Org chart structure exported as PNG image', 'success');
  }, 1000);
}
</script>

<template>
  <div class="manager-hub">
    <!-- SUB-TABS -->
    <div class="tabs-container">
      <div class="tabs">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'dashboard' }"
          @click="changeTab('dashboard')"
        >
          Manager Dashboard
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'team' }"
          @click="changeTab('team')"
        >
          Team Overview
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'orgchart' }"
          @click="changeTab('orgchart')"
        >
          Organization Org Chart
        </button>
      </div>
    </div>

    <!-- TAB 1: MANAGER DASHBOARD -->
    <template v-if="activeTab === 'dashboard'">
      <!-- Exec indicators -->
      <div class="summary-grid">
        <div class="summary-card glass-card">
          <div class="summary-card-icon bg-primary-light text-primary">
            <IconHelper name="users" size="20" />
          </div>
          <div>
            <div class="summary-card-label">Direct Team Headcount</div>
            <div class="summary-card-value">{{ directReports.length }} <span class="text-xs font-normal">employees</span></div>
          </div>
        </div>

        <div class="summary-card glass-card clickable" @click="router.push('/manager/leave')">
          <div class="summary-card-icon bg-warning-light text-warning">
            <IconHelper name="calendar" size="20" />
          </div>
          <div>
            <div class="summary-card-label">Pending Leave Approvals</div>
            <div class="summary-card-value">{{ pendingLeavesCount }} <span class="text-xs font-normal">awaiting</span></div>
          </div>
        </div>

        <div class="summary-card glass-card clickable" @click="router.push('/manager/expense')">
          <div class="summary-card-icon bg-info-light text-info">
            <IconHelper name="credit-card" size="20" />
          </div>
          <div>
            <div class="summary-card-label">Pending Expense Approvals</div>
            <div class="summary-card-value">{{ pendingExpensesCount }} <span class="text-xs font-normal">awaiting</span></div>
          </div>
        </div>

        <div class="summary-card glass-card">
          <div class="summary-card-icon bg-success-light text-success">
            <IconHelper name="check-circle" size="20" />
          </div>
          <div>
            <div class="summary-card-label">Out of Office Today</div>
            <div class="summary-card-value">{{ outTodayList.length }} <span class="text-xs font-normal">absent</span></div>
          </div>
        </div>
      </div>

      <div class="grid-12">
        <!-- Quick actions -->
        <div class="col-6">
          <div class="glass-card h-full">
            <h3 class="mb-6">Management Action Center</h3>
            <div class="quick-action-list">
              <div class="action-item clickable" @click="router.push('/manager/leave')">
                <div class="action-icon bg-warning-light text-warning">
                  <IconHelper name="calendar" size="18" />
                </div>
                <div class="action-details">
                  <div class="action-title">Review Leave Requests</div>
                  <div class="action-desc">Approve or reject leave logs for direct reports</div>
                </div>
                <IconHelper name="plus" size="14" style="transform: rotate(45deg); opacity: 0.5" />
              </div>

              <div class="action-item clickable" @click="router.push('/manager/expense')">
                <div class="action-icon bg-info-light text-info">
                  <IconHelper name="credit-card" size="18" />
                </div>
                <div class="action-details">
                  <div class="action-title">Verify Expense Claims</div>
                  <div class="action-desc">Audit billing claims and receipt proofs</div>
                </div>
                <IconHelper name="plus" size="14" style="transform: rotate(45deg); opacity: 0.5" />
              </div>

              <div class="action-item clickable" @click="changeTab('orgchart')">
                <div class="action-icon bg-primary-light text-primary">
                  <IconHelper name="users" size="18" />
                </div>
                <div class="action-details">
                  <div class="action-title">Browse Org Structure</div>
                  <div class="action-desc">Inspect team hierarchy visualizer</div>
                </div>
                <IconHelper name="plus" size="14" style="transform: rotate(45deg); opacity: 0.5" />
              </div>
            </div>
          </div>
        </div>

        <!-- out today details -->
        <div class="col-6">
          <div class="glass-card h-full">
            <h3 class="mb-6">Absent Team Members Today</h3>
            <div class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>Staff Name</th>
                    <th>Leave Type</th>
                    <th>Duration Range</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="out in outTodayList" :key="out.id">
                    <td class="font-bold">{{ out.fullName }}</td>
                    <td><span class="badge badge-muted">{{ out.leaveType }}</span></td>
                    <td class="text-xs text-secondary">{{ out.fromDate }} to {{ out.toDate }}</td>
                  </tr>
                  <tr v-if="outTodayList.length === 0">
                    <td colspan="3" class="text-center text-secondary p-6">All staff members present today.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- TAB 2: TEAM OVERVIEW -->
    <template v-if="activeTab === 'team'">
      <div class="grid-12">
        <div v-for="team in teamOverviewData" :key="team.id" class="col-4">
          <div class="glass-card team-card">
            <div class="flex items-center gap-4 mb-4">
              <img :src="team.photo" class="team-avatar" />
              <div>
                <h3 class="text-base font-bold">{{ team.fullName }}</h3>
                <p class="text-xs text-secondary font-semibold">{{ team.designation }}</p>
                <p class="text-xs text-muted font-mono">{{ team.employeeCode }}</p>
              </div>
            </div>
            
            <div class="divider"></div>

            <div class="grid-12 m-0 mb-4 text-center">
              <div class="col-6 bg-surface p-2 rounded">
                <div class="text-xs text-muted">Leave Bal</div>
                <div class="font-bold font-mono text-success text-sm mt-1">{{ team.remainingLeave }} Days</div>
              </div>
              <div class="col-6 bg-surface p-2 rounded">
                <div class="text-xs text-muted">Pending claims</div>
                <div class="font-bold font-mono text-warning text-sm mt-1">{{ team.openClaimsCount }} Claims</div>
              </div>
            </div>

            <div class="flex justify-between items-center text-xs text-secondary mb-4">
              <span>Date Joined: {{ team.joiningDate }}</span>
              <span>Type: {{ team.employmentType }}</span>
            </div>

            <button class="btn btn-secondary btn-sm btn-full" @click="openProfileInspector(team)">
              <IconHelper name="user" size="12" />
              View Employee Profile
            </button>
          </div>
        </div>
      </div>
    </template>

    <!-- TAB 3: VISUAL ORG CHART -->
    <template v-if="activeTab === 'orgchart'">
      <div class="glass-card mb-6">
        <div class="flex justify-between items-center">
          <div class="form-group m-0 w-260">
            <input type="text" v-model="orgSearch" class="form-control" placeholder="Search node by name..." />
          </div>
          <button class="btn btn-secondary btn-sm" @click="exportOrgChart">
            <IconHelper name="download" size="14" />
            Export Hierarchy
          </button>
        </div>
      </div>

      <!-- VISUAL TREE (Mocked hierarchy layout) -->
      <div class="glass-card text-center overflow-x-auto">
        <div class="org-tree-wrapper" style="min-width: 600px;">
          
          <!-- ROOT NODE: VP Vance (emp-103) -->
          <div class="org-node-container">
            <div
              class="org-chart-node"
              :class="{ highlighted: orgSearch && 'David Vance'.toLowerCase().includes(orgSearch.toLowerCase()) }"
              @click="toggleNode('emp-103')"
            >
              <div class="font-bold text-sm">David Vance</div>
              <div class="text-xs text-primary font-semibold">VP of Engineering</div>
              <div class="text-xs text-muted">Executive</div>
              <div class="expand-indicator font-mono mt-1 text-xs">[{{ expandedNodes['emp-103'] ? '-' : '+' }}]</div>
            </div>

            <!-- LEVEL 2 SUB NODES (Sarah, Vikram, Alice) -->
            <div class="org-children-wrapper" v-if="expandedNodes['emp-103']">
              
              <!-- SUB NODE 1: Sarah Jenkins (emp-102) (Manager Node) -->
              <div class="org-branch">
                <div
                  class="org-chart-node"
                  :class="{ highlighted: orgSearch && 'Sarah Jenkins'.toLowerCase().includes(orgSearch.toLowerCase()) }"
                  @click="toggleNode('emp-102')"
                >
                  <div class="font-bold text-sm">Sarah Jenkins</div>
                  <div class="text-xs text-primary font-semibold">Engineering Manager</div>
                  <div class="text-xs text-muted">Engineering</div>
                  <div class="expand-indicator font-mono mt-1 text-xs">[{{ expandedNodes['emp-102'] ? '-' : '+' }}]</div>
                </div>

                <!-- LEVEL 3: Jane Doe (emp-101) (Leaf) -->
                <div class="org-children-wrapper" v-if="expandedNodes['emp-102']">
                  <div class="org-branch">
                    <div
                      class="org-chart-node leaf"
                      :class="{ highlighted: orgSearch && 'Jane Doe'.toLowerCase().includes(orgSearch.toLowerCase()) }"
                    >
                      <div class="font-bold text-sm">Jane Doe</div>
                      <div class="text-xs text-primary font-semibold">Senior Developer</div>
                      <div class="text-xs text-muted">Engineering</div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- SUB NODE 2: Vikram Mehta (emp-104) (Leaf) -->
              <div class="org-branch">
                <div
                  class="org-chart-node leaf"
                  :class="{ highlighted: orgSearch && 'Vikram Mehta'.toLowerCase().includes(orgSearch.toLowerCase()) }"
                >
                  <div class="font-bold text-sm">Vikram Mehta</div>
                  <div class="text-xs text-primary font-semibold">HR Lead</div>
                  <div class="text-xs text-muted">HR Dept</div>
                </div>
              </div>

              <!-- SUB NODE 3: Alice Sterling (emp-105) (Leaf) -->
              <div class="org-branch">
                <div
                  class="org-chart-node leaf"
                  :class="{ highlighted: orgSearch && 'Alice Sterling'.toLowerCase().includes(orgSearch.toLowerCase()) }"
                >
                  <div class="font-bold text-sm">Alice Sterling</div>
                  <div class="text-xs text-primary font-semibold">Finance Controller</div>
                  <div class="text-xs text-muted">Finance Dept</div>
                </div>
              </div>

            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- READ ONLY PROFILE INSPECTION MODAL -->
    <div class="modal-overlay" v-if="showProfileModal && activeProfile">
      <div class="modal-content" style="max-width: 450px;">
        <div class="modal-header">
          <h3>Employee Profile Summary</h3>
          <button class="btn btn-ghost btn-sm" @click="showProfileModal = false">
            <IconHelper name="plus" size="14" style="transform: rotate(45deg);" />
          </button>
        </div>
        <div class="modal-body text-center">
          <img :src="activeProfile.photo" class="profile-avatar-lg mb-4" />
          <h3 class="mb-1">{{ activeProfile.fullName }}</h3>
          <p class="text-secondary text-sm mb-4">{{ activeProfile.designation }} • {{ activeProfile.department }}</p>
          
          <div class="badge badge-success mb-6">{{ activeProfile.employmentType }}</div>
          
          <div class="divider"></div>
          
          <div class="text-left font-mono text-xs">
            <div class="flex justify-between mb-2 pb-1 border-b">
              <span class="text-secondary">Employee Code:</span>
              <span>{{ activeProfile.employeeCode }}</span>
            </div>
            <div class="flex justify-between mb-2 pb-1 border-b">
              <span class="text-secondary">Email:</span>
              <span>{{ activeProfile.email }}</span>
            </div>
            <div class="flex justify-between mb-2 pb-1 border-b">
              <span class="text-secondary">Phone:</span>
              <span>{{ activeProfile.phone || 'Not provided' }}</span>
            </div>
            <div class="flex justify-between mb-2 pb-1 border-b">
              <span class="text-secondary">Manager Name:</span>
              <span>{{ activeProfile.managerName }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-secondary">Joined Date:</span>
              <span>{{ activeProfile.joiningDate }}</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showProfileModal = false">Close Profile</button>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.manager-hub {
  animation: fadeIn 0.4s ease;
}

.divider {
  height: 1px;
  background-color: var(--border-color);
  margin: 16px 0;
}

.clickable { cursor: pointer; }

/* Quick Action List */
.quick-action-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.action-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  background-color: rgba(255, 255, 255, 0.02);
  border: 1px solid var(--border-color);
  transition: all var(--transition-fast);
}
.action-item:hover {
  border-color: rgba(255, 255, 255, 0.15);
  background-color: rgba(255, 255, 255, 0.04);
}
.action-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
}
.action-details {
  flex: 1;
}
.action-title {
  font-weight: 600;
  font-size: 13px;
}
.action-desc {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.team-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--border-color);
}
.profile-avatar-lg {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--border-color);
}

.w-260 { width: 260px; }
.bg-surface { background-color: rgba(15, 23, 42, 0.3); }
.rounded { border-radius: var(--radius-sm); }
.border-b { border-bottom: 1px solid var(--border-color); }

.font-normal { font-weight: 400; }
.font-semibold { font-weight: 600; }
.font-bold { font-weight: 700; }
.font-mono { font-family: monospace; }
.text-xs { font-size: 12px; }
.text-sm { font-size: 13px; }
.text-base { font-size: 15px; }
.text-lg { font-size: 18px; }

.flex { display: flex; }
.flex-col { display: flex; flex-direction: column; }
.flex-1 { flex: 1; }
.items-center { align-items: center; }
.justify-between { justify-content: space-between; }
.gap-2 { gap: 8px; }
.gap-4 { gap: 16px; }

.mb-1 { margin-bottom: 4px; }
.mb-2 { margin-bottom: 8px; }
.mb-4 { margin-bottom: 16px; }
.mb-6 { margin-bottom: 24px; }
.mt-1 { margin-top: 4px; }

.p-2 { padding: 8px; }
.p-6 { padding: 24px; }
.text-center { text-align: center; }
.text-left { text-align: left; }

/* Org chart styling tree layout */
.org-tree-wrapper {
  padding: 30px 10px;
  display: flex;
  justify-content: center;
}
.org-node-container {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.org-children-wrapper {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  position: relative;
  padding-top: 20px;
  margin-top: 10px;
}
.org-children-wrapper::before {
  content: '';
  position: absolute;
  top: 0;
  left: 50%;
  width: 2px;
  height: 20px;
  background-color: var(--border-color);
}
.org-branch {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 10px;
  position: relative;
}
.org-chart-node.highlighted {
  border-color: var(--border-focus);
  box-shadow: 0 0 10px rgba(99, 102, 241, 0.4);
}
.expand-indicator {
  cursor: pointer;
}
</style>
