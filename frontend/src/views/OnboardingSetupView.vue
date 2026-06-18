<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/authStore.js';
import { useHrStore } from '../stores/hrStore.js';
import { apiRequest } from '../utils/api.js';
import IconHelper from '../components/IconHelper.vue';

const router = useRouter();
const authStore = useAuthStore();
const hrStore = useHrStore();

// Form state
const school = ref('');
const college = ref('');
const experience = ref('');
const uploadedFiles = ref([]);
const isDragging = ref(false);
const errorMsg = ref('');
const isSubmitting = ref(false);

const onboardingStatus = computed(() => {
  return authStore.user.onboardingStatus || 'PENDING_DETAILS';
});

// Fetch current details if user reloads
onMounted(async () => {
  if (authStore.user.id) {
    try {
      const profile = await apiRequest(`/api/v1/employees/code/${authStore.user.employeeCode}`);
      if (profile) {
        school.value = profile.school || '';
        college.value = profile.college || '';
        experience.value = profile.experience || '';
        if (profile.certificates) {
          uploadedFiles.value = profile.certificates.split(',').filter(Boolean);
        }
        // Sync to local store
        authStore.user.onboardingStatus = profile.onboardingStatus;
      }
    } catch (e) {
      console.warn('Failed to load profile for onboarding setup:', e);
    }
  }
});

function handleFileDrop(e) {
  isDragging.value = false;
  const files = e.dataTransfer.files;
  for (let i = 0; i < files.length; i++) {
    uploadedFiles.value.push(files[i].name);
  }
}

function handleFileSelect(e) {
  const files = e.target.files;
  for (let i = 0; i < files.length; i++) {
    uploadedFiles.value.push(files[i].name);
  }
}

function removeFile(index) {
  uploadedFiles.value.splice(index, 1);
}

async function handleSubmitDetails() {
  if (!school.value || !college.value) {
    errorMsg.value = 'School and College background details are required.';
    return;
  }

  isSubmitting.value = true;
  errorMsg.value = '';

  try {
    const data = {
      school: school.value,
      college: college.value,
      experience: experience.value || 'None',
      certificates: uploadedFiles.value.join(','),
      onboardingStatus: 'PENDING_APPROVAL',
      onboardingPercent: 50
    };

    // Update details in DB
    const response = await apiRequest(`/api/v1/employees/${authStore.user.id}`, {
      method: 'PUT',
      body: JSON.stringify({
        ...data,
        name: authStore.user.fullName,
        email: authStore.user.email,
        employeeCode: authStore.user.employeeCode,
        joiningDate: new Date().toISOString().split('T')[0] // dummy required field
      })
    });

    // Sync auth store status
    authStore.user.onboardingStatus = 'PENDING_APPROVAL';
    
    // Add audit log
    hrStore.addLog(authStore.user.email, 'Onboarding', 'SUBMIT_BACKGROUND_DETAILS', 'Submitted background details and certificates for verification');
    
    window.showPortalToast('Background details submitted successfully. HR will review it soon.', 'success');
  } catch (e) {
    errorMsg.value = e.message || 'Failed to submit details.';
  } finally {
    isSubmitting.value = false;
  }
}

async function checkApprovalStatus() {
  try {
    const profile = await apiRequest(`/api/v1/employees/code/${authStore.user.employeeCode}`);
    if (profile) {
      authStore.user.onboardingStatus = profile.onboardingStatus;
      if (profile.onboardingStatus === 'APPROVED') {
        window.showPortalToast('Congratulations! Your onboarding profile has been approved.', 'success');
        router.push('/profile');
      } else {
        window.showPortalToast('Your profile is still under review by the HR team.', 'info');
      }
    }
  } catch (e) {
    window.showPortalToast('Error checking approval status.', 'error');
  }
}

function handleLogout() {
  authStore.logout();
  router.push('/login');
}
</script>

<template>
  <div class="onboarding-setup-page">
    <div class="bg-glow bg-glow-1"></div>
    <div class="bg-glow bg-glow-2"></div>

    <div class="onboarding-wrapper">
      <div class="branding mb-6 text-center">
        <div class="logo-box mb-4">
          <IconHelper name="shield" size="36" color="#818cf8" />
        </div>
        <h1>Excellathon ESSP</h1>
        <p>Employee Onboarding Verification</p>
      </div>

      <!-- STATE 1: FILL DETAILS -->
      <div v-if="onboardingStatus === 'PENDING_DETAILS'" class="glass-card main-card">
        <h2 class="mb-2">Background Verification Form</h2>
        <p class="text-secondary text-sm mb-6">Please provide your academic qualifications and previous professional experiences to activate your portal credentials.</p>

        <div v-if="errorMsg" class="alert alert-error mb-4" style="min-width: 100%">
          <span>{{ errorMsg }}</span>
        </div>

        <form @submit.prevent="handleSubmitDetails">
          <div class="form-group">
            <label class="form-label">School Details (High School, Board, Year) *</label>
            <input
              type="text"
              v-model="school"
              class="form-control"
              placeholder="e.g. St. Xavier Senior Secondary School (CBSE) - 2018"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label">College/University (Degree, Specialization, Year) *</label>
            <input
              type="text"
              v-model="college"
              class="form-control"
              placeholder="e.g. Birla Institute of Technology, B.E. Computer Science - 2022"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label">Previous Work Experience (Leave empty if fresher)</label>
            <textarea
              v-model="experience"
              class="form-control"
              placeholder="e.g. Software Engineer Intern at ABC Solutions (6 months), worked on Spring Boot & Vue.js application architectures."
              rows="3"
            ></textarea>
          </div>

          <!-- Document Upload Widget -->
          <div class="form-group">
            <label class="form-label">Upload Academic & Experience Certificates</label>
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
                multiple
                @change="handleFileSelect"
              />
              <div class="mb-3">
                <IconHelper name="upload" size="32" color="var(--primary-color)" />
              </div>
              <p class="font-semibold text-sm">Drag & Drop files here</p>
              <p class="text-xs text-secondary mt-1">or click to browse files (PDF, JPG, PNG)</p>
            </div>

            <!-- Uploaded file badges -->
            <div class="uploaded-files-list mt-4" v-if="uploadedFiles.length > 0">
              <div v-for="(file, index) in uploadedFiles" :key="index" class="file-chip">
                <IconHelper name="file-text" size="14" />
                <span class="file-name">{{ file }}</span>
                <button type="button" class="remove-btn" @click.stop="removeFile(index)">&times;</button>
              </div>
            </div>
          </div>

          <div class="flex gap-4 mt-6">
            <button type="button" class="btn btn-secondary flex-1" @click="handleLogout">
              Log Out
            </button>
            <button type="submit" class="btn btn-primary flex-2" :disabled="isSubmitting">
              <span v-if="isSubmitting" class="spinner-sm mr-2"></span>
              Submit Verification Details
            </button>
          </div>
        </form>
      </div>

      <!-- STATE 2: PENDING APPROVAL -->
      <div v-else-if="onboardingStatus === 'PENDING_APPROVAL'" class="glass-card main-card text-center py-8">
        <div class="status-icon-box mb-6">
          <IconHelper name="clock" size="48" color="#f59e0b" />
        </div>
        <h2 class="mb-2">Verification Under Review</h2>
        <p class="text-secondary text-sm mb-6 max-width-340">
          Your credentials and academic certificates have been submitted for verification. The HR department is currently reviewing your profile.
        </p>

        <div class="user-info-summary mb-6">
          <div class="info-row">
            <span class="lbl">Email:</span>
            <span class="val">{{ authStore.user.email }}</span>
          </div>
          <div class="info-row">
            <span class="lbl">Status:</span>
            <span class="val badge badge-warning">PENDING HR APPROVAL</span>
          </div>
        </div>

        <div class="flex flex-col gap-3">
          <button class="btn btn-primary btn-full py-12" @click="checkApprovalStatus">
            Check Approval Status
          </button>
          <button class="btn btn-secondary btn-full py-12" @click="handleLogout">
            Log Out
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.onboarding-setup-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background-color: var(--bg-main);
  padding: 32px;
  overflow-y: auto;
}

.onboarding-wrapper {
  width: 100%;
  max-width: 500px;
  z-index: 10;
  margin: auto;
}

.logo-box {
  margin: 0 auto;
  width: 56px;
  height: 56px;
  border-radius: var(--radius-md);
  background-color: var(--primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
}

.branding h1 {
  font-size: 26px;
  font-weight: 800;
  margin-bottom: 6px;
}

.branding p {
  color: var(--text-secondary);
}

.main-card {
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5), var(--shadow-glass);
}

.max-width-340 {
  max-width: 340px;
  margin: 0 auto;
}

.status-icon-box {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background-color: rgba(245, 158, 11, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
}

.upload-zone {
  border: 2px dashed var(--border-color);
  border-radius: var(--radius-md);
  padding: 24px;
  text-align: center;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.02);
  transition: all var(--transition-normal);
}

.upload-zone:hover, .upload-zone.active {
  border-color: var(--primary-color);
  background: rgba(129, 140, 248, 0.05);
}

.uploaded-files-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.file-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  background-color: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--border-color);
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  font-size: 12px;
}

.file-name {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remove-btn {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 16px;
  padding: 0;
  margin-left: 4px;
}

.remove-btn:hover {
  color: var(--danger);
}

.user-info-summary {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 16px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
}
.info-row:last-child {
  margin-bottom: 0;
}

.info-row .lbl {
  color: var(--text-secondary);
}

.info-row .val {
  font-weight: 600;
}

.flex { display: flex; }
.flex-col { display: flex; flex-direction: column; }
.gap-3 { gap: 12px; }
.gap-4 { gap: 16px; }
.flex-1 { flex: 1; }
.flex-2 { flex: 2; }
.btn-full { width: 100%; }
.py-8 { padding-top: 32px; padding-bottom: 32px; }
.py-12 { padding-top: 12px; padding-bottom: 12px; }
.mr-2 { margin-right: 8px; }

/* Micro spinners */
.spinner-sm {
  width: 16px;
  height: 16px;
  border: 2px solid var(--border-color);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s infinite linear;
  display: inline-block;
  vertical-align: middle;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
