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
const isLoading = ref(false);

// Search & Filtering Directory
const searchQuery = ref('');
const filterDept = ref('All Departments');
const filterStatus = ref('All Status');

// Add New Employee Form State
const newEmpName = ref('');
const newEmpEmail = ref('');
const newEmpPhone = ref('');
const newEmpDob = ref('');
const newEmpAddress = ref('');
const newEmpEmergency = ref('');
const newEmpDept = ref('');
const newEmpDesig = ref('');
const newEmpWorkLocation = ref('');
const newEmpType = ref('Full-time');
const newEmpRole = ref('EMPLOYEE');
const newEmpSalary = ref(800000);
const newEmpSalaryBand = ref('Band 1 (L1-L2)');

// Inspector (Edit Employee) state
const selectedEmp = ref(null);
const inspectorDept = ref('');
const inspectorDesig = ref('');
const inspectorManager = ref('');
const inspectorType = ref('Full-time');
const inspectorStatus = ref('ACTIVE');
const inspectorSalary = ref(0);
const inspectorSalaryBand = ref('');

// Actions dropdown tracking
const activeDropdownId = ref(null);

function toggleDropdown(empId, event) {
  event.stopPropagation();
  if (activeDropdownId.value === empId) {
    activeDropdownId.value = null;
  } else {
    activeDropdownId.value = empId;
  }
}

// Watch route metadata changes
watch(() => route.meta.tab, (newTab) => {
  if (newTab === 'edit-employee') {
    activeTab.value = 'inspector';
  } else {
    activeTab.value = newTab || 'directory';
  }
  if (activeTab.value !== 'inspector') {
    selectedEmp.value = null;
  }
}, { immediate: true });

onMounted(async () => {
  checkRouteParams();
  isLoading.value = true;
  try {
    await hrStore.fetchEmployees();
    await hrStore.fetchMandatoryDocuments();
    for (const emp of hrStore.employees) {
      await hrStore.fetchEmployeeDocuments(emp.id);
    }
    await new Promise(r => setTimeout(r, 500));
  } finally {
    isLoading.value = false;
  }
  
  // Close dropdowns on document click
  window.addEventListener('click', () => {
    activeDropdownId.value = null;
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
  if (tab === 'directory') router.push('/hr/employees');
  else if (tab === 'new-employee') router.push('/hr/employees/new');
  else if (tab === 'onboarding') router.push('/hr/employees/onboarding');
  else if (tab === 'documents') router.push('/hr/documents');
}

// Filtered employee list
const filteredEmployees = computed(() => {
  return hrStore.employees.filter(emp => {
    const query = searchQuery.value.toLowerCase().trim();
    const matchSearch = !query || 
                        emp.fullName.toLowerCase().includes(query) ||
                        emp.employeeCode.toLowerCase().includes(query) ||
                        emp.email.toLowerCase().includes(query) ||
                        emp.designation.toLowerCase().includes(query);
                        
    const matchDept = filterDept.value === 'All Departments' || emp.department === filterDept.value;
    
    let matchStatus = true;
    if (filterStatus.value !== 'All Status') {
      if (filterStatus.value === 'ACTIVE') {
        matchStatus = emp.status === 'ACTIVE';
      } else if (filterStatus.value === 'ON_LEAVE') {
        matchStatus = emp.status === 'ON_LEAVE';
      } else if (filterStatus.value === 'INACTIVE') {
        matchStatus = emp.status === 'INACTIVE';
      }
    }
    
    return matchSearch && matchDept && matchStatus;
  });
});

// Departments list for filters
const departments = computed(() => {
  const depts = hrStore.employees.map(e => e.department).filter(Boolean);
  return ['All Departments', ...new Set(depts)];
});

// Manager listings
const managers = computed(() => {
  return hrStore.employees.filter(e => e.role === 'MANAGER' || e.role === 'HR_ADMIN');
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

// Generate next sequential employee ID code (EMP001, EMP002, etc.)
function generateAutoEmployeeCode() {
  const nextNum = hrStore.employees.length + 1;
  return `EMP${String(nextNum).padStart(3, '0')}`;
}

// Save Employee Form
async function handleCreateEmployee() {
  if (!newEmpName.value || !newEmpEmail.value || !newEmpPhone.value || !newEmpJoinDate.value || !newEmpDept.value || !newEmpDesig.value || !newEmpWorkLocation.value) {
    window.showPortalToast('Please fill all required fields', 'error');
    return;
  }
  
  const parts = newEmpEmergency.value.split(';');
  const emergencyName = parts[0]?.trim() || '';
  const emergencyPhone = parts[1]?.trim() || '';

  // Auto-generate employee code behind the scenes
  const autoCode = generateAutoEmployeeCode();
  
  const data = {
    employeeCode: autoCode,
    fullName: newEmpName.value,
    email: newEmpEmail.value,
    phone: newEmpPhone.value,
    address: newEmpAddress.value,
    department: newEmpDept.value,
    designation: newEmpDesig.value,
    managerId: null,
    managerName: null,
    joiningDate: newEmpJoinDate.value,
    employmentType: newEmpType.value,
    role: newEmpRole.value,
    salary: newEmpSalary.value || 800000,
    salaryBand: newEmpSalaryBand.value || 'Band 1 (L1-L2)',
    emergencyName: emergencyName,
    emergencyPhone: emergencyPhone,
    dob: newEmpDob.value,
    workLocation: newEmpWorkLocation.value
  };

  isLoading.value = true;
  try {
    const added = await hrStore.addNewEmployee(data, authStore.user.email);
    if (added) {
      window.showPortalToast('Employee created and onboarding checklist triggered', 'success');
      alert(`Employee created successfully!\n\nHere is the Welcome Email Link dispatched to the employee:\n\n${added.welcomeLink}\n\nCopy this link and open it in a new window/private tab to set their initial password and complete onboarding details.`);
      resetForm();
      changeTab('directory');
    }
  } catch (e) {
    window.showPortalToast(e.message || 'Failed to create employee', 'error');
  } finally {
    isLoading.value = false;
  }
}

async function handleCreateDraftEmployee() {
  if (!newEmpName.value || !newEmpEmail.value || !newEmpPhone.value || !newEmpJoinDate.value) {
    window.showPortalToast('Full Name, Email, Phone Number, and Joining Date are required to save draft', 'error');
    return;
  }
  
  const parts = newEmpEmergency.value.split(';');
  const emergencyName = parts[0]?.trim() || '';
  const emergencyPhone = parts[1]?.trim() || '';

  // Auto-generate employee code behind the scenes
  const autoCode = generateAutoEmployeeCode();
  
  const data = {
    employeeCode: autoCode,
    fullName: newEmpName.value,
    email: newEmpEmail.value,
    phone: newEmpPhone.value,
    address: newEmpAddress.value,
    department: newEmpDept.value || 'HR',
    designation: newEmpDesig.value || 'HR Trainee',
    managerId: null,
    managerName: null,
    joiningDate: newEmpJoinDate.value,
    employmentType: newEmpType.value,
    role: newEmpRole.value,
    salary: newEmpSalary.value || 800000,
    salaryBand: newEmpSalaryBand.value || 'Band 1 (L1-L2)',
    emergencyName: emergencyName,
    emergencyPhone: emergencyPhone,
    dob: newEmpDob.value,
    workLocation: newEmpWorkLocation.value || 'Remote'
  };

  isLoading.value = true;
  try {
    const added = await hrStore.addNewEmployee(data, authStore.user.email);
    if (added) {
      added.status = 'INACTIVE';
      added.onboardingStatus = 'PENDING_DETAILS';
      window.showPortalToast('Employee profile saved as DRAFT', 'success');
      resetForm();
      changeTab('directory');
    }
  } catch (e) {
    window.showPortalToast(e.message || 'Failed to save draft', 'error');
  } finally {
    isLoading.value = false;
  }
}

function resetForm() {
  newEmpName.value = '';
  newEmpEmail.value = '';
  newEmpPhone.value = '';
  newEmpDob.value = '';
  newEmpAddress.value = '';
  newEmpEmergency.value = '';
  newEmpDept.value = '';
  newEmpDesig.value = '';
  newEmpWorkLocation.value = '';
  newEmpType.value = 'Full-time';
  newEmpRole.value = 'EMPLOYEE';
  newEmpSalary.value = 800000;
  newEmpSalaryBand.value = 'Band 1 (L1-L2)';
}

async function handleApproveOnboarding(empId) {
  const success = await hrStore.approveOnboarding(empId);
  if (success) {
    window.showPortalToast('Employee onboarding approved successfully. Credentials activated.', 'success');
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
  window.showPortalToast('Toggled task status', 'info');
}

// Inline deactivation from dots menu
function deactivateEmployeeInline(emp) {
  const confirmDeactivate = confirm(`Are you sure you want to deactivate ${emp.fullName}?`);
  if (!confirmDeactivate) return;
  
  const success = hrStore.updateEmployeeHrView(emp.id, {
    department: emp.department,
    designation: emp.designation,
    managerId: emp.managerId,
    employmentType: emp.employmentType,
    status: 'INACTIVE',
    salary: emp.salary,
    salaryBand: emp.salaryBand
  }, authStore.user.email);
  
  if (success) {
    window.showPortalToast(`${emp.fullName} has been deactivated`, 'success');
  }
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

// Format date into long readable format (e.g. Feb 10, 2023)
function formatDateLong(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  if (isNaN(date.getTime())) return dateStr;
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  return `${months[date.getMonth()]} ${date.getDate()}, ${date.getFullYear()}`;
}

// Initialized avatar initials
function getInitials(name) {
  if (!name) return 'EE';
  const parts = name.trim().split(/\s+/);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }
  return parts[0].substring(0, 2).toUpperCase();
}

// Soft background colors for initials avatar
function getAvatarColor(name) {
  if (!name) return '#6366f1';
  const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#6366f1', '#14b8a6'];
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  const index = Math.abs(hash) % colors.length;
  return colors[index];
}

// Status translation helpers
function getStatusLabel(status) {
  if (status === 'ACTIVE') return 'Active';
  if (status === 'ON_LEAVE') return 'On Leave';
  if (status === 'INACTIVE') return 'Inactive';
  return status;
}

function getStatusClass(status) {
  if (status === 'ACTIVE') return 'badge-success-pill';
  if (status === 'ON_LEAVE') return 'badge-warning-pill';
  return 'badge-danger-pill';
}

// Onboarding Employee James Wilson helper
const onboardingEmployee = computed(() => {
  return hrStore.employees.find(e => e.fullName === 'James Wilson' || e.employeeCode === 'EMP006');
});

// Action button check function
function updateOnboardingTask(empId, taskId) {
  hrStore.toggleOnboardingTask(empId, taskId, authStore.user.email);
  window.showPortalToast('Onboarding task updated', 'success');
}
</script>

<template>
  <div class="directory-view">
    
    <!-- TAB 1: EMPLOYEE DIRECTORY -->
    <template v-if="activeTab === 'directory'">
      <!-- HEADER -->
      <div class="view-header flex justify-between items-center mb-6">
        <div>
          <h1 class="page-title">Employee Directory</h1>
          <p class="page-description">Manage and view all employee information</p>
        </div>
        <div class="header-actions flex gap-3">
          <button class="btn-export" @click="exportToCSV">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="7 10 12 15 17 10" />
              <line x1="12" y1="15" x2="12" y2="3" />
            </svg>
            <span>Export</span>
          </button>
          <button class="btn-primary-dark" @click="changeTab('new-employee')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="8" cy="7" r="4" />
              <line x1="20" y1="8" x2="20" y2="14" />
              <line x1="17" y1="11" x2="23" y2="11" />
            </svg>
            <span>Add Employee</span>
          </button>
        </div>
      </div>

      <!-- SEARCH & FILTER -->
      <div class="glass-card mb-6 p-6">
        <h4 class="card-section-title mb-3" style="border: none; padding: 0;">Search & Filter</h4>
        <div class="filters-row flex gap-4">
          <div class="search-input-wrapper">
            <svg class="search-input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#64748b" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input type="text" v-model="searchQuery" class="search-input-field" placeholder="Search by name, email, or ID.." />
          </div>
          
          <select v-model="filterDept" class="filter-select">
            <option v-for="dept in departments" :key="dept" :value="dept">{{ dept }}</option>
          </select>

          <select v-model="filterStatus" class="filter-select">
            <option value="All Status">All Status</option>
            <option value="ACTIVE">Active</option>
            <option value="ON_LEAVE">On Leave</option>
            <option value="INACTIVE">Inactive</option>
          </select>
        </div>
      </div>

      <!-- EMPLOYEES LIST CARD -->
      <div class="glass-card no-padding overflow-hidden">
        <div class="card-header flex justify-between items-center p-6 border-b">
          <h3 class="card-title">Employees ({{ filteredEmployees.length }})</h3>
          <span class="card-subtitle" style="font-weight: 500;">Total: {{ hrStore.employees.length }} employees</span>
        </div>

        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Employee</th>
                <th>Employee ID</th>
                <th>Department</th>
                <th>Designation</th>
                <th>Joining Date</th>
                <th>Status</th>
                <th class="text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="emp in filteredEmployees" :key="emp.id" class="table-row">
                <td>
                  <div class="employee-cell flex items-center gap-3">
                    <div class="avatar-circle" :style="{ backgroundColor: getAvatarColor(emp.fullName) }">
                      {{ getInitials(emp.fullName) }}
                    </div>
                    <div class="employee-info flex flex-col">
                      <span class="employee-name">{{ emp.fullName }}</span>
                      <span class="employee-email">{{ emp.email }}</span>
                    </div>
                  </div>
                </td>
                <td>
                  <span class="font-mono text-sm font-semibold text-secondary">{{ emp.employeeCode }}</span>
                </td>
                <td>
                  <span class="text-secondary text-sm">{{ emp.department }}</span>
                </td>
                <td>
                  <span class="text-secondary text-sm">{{ emp.designation }}</span>
                </td>
                <td>
                  <span class="text-secondary text-sm">{{ formatDateLong(emp.joiningDate) }}</span>
                </td>
                <td>
                  <span :class="getStatusClass(emp.status)">{{ getStatusLabel(emp.status) }}</span>
                </td>
                <td class="text-right">
                  <div class="actions-cell-container">
                    <button class="btn-actions-dots" @click="toggleDropdown(emp.id, $event)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="1.5" />
                        <circle cx="12" cy="5" r="1.5" />
                        <circle cx="12" cy="19" r="1.5" />
                      </svg>
                    </button>
                    
                    <div class="actions-dropdown-menu" v-if="activeDropdownId === emp.id" @click.stop>
                      <button class="dropdown-item" @click="openInspector(emp)">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                        </svg>
                        <span>Configure Profile</span>
                      </button>
                      <button class="dropdown-item text-danger" @click="deactivateEmployeeInline(emp)" v-if="emp.status === 'ACTIVE'">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                          <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                        </svg>
                        <span>Deactivate</span>
                      </button>
                    </div>
                  </div>
                </td>
              </tr>
              <tr v-if="filteredEmployees.length === 0">
                <td colspan="7" class="text-center text-secondary py-8">
                  <h4 class="mb-1">No employees found</h4>
                  <p class="text-xs text-muted">Try resetting search filters.</p>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- TAB 2: ADD NEW EMPLOYEE -->
    <template v-if="activeTab === 'new-employee'">
      <!-- HEADER -->
      <div class="view-header flex items-center gap-4 mb-6">
        <button class="btn-back" @click="changeTab('directory')" aria-label="Go back to directory">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
        </button>
        <div>
          <h1 class="page-title">Add New Employee</h1>
          <p class="page-description">Enter employee details to create a new profile</p>
        </div>
      </div>

      <!-- FORM GRID -->
      <div class="grid-layout">
        <!-- LEFT COLUMN: FORMS -->
        <div class="form-column">
          <!-- CARD 1: PERSONAL INFORMATION -->
          <div class="glass-card mb-6 p-6">
            <h3 class="card-section-title mb-6">Personal Information</h3>
            <div class="form-grid-2">
              <div class="form-group-item">
                <label class="input-label">Full Name *</label>
                <input type="text" v-model="newEmpName" class="form-input-text" placeholder="John Doe" required />
              </div>
              <div class="form-group-item">
                <label class="input-label">Email Address *</label>
                <input type="email" v-model="newEmpEmail" class="form-input-text" placeholder="john.doe@company.com" required />
              </div>
              <div class="form-group-item">
                <label class="input-label">Phone Number *</label>
                <input type="text" v-model="newEmpPhone" class="form-input-text" placeholder="+1 234 567 8900" required />
              </div>
              <div class="form-group-item">
                <label class="input-label">Date of Birth</label>
                <div class="input-date-wrapper">
                  <input type="date" v-model="newEmpDob" class="form-input-text input-date-field" placeholder="dd-mm-yyyy" />
                </div>
              </div>
              <div class="form-group-item col-span-2">
                <label class="input-label">Address</label>
                <input type="text" v-model="newEmpAddress" class="form-input-text" placeholder="123 Main St, City, State, ZIP" />
              </div>
              <div class="form-group-item col-span-2">
                <label class="input-label">Emergency Contact</label>
                <input type="text" v-model="newEmpEmergency" class="form-input-text" placeholder="Name; Contact Number" />
              </div>
            </div>
          </div>

          <!-- CARD 2: EMPLOYMENT DETAILS -->
          <div class="glass-card p-6">
            <h3 class="card-section-title mb-6">Employment Details</h3>
            <div class="form-grid-2">
              <div class="form-group-item">
                <label class="input-label">Department *</label>
                <select v-model="newEmpDept" class="form-select-box" required>
                  <option value="" disabled selected>Select department</option>
                  <option value="Engineering">Engineering</option>
                  <option value="HR">HR</option>
                  <option value="Finance">Finance</option>
                  <option value="Marketing">Marketing</option>
                  <option value="Product">Product</option>
                  <option value="Sales">Sales</option>
                </select>
              </div>
              <div class="form-group-item">
                <label class="input-label">Designation *</label>
                <input type="text" v-model="newEmpDesig" class="form-input-text" placeholder="e.g., Software Engineer" required />
              </div>
              <div class="form-group-item">
                <label class="input-label">Joining Date *</label>
                <div class="input-date-wrapper">
                  <input type="date" v-model="newEmpJoinDate" class="form-input-text input-date-field" placeholder="dd-mm-yyyy" required />
                </div>
              </div>
              <div class="form-group-item">
                <label class="input-label">Work Location *</label>
                <select v-model="newEmpWorkLocation" class="form-select-box" required>
                  <option value="" disabled selected>Select work location</option>
                  <option value="New York">New York</option>
                  <option value="San Francisco">San Francisco</option>
                  <option value="Hyderabad">Hyderabad</option>
                  <option value="Remote">Remote</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        <!-- RIGHT COLUMN: ACTIONS & TIPS -->
        <div class="sidebar-column">
          <!-- CARD 1: ACTIONS -->
          <div class="glass-card mb-6 p-6">
            <h3 class="card-section-title mb-6">Actions</h3>
            <div class="actions-buttons-stack flex flex-col gap-3">
              <button class="btn-primary-dark btn-full-width" @click="handleCreateEmployee">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                  <circle cx="8" cy="7" r="4" />
                  <line x1="20" y1="8" x2="20" y2="14" />
                  <line x1="17" y1="11" x2="23" y2="11" />
                </svg>
                <span>Add Employee</span>
              </button>
              
              <button class="btn-white btn-full-width" @click="handleCreateDraftEmployee">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                  <polyline points="14 2 14 8 20 8" />
                  <line x1="16" y1="13" x2="8" y2="13" />
                  <line x1="16" y1="17" x2="8" y2="17" />
                  <polyline points="10 9 9 9 8 9" />
                </svg>
                <span>Save as Draft</span>
              </button>

              <button class="btn-cancel" @click="changeTab('directory')">
                <span>Cancel</span>
              </button>
            </div>
          </div>

          <!-- CARD 2: QUICK TIPS -->
          <div class="glass-card p-6">
            <h3 class="card-section-title mb-4">Quick Tips</h3>
            <ul class="quick-tips-list">
              <li>All fields marked with * are required</li>
              <li>Email should be unique for each employee</li>
              <li>Salary information will be confidential</li>
              <li>Employee ID will be auto-generated</li>
            </ul>
          </div>
        </div>
      </div>
    </template>

    <!-- TAB 3: ONBOARDING TRACKER -->
    <template v-if="activeTab === 'onboarding'">
      <!-- HEADER -->
      <div class="view-header mb-6">
        <h1 class="page-title">Onboarding Tracker</h1>
        <p class="page-description">Monitor new employee onboarding progress</p>
      </div>

      <!-- EMPLOYEE OVERVIEW CARD -->
      <div class="onboarding-overview-card" v-if="onboardingEmployee">
        <div class="onboarding-overview-top">
          <h2 class="onboarding-emp-name">{{ onboardingEmployee.fullName }}</h2>
          <p class="onboarding-emp-role">{{ onboardingEmployee.designation }}</p>
        </div>

        <div class="onboarding-progress-section">
          <div class="onboarding-progress-header">
            <span class="onboarding-progress-label">Progress</span>
            <span class="onboarding-progress-count">
              {{ onboardingEmployee.onboardingTasks.filter(t => t.done).length }}/{{ onboardingEmployee.onboardingTasks.length }} tasks
            </span>
          </div>
          <div class="onboarding-progress-track">
            <div class="onboarding-progress-fill" :style="{ width: onboardingEmployee.onboardingPercent + '%' }"></div>
          </div>
        </div>

        <div class="onboarding-overview-bottom">
          <span class="onboarding-joined-label">Joined {{ formatDateLong(onboardingEmployee.joiningDate) }}</span>
        </div>
      </div>

      <!-- ONBOARDING TASKS TABLE -->
      <div class="glass-card no-padding overflow-hidden" v-if="onboardingEmployee">
        <div class="card-header p-6 border-b">
          <h3 class="card-title">Onboarding Tasks</h3>
        </div>

        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Employee</th>
                <th>Task</th>
                <th>Due Date</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="task in onboardingEmployee.onboardingTasks" :key="task.id" class="table-row">
                <td>
                  <span class="onboarding-table-emp">{{ onboardingEmployee.fullName }}</span>
                </td>
                <td>
                  <div class="task-cell">
                    <svg v-if="task.done" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#22c55e" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="task-icon">
                      <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                      <polyline points="22 4 12 14.01 9 11.01" />
                    </svg>
                    <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="task-icon">
                      <circle cx="12" cy="12" r="10" />
                      <polyline points="12 6 12 12 16 14" />
                    </svg>
                    <span class="task-title" :class="{ 'task-title-done': task.done }">{{ task.title }}</span>
                  </div>
                </td>
                <td>
                  <span class="onboarding-table-date">{{ task.dueDate }}</span>
                </td>
                <td>
                  <span :class="task.done ? 'badge-success-pill' : 'badge-warning-pill'">
                    {{ task.done ? 'Completed' : 'In Progress' }}
                  </span>
                </td>
                <td>
                  <button v-if="!task.done" class="btn-update-task" @click="updateOnboardingTask(onboardingEmployee.id, task.id)">
                    Update
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- TAB 4: HR PROFILE INSPECTOR -->
    <template v-if="activeTab === 'inspector'">
      <div class="view-header flex items-center gap-4 mb-6" v-if="selectedEmp">
        <button class="btn-back" @click="changeTab('directory')" aria-label="Go back to directory">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
        </button>
        <div>
          <h1 class="page-title">Configure Employee Profile</h1>
          <p class="page-description">Inspect and configure HR parameters for {{ selectedEmp.fullName }}</p>
        </div>
      </div>

      <div class="grid-layout" v-if="selectedEmp">
        <!-- left configuration -->
        <div class="form-column">
          <div class="glass-card mb-6 p-6">
            <div class="flex items-center gap-4 mb-6">
              <div class="avatar-circle" :style="{ backgroundColor: getAvatarColor(selectedEmp.fullName), width: '64px', height: '64px', fontSize: '20px' }">
                {{ getInitials(selectedEmp.fullName) }}
              </div>
              <div>
                <h2 class="overview-name">{{ selectedEmp.fullName }}</h2>
                <p class="page-description font-mono">{{ selectedEmp.employeeCode }} • Joined {{ formatDateLong(selectedEmp.joiningDate) }}</p>
              </div>
            </div>
            
            <form @submit.prevent="handleSaveInspector">
              <div class="form-grid-2">
                <!-- Onboarding verification review -->
                <div v-if="selectedEmp.onboardingStatus === 'PENDING_APPROVAL' || selectedEmp.onboardingStatus === 'PENDING_DETAILS'" class="col-span-2 mb-6 p-4 rounded" style="background: rgba(245, 158, 11, 0.05); border: 1px solid rgba(245, 158, 11, 0.2); width: 100%;">
                  <h3 class="text-sm font-bold mb-3 flex items-center gap-2" style="color: #f59e0b; margin-top: 0; font-size: 14px;">
                    <IconHelper name="clock" size="16" />
                    Onboarding Verification Review (Status: {{ selectedEmp.onboardingStatus }})
                  </h3>
                  
                  <div class="form-grid-2 gap-3 mb-4">
                    <div class="form-group-item">
                      <span class="text-xs text-muted block" style="font-size: 11px;">School Background</span>
                      <p class="text-xs font-semibold m-0" style="font-size: 12px; font-weight: 600;">{{ selectedEmp.school || 'Not entered yet' }}</p>
                    </div>
                    <div class="form-group-item">
                      <span class="text-xs text-muted block" style="font-size: 11px;">College / University</span>
                      <p class="text-xs font-semibold m-0" style="font-size: 12px; font-weight: 600;">{{ selectedEmp.college || 'Not entered yet' }}</p>
                    </div>
                    <div class="form-group-item col-span-2">
                      <span class="text-xs text-muted block" style="font-size: 11px;">Previous Work Experience</span>
                      <p class="text-xs font-semibold m-0" style="font-size: 12px; font-weight: 600; white-space: pre-wrap;">{{ selectedEmp.experience || 'Not entered yet' }}</p>
                    </div>
                  </div>

                  <div class="flex justify-end gap-3 mt-4 pt-4" style="border-top: 1px solid rgba(0,0,0,0.05);" v-if="selectedEmp.onboardingStatus === 'PENDING_APPROVAL'">
                    <button type="button" class="btn-primary-dark btn-sm" @click="handleApproveOnboarding(selectedEmp.id)">
                      Approve Onboarding Profile
                    </button>
                  </div>
                </div>

                <h4 class="col-span-2 section-sub-header">Employment Parameters</h4>
                <div class="form-group-item">
                  <label class="input-label">Department</label>
                  <select v-model="inspectorDept" class="form-select-box">
                    <option value="Engineering">Engineering</option>
                    <option value="HR">HR</option>
                    <option value="Finance">Finance</option>
                    <option value="Marketing">Marketing</option>
                    <option value="Product">Product</option>
                    <option value="Sales">Sales</option>
                  </select>
                </div>
                <div class="form-group-item">
                  <label class="input-label">Designation</label>
                  <input type="text" v-model="inspectorDesig" class="form-input-text" />
                </div>
                <div class="form-group-item">
                  <label class="input-label">Reporting Manager</label>
                  <select v-model="inspectorManager" class="form-select-box">
                    <option value="">None (Top Node)</option>
                    <option v-for="mgr in managers" :key="mgr.id" :value="mgr.id">{{ mgr.fullName }}</option>
                  </select>
                </div>
                <div class="form-group-item">
                  <label class="input-label">Employment Type</label>
                  <select v-model="inspectorType" class="form-select-box">
                    <option value="Full-time">Full-time</option>
                    <option value="Contract">Contract</option>
                  </select>
                </div>

                <h4 class="col-span-2 section-sub-header">Compensation & Security</h4>
                <div class="form-group-item">
                  <label class="input-label">Salary Band</label>
                  <select v-model="inspectorSalaryBand" class="form-select-box">
                    <option value="Band 1 (L1-L2)">Band 1 (L1-L2)</option>
                    <option value="Band 2 (L2-L3)">Band 2 (L2-L3)</option>
                    <option value="Band 3 (L3-L4)">Band 3 (L3-L4)</option>
                    <option value="Band 4 (L5-L6)">Band 4 (L5-L6)</option>
                    <option value="Band 5 (L7+)">Band 5 (L7+)</option>
                  </select>
                </div>
                <div class="form-group-item">
                  <label class="input-label">Annual Salary (INR)</label>
                  <input type="number" v-model="inspectorSalary" class="form-input-text" />
                </div>
                
                <div class="form-group-item">
                  <label class="input-label">Account Status</label>
                  <select v-model="inspectorStatus" class="form-select-box">
                    <option value="ACTIVE">Active Account</option>
                    <option value="INACTIVE">Deactivated Account</option>
                  </select>
                </div>
              </div>

              <div class="flex justify-between items-center mt-6 pt-4 border-t">
                <button type="button" class="dropdown-item text-danger" style="width: auto; padding: 8px 16px; border: 1px solid #ef4444; border-radius: 6px;" @click="inspectorStatus = 'INACTIVE'; handleSaveInspector()" v-if="selectedEmp.status === 'ACTIVE'">
                  Deactivate Employee
                </button>
                <span v-else></span>
                
                <div class="flex gap-3">
                  <button type="button" class="btn-white" @click="changeTab('directory')">Back</button>
                  <button type="submit" class="btn-primary-dark">Save Configuration</button>
                </div>
              </div>
            </form>
          </div>
        </div>

        <!-- right checks & document history logs -->
        <div class="sidebar-column">
          <!-- Onboarding status -->
          <div class="glass-card mb-6 p-6" v-if="selectedEmp.onboardingTasks && selectedEmp.onboardingTasks.length">
            <h3 class="card-section-title mb-4">Onboarding Completion ({{ selectedEmp.onboardingPercent }}%)</h3>
            <div class="checklist-items">
              <div
                v-for="task in selectedEmp.onboardingTasks"
                :key="task.id"
                class="checklist-item py-2 flex items-center gap-2"
              >
                <input type="checkbox" :checked="task.done" @change="toggleInspectorOnboardingTask(task.id)" class="checkbox-custom" />
                <span class="text-xs" :class="{ 'task-title-done': task.done }">{{ task.title }}</span>
              </div>
            </div>
          </div>

          <!-- Uploaded Documents log -->
          <div class="glass-card p-6">
            <h3 class="card-section-title mb-4">Uploaded Files Log</h3>
            <div class="table-container">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>Filename</th>
                    <th>Type</th>
                    <th>Size</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="doc in selectedEmp.documents" :key="doc.name" class="table-row">
                    <td class="text-xs font-semibold">{{ doc.name }}</td>
                    <td><span class="badge badge-muted">{{ doc.type }}</span></td>
                    <td class="text-xs text-secondary">{{ doc.size }}</td>
                  </tr>
                  <tr v-if="selectedEmp.documents.length === 0">
                    <td colspan="3" class="text-center text-xs text-secondary py-4">No documents uploaded.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- TAB 5: GLOBAL DOCUMENTS LIST -->
    <template v-if="activeTab === 'documents'">
      <div class="view-header mb-6">
        <h1 class="page-title">Organization Documents</h1>
        <p class="page-description">Manage mandatory document templates and audit uploads</p>
      </div>

      <div class="grid-layout">
        <!-- Left: Manage Mandatory Document Requirements -->
        <div class="form-column">
          <div class="glass-card mb-6 p-6">
            <h3 class="card-section-title mb-4">Global Document Logs</h3>
            <div class="table-container">
              <table class="data-table">
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
                  <tr v-for="(doc, idx) in allDocuments" :key="idx" class="table-row">
                    <td>
                      <div class="flex flex-col">
                        <span class="font-bold text-sm text-secondary">{{ doc.employeeName }}</span>
                        <span class="font-mono text-xs text-muted">{{ doc.employeeCode }}</span>
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
                      <button class="btn-actions-dots" @click="downloadEmployeeDoc(doc.employeeId, doc)" title="Download file" aria-label="Download document file">
                        <IconHelper name="download" size="14" />
                      </button>
                    </td>
                  </tr>
                  <tr v-if="allDocuments.length === 0">
                    <td colspan="5" class="text-center text-secondary py-6">No documents logged in database.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Right: Global Document Logs -->
        <div class="sidebar-column">
          <div class="glass-card mb-6 p-6">
            <h3 class="card-section-title mb-4">Add Required Document</h3>
            <div class="form-group-item mb-4">
              <label class="input-label">Required Document Name</label>
              <input type="text" v-model="newMandatoryName" class="form-input-text" placeholder="e.g. Passport, Tax Declaration Form" />
            </div>
            <button class="btn-primary-dark btn-full-width" @click="handleAddMandatoryDocument">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 4px;">
                <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              <span>Add Mandatory Document</span>
            </button>
          </div>

          <div class="glass-card p-6">
            <h3 class="card-section-title mb-4">Mandatory Document Types</h3>
            <div class="mandatory-docs-list-hr">
              <div v-for="mDoc in hrStore.mandatoryDocuments" :key="mDoc.id" class="p-3 mb-2 rounded border" style="background: rgba(0, 0, 0, 0.01); border-color: #e2e8f0">
                <div class="flex items-center justify-between">
                  <span class="font-semibold text-sm text-secondary">{{ mDoc.name }}</span>
                  <span class="text-xs text-muted">Required</span>
                </div>
              </div>
              <div v-if="hrStore.mandatoryDocuments.length === 0" class="text-center text-muted text-xs p-4">
                No mandatory documents defined yet.
              </div>
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
  padding: 8px;
}

.view-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 4px;
}

.page-description {
  font-size: 14px;
  color: #64748b;
}

/* BUTTONS */
.btn-primary-dark {
  background-color: #0f172a; /* Slate 900 */
  color: #ffffff;
  border: 1px solid #0f172a;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.btn-primary-dark:hover {
  background-color: #1e293b;
  border-color: #1e293b;
}

.btn-white {
  background-color: #ffffff;
  color: #0f172a;
  border: 1px solid #e2e8f0;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.btn-white:hover {
  background-color: #f8fafc;
  border-color: #cbd5e1;
}

.btn-export {
  background-color: #ffffff;
  color: #0f172a;
  border: 1px solid #e2e8f0;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.btn-export:hover {
  background-color: #f8fafc;
  border-color: #cbd5e1;
}

.btn-back {
  background: none;
  border: none;
  color: #0f172a;
  cursor: pointer;
  padding: 8px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s ease;
  border: 1px solid #e2e8f0;
  background-color: #ffffff;
}
.btn-back:hover {
  background-color: #f1f5f9;
}

.btn-cancel {
  background: none;
  border: none;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  padding: 10px;
  cursor: pointer;
  transition: color 0.2s ease;
  text-align: center;
  border: 1px dashed #e2e8f0;
  border-radius: 6px;
  display: block;
}
.btn-cancel:hover {
  color: #0f172a;
  background-color: #f8fafc;
}

.btn-full-width {
  width: 100%;
  justify-content: center;
}

/* CARDS */
.glass-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05);
}
.glass-card.no-padding {
  padding: 0;
}

.card-header {
  border-bottom: 1px solid #e2e8f0;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
}

.card-subtitle {
  font-size: 12px;
  color: #64748b;
}

/* SEARCH & FILTER */
.search-input-wrapper {
  position: relative;
  flex: 1;
}
.search-input-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
}
.search-input-field {
  width: 100%;
  padding: 8px 12px 8px 36px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
  background-color: #f8fafc;
  outline: none;
  transition: all 0.2s ease;
}
.search-input-field:focus {
  border-color: #3b82f6;
  background-color: #ffffff;
}

.filter-select {
  padding: 8px 32px 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
  background-color: #f8fafc;
  outline: none;
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%2364748b' stroke-width='2.5'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' d='M19.5 8.25l-7.5 7.5-7.5-7.5'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  background-size: 12px;
  min-width: 160px;
}
.filter-select:focus {
  border-color: #3b82f6;
  background-color: #ffffff;
}

/* TABLES */
.table-container {
  width: 100%;
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.data-table th {
  padding: 14px 24px;
  border-bottom: 1px solid #e2e8f0;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  background-color: #f8fafc;
}

.data-table td {
  padding: 16px 24px;
  border-bottom: 1px solid #e2e8f0;
  vertical-align: middle;
}

.table-row {
  transition: background-color 0.15s ease;
}
.table-row:hover {
  background-color: #f8fafc;
}

/* EMPLOYEE CELL */
.avatar-circle {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 14px;
}

.employee-name {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.employee-email {
  font-size: 12px;
  color: #64748b;
}

/* ACTIONS DOTS MENU */
.actions-cell-container {
  position: relative;
  display: inline-block;
}

.btn-actions-dots {
  background: none;
  border: none;
  color: #64748b;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}
.btn-actions-dots:hover {
  background-color: #f1f5f9;
  color: #0f172a;
}

.actions-dropdown-menu {
  position: absolute;
  right: 0;
  top: 100%;
  margin-top: 4px;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  z-index: 50;
  min-width: 170px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.dropdown-item {
  background: none;
  border: none;
  padding: 10px 16px;
  font-size: 13px;
  color: #0f172a;
  text-align: left;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: background-color 0.15s ease;
  width: 100%;
}
.dropdown-item:hover {
  background-color: #f1f5f9;
}
.dropdown-item.text-danger {
  color: #ef4444;
}
.dropdown-item.text-danger:hover {
  background-color: #fef2f2;
}

/* BADGES PILLS */
.badge-success-pill {
  background-color: #e6f4ea;
  color: #137333;
  padding: 4px 12px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
  display: inline-block;
}
.badge-warning-pill {
  background-color: #fef7e0;
  color: #b06000;
  padding: 4px 12px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
  display: inline-block;
}
.badge-danger-pill {
  background-color: #fce8e6;
  color: #c5221f;
  padding: 4px 12px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
  display: inline-block;
}

/* FORM LAYOUTS */
.grid-layout {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.form-group-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.col-span-2 {
  grid-column: span 2;
}

.input-label {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.form-input-text, .form-select-box {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
  background-color: #ffffff;
  outline: none;
  transition: all 0.2s ease;
  color: #0f172a;
}
.form-input-text:focus, .form-select-box:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

.form-select-box {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%2364748b' stroke-width='2.5'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' d='M19.5 8.25l-7.5 7.5-7.5-7.5'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  background-size: 12px;
}

.input-date-wrapper {
  position: relative;
}

.input-date-field::-webkit-calendar-picker-indicator {
  cursor: pointer;
  opacity: 0.6;
  transition: opacity 0.2s;
}
.input-date-field::-webkit-calendar-picker-indicator:hover {
  opacity: 1;
}

.card-section-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 8px;
  margin-top: 0;
}

.section-sub-header {
  font-size: 14px;
  font-weight: 700;
  color: #475569;
  border-bottom: 1px dashed #e2e8f0;
  padding-bottom: 4px;
  margin-top: 12px;
}

/* QUICK TIPS */
.quick-tips-list {
  padding-left: 20px;
  margin: 0;
  font-size: 13px;
  color: #475569;
  line-height: 1.8;
}

/* ONBOARDING TRACKER */
.max-width-600 {
  max-width: 600px;
}

.onboarding-overview-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 28px 32px;
  margin-bottom: 24px;
  max-width: 520px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.onboarding-overview-top {
  margin-bottom: 20px;
}

.onboarding-emp-name {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 4px 0;
  line-height: 1.3;
}

.onboarding-emp-role {
  font-size: 14px;
  color: #64748b;
  margin: 0;
  font-weight: 400;
}

.onboarding-progress-section {
  margin-bottom: 16px;
}

.onboarding-progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.onboarding-progress-label {
  font-size: 14px;
  font-weight: 500;
  color: #0f172a;
}

.onboarding-progress-count {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.onboarding-progress-track {
  width: 100%;
  height: 10px;
  background-color: #e2e8f0;
  border-radius: 9999px;
  overflow: hidden;
}

.onboarding-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #1a73e8, #4285f4);
  border-radius: 9999px;
  transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.onboarding-overview-bottom {
  padding-top: 4px;
}

.onboarding-joined-label {
  font-size: 13px;
  color: #94a3b8;
  font-weight: 400;
}

/* Onboarding table specifics */
.onboarding-table-emp {
  font-size: 14px;
  font-weight: 600;
  color: #1a73e8;
}

.onboarding-table-date {
  font-size: 13px;
  color: #475569;
}

.overview-name {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 2px;
}

.overview-designation {
  font-size: 14px;
  color: #64748b;
}

.task-title-done {
  text-decoration: line-through;
  color: #94a3b8;
}

.btn-update-task {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  color: #0f172a;
  padding: 6px 16px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}
.btn-update-task:hover {
  background-color: #f0f7ff;
  border-color: #1a73e8;
  color: #1a73e8;
}

.task-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.task-icon {
  flex-shrink: 0;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 1024px) {
  .grid-layout {
    grid-template-columns: 1fr;
  }
}
</style>
