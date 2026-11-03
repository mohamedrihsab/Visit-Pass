# SAIT Group — Touchless Mobile Visitor Registration Portal

This directory contains the GitHub Pages web portal for the SAIT Group Visitor Pass Kiosk application.

## How It Works
1. Visitors scan the dynamic QR code displayed on the Kiosk terminal using their smartphone.
2. The URL opens this portal (`https://<username>.github.io/<repo>/docs/` or `https://<username>.github.io/<repo>/`).
3. The visitor fills in their name, mandatory email, mobile number, company, department, host, and accepts QHSE safety rules.
4. When submitted, the page writes directly to Cloud Firestore (`visitors` collection).
5. The Kiosk terminal tablet listens in real-time, instantly detects the new visitor pass, displays the visitor pass badge on screen, and emails them their safety guidelines!

## 1-Minute GitHub Pages Setup
1. Push your repository to GitHub.
2. In your repository on GitHub, click **Settings** (tab at top).
3. In the left navigation menu, click **Pages**.
4. Under **Build and deployment**:
   - **Source**: `Deploy from a branch`
   - **Branch**: `main`
   - **Folder**: `/docs`
5. Click **Save**.
6. Within 1 minute, GitHub will show: *"Your site is live at https://<username>.github.io/<repo>/"*.
7. In the Android App **Admin Dashboard -> Touchless Kiosk (GitHub Pages)**, paste your live URL or leave default.
