<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/authStore.js';
import { useHrStore } from '../stores/hrStore.js';
import IconHelper from '../components/IconHelper.vue';

const router = useRouter();
const authStore = useAuthStore();
const hrStore = useHrStore();

const email = ref('jane.doe@company.com');
const password = ref('password123');
const errorMsg = ref('');
const showSsoModal = ref(false);
const ssoProvider = ref('');
const isSsoLoading = ref(false);

function handleLocalLogin() {
  if (!email.value || !password.value) {
    errorMsg.value = 'Please enter both email and password';
    return;
  }
  
  // Find matching employee by email to sync login profile
  const matchedEmp = hrStore.employees.find(e => e.email === email.value);
  if (matchedEmp) {
    authStore.user = {
      id: matchedEmp.id,
      employeeCode: matchedEmp.employeeCode,
      fullName: matchedEmp.fullName,
      email: matchedEmp.email,
      role: matchedEmp.role,
      department: matchedEmp.department,
      designation: matchedEmp.designation
    };
    authStore.activeRole = matchedEmp.role;
  } else {
    // Default mock user
    authStore.user = {
      id: 'emp-custom',
      employeeCode: 'EMP2026999',
      fullName: 'External Guest',
      email: email.value,
      role: 'EMPLOYEE',
      department: 'Guest Office',
      designation: 'Contractor'
    };
    authStore.activeRole = 'EMPLOYEE';
  }

  authStore.login(email.value, password.value);
  hrStore.addLog(email.value, 'Auth', 'LOGIN', 'Successful credential login');
  router.push('/profile');
}

function triggerSSO(provider) {
  ssoProvider.value = provider;
  showSsoModal.value = true;
  isSsoLoading.value = true;
  
  // Simulate redirect popup sequence
  setTimeout(() => {
    isSsoLoading.value = false;
  }, 2000);
}

function completeSSO() {
  // Sync to a mock employee for SSO
  const matchedEmp = hrStore.employees[0]; // Jane Doe
  authStore.user = {
    id: matchedEmp.id,
    employeeCode: matchedEmp.employeeCode,
    fullName: matchedEmp.fullName,
    email: matchedEmp.email,
    role: matchedEmp.role,
    department: matchedEmp.department,
    designation: matchedEmp.designation
  };
  authStore.activeRole = matchedEmp.role;
  authStore.loginSSO(ssoProvider.value);
  
  hrStore.addLog(matchedEmp.email, 'Auth', 'SSO_LOGIN', `Logged in via SAML SSO (${ssoProvider.value})`);
  
  showSsoModal.value = false;
  router.push('/profile');
}
</script>

<template>
  <div class="login-page">
    <!-- BACKGROUND BLUR GLOWS -->
    <div class="bg-glow bg-glow-1"></div>
    <div class="bg-glow bg-glow-2"></div>

    <div class="login-wrapper">
      <div class="login-branding">
        <div class="logo-box mb-4">
          <IconHelper name="shield" size="36" color="#818cf8" />
        </div>
        <h1>Excellathon ESSP</h1>
        <p>Enterprise Employee Self-Service Portal</p>
      </div>

      <div class="glass-card login-card">
        <h2 class="mb-6 text-center">Sign In</h2>
        
        <div v-if="errorMsg" class="alert alert-error mb-4" style="min-width: 100%">
          <span>{{ errorMsg }}</span>
        </div>

        <form @submit.prevent="handleLocalLogin">
          <div class="form-group">
            <label class="form-label">Email Address</label>
            <input
              type="email"
              v-model="email"
              class="form-control"
              placeholder="name@company.com"
              required
            />
          </div>

          <div class="form-group">
            <div class="flex justify-between items-center mb-2">
              <label class="form-label m-0">Password</label>
              <router-link to="/forgot-password" class="text-xs">Forgot password?</router-link>
            </div>
            <input
              type="password"
              v-model="password"
              class="form-control"
              placeholder="••••••••"
              required
            />
          </div>

          <button type="submit" class="btn btn-primary btn-full mb-4">
            Sign In with Email
          </button>
        </form>

        <div class="separator-text mb-4">
          <span>Or Continue With</span>
        </div>

        <div class="sso-grid">
          <button type="button" class="btn btn-secondary flex-1" @click="triggerSSO('Azure AD')">
            <img src="https://upload.wikimedia.org/wikipedia/commons/4/44/Microsoft_logo.svg" alt="Microsoft" class="sso-logo" />
            Azure AD sso
          </button>
          
          <button type="button" class="btn btn-secondary flex-1" @click="triggerSSO('Okta')">
            <img src="https://upload.wikimedia.org/wikipedia/commons/5/5c/Okta_logo.svg" alt="Okta" class="sso-logo" />
            Okta sso
          </button>
        </div>
      </div>
    </div>

    <!-- SSO POPUP DIALOG MODAL -->
    <div class="modal-overlay" v-if="showSsoModal">
      <div class="modal-content text-center" style="max-width: 420px;">
        <div class="modal-body p-6">
          <div v-if="isSsoLoading" class="p-8">
            <div class="spinner mb-4"></div>
            <h3>Connecting to {{ ssoProvider }}...</h3>
            <p class="text-secondary text-sm">Federating SAML SSO security tokens</p>
          </div>
          <div v-else class="p-6">
            <div class="mb-4">
              <IconHelper name="check-circle" size="48" color="#10b981" />
            </div>
            <h3>SAML Handshake Success</h3>
            <p class="text-secondary text-sm mb-6">Verified authentication token issued by {{ ssoProvider }}.</p>
            <div class="user-pill mb-6 justify-center">
              <img :src="hrStore.employees[0].photo" alt="Jane" class="user-avatar-sm" />
              <div class="text-left">
                <div class="font-bold text-sm">{{ hrStore.employees[0].fullName }}</div>
                <div class="text-xs text-secondary">{{ hrStore.employees[0].email }}</div>
              </div>
            </div>
            <button class="btn btn-primary btn-full" @click="completeSSO">Redirect to Portal</button>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background-color: var(--bg-main);
  padding: 24px;
}

.login-wrapper {
  width: 100%;
  max-width: 440px;
  z-index: 10;
}

.login-branding {
  text-align: center;
  margin-bottom: 30px;
}
.login-branding h1 {
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.025em;
  margin-bottom: 6px;
}
.login-branding p {
  color: var(--text-secondary);
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

.login-card {
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5), var(--shadow-glass);
}

.flex { display: flex; }
.justify-between { justify-content: space-between; }
.justify-center { justify-content: center; }
.items-center { align-items: center; }
.text-xs { font-size: 12px; }
.text-sm { font-size: 13px; }
.m-0 { margin: 0; }
.mb-2 { margin-bottom: 8px; }
.mb-4 { margin-bottom: 16px; }
.mb-6 { margin-bottom: 24px; }
.p-6 { padding: 24px; }
.p-8 { padding: 32px; }

.separator-text {
  display: flex;
  align-items: center;
  text-align: center;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 500;
}
.separator-text::before, .separator-text::after {
  content: '';
  flex: 1;
  border-bottom: 1px solid var(--border-color);
}
.separator-text::before {
  margin-right: 12px;
}
.separator-text::after {
  margin-left: 12px;
}

.sso-grid {
  display: flex;
  gap: 12px;
}
.sso-logo {
  width: 16px;
  height: 16px;
  margin-right: 8px;
}

/* Spinner styling */
.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--border-color);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 1s infinite linear;
  margin: 0 auto;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

.btn-full {
  width: 100%;
}
</style>
