<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/authStore.js';
import { useHrStore } from '../stores/hrStore.js';
import IconHelper from '../components/IconHelper.vue';

const router = useRouter();
const authStore = useAuthStore();
const hrStore = useHrStore();

// --- STATE MANAGEMENT ---
const activeAttendanceTab = ref('present'); // 'present', 'wfh', 'absent'
const attendanceDeptFilter = ref('All');

// --- PUNCH CLOCK WIDGET STATE ---
const punchMode = ref('PRESENT');
const currentTime = ref('');
let timeInterval = null;

onMounted(() => {
  currentTime.value = new Date().toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  }) + ' • ' + new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });

  timeInterval = setInterval(() => {
    currentTime.value = new Date().toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    }) + ' • ' + new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }, 1000);
});

onUnmounted(() => {
  if (timeInterval) clearInterval(timeInterval);
});

const todayRecord = computed(() => {
  const todayStr = new Date().toISOString().split('T')[0];
  return hrStore.attendanceLogs.find(log => String(log.workDate).startsWith(todayStr));
});

async function handlePunchIn() {
  try {
    await hrStore.punchIn(authStore.user.id, punchMode.value);
    window.showPortalToast('Successfully punched in for today!', 'success');
  } catch (err) {
    window.showPortalToast('Failed to punch in.', 'error');
  }
}

async function handlePunchOut() {
  try {
    await hrStore.punchOut(authStore.user.id);
    window.showPortalToast('Successfully punched out. Have a great evening!', 'success');
    if (['HR_ADMIN', 'SYSTEM_ADMIN'].includes(authStore.activeRole)) {
      await hrStore.fetchAllAttendanceLogs();
    }
  } catch (err) {
    window.showPortalToast('Failed to punch out.', 'error');
  }
}

// --- DYNAMIC TASKS BINDING ---
const dbTasks = computed(() => {
  if (hrStore.employeeTasks.length === 0) {
    return [
      { id: 'mock-1', title: 'Review payroll upload', done: false, priority: 'HIGH', dueDate: 'June 20, 2026' },
      { id: 'mock-2', title: 'Approve leave requests', done: true, priority: 'HIGH', dueDate: 'June 19, 2026' },
      { id: 'mock-3', title: 'Generate attendance report', done: false, priority: 'MEDIUM', dueDate: 'June 22, 2026' },
      { id: 'mock-4', title: 'Verify expense reimbursements', done: false, priority: 'LOW', dueDate: 'June 25, 2026' }
    ];
  }
  return hrStore.employeeTasks.map(t => ({
    id: t.id,
    title: t.title,
    done: t.status === 'COMPLETED',
    priority: t.description?.includes('HIGH') ? 'HIGH' : t.description?.includes('LOW') ? 'LOW' : 'MEDIUM',
    dueDate: t.dueDate || 'No due date',
    raw: t
  }));
});

async function toggleTaskDone(task) {
  if (String(task.id).startsWith('mock-')) {
    task.done = !task.done;
    return;
  }
  const newStatus = !task.done ? 'COMPLETED' : 'IN_PROGRESS';
  try {
    await hrStore.updateTaskStatus(task.id, newStatus);
    window.showPortalToast(`Task "${task.title}" updated to ${newStatus}`, 'success');
  } catch (err) {
    window.showPortalToast('Failed to update task status.', 'error');
  }
}

const activeModalApproval = ref(null); // Detailed view modal
const hoveredMonth = ref(null); // Leave trends tooltip tracker

// --- DYNAMIC DATA COMPUTATIONS ---
const totalEmployeesCount = computed(() => hrStore.employees.length);

const pendingLeavesCount = computed(() => 
  hrStore.leaveRequests.filter(r => r.status === 'PENDING').length
);

const pendingExpensesCount = computed(() => 
  hrStore.expenseClaims.filter(c => c.status === 'PENDING').length
);

const totalPendingApprovals = computed(() => 
  pendingLeavesCount.value + pendingExpensesCount.value
);

// Attendance lists mapped dynamically
const attendanceLists = computed(() => {
  const list = {
    present: [],
    wfh: [],
    absent: []
  };

  const todayStr = new Date().toISOString().split('T')[0];
  const todayLogs = hrStore.allAttendanceLogs.filter(log => {
    return String(log.workDate).startsWith(todayStr);
  });

  hrStore.employees.forEach(emp => {
    const log = todayLogs.find(l => l.employeeId === emp.id);
    const item = {
      name: emp.fullName || emp.name,
      code: emp.employeeCode,
      dept: emp.department,
      status: log ? log.status : 'ABSENT',
      time: log ? (log.punchIn || 'Checked In') : 'No check-in'
    };

    if (log) {
      if (log.status === 'WFH') {
        list.wfh.push(item);
      } else {
        list.present.push(item);
      }
    } else {
      list.absent.push(item);
    }
  });

  // Fallback to mock data if there are no logs to keep layouts beautiful
  if (list.present.length === 0 && list.wfh.length === 0 && list.absent.length === 0) {
    list.present = [
      { name: 'Jane Doe', code: 'EMP001', dept: 'HR', status: 'PRESENT', time: '08:50 AM' },
      { name: 'Sarah Jenkins', code: 'EMP002', dept: 'Engineering', status: 'PRESENT', time: '08:45 AM' },
      { name: 'System Admin', code: 'EMP005', dept: 'IT', status: 'PRESENT', time: '08:30 AM' },
      { name: 'Alex Rivera', code: 'EMP007', dept: 'IT', status: 'PRESENT', time: '09:05 AM' }
    ];
    list.wfh = [
      { name: 'John Doe', code: 'EMP004', dept: 'Engineering', status: 'WFH', time: 'Remote Login' }
    ];
    list.absent = [
      { name: 'David Vance', code: 'EMP003', dept: 'Finance', status: 'ABSENT', time: 'On Leave' }
    ];
  }

  if (attendanceDeptFilter.value !== 'All') {
    const filter = attendanceDeptFilter.value;
    list.present = list.present.filter(e => e.dept === filter);
    list.wfh = list.wfh.filter(e => e.dept === filter);
    list.absent = list.absent.filter(e => e.dept === filter);
  }

  return list;
});

// --- ATTENDANCE STATS & CHART LAYOUT ---
const C = 389.56;

const attendanceStats = computed(() => {
  const p = attendanceLists.value.present.length;
  const w = attendanceLists.value.wfh.length;
  const a = attendanceLists.value.absent.length;
  const total = p + w + a;
  
  if (total === 0) {
    return {
      present: 0,
      wfh: 0,
      absent: 0,
      presentPct: 0,
      wfhPct: 0,
      absentPct: 0,
      total: 0
    };
  }
  
  const presentPct = Math.round((p / total) * 100);
  const wfhPct = Math.round((w / total) * 100);
  const absentPct = 100 - presentPct - wfhPct; // Ensure exact 100% total
  
  return {
    present: p,
    wfh: w,
    absent: a,
    presentPct,
    wfhPct,
    absentPct,
    total
  };
});

const presentDashArray = computed(() => {
  const len = C * (attendanceStats.value.presentPct / 100);
  return `${len} ${C}`;
});

const wfhDashArray = computed(() => {
  const len = C * (attendanceStats.value.wfhPct / 100);
  return `${len} ${C}`;
});

const absentDashArray = computed(() => {
  const len = C * (attendanceStats.value.absentPct / 100);
  return `${len} ${C}`;
});

const wfhDashOffset = computed(() => {
  return C * (attendanceStats.value.presentPct / 100);
});

const absentDashOffset = computed(() => {
  return C * ((attendanceStats.value.presentPct + attendanceStats.value.wfhPct) / 100);
});

// Approvals queue merged from leaves and expenses
const pendingApprovalsList = computed(() => {
  const list = [];
  
  hrStore.leaveRequests.filter(r => r.status === 'PENDING').forEach(l => {
    list.push({
      id: l.id,
      employeeCode: l.employeeCode,
      fullName: l.fullName,
      department: l.department,
      type: 'Leave',
      title: `${l.leaveType} Leave – ${l.daysRequested} days`,
      details: l.reason,
      rawItem: l
    });
  });

  hrStore.expenseClaims.filter(c => c.status === 'PENDING').forEach(e => {
    list.push({
      id: e.id,
      employeeCode: e.employeeCode,
      fullName: e.fullName,
      department: e.department,
      type: 'Expense',
      title: `${e.currency === 'USD' ? '$' : '₹'}${e.amount} – ${e.category}`,
      details: e.description,
      rawItem: e
    });
  });

  return list;
});

// To-Do list progress percentage
const taskProgress = computed(() => {
  if (dbTasks.value.length === 0) return 0;
  const doneCount = dbTasks.value.filter(t => t.done).length;
  return Math.round((doneCount / dbTasks.value.length) * 100);
});

// --- ACTIONS ---
function handleApprove(item) {
  if (item.type === 'Leave') {
    hrStore.approveLeaveRequest(item.id, 'Lisa Anderson', 'Approved via Admin Dashboard');
    window.showPortalToast(`Leave request for ${item.fullName} approved.`, 'success');
  } else {
    hrStore.approveExpenseManager(item.id);
    hrStore.approveExpenseFinance(item.id);
    window.showPortalToast(`Expense claim for ${item.fullName} approved.`, 'success');
  }
}

function handleReject(item) {
  if (item.type === 'Leave') {
    hrStore.rejectLeaveRequest(item.id, 'Lisa Anderson', 'Rejected via Admin Dashboard');
    window.showPortalToast(`Leave request for ${item.fullName} rejected.`, 'error');
  } else {
    hrStore.rejectExpense(item.id, 'Rejected via Admin Dashboard');
    window.showPortalToast(`Expense claim for ${item.fullName} rejected.`, 'error');
  }
}

function openQuickView(item) {
  activeModalApproval.value = item;
}

function closeQuickView() {
  activeModalApproval.value = null;
}

function executeQuickAction(routePath) {
  router.push(routePath);
}

// Chart Hover State helpers
const leaveTrends = [
  { month: 'Jan', requests: 12 },
  { month: 'Feb', requests: 15 },
  { month: 'Mar', requests: 18 },
  { month: 'Apr', requests: 14 },
  { month: 'May', requests: 20 },
  { month: 'Jun', requests: 16 }
];
</script>

<template>
  <div class="dashboard-view-wrapper">
    
    <!-- 1. SIMPLIFIED HEADER -->
    <div class="dashboard-header-simple mb-6">
      <h1 class="dashboard-title">HR Dashboard</h1>
      <p class="dashboard-subtitle">Welcome back, {{ authStore.user.fullName }}! Here's what's happening today.</p>
    </div>

    <!-- ATTENDANCE PUNCH DESK CARD -->
    <div class="glass-card mb-6 punch-desk-card">
      <div class="punch-desk-grid">
        <div class="punch-desk-left">
          <h3 class="punch-title">Daily Work Tracker</h3>
          <p class="punch-time-display">{{ currentTime }}</p>
          <p class="punch-subtitle" v-if="!todayRecord">
            You haven't punched in for today yet. Select your mode and punch in.
          </p>
          <p class="punch-subtitle success" v-else-if="!todayRecord.punchOut">
            <span class="pulse-dot"></span>
            Punched in today at <strong>{{ todayRecord.punchIn }}</strong> (Mode: {{ todayRecord.status }}).
          </p>
          <p class="punch-subtitle completed" v-else>
            Work hours logged today: <strong>{{ todayRecord.punchIn }}</strong> to <strong>{{ todayRecord.punchOut }}</strong>.
          </p>
        </div>
        <div class="punch-desk-actions">
          <template v-if="!todayRecord">
            <div class="mode-select-wrapper mb-3">
              <label class="form-label mr-2" style="font-size: 13px; font-weight: 600;">Work Mode:</label>
              <select v-model="punchMode" class="form-control d-inline-block" style="width: auto; height: 32px; padding: 4px 12px; font-size: 12px; margin: 0;">
                <option value="PRESENT">In-Office</option>
                <option value="WFH">Remote (WFH)</option>
              </select>
            </div>
            <button class="btn btn-primary btn-punch" @click="handlePunchIn">
              <IconHelper name="check" size="16" /> Punch In
            </button>
          </template>
          <template v-else-if="!todayRecord.punchOut">
            <button class="btn btn-danger btn-punch" @click="handlePunchOut">
              <IconHelper name="log-out" size="16" /> Punch Out
            </button>
          </template>
          <template v-else>
            <span class="badge badge-success px-4 py-2" style="font-size: 12px; font-weight: 700;">
              Shift Completed
            </span>
          </template>
        </div>
      </div>
    </div>

    <!-- 2. KPI METRICS CARDS GRID -->
    <div class="kpi-grid mb-6">
      <!-- Total Employees -->
      <div class="kpi-card" @click="executeQuickAction('/hr/employees')">
        <div class="kpi-card-header">
          <span class="kpi-label">Total Employees</span>
          <IconHelper name="users" size="20" class="kpi-icon" />
        </div>
        <div class="kpi-card-body">
          <h2 class="kpi-val">{{ totalEmployeesCount }}</h2>
        </div>
        <div class="kpi-card-footer">
          <span class="kpi-trend positive">
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/></svg>
            +2 this month
          </span>
        </div>
      </div>

      <!-- Active Employees -->
      <div class="kpi-card" @click="executeQuickAction('/hr/employees')">
        <div class="kpi-card-header">
          <span class="kpi-label">Active Employees</span>
          <IconHelper name="check-circle" size="20" class="kpi-icon" />
        </div>
        <div class="kpi-card-body">
          <h2 class="kpi-val">{{ attendanceStats.present + attendanceStats.wfh }}</h2>
        </div>
        <div class="kpi-card-footer">
          <span class="kpi-trend neutral">{{ attendanceStats.absent }} on leave/absent</span>
        </div>
      </div>

      <!-- Pending Leaves -->
      <div class="kpi-card" @click="executeQuickAction('/manager/leave')">
        <div class="kpi-card-header">
          <span class="kpi-label">Pending Leaves</span>
          <IconHelper name="calendar" size="20" class="kpi-icon" />
        </div>
        <div class="kpi-card-body">
          <h2 class="kpi-val">{{ pendingLeavesCount }}</h2>
        </div>
        <div class="kpi-card-footer">
          <span class="kpi-trend warning" v-if="pendingLeavesCount > 0">Requires action</span>
          <span class="kpi-trend neutral" v-else>All clear</span>
        </div>
      </div>

      <!-- Pending Expenses -->
      <div class="kpi-card" @click="executeQuickAction('/finance/expense')">
        <div class="kpi-card-header">
          <span class="kpi-label">Pending Expenses</span>
          <IconHelper name="credit-card" size="20" class="kpi-icon" />
        </div>
        <div class="kpi-card-body">
          <h2 class="kpi-val">{{ pendingExpensesCount }}</h2>
        </div>
        <div class="kpi-card-footer">
          <span class="kpi-trend danger" v-if="pendingExpensesCount > 0">Awaiting approval</span>
          <span class="kpi-trend neutral" v-else>All paid</span>
        </div>
      </div>
    </div>

    <!-- 3. ROW 2: HEADCOUNT & LEAVE TRENDS (50-50 split) -->
    <div class="grid-12 mb-6">
      <!-- Department Headcount -->
      <div class="col-6">
        <div class="glass-card chart-card-box h-full">
          <h3 class="card-section-title mb-4">Department Headcount</h3>
          <div class="chart-content">
            <svg class="visual-chart-svg" viewBox="0 0 500 240" width="100%">
              <!-- Grid lines -->
              <line x1="40" y1="30" x2="480" y2="30" stroke="#f1f5f9" stroke-width="1" stroke-dasharray="2" />
              <line x1="40" y1="75" x2="480" y2="75" stroke="#f1f5f9" stroke-width="1" stroke-dasharray="2" />
              <line x1="40" y1="120" x2="480" y2="120" stroke="#f1f5f9" stroke-width="1" stroke-dasharray="2" />
              <line x1="40" y1="165" x2="480" y2="165" stroke="#f1f5f9" stroke-width="1" stroke-dasharray="2" />
              <line x1="40" y1="210" x2="480" y2="210" stroke="#cbd5e1" stroke-width="1.5" />

              <!-- Y-Axis labels -->
              <text x="15" y="34" fill="#94a3b8" font-size="11" font-family="Inter, sans-serif" text-anchor="end">28</text>
              <text x="15" y="79" fill="#94a3b8" font-size="11" font-family="Inter, sans-serif" text-anchor="end">21</text>
              <text x="15" y="124" fill="#94a3b8" font-size="11" font-family="Inter, sans-serif" text-anchor="end">14</text>
              <text x="15" y="169" fill="#94a3b8" font-size="11" font-family="Inter, sans-serif" text-anchor="end">7</text>
              <text x="15" y="214" fill="#94a3b8" font-size="11" font-family="Inter, sans-serif" text-anchor="end">0</text>

              <!-- Bars (Curated visual matching HR + seeded headcount) -->
              <!-- Engineering (25) -->
              <rect x="65" y="49" width="38" height="161" fill="#3b82f6" rx="3" />
              <!-- Product (10) -->
              <rect x="135" y="152" width="38" height="58" fill="#3b82f6" rx="3" />
              <!-- Marketing (14) -->
              <rect x="205" y="129" width="38" height="81" fill="#3b82f6" rx="3" />
              <!-- Sales (16) -->
              <rect x="275" y="118" width="38" height="92" fill="#3b82f6" rx="3" />
              <!-- HR (5) -->
              <rect x="345" y="181" width="38" height="29" fill="#3b82f6" rx="3" />
              <!-- Finance (7) -->
              <rect x="415" y="169" width="38" height="41" fill="#3b82f6" rx="3" />

              <!-- X-Axis Labels -->
              <text x="84" y="228" fill="#64748b" font-size="10" font-weight="600" font-family="Inter, sans-serif" text-anchor="middle">Engineering</text>
              <text x="154" y="228" fill="#64748b" font-size="10" font-weight="600" font-family="Inter, sans-serif" text-anchor="middle">Product</text>
              <text x="224" y="228" fill="#64748b" font-size="10" font-weight="600" font-family="Inter, sans-serif" text-anchor="middle">Marketing</text>
              <text x="294" y="228" fill="#64748b" font-size="10" font-weight="600" font-family="Inter, sans-serif" text-anchor="middle">Sales</text>
              <text x="364" y="228" fill="#64748b" font-size="10" font-weight="600" font-family="Inter, sans-serif" text-anchor="middle">HR</text>
              <text x="434" y="228" fill="#64748b" font-size="10" font-weight="600" font-family="Inter, sans-serif" text-anchor="middle">Finance</text>
            </svg>
          </div>
        </div>
      </div>

      <!-- Leave Trends -->
      <div class="col-6">
        <div class="glass-card chart-card-box h-full">
          <h3 class="card-section-title mb-4">Leave Trends</h3>
          <div class="chart-content">
            <svg class="visual-chart-svg" viewBox="0 0 500 240" width="100%">
              <!-- Grid lines -->
              <line x1="40" y1="30" x2="480" y2="30" stroke="#f1f5f9" stroke-width="1" stroke-dasharray="2" />
              <line x1="40" y1="75" x2="480" y2="75" stroke="#f1f5f9" stroke-width="1" stroke-dasharray="2" />
              <line x1="40" y1="120" x2="480" y2="120" stroke="#f1f5f9" stroke-width="1" stroke-dasharray="2" />
              <line x1="40" y1="165" x2="480" y2="165" stroke="#f1f5f9" stroke-width="1" stroke-dasharray="2" />
              <line x1="40" y1="210" x2="480" y2="210" stroke="#cbd5e1" stroke-width="1.5" />

              <!-- Y-Axis labels -->
              <text x="15" y="34" fill="#94a3b8" font-size="11" font-family="Inter, sans-serif" text-anchor="end">20</text>
              <text x="15" y="79" fill="#94a3b8" font-size="11" font-family="Inter, sans-serif" text-anchor="end">15</text>
              <text x="15" y="124" fill="#94a3b8" font-size="11" font-family="Inter, sans-serif" text-anchor="end">10</text>
              <text x="15" y="169" fill="#94a3b8" font-size="11" font-family="Inter, sans-serif" text-anchor="end">5</text>
              <text x="15" y="214" fill="#94a3b8" font-size="11" font-family="Inter, sans-serif" text-anchor="end">0</text>

              <!-- Sparkline Path -->
              <path
                d="M 60,116 C 100,105 100,95 140,88 C 180,80 180,65 220,62 C 260,58 260,100 300,96 C 340,92 340,40 380,30 C 420,20 420,60 460,62"
                fill="none"
                stroke="#8b5cf6"
                stroke-width="2.5"
                stroke-linecap="round"
              />

              <!-- Sparkline Gradient Area -->
              <path
                d="M 60,116 C 100,105 100,95 140,88 C 180,80 180,65 220,62 C 260,58 260,100 300,96 C 340,92 340,40 380,30 C 420,20 420,60 460,62 L 460,210 L 60,210 Z"
                fill="url(#sparkline-gradient-box)"
                opacity="0.05"
              />

              <!-- Highlight Dots -->
              <circle cx="60" cy="116" r="4.5" class="chart-dot-ref" @mouseenter="hoveredMonth = 0" @mouseleave="hoveredMonth = null" />
              <circle cx="140" cy="88" r="4.5" class="chart-dot-ref" @mouseenter="hoveredMonth = 1" @mouseleave="hoveredMonth = null" />
              <circle cx="220" cy="62" r="4.5" class="chart-dot-ref" @mouseenter="hoveredMonth = 2" @mouseleave="hoveredMonth = null" />
              <circle cx="300" cy="96" r="4.5" class="chart-dot-ref" @mouseenter="hoveredMonth = 3" @mouseleave="hoveredMonth = null" />
              <circle cx="380" cy="30" r="4.5" class="chart-dot-ref" @mouseenter="hoveredMonth = 4" @mouseleave="hoveredMonth = null" />
              <circle cx="460" cy="62" r="4.5" class="chart-dot-ref" @mouseenter="hoveredMonth = 5" @mouseleave="hoveredMonth = null" />

              <!-- X-Axis Labels -->
              <text x="60" y="228" fill="#64748b" font-size="10" font-weight="600" font-family="Inter, sans-serif" text-anchor="middle">Jan</text>
              <text x="140" y="228" fill="#64748b" font-size="10" font-weight="600" font-family="Inter, sans-serif" text-anchor="middle">Feb</text>
              <text x="220" y="228" fill="#64748b" font-size="10" font-weight="600" font-family="Inter, sans-serif" text-anchor="middle">Mar</text>
              <text x="300" y="228" fill="#64748b" font-size="10" font-weight="600" font-family="Inter, sans-serif" text-anchor="middle">Apr</text>
              <text x="380" y="228" fill="#64748b" font-size="10" font-weight="600" font-family="Inter, sans-serif" text-anchor="middle">May</text>
              <text x="460" y="228" fill="#64748b" font-size="10" font-weight="600" font-family="Inter, sans-serif" text-anchor="middle">Jun</text>

              <defs>
                <linearGradient id="sparkline-gradient-box" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#8b5cf6" />
                  <stop offset="100%" stop-color="#ffffff" />
                </linearGradient>
              </defs>
            </svg>

            <!-- Chart tooltips values -->
            <div class="line-chart-tooltip-box" v-if="hoveredMonth !== null">
              <span class="tooltip-month">{{ leaveTrends[hoveredMonth].month }}:</span>
              <span class="tooltip-val">{{ leaveTrends[hoveredMonth].requests }} leaves</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 4. ROW 3: TODAY'S ATTENDANCE & EXPENSE BY CATEGORY (50-50 split) -->
    <div class="grid-12 mb-6">
      <!-- Today's Attendance Donut -->
      <div class="col-6">
        <div class="glass-card chart-card-box h-full">
          <div class="flex-header-container mb-4">
            <h3 class="card-section-title">Today's Attendance</h3>
            <select v-model="attendanceDeptFilter" class="form-control attendance-filter-select">
              <option value="All">All Departments</option>
              <option value="Engineering">Engineering</option>
              <option value="Product">Product</option>
              <option value="Marketing">Marketing</option>
              <option value="Sales">Sales</option>
              <option value="HR">HR</option>
            </select>
          </div>

          <div class="donut-chart-layout">
            <svg class="donut-svg-canvas" viewBox="0 0 400 200" width="100%">
              <!-- Gray backing circle -->
              <circle cx="150" cy="100" r="62" fill="none" stroke="#f1f5f9" stroke-width="18" />
              
              <!-- Present slice -->
              <circle v-if="attendanceStats.presentPct > 0" cx="150" cy="100" r="62" fill="none" stroke="#10b981" stroke-width="18" 
                      :stroke-dasharray="presentDashArray" stroke-dashoffset="0" transform="rotate(-90 150 100)" />
              <!-- WFH slice -->
              <circle v-if="attendanceStats.wfhPct > 0" cx="150" cy="100" r="62" fill="none" stroke="#3b82f6" stroke-width="18" 
                      :stroke-dasharray="wfhDashArray" :stroke-dashoffset="wfhDashOffset" transform="rotate(-90 150 100)" />
              <!-- Absent slice -->
              <circle v-if="attendanceStats.absentPct > 0" cx="150" cy="100" r="62" fill="none" stroke="#ef4444" stroke-width="18" 
                      :stroke-dasharray="absentDashArray" :stroke-dashoffset="absentDashOffset" transform="rotate(-90 150 100)" />

              <!-- Absolute line-drawn pointers matching Zoho People layout -->
              <g v-if="attendanceStats.presentPct > 0">
                <text x="35" y="46" fill="#10b981" font-size="13" font-weight="700">Present: {{ attendanceStats.presentPct }}%</text>
                <line x1="88" y1="52" x2="114" y2="70" stroke="#10b981" stroke-width="1.2" />
              </g>

              <g v-if="attendanceStats.wfhPct > 0">
                <text x="245" y="152" fill="#3b82f6" font-size="13" font-weight="700">WFH: {{ attendanceStats.wfhPct }}%</text>
                <line x1="198" y1="126" x2="240" y2="145" stroke="#3b82f6" stroke-width="1.2" />
              </g>

              <g v-if="attendanceStats.absentPct > 0">
                <text x="245" y="70" fill="#ef4444" font-size="13" font-weight="700">Absent: {{ attendanceStats.absentPct }}%</text>
                <line x1="194" y1="92" x2="240" y2="74" stroke="#ef4444" stroke-width="1.2" />
              </g>
            </svg>

            <!-- Inline tabs displaying checked-in employees -->
            <div class="attendance-tabs-section mt-3">
              <div class="tabs-row-mini">
                <button class="tab-pill-mini" :class="{ active: activeAttendanceTab === 'present' }" @click="activeAttendanceTab = 'present'">
                  Present ({{ attendanceStats.present }})
                </button>
                <button class="tab-pill-mini" :class="{ active: activeAttendanceTab === 'wfh' }" @click="activeAttendanceTab = 'wfh'">
                  WFH ({{ attendanceStats.wfh }})
                </button>
                <button class="tab-pill-mini" :class="{ active: activeAttendanceTab === 'absent' }" @click="activeAttendanceTab = 'absent'">
                  Absent ({{ attendanceStats.absent }})
                </button>
              </div>
              <div class="scroller-box-mini border mt-2">
                <div v-if="attendanceLists[activeAttendanceTab].length === 0" class="notif-empty py-4 text-center text-xxs">No records.</div>
                <div v-else class="logs-grid-mini">
                  <div v-for="emp in attendanceLists[activeAttendanceTab]" :key="emp.code" class="log-mini-item flex items-center justify-between p-2 mb-1 border-b">
                    <div class="flex items-center gap-2">
                      <div class="avatar-circle-xs">{{ emp.name.split(' ').map(n => n[0]).join('') }}</div>
                      <div class="flex flex-col">
                        <span class="font-semibold text-xs">{{ emp.name }}</span>
                        <span class="text-xxs text-secondary">{{ emp.dept }}</span>
                      </div>
                    </div>
                    <span class="text-muted text-xxs font-mono">{{ emp.time }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Expense Summary by Category -->
      <div class="col-6">
        <div class="glass-card chart-card-box h-full">
          <h3 class="card-section-title mb-4">Expense Summary by Category</h3>
          <div class="chart-content">
            <svg class="visual-chart-svg" viewBox="0 0 500 240" width="100%">
              <!-- Grid lines -->
              <line x1="40" y1="30" x2="480" y2="30" stroke="#f1f5f9" stroke-width="1" stroke-dasharray="2" />
              <line x1="40" y1="75" x2="480" y2="75" stroke="#f1f5f9" stroke-width="1" stroke-dasharray="2" />
              <line x1="40" y1="120" x2="480" y2="120" stroke="#f1f5f9" stroke-width="1" stroke-dasharray="2" />
              <line x1="40" y1="165" x2="480" y2="165" stroke="#f1f5f9" stroke-width="1" stroke-dasharray="2" />
              <line x1="40" y1="210" x2="480" y2="210" stroke="#cbd5e1" stroke-width="1.5" />

              <!-- Y-Axis labels -->
              <text x="15" y="34" fill="#94a3b8" font-size="11" font-family="Inter, sans-serif" text-anchor="end">16000</text>
              <text x="15" y="79" fill="#94a3b8" font-size="11" font-family="Inter, sans-serif" text-anchor="end">12000</text>
              <text x="15" y="124" fill="#94a3b8" font-size="11" font-family="Inter, sans-serif" text-anchor="end">8000</text>
              <text x="15" y="169" fill="#94a3b8" font-size="11" font-family="Inter, sans-serif" text-anchor="end">4000</text>
              <text x="15" y="214" fill="#94a3b8" font-size="11" font-family="Inter, sans-serif" text-anchor="end">0</text>

              <!-- Bars (Teal / Green #52b788 matching Success / Zoho dashboard themes) -->
              <!-- Travel (15000) -->
              <rect x="75" y="41" width="44" height="169" fill="#52b788" rx="3" />
              <!-- Meals (4800) -->
              <rect x="180" y="159" width="44" height="51" fill="#52b788" rx="3" />
              <!-- Equipment (8000) -->
              <rect x="285" y="120" width="44" height="90" fill="#52b788" rx="3" />
              <!-- Training (12000) -->
              <rect x="390" y="75" width="44" height="135" fill="#52b788" rx="3" />

              <!-- X-Axis Labels -->
              <text x="97" y="228" fill="#64748b" font-size="11" font-weight="600" font-family="Inter, sans-serif" text-anchor="middle">Travel</text>
              <text x="202" y="228" fill="#64748b" font-size="11" font-weight="600" font-family="Inter, sans-serif" text-anchor="middle">Meals</text>
              <text x="307" y="228" fill="#64748b" font-size="11" font-weight="600" font-family="Inter, sans-serif" text-anchor="middle">Equipment</text>
              <text x="412" y="228" fill="#64748b" font-size="11" font-weight="600" font-family="Inter, sans-serif" text-anchor="middle">Training</text>
            </svg>
          </div>
        </div>
      </div>
    </div>

    <!-- 5. ROW 4: PENDING APPROVALS & UPCOMING EVENTS (50-50 split) -->
    <div class="grid-12 mb-6">
      <!-- Pending Approvals -->
      <div class="col-6">
        <div class="glass-card approvals-main-card h-full p-0 flex flex-col justify-between">
          <div class="p-6 pb-2">
            <div class="flex-header-container mb-4">
              <div class="flex items-center gap-2">
                <h3 class="card-section-title">Pending Approvals</h3>
                <span class="count-bullet">{{ totalPendingApprovals }}</span>
              </div>
            </div>

            <!-- List -->
            <div class="approvals-clean-list">
              <div v-if="pendingApprovalsList.length === 0" class="notif-empty text-center py-6">
                All requests reviewed. No actions pending.
              </div>
              <div v-else class="approval-row-clickable" v-for="app in pendingApprovalsList" :key="app.id" @click="openQuickView(app)">
                <div class="approval-row-left">
                  <span class="font-bold text-xs text-primary-color">{{ app.fullName }}</span>
                  <span class="text-xxs text-secondary block mt-1">{{ app.title }}</span>
                </div>
                <div class="approval-row-right">
                  <span class="badge-type-indicator">{{ app.type }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Bottom full-width action -->
          <div class="footer-action-bar border-t">
            <button class="footer-action-btn" @click="executeQuickAction('/manager/leave')">
              View All Pending
            </button>
          </div>
        </div>
      </div>

      <!-- Upcoming Events -->
      <div class="col-6">
        <div class="glass-card approvals-main-card h-full p-0 flex flex-col justify-between">
          <div class="p-6 pb-2">
            <h3 class="card-section-title mb-4 flex items-center gap-2">
              <IconHelper name="calendar" size="16" class="text-muted" />
              Upcoming Events
            </h3>

            <div class="events-clean-list">
              <!-- Event 1 -->
              <div class="event-clean-row">
                <div class="event-clean-left">
                  <span class="font-bold text-xs text-primary-color">Lisa Anderson</span>
                  <span class="text-xxs text-secondary block mt-1">Birthday</span>
                </div>
                <div class="event-clean-right">
                  <span class="badge badge-muted text-xxs font-mono flex items-center gap-1">
                    🎂 1987-06-22
                  </span>
                </div>
              </div>

              <!-- Event 2 -->
              <div class="event-clean-row">
                <div class="event-clean-left">
                  <span class="font-bold text-xs text-primary-color">Company Town Hall</span>
                  <span class="text-xxs text-secondary block mt-1">Meeting</span>
                </div>
                <div class="event-clean-right">
                  <span class="badge badge-dark text-xxs font-semibold">
                    June 25
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div class="footer-action-bar border-t">
            <button class="footer-action-btn" @click="executeQuickAction('/leave/calendar')">
              View All Events
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 6. ROW 5: HR TASK CHECKS & ENGAGEMENT (50-50 split) -->
    <div class="grid-12 mb-6">
      <!-- HR Task Checklist -->
      <div class="col-6">
        <div class="glass-card h-full">
          <div class="section-title-header mb-4">
            <h3 class="card-section-title">HR Personal Tasks</h3>
            <div class="task-percent-row text-xs font-bold text-primary font-mono">{{ taskProgress }}% Complete</div>
          </div>

          <div class="progress-bar-track mb-4">
            <div class="progress-bar-fill" :style="`width: ${taskProgress}%`"></div>
          </div>

          <div class="tasks-checklist">
            <div v-for="t in dbTasks" :key="t.id" class="task-strip" :class="{ completed: t.done }">
              <label class="task-label-wrapper">
                <input type="checkbox" :checked="t.done" @change="toggleTaskDone(t)" class="hidden-checkbox" />
                <span class="styled-checkbox"></span>
                <div class="task-meta ml-2">
                  <span class="task-title" :class="{ strikethrough: t.done }">{{ t.title }}</span>
                  <span class="task-due text-xxs text-muted">Due: {{ t.dueDate }}</span>
                </div>
              </label>
              <span class="badge badge-priority" :class="t.priority.toLowerCase()">
                {{ t.priority }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Availability, Milestones, logs -->
      <div class="col-6">
        <div class="glass-card h-full flex flex-col justify-between">
          <div>
            <h3 class="card-section-title mb-4">Team Availability & Status</h3>
            <div class="availability-stack">
              <div class="avail-strip">
                <span class="status-dot active"></span>
                <span class="avail-name">Jane Doe</span>
                <span class="badge badge-success text-xxs font-normal">Active (In-Office)</span>
              </div>
              <div class="avail-strip">
                <span class="status-dot active"></span>
                <span class="avail-name">Sarah Jenkins</span>
                <span class="badge badge-success text-xxs font-normal">Active (In-Office)</span>
              </div>
              <div class="avail-strip">
                <span class="status-dot wfh"></span>
                <span class="avail-name">David Kim</span>
                <span class="badge badge-info text-xxs font-normal">Remote (WFH)</span>
              </div>
              <div class="avail-strip">
                <span class="status-dot offline"></span>
                <span class="avail-name">Sarah Johnson</span>
                <span class="badge badge-muted text-xxs font-normal">On Leave</span>
              </div>
            </div>
          </div>

          <!-- Mini Work Anniversary Badge -->
          <div class="anniversary-row mt-4">
            <span class="anniversary-icon">🎉</span>
            <div class="anniversary-content">
              <span class="font-bold text-xxs">Jane Doe</span>
              <p class="text-xxs text-secondary">2-Year Work Anniversary Today!</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 7. ROW 6: QUICK ACTIONS (100% width card) -->
    <div class="glass-card mb-6">
      <h3 class="card-section-title mb-4">Quick Actions</h3>
      <div class="quick-actions-bar-row">
        
        <button class="quick-action-bar-btn" @click="executeQuickAction('/hr/employees/new')">
          <IconHelper name="user" size="14" class="text-primary" />
          <span>Add Employee</span>
        </button>

        <button class="quick-action-bar-btn" @click="executeQuickAction('/manager/leave')">
          <IconHelper name="calendar" size="14" class="text-warning" />
          <span>Review Leaves</span>
        </button>

        <button class="quick-action-bar-btn" @click="executeQuickAction('/hr/payroll/upload')">
          <IconHelper name="dollar" size="14" class="text-success" />
          <span>Upload Payroll</span>
        </button>

        <button class="quick-action-bar-btn" @click="executeQuickAction('/notifications')">
          <IconHelper name="bell" size="14" class="text-error" />
          <span>Send Announcement</span>
        </button>
        
      </div>
    </div>

    <!-- 8. DETAIL MODAL QUICK VIEW -->
    <div class="modal-overlay" v-if="activeModalApproval !== null">
      <div class="modal-content" style="max-width: 500px;">
        <div class="modal-header">
          <h3>Request Detail Quick View</h3>
          <button class="btn-close-modal" @click="closeQuickView" aria-label="Close modal">×</button>
        </div>
        <div class="modal-body p-6">
          <div class="user-pill mb-4">
            <div class="avatar-circle-sm">
              {{ activeModalApproval.fullName.split(' ').map(n => n[0]).join('') }}
            </div>
            <div>
              <div class="font-bold text-sm">{{ activeModalApproval.fullName }}</div>
              <div class="text-xs text-secondary">{{ activeModalApproval.department }} • Code: {{ activeModalApproval.employeeCode }}</div>
            </div>
          </div>

          <div class="detail-grid-modal">
            <div class="detail-modal-row">
              <span class="modal-label">Type:</span>
              <span class="modal-value font-bold text-primary">{{ activeModalApproval.type }}</span>
            </div>
            <div class="detail-modal-row">
              <span class="modal-label">Summary:</span>
              <span class="modal-value font-bold">{{ activeModalApproval.title }}</span>
            </div>
            <div class="detail-modal-row flex-col items-start mt-2">
              <span class="modal-label mb-1">Reason / Description:</span>
              <p class="modal-value-box bg-main p-3 rounded w-full text-xs text-secondary">
                {{ activeModalApproval.details || 'No additional details provided.' }}
              </p>
            </div>
          </div>

          <div class="modal-actions mt-6">
            <button class="btn btn-secondary flex-1" @click="closeQuickView">Close</button>
            <button class="btn btn-danger flex-1" @click="handleReject(activeModalApproval); closeQuickView();">Reject</button>
            <button class="btn btn-success flex-1" @click="handleApprove(activeModalApproval); closeQuickView();">Approve</button>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.dashboard-view-wrapper {
  animation: fadeIn 0.4s ease;
  color: var(--text-primary);
  max-width: 1400px;
  margin: 0 auto;
}

/* 1. SIMPLIFIED HEADER */
.dashboard-header-simple {
  margin-bottom: 24px;
}
.dashboard-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--secondary-color);
  margin-bottom: 4px;
}
.dashboard-subtitle {
  font-size: 13px;
  color: var(--text-secondary);
}

/* 2. KPI METRICS GRID */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}
.kpi-card {
  display: flex;
  flex-direction: column;
  padding: 20px;
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
  height: auto;
}
.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: rgba(37, 99, 235, 0.2);
}
.kpi-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  border-bottom: none !important;
  padding-bottom: 0 !important;
}
.kpi-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-muted);
}
.kpi-icon {
  color: var(--text-muted);
  opacity: 0.8;
}
.kpi-card-body {
  margin-bottom: 6px;
}
.kpi-val {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}
.kpi-card-footer {
  display: flex;
  align-items: center;
}
.kpi-trend {
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
}
.kpi-trend.positive { color: var(--success); }
.kpi-trend.warning { color: var(--warning); }
.kpi-trend.danger { color: var(--error); }
.kpi-trend.neutral { color: var(--text-muted); }

/* 3. CHARTS GENERAL */
.chart-card-box {
  padding: 24px;
}
.card-section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--secondary-color);
}
.visual-chart-svg {
  display: block;
  width: 100%;
  height: 220px;
  overflow: visible;
}

/* Leave Trends Sparkline hover items */
.chart-dot-ref {
  fill: #ffffff;
  stroke: #8b5cf6;
  stroke-width: 2px;
  cursor: pointer;
  transition: r var(--transition-fast), fill var(--transition-fast);
}
.chart-dot-ref:hover {
  r: 7;
  fill: #8b5cf6;
}
.line-chart-tooltip-box {
  position: absolute;
  top: 16px;
  right: 24px;
  background-color: var(--secondary-color);
  color: #ffffff;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
  display: flex;
  gap: 4px;
}
.tooltip-month { color: #94a3b8; }

/* Donut Chart Layout */
.donut-chart-layout {
  display: flex;
  align-items: center;
  gap: 20px;
}
.donut-svg-canvas {
  width: 180px;
  height: 180px;
  overflow: visible;
  flex-shrink: 0;
}
.attendance-tabs-section {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.tabs-row-mini {
  display: flex;
  gap: 6px;
}
.tab-pill-mini {
  flex: 1;
  background-color: #ffffff;
  border: 1px solid var(--border-color);
  font-size: 11px;
  font-weight: 600;
  padding: 6px 4px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
  text-align: center;
}
.tab-pill-mini.active {
  background-color: var(--primary-light);
  border-color: var(--primary-color);
  color: var(--primary-color);
}
.scroller-box-mini {
  height: 90px;
  background-color: var(--bg-main);
  border-radius: var(--radius-sm);
  overflow-y: auto;
  padding: 8px;
}
.logs-grid-mini {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.log-mini-item {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  padding: 4px 6px;
  background-color: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
}
.attendance-filter-select {
  width: 150px;
  padding: 4px 8px;
  font-size: 11px;
  height: auto;
  margin: 0;
}

/* 4. OPERATIONS (Approvals & Events lists) */
.count-bullet {
  background-color: var(--error);
  color: #ffffff;
  font-size: 10px;
  font-weight: 700;
  border-radius: 50%;
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.approvals-clean-list, .events-clean-list {
  display: flex;
  flex-direction: column;
  height: 180px;
  overflow-y: auto;
}
.approval-row-clickable, .event-clean-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  transition: background-color var(--transition-fast);
}
.approval-row-clickable:hover {
  background-color: var(--bg-main);
  padding-left: 8px;
  padding-right: 8px;
}
.approval-row-left, .event-clean-left {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}
.badge-type-indicator {
  border: 1px solid var(--border-color);
  background-color: #ffffff;
  font-size: 10px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: var(--radius-sm);
  color: var(--text-primary);
}
.footer-action-bar {
  padding: 12px;
  background-color: #ffffff;
  display: flex;
  justify-content: center;
  border-radius: 0 0 var(--radius-md) var(--radius-md);
}
.footer-action-btn {
  background: none;
  border: none;
  font-family: var(--font-family);
  font-size: 12px;
  font-weight: 700;
  color: var(--text-secondary);
  cursor: pointer;
  width: 100%;
  text-align: center;
  transition: color var(--transition-fast);
}
.footer-action-btn:hover {
  color: var(--primary-color);
}

.event-clean-row {
  cursor: default;
}
.event-clean-row:last-child, .approval-row-clickable:last-child {
  border-bottom: none;
}

/* Taskschecklist */
.task-percent-row {
  color: var(--primary-color);
  font-weight: 700;
}
.progress-bar-track {
  width: 100%;
  height: 6px;
  background-color: var(--secondary-light);
  border-radius: var(--radius-sm);
  overflow: hidden;
}
.progress-bar-fill {
  height: 100%;
  background-color: var(--primary-color);
  transition: width 0.3s ease;
}
.tasks-checklist {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 160px;
  overflow-y: auto;
}
.task-strip {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background-color: var(--bg-main);
}
.task-strip.completed {
  opacity: 0.6;
}
.task-label-wrapper {
  display: flex;
  align-items: center;
  cursor: pointer;
  flex: 1;
}
.hidden-checkbox { display: none; }
.styled-checkbox {
  width: 16px;
  height: 16px;
  border: 2px solid var(--border-color);
  border-radius: 4px;
  background-color: #ffffff;
  position: relative;
  flex-shrink: 0;
}
.hidden-checkbox:checked + .styled-checkbox {
  background-color: var(--primary-color);
  border-color: var(--primary-color);
}
.hidden-checkbox:checked + .styled-checkbox::after {
  content: '';
  position: absolute;
  left: 4px;
  top: 1px;
  width: 4px;
  height: 7px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}
.task-meta { display: flex; flex-direction: column; }
.task-title { font-size: 11px; font-weight: 600; }
.task-title.strikethrough { text-decoration: line-through; color: var(--text-muted); }
.badge-priority { font-size: 8px; padding: 2px 6px; border-radius: 999px; text-transform: uppercase; font-weight: 700; }
.badge-priority.high { color: var(--error); background-color: var(--error-light); }
.badge-priority.medium { color: var(--warning); background-color: var(--warning-light); }
.badge-priority.low { color: var(--info); background-color: var(--info-light); }

/* Availability indicators */
.availability-stack {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.avail-strip {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  border: 1px solid var(--border-color);
  background-color: var(--bg-main);
  border-radius: var(--radius-sm);
}
.status-dot { width: 6px; height: 6px; border-radius: 50%; margin-right: 10px; }
.status-dot.active { background-color: var(--success); }
.status-dot.wfh { background-color: var(--primary-color); }
.status-dot.offline { background-color: var(--text-muted); }
.avail-name { flex: 1; font-size: 12px; font-weight: 600; }

.anniversary-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border: 1px solid var(--success);
  border-radius: var(--radius-sm);
  background-color: var(--success-light);
}
.anniversary-icon { font-size: 18px; }

/* 5. QUICK ACTIONS IN 100% ROW */
.quick-actions-bar-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}
.quick-action-bar-btn {
  background-color: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-family: var(--font-family);
  font-weight: 600;
  font-size: 13px;
  color: var(--text-primary);
  cursor: pointer;
  transition: all var(--transition-fast);
}
.quick-action-bar-btn:hover {
  background-color: var(--secondary-light);
  border-color: #cbd5e1;
  transform: translateY(-1px);
}

/* Modals */
.btn-close-modal { background: none; border: none; font-size: 24px; cursor: pointer; color: var(--text-muted); }
.detail-grid-modal { display: flex; flex-direction: column; gap: 12px; }
.detail-modal-row { display: flex; justify-content: space-between; font-size: 13px; }
.modal-label { color: var(--text-secondary); font-weight: 500; }
.modal-value { color: var(--text-primary); text-align: right; }
.modal-value-box { border: 1px solid var(--border-color); max-height: 100px; overflow-y: auto; }
.modal-actions { display: flex; gap: 12px; }
.bg-main { background-color: var(--bg-main); }
.border-t-none { border-top: none; }

.avatar-circle-xs {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: var(--primary-light);
  color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
}

.flex-header-container, .section-title-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* Punch Desk Card Styles */
.punch-desk-card {
  padding: 24px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-fast);
}
.punch-desk-card:hover {
  border-color: rgba(37, 99, 235, 0.2);
  box-shadow: var(--shadow-md);
}
.punch-desk-grid {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}
.punch-desk-left {
  flex: 1;
  min-width: 280px;
}
.punch-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--secondary-color);
  margin-bottom: 6px;
}
.punch-time-display {
  font-size: 20px;
  font-weight: 800;
  color: var(--primary-color);
  margin-bottom: 8px;
  font-family: 'Inter', monospace;
  letter-spacing: -0.02em;
}
.punch-subtitle {
  font-size: 13px;
  color: var(--text-secondary);
}
.punch-subtitle.success {
  color: var(--success);
  display: flex;
  align-items: center;
  gap: 6px;
}
.punch-subtitle.completed {
  color: var(--text-muted);
}
.pulse-dot {
  width: 8px;
  height: 8px;
  background-color: var(--success);
  border-radius: 50%;
  animation: pulse 1.5s infinite;
}
@keyframes pulse {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(16, 185, 129, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
}
.punch-desk-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  min-width: 200px;
}
.btn-punch {
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 700;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all var(--transition-fast);
}
.mode-select-wrapper {
  display: flex;
  align-items: center;
}
.d-inline-block {
  display: inline-block;
}
.mr-2 {
  margin-right: 8px;
}
</style>
