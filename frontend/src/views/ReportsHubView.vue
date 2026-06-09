<script setup>
import { ref, computed } from 'vue';
import { useAuthStore } from '../stores/authStore.js';
import { useHrStore } from '../stores/hrStore.js';
import IconHelper from '../components/IconHelper.vue';

const authStore = useAuthStore();
const hrStore = useHrStore();

const activeTab = ref('headcount');

// Department filters

// Export mock reports
function triggerExport(reportName) {
  window.showPortalToast(`Generating ${reportName} data...`, 'info');
  setTimeout(() => {
    // Generate CSV data download
    let csv = 'data:text/csv;charset=utf-8,';
    if (reportName === 'headcount') {
      csv += 'Department,Headcount,Percentage\r\n';
      csv += 'Engineering,3,60%\r\nHR,1,20%\r\nFinance,1,20%\r\n';
    } else if (reportName === 'leave') {
      csv += 'Employee Code,Name,Leave Allowed,Leave Used,Utilisation %\r\n';
      csv += 'EMP2026101,Jane Doe,42,6,14.3%\r\nEMP2023102,Sarah Jenkins,47,6,12.7%\r\n';
    } else {
      csv += 'Month,Meals,Travel,Accommodation,Total Claimed\r\n';
      csv += 'May 2026,0,8500,0,8500\r\nJune 2026,1500,0,4500,6000\r\n';
    }
    
    const uri = encodeURI(csv);
    const link = document.createElement('a');
    link.setAttribute('href', uri);
    link.setAttribute('download', `${reportName}_report_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    window.showPortalToast('Report exported successfully as CSV', 'success');
  }, 800);
}

// 1. Headcount Computations
const headcountData = computed(() => {
  const depts = {};
  hrStore.employees.forEach(e => {
    depts[e.department] = (depts[e.department] || 0) + 1;
  });
  
  const total = hrStore.employees.length;
  return Object.keys(depts).map(d => ({
    name: d,
    count: depts[d],
    percent: Math.round((depts[d] / total) * 100)
  }));
});

const activeHeadcountCount = computed(() => {
  return hrStore.employees.filter(e => e.status === 'ACTIVE').length;
});

// 2. Leave Utilisation Computations
const leaveUtilisationData = computed(() => {
  return hrStore.employees.map(emp => {
    const balances = hrStore.leaveBalances[emp.employeeCode] || [];
    let totalAccrued = 0;
    let totalUsed = 0;
    
    balances.forEach(b => {
      totalAccrued += (b.accrued + b.carryForward);
      totalUsed += b.used;
    });
    
    const utilPercent = totalAccrued > 0 ? Math.round((totalUsed / totalAccrued) * 100) : 0;
    
    return {
      name: emp.fullName,
      code: emp.employeeCode,
      dept: emp.department,
      allowed: totalAccrued,
      used: totalUsed,
      percent: utilPercent
    };
  });
});

const zeroLeaveEmployees = computed(() => {
  return leaveUtilisationData.value.filter(u => u.used === 0);
});

// Average utilisation by dept
const avgLeaveUtilByDept = computed(() => {
  const depts = {};
  leaveUtilisationData.value.forEach(u => {
    if (!depts[u.dept]) depts[u.dept] = { total: 0, count: 0 };
    depts[u.dept].total += u.percent;
    depts[u.dept].count++;
  });
  return Object.keys(depts).map(d => ({
    name: d,
    avg: Math.round(depts[d].total / depts[d].count)
  }));
});

// 3. Expense Analytics Computations
const totalClaimedAmount = computed(() => {
  return hrStore.expenseClaims.reduce((sum, c) => sum + c.amount, 0);
});

const totalApprovedAmount = computed(() => {
  return hrStore.expenseClaims.filter(c => ['APPROVED_FINANCE', 'PAID'].includes(c.status))
                              .reduce((sum, c) => sum + c.amount, 0);
});

const totalPaidAmount = computed(() => {
  return hrStore.expenseClaims.filter(c => c.status === 'PAID')
                              .reduce((sum, c) => sum + c.amount, 0);
});

const expenseCategoryStats = computed(() => {
  const categories = {};
  hrStore.expenseClaims.forEach(c => {
    categories[c.category] = (categories[c.category] || 0) + c.amount;
  });
  const total = totalClaimedAmount.value || 1;
  return Object.keys(categories).map(cat => ({
    name: cat,
    amount: categories[cat],
    percent: Math.round((categories[cat] / total) * 100)
  }));
});
</script>

<template>
  <div class="reports-hub">
    <!-- SUB-TABS -->
    <div class="tabs-container">
      <div class="tabs">
        <button
          v-if="authStore.activeRole === 'HR_ADMIN'"
          class="tab-btn"
          :class="{ active: activeTab === 'headcount' }"
          @click="activeTab = 'headcount'"
        >
          Headcount Report
        </button>
        <button
          v-if="authStore.activeRole === 'HR_ADMIN'"
          class="tab-btn"
          :class="{ active: activeTab === 'leave' }"
          @click="activeTab = 'leave'"
        >
          Leave Utilisation Report
        </button>
        <button
          v-if="['HR_ADMIN', 'FINANCE_ADMIN'].includes(authStore.activeRole)"
          class="tab-btn"
          :class="{ active: activeTab === 'expense' }"
          @click="activeTab = 'expense'"
        >
          Expense Summary Report
        </button>
      </div>
    </div>

    <!-- TAB 1: HEADCOUNT ANALYTICS -->
    <template v-if="activeTab === 'headcount' && authStore.activeRole === 'HR_ADMIN'">
      <div class="summary-grid">
        <div class="summary-card glass-card">
          <div class="summary-card-icon bg-primary-light text-primary">
            <IconHelper name="users" size="20" />
          </div>
          <div>
            <div class="summary-card-label">Total Organization Headcount</div>
            <div class="summary-card-value">{{ hrStore.employees.length }} <span class="text-xs font-normal">profiles</span></div>
          </div>
        </div>

        <div class="summary-card glass-card">
          <div class="summary-card-icon bg-success-light text-success">
            <IconHelper name="check-circle" size="20" />
          </div>
          <div>
            <div class="summary-card-label">Active Credentials</div>
            <div class="summary-card-value">{{ activeHeadcountCount }} <span class="text-xs font-normal">accounts</span></div>
          </div>
        </div>

        <div class="summary-card glass-card">
          <div class="summary-card-icon bg-warning-light text-warning">
            <IconHelper name="clock" size="20" />
          </div>
          <div>
            <div class="summary-card-label">Pending Onboarding</div>
            <div class="summary-card-value">
              {{ hrStore.employees.filter(e => e.onboardingPercent < 100).length }} <span class="text-xs font-normal">in progress</span>
            </div>
          </div>
        </div>
      </div>

      <div class="grid-12">
        <!-- left charts -->
        <div class="col-6">
          <div class="glass-card h-full">
            <div class="flex justify-between items-center mb-6">
              <h3>Department Breakdowns</h3>
              <button class="btn btn-secondary btn-sm" @click="triggerExport('headcount')">Export Headcount</button>
            </div>
            
            <div class="chart-bar-container">
              <div v-for="dept in headcountData" :key="dept.name" class="chart-bar-row">
                <span class="chart-bar-label">{{ dept.name }}</span>
                <div class="chart-bar-track">
                  <div class="chart-bar-fill" :style="`width: ${dept.percent}%`"></div>
                </div>
                <span class="chart-bar-value font-mono">{{ dept.count }} ({{ dept.percent }}%)</span>
              </div>
            </div>
          </div>
        </div>

        <!-- right listings -->
        <div class="col-6">
          <div class="glass-card h-full">
            <h3>Registered Employees</h3>
            <div class="table-container mt-4">
              <table class="table">
                <thead>
                  <tr>
                    <th>Code</th>
                    <th>Name</th>
                    <th>Dept</th>
                    <th>Designation</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="emp in hrStore.employees" :key="emp.id">
                    <td class="font-mono text-xs">{{ emp.employeeCode }}</td>
                    <td class="font-bold text-xs">{{ emp.fullName }}</td>
                    <td><span class="badge badge-muted">{{ emp.department }}</span></td>
                    <td class="text-xs text-secondary">{{ emp.designation }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- TAB 2: LEAVE UTILISATION -->
    <template v-if="activeTab === 'leave' && authStore.activeRole === 'HR_ADMIN'">
      <div class="grid-12">
        <!-- left: averages & warnings -->
        <div class="col-4">
          <div class="glass-card mb-6">
            <h3>Average Utilisation by Dept</h3>
            <div class="chart-bar-container mt-4">
              <div v-for="d in avgLeaveUtilByDept" :key="d.name" class="chart-bar-row">
                <span class="chart-bar-label text-xs">{{ d.name }}</span>
                <div class="chart-bar-track" style="height: 8px;">
                  <div class="chart-bar-fill" :style="`width: ${d.avg}%; background: var(--success);`"></div>
                </div>
                <span class="chart-bar-value font-mono text-xs">{{ d.avg }}%</span>
              </div>
            </div>
          </div>

          <div class="glass-card alert-card">
            <div class="flex items-center gap-2 mb-2 text-warning font-bold text-sm">
              <IconHelper name="clock" size="18" />
              <span>Zero-Leave Warning List</span>
            </div>
            <p class="text-xs text-secondary mb-4">
              These employees have taken 0 days of leave this year. Alert them to schedule vacation.
            </p>
            <div class="table-container">
              <table class="table text-xs">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Dept</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="emp in zeroLeaveEmployees" :key="emp.code">
                    <td class="font-bold text-xs">{{ emp.name }}</td>
                    <td>{{ emp.dept }}</td>
                  </tr>
                  <tr v-if="zeroLeaveEmployees.length === 0">
                    <td colspan="2" class="text-center text-xs text-secondary">All employees have taken leave.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- right: main table -->
        <div class="col-8">
          <div class="glass-card h-full">
            <div class="flex justify-between items-center mb-6">
              <h3>Employee Utilisation Ratios</h3>
              <button class="btn btn-secondary btn-sm" @click="triggerExport('leave')">Export Leave Data</button>
            </div>

            <div class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>Employee</th>
                    <th>Dept</th>
                    <th>Accrued Days</th>
                    <th>Used Days</th>
                    <th>Utilisation %</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="u in leaveUtilisationData" :key="u.code">
                    <td>
                      <div class="flex flex-col">
                        <span class="font-bold text-xs">{{ u.name }}</span>
                        <span class="font-mono text-xs text-secondary">{{ u.code }}</span>
                      </div>
                    </td>
                    <td><span class="badge badge-muted">{{ u.dept }}</span></td>
                    <td class="font-mono">{{ u.allowed }}</td>
                    <td class="font-mono text-success">{{ u.used }}</td>
                    <td>
                      <div class="flex items-center gap-2">
                        <div class="chart-bar-track w-80" style="height: 6px;">
                          <div class="chart-bar-fill" :style="`width: ${u.percent}%; background: var(--border-focus);`"></div>
                        </div>
                        <span class="font-mono font-bold">{{ u.percent }}%</span>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- TAB 3: EXPENSE SUMMARY -->
    <template v-if="activeTab === 'expense' && ['HR_ADMIN', 'FINANCE_ADMIN'].includes(authStore.activeRole)">
      <!-- metrics -->
      <div class="summary-grid">
        <div class="summary-card glass-card">
          <div class="summary-card-icon bg-primary-light text-primary">
            <IconHelper name="credit-card" size="20" />
          </div>
          <div>
            <div class="summary-card-label">Total Expense Claimed</div>
            <div class="summary-card-value">INR {{ totalClaimedAmount.toLocaleString() }}</div>
          </div>
        </div>

        <div class="summary-card glass-card">
          <div class="summary-card-icon bg-success-light text-success">
            <IconHelper name="check-circle" size="20" />
          </div>
          <div>
            <div class="summary-card-label">Approved by Finance</div>
            <div class="summary-card-value">INR {{ totalApprovedAmount.toLocaleString() }}</div>
          </div>
        </div>

        <div class="summary-card glass-card">
          <div class="summary-card-icon bg-info-light text-info">
            <IconHelper name="dollar" size="20" />
          </div>
          <div>
            <div class="summary-card-label">Disbursed (Paid)</div>
            <div class="summary-card-value">INR {{ totalPaidAmount.toLocaleString() }}</div>
          </div>
        </div>
      </div>

      <div class="grid-12">
        <!-- Category shares horizontal chart -->
        <div class="col-6">
          <div class="glass-card h-full">
            <div class="flex justify-between items-center mb-6">
              <h3>Category Expenditures</h3>
              <button class="btn btn-secondary btn-sm" @click="triggerExport('expense')">Export Expense Summary</button>
            </div>

            <div class="chart-bar-container">
              <div v-for="cat in expenseCategoryStats" :key="cat.name" class="chart-bar-row">
                <span class="chart-bar-label">{{ cat.name }}</span>
                <div class="chart-bar-track">
                  <div class="chart-bar-fill" :style="`width: ${cat.percent}%; background: var(--info);`"></div>
                </div>
                <span class="chart-bar-value font-mono">INR {{ cat.amount.toLocaleString() }} ({{ cat.percent }}%)</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Month over month line charts mock -->
        <div class="col-6">
          <div class="glass-card h-full">
            <h3>Month-over-Month Claim Trends</h3>
            <div class="flex items-end justify-between h-150 pt-8" style="border-bottom: 2px solid var(--border-color)">
              
              <!-- Jan -->
              <div class="flex flex-col items-center flex-1 gap-2">
                <span class="text-xs font-mono text-secondary">12k</span>
                <div class="w-20 rounded bg-primary-light" style="height: 60px;">
                  <div class="w-full bg-primary rounded-bottom" style="height: 35px; border-radius: 3px;"></div>
                </div>
                <span class="text-xs text-muted">Jan</span>
              </div>

              <!-- Feb -->
              <div class="flex flex-col items-center flex-1 gap-2">
                <span class="text-xs font-mono text-secondary">8.5k</span>
                <div class="w-20 rounded bg-primary-light" style="height: 60px;">
                  <div class="w-full bg-primary rounded-bottom" style="height: 20px; border-radius: 3px;"></div>
                </div>
                <span class="text-xs text-muted">Feb</span>
              </div>

              <!-- Mar -->
              <div class="flex flex-col items-center flex-1 gap-2">
                <span class="text-xs font-mono text-secondary">15k</span>
                <div class="w-20 rounded bg-primary-light" style="height: 60px;">
                  <div class="w-full bg-primary rounded-bottom" style="height: 48px; border-radius: 3px;"></div>
                </div>
                <span class="text-xs text-muted">Mar</span>
              </div>

              <!-- Apr -->
              <div class="flex flex-col items-center flex-1 gap-2">
                <span class="text-xs font-mono text-secondary">19k</span>
                <div class="w-20 rounded bg-primary-light" style="height: 60px;">
                  <div class="w-full bg-primary rounded-bottom" style="height: 55px; border-radius: 3px;"></div>
                </div>
                <span class="text-xs text-muted">Apr</span>
              </div>

              <!-- May -->
              <div class="flex flex-col items-center flex-1 gap-2">
                <span class="text-xs font-mono text-secondary">14.5k</span>
                <div class="w-20 rounded bg-primary-light" style="height: 60px;">
                  <div class="w-full bg-primary rounded-bottom" style="height: 42px; border-radius: 3px;"></div>
                </div>
                <span class="text-xs text-muted">May</span>
              </div>

            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.reports-hub {
  animation: fadeIn 0.4s ease;
}

.text-danger { color: var(--error); }
.text-success { color: var(--success); }

.w-80 { width: 80px; }
.w-20 { width: 20px; }
.h-150 { height: 150px; }

.font-normal { font-weight: 400; }
.font-semibold { font-weight: 600; }
.font-bold { font-weight: 700; }
.font-mono { font-family: monospace; }
.text-xs { font-size: 11px; }
.text-sm { font-size: 13px; }
.text-lg { font-size: 18px; }

.flex { display: flex; }
.flex-col { display: flex; flex-direction: column; }
.flex-1 { flex: 1; }
.items-center { align-items: center; }
.items-end { align-items: flex-end; }
.justify-between { justify-content: space-between; }
.gap-2 { gap: 8px; }

.mb-2 { margin-bottom: 8px; }
.mb-4 { margin-bottom: 16px; }
.mb-6 { margin-bottom: 24px; }
.mt-1 { margin-top: 4px; }
.mt-4 { margin-top: 16px; }
.pt-8 { padding-top: 32px; }
.pl-4 { padding-left: 16px; }
.p-8 { padding: 32px; }
.p-6 { padding: 24px; }
.list-disc { list-style-type: disc; }

.rounded { border-radius: var(--radius-sm); }
.rounded-bottom { border-bottom-left-radius: 3px; border-bottom-right-radius: 3px; }
.text-center { text-align: center; }
</style>
