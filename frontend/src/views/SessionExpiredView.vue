<script setup>
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/authStore.js';
import IconHelper from '../components/IconHelper.vue';

const router = useRouter();
const authStore = useAuthStore();

function handleLoginAgain() {
  authStore.logout();
  router.push('/login');
}
</script>

<template>
  <div class="expired-page">
    <div class="bg-glow bg-glow-1"></div>
    <div class="bg-glow bg-glow-2"></div>

    <div class="expired-wrapper">
      <div class="glass-card expired-card text-center">
        <div class="logo-box mb-4 bg-warning-light">
          <IconHelper name="clock" size="36" color="#f59e0b" />
        </div>
        <h2 class="mb-2">Session Expired</h2>
        <p class="text-secondary text-sm mb-6">
          Your session was automatically invalidated after 20 minutes of inactivity to safeguard company resources.
        </p>

        <button class="btn btn-primary btn-full mb-4" @click="handleLoginAgain">
          Login Again
        </button>

        <p class="text-xs text-muted">
          Secured by Keycloak SAML Federated Authentication
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.expired-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background-color: var(--bg-main);
  padding: 24px;
}

.expired-wrapper {
  width: 100%;
  max-width: 420px;
  z-index: 10;
}

.expired-card {
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5), var(--shadow-glass);
  padding: 36px 24px;
}

.logo-box {
  margin: 0 auto;
  width: 64px;
  height: 64px;
  border-radius: var(--radius-md);
  background-color: var(--primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
}

.bg-warning-light {
  background-color: var(--warning-light);
}

.btn-full {
  width: 100%;
}
.text-center { text-align: center; }
.text-sm { font-size: 13px; }
.text-xs { font-size: 11px; }
.mb-2 { margin-bottom: 8px; }
.mb-4 { margin-bottom: 16px; }
.mb-6 { margin-bottom: 24px; }
</style>
