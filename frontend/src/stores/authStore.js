import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useAuthStore = defineStore('auth', () => {
  const user = ref({
    id: 'emp-101',
    employeeCode: 'EMP2026101',
    fullName: 'Jane Doe',
    email: 'jane.doe@company.com',
    role: 'EMPLOYEE', // DEFAULT ROLE
    department: 'Engineering',
    designation: 'Senior Frontend Developer'
  });

  const isAuthenticated = ref(true); // Pre-authenticated for easy testing
  const activeRole = ref('EMPLOYEE'); // Active role in switcher: EMPLOYEE, MANAGER, HR_ADMIN, FINANCE_ADMIN, SYSTEM_ADMIN
  
  // Session timers
  const idleTimeSeconds = ref(0);
  const timeLimitSeconds = ref(1200); // 20 minutes
  const warningLimitSeconds = ref(1080); // 18 minutes
  const timerSpeed = ref(1); // multiplier: 1x, or 60x for demo (1s real time = 1 min simulation)
  
  let timerInterval = null;

  function resetIdleTimer() {
    idleTimeSeconds.value = 0;
  }

  function startIdleTimer(router) {
    if (timerInterval) clearInterval(timerInterval);
    timerInterval = setInterval(() => {
      if (!isAuthenticated.value) return;
      
      // Advance timer by speed factor
      idleTimeSeconds.value += timerSpeed.value;
      
      if (idleTimeSeconds.value >= timeLimitSeconds.value) {
        logout();
        if (router) {
          router.push('/session-expired');
        }
      }
    }, 1000);
  }

  function stopIdleTimer() {
    if (timerInterval) {
      clearInterval(timerInterval);
      timerInterval = null;
    }
  }

  function setRole(role) {
    activeRole.value = role;
    resetIdleTimer();
  }

  function login(_email, _password) {
    isAuthenticated.value = true;
    resetIdleTimer();
    return true;
  }

  function loginSSO(_provider) {
    isAuthenticated.value = true;
    resetIdleTimer();
    return true;
  }

  function logout() {
    isAuthenticated.value = false;
    stopIdleTimer();
  }

  return {
    user,
    isAuthenticated,
    activeRole,
    idleTimeSeconds,
    timeLimitSeconds,
    warningLimitSeconds,
    timerSpeed,
    resetIdleTimer,
    startIdleTimer,
    stopIdleTimer,
    setRole,
    login,
    loginSSO,
    logout
  };
});
