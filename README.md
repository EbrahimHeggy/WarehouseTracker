# 🏭 Warehouse Management App
**نظام متكامل لإدارة المستودعات وتتبع أوقات عمل الموظفين**

---

## 📱 Screenshots

> Coming soon...

---

## ✨ Features

### 👤 Authentication & Roles
- Firebase Authentication (Login / Password Reset)
- Two roles: **Admin** 🔴 and **Employee** 🟢
- Admin creates all user accounts (no self-registration)
- Secure account deactivation — deleted users can't log in

### 🏢 Branch Management
- Add / Delete multiple branches
- Switch between branches instantly
- Each branch has its own employee list and data

### 👷 Employee Management
- Add employees manually
- **Bulk import** via Excel (.xlsx) or CSV file
- Import supports multiple branches in one file
- Search employees by name or code

### ⏱️ Time Tracking
Each employee has **3 phases** tracked separately:

| Phase | Description |
|-------|-------------|
| 📋 Prep & Check | Preparation and checking stage |
| 🔄 Cycle Count | Inventory cycle count stage |
| 🚚 Loading | Loading and unloading stage |

- **IN / OUT buttons** per phase per employee
- Multiple sessions per phase per day
- Manual time adjustment with exact timestamps
- Reset individual phase data

### 📊 Reports & Export
- **Today** or **Custom Date Range** export
- CSV report with 3 sections:
  - Summary (total hours per employee)
  - Daily breakdown (day by day)
  - Detailed log (every IN/OUT session)
- Share via WhatsApp, Email, Google Drive, etc.

### 👁️ Employee View (Read Only)
- Employees see their branch activity
- No editing permissions
- Real-time status of colleagues

### 🔐 Profile Management
- View account info
- Change password
- Send password reset email

---

## 🏗️ Architecture

```
app/
├── data/
│   ├── model/          # Data classes (Branch, Employee, UserProfile...)
│   └── repository/     # Firebase calls (Auth, Branch, Employee, Tracking)
├── ui/
│   ├── screens/        # All Composable screens
│   └── viewmodel/      # AuthViewModel, DashboardViewModel
├── navigation/         # AppNavigation, Screen sealed class
└── MainActivity.kt
```

**Pattern:** MVVM (Model - View - ViewModel)

```
UI (Compose) ──► ViewModel ──► Repository ──► Firebase
     ▲               │
     └───────────────┘
        StateFlow (one-way data flow)
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material Design 3 |
| Architecture | MVVM |
| Database | Firebase Firestore |
| Authentication | Firebase Auth |
| File Import | Apache POI (Excel) |
| Export | CSV via FileProvider |
| Min SDK | API 26 (Android 8.0) |

---

## 🔥 Firebase Structure

```
Firestore/
├── branches/
│   └── {branchId}/
│       └── name: "Luxor"
│
├── employees/
│   └── {empId}/
│       ├── name: "Ibrahim Desouky"
│       ├── code: "1046"
│       └── branchId: "..."
│
├── users/
│   └── {uid}/
│       ├── email: "admin@company.com"
│       ├── name: "Admin"
│       ├── role: "admin" | "user"
│       └── branchId: "..."
│
└── tracking/
    └── {date}/          ← e.g. "2026-04-14"
        └── employees/
            └── {empId}/
                ├── preparation/
                │   ├── isActive: false
                │   ├── currentStartTime: ""
                │   └── history: [{startTime, endTime, durationSeconds}]
                ├── cycleCount/  ...same structure
                └── loading/     ...same structure
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Firebase project with Auth + Firestore enabled

### Setup

**1. Clone the repo**
```bash
git clone https://github.com/EbrahimHeggy/WarehouseTracker
cd WarehouseTracker
```

**2. Add Firebase**
- Go to [Firebase Console](https://console.firebase.google.com)
- Create a new project
- Add an Android app with package `com.example.warehousetracker`
- Download `google-services.json` and place it in `/app`

**3. Enable Firebase Services**
- Authentication → Email/Password
- Firestore Database → Start in test mode

**4. Run the app**
```bash
# Open in Android Studio and click Run ▶️
```

### First Login
The first admin account must be created manually in Firebase Console:
- Go to Authentication → Users → Add User
- Then add the user document in Firestore under `users/{uid}`

---

## 📂 Import File Format

To bulk import employees, use Excel (.xlsx) or CSV:

| Name | Code | Branch |
|------|------|--------|
| Ibrahim Desouky | 1046 | Luxor |
| Hassan Mohamed | 2011 | Cairo |
| Mona Samir | 4401 | Aswan |
---

## 📄 Export Report Structure

```
=== SUMMARY REPORT ===
Period: 2026-04-01 to 2026-04-14
Branch, Name, Code, Total Prep, Total Cycle, Total Loading, Total WH Time

=== DAILY BREAKDOWN ===
Date, Branch, Name, Code, Prep, Cycle Count, Loading, Total

=== DETAILED LOG ===
Date, Branch, Name, Code, Phase, Time In, Time Out, Duration
```

---

## 🗺️ Roadmap

- [ ] Google Play Store release
- [ ] Monthly analytics dashboard
- [ ] PDF report export
- [ ] Dark mode

---

## 📝 License

This project is proprietary software. All rights reserved.
© 2026 — Built with Ebrahim Heggy using Kotlin & Firebase
**⭐ If you find this useful, please give it a star!**
