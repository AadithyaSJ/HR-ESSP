import { defineStore } from 'pinia';
import { ref } from 'vue';
import { apiRequest } from '../utils/api';

export const useAuthStore = defineStore('auth', () => {
  const user = ref({
    id: null,
    employeeCode: null,
    fullName: 'Guest User',
    email: '',
    role: 'EMPLOYEE',
    department: '',
    designation: ''
  });

  const isAuthenticated = ref(false); // Change default to false for real security
  const activeRole = ref('EMPLOYEE');
  
  // Session timers
  const idleTimeSeconds = ref(0);
  const timeLimitSeconds = ref(1200); // 20 minutes
  const warningLimitSeconds = ref(1080); // 18 minutes
  const timerSpeed = ref(1);
  
  let timerInterval = null;

  function resetIdleTimer() {
    idleTimeSeconds.value = 0;
  }

  function startIdleTimer(router) {
    if (timerInterval) clearInterval(timerInterval);
    timerInterval = setInterval(() => {
      if (!isAuthenticated.value) return;
      
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

  async function login(email, password) {
    try {
      const data = await apiRequest('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password })
      });
      
      localStorage.setItem('jwt_token', data.token);
      
      // Fetch employee profile details to populate user model
      let profile = null;
      try {
        profile = await apiRequest(`/api/v1/employees/code/${data.employeeCode}`);
      } catch (e) {
        // Fallback profile if employee not created yet
      }

      user.value = {
        id: profile?.id || 'emp-id',
        employeeCode: data.employeeCode,
        fullName: data.fullName,
        email: data.email,
        role: data.role,
        department: profile?.department || '',
        designation: profile?.designation || ''
      };

      activeRole.value = data.role;
      isAuthenticated.value = true;
      resetIdleTimer();
      
      try {
        const { useHrStore } = await import('./hrStore');
        const hrStore = useHrStore();
        await hrStore.syncAll(profile?.id, profile?.id);
      } catch (e) {
        console.warn('Sync failed on login:', e);
      }

      return true;
    } catch (e) {
      isAuthenticated.value = false;
      throw e;
    }
  }

  async function loginSSO(provider) {
    try {
      const data = await apiRequest(`/api/auth/sso-login?provider=${provider}`, {
        method: 'POST'
      });
      
      localStorage.setItem('jwt_token', data.token);
      
      let profile = null;
      try {
        profile = await apiRequest(`/api/v1/employees/code/${data.employeeCode}`);
      } catch (e) {
        // fallback
      }

      user.value = {
        id: profile?.id || 'emp-id',
        employeeCode: data.employeeCode,
        fullName: data.fullName,
        email: data.email,
        role: data.role,
        department: profile?.department || '',
        designation: profile?.designation || ''
      };

      activeRole.value = data.role;
      isAuthenticated.value = true;
      resetIdleTimer();

      try {
        const { useHrStore } = await import('./hrStore');
        const hrStore = useHrStore();
        await hrStore.syncAll(profile?.id, profile?.id);
      } catch (e) {
        console.warn('Sync failed on SSO login:', e);
      }

      return true;
    } catch (e) {
      isAuthenticated.value = false;
      throw e;
    }
  }

  function logout() {
    localStorage.removeItem('jwt_token');
    user.value = {
      id: null,
      employeeCode: null,
      fullName: 'Guest User',
      email: '',
      role: 'EMPLOYEE',
      department: '',
      designation: ''
    };
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
