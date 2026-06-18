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

// Real Document Uploads & Downloads
import { API_BASE_URL } from '../config.js';

const docType = ref('Personal');
const isDragging = ref(false);

async function handleMandatoryFileUpload(e, mandatoryDocType) {
  const files = e.target.files;
  if (files.length > 0) {
    const file = files[0];
    const success = await hrStore.uploadDocument(employee.value.id, file, mandatoryDocType, authStore.user.email);
    if (success) {
      window.showPortalToast(`"${file.name}" uploaded successfully.`, 'success');
    } else {
      window.showPortalToast(`Failed to upload "${file.name}".`, 'error');
    }
  }
}

async function handleOtherFileUpload(e) {
  const files = e.target.files;
  if (files.length > 0) {
    const file = files[0];
    const success = await hrStore.uploadDocument(employee.value.id, file, docType.value, authStore.user.email);
    if (success) {
      window.showPortalToast(`"${file.name}" uploaded successfully.`, 'success');
    } else {
      window.showPortalToast(`Failed to upload "${file.name}".`, 'error');
    }
  }
}

async function handleFileDrop(e) {
  isDragging.value = false;
  const files = e.dataTransfer.files;
  if (files.length > 0) {
    const file = files[0];
    const success = await hrStore.uploadDocument(employee.value.id, file, docType.value, authStore.user.email);
    if (success) {
      window.showPortalToast(`"${file.name}" uploaded successfully.`, 'success');
    } else {
      window.showPortalToast(`Failed to upload "${file.name}".`, 'error');
    }
  }
}

function downloadRealDoc(doc) {
  const token = localStorage.getItem('jwt_token');
  const url = `${API_BASE_URL}/api/v1/employees/${employee.value.id}/documents/${doc.id}/download`;
  
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

async function deleteRealDoc(docId) {
  if (confirm('Are you sure you want to delete this document?')) {
    const success = await hrStore.deleteDocument(employee.value.id, docId, authStore.user.email);
    if (success) {
      window.showPortalToast('Document deleted successfully.', 'success');
    } else {
      window.showPortalToast('Failed to delete document.', 'error');
    }
  }
}

function getUploadedDoc(mandatoryName) {
  return employee.value.documents?.find(d => d.type === mandatoryName);
}

const otherDocuments = computed(() => {
  const mandatoryNames = hrStore.mandatoryDocuments.map(m => m.name);
  return employee.value.documents?.filter(d => !mandatoryNames.includes(d.type)) || [];
});

function triggerFileInput(id) {
  const el = document.getElementById(`file-input-${id}`);
  if (el) el.click();
}

onMounted(() => {
  hrStore.fetchMandatoryDocuments();
  if (employee.value) {
    hrStore.fetchEmployeeDocuments(employee.value.id);
  }
});
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
          My Documents
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

      <!-- TAB 3: MY DOCUMENTS -->
      <template v-if="activeTab === 'documents'">
        <!-- Left: Mandatory Documents Checklist -->
        <div class="col-8">
          <div class="glass-card">
            <h3 class="mb-4">Mandatory Required Documents</h3>
            <p class="text-secondary text-sm mb-6">The following documents are required for organizational verification. Items highlighted in red are pending upload.</p>
            
            <div class="mandatory-docs-list">
              <div v-for="mDoc in hrStore.mandatoryDocuments" :key="mDoc.id" class="mandatory-doc-item mb-4 p-4 rounded border" :style="getUploadedDoc(mDoc.name) ? 'border-color: rgba(34, 197, 94, 0.2); background: rgba(34, 197, 94, 0.02);' : 'border-color: rgba(239, 68, 68, 0.2); background: rgba(239, 68, 68, 0.02);'">
                <div class="flex items-center justify-between gap-4">
                  <div>
                    <h4 class="font-bold text-sm m-0">{{ mDoc.name }}</h4>
                    <div class="mt-2 flex items-center gap-2">
                      <span v-if="getUploadedDoc(mDoc.name)" class="badge badge-success text-xs flex items-center gap-1">
                        <IconHelper name="check" size="12" />
                        Uploaded: {{ getUploadedDoc(mDoc.name).name }}
                      </span>
                      <span v-else class="badge badge-danger text-xs font-semibold" style="background-color: rgba(239, 68, 68, 0.1); color: #ef4444; border: 1px solid rgba(239, 68, 68, 0.2); padding: 2px 6px; border-radius: 4px;">
                        ⚠️ Pending Upload
                      </span>
                    </div>
                  </div>
                  
                  <div class="flex items-center gap-3">
                    <template v-if="getUploadedDoc(mDoc.name)">
                      <button class="btn btn-secondary btn-sm flex items-center gap-1" @click="downloadRealDoc(getUploadedDoc(mDoc.name))" title="Download Document">
                        <IconHelper name="download" size="14" /> Download
                      </button>
                      <button class="btn btn-ghost btn-sm text-danger-btn" @click="deleteRealDoc(getUploadedDoc(mDoc.name).id)" title="Delete Document">
                        <IconHelper name="trash" size="14" />
                      </button>
                    </template>
                    <template v-else>
                      <div class="inline-upload-form flex items-center gap-2">
                        <input
                          type="file"
                          :id="`file-input-${mDoc.id}`"
                          style="display: none"
                          @change="e => handleMandatoryFileUpload(e, mDoc.name)"
                        />
                        <button class="btn btn-primary btn-sm flex items-center gap-1" @click="triggerFileInput(mDoc.id)">
                          <IconHelper name="upload" size="14" /> Upload Document
                        </button>
                      </div>
                    </template>
                  </div>
                </div>
              </div>
              <div v-if="hrStore.mandatoryDocuments.length === 0" class="text-center p-8 text-secondary">
                No mandatory documents required by HR.
              </div>
            </div>
          </div>
        </div>

        <!-- Right: Other Custom Documents Upload & List -->
        <div class="col-4">
          <div class="glass-card mb-6">
            <h3 class="mb-4">Upload Other Document</h3>
            <div class="form-group">
              <label class="form-label">Document Category / Title</label>
              <input type="text" v-model="docType" class="form-control" placeholder="e.g. Previous Payslip, Resume" />
            </div>
            
            <div
              class="upload-zone"
              :class="{ active: isDragging }"
              @dragover.prevent="isDragging = true"
              @dragleave="isDragging = false"
              @drop.prevent="handleFileDrop"
              @click="$refs.otherFileInput.click()"
            >
              <input
                type="file"
                ref="otherFileInput"
                style="display: none"
                @change="handleOtherFileUpload"
              />
              <div class="mb-3">
                <IconHelper name="upload" size="32" color="var(--primary-color)" />
              </div>
              <p class="font-semibold text-sm">Drag & Drop files here</p>
              <p class="text-xs text-secondary mt-1">or click to browse files (PDF, JPG, PNG)</p>
            </div>
          </div>

          <div class="glass-card">
            <h3 class="mb-4">Other Uploaded Documents</h3>
            <div class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Type</th>
                    <th class="text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="doc in otherDocuments" :key="doc.id">
                    <td class="text-truncate" style="max-width: 120px;" :title="doc.name">
                      <div class="flex items-center gap-1">
                        <IconHelper name="file-text" size="14" color="#94a3b8" />
                        <span class="text-xs font-semibold">{{ doc.name }}</span>
                      </div>
                    </td>
                    <td><span class="badge badge-muted text-xs">{{ doc.type }}</span></td>
                    <td class="text-right">
                      <div class="flex justify-end gap-1">
                        <button class="btn btn-ghost btn-sm p-1" @click="downloadRealDoc(doc)">
                          <IconHelper name="download" size="13" />
                        </button>
                        <button class="btn btn-ghost btn-sm text-danger-btn p-1" @click="deleteRealDoc(doc.id)">
                          <IconHelper name="trash" size="13" />
                        </button>
                      </div>
                    </td>
                  </tr>
                  <tr v-if="otherDocuments.length === 0">
                    <td colspan="3" class="text-center text-xs text-secondary p-4">No other documents uploaded.</td>
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
