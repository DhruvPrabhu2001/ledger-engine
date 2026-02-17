const API_BASE_URL = 'http://localhost:8080/api';

const state = {
    accounts: [],
    currentView: 'dashboard',
    selectedAccount: null,
    isOnline: false
};

function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

function formatCurrency(cents) {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD'
    }).format(cents / 100);
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    }).format(date);
}

function truncateId(id) {
    return `${id.substring(0, 8)}...`;
}

function showNotification(message, type = 'info') {
    const notificationArea = document.getElementById('notificationArea');
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.innerHTML = `
        <div style="flex: 1;">${message}</div>
    `;
    notificationArea.appendChild(notification);

    setTimeout(() => {
        notification.style.animation = 'slideIn 0.3s ease-out reverse';
        setTimeout(() => notification.remove(), 300);
    }, 4000);
}

async function apiRequest(endpoint, options = {}) {
    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            ...options,
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            }
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({ message: 'Request failed' }));
            throw new Error(error.message || `HTTP ${response.status}`);
        }

        return await response.json();
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
}

async function checkServerStatus() {
    try {
        await apiRequest('/accounts');
        updateStatus(true);
    } catch (error) {
        updateStatus(false);
    }
}

function updateStatus(isOnline) {
    state.isOnline = isOnline;
    const statusDot = document.getElementById('statusDot');
    const statusText = document.getElementById('statusText');

    if (isOnline) {
        statusDot.className = 'status-dot online';
        statusText.textContent = 'Connected';
    } else {
        statusDot.className = 'status-dot offline';
        statusText.textContent = 'Disconnected';
    }
}

async function createAccount() {
    try {
        const account = await apiRequest('/accounts', {
            method: 'POST'
        });

        showNotification(`Account created successfully: ${truncateId(account.accountId)}`, 'success');
        await loadAccounts();
    } catch (error) {
        showNotification(`Failed to create account: ${error.message}`, 'error');
    }
}

async function loadAccounts() {
    try {
        const accounts = await apiRequest('/accounts');
        state.accounts = accounts;
        renderAccounts();
        populateAccountSelectors();
    } catch (error) {
        console.error('Failed to load accounts:', error);
        showNotification('Failed to load accounts', 'error');
    }
}

async function getAccountBalance(accountId) {
    try {
        const response = await apiRequest(`/accounts/${accountId}/balance`);
        return response.balance;
    } catch (error) {
        console.error('Failed to get balance:', error);
        return 0;
    }
}

function renderAccounts() {
    const grid = document.getElementById('accountsGrid');

    if (state.accounts.length === 0) {
        grid.innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1;">
                <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
                    <rect x="1" y="4" width="22" height="16" rx="2" ry="2"/>
                    <line x1="1" y1="10" x2="23" y2="10"/>
                </svg>
                <p>No accounts yet. Create your first account to get started!</p>
            </div>
        `;
        return;
    }

    grid.innerHTML = state.accounts.map(account => `
        <div class="account-card">
            <div class="account-header">
                <div class="account-id" title="${account.accountId}">${truncateId(account.accountId)}</div>
                <div class="account-status ${account.status.toLowerCase()}">${account.status}</div>
            </div>
            <div class="account-balance">
                <label>Current Balance</label>
                <div class="account-balance-value" data-account-id="${account.accountId}">Loading...</div>
            </div>
            <div class="account-meta">
                Created ${formatDate(account.createdAt)}
            </div>
            <div class="account-actions">
                <button class="btn btn-sm btn-success" onclick="showDeposit('${account.accountId}')">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="12" y1="5" x2="12" y2="19"/>
                        <polyline points="19 12 12 19 5 12"/>
                    </svg>
                    Deposit
                </button>
                <button class="btn btn-sm btn-warning" onclick="showWithdraw('${account.accountId}')">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="12" y1="19" x2="12" y2="5"/>
                        <polyline points="5 12 12 5 19 12"/>
                    </svg>
                    Withdraw
                </button>
                <button class="btn btn-sm btn-secondary" onclick="showTransactions('${account.accountId}')">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="12" y1="1" x2="12" y2="23"/>
                        <polyline points="17 8 12 3 7 8"/>
                        <polyline points="7 16 12 21 17 16"/>
                    </svg>
                    History
                </button>
            </div>
        </div>
    `).join('');

    state.accounts.forEach(async account => {
        const balance = await getAccountBalance(account.accountId);
        const balanceElement = document.querySelector(`[data-account-id="${account.accountId}"]`);
        if (balanceElement) {
            balanceElement.textContent = formatCurrency(balance);
        }
    });
}

function populateAccountSelectors() {
    const selectors = [
        document.getElementById('depositAccount'),
        document.getElementById('withdrawAccount'),
        document.getElementById('transferFrom'),
        document.getElementById('transferTo')
    ];

    selectors.forEach(select => {
        if (!select) return;
        const currentValue = select.value;
        select.innerHTML = '<option value="">Choose an account...</option>' +
            state.accounts.map(account =>
                `<option value="${account.accountId}">${truncateId(account.accountId)}</option>`
            ).join('');
        if (currentValue) select.value = currentValue;
    });
}

async function handleDeposit(event) {
    event.preventDefault();

    const accountId = document.getElementById('depositAccount').value;
    const amountUSD = parseFloat(document.getElementById('depositAmount').value);
    const amount = Math.round(amountUSD * 100);

    if (!accountId) {
        showNotification('Please select an account', 'error');
        return;
    }

    if (amount <= 0) {
        showNotification('Amount must be greater than zero', 'error');
        return;
    }

    try {
        await apiRequest('/transactions/deposit', {
            method: 'POST',
            body: JSON.stringify({
                accountId,
                amount,
                idempotencyKey: generateUUID()
            })
        });

        showNotification(`Successfully deposited ${formatCurrency(amount)}`, 'success');
        document.getElementById('depositForm').reset();
        await loadAccounts();
    } catch (error) {
        showNotification(`Deposit failed: ${error.message}`, 'error');
    }
}

async function handleWithdraw(event) {
    event.preventDefault();

    const accountId = document.getElementById('withdrawAccount').value;
    const amountUSD = parseFloat(document.getElementById('withdrawAmount').value);
    const amount = Math.round(amountUSD * 100);

    if (!accountId) {
        showNotification('Please select an account', 'error');
        return;
    }

    if (amount <= 0) {
        showNotification('Amount must be greater than zero', 'error');
        return;
    }

    try {
        await apiRequest('/transactions/withdraw', {
            method: 'POST',
            body: JSON.stringify({
                accountId,
                amount,
                idempotencyKey: generateUUID()
            })
        });

        showNotification(`Successfully withdrew ${formatCurrency(amount)}`, 'success');
        document.getElementById('withdrawForm').reset();
        await loadAccounts();
    } catch (error) {
        showNotification(`Withdrawal failed: ${error.message}`, 'error');
    }
}

async function handleTransfer(event) {
    event.preventDefault();

    const fromAccountId = document.getElementById('transferFrom').value;
    const toAccountId = document.getElementById('transferTo').value;
    const amountUSD = parseFloat(document.getElementById('transferAmount').value);
    const amount = Math.round(amountUSD * 100);

    if (!fromAccountId || !toAccountId) {
        showNotification('Please select both accounts', 'error');
        return;
    }

    if (fromAccountId === toAccountId) {
        showNotification('Cannot transfer to the same account', 'error');
        return;
    }

    if (amount <= 0) {
        showNotification('Amount must be greater than zero', 'error');
        return;
    }

    try {
        await apiRequest('/transactions/transfer', {
            method: 'POST',
            body: JSON.stringify({
                fromAccountId,
                toAccountId,
                amount,
                idempotencyKey: generateUUID()
            })
        });

        showNotification(`Successfully transferred ${formatCurrency(amount)}`, 'success');
        document.getElementById('transferForm').reset();
        await loadAccounts();
    } catch (error) {
        showNotification(`Transfer failed: ${error.message}`, 'error');
    }
}

async function showTransactions(accountId) {
    const modal = document.getElementById('transactionModal');
    const modalAccountInfo = document.getElementById('modalAccountInfo');
    const transactionList = document.getElementById('transactionList');

    modal.classList.add('active');
    transactionList.innerHTML = '<div class="loading-skeleton"></div><div class="loading-skeleton"></div>';

    try {
        const account = state.accounts.find(a => a.accountId === accountId);
        const balance = await getAccountBalance(accountId);

        modalAccountInfo.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center;">
                <div>
                    <div style="font-size: 0.75rem; color: var(--slate-400); margin-bottom: 0.25rem;">Account</div>
                    <div style="font-family: monospace;">${truncateId(accountId)}</div>
                </div>
                <div>
                    <div style="font-size: 0.75rem; color: var(--slate-400); margin-bottom: 0.25rem;">Balance</div>
                    <div style="font-size: 1.25rem; font-weight: 700; color: var(--emerald-400);">${formatCurrency(balance)}</div>
                </div>
            </div>
        `;

        const transactions = await apiRequest(`/accounts/${accountId}/transactions`);

        if (transactions.length === 0) {
            transactionList.innerHTML = `
                <div class="empty-state">
                    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
                        <line x1="12" y1="1" x2="12" y2="23"/>
                        <polyline points="17 8 12 3 7 8"/>
                        <polyline points="7 16 12 21 17 16"/>
                    </svg>
                    <p>No transactions yet</p>
                </div>
            `;
            return;
        }

        transactionList.innerHTML = transactions.map(tx => {
            const isDebit = tx.amount < 0;
            const typeLabel = isDebit ? 'DEBIT' : 'CREDIT';

            return `
                <div class="transaction-item">
                    <div class="transaction-info">
                        <div class="transaction-type">${typeLabel}</div>
                        <div class="transaction-date">${formatDate(tx.createdAt)}</div>
                    </div>
                    <div class="transaction-amount ${isDebit ? 'debit' : 'credit'}">
                        ${isDebit ? '-' : '+'}${formatCurrency(Math.abs(tx.amount))}
                    </div>
                </div>
            `;
        }).join('');
    } catch (error) {
        showNotification(`Failed to load transactions: ${error.message}`, 'error');
        modal.classList.remove('active');
    }
}

function closeModal() {
    document.getElementById('transactionModal').classList.remove('active');
}

function switchView(viewName) {
    document.querySelectorAll('.view').forEach(view => {
        view.classList.remove('active');
    });

    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });

    document.getElementById(`${viewName}View`).classList.add('active');
    document.querySelector(`[data-view="${viewName}"]`).classList.add('active');

    state.currentView = viewName;
}

function showDeposit(accountId) {
    switchView('deposit');
    document.getElementById('depositAccount').value = accountId;
}

function showWithdraw(accountId) {
    switchView('withdraw');
    document.getElementById('withdrawAccount').value = accountId;
    updateWithdrawBalance();
}

async function updateWithdrawBalance() {
    const accountId = document.getElementById('withdrawAccount').value;
    const balanceDisplay = document.getElementById('withdrawBalanceDisplay');

    if (!accountId) {
        balanceDisplay.innerHTML = '';
        return;
    }

    const balance = await getAccountBalance(accountId);
    balanceDisplay.innerHTML = `
        <label>Available Balance</label>
        <div class="balance-display-value">${formatCurrency(balance)}</div>
    `;
}

async function updateTransferBalance() {
    const accountId = document.getElementById('transferFrom').value;
    const balanceDisplay = document.getElementById('transferBalanceDisplay');

    if (!accountId) {
        balanceDisplay.innerHTML = '';
        return;
    }

    const balance = await getAccountBalance(accountId);
    balanceDisplay.innerHTML = `
        <label>Available Balance</label>
        <div class="balance-display-value">${formatCurrency(balance)}</div>
    `;
}

async function init() {
    await checkServerStatus();

    if (state.isOnline) {
        await loadAccounts();
    }

    document.getElementById('createAccountBtn').addEventListener('click', createAccount);
    document.getElementById('refreshBtn').addEventListener('click', loadAccounts);
    document.getElementById('depositForm').addEventListener('submit', handleDeposit);
    document.getElementById('withdrawForm').addEventListener('submit', handleWithdraw);
    document.getElementById('transferForm').addEventListener('submit', handleTransfer);
    document.getElementById('closeModalBtn').addEventListener('click', closeModal);

    document.getElementById('withdrawAccount').addEventListener('change', updateWithdrawBalance);
    document.getElementById('transferFrom').addEventListener('change', updateTransferBalance);

    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const viewName = e.currentTarget.getAttribute('data-view');
            switchView(viewName);
        });
    });

    document.getElementById('transactionModal').addEventListener('click', (e) => {
        if (e.target.id === 'transactionModal') {
            closeModal();
        }
    });

    setInterval(async () => {
        if (state.currentView === 'dashboard' && state.isOnline) {
            await loadAccounts();
        }
    }, 3000);

    setInterval(checkServerStatus, 5000);
}

document.addEventListener('DOMContentLoaded', init);
