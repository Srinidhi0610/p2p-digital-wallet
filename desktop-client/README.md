# pocket // peer — JavaFX Desktop Client

A high-fidelity JavaFX 21 desktop client reproducing the **pocket // peer** ("Money in Motion") fintech web experience.

---

## 🎨 Visual Identity & Architecture

- **Color Palette & Theme**:
  - **Midnight Navy** (`#101A2B`) & **Deep Slate** (`#19273A` / `#21324B`) layered surfaces
  - **Electric Lime** (`#C8F169`) for primary calls to action, positive incoming credits, and active indicators
  - **Warm Ivory** (`#F5F1E8`) high-contrast text and headings
  - **Soft Coral** (`#F2A88A`) for outgoing debits and alert notices
  - **Muted Blue** (`#AAB5C6`) for metadata labels and secondary captions
  - Clean monospace typography for transaction IDs, currency codes, and cryptographic hashes
- **Thread Safety**: All HTTP networking runs asynchronously via Java 11+ `HttpClient` and `CompletableFuture`, delivering results to the JavaFX Application Thread via `Platform.runLater()`.
- **Session Management**: Thread-safe in-memory `SessionManager` persisting active JWT tokens and peer profile details, auto-clearing and redirecting on 401 unauthenticated responses.

---

## 🚀 Features & Screens

1. **Authentication**
   - **Login**: Split-screen brand layout with real-time field validation, password toggle, and error banners.
   - **Register**: Account provisioning with full name, email, and password.
2. **Overview (Dashboard)**
   - **Settlement Node Status**: Live health indicator checking Spring Boot backend availability.
   - **Hero Liquidity Balance**: Oversized typography with dynamic currency selector chips.
   - **Architectural Wallet Cards**: Visual partitions displaying currency, settled balance, and quick action shortcuts ("Send", "Add Demo Funds", "Copy ID").
   - **Recent Activity Ledger**: Quick glance at the latest peer transfers with directional indicators.
3. **Send Liquidity**
   - Source wallet partition dropdown selector.
   - Recipient Peer Wallet ID entry.
   - Authoritative balance validation and amount entry.
   - Unique UUID idempotency key generator to prevent double-spending.
   - Double-entry confirmation modal reviewing debited and credited partitions before submission.
   - Verified Settlement Receipt upon transaction confirmation.
4. **Ledger (Transactions)**
   - Full paginated table of immutable ledger entries.
   - Filter by direction: `ALL`, `INCOMING (+)` (electric lime), or `OUTGOING (-)` (soft coral).
   - Click any transaction to open the **Transfer Audit Modal**, displaying counterparties, amount, fee ($0.00), idempotency key, and formatted timestamps.
5. **Accounts & Settings**
   - **Peer Identity**: User node ID, avatar badge, email, and registration timestamp.
   - **Open Currency Account**: Allocate isolated ledger balance partitions using common ISO codes (`USD`, `EUR`, `GBP`, `CAD`, `AUD`, `INR`, `JPY`, `SGD`, `CHF`) or custom 3-character currency codes.
   - **Allocated Wallet Partitions**: Comprehensive table with quick "Add Demo Funds" modal and one-click "Copy ID" clipboard action.

---

## 📋 Prerequisites

- **Java Development Kit (JDK) 21** or higher.
- Internet connectivity for first-time Maven dependency downloads.

---

## ⚙️ Configuration

By default, the desktop client connects to the Spring Boot REST backend running on:
```text
http://localhost:8080
```

To configure a different backend address, use any of the following methods:

### Option 1: System Property (Recommended for CLI)
```powershell
.\mvnw javafx:run -Dwallet.api.url=http://localhost:8080
```

### Option 2: Environment Variable
```powershell
$env:WALLET_API_URL = "http://localhost:8080"
.\mvnw javafx:run
```

---

## 🛠️ Building and Running

From the `desktop-client/` directory:

### 1. Compile the Project
```powershell
.\mvnw clean compile
```

### 2. Launch the Application (JavaFX Plugin)
```powershell
.\mvnw javafx:run
```

### 3. Launch via Exec Plugin
```powershell
.\mvnw exec:java
```

### 4. Package JAR
```powershell
.\mvnw clean package
```
