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

const activeTab = ref(route.meta.tab || 'my-payslips');

// Upload File State
const selectedMonth = ref('2026-06');
const csvFile = ref(null);
const isDragging = ref(false);
const validationResults = ref(null);
const rawCsvContent = ref('');

// Payslip Modal State
const showPayslipModal = ref(false);
const activePayslip = ref(null);

onMounted(() => {
  activeTab.value = route.meta.tab || 'my-payslips';
});

function changeTab(tab) {
  activeTab.value = tab;
  router.push(tab === 'my-payslips' ? '/payslip' : tab === 'upload' ? '/hr/payroll/upload' : tab === 'publish' ? '/hr/payroll/publish' : '/hr/payroll/history');
}

// User payslips
const myPayslips = computed(() => {
  return hrStore.payslips.filter(ps => ps.employeeCode === authStore.user.employeeCode && ps.status === 'PUBLISHED');
});

// Download sample CSV
function downloadSampleCSV() {
  const headers = 'employee_id,basic,allowances,deductions\r\n';
  const row1 = 'EMP2026101,70000,16666,12500\r\n';
  const row2 = 'EMP2023102,140000,33333,25000\r\n';
  const row3 = 'EMP2025104,80000,15000,10000\r\n';
  const csvContent = 'data:text/csv;charset=utf-8,' + headers + row1 + row2 + row3;
  
  const encoded = encodeURI(csvContent);
  const link = document.createElement('a');
  link.setAttribute('href', encoded);
  link.setAttribute('download', 'payroll_sample_template.csv');
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

// Drag & drop file
function handleCsvDrop(e) {
  isDragging.value = false;
  const files = e.dataTransfer.files;
  if (files.length > 0) {
    loadCSV(files[0]);
  }
}

function handleCsvSelect(e) {
  const files = e.target.files;
  if (files.length > 0) {
    loadCSV(files[0]);
  }
}

function loadCSV(file) {
  csvFile.value = file;
  const reader = new FileReader();
  reader.onload = (evt) => {
    rawCsvContent.value = evt.target.result;
    validateCsvData();
  };
  reader.readAsText(file);
}

function validateCsvData() {
  if (!rawCsvContent.value) return;
  const res = hrStore.processPayrollUpload(rawCsvContent.value, selectedMonth.value, authStore.user.email);
  validationResults.value = res;
  
  if (res.success) {
    window.showPortalToast('CSV payload structure parsed. No integrity errors found.', 'success');
  } else {
    window.showPortalToast('Integrity validation failed. Check row-level errors.', 'error');
  }
}

// Confirm batch process
function handleConfirmUpload() {
  if (!validationResults.value || !validationResults.value.success) return;
  
  // Batch is created in draft state in hrStore during processPayrollUpload
  window.showPortalToast(`Payroll batch drafts generated for ${selectedMonth.value}. Forwarded to Publish queue.`, 'success');
  
  // Clear
  csvFile.value = null;
  validationResults.value = null;
  rawCsvContent.value = '';
  
  changeTab('publish');
}

// Draft batches listing
const draftBatches = computed(() => {
  return hrStore.payrollBatches.filter(b => b.status === 'DRAFT');
});

function handlePublishBatch(batchId, month) {
  const success = hrStore.publishPayrollBatch(batchId, month, authStore.user.email);
  if (success) {
    window.showPortalToast(`Payroll batch for ${month} published. Employee alerts triggered.`, 'success');
  }
}

// Batches history
const allBatches = computed(() => {
  return hrStore.payrollBatches;
});

// Inline Payslip Details
function openPayslip(ps) {
  activePayslip.value = ps;
  showPayslipModal.value = true;
}

function handleDownloadPayslip(ps) {
  alert(`🔐 Security Notice: This PDF file is password protected. Enter your Employee ID (${ps.employeeCode}) to open this payslip.`);
  window.showPortalToast(`Downloading payslip_${ps.month}.pdf securely from AWS S3...`, 'success');
}

// Admin / HR download on behalf of staff
const allPayslips = computed(() => {
  return hrStore.payslips;
});

function getEmployeeName(code) {
  const emp = hrStore.employees.find(e => e.employeeCode === code);
  return emp ? emp.fullName : code;
}
</script>

<template>
  <div class="payroll-hub">
    <!-- SUB-TABS -->
    <div class="tabs-container">
      <div class="tabs">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'my-payslips' }"
          @click="changeTab('my-payslips')"
        >
          My Payslips
        </button>
        <button
          v-if="authStore.activeRole === 'HR_ADMIN'"
          class="tab-btn"
          :class="{ active: activeTab === 'upload' }"
          @click="changeTab('upload')"
        >
          Upload Payroll CSV
        </button>
        <button
          v-if="authStore.activeRole === 'HR_ADMIN'"
          class="tab-btn"
          :class="{ active: activeTab === 'publish' }"
          @click="changeTab('publish')"
        >
          Draft Batches & Publish
        </button>
        <button
          v-if="['HR_ADMIN', 'FINANCE_ADMIN'].includes(authStore.activeRole)"
          class="tab-btn"
          :class="{ active: activeTab === 'history' }"
          @click="changeTab('history')"
        >
          Payroll Run History
        </button>
      </div>
    </div>

    <!-- TAB 1: MY PAYSLIPS -->
    <template v-if="activeTab === 'my-payslips'">
      <div class="glass-card">
        <h3 class="mb-6">Published Monthly Payslips</h3>
        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>Salary Month</th>
                <th>Gross Earnings</th>
                <th>Total Deductions</th>
                <th>Net Payout Salary</th>
                <th class="text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="ps in myPayslips" :key="ps.id">
                <td class="font-bold">{{ ps.month }}</td>
                <td class="font-mono">INR {{ ps.grossPay.toLocaleString() }}</td>
                <td class="font-mono text-danger">INR {{ ps.deductions.toLocaleString() }}</td>
                <td class="font-mono text-success font-semibold">INR {{ ps.netSalary.toLocaleString() }}</td>
                <td class="text-right">
                  <div class="flex gap-2 justify-end">
                    <button class="btn btn-ghost btn-sm px-2" @click="openPayslip(ps)" title="View breakdown inline">
                      <IconHelper name="eye" size="14" />
                      Breakdown
                    </button>
                    <button class="btn btn-secondary btn-sm px-2" @click="handleDownloadPayslip(ps)" title="Download password-protected PDF">
                      <IconHelper name="download" size="14" />
                      PDF
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="myPayslips.length === 0">
                <td colspan="5" class="text-center text-secondary">No published payslips found on record.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- TAB 2: UPLOAD PAYROLL CSV -->
    <template v-if="activeTab === 'upload' && authStore.activeRole === 'HR_ADMIN'">
      <div class="grid-12">
        <!-- left config/form -->
        <div class="col-4">
          <div class="glass-card">
            <h3 class="mb-4">Process New Payroll</h3>
            <div class="form-group">
              <label class="form-label">Salary Month</label>
              <input type="month" v-model="selectedMonth" class="form-control" />
            </div>

            <!-- Upload drag/drop -->
            <div class="form-group">
              <label class="form-label">Payroll Data File (CSV)</label>
              <div
                class="upload-zone py-8"
                :class="{ active: isDragging }"
                @dragover.prevent="isDragging = true"
                @dragleave="isDragging = false"
                @drop.prevent="handleCsvDrop"
                @click="$refs.csvInput.click()"
              >
                <input type="file" ref="csvInput" style="display:none" @change="handleCsvSelect" />
                <IconHelper name="upload" size="32" color="var(--primary-color)" class="mb-2" />
                <p class="text-xs font-semibold" v-if="!csvFile">Drag payroll CSV here or click to browse</p>
                <p class="text-xs text-success font-semibold" v-else>📎 Loaded: {{ csvFile.name }}</p>
              </div>
            </div>

            <div class="flex justify-between items-center mt-6">
              <button type="button" class="btn btn-ghost btn-xs" @click="downloadSampleCSV">
                <IconHelper name="download" size="12" />
                Sample CSV Template
              </button>
              
              <button
                type="button"
                class="btn btn-primary"
                :disabled="!validationResults || !validationResults.success"
                @click="handleConfirmUpload"
              >
                Generate Drafts
              </button>
            </div>
          </div>
        </div>

        <!-- right validation results -->
        <div class="col-8">
          <div class="glass-card">
            <h3 class="mb-6">CSV Integrity Validation Report</h3>
            
            <div v-if="!validationResults" class="text-center p-8 text-secondary">
              Upload a CSV file to check validation checks.
            </div>

            <div v-else>
              <!-- Success Banner -->
              <div v-if="validationResults.success" class="alert alert-success mb-6" style="min-width:100%">
                <div class="flex items-center gap-2">
                  <IconHelper name="check-circle" size="18" />
                  <span>File validated. Ready to process drafts for {{ selectedMonth }}.</span>
                </div>
              </div>

              <!-- Error list -->
              <div v-else class="alert alert-error mb-6" style="min-width:100%; flex-direction:column; align-items:flex-start;">
                <div class="flex items-center gap-2 font-bold mb-2">
                  <IconHelper name="info" size="18" />
                  <span>Validation Errors Detected:</span>
                </div>
                <ul class="text-xs list-disc pl-4" style="text-align: left;">
                  <li v-for="(err, idx) in validationResults.errors" :key="idx" class="mt-1">{{ err }}</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- TAB 3: DRAFT BATCHES -->
    <template v-if="activeTab === 'publish' && authStore.activeRole === 'HR_ADMIN'">
      <div class="glass-card">
        <h3 class="mb-6">Draft Batches Awaiting Publication</h3>
        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>Salary Month</th>
                <th>File Source</th>
                <th>Accrued Records</th>
                <th>Status</th>
                <th class="text-right">Publish Action</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="b in draftBatches" :key="b.id">
                <td class="font-bold">{{ b.month }}</td>
                <td class="font-mono text-xs">{{ b.csvName }}</td>
                <td>{{ b.totalEmployees }} Staff Payslips</td>
                <td><span class="badge badge-warning">Draft</span></td>
                <td class="text-right">
                  <button class="btn btn-success btn-sm px-2 py-6" @click="handlePublishBatch(b.id, b.month)">
                    <IconHelper name="check-circle" size="14" />
                    Confirm & Publish
                  </button>
                </td>
              </tr>
              <tr v-if="draftBatches.length === 0">
                <td colspan="5" class="text-center text-secondary p-8">No draft payroll runs. All batches published.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- TAB 4: HISTORICAL BATCH RUNS -->
    <template v-if="activeTab === 'history' && ['HR_ADMIN', 'FINANCE_ADMIN'].includes(authStore.activeRole)">
      <div class="grid-12">
        <!-- Batches -->
        <div class="col-6">
          <div class="glass-card">
            <h3 class="mb-6">Upload Runs Archive</h3>
            <div class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>Month</th>
                    <th>Date Uploaded</th>
                    <th>Records</th>
                    <th>CSV Source</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="b in allBatches" :key="b.id">
                    <td class="font-bold">{{ b.month }}</td>
                    <td>{{ b.uploadDate }}</td>
                    <td>{{ b.totalEmployees }} profiles</td>
                    <td>
                      <a href="#" class="text-xs font-mono flex items-center gap-1" @click.prevent="window.showPortalToast('Downloading source CSV database...', 'success')">
                        <IconHelper name="download" size="14" />
                        {{ b.csvName }}
                      </a>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Individual payslip database -->
        <div class="col-6">
          <div class="glass-card">
            <h3 class="mb-6">All Staff Payslips Archive</h3>
            <div class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>Employee</th>
                    <th>Month</th>
                    <th>Net Pay</th>
                    <th class="text-right">Action</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="ps in allPayslips" :key="ps.id">
                    <td>
                      <div class="flex flex-col">
                        <span class="font-bold text-xs">{{ getEmployeeName(ps.employeeCode) }}</span>
                        <span class="text-xs text-secondary font-mono">{{ ps.employeeCode }}</span>
                      </div>
                    </td>
                    <td>{{ ps.month }}</td>
                    <td class="font-mono text-xs">INR {{ ps.netSalary.toLocaleString() }}</td>
                    <td class="text-right">
                      <button class="btn btn-ghost btn-sm px-2" @click="handleDownloadPayslip(ps)" title="Download securely on behalf of employee">
                        <IconHelper name="download" size="14" />
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- INLINE PAYSLIP MODAL -->
    <div class="modal-overlay" v-if="showPayslipModal && activePayslip">
      <div class="modal-content" style="max-width: 500px;">
        <div class="modal-header">
          <h3>Payslip Breakdown - {{ activePayslip.month }}</h3>
          <button class="btn btn-ghost btn-sm" @click="showPayslipModal = false">
            <IconHelper name="plus" size="14" style="transform: rotate(45deg);" />
          </button>
        </div>
        
        <div class="modal-body font-mono">
          <div class="text-center mb-6">
            <h2 class="mb-1 text-lg">EXCELLATHON PRIVATE LTD</h2>
            <p class="text-xs text-secondary">Paycheck Advice Statement</p>
          </div>
          
          <div class="flex justify-between mb-4 border-b pb-2 text-xs">
            <div>
              <div>Employee: <b>{{ authStore.user.fullName }}</b></div>
              <div>ID Code: <b>{{ activePayslip.employeeCode }}</b></div>
            </div>
            <div>
              <div>Month: <b>{{ activePayslip.month }}</b></div>
              <div>Status: <span class="badge badge-success py-0 px-2">{{ activePayslip.status }}</span></div>
            </div>
          </div>

          <div class="mb-4">
            <div class="font-bold border-b pb-1 text-sm text-primary mb-2">EARNINGS BREAKDOWN</div>
            <div class="flex justify-between text-xs mb-1">
              <span>Basic Salary:</span>
              <span>INR {{ activePayslip.basic.toLocaleString() }}</span>
            </div>
            <div class="flex justify-between text-xs mb-1">
              <span>Allowances (HRA / Special):</span>
              <span>INR {{ activePayslip.allowances.toLocaleString() }}</span>
            </div>
            <div class="flex justify-between font-bold text-xs mt-2 pt-1 border-t">
              <span>Gross Earnings:</span>
              <span>INR {{ activePayslip.grossPay.toLocaleString() }}</span>
            </div>
          </div>

          <div class="mb-6">
            <div class="font-bold border-b pb-1 text-sm text-danger mb-2">DEDUCTIONS BREAKDOWN</div>
            <div class="flex justify-between text-xs mb-1">
              <span>Provident Fund (PF):</span>
              <span>INR {{ activePayslip.pf.toLocaleString() }}</span>
            </div>
            <div class="flex justify-between text-xs mb-1">
              <span>Professional Tax / TDS:</span>
              <span>INR {{ activePayslip.tax.toLocaleString() }}</span>
            </div>
            <div class="flex justify-between font-bold text-xs mt-2 pt-1 border-t">
              <span>Total Deductions:</span>
              <span>INR {{ activePayslip.deductions.toLocaleString() }}</span>
            </div>
          </div>

          <div class="flex justify-between font-bold text-sm bg-primary-light p-3 rounded">
            <span>NET SALARY DISBURSED:</span>
            <span class="text-success">INR {{ activePayslip.netSalary.toLocaleString() }}</span>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showPayslipModal = false">Close</button>
          <button class="btn btn-primary" @click="handleDownloadPayslip(activePayslip)">
            <IconHelper name="download" size="14" />
            Download PDF File
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.payroll-hub {
  animation: fadeIn 0.4s ease;
}

.text-danger { color: var(--error); }
.text-success { color: var(--success); }

.list-disc { list-style-type: disc; }
.pl-4 { padding-left: 16px; }

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
.justify-between { justify-content: space-between; }
.justify-end { justify-content: flex-end; }
.gap-2 { gap: 8px; }
.text-right { text-align: right; }
.w-80 { width: 80px; }
.w-120 { width: 120px; }
.p-3 { padding: 12px; }
.p-8 { padding: 32px; }

.mb-1 { margin-bottom: 4px; }
.mb-2 { margin-bottom: 8px; }
.mb-4 { margin-bottom: 16px; }
.mb-6 { margin-bottom: 24px; }
.mt-1 { margin-top: 4px; }
.mt-2 { margin-top: 8px; }
.mt-6 { margin-top: 24px; }
.pb-1 { padding-bottom: 4px; }
.pb-2 { padding-bottom: 8px; }
.pt-1 { padding-top: 4px; }
.py-0 { padding-top: 0; padding-bottom: 0; }
.py-8 { padding-top: 32px; padding-bottom: 32px; }
.py-6 { padding-top: 6px; padding-bottom: 6px; }
.border-b { border-bottom: 1px solid var(--border-color); }
.border-t { border-top: 1px solid var(--border-color); }
</style>
