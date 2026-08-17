<p align="center">
  <img src="app/src/main/res/drawable/financebuddy.png" alt="FinanceBuddy Logo" width="120" />
</p>

<h1 align="center">FinanceBuddy</h1>

<p align="center">
  <strong>A premium, offline-first personal finance & investment management platform tailored for Bangladesh.</strong>
</p>

<p align="center">
  <a href="#key-features">Key Features</a> •
  <a href="#technical-architecture">Architecture</a> •
  <a href="#security--privacy">Security</a> •
  <a href="#tech-stack">Tech Stack</a> •
  <a href="#getting-started">Getting Started</a>
</p>

---

## Overview

**FinanceBuddy** is an enterprise-grade personal finance and investment tracking application engineered for privacy-conscious users in Bangladesh. Built with modern Android standards and Jetpack Compose, FinanceBuddy provides end-to-end management of multi-account balances, inter-bank transfers, category budgets, savings goals, credit loans, and a comprehensive investment portfolio (FDR, Sanchayapatra, Stock Market, Gold, Real Estate, Crypto) — all operating **100% offline** with hardware-backed AES-256 encryption.

---

## Key Features

### 🏦 Localized Financial Ecosystem
* **Pre-seeded Institutions**: Native support for major Bangladeshi commercial banks (BRAC, EBL, DBBL, City Bank, Prime Bank, IBBL, etc.) and Mobile Financial Services (bKash, Nagad, Rocket, Upay, CellFin).
* **Smart Default Routing**: Intelligent pre-selection of Hand Cash for daily expense tracking and context-aware action routing across dashboard views.

### 📈 Comprehensive Investment Portfolio
* **Multi-Asset Management**: Real-time tracking of Fixed Deposits (FDR/DPS), Sanchayapatra (National Savings Certificates), Stock Market (DSE), Gold, Real Estate, and Foreign Exchange.
* **Returns & Dividend Payouts**: Record investment returns, dividends, and interest payouts directly into linked Bank/MFS accounts.

### 📲 On-Device Automated SMS Parsing
* **Instant Detection**: Parses incoming bank SMS alerts locally to log transactions, category tags, and account balances without external servers or cloud dependencies.
* **Custom Shortcode Mapping**: Map custom bank or MFS shortcodes directly to specific local accounts.

### 📊 Advanced Analytics & Reporting
* **Interactive Canvas Charts**: Grouped income vs. expense bar comparisons, 30-day Bezier balance trend lines, and category distribution pie charts.
* **Client-Side PDF Export**: Generate formatted, professional financial statements directly to device storage.

### 🎯 Budgeting & Savings Targets
* **Spending Limits**: Set category-level budget thresholds with visual visual usage indicators.
* **Savings Ring Tracking**: Track progress toward financial goals with deposit logs.

### 🔒 Hardware-Backed Privacy & Security
* **SQLCipher AES-256 Encryption**: Complete encryption of local databases at rest.
* **TEE Keystore Isolation**: Encryption keys are generated cryptographically and isolated inside the device's hardware Trusted Execution Environment.
* **Biometric & PIN Lock**: Enforces biometric/PIN authentication on cold starts and background resumes.

---

## Technical Architecture

FinanceBuddy adheres to Clean Architecture principles with unidirectional data flow (UDF) powered by Jetpack Compose and Kotlin Coroutines.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Declarative Compose UI                          │
│        (HomeScreen, InvestmentsScreen, StatisticsScreen, Loans)       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Unidirectional Reactive Streams (Flow)
┌───────────────────────────────────▼────────────────────────────────────┐
│                        ViewModel & Repository Layer                    │
│            (Room Reactive Queries & DataStore Preferences)             │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Encrypted SQLCipher Bridge
┌───────────────────────────────────▼────────────────────────────────────┐
│                    SQLCipher AES-256 SQLite Database                   │
│         (Versioned Migrations, TEE Keystore Master Passphrase)         │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Domain | Technology |
| :--- | :--- |
| **Language** | Kotlin 2.0 |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Architecture** | Clean Architecture / MVI with ViewModel |
| **Local Database** | Room Database + SQLCipher (AES-256 Encryption) |
| **Key Storage** | Android Keystore System + EncryptedSharedPreferences |
| **Concurrency** | Kotlin Coroutines & Reactive `Flow` |
| **Graphics Engine** | Native Jetpack Compose Canvas APIs |
| **Build System** | Gradle Kotlin DSL + KSP |

---

## Project Structure

```
app/src/main/java/com/shejan/financebuddy/
├── data/
│   ├── db/                 # Room Database entities, DAOs, & SQLCipher migrations
│   └── PreferencesManager  # Encrypted DataStore preferences
├── sms/                    # On-device regex SMS parsing engine & receivers
├── ui/
│   ├── home/               # Dashboard & transaction management
│   ├── investments/        # Multi-asset portfolio manager
│   ├── statistics/         # Visual analytics & Compose Canvas charts
│   ├── accounts/           # Bank & MFS account management
│   ├── budget/             # Spending limits & budget meters
│   ├── goals/              # Savings target rings
│   ├── loans/              # Credit & lending manager
│   ├── history/            # Searchable transaction records
│   ├── reports/            # Client-side PDF statement builder
│   ├── settings/           # Security, biometric lock & preferences
│   └── theme/              # Dark fintech design system & typography
└── MainActivity.kt          # Single-activity navigation entry point
```

---

## Getting Started

### Prerequisites
* **JDK**: 17 or higher
* **Android SDK**: Target 35 (Min SDK: 24 / Android 7.0 Nougat)
* **IDE**: Android Studio Ladybug (2024.2.1+) or newer

### Building from Source

```bash
# 1. Clone the repository
git clone https://github.com/shejanahmmed/FinanceBuddy.git
cd FinanceBuddy

# 2. Build debug APK
./gradlew assembleDebug

# 3. Verify Kotlin compilation
./gradlew compileDebugKotlin
```

---

<p align="center">
  Crafted with ❤️ for Bangladesh 🇧🇩
</p>
