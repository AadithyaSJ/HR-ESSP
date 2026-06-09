<script setup>
import { ref } from 'vue';
import { useHrStore } from '../stores/hrStore.js';
import IconHelper from '../components/IconHelper.vue';

const hrStore = useHrStore();

const email = ref('');
const password = ref('');
const confirmPassword = ref('');
const step = ref(1); // 1 = Enter Email, 2 = Check Email/Set Password, 3 = Success
const errorMsg = ref('');

function handleSendLink() {
  if (!email.value) {
    errorMsg.value = 'Please enter your email';
    return;
  }
  
  // Simulate checking register email
  const emp = hrStore.employees.find(e => e.email === email.value);
  if (!emp && email.value !== 'jane.doe@company.com') {
    errorMsg.value = 'Email address not found in system record';
    return;
  }
  
  errorMsg.value = '';
  hrStore.addLog(email.value, 'Auth', 'FORGOT_PASSWORD_REQUEST', 'Triggered password reset email');
  hrStore.addNotification('Password Reset Link Sent', `Reset link for ${email.value} dispatched via AWS SES.`, 'System');
  
  step.value = 2;
}

function handleResetPassword() {
  if (!password.value || !confirmPassword.value) {
    errorMsg.value = 'Please fill all password fields';
    return;
  }
  if (password.value !== confirmPassword.value) {
    errorMsg.value = 'Passwords do not match';
    return;
  }
  
  errorMsg.value = '';
  hrStore.addLog(email.value, 'Auth', 'FORGOT_PASSWORD_RESET', 'Successfully reset password');
  
  step.value = 3;
}
</script>

<template>
  <div class="forgot-page">
    <div class="bg-glow bg-glow-1"></div>
    <div class="bg-glow bg-glow-2"></div>

    <div class="forgot-wrapper">
      <div class="glass-card forgot-card">
        <!-- STEP 1: ENTER EMAIL -->
        <div v-if="step === 1">
          <div class="text-center mb-6">
            <div class="logo-box mb-4">
              <IconHelper name="lock" size="32" color="#818cf8" />
            </div>
            <h2>Forgot Password?</h2>
            <p class="text-secondary text-sm">Enter your email and we'll send you a password reset link.</p>
          </div>

          <div v-if="errorMsg" class="alert alert-error mb-4" style="min-width: 100%">
            <span>{{ errorMsg }}</span>
          </div>

          <form @submit.prevent="handleSendLink">
            <div class="form-group">
              <label class="form-label">Registered Email</label>
              <input
                type="email"
                v-model="email"
                class="form-control"
                placeholder="name@company.com"
                required
              />
            </div>

            <button type="submit" class="btn btn-primary btn-full mb-4">
              Send Reset Link
            </button>

            <router-link to="/login" class="btn btn-ghost btn-full">
              Back to Login
            </router-link>
          </form>
        </div>

        <!-- STEP 2: SET NEW PASSWORD -->
        <div v-else-if="step === 2">
          <div class="text-center mb-6">
            <div class="logo-box mb-4 bg-success-light">
              <IconHelper name="check-circle" size="32" color="#10b981" />
            </div>
            <h2>Link Sent via AWS SES</h2>
            <p class="text-secondary text-sm">
              A secure password reset link was dispatched to <b>{{ email }}</b>.
              Please verify your inbox and input your new credentials below.
            </p>
          </div>

          <div v-if="errorMsg" class="alert alert-error mb-4" style="min-width: 100%">
            <span>{{ errorMsg }}</span>
          </div>

          <form @submit.prevent="handleResetPassword">
            <div class="form-group">
              <label class="form-label">New Password</label>
              <input
                type="password"
                v-model="password"
                class="form-control"
                placeholder="••••••••"
                required
              />
            </div>

            <div class="form-group">
              <label class="form-label">Confirm New Password</label>
              <input
                type="password"
                v-model="confirmPassword"
                class="form-control"
                placeholder="••••••••"
                required
              />
            </div>

            <button type="submit" class="btn btn-success btn-full mb-4">
              Update Password
            </button>

            <button type="button" class="btn btn-ghost btn-full" @click="step = 1">
              Change Email
            </button>
          </form>
        </div>

        <!-- STEP 3: RESET SUCCESS -->
        <div v-else-if="step === 3" class="text-center">
          <div class="logo-box mb-4 bg-success-light">
            <IconHelper name="check-circle" size="32" color="#10b981" />
          </div>
          <h2 class="mb-2">Password Updated</h2>
          <p class="text-secondary text-sm mb-6">Your password has been changed successfully. You can now login with your new credentials.</p>
          
          <router-link to="/login" class="btn btn-primary btn-full">
            Proceed to Login
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.forgot-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background-color: var(--bg-main);
  padding: 24px;
}

.forgot-wrapper {
  width: 100%;
  max-width: 440px;
  z-index: 10;
}

.forgot-card {
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5), var(--shadow-glass);
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

.bg-success-light {
  background-color: var(--success-light);
}

.btn-full {
  width: 100%;
}
.text-sm { font-size: 13px; }
.mb-2 { margin-bottom: 8px; }
.mb-4 { margin-bottom: 16px; }
.mb-6 { margin-bottom: 24px; }
</style>
