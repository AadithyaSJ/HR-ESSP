<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/authStore.js';
import { useHrStore } from '../stores/hrStore.js';
import IconHelper from '../components/IconHelper.vue';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const hrStore = useHrStore();

const activeTab = ref(route.meta.tab || 'directory');

// Search & Filtering Directory
const searchQuery = ref('');
const filterDept = ref('All');
const filterType = ref('All');
const filterStatus = ref('All');

// Add New Employee Form State
const newEmpCode = ref('');
const newEmpName = ref('');
const newEmpEmail = ref('');
const newEmpDept = ref('Engineering');
const newEmpDesig = ref('Software Engineer');
const newEmpManager = ref('');
const newEmpJoinDate = ref(new Date().toISOString().split('T')[0]);
const newEmpType = ref('Full-time');
const newEmpRole = ref('EMPLOYEE');
const newEmpSalary = ref(800000);
const newEmpSalaryBand = ref('Band 1 (L1-L2)');

// Edit Employee Profile State (for inspector view)
const selectedEmp = ref(null);
const inspectorDept = ref('');
const inspectorDesig = ref('');
const inspectorManager = ref('');
const inspectorType = ref('Full-time');
const inspectorStatus = ref('ACTIVE');
const inspectorSalary = ref(0);
const inspectorSalaryBand = ref('');

// Watch route metadata changes
onMounted(() => {
  activeTab.value = route.meta.tab || 'directory';
  checkRouteParams();
  hrStore.fetchMandatoryDocuments();
  hrStore.employees.forEach(emp => {
    hrStore.fetchEmployeeDocuments(emp.id);
  });
});

watch(() => route.params.id, () => {
  checkRouteParams();
});

function checkRouteParams() {
  if (route.params.id) {
    const emp = hrStore.employees.find(e => e.id === route.params.id);
    if (emp) {
      openInspector(emp);
    }
  }
}

function changeTab(tab) {
  activeTab.value = tab;
  router.push(tab === 'directory' ? '/hr/employees' : tab === 'new-employee' ? '/hr/employees/new' : '/hr/documents');
}

// Filtered employee list
const filteredEmployees = computed(() => {
  return hrStore.employees.filter(emp => {
    const matchSearch = emp.fullName.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
                        emp.employeeCode.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
                        emp.designation.toLowerCase().includes(searchQuery.value.toLowerCase());
                        
    const matchDept = filterDept.value === 'All' || emp.department === filterDept.value;
    const matchType = filterType.value === 'All' || emp.employmentType === filterType.value;
    const matchStatus = filterStatus.value === 'All' || emp.status === filterStatus.value;
    
    return matchSearch && matchDept && matchType && matchStatus;
  });
});

// Departments list for filters
const departments = computed(() => {
  const depts = hrStore.employees.map(e => e.department);
  return ['All', ...new Set(depts)];
});

// Manager listings
const managers = computed(() => {
  return hrStore.employees.filter(e => e.role === 'MANAGER');
});

// Export directory to CSV
function exportToCSV() {
  let csvContent = 'data:text/csv;charset=utf-8,';
  csvContent += 'Employee Code,Full Name,Email,Department,Designation,Joining Date,Employment Type,Status\r\n';
  
  filteredEmployees.value.forEach(e => {
    csvContent += `"${e.employeeCode}","${e.fullName}","${e.email}","${e.department}","${e.designation}","${e.joiningDate}","${e.employmentType}","${e.status}"\r\n`;
  });
  
  const encodedUri = encodeURI(csvContent);
  const link = document.createElement('a');
  link.setAttribute('href', encodedUri);
  link.setAttribute('download', `employees_export_${new Date().toISOString().split('T')[0]}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  
  hrStore.addLog(authStore.user.email, 'Employee', 'EXPORT_CSV', 'Exported employee directory to CSV');
  window.showPortalToast('Employee directory exported successfully', 'success');
}

// Save Employee Form
async function handleCreateEmployee() {
  if (!newEmpCode.value || !newEmpName.value || !newEmpEmail.value || !newEmpJoinDate.value) {
    window.showPortalToast('Please fill all required fields', 'error');
    return;
  }
  
  // Find manager name from ID
  const mgr = hrStore.employees.find(e => e.id === newEmpManager.value);
  
  const data = {
    employeeCode: newEmpCode.value,
    fullName: newEmpName.value,
    email: newEmpEmail.value,
    department: newEmpDept.value,
    designation: newEmpDesig.value,
    managerId: newEmpManager.value || null,
    managerName: mgr ? mgr.fullName : null,
    joiningDate: newEmpJoinDate.value,
    employmentType: newEmpType.value,
    role: newEmpRole.value,
    salary: newEmpSalary.value,
    salaryBand: newEmpSalaryBand.value
  };

  const added = await hrStore.addNewEmployee(data, authStore.user.email);
  if (added) {
    window.showPortalToast('Employee created and onboarding checklist triggered', 'success');
    
    // Show welcome link in an alert dialog so it can be copied for local testing
    alert(`Employee created successfully!\n\nHere is the Welcome Email Link dispatched to the employee:\n\n${added.welcomeLink}\n\nCopy this link and open it in a new window/private tab to set their initial password and complete onboarding details.`);
    
    // Clear form
    newEmpCode.value = '';
    newEmpName.value = '';
    newEmpEmail.value = '';
    newEmpManager.value = '';
    
    // Switch to directory
    changeTab('directory');
  }
}

async function handleApproveOnboarding(empId) {
  const success = await hrStore.approveOnboarding(empId);
  if (success) {
    window.showPortalToast('Employee onboarding approved successfully. Credentials activated.', 'success');
    // Refresh selected employee reference to update view
    selectedEmp.value = hrStore.employees.find(e => e.id === empId);
  } else {
    window.showPortalToast('Failed to approve employee onboarding.', 'error');
  }
}

// Inspector View (Edit Employee / HR details)
function openInspector(emp) {
  selectedEmp.value = emp;
  inspectorDept.value = emp.department;
  inspectorDesig.value = emp.designation;
  inspectorManager.value = emp.managerId || '';
  inspectorType.value = emp.employmentType;
  inspectorStatus.value = emp.status;
  inspectorSalary.value = emp.salary;
  inspectorSalaryBand.value = emp.salaryBand;
  
  // Update route path manually if click direct
  if (route.params.id !== emp.id) {
    router.push(`/hr/employees/${emp.id}`);
  }
  activeTab.value = 'inspector';
}

function handleSaveInspector() {
  const data = {
    department: inspectorDept.value,
    designation: inspectorDesig.value,
    managerId: inspectorManager.value || null,
    employmentType: inspectorType.value,
    status: inspectorStatus.value,
    salary: inspectorSalary.value,
    salaryBand: inspectorSalaryBand.value
  };
  
  const success = hrStore.updateEmployeeHrView(selectedEmp.value.id, data, authStore.user.email);
  if (success) {
    window.showPortalToast('Employee configuration updated successfully', 'success');
    router.push('/hr/employees');
    activeTab.value = 'directory';
    selectedEmp.value = null;
  }
}

function toggleInspectorOnboardingTask(taskId) {
  hrStore.toggleOnboardingTask(selectedEmp.value.id, taskId, authStore.user.email);
  window.showPortalToast('Checked task for employee onboarding', 'info');
}

// Global Document Logs Listing & Operations
import { API_BASE_URL } from '../config.js';

const newMandatoryName = ref('');

async function handleAddMandatoryDocument() {
  if (!newMandatoryName.value || !newMandatoryName.value.trim()) {
    window.showPortalToast('Please enter a document name.', 'error');
    return;
  }
  const success = await hrStore.addMandatoryDocument(newMandatoryName.value.trim(), authStore.user.email);
  if (success) {
    window.showPortalToast(`Required document type "${newMandatoryName.value}" added successfully.`, 'success');
    newMandatoryName.value = '';
  } else {
    window.showPortalToast('Failed to add mandatory document.', 'error');
  }
}

function downloadEmployeeDoc(empId, doc) {
  const token = localStorage.getItem('jwt_token');
  const url = `${API_BASE_URL}/api/v1/employees/${empId}/documents/${doc.id}/download`;
  
  window.showPortalToast(`Downloading ${doc.name}...`, 'info');
  fetch(url, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  })
  .then(response => {
    if (!response.ok) throw new Error('Download failed');
    return response.blob();
  })
  .then(blob => {
    const downloadUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.setAttribute('download', doc.name);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(downloadUrl);
  })
  .catch(err => {
    console.error(err);
    window.showPortalToast('Failed to download document.', 'error');
  });
}

const allDocuments = computed(() => {
  const docs = [];
  hrStore.employees.forEach(emp => {
    if (emp.documents) {
      emp.documents.forEach(doc => {
        docs.push({
          ...doc,
          employeeId: emp.id,
          employeeName: emp.fullName,
          employeeCode: emp.employeeCode
        });
      });
    }
  });
  return docs;
});
</script>

<template>
  <div class="directory-view">
    <!-- SUB-TABS -->
    <div class="tabs-container">
      <div class="tabs">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'directory' }"
          @click="changeTab('directory')"
        >
          Employee Directory
        </button>
        <button
          v-if="authStore.activeRole === 'HR_ADMIN'"
          class="tab-btn"
          :class="{ active: activeTab === 'new-employee' }"
          @click="changeTab('new-employee')"
        >
          Add New Employee
        </button>
        <button
          v-if="activeTab === 'inspector'"
          class="tab-btn active"
        >
          Inspect Profile: {{ selectedEmp?.fullName }}
        </button>
        <button
          v-if="authStore.activeRole === 'HR_ADMIN'"
          class="tab-btn"
          :class="{ active: activeTab === 'documents' }"
          @click="changeTab('documents')"
        >
          Organization Documents
        </button>
      </div>
    </div>

    <!-- TAB 1: EMPLOYEE DIRECTORY -->
    <template v-if="activeTab === 'directory'">
      <!-- FILTERS -->
      <div class="glass-card mb-6">
        <div class="grid-12">
          <div class="col-4 form-group m-0">
            <label class="form-label">Search Directory</label>
            <input type="text" v-model="searchQuery" class="form-control" placeholder="Search by name, ID or title..." />
          </div>
          
          <div class="col-3 form-group m-0">
            <label class="form-label">Department</label>
            <select v-model="filterDept" class="form-control">
              <option v-for="dept in departments" :key="dept" :value="dept">{{ dept }}</option>
            </select>
          </div>

          <div class="col-2 form-group m-0">
            <label class="form-label">Employment Type</label>
            <select v-model="filterType" class="form-control">
              <option value="All">All Types</option>
              <option value="Full-time">Full-time</option>
              <option value="Contract">Contract</option>
            </select>
          </div>

          <div class="col-2 form-group m-0">
            <label class="form-label">Account Status</label>
            <select v-model="filterStatus" class="form-control">
              <option value="All">All Statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </select>
          </div>

          <div class="col-1 flex items-end">
            <button class="btn btn-secondary btn-sm btn-full py-10" @click="exportToCSV" title="Export current list to CSV file">
              <IconHelper name="download" size="14" />
              CSV
            </button>
          </div>
        </div>
      </div>

      <!-- DIRECTORY GRID -->
      <div class="grid-12">
        <div
          v-for="emp in filteredEmployees"
          :key="emp.id"
          class="col-4"
        >
          <div class="glass-card flex gap-4 employee-summary-card relative">
            <img :src="emp.photo" alt="Avatar" class="directory-avatar" />
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between mb-1">
                <span class="badge badge-muted font-mono text-xs">{{ emp.employeeCode }}</span>
                <span v-if="emp.onboardingStatus === 'PENDING_DETAILS'" class="badge badge-info">PENDING DETAILS</span>
                <span v-else-if="emp.onboardingStatus === 'PENDING_APPROVAL'" class="badge badge-warning">PENDING APPROVAL</span>
                <span v-else class="badge" :class="emp.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'">{{ emp.status }}</span>
              </div>
              <h3 class="text-base font-bold text-truncate">{{ emp.fullName }}</h3>
              <p class="text-secondary text-xs font-semibold text-truncate mb-2">{{ emp.designation }}</p>
              <p class="text-muted text-xs mb-3">{{ emp.department }}</p>
              
              <div class="flex justify-between items-center pt-2 border-t">
                <span class="text-xs text-muted">Joined: {{ emp.joiningDate }}</span>
                <button class="btn btn-ghost btn-sm px-2 py-1" @click="openInspector(emp)">
                  <IconHelper name="eye" size="14" />
                  Configure
                </button>
              </div>
            </div>
          </div>
        </div>
        <div v-if="filteredEmployees.length === 0" class="col-12 glass-card text-center p-8 text-secondary">
          No employees matching filters found.
        </div>
      </div>
    </template>

    <!-- TAB 2: ADD NEW EMPLOYEE -->
    <template v-if="activeTab === 'new-employee' && authStore.activeRole === 'HR_ADMIN'">
      <div class="glass-card max-width-800">
        <h3 class="mb-6">Onboard New Employee Profile</h3>
        <form @submit.prevent="handleCreateEmployee">
          <div class="grid-12">
            <div class="col-4 form-group">
              <label class="form-label">Employee Code *</label>
              <input type="text" v-model="newEmpCode" class="form-control" placeholder="EMP2026xxx" required />
            </div>
            
            <div class="col-8 form-group">
              <label class="form-label">Full Name *</label>
              <input type="text" v-model="newEmpName" class="form-control" placeholder="Johnathan Doe" required />
            </div>

            <div class="col-6 form-group">
              <label class="form-label">Company Email *</label>
              <input type="email" v-model="newEmpEmail" class="form-control" placeholder="john.d@company.com" required />
            </div>

            <div class="col-6 form-group">
              <label class="form-label">Joining Date *</label>
              <input type="date" v-model="newEmpJoinDate" class="form-control" required />
            </div>

            <div class="col-4 form-group">
              <label class="form-label">Department</label>
              <select v-model="newEmpDept" class="form-control">
                <option value="Engineering">Engineering</option>
                <option value="HR">HR</option>
                <option value="Finance">Finance</option>
                <option value="Marketing">Marketing</option>
                <option value="Executive">Executive</option>
              </select>
            </div>

            <div class="col-4 form-group">
              <label class="form-label">Designation</label>
              <input type="text" v-model="newEmpDesig" class="form-control" placeholder="Software Engineer" />
            </div>

            <div class="col-4 form-group">
              <label class="form-label">Reporting Manager</label>
              <select v-model="newEmpManager" class="form-control">
                <option value="">None (Top Node)</option>
                <option v-for="mgr in managers" :key="mgr.id" :value="mgr.id">{{ mgr.fullName }}</option>
              </select>
            </div>

            <div class="col-4 form-group">
              <label class="form-label">Employment Type</label>
              <select v-model="newEmpType" class="form-control">
                <option value="Full-time">Full-time</option>
                <option value="Contract">Contract</option>
              </select>
            </div>

            <div class="col-4 form-group">
              <label class="form-label">Initial Role Permission</label>
              <select v-model="newEmpRole" class="form-control">
                <option value="EMPLOYEE">Employee</option>
                <option value="MANAGER">Manager / Team Lead</option>
                <option value="HR_ADMIN">HR Administrator</option>
                <option value="FINANCE_ADMIN">Finance Admin</option>
                <option value="SYSTEM_ADMIN">System Admin</option>
              </select>
            </div>

            <div class="col-4 form-group">
              <label class="form-label">Salary Band & Annual Comp</label>
              <div class="flex gap-2">
                <select v-model="newEmpSalaryBand" class="form-control flex-1">
                  <option value="Band 1 (L1-L2)">Band 1 (L1-L2)</option>
                  <option value="Band 2 (L2-L3)">Band 2 (L2-L3)</option>
                  <option value="Band 3 (L3-L4)">Band 3 (L3-L4)</option>
                  <option value="Band 4 (L5-L6)">Band 4 (L5-L6)</option>
                  <option value="Band 5 (L7+)">Band 5 (L7+)</option>
                </select>
                <input type="number" v-model="newEmpSalary" class="form-control w-120" placeholder="Salary" />
              </div>
            </div>
          </div>

          <p class="text-xs text-muted mb-6">
            💡 Saving this profile triggers an automated Onboarding Checklist assignment and dispatches a welcome email via AWS SES.
          </p>

          <div class="flex justify-end gap-3">
            <button type="button" class="btn btn-secondary" @click="changeTab('directory')">Cancel</button>
            <button type="submit" class="btn btn-primary">Save Employee Profile</button>
          </div>
        </form>
      </div>
    </template>

    <!-- TAB 3: HR PROFILE INSPECTOR -->
    <template v-if="activeTab === 'inspector'">
      <div class="grid-12" v-if="selectedEmp">
        <!-- left configuration -->
        <div class="col-7">
          <div class="glass-card">
            <div class="flex items-center gap-4 mb-6">
              <img :src="selectedEmp.photo" class="profile-avatar-lg" />
              <div>
                <h2>{{ selectedEmp.fullName }}</h2>
                <p class="text-secondary text-sm font-mono">{{ selectedEmp.employeeCode }} • Joined {{ selectedEmp.joiningDate }}</p>
              </div>
            </div>
            
            <form @submit.prevent="handleSaveInspector">
              <div class="grid-12">
                <!-- Onboarding verification review -->
                <div v-if="selectedEmp.onboardingStatus === 'PENDING_APPROVAL' || selectedEmp.onboardingStatus === 'PENDING_DETAILS'" class="col-12 onboarding-review-panel mb-6 p-4 rounded" style="background: rgba(245, 158, 11, 0.05); border: 1px solid rgba(245, 158, 11, 0.2); width: 100%;">
                  <h3 class="text-sm font-bold mb-3 flex items-center gap-2" style="color: #f59e0b; margin-top: 0;">
                    <IconHelper name="clock" size="16" />
                    Onboarding Verification Review (Status: {{ selectedEmp.onboardingStatus }})
                  </h3>
                  
                  <div class="grid-12 gap-3 mb-4">
                    <div class="col-6 mb-2" style="padding-left: 0;">
                      <span class="text-xs text-muted block" style="font-size: 11px; display: block; margin-bottom: 2px;">School Background</span>
                      <p class="text-xs font-semibold m-0" style="font-size: 12px; font-weight: 600;">{{ selectedEmp.school || 'Not entered yet' }}</p>
                    </div>
                    <div class="col-6 mb-2" style="padding-left: 0;">
                      <span class="text-xs text-muted block" style="font-size: 11px; display: block; margin-bottom: 2px;">College / University</span>
                      <p class="text-xs font-semibold m-0" style="font-size: 12px; font-weight: 600;">{{ selectedEmp.college || 'Not entered yet' }}</p>
                    </div>
                    <div class="col-12" style="padding-left: 0;">
                      <span class="text-xs text-muted block" style="font-size: 11px; display: block; margin-bottom: 2px;">Previous Work Experience</span>
                      <p class="text-xs font-semibold m-0" style="font-size: 12px; font-weight: 600; white-space: pre-wrap;">{{ selectedEmp.experience || 'Not entered yet' }}</p>
                    </div>
                  </div>

                  <div class="flex justify-end gap-3 mt-4 pt-4" style="border-top: 1px solid rgba(255,255,255,0.05);" v-if="selectedEmp.onboardingStatus === 'PENDING_APPROVAL'">
                    <button type="button" class="btn btn-primary btn-sm" @click="handleApproveOnboarding(selectedEmp.id)">
                      Approve Onboarding Profile
                    </button>
                  </div>
                </div>

                <h4 class="col-12 section-header">Employment Parameters</h4>
                <div class="col-6 form-group">
                  <label class="form-label">Department</label>
                  <select v-model="inspectorDept" class="form-control">
                    <option value="Engineering">Engineering</option>
                    <option value="HR">HR</option>
                    <option value="Finance">Finance</option>
                    <option value="Marketing">Marketing</option>
                    <option value="Executive">Executive</option>
                  </select>
                </div>
                <div class="col-6 form-group">
                  <label class="form-label">Designation</label>
                  <input type="text" v-model="inspectorDesig" class="form-control" />
                </div>
                <div class="col-6 form-group">
                  <label class="form-label">Reporting Manager</label>
                  <select v-model="inspectorManager" class="form-control">
                    <option value="">None (Top Node)</option>
                    <option v-for="mgr in managers" :key="mgr.id" :value="mgr.id">{{ mgr.fullName }}</option>
                  </select>
                </div>
                <div class="col-6 form-group">
                  <label class="form-label">Employment Type</label>
                  <select v-model="inspectorType" class="form-control">
                    <option value="Full-time">Full-time</option>
                    <option value="Contract">Contract</option>
                  </select>
                </div>

                <h4 class="col-12 section-header">Compensation & Security</h4>
                <div class="col-6 form-group">
                  <label class="form-label">Salary Band</label>
                  <select v-model="inspectorSalaryBand" class="form-control">
                    <option value="Band 1 (L1-L2)">Band 1 (L1-L2)</option>
                    <option value="Band 2 (L2-L3)">Band 2 (L2-L3)</option>
                    <option value="Band 3 (L3-L4)">Band 3 (L3-L4)</option>
                    <option value="Band 4 (L5-L6)">Band 4 (L5-L6)</option>
                    <option value="Band 5 (L7+)">Band 5 (L7+)</option>
                  </select>
                </div>
                <div class="col-6 form-group">
                  <label class="form-label">Annual Salary (INR)</label>
                  <input type="number" v-model="inspectorSalary" class="form-control" />
                </div>
                
                <div class="col-6 form-group">
                  <label class="form-label">Account Status</label>
                  <select v-model="inspectorStatus" class="form-control">
                    <option value="ACTIVE">Active Account</option>
                    <option value="INACTIVE">Deactivated Account</option>
                  </select>
                </div>
              </div>

              <div class="flex justify-between items-center mt-6 pt-4 border-t">
                <button type="button" class="btn btn-danger btn-sm" @click="inspectorStatus = 'INACTIVE'; handleSaveInspector()" v-if="selectedEmp.status === 'ACTIVE'">
                  Deactivate Employee
                </button>
                <span v-else></span>
                
                <div class="flex gap-3">
                  <button type="button" class="btn btn-secondary" @click="changeTab('directory')">Back</button>
                  <button type="submit" class="btn btn-primary">Save Configuration</button>
                </div>
              </div>
            </form>
          </div>
        </div>

        <!-- right checks & document history logs -->
        <div class="col-5">
          <!-- Onboarding status -->
          <div class="glass-card mb-6" v-if="selectedEmp.onboardingTasks && selectedEmp.onboardingTasks.length">
            <h3 class="mb-4">Onboarding Completion ({{ selectedEmp.onboardingPercent }}%)</h3>
            <div class="checklist-items">
              <div
                v-for="task in selectedEmp.onboardingTasks"
                :key="task.id"
                class="checklist-item py-2"
                :class="{ checked: task.done }"
              >
                <div class="checklist-item-left">
                  <div class="checkbox-custom" @click="toggleInspectorOnboardingTask(task.id)">
                    <IconHelper v-if="task.done" name="check" size="14" />
                  </div>
                  <span class="text-xs">{{ task.title }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Uploaded Documents log -->
          <div class="glass-card">
            <h3 class="mb-4">Uploaded Files Log</h3>
            <div class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>Filename</th>
                    <th>Type</th>
                    <th>Size</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="doc in selectedEmp.documents" :key="doc.name">
                    <td class="text-xs font-semibold">{{ doc.name }}</td>
                    <td><span class="badge badge-muted">{{ doc.type }}</span></td>
                    <td class="text-xs text-secondary">{{ doc.size }}</td>
                  </tr>
                  <tr v-if="selectedEmp.documents.length === 0">
                    <td colspan="3" class="text-center text-xs text-secondary">No documents uploaded.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- TAB 4: GLOBAL DOCUMENTS LIST -->
    <template v-if="activeTab === 'documents' && authStore.activeRole === 'HR_ADMIN'">
      <div class="grid-12">
        <!-- Left: Manage Mandatory Document Requirements -->
        <div class="col-4">
          <div class="glass-card mb-6">
            <h3 class="mb-4">Add Required Document</h3>
            <div class="form-group">
              <label class="form-label">Required Document Name</label>
              <input type="text" v-model="newMandatoryName" class="form-control" placeholder="e.g. Passport, Tax Declaration Form" />
            </div>
            <button class="btn btn-primary btn-sm btn-full mt-2" @click="handleAddMandatoryDocument">
              <IconHelper name="plus" size="14" /> Add Mandatory Document
            </button>
          </div>

          <div class="glass-card">
            <h3 class="mb-4">Mandatory Document Types</h3>
            <div class="mandatory-docs-list-hr">
              <div v-for="mDoc in hrStore.mandatoryDocuments" :key="mDoc.id" class="p-3 mb-2 rounded border" style="background: rgba(255, 255, 255, 0.01); border-color: var(--border-color)">
                <div class="flex items-center justify-between">
                  <span class="font-semibold text-sm">{{ mDoc.name }}</span>
                  <span class="text-xs text-secondary">Required</span>
                </div>
              </div>
              <div v-if="hrStore.mandatoryDocuments.length === 0" class="text-center text-secondary text-xs p-4">
                No mandatory documents defined yet.
              </div>
            </div>
          </div>
        </div>

        <!-- Right: Global Document Logs -->
        <div class="col-8">
          <div class="glass-card">
            <h3 class="mb-6">Global Document Logs</h3>
            <div class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>Employee</th>
                    <th>File Name</th>
                    <th>Category</th>
                    <th>Date Uploaded</th>
                    <th class="text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(doc, idx) in allDocuments" :key="idx">
                    <td>
                      <div class="flex flex-col">
                        <span class="font-bold text-sm">{{ doc.employeeName }}</span>
                        <span class="font-mono text-xs text-secondary">{{ doc.employeeCode }}</span>
                      </div>
                    </td>
                    <td>
                      <div class="flex items-center gap-2">
                        <IconHelper name="file-text" size="16" color="#94a3b8" />
                        <span class="text-truncate" style="max-width: 150px;" :title="doc.name">{{ doc.name }}</span>
                      </div>
                    </td>
                    <td><span class="badge badge-muted text-xs">{{ doc.type }}</span></td>
                    <td>{{ doc.date }}</td>
                    <td class="text-right">
                      <button class="btn btn-ghost btn-sm" @click="downloadEmployeeDoc(doc.employeeId, doc)" title="Download file">
                        <IconHelper name="download" size="14" />
                      </button>
                    </td>
                  </tr>
                  <tr v-if="allDocuments.length === 0">
                    <td colspan="5" class="text-center text-secondary">No documents logged in database.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.directory-view {
  animation: fadeIn 0.4s ease;
}

.max-width-800 {
  max-width: 800px;
  margin: 0 auto;
}

.employee-summary-card {
  align-items: flex-start;
  transition: transform var(--transition-normal);
}
.employee-summary-card:hover {
  transform: translateY(-2px);
}
.directory-avatar {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  border: 1px solid var(--border-color);
}

.profile-avatar-lg {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--border-color);
}

.w-120 { width: 120px; }
.section-header {
  font-size: 13px;
  font-weight: 700;
  color: var(--primary-color);
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 6px;
  margin-top: 16px;
  margin-bottom: 12px;
}

.text-base { font-size: 16px; }
.text-xs { font-size: 12px; }
.text-sm { font-size: 13px; }
.font-mono { font-family: monospace; }
.font-semibold { font-weight: 600; }
.font-bold { font-weight: 700; }
.text-truncate {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.flex { display: flex; }
.flex-col { display: flex; flex-direction: column; }
.flex-1 { flex: 1; }
.items-center { align-items: center; }
.items-end { align-items: flex-end; }
.justify-between { justify-content: space-between; }
.justify-end { justify-content: flex-end; }
.gap-2 { gap: 8px; }
.gap-3 { gap: 12px; }
.gap-4 { gap: 16px; }
.mb-1 { margin-bottom: 4px; }
.mb-2 { margin-bottom: 8px; }
.mb-3 { margin-bottom: 12px; }
.mb-4 { margin-bottom: 16px; }
.mb-6 { margin-bottom: 24px; }
.mt-6 { margin-top: 24px; }
.pt-2 { padding-top: 8px; }
.pt-4 { padding-top: 16px; }
.py-2 { padding-top: 8px; padding-bottom: 8px; }
.py-10 { padding-top: 10px; padding-bottom: 10px; }
.border-t { border-top: 1px solid var(--border-color); }
.text-right { text-align: right; }
.p-8 { padding: 32px; }
</style>
