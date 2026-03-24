# BillingApp

BillingApp is an Android billing and customer management app for small shops and daily sales workflows.
It helps you create bills, track payments, manage customers, and share invoices quickly.

## 📥 Download APK
Get the latest APK from GitHub Releases:

- **Latest Release:** https://github.com/KSD-333/Billing-App/releases/latest
- **All Releases:** https://github.com/KSD-333/Billing-App/releases

> Upload your APK file in each GitHub release, and users can download directly from the links above.

## 📱 App Overview
BillingApp includes dedicated modules for:

- Store and product management
- Billing and checkout
- Customer management with bill history
- Analytics and reports
- Settings and store profile

## ✨ Features
- **Product Catalog:** Add and manage products for billing
- **Smart Billing Flow:** Add items to cart, apply discount, set payment mode, and generate bill
- **Customer Support:** Save customer details and view customer-wise bills
- **Pending/Paid Tracking:** Mark bills as paid, track pending amounts, and update status
- **Invoice PDF Export:** Generate professional PDF bills
- **WhatsApp Sharing:** Share invoice PDF or payment reminder directly
- **History Management:** View, edit, and maintain past billing records
- **Analytics Screen:** Monitor sales and billing trends
- **Theme Toggle:** Switch between light and dark mode
- **Contact Picker:** Quickly fill customer number from phone contacts

## 🧰 Tech Stack
- **Language:** Java
- **Platform:** Android (minSdk 24)
- **Database/Backend:** Firebase Firestore
- **Build System:** Gradle
- **UI Components:** Material Design Components

## 🚀 Run Locally
### Prerequisites
- Android Studio (latest stable)
- JDK 11+
- Android SDK configured

### Build debug APK
Windows PowerShell:
```powershell
.\gradlew.bat assembleDebug
```

macOS/Linux:
```bash
./gradlew assembleDebug
```

### Install debug build
```powershell
.\gradlew.bat installDebug
```

## 📦 Publish New Release
1. Build your release APK/AAB.
2. Open GitHub repository **Releases**.
3. Click **Draft a new release**.
4. Upload APK/AAB under release assets.
5. Publish release.

## 🗂 Project Structure
- `app/` - Android app module
- `gradle/` - Gradle wrapper and version catalog
- `build.gradle`, `settings.gradle` - Root project configuration

## 🤝 Contributing
Contributions, issues, and suggestions are welcome.
