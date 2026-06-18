<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/authStore.js';
import { useHrStore } from '../stores/hrStore.js';
import IconHelper from '../components/IconHelper.vue';
import { API_BASE_URL } from '../config';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const hrStore = useHrStore();

const activeTab = ref(route.meta.tab || 'my-leaves');

// Apply Leave Form State
const applyType = ref('Annual');
const applyFromDate = ref('');
const applyToDate = ref('');
const applyReason = ref('');

// Filter Requests
const requestFilter = ref('All');

// Approvals dialog inputs
const showReviewModal = ref(false);
const activeRequest = ref(null);
const reviewComment = ref('');
const reviewAction = ref(''); // APPROVE or REJECT
const errorMsg = ref('');

// Calendar state
const calMonth = ref(new Date().getMonth()); // 0-11
const calYear = ref(2026);
const calendarCountry = ref('India');
const calendarDeptFilter = ref('All');

// Policy Config HR inputs

// Public Holidays state
const holidayCountry = ref('India');
const newHolidayName = ref('');
const newHolidayDate = ref('');

const selectedFile = ref(null);
const fileInput = ref(null);

function handleFileChange(event) {
  const file = event.target.files[0];
  if (file) {
    selectedFile.value = file;
  } else {
    selectedFile.value = null;
  }
}

async function triggerDownload(filePath, fileName) {
  try {
    const token = localStorage.getItem('jwt_token');
    const resolvedBase = API_BASE_URL.endsWith('/') ? API_BASE_URL.slice(0, -1) : API_BASE_URL;
    const url = `${resolvedBase}/api/v1/leaves/download?path=${encodeURIComponent(filePath)}`;
    
    const response = await fetch(url, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (!response.ok) {
      throw new Error('Download failed');
    }
    
    const blob = await response.blob();
    const downloadUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.setAttribute('download', fileName || 'attachment');
    document.body.appendChild(link);
    link.click();
    link.parentNode.removeChild(link);
    window.URL.revokeObjectURL(downloadUrl);
  } catch (err) {
    window.showPortalToast('Failed to download attachment: ' + err.message, 'error');
  }
}

const isLoading = ref(false);

onMounted(async () => {
  activeTab.value = route.meta.tab || 'my-leaves';
  isLoading.value = true;
  try {
    await hrStore.fetchEmployees();
    await hrStore.fetchPublicHolidays();
    if (authStore.user) {
      await hrStore.fetchLeaves(authStore.user.id);
    }
    await new Promise(r => setTimeout(r, 500));
  } finally {
    isLoading.value = false;
  }
});

function changeTab(tab) {
  activeTab.value = tab;
  router.push(tab === 'my-leaves' ? '/leave' : tab === 'calendar' ? '/leave/calendar' : tab === 'approvals' ? '/manager/leave' : '/admin/leave-policy');
}

// Find balances for active user
const myBalances = computed(() => {
  return hrStore.leaveBalances[authStore.user.employeeCode] || [];
});

// Calculate requested days
const calculatedDays = computed(() => {
  if (!applyFromDate.value || !applyToDate.value) return 0;
  return hrStore.calculateLeaveDaysFrontend(authStore.user.id, applyFromDate.value, applyToDate.value);
});

// Submitting leave form
async function handleApplyLeave() {
  if (!applyFromDate.value || !applyToDate.value || !applyReason.value) {
    window.showPortalToast('Please complete all form fields', 'error');
    return;
  }
  if (calculatedDays.value <= 0) {
    window.showPortalToast('End date must be on or after start date and exclude weekends/holidays', 'error');
    return;
  }

  // Check balance limit
  const activeBal = myBalances.value.find(b => b.type === applyType.value);
  if (activeBal && activeBal.remaining < calculatedDays.value) {
    window.showPortalToast(`Insufficient ${applyType.value} leave balance. Remaining: ${activeBal.remaining}`, 'error');
    return;
  }

  // Check overlap
  const overlap = hrStore.leaveRequests.some(r => {
    if (r.employeeCode !== authStore.user.employeeCode || r.status === 'CANCELLED' || r.status === 'REJECTED') return false;
    const startA = new Date(r.fromDate);
    const endA = new Date(r.toDate);
    const startB = new Date(applyFromDate.value);
    const endB = new Date(applyToDate.value);
    return startB <= endA && startA <= endB;
  });

  if (overlap) {
    window.showPortalToast('Leave request overlaps with another submission', 'error');
    return;
  }

  let attachmentName = null;
  let attachmentPath = null;

  if (selectedFile.value) {
    try {
      window.showPortalToast('Uploading attachment...', 'info');
      const uploadRes = await hrStore.uploadLeaveAttachment(selectedFile.value);
      if (uploadRes) {
        attachmentName = uploadRes.fileName;
        attachmentPath = uploadRes.filePath;
      }
    } catch (e) {
      window.showPortalToast('File upload failed. Leave application aborted.', 'error');
      return;
    }
  }

  const payload = {
    employeeCode: authStore.user.employeeCode,
    fullName: authStore.user.fullName,
    department: authStore.user.department,
    leaveType: applyType.value,
    fromDate: applyFromDate.value,
    toDate: applyToDate.value,
    daysRequested: calculatedDays.value,
    reason: applyReason.value,
    attachmentName,
    attachmentPath
  };

  try {
    await hrStore.applyLeave(payload, authStore.user.email);
    window.showPortalToast('Leave request submitted successfully', 'success');

    // Reset form
    applyFromDate.value = '';
    applyToDate.value = '';
    applyReason.value = '';
    selectedFile.value = null;
    if (fileInput.value) {
      fileInput.value.value = '';
    }
  } catch (err) {
    window.showPortalToast(err.message || 'Failed to submit leave request', 'error');
  }
}

// User requests list
const requestTypeFilter = ref('All');
const requestSearchQuery = ref('');

const filteredMyRequests = computed(() => {
  let reqs = hrStore.leaveRequests.filter(r => r.employeeCode === authStore.user.employeeCode);
  if (requestFilter.value !== 'All') {
    reqs = reqs.filter(r => r.status === requestFilter.value);
  }
  if (requestTypeFilter.value !== 'All') {
    reqs = reqs.filter(r => r.leaveType.toLowerCase() === requestTypeFilter.value.toLowerCase());
  }
  if (requestSearchQuery.value) {
    reqs = reqs.filter(r => r.reason && r.reason.toLowerCase().includes(requestSearchQuery.value.toLowerCase()));
  }
  return reqs;
});

function handleCancelRequest(id) {
  const cancelled = hrStore.cancelLeaveRequest(id, authStore.user.email);
  if (cancelled) {
    window.showPortalToast('Leave request cancelled', 'info');
  }
}

// Manager Approvals
const pendingApprovals = computed(() => {
  return hrStore.leaveRequests.filter(r => r.status === 'PENDING');
});

function openReview(req, action) {
  activeRequest.value = req;
  reviewAction.value = action;
  reviewComment.value = '';
  errorMsg.value = '';
  showReviewModal.value = true;
}

function handleConfirmReview() {
  if (reviewAction.value === 'REJECT' && !reviewComment.value) {
    errorMsg.value = 'Rejection reason is mandatory';
    return;
  }

  if (reviewAction.value === 'APPROVE') {
    hrStore.approveLeaveRequest(activeRequest.value.id, authStore.user.fullName, reviewComment.value, authStore.user.email);
    window.showPortalToast('Leave request approved', 'success');
  } else {
    hrStore.rejectLeaveRequest(activeRequest.value.id, authStore.user.fullName, reviewComment.value, authStore.user.email);
    window.showPortalToast('Leave request rejected', 'warning');
  }

  showReviewModal.value = false;
  activeRequest.value = null;
}

// Calendar Month Grid Generator
const calendarDays = computed(() => {
  const year = calYear.value;
  const month = calMonth.value;
  
  // First day of month
  const firstDayIndex = new Date(year, month, 1).getDay();
  // Total days in month
  const totalDays = new Date(year, month + 1, 0).getDate();
  // Total days in previous month
  const prevTotalDays = new Date(year, month, 0).getDate();

  const days = [];

  // Previous month placeholders
  for (let i = firstDayIndex - 1; i >= 0; i--) {
    days.push({
      day: prevTotalDays - i,
      isOtherMonth: true,
      dateStr: `${year}-${String(month).padStart(2, '0')}-${String(prevTotalDays - i).padStart(2, '0')}`,
      events: []
    });
  }

  // Current month days
  for (let i = 1; i <= totalDays; i++) {
    const formattedDate = `${year}-${String(month + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`;
    
    // Check public holidays
    const holidays = hrStore.publicHolidays.filter(h => h.country === calendarCountry.value && h.date === formattedDate);
    
    // Check approved leaves depending on role access
    const leaves = hrStore.leaveRequests.filter(r => {
      if (r.status !== 'APPROVED') return false;
      
      const start = new Date(r.fromDate);
      const end = new Date(r.toDate);
      const current = new Date(formattedDate);
      const isDateInRange = current >= start && current <= end;
      
      if (!isDateInRange) return false;
      
      // Filter by role view
      if (authStore.activeRole === 'EMPLOYEE') {
        return r.employeeCode === authStore.user.employeeCode;
      } else if (authStore.activeRole === 'MANAGER') {
        // Manager sees team + self
        return true; 
      } else {
        // HR Admin sees all
        if (calendarDeptFilter.value === 'All') return true;
        return r.department === calendarDeptFilter.value;
      }
    });

    const cellEvents = [];
    holidays.forEach(h => {
      cellEvents.push({ text: `🎉 ${h.name}`, type: 'holiday' });
    });
    leaves.forEach(l => {
      cellEvents.push({ text: `👤 ${l.fullName}`, type: 'leave' });
    });

    days.push({
      day: i,
      isOtherMonth: false,
      dateStr: formattedDate,
      events: cellEvents
    });
  }

  return days;
});

const monthNames = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

function nextMonth() {
  if (calMonth.value === 11) {
    calMonth.value = 0;
    calYear.value++;
  } else {
    calMonth.value++;
  }
}

function prevMonth() {
  if (calMonth.value === 0) {
    calMonth.value = 11;
    calYear.value--;
  } else {
    calMonth.value--;
  }
}

// Add Custom Holiday
async function handleAddHoliday() {
  if (!newHolidayName.value || !newHolidayDate.value) {
    window.showPortalToast('Please input holiday name and date', 'error');
    return;
  }

  try {
    await hrStore.addPublicHoliday({
      country: holidayCountry.value,
      name: newHolidayName.value,
      date: newHolidayDate.value
    }, authStore.user.email);

    window.showPortalToast(`Holiday "${newHolidayName.value}" added to calendar`, 'success');
    newHolidayName.value = '';
    newHolidayDate.value = '';
  } catch (e) {
    window.showPortalToast('Failed to add public holiday: ' + (e.message || e), 'error');
  }
}
</script>

<template>
  <div class="leave-hub">
    <!-- SUB-TABS -->
    <div class="tabs-container">
      <div class="tabs">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'my-leaves' }"
          @click="changeTab('my-leaves')"
        >
          My Leaves
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'calendar' }"
          @click="changeTab('calendar')"
        >
          Leave Calendar
        </button>
        <button
          v-if="['MANAGER', 'HR_ADMIN'].includes(authStore.activeRole)"
          class="tab-btn"
          :class="{ active: activeTab === 'approvals' }"
          @click="changeTab('approvals')"
        >
          Team Leave Approvals ({{ pendingApprovals.length }})
        </button>
        <button
          v-if="authStore.activeRole === 'HR_ADMIN'"
          class="tab-btn"
          :class="{ active: activeTab === 'policy' }"
          @click="changeTab('policy')"
        >
          Leave Policy Config
        </button>
      </div>
    </div>

    <!-- TAB 1: MY LEAVES -->
    <template v-if="activeTab === 'my-leaves'">
      <!-- Leave Balances cards -->
      <div class="summary-grid">
        <div v-for="bal in myBalances" :key="bal.type" class="summary-card glass-card">
          <div class="summary-card-icon" :class="bal.remaining > 5 ? 'bg-success-light text-success' : 'bg-warning-light text-warning'">
            <IconHelper name="calendar" size="20" />
          </div>
          <div>
            <div class="summary-card-label">{{ bal.type }} Leave Balance</div>
            <div class="summary-card-value">{{ bal.remaining }} <span class="text-xs font-normal">days left</span></div>
            <div class="text-xs text-muted mt-1">Accrued: {{ bal.accrualFrequency ? 'Monthly' : 'Accrued' }} • Carry: {{ bal.carryForward }}</div>
          </div>
        </div>
      </div>

      <div class="grid-12">
        <!-- Apply form -->
        <div class="col-4">
          <div class="glass-card">
            <h3 class="mb-6">Apply for Leave</h3>
            <form @submit.prevent="handleApplyLeave">
              <div class="form-group">
                <label class="form-label">Leave Type</label>
                <select v-model="applyType" class="form-control">
                  <option v-for="bal in myBalances" :key="bal.type" :value="bal.type">{{ bal.type }} Leave</option>
                  <option value="Unpaid">Unpaid Leave</option>
                </select>
              </div>

              <div class="form-group">
                <label class="form-label">From Date</label>
                <input type="date" v-model="applyFromDate" class="form-control" required />
              </div>

              <div class="form-group">
                <label class="form-label">To Date</label>
                <input type="date" v-model="applyToDate" class="form-control" required />
              </div>

              <div class="form-group mb-4" v-if="calculatedDays > 0">
                <div class="flex justify-between items-center text-xs p-3 bg-primary-light rounded border border-primary">
                  <span>Total Duration:</span>
                  <span class="font-bold font-mono">{{ calculatedDays }} Days</span>
                </div>
              </div>

              <div class="form-group">
                <label class="form-label">Reason / Notes</label>
                <textarea v-model="applyReason" class="form-control" placeholder="Specify reason..." required></textarea>
              </div>

              <div class="form-group">
                <label class="form-label">Attachment (Optional, e.g. Medical Certificate)</label>
                <input type="file" ref="fileInput" @change="handleFileChange" class="form-control" />
                <div v-if="selectedFile" class="text-xs text-secondary mt-1">
                  Selected: {{ selectedFile.name }} ({{ (selectedFile.size / 1024).toFixed(1) }} KB)
                </div>
              </div>

              <button type="submit" class="btn btn-primary btn-full">
                Submit Request
              </button>
            </form>
          </div>
        </div>

        <!-- Requests log -->
        <div class="col-8">
          <div class="glass-card">
            <div class="flex justify-between items-center mb-6">
              <h3>Leave Request History</h3>
              <div class="flex gap-2">
                <button
                  v-for="status in ['All', 'PENDING', 'APPROVED', 'REJECTED']"
                  :key="status"
                  class="btn btn-ghost btn-xs px-3"
                  :class="{ 'btn-secondary border': requestFilter === status }"
                  @click="requestFilter = status"
                >
                  {{ status }}
                </button>
              </div>
            </div>

            <!-- FILTERS PANEL -->
            <div class="grid-12 mb-4">
              <div class="col-6 form-group m-0">
                <label class="form-label text-xs">Search Reason</label>
                <input type="text" v-model="requestSearchQuery" class="form-control text-xs" placeholder="Search by reason..." />
              </div>
              <div class="col-6 form-group m-0">
                <label class="form-label text-xs">Leave Type</label>
                <select v-model="requestTypeFilter" class="form-control text-xs">
                  <option value="All">All Types</option>
                  <option value="Annual">Annual</option>
                  <option value="Sick">Sick</option>
                  <option value="Casual">Casual</option>
                  <option value="Unpaid">Unpaid</option>
                </select>
              </div>
            </div>

            <div class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>Type</th>
                    <th>Dates</th>
                    <th>Days</th>
                    <th>Reason</th>
                    <th>Status</th>
                    <th class="text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="isLoading" v-for="i in 3" :key="i" class="animate-pulse">
                    <td><div class="skeleton-line" style="width: 60%"></div></td>
                    <td><div class="skeleton-line-sm" style="width: 80%"></div></td>
                    <td><div class="skeleton-line-sm" style="width: 40%"></div></td>
                    <td><div class="skeleton-line-sm" style="width: 90%"></div></td>
                    <td><div class="skeleton-line-sm" style="width: 50%"></div></td>
                    <td></td>
                  </tr>
                  <template v-else>
                    <tr v-for="req in filteredMyRequests" :key="req.id">
                      <td class="font-semibold">{{ req.leaveType }}</td>
                      <td>
                        <div class="text-sm">{{ req.fromDate }} to {{ req.toDate }}</div>
                      </td>
                      <td class="font-mono text-sm">{{ req.daysRequested }}</td>
                      <td class="text-secondary max-width-200" :title="req.reason">
                        <div class="text-truncate">{{ req.reason }}</div>
                        <div v-if="req.attachmentName" class="mt-1">
                          <button
                            type="button"
                            class="btn btn-ghost btn-xs text-primary flex items-center gap-1 p-0"
                            style="border: none; background: transparent; cursor: pointer; color: var(--primary-color);"
                            @click="triggerDownload(req.attachmentPath, req.attachmentName)"
                          >
                            <IconHelper name="download" size="12" />
                            <span class="text-xs font-semibold text-truncate max-width-200" style="display: inline-block;">{{ req.attachmentName }}</span>
                          </button>
                        </div>
                      </td>
                      <td>
                        <span
                          class="badge"
                          :class="req.status === 'APPROVED' ? 'badge-success' : req.status === 'PENDING' ? 'badge-warning' : req.status === 'REJECTED' ? 'badge-danger' : 'badge-muted'"
                        >
                          {{ req.status }}
                        </span>
                        <div v-if="req.comments" class="text-xs text-muted mt-1 italic">
                          "{{ req.comments }}"
                        </div>
                      </td>
                      <td class="text-right">
                        <button
                          v-if="req.status === 'PENDING'"
                          class="btn btn-danger btn-sm px-2 py-6"
                          @click="handleCancelRequest(req.id)"
                        >
                          Cancel
                        </button>
                      </td>
                    </tr>
                    <tr v-if="filteredMyRequests.length === 0">
                      <td colspan="6" class="text-center p-8 text-secondary">
                        <IconHelper name="calendar" size="36" class="mb-2 text-muted" />
                        <p class="font-semibold text-sm">No Leave Requests Found</p>
                        <p class="text-xs text-muted">Try adjusting your filters or apply for a new leave request.</p>
                      </td>
                    </tr>
                  </template>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- TAB 2: LEAVE CALENDAR -->
    <template v-if="activeTab === 'calendar'">
      <div class="glass-card mb-6">
        <div class="flex justify-between items-center">
          <div class="flex items-center gap-4">
            <button class="btn btn-secondary btn-sm" @click="prevMonth" title="Previous Month" aria-label="Previous Month">
              <IconHelper name="arrow-left" size="14" />
            </button>
            <h3 class="font-bold text-lg" style="min-width: 150px; text-align: center;">
              {{ monthNames[calMonth] }} {{ calYear }}
            </h3>
            <button class="btn btn-secondary btn-sm" @click="nextMonth" title="Next Month" aria-label="Next Month">
              <IconHelper name="plus" size="14" style="transform: rotate(45deg);" />
            </button>
          </div>

          <div class="flex gap-3">
            <div class="form-group m-0 w-160">
              <select v-model="calendarCountry" class="form-control">
                <option value="India">Holidays: India</option>
                <option value="USA">Holidays: USA</option>
                <option value="UK">Holidays: UK</option>
              </select>
            </div>
            
            <div class="form-group m-0 w-200" v-if="authStore.activeRole !== 'EMPLOYEE'">
              <select v-model="calendarDeptFilter" class="form-control">
                <option value="All">Org-wide leaves</option>
                <option value="Engineering">Engineering Dept</option>
                <option value="HR">HR Dept</option>
                <option value="Finance">Finance Dept</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      <!-- Calendar grid -->
      <div class="calendar-grid">
        <div v-for="d in ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']" :key="d" class="calendar-day-header">
          {{ d }}
        </div>
        <div
          v-for="(cell, idx) in calendarDays"
          :key="idx"
          class="calendar-day-cell"
          :class="{ 'other-month': cell.isOtherMonth }"
        >
          <span class="calendar-day-num">{{ cell.day }}</span>
          
          <div class="calendar-events">
            <div
              v-for="(ev, eIdx) in cell.events"
              :key="eIdx"
              class="calendar-event"
              :style="ev.type === 'holiday' ? 'background: var(--primary-light); color: var(--border-focus); border-left: 2px solid var(--primary-color)' : 'background: rgba(16, 185, 129, 0.15); color: var(--success); border-left: 2px solid var(--success)'"
            >
              {{ ev.text }}
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- TAB 3: TEAM LEAVE APPROVALS -->
    <template v-if="activeTab === 'approvals' && ['MANAGER', 'HR_ADMIN'].includes(authStore.activeRole)">
      <div class="glass-card">
        <h3 class="mb-6">Pending Leave Request Approvals</h3>
        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>Employee</th>
                <th>Leave Type</th>
                <th>Dates</th>
                <th>Days</th>
                <th>Reason</th>
                <th>Availability Check</th>
                <th class="text-right">Action</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="req in pendingApprovals" :key="req.id">
                <td>
                  <div class="flex flex-col">
                    <span class="font-bold">{{ req.fullName }}</span>
                    <span class="text-xs text-secondary font-mono">{{ req.employeeCode }}</span>
                  </div>
                </td>
                <td>{{ req.leaveType }}</td>
                <td>{{ req.fromDate }} to {{ req.toDate }}</td>
                <td class="font-mono">{{ req.daysRequested }}</td>
                <td class="text-secondary max-width-200" :title="req.reason">
                  <div class="text-truncate">{{ req.reason }}</div>
                  <div v-if="req.attachmentName" class="mt-1">
                    <button
                      type="button"
                      class="btn btn-ghost btn-xs text-primary flex items-center gap-1 p-0"
                      style="border: none; background: transparent; cursor: pointer; color: var(--primary-color);"
                      @click="triggerDownload(req.attachmentPath, req.attachmentName)"
                    >
                      <IconHelper name="download" size="12" />
                      <span class="text-xs font-semibold text-truncate max-width-200" style="display: inline-block;">{{ req.attachmentName }}</span>
                    </button>
                  </div>
                </td>
                <td>
                  <!-- Simulated team availability warning -->
                  <div class="flex items-center gap-1 text-success text-xs font-semibold">
                    <IconHelper name="check-circle" size="14" />
                    <span>Cleared (0 overlaps)</span>
                  </div>
                </td>
                <td class="text-right">
                  <div class="flex gap-2 justify-end">
                    <button class="btn btn-success btn-sm px-2 py-6" @click="openReview(req, 'APPROVE')">Approve</button>
                    <button class="btn btn-danger btn-sm px-2 py-6" @click="openReview(req, 'REJECT')">Reject</button>
                  </div>
                </td>
              </tr>
              <tr v-if="pendingApprovals.length === 0">
                <td colspan="7" class="text-center text-secondary p-8">No pending leave requests awaiting your approval.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- TAB 4: LEAVE POLICY CONFIG -->
    <template v-if="activeTab === 'policy' && authStore.activeRole === 'HR_ADMIN'">
      <div class="grid-12">
        <!-- Leave types -->
        <div class="col-8">
          <div class="glass-card">
            <h3 class="mb-6">Configure Policy Accruals</h3>
            <div class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>Leave Category</th>
                    <th>Accrual Frequency</th>
                    <th>Max Cap (Per Year)</th>
                    <th>Carry Forward</th>
                    <th>Eligibility</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="type in hrStore.leaveTypes" :key="type.id">
                    <td class="font-semibold">{{ type.name }}</td>
                    <td>
                      <select v-model="type.accrualFrequency" class="form-control text-xs py-1">
                        <option value="MONTHLY">Monthly Accrual</option>
                        <option value="QUARTERLY">Quarterly Accrual</option>
                        <option value="ANNUAL">Annual Accrual</option>
                      </select>
                    </td>
                    <td>
                      <input type="number" v-model="type.maxBalance" class="form-control text-xs w-80 py-1" />
                    </td>
                    <td>{{ type.carryForwardLimit }} Days</td>
                    <td>{{ type.eligibility }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="flex justify-end mt-4">
              <button class="btn btn-primary btn-sm" @click="window.showPortalToast('Leave accrual rules saved successfully', 'success')">
                Save Accrual Policies
              </button>
            </div>
          </div>
        </div>

        <!-- Holidays config -->
        <div class="col-4">
          <div class="glass-card">
            <h3 class="mb-6">Add Public Holiday</h3>
            <form @submit.prevent="handleAddHoliday">
              <div class="form-group">
                <label class="form-label">Target Country</label>
                <select v-model="holidayCountry" class="form-control">
                  <option value="India">India</option>
                  <option value="USA">USA</option>
                  <option value="UK">UK</option>
                </select>
              </div>

              <div class="form-group">
                <label class="form-label">Holiday Name</label>
                <input type="text" v-model="newHolidayName" class="form-control" placeholder="e.g. Christmas" required />
              </div>

              <div class="form-group">
                <label class="form-label">Holiday Date</label>
                <input type="date" v-model="newHolidayDate" class="form-control" required />
              </div>

              <button type="submit" class="btn btn-primary btn-full">
                Add to Calendar
              </button>
            </form>
          </div>
        </div>
      </div>
    </template>

    <!-- REVIEW APPROVAL / REJECTION MODAL -->
    <div class="modal-overlay" v-if="showReviewModal && activeRequest">
      <div class="modal-content">
        <div class="modal-header">
          <h3>Confirm {{ reviewAction === 'APPROVE' ? 'Approval' : 'Rejection' }}</h3>
          <button class="btn btn-ghost btn-sm" @click="showReviewModal = false">
            <IconHelper name="plus" size="14" style="transform: rotate(45deg);" />
          </button>
        </div>
        <div class="modal-body">
          <p class="mb-4">
            You are processing the <b>{{ activeRequest.leaveType }} Leave</b> request for
            <b>{{ activeRequest.fullName }}</b> ({{ activeRequest.daysRequested }} days: {{ activeRequest.fromDate }} to {{ activeRequest.toDate }}).
          </p>
          
          <div v-if="errorMsg" class="alert alert-error mb-4" style="min-width: 100%">
            <span>{{ errorMsg }}</span>
          </div>

          <div class="form-group">
            <label class="form-label">Comments / Decision Reason {{ reviewAction === 'REJECT' ? '*' : '' }}</label>
            <textarea
              v-model="reviewComment"
              class="form-control"
              :placeholder="reviewAction === 'APPROVE' ? 'Optional comments...' : 'Mandatory rejection reason...'"
              required
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showReviewModal = false">Cancel</button>
          <button
            class="btn"
            :class="reviewAction === 'APPROVE' ? 'btn-success' : 'btn-danger'"
            @click="handleConfirmReview"
          >
            Confirm {{ reviewAction === 'APPROVE' ? 'Approval' : 'Rejection' }}
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.leave-hub {
  animation: fadeIn 0.4s ease;
}

.max-width-200 {
  max-width: 200px;
}
.text-truncate {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bg-success-light { background-color: var(--success-light); }
.bg-warning-light { background-color: var(--warning-light); }

.w-80 { width: 80px; }
.w-160 { width: 160px; }
.w-200 { width: 200px; }

.font-normal { font-weight: 400; }
.font-semibold { font-weight: 600; }
.font-bold { font-weight: 700; }
.font-mono { font-family: monospace; }
.text-xs { font-size: 12px; }
.text-sm { font-size: 13px; }
.text-lg { font-size: 18px; }

.flex { display: flex; }
.flex-col { display: flex; flex-direction: column; }
.flex-1 { flex: 1; }
.items-center { align-items: center; }
.items-end { align-items: flex-end; }
.justify-between { justify-content: space-between; }
.justify-end { justify-content: flex-end; }
.gap-1 { gap: 4px; }
.gap-2 { gap: 8px; }
.gap-3 { gap: 12px; }
.gap-4 { gap: 16px; }

.rounded { border-radius: var(--radius-sm); }
.border { border: 1px solid var(--primary-color); }
.border-t { border-top: 1px solid var(--border-color); }

.mb-1 { margin-bottom: 4px; }
.mb-2 { margin-bottom: 8px; }
.mb-3 { margin-bottom: 12px; }
.mb-4 { margin-bottom: 16px; }
.mb-6 { margin-bottom: 24px; }
.mt-1 { margin-top: 4px; }
.mt-4 { margin-top: 16px; }
.py-1 { padding-top: 4px; padding-bottom: 4px; }
.py-6 { padding-top: 6px; padding-bottom: 6px; }
.py-10 { padding-top: 10px; padding-bottom: 10px; }
.p-3 { padding: 12px; }
.p-8 { padding: 32px; }
.text-right { text-align: right; }
.btn-full { width: 100%; }
.italic { font-style: italic; }
</style>
