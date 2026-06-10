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

// Set active tab based on router metadata
const activeTab = ref(route.meta.tab || 'details');

// State for profile editing
const phone = ref('');
const address = ref('');
const emergencyName = ref('');
const emergencyRelation = ref('');
const emergencyPhone = ref('');
const bankAccountNo = ref('');
const bankIfsc = ref('');
const bankName = ref('');

// Seed local edit state from matched employee
const employee = computed(() => {
  return hrStore.employees.find(e => e.id === authStore.user.id) || hrStore.employees[0];
});

onMounted(() => {
  if (employee.value) {
    phone.value = employee.value.phone;
    address.value = employee.value.address;
    emergencyName.value = employee.value.emergencyContact.name;
    emergencyRelation.value = employee.value.emergencyContact.relation;
    emergencyPhone.value = employee.value.emergencyContact.phone;
    bankAccountNo.value = employee.value.bankDetails.accountNo;
    bankIfsc.value = employee.value.bankDetails.ifsc;
    bankName.value = employee.value.bankDetails.bankName;
  }
});

// Watch tab prop
onMounted(() => {
  activeTab.value = route.meta.tab || 'details';
});

function changeTab(tab) {
  activeTab.value = tab;
  router.push(`/${tab === 'details' ? 'profile' : tab}`);
}

function handleSaveProfile() {
  const data = {
    phone: phone.value,
    address: address.value,
    emergencyContact: {
      name: emergencyName.value,
      relation: emergencyRelation.value,
      phone: emergencyPhone.value
    },
    bankDetails: {
      accountNo: bankAccountNo.value,
      ifsc: bankIfsc.value,
      bankName: bankName.value
    }
  };
  
  const success = hrStore.updateEmployeeProfile(employee.value.id, data, authStore.user.email);
  if (success) {
    window.showPortalToast('Profile details updated successfully', 'success');
  }
}

// Onboarding Task Toggle
function toggleTask(taskId) {
  hrStore.toggleOnboardingTask(employee.value.id, taskId, authStore.user.email);
  window.showPortalToast('Onboarding task checklist updated', 'info');
}

// Document Upload Mock
const docType = ref('Personal');
const isDragging = ref(false);

function handleFileDrop(e) {
  isDragging.value = false;
  const files = e.dataTransfer.files;
  if (files.length > 0) {
    processMockUpload(files[0]);
  }
}

function handleFileSelect(e) {
  const files = e.target.files;
  if (files.length > 0) {
    processMockUpload(files[0]);
  }
}

function processMockUpload(file) {
  const newDoc = {
    name: file.name,
    size: `${(file.size / 1024).toFixed(0)} KB`,
    date: new Date().toISOString().split('T')[0],
    type: docType.value
  };

  // Add document to employee list via hrStore
  const data = { newDocument: newDoc };
  hrStore.updateEmployeeProfile(employee.value.id, data, authStore.user.email);
  window.showPortalToast(`File "${file.name}" uploaded successfully to AWS S3.`, 'success');
}

function downloadMockDoc(docName) {
  window.showPortalToast(`Downloading ${docName} (Decrypted PDF)...`, 'success');
  // Simple simulation of secure file retrieval via presigned URL
  const link = document.createElement('a');
  link.href = '#';
  link.setAttribute('download', docName);
  document.body.appendChild(link);
  setTimeout(() => {
    document.body.removeChild(link);
  }, 100);
}
</script>

<template>
  <div class="profile-view" v-if="employee">
    <!-- TABS NAVIGATION -->
    <div class="tabs-container">
      <div class="tabs">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'details' }"
          @click="changeTab('details')"
        >
          My Profile
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'onboarding' }"
          @click="changeTab('onboarding')"
        >
          Onboarding Tasks ({{ employee.onboardingPercent }}%)
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'documents' }"
          @click="changeTab('documents')"
        >
          HR Documents
        </button>
      </div>
    </div>

    <!-- MAIN BODY -->
    <div class="grid-12">
      <!-- TAB 1: PROFILE DETAILS -->
      <template v-if="activeTab === 'details'">
        <!-- Avatar card -->
        <div class="col-4">
          <div class="glass-card text-center profile-summary-card">
            <div class="avatar-container mb-4">
              <img :src="employee.photo" alt="Profile" class="profile-avatar-lg" />
              <div class="photo-upload-badge" title="Change Profile Photo">
                <IconHelper name="upload" size="14" />
              </div>
            </div>
            <h3 class="mb-1">{{ employee.fullName }}</h3>
            <p class="text-secondary text-sm mb-4">{{ employee.designation }} • {{ employee.department }}</p>
            <div class="badge badge-success mb-6">{{ employee.employmentType }}</div>
            
            <div class="divider"></div>
            
            <div class="info-row text-left mb-3">
              <span class="info-label">Employee ID</span>
              <span class="info-val font-mono">{{ employee.employeeCode }}</span>
            </div>
            <div class="info-row text-left mb-3">
              <span class="info-label">Join Date</span>
              <span class="info-val">{{ employee.joiningDate }}</span>
            </div>
            <div class="info-row text-left">
              <span class="info-label">Manager</span>
              <span class="info-val font-semibold">{{ employee.managerName || 'None' }}</span>
            </div>
          </div>
        </div>

        <!-- Editable details form -->
        <div class="col-8">
          <div class="glass-card">
            <h3 class="mb-6">Edit Contact & Financial Information</h3>
            <form @submit.prevent="handleSaveProfile">
              <div class="grid-12">
                <!-- Contact info -->
                <h4 class="col-12 section-header">Personal Information</h4>
                <div class="col-6 form-group">
                  <label class="form-label">Phone Number</label>
                  <input type="text" v-model="phone" class="form-control" />
                </div>
                <div class="col-6 form-group">
                  <label class="form-label">Work Email</label>
                  <input type="email" :value="employee.email" class="form-control" disabled />
                </div>
                <div class="col-12 form-group">
                  <label class="form-label">Residential Address</label>
                  <textarea v-model="address" class="form-control"></textarea>
                </div>

                <!-- Emergency contact -->
                <h4 class="col-12 section-header">Emergency Contact</h4>
                <div class="col-4 form-group">
                  <label class="form-label">Contact Person</label>
                  <input type="text" v-model="emergencyName" class="form-control" />
                </div>
                <div class="col-4 form-group">
                  <label class="form-label">Relation</label>
                  <input type="text" v-model="emergencyRelation" class="form-control" />
                </div>
                <div class="col-4 form-group">
                  <label class="form-label">Contact Number</label>
                  <input type="text" v-model="emergencyPhone" class="form-control" />
                </div>

                <!-- Banking details -->
                <h4 class="col-12 section-header">Bank Account Details (For Salary & Claims)</h4>
                <div class="col-4 form-group">
                  <label class="form-label">Bank Name</label>
                  <input type="text" v-model="bankName" class="form-control" />
                </div>
                <div class="col-4 form-group">
                  <label class="form-label">Account Number</label>
                  <input type="text" v-model="bankAccountNo" class="form-control" />
                </div>
                <div class="col-4 form-group">
                  <label class="form-label">IFSC Code</label>
                  <input type="text" v-model="bankIfsc" class="form-control" />
                </div>
              </div>

              <div class="flex justify-end gap-3 mt-4">
                <button type="submit" class="btn btn-primary">
                  Save Changes
                </button>
              </div>
            </form>
          </div>
        </div>
      </template>

      <!-- TAB 2: ONBOARDING CHECKLIST -->
      <template v-if="activeTab === 'onboarding'">
        <!-- Left: Progress card -->
        <div class="col-4">
          <div class="glass-card text-center">
            <h3 class="mb-4">Onboarding Summary</h3>
            <div class="progress-ring-container mb-6">
              <div class="progress-ring-fill" :style="`background: conic-gradient(var(--success) ${employee.onboardingPercent}%, rgba(255,255,255,0.05) ${employee.onboardingPercent}% 100%)`">
                <div class="progress-ring-inner">
                  <span class="progress-ring-text">{{ employee.onboardingPercent }}%</span>
                </div>
              </div>
            </div>
            <p class="text-secondary text-sm">
              Please complete all checklist items to complete your profile setup. HR is notified of updates.
            </p>
          </div>
        </div>

        <!-- Right: Task checklist -->
        <div class="col-8">
          <div class="glass-card">
            <h3 class="mb-6">Tasks to Complete</h3>
            <div class="checklist-items" v-if="employee.onboardingTasks && employee.onboardingTasks.length">
              <div
                v-for="task in employee.onboardingTasks"
                :key="task.id"
                class="checklist-item"
                :class="{ checked: task.done }"
              >
                <div class="checklist-item-left">
                  <div class="checkbox-custom" @click="toggleTask(task.id)">
                    <IconHelper v-if="task.done" name="check" size="14" />
                  </div>
                  <span class="task-title" :style="task.done ? 'text-decoration: line-through; color: var(--text-muted);' : ''">
                    {{ task.title }}
                  </span>
                </div>
                <span class="badge" :class="task.done ? 'badge-success' : 'badge-warning'">
                  {{ task.done ? 'Done' : 'Pending' }}
                </span>
              </div>
            </div>
            <div v-else class="text-center p-8 text-secondary">
              No active onboarding tasks assigned to your profile.
            </div>
          </div>
        </div>
      </template>

      <!-- TAB 3: HR DOCUMENTS -->
      <template v-if="activeTab === 'documents'">
        <!-- Left: Upload Widget -->
        <div class="col-4">
          <div class="glass-card">
            <h3 class="mb-6">Upload Document</h3>
            <div class="form-group">
              <label class="form-label">Document Category</label>
              <select v-model="docType" class="form-control">
                <option value="Personal">Personal Details</option>
                <option value="Identity">Identity Proof (Aadhaar/PAN/Passport)</option>
                <option value="Education">Educational Certificates</option>
                <option value="Tax">Declaration & Forms</option>
              </select>
            </div>
            
            <div
              class="upload-zone"
              :class="{ active: isDragging }"
              @dragover.prevent="isDragging = true"
              @dragleave="isDragging = false"
              @drop.prevent="handleFileDrop"
              @click="$refs.fileInput.click()"
            >
              <input
                type="file"
                ref="fileInput"
                style="display: none"
                @change="handleFileSelect"
              />
              <div class="mb-3">
                <IconHelper name="upload" size="32" color="var(--primary-color)" />
              </div>
              <p class="font-semibold text-sm">Drag & Drop files here</p>
              <p class="text-xs text-secondary mt-1">or click to browse files (PDF, JPG, PNG)</p>
            </div>
          </div>
        </div>

        <!-- Right: Uploaded Documents list -->
        <div class="col-8">
          <div class="glass-card">
            <h3 class="mb-6">Linked Documents</h3>
            <div class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>Document Name</th>
                    <th>Category</th>
                    <th>Upload Date</th>
                    <th>Size</th>
                    <th class="text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="doc in employee.documents" :key="doc.name">
                    <td>
                      <div class="flex items-center gap-2">
                        <IconHelper name="file-text" size="16" color="#94a3b8" />
                        <span class="font-semibold">{{ doc.name }}</span>
                      </div>
                    </td>
                    <td>
                      <span class="badge badge-muted">{{ doc.type }}</span>
                    </td>
                    <td>{{ doc.date }}</td>
                    <td>{{ doc.size }}</td>
                    <td class="text-right">
                      <button class="btn btn-ghost btn-sm" @click="downloadMockDoc(doc.name)">
                        <IconHelper name="download" size="14" />
                      </button>
                    </td>
                  </tr>
                  <tr v-if="employee.documents.length === 0">
                    <td colspan="5" class="text-center text-secondary">
                      No documents linked to this profile.
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.profile-view {
  animation: fadeIn 0.4s ease;
}

.profile-summary-card {
  padding-top: 36px;
}

.avatar-container {
  position: relative;
  display: inline-block;
}
.profile-avatar-lg {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid var(--secondary-light);
  box-shadow: var(--shadow-md);
}
.photo-upload-badge {
  position: absolute;
  bottom: 0;
  right: 0;
  background-color: var(--primary-color);
  color: #fff;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--bg-surface-solid);
  cursor: pointer;
}

.divider {
  height: 1px;
  background-color: var(--border-color);
  margin: 20px 0;
}

.info-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}
.info-label {
  color: var(--text-secondary);
}

.section-header {
  font-size: 14px;
  font-weight: 700;
  color: var(--primary-color);
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 8px;
  margin-top: 16px;
  margin-bottom: 16px;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

/* Progress Ring Visual */
.progress-ring-container {
  display: flex;
  justify-content: center;
}
.progress-ring-fill {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-md);
}
.progress-ring-inner {
  width: 110px;
  height: 110px;
  background-color: var(--bg-surface-solid);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.progress-ring-text {
  font-size: 24px;
  font-weight: 800;
  color: var(--success);
}

.text-left { text-align: left; }
.text-center { text-align: center; }
.text-right { text-align: right; }
.mb-1 { margin-bottom: 4px; }
.mb-3 { margin-bottom: 12px; }
.mb-4 { margin-bottom: 16px; }
.mb-6 { margin-bottom: 24px; }
.mt-4 { margin-top: 16px; }
.p-8 { padding: 32px; }
.font-mono { font-family: monospace; }
.font-semibold { font-weight: 600; }
.font-bold { font-weight: 700; }
.flex { display: flex; }
.items-center { align-items: center; }
.gap-2 { gap: 8px; }
.gap-3 { gap: 12px; }
.justify-end { justify-content: flex-end; }
</style>
