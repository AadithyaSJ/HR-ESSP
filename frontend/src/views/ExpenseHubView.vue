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

const activeTab = ref(route.meta.tab || 'my-expenses');

// Claim Form State
const expenseCategory = ref('Meals');
const expenseAmount = ref('');
const expenseCurrency = ref('INR');
const expenseDate = ref(new Date().toISOString().split('T')[0]);
const expenseDesc = ref('');
const uploadedFile = ref(null);
const isDragging = ref(false);

// Finance Review Modal State
const showReviewModal = ref(false);
const activeClaim = ref(null);
const reviewAction = ref(''); // APPROVE, REJECT, PAY
const reviewReason = ref('');
const paymentRefInput = ref('');
const errorMsg = ref('');

// Limits Admin State
const limitCategory = ref('Meals');
const limitAmount = ref('');
const limitFreq = ref('PER_DAY');
const limitGrade = ref('All Grades');

// Currency State
const newCurrencyCode = ref('');
const newCurrencyRate = ref('');

onMounted(() => {
  activeTab.value = route.meta.tab || 'my-expenses';
});

function changeTab(tab) {
  activeTab.value = tab;
  router.push(tab === 'my-expenses' ? '/expense' : tab === 'approvals' ? '/manager/expense' : tab === 'finance-queue' ? '/finance/expense' : tab === 'limits' ? '/admin/expense-limits' : '/admin/currency-rates');
}

// User claims
const myClaims = computed(() => {
  return hrStore.expenseClaims.filter(c => c.employeeCode === authStore.user.employeeCode);
});

// Drag & drop receipts
function handleReceiptDrop(e) {
  isDragging.value = false;
  const files = e.dataTransfer.files;
  if (files.length > 0) {
    uploadedFile.value = files[0];
    window.showPortalToast(`Receipt file "${files[0].name}" linked to claim.`, 'info');
  }
}

function handleReceiptSelect(e) {
  const files = e.target.files;
  if (files.length > 0) {
    uploadedFile.value = files[0];
    window.showPortalToast(`Receipt file "${files[0].name}" linked to claim.`, 'info');
  }
}

// Submit claim
function handleSendClaim() {
  if (!expenseAmount.value || !expenseDate.value || !expenseDesc.value) {
    window.showPortalToast('Please fill all required claim details', 'error');
    return;
  }

  // Check Limit Policy
  const limitRule = hrStore.expenseLimits.find(l => l.category === expenseCategory.value);
  if (limitRule && parseFloat(expenseAmount.value) > limitRule.maxAmount) {
    window.showPortalToast(`⚠️ Request exceeds policy category limit of ${limitRule.maxAmount}. Submitted anyway for approval.`, 'warning');
  }

  const payload = {
    employeeCode: authStore.user.employeeCode,
    fullName: authStore.user.fullName,
    department: authStore.user.department,
    category: expenseCategory.value,
    amount: parseFloat(expenseAmount.value),
    currency: expenseCurrency.value,
    date: expenseDate.value,
    description: expenseDesc.value,
    receiptName: uploadedFile.value ? uploadedFile.value.name : 'uploaded_receipt.pdf'
  };

  hrStore.submitExpenseClaim(payload, authStore.user.email);
  window.showPortalToast('Expense claim submitted for approval', 'success');

  // Reset
  expenseAmount.value = '';
  expenseDesc.value = '';
  uploadedFile.value = null;
}

function handleCancelClaim(id) {
  const success = hrStore.cancelExpenseClaim(id, authStore.user.email);
  if (success) {
    window.showPortalToast('Pending expense claim cancelled', 'info');
  }
}

// Manager Approvals
const managerApprovalsList = computed(() => {
  return hrStore.expenseClaims.filter(c => c.status === 'PENDING');
});

function handleManagerApprove(id) {
  hrStore.approveExpenseManager(id, authStore.user.fullName, authStore.user.email);
  window.showPortalToast('Expense claim approved. Forwarded to Finance Queue.', 'success');
}

function handleManagerReject(req) {
  activeClaim.value = req;
  reviewAction.value = 'REJECT';
  reviewReason.value = '';
  showReviewModal.value = true;
}

// Finance Queue
const financeQueueList = computed(() => {
  return hrStore.expenseClaims.filter(c => ['APPROVED_MANAGER', 'APPROVED_FINANCE'].includes(c.status));
});

function openFinanceReview(claim, action) {
  activeClaim.value = claim;
  reviewAction.value = action;
  reviewReason.value = '';
  paymentRefInput.value = claim.paymentRef || '';
  errorMsg.value = '';
  showReviewModal.value = true;
}

function handleConfirmFinanceAction() {
  if (reviewAction.value === 'REJECT' && !reviewReason.value) {
    errorMsg.value = 'Rejection reason is mandatory';
    return;
  }
  if (reviewAction.value === 'APPROVE' && !paymentRefInput.value) {
    errorMsg.value = 'Payment transaction reference is required';
    return;
  }

  if (reviewAction.value === 'REJECT') {
    hrStore.rejectExpense(activeClaim.value.id, authStore.user.fullName, reviewReason.value, authStore.user.email);
    window.showPortalToast('Claim rejected and sent back to employee', 'warning');
  } else if (reviewAction.value === 'APPROVE') {
    hrStore.approveExpenseFinance(activeClaim.value.id, authStore.user.fullName, paymentRefInput.value, authStore.user.email);
    window.showPortalToast('Claim approved for payout release', 'success');
  } else if (reviewAction.value === 'PAY') {
    hrStore.markExpensePaid(activeClaim.value.id, authStore.user.email);
    window.showPortalToast('Claim marked as PAID', 'success');
  }

  showReviewModal.value = false;
  activeClaim.value = null;
}

// Export approved to payroll CSV
function exportApprovedClaims() {
  let csv = 'data:text/csv;charset=utf-8,';
  csv += 'Claim ID,Employee ID,Full Name,Category,Amount,Currency,Date,Payment Reference\r\n';
  
  financeQueueList.value.forEach(c => {
    csv += `"${c.id}","${c.employeeCode}","${c.fullName}","${c.category}","${c.amount}","${c.currency}","${c.date}","${c.paymentRef || ''}"\r\n`;
  });
  
  const uri = encodeURI(csv);
  const link = document.createElement('a');
  link.setAttribute('href', uri);
  link.setAttribute('download', `approved_expenses_${new Date().toISOString().split('T')[0]}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  hrStore.addLog(authStore.user.email, 'Expense', 'EXPORT_FINANCE_CLAIMS', 'Exported approved finance claims to CSV');
}

// Duplicate checker simulation
function checkDuplicate(claim) {
  // Simple check: same employee, same amount, same category on same date
  const matches = hrStore.expenseClaims.filter(c => 
    c.id !== claim.id &&
    c.employeeCode === claim.employeeCode &&
    c.amount === claim.amount &&
    c.category === claim.category &&
    c.date === claim.date
  );
  return matches.length > 0;
}

// Limits policy creator
function handleAddLimit() {
  if (!limitAmount.value) return;
  hrStore.expenseLimits.push({
    id: `lim-${Date.now()}`,
    category: limitCategory.value,
    maxAmount: parseFloat(limitAmount.value),
    frequency: limitFreq.value,
    grade: limitGrade.value
  });
  window.showPortalToast('Expense limit policy updated', 'success');
  limitAmount.value = '';
}

function handleDeactivateLimit(id) {
  hrStore.expenseLimits = hrStore.expenseLimits.filter(l => l.id !== id);
  window.showPortalToast('Expense limit policy deactivated', 'info');
}

// Currency rate creator
function handleAddCurrency() {
  if (!newCurrencyCode.value || !newCurrencyRate.value) return;
  hrStore.currencyRates.push({
    code: newCurrencyCode.value.toUpperCase(),
    rate: parseFloat(newCurrencyRate.value),
    symbol: newCurrencyCode.value.toUpperCase()
  });
  window.showPortalToast('Exchange rates adjusted', 'success');
  newCurrencyCode.value = '';
  newCurrencyRate.value = '';
}
</script>

<template>
  <div class="expense-hub">
    <!-- SUB-TABS -->
    <div class="tabs-container">
      <div class="tabs">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'my-expenses' }"
          @click="changeTab('my-expenses')"
        >
          My Expenses
        </button>
        <button
          v-if="['MANAGER', 'HR_ADMIN'].includes(authStore.activeRole)"
          class="tab-btn"
          :class="{ active: activeTab === 'approvals' }"
          @click="changeTab('approvals')"
        >
          Team Expense Approvals
        </button>
        <button
          v-if="['FINANCE_ADMIN', 'HR_ADMIN'].includes(authStore.activeRole)"
          class="tab-btn"
          :class="{ active: activeTab === 'finance-queue' }"
          @click="changeTab('finance-queue')"
        >
          Finance Queue
        </button>
        <button
          v-if="authStore.activeRole === 'HR_ADMIN'"
          class="tab-btn"
          :class="{ active: activeTab === 'limits' }"
          @click="changeTab('limits')"
        >
          Expense Limits Config
        </button>
        <button
          v-if="['HR_ADMIN', 'FINANCE_ADMIN'].includes(authStore.activeRole)"
          class="tab-btn"
          :class="{ active: activeTab === 'currencies' }"
          @click="changeTab('currencies')"
        >
          Currency Rates
        </button>
      </div>
    </div>

    <!-- TAB 1: MY CLAIMS -->
    <template v-if="activeTab === 'my-expenses'">
      <div class="grid-12">
        <!-- Submit form -->
        <div class="col-4">
          <div class="glass-card">
            <h3 class="mb-6">Submit Expense Claim</h3>
            <form @submit.prevent="handleSendClaim">
              <div class="form-group">
                <label class="form-label">Expense Category</label>
                <select v-model="expenseCategory" class="form-control">
                  <option value="Travel">Travel / Flights</option>
                  <option value="Meals">Meals / Client Dinner</option>
                  <option value="Accommodation">Accommodation / Hotel</option>
                  <option value="Other">Other Expenses</option>
                </select>
              </div>

              <div class="grid-12 m-0">
                <div class="col-6 form-group">
                  <label class="form-label">Amount</label>
                  <input type="number" v-model="expenseAmount" class="form-control" placeholder="0.00" required />
                </div>
                <div class="col-6 form-group">
                  <label class="form-label">Currency</label>
                  <select v-model="expenseCurrency" class="form-control">
                    <option v-for="c in hrStore.currencyRates" :key="c.code" :value="c.code">{{ c.code }} ({{ c.symbol }})</option>
                  </select>
                </div>
              </div>

              <div class="form-group">
                <label class="form-label">Transaction Date</label>
                <input type="date" v-model="expenseDate" class="form-control" required />
              </div>

              <div class="form-group">
                <label class="form-label">Description</label>
                <textarea v-model="expenseDesc" class="form-control" placeholder="Specify invoice details..." required></textarea>
              </div>

              <!-- Drag receipt -->
              <div class="form-group">
                <label class="form-label">Receipt Proof</label>
                <div
                  class="upload-zone py-6"
                  :class="{ active: isDragging }"
                  @dragover.prevent="isDragging = true"
                  @dragleave="isDragging = false"
                  @drop.prevent="handleReceiptDrop"
                  @click="$refs.receiptInput.click()"
                >
                  <input type="file" ref="receiptInput" style="display:none" @change="handleReceiptSelect" />
                  <IconHelper name="upload" size="24" color="var(--primary-color)" class="mb-2" />
                  <p class="text-xs font-semibold" v-if="!uploadedFile">Drop Invoice PDF or click to browse</p>
                  <p class="text-xs text-success font-semibold" v-else>📎 Linked: {{ uploadedFile.name }}</p>
                </div>
              </div>

              <button type="submit" class="btn btn-primary btn-full">
                Submit Claim
              </button>
            </form>
          </div>
        </div>

        <!-- Claims log -->
        <div class="col-8">
          <div class="glass-card">
            <h3 class="mb-6">Expense Claims History</h3>
            <div class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>Category</th>
                    <th>Date</th>
                    <th>Amount</th>
                    <th>Receipt</th>
                    <th>Status</th>
                    <th class="text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="c in myClaims" :key="c.id">
                    <td>
                      <span class="font-semibold">{{ c.category }}</span>
                    </td>
                    <td>{{ c.date }}</td>
                    <td class="font-mono">{{ c.currency }} {{ c.amount }}</td>
                    <td>
                      <a href="#" class="text-xs flex items-center gap-1 font-mono" @click.prevent="window.showPortalToast('Opening receipt PDF preview...', 'info')">
                        <IconHelper name="file-text" size="14" />
                        {{ c.receiptName }}
                      </a>
                    </td>
                    <td>
                      <span
                        class="badge"
                        :class="c.status === 'PAID' ? 'badge-success' : c.status === 'APPROVED_FINANCE' ? 'badge-success' : c.status === 'APPROVED_MANAGER' ? 'badge-info' : c.status === 'PENDING' ? 'badge-warning' : 'badge-danger'"
                      >
                        {{ c.status.replace('_', ' ') }}
                      </span>
                    </td>
                    <td class="text-right">
                      <div class="flex gap-2 justify-end">
                        <button class="btn btn-ghost btn-sm px-2" @click="openFinanceReview(c, 'VIEW')" title="View approval history timeline">
                          <IconHelper name="clock" size="14" />
                        </button>
                        <button v-if="c.status === 'PEND'" class="btn btn-danger btn-sm px-2 py-6" @click="handleCancelClaim(c.id)">
                          Cancel
                        </button>
                      </div>
                    </td>
                  </tr>
                  <tr v-if="myClaims.length === 0">
                    <td colspan="6" class="text-center text-secondary">No expense claims logged.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- TAB 2: MANAGER APPROVALS -->
    <template v-if="activeTab === 'approvals' && ['MANAGER', 'HR_ADMIN'].includes(authStore.activeRole)">
      <div class="glass-card">
        <h3 class="mb-6">Pending Manager Approvals</h3>
        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>Employee</th>
                <th>Category</th>
                <th>Amount</th>
                <th>Date</th>
                <th>Reason</th>
                <th>Receipt</th>
                <th class="text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in managerApprovalsList" :key="c.id">
                <td>
                  <div class="flex flex-col">
                    <span class="font-bold">{{ c.fullName }}</span>
                    <span class="text-xs text-secondary font-mono">{{ c.employeeCode }}</span>
                  </div>
                </td>
                <td>{{ c.category }}</td>
                <td class="font-mono">{{ c.currency }} {{ c.amount }}</td>
                <td>{{ c.date }}</td>
                <td class="text-truncate text-secondary max-width-200" :title="c.description">{{ c.description }}</td>
                <td>
                  <a href="#" class="text-xs flex items-center gap-1 font-mono" @click.prevent="window.showPortalToast('Opening Invoice attachment...', 'info')">
                    <IconHelper name="file-text" size="14" />
                    {{ c.receiptName }}
                  </a>
                </td>
                <td class="text-right">
                  <div class="flex gap-2 justify-end">
                    <button class="btn btn-success btn-sm px-2 py-6" @click="handleManagerApprove(c.id)">Approve</button>
                    <button class="btn btn-danger btn-sm px-2 py-6" @click="handleManagerReject(c)">Reject</button>
                  </div>
                </td>
              </tr>
              <tr v-if="managerApprovalsList.length === 0">
                <td colspan="7" class="text-center text-secondary p-8">No pending manager expense approvals.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- TAB 3: FINANCE QUEUE -->
    <template v-if="activeTab === 'finance-queue' && ['FINANCE_ADMIN', 'HR_ADMIN'].includes(authStore.activeRole)">
      <div class="glass-card">
        <div class="flex justify-between items-center mb-6">
          <h3>Finance Audit Queue</h3>
          <button class="btn btn-secondary btn-sm" @click="exportApprovedClaims">
            <IconHelper name="download" size="14" />
            Export Approved for Accounting System
          </button>
        </div>

        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>Employee</th>
                <th>Category</th>
                <th>Amount</th>
                <th>Date</th>
                <th>Receipt</th>
                <th>Integrity Audit</th>
                <th>Status</th>
                <th class="text-right">Audit Decision</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in financeQueueList" :key="c.id">
                <td>
                  <div class="flex flex-col">
                    <span class="font-bold">{{ c.fullName }}</span>
                    <span class="text-xs text-secondary font-mono">{{ c.employeeCode }}</span>
                  </div>
                </td>
                <td>{{ c.category }}</td>
                <td class="font-mono">{{ c.currency }} {{ c.amount }}</td>
                <td>{{ c.date }}</td>
                <td>
                  <a href="#" class="text-xs flex items-center gap-1 font-mono" @click.prevent="window.showPortalToast('Downloading receipt file from S3...', 'success')">
                    <IconHelper name="download" size="14" />
                    Receipt
                  </a>
                </td>
                <td>
                  <!-- Simulated duplicate checker flags -->
                  <span v-if="checkDuplicate(c)" class="badge badge-danger">⚠️ Potential Duplicate</span>
                  <span v-else class="badge badge-success">✓ Unique Submission</span>
                </td>
                <td>
                  <span class="badge" :class="c.status === 'APPROVED_FINANCE' ? 'badge-success' : 'badge-info'">
                    {{ c.status.replace('_', ' ') }}
                  </span>
                  <div v-if="c.paymentRef" class="text-xs font-mono text-muted mt-1">Ref: {{ c.paymentRef }}</div>
                </td>
                <td class="text-right">
                  <div class="flex gap-2 justify-end">
                    <button v-if="c.status === 'APPROVED_MANAGER'" class="btn btn-primary btn-sm px-2 py-6" @click="openFinanceReview(c, 'APPROVE')">
                      Approve Payout
                    </button>
                    <button v-if="c.status === 'APPROVED_FINANCE'" class="btn btn-success btn-sm px-2 py-6" @click="openFinanceReview(c, 'PAY')">
                      Mark Paid
                    </button>
                    <button v-if="c.status === 'APPROVED_MANAGER'" class="btn btn-danger btn-sm px-2 py-6" @click="openFinanceReview(c, 'REJECT')">
                      Reject
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="financeQueueList.length === 0">
                <td colspan="8" class="text-center text-secondary p-8">No claims awaiting finance settlement.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- TAB 4: EXPENSE LIMITS -->
    <template v-if="activeTab === 'limits' && authStore.activeRole === 'HR_ADMIN'">
      <div class="grid-12">
        <!-- List Limits -->
        <div class="col-8">
          <div class="glass-card">
            <h3 class="mb-6">Expense Policy Limit Rules</h3>
            <div class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>Category</th>
                    <th>Maximum Cap Limit</th>
                    <th>Frequency</th>
                    <th>Grade Eligibility</th>
                    <th class="text-right">Action</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="l in hrStore.expenseLimits" :key="l.id">
                    <td class="font-semibold">{{ l.category }}</td>
                    <td class="font-mono">INR {{ l.maxAmount }}</td>
                    <td>{{ l.frequency.replace('_', ' ') }}</td>
                    <td>{{ l.grade }}</td>
                    <td class="text-right">
                      <button class="btn btn-ghost btn-sm text-danger-btn px-2" @click="handleDeactivateLimit(l.id)">
                        <IconHelper name="trash" size="14" />
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Add Limit form -->
        <div class="col-4">
          <div class="glass-card">
            <h3 class="mb-6">Add Policy Limit Rule</h3>
            <form @submit.prevent="handleAddLimit">
              <div class="form-group">
                <label class="form-label">Expense Category</label>
                <select v-model="limitCategory" class="form-control">
                  <option value="Meals">Meals</option>
                  <option value="Travel">Travel</option>
                  <option value="Accommodation">Accommodation</option>
                  <option value="Other">Other</option>
                </select>
              </div>

              <div class="form-group">
                <label class="form-label">Max Limit Amount (INR)</label>
                <input type="number" v-model="limitAmount" class="form-control" required />
              </div>

              <div class="form-group">
                <label class="form-label">Accrual Frequency</label>
                <select v-model="limitFreq" class="form-control">
                  <option value="PER_DAY">Per Day Limit</option>
                  <option value="PER_TRIP">Per Trip Limit</option>
                  <option value="PER_MONTH">Per Month Limit</option>
                </select>
              </div>

              <div class="form-group">
                <label class="form-label">Employee Grade Eligibility</label>
                <select v-model="limitGrade" class="form-control">
                  <option value="All Grades">All Grades</option>
                  <option value="Grade L1-L4">Grade L1-L4</option>
                  <option value="Grade L5+">Grade L5+</option>
                </select>
              </div>

              <button type="submit" class="btn btn-primary btn-full">
                Add Limit Rule
              </button>
            </form>
          </div>
        </div>
      </div>
    </template>

    <!-- TAB 5: CURRENCY RATES -->
    <template v-if="activeTab === 'currencies' && ['HR_ADMIN', 'FINANCE_ADMIN'].includes(authStore.activeRole)">
      <div class="grid-12">
        <!-- Supported Currencies table -->
        <div class="col-8">
          <div class="glass-card">
            <h3 class="mb-6">Exchange Rate Matrix</h3>
            <div class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>Currency Code</th>
                    <th>Base Rate (vs INR)</th>
                    <th>Symbol</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="c in hrStore.currencyRates" :key="c.code">
                    <td class="font-bold">{{ c.code }}</td>
                    <td>
                      <input type="number" step="0.01" v-model="c.rate" class="form-control text-xs w-120 py-1" />
                    </td>
                    <td>{{ c.symbol }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="flex justify-end mt-4">
              <button class="btn btn-primary btn-sm" @click="window.showPortalToast('Currency exchange rates updated', 'success')">
                Save Exchange Matrix
              </button>
            </div>
          </div>
        </div>

        <!-- Add currency form -->
        <div class="col-4">
          <div class="glass-card">
            <h3 class="mb-6">Add Currency</h3>
            <form @submit.prevent="handleAddCurrency">
              <div class="form-group">
                <label class="form-label">ISO Currency Code</label>
                <input type="text" v-model="newCurrencyCode" class="form-control" placeholder="e.g. CAD" required />
              </div>

              <div class="form-group">
                <label class="form-label">Exchange Rate (vs INR)</label>
                <input type="number" step="0.01" v-model="newCurrencyRate" class="form-control" placeholder="1.0" required />
              </div>

              <button type="submit" class="btn btn-primary btn-full">
                Register Currency
              </button>
            </form>
          </div>
        </div>
      </div>
    </template>

    <!-- MODAL FOR AUDIT DETAILS / DECISION FORM -->
    <div class="modal-overlay" v-if="showReviewModal && activeClaim">
      <div class="modal-content">
        <div class="modal-header">
          <h3>
            <span v-if="reviewAction === 'VIEW'">Claim Review Timeline</span>
            <span v-else>Process Claim Decision</span>
          </h3>
          <button class="btn btn-ghost btn-sm" @click="showReviewModal = false">
            <IconHelper name="plus" size="14" style="transform: rotate(45deg);" />
          </button>
        </div>
        
        <div class="modal-body">
          <!-- VIEW HISTORY TIMELINE -->
          <div v-if="reviewAction === 'VIEW'">
            <div class="mb-4">
              <div class="text-xs text-muted">Claim ID</div>
              <div class="font-mono font-bold">{{ activeClaim.id }}</div>
            </div>
            <div class="timeline mt-4">
              <div
                v-for="(t, idx) in activeClaim.timeline"
                :key="idx"
                class="timeline-item"
                :class="{ success: t.status === 'PAID' || t.status === 'APPROVED_FINANCE', active: t.status === 'APPROVED_MANAGER' }"
              >
                <div class="timeline-marker"></div>
                <div class="timeline-content">
                  <div class="timeline-title">{{ t.title }}</div>
                  <div class="timeline-time">{{ new Date(t.timestamp).toLocaleString() }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- DECISION MAKING FORMS -->
          <div v-else>
            <p class="mb-4 text-sm">
              Reviewing claim of <b>{{ activeClaim.currency }} {{ activeClaim.amount }}</b> submitted by <b>{{ activeClaim.fullName }}</b>.
            </p>

            <div v-if="errorMsg" class="alert alert-error mb-4" style="min-width: 100%">
              <span>{{ errorMsg }}</span>
            </div>

            <!-- Transaction ref for finance approval -->
            <div class="form-group" v-if="reviewAction === 'APPROVE'">
              <label class="form-label">Payout Transaction Reference Number *</label>
              <input type="text" v-model="paymentRefInput" class="form-control" placeholder="TXN-xxxxxxx" required />
            </div>

            <!-- Reject comment -->
            <div class="form-group" v-if="reviewAction === 'REJECT'">
              <label class="form-label">Rejection Reason Description *</label>
              <textarea v-model="reviewReason" class="form-control" placeholder="Specify why the claim is rejected..." required></textarea>
            </div>
            
            <p class="text-xs text-muted" v-if="reviewAction === 'PAY'">
              Confirming this claim status as <b>PAID</b> completes the payout workflow. The employee will receive an in-app alert.
            </p>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showReviewModal = false">
            {{ reviewAction === 'VIEW' ? 'Close' : 'Cancel' }}
          </button>
          <button
            v-if="reviewAction !== 'VIEW'"
            class="btn"
            :class="reviewAction === 'APPROVE' || reviewAction === 'PAY' ? 'btn-success' : 'btn-danger'"
            @click="handleConfirmFinanceAction"
          >
            Confirm Action
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.expense-hub {
  animation: fadeIn 0.4s ease;
}

.max-width-200 { max-width: 200px; }
.text-truncate {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.m-0 { margin: 0; }
.mb-2 { margin-bottom: 8px; }
.mb-4 { margin-bottom: 16px; }
.mb-6 { margin-bottom: 24px; }
.mt-4 { margin-top: 16px; }
.py-6 { padding-top: 24px; padding-bottom: 24px; }
.py-1 { padding-top: 4px; padding-bottom: 4px; }
.py-10 { padding-top: 10px; padding-bottom: 10px; }
.py-6 { padding-top: 6px; padding-bottom: 6px; }

.font-normal { font-weight: 400; }
.font-semibold { font-weight: 600; }
.font-bold { font-weight: 700; }
.font-mono { font-family: monospace; }
.text-xs { font-size: 12px; }
.text-sm { font-size: 13px; }
.text-lg { font-size: 18px; }

.flex { display: flex; }
.flex-col { display: flex; flex-direction: column; }
.items-center { align-items: center; }
.items-end { align-items: flex-end; }
.justify-between { justify-content: space-between; }
.justify-end { justify-content: flex-end; }
.gap-1 { gap: 4px; }
.gap-2 { gap: 8px; }
.gap-3 { gap: 12px; }
.gap-4 { gap: 16px; }
.text-right { text-align: right; }
.btn-full { width: 100%; }
.w-80 { width: 80px; }
.w-120 { width: 120px; }
.w-160 { width: 160px; }
.w-200 { width: 200px; }
.p-8 { padding: 32px; }
</style>
