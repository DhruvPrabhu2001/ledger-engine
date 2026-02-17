# Banking Demo Application

A modern, beautiful web application that demonstrates the capabilities of the Ledger-Based Banking Engine.

## Features

- **Account Management**: Create and view multiple bank accounts
- **Deposits**: Add funds to any account
- **Withdrawals**: Withdraw funds with balance validation
- **Transfers**: Move money between accounts
- **Transaction History**: View complete transaction ledger for each account
- **Real-time Updates**: Auto-refresh balances every 3 seconds
- **Modern UI**: Dark theme with glassmorphism effects and smooth animations

## Getting Started

### Prerequisites

1. **Banking Engine Running**: Ensure the Ledger Banking Engine is running at `http://localhost:8080`

```bash
cd d:\banking_engine_v2
docker-compose up --build
```

Wait for the message indicating the server has started successfully.

### Running the Demo App

You have three options to run the demo application:

#### Option 1: Direct File Open (Simplest)
```bash
cd d:\banking_engine_v2\demo-app
start index.html
```

#### Option 2: Python HTTP Server
```bash
cd d:\banking_engine_v2\demo-app
python -m http.server 3000
```
Then navigate to `http://localhost:3000`

#### Option 3: Node.js HTTP Server
```bash
cd d:\banking_engine_v2\demo-app
npx http-server -p 3000
```
Then navigate to `http://localhost:3000`

## Usage Guide

### Creating an Account

1. Click the **"Create Account"** button in the dashboard
2. A new account will be created with a unique UUID
3. The account appears immediately in the dashboard with a $0.00 balance

### Making a Deposit

1. Click **"Deposit"** on an account card, or use the Deposit tab
2. Select the account from the dropdown
3. Enter amount in dollars (e.g., `100.00` for $100)
4. Click **"Execute Deposit"**
5. Success notification appears and balance updates

### Withdrawing Funds

1. Click **"Withdraw"** on an account card, or use the Withdraw tab
2. Select the account from the dropdown
3. See the available balance displayed
4. Enter amount to withdraw (must be ≤ available balance)
5. Click **"Execute Withdrawal"**
6. Balance updates immediately

### Transferring Between Accounts

1. Go to the **Transfer** tab
2. Select source account (from)
3. Select destination account (to)
4. Enter transfer amount
5. Click **"Execute Transfer"**
6. Both account balances update

### Viewing Transaction History

1. Click **"History"** button on any account card
2. Modal opens showing all transactions for that account
3. See debits (withdrawals/outgoing transfers) and credits (deposits/incoming transfers)
4. Transactions show timestamp and running balance

## Technical Details

### API Endpoints Used

- `POST /api/accounts` - Create new account
- `GET /api/accounts` - List all accounts
- `GET /api/accounts/{id}/balance` - Get account balance
- `GET /api/accounts/{id}/transactions` - Get transaction history
- `POST /api/transactions/deposit` - Deposit funds
- `POST /api/transactions/withdraw` - Withdraw funds
- `POST /api/transactions/transfer` - Transfer between accounts

### Currency Format

All amounts are handled in **cents** internally:
- User enters: `$100.00`
- Sent to API: `10000` (cents)
- Display format: `$100.00`

### Idempotency

Each transaction generates a unique idempotency key (UUID v4) to prevent duplicate transactions if requests are retried.

### Auto-Refresh

- Dashboard balances refresh every 3 seconds when active
- Server connection status checked every 5 seconds
- Manual refresh available via refresh button in header

## Design

The application features:
- **Dark theme** with slate/purple/cyan color palette
- **Glassmorphism** cards with backdrop blur
- **Gradient buttons** with hover animations
- **Smooth transitions** on all interactive elements
- **Responsive layout** for mobile, tablet, and desktop
- **Loading skeletons** for async operations
- **Toast notifications** for user feedback

## Browser Compatibility

- Chrome/Edge (latest)
- Firefox (latest)
- Safari (latest)
- Requires modern JavaScript (ES6+)

## Troubleshooting

### "Disconnected" Status

If the app shows "Disconnected":
1. Ensure the banking engine is running: `docker-compose up`
2. Check it's accessible at `http://localhost:8080/api/accounts`
3. Check browser console for CORS errors

### CORS Issues

If you encounter CORS errors when opening the HTML directly:
- Use one of the HTTP server options instead (Python or Node.js)
- Make sure the banking engine allows CORS from the origin

### Transactions Not Appearing

- Check browser console for API errors
- Verify the banking engine logs for error messages
- Ensure amounts are positive numbers

## File Structure

```
demo-app/
├── index.html    # Main application structure
├── styles.css    # Complete design system
├── app.js        # Application logic and API client
└── README.md     # This file
```

## Next Steps

To enhance this demo, you could:
- Add account closing functionality
- Implement transaction search/filtering
- Add CSV export for transaction history
- Create charts/graphs for spending analysis
- Add multi-currency support
- Implement user authentication
- Add transaction descriptions/notes

## License

MIT License - matches the parent banking engine project.
