# SAIT Group Visitor Pass — Master UI/UX & System Architecture Specification
> **The Complete, Exhaustive Specification of All Visible Views, Layouts, Screens, Subviews, Cards, Dialogs, Inputs, States, Badges, and Interactions for Google Stitch**  
> *Target System:* Android Enterprise Kiosk & Management Console (Jetpack Compose / Material 3)  
> *Version:* 4.0 (Enterprise Comprehensive Edition)  
> *Brand Identity:* SAIT Group International (Offshore, Marine, Projects & Engineering Facilities)

---

## Table of Contents
1. [Product Overview & Operational Role Hierarchy](#1-product-overview--operational-role-hierarchy)
2. [Global Application Shell & Top App Bar Architecture](#2-global-application-shell--top-app-bar-architecture)
3. [Bottom Navigation Bar Configurations](#3-bottom-navigation-bar-configurations)
4. [Screen 1: Visitor Registration Screen (Kiosk & Staff Form)](#4-screen-1-visitor-registration-screen)
5. [Screen 2: Official Visitor Pass Modal Dialog](#5-screen-2-official-visitor-pass-modal-dialog)
6. [Screen 3: Visitor Logbook & Executive Analytics Screen](#6-screen-3-visitor-logbook--executive-analytics-screen)
7. [Screen 4: Enterprise Admin & Cloud Dashboard](#7-screen-4-enterprise-admin--cloud-dashboard)
8. [Comprehensive Admin & Configuration Dialogs Catalog](#8-comprehensive-admin--configuration-dialogs-catalog)
9. [Screen 5: Safety Information & Emergency Evacuation Screen / Dialog](#9-screen-5-safety-information--emergency-evacuation-screen--dialog)
10. [Global Utility Dialogs & System Overlays](#10-global-utility-dialogs--system-overlays)
11. [Design Tokens, Typography & Brand Guidelines](#11-design-tokens-typography--brand-guidelines)
12. [Serial Number Generation Formula](#12-serial-number-generation-formula)
13. [Complete Data Models & Schemas](#13-complete-data-models--schemas)
14. [Google Stitch UI Screen-by-Screen Prompt Blueprints](#14-google-stitch-ui-screen-by-screen-prompt-blueprints)

---

## 1. Product Overview & Operational Role Hierarchy

**SAIT Group Visitor Pass** is an enterprise-grade digital visitor management, security badging, and Health, Safety & Environment (QHSE) compliance platform designed for industrial yards, offshore bases, corporate headquarters, and engineering centres worldwide.

The application operates seamlessly in three role-based operational modes:

| Feature / Attribute | Visitor Self-Service Kiosk (`VISITOR_KIOSK`) | Receptionist Console (`RECEPTIONIST`) | Enterprise Admin Console (`ADMIN`) |
| :--- | :--- | :--- | :--- |
| **Primary Device** | Floor or desk-mounted touch terminal | Front desk PC / Android Tablet | Management Workstation / Tablet |
| **Top App Bar Title** | Full Branch Name (e.g. `Dubai Headquarters (UAE)`) — *No "KIOSK" text* | `[Branch Name]` on Tab 0, `[CODE] RECEPTION` on sub-tabs | `[Branch Name]` on Tab 0, `[CODE] ADMIN` on sub-tabs |
| **Company Logo** | Solid White Rounded Container (`#FFFFFF`) | Solid White Rounded Container (`#FFFFFF`) | Solid White Rounded Container (`#FFFFFF`) |
| **Access Switcher** | Lock icon — **Strict 3-second touch & hold gesture only** (No single-tap dialog) | `AdminPanelSettings` button — **Direct 1-tap** opens Admin PIN prompt | `Store` icon to launch Kiosk; Mode switch button |
| **Notification Bell** | **Hidden** | **Visible** with unread count badge | **Visible** with unread count badge |
| **Theme Toggle** | **Hidden** | **Visible** (Light / Dark switcher) | **Visible** (Light / Dark switcher) |
| **Bottom Navigation** | **Hidden** (Locked single-form self-service surface) | **3 Tabs** (Register, Branch Logbook, Safety) | **4 Tabs** (Register, Global Logbook, Cloud & Admin, Safety) |
| **Data Scope** | Local terminal active branch | Strictly scoped to terminal's assigned branch | Global (All international branches + multi-filter) |
| **Visitor Pass Dismiss** | Auto-resets with live countdown timer (5s) + "Next Visitor" CTA | Manual "Share" & "Done" buttons | Manual "Share" & "Done" buttons |
| **Record Deletion** | Prohibited | Prohibited | Enabled (Cascade deletes local SQLite + Cloud Firestore) |

---

## 2. Global Application Shell & Top App Bar Architecture

The global application shell is constructed using Jetpack Compose `Scaffold` with edge-to-edge system window insets, a dynamic `CenterAlignedTopAppBar`, a contextual `NavigationBar`, and a floating `SnackbarHost`.

### 2.1 Top App Bar Visual Layout

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ [White Logo Box] Branch Full Name / Mode Title          (Logged Chip)  [Theme]  [Bell]  [Lock/Role]│
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

#### Detailed Element Specifications:
1. **Company Logo Container**:
   - Surface container: Pure White (`#FFFFFF`).
   - Dimensions: `30dp x 30dp`, rounded corners `8dp`.
   - Border: `1dp` solid `outlineVariant` (`#CBD5E1` in light, `#334155` in dark).
   - Content: High-resolution branch company logo (`AsyncImage`, `ContentScale.Fit`, `padding(3.dp)`). If logo is unset, displays a fallback role icon (`Store`, `Badge`, or `Security`) in `SaitPrimary` (`#0F2942`).
2. **Title Text**:
   - Font: `16sp`, `FontWeight.Bold`, single-line truncated (`maxLines = 1`).
   - Color: `MaterialTheme.colorScheme.onSurface`.
   - String logic:
     - In `VISITOR_KIOSK`: Always displays `activeBranch.name` (e.g., `"Dubai Headquarters (UAE)"`). *Never displays the word "KIOSK"*.
     - In `RECEPTIONIST`: Displays `activeBranch.name` on registration tab; displays `"${activeBranch.shortCode} RECEPTION"` on secondary tabs.
     - In `ADMIN`: Displays `activeBranch.name` on registration tab; displays `"${activeBranch.shortCode} ADMIN"` on secondary tabs.
3. **Logged Count Chip** (Receptionist & Admin modes only):
   - Pill badge with `RoundedCornerShape(12.dp)`.
   - Background: `StatusSuccess.copy(alpha = 0.15f)` (`#10B981` at 15%).
   - Text: `"[N] Logged"` (`11sp`, `FontWeight.Bold`, `#10B981`).
4. **Theme Toggle Button** (Receptionist & Admin modes only):
   - Interactive `IconButton` (`48dp` touch target).
   - Icon: `Icons.Default.LightMode` (when currently dark) or `Icons.Default.DarkMode` (when light).
   - Toggles instant high-contrast enterprise theme without restarting activity.
5. **Notification Bell Button** (Receptionist & Admin modes only):
   - `BadgedBox` containing `IconButton`.
   - Badge: Crimson red circle (`#EF4444`) with white bold text showing unread notification count.
   - Icon: `Icons.Default.Notifications` (filled when unread > 0, outlined when 0).
   - Tap action: Opens `NotificationCenterDialog`.
6. **Return to Kiosk Button** (Admin mode only):
   - `IconButton` with `Icons.Default.Store`.
   - Tap action: Immediately locks console into `VISITOR_KIOSK` mode.
7. **Role Access Shortcut Button**:
   - In `VISITOR_KIOSK`:
     - Container: `48dp x 48dp` Box with `pointerInput`.
     - Interaction: **3-second touch and hold only**. Single-tap does **nothing** (no dialog, no toast, preventing visitor tampering).
     - Visual feedback: Lock icon turns `primary` while pressed; upon 3,000ms duration, triggers haptic feedback and launches `AdminPinDialog`.
   - In `RECEPTIONIST`:
     - Standard `IconButton` with `Icons.Default.AdminPanelSettings`.
     - Single tap immediately opens `AdminPinDialog` (prompting for Admin PIN to elevate privileges).

---

## 3. Bottom Navigation Bar Configurations

The bottom bar is hidden in Kiosk mode and appears in Staff modes with Material 3 active indicator pills, unread count badges, and labeled icons:

### 3.1 Receptionist Mode (3 Tabs)
1. **Manual Entry (`Tab 0`)**:
   - Icon: `Icons.Default.PersonAdd` (Active) / `Icons.Outlined.PersonAdd` (Inactive).
   - Label: `"Manual Entry"`.
2. **Visitor Logbook (`Tab 1`)**:
   - Icon: `Icons.Default.Assignment` (Active) / `Icons.Outlined.Assignment` (Inactive).
   - Label: `"Visitor Logbook"`.
   - Badge: Displays current active branch checked-in count.
3. **Safety Guidelines (`Tab 2`)**:
   - Icon: `Icons.Default.HealthAndSafety` (Active) / `Icons.Outlined.HealthAndSafety` (Inactive).
   - Label: `"Safety"`.

### 3.2 Enterprise Admin Mode (4 Tabs)
1. **Register (`Tab 0`)**:
   - Icon: `Icons.Default.PersonAdd`.
   - Label: `"Register"`.
2. **Global Logbook (`Tab 1`)**:
   - Icon: `Icons.Default.Assessment`.
   - Label: `"Logbook"`.
   - Badge: Displays total multi-branch logged records.
3. **Cloud & Admin (`Tab 2`)**:
   - Icon: `Icons.Default.CloudSync`.
   - Label: `"Cloud & Admin"`.
   - Badge: Displays Amber dot if offline/unsynced records exist.
4. **Safety (`Tab 3`)**:
   - Icon: `Icons.Default.HealthAndSafety`.
   - Label: `"Safety"`.

---

## 4. Screen 1: Visitor Registration Screen

The entry point for visitors and staff. Rendered as a centered, responsive card on tablets (`widthIn(max = 680.dp)`) and full-width on mobile.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ HERO HEADER: [White Logo] SAIT GROUP - Dubai HQ (UAE)                       │
│ Location: Plot 42, JAFZA South • Marine & Offshore Engineering Yard          │
│ [ 🛡️ View Facility Safety Guidelines & Emergency Exit Drawing ]             │
├─────────────────────────────────────────────────────────────────────────────┤
│ RECENT VISITORS QUICK-FILL (Receptionist & Admin modes only)                │
│ [John Smith - Technip]  [Tariq Al-Mansoor - Drydocks]  [Elena Rostova]      │
├─────────────────────────────────────────────────────────────────────────────┤
│ REGISTRATION FORM:                                                          │
│ 1. Full Name *                  [ 👤 Johnathan Doe                 ] [ ✕ ]  │
│ 2. Mobile Number *              [ 🇦🇪 +971 ▾ ] [ 50 123 4567         ] [ ✕ ]  │
│ 3. Email ID *                   [ ✉️ j.doe@clientenergy.com         ] [ ✕ ]  │
│    (Required for Visitor Pass & Safety Briefing delivery)                   │
│ 4. Company / Organization *     [ 🏢 McDermott Middle East          ] [ ✕ ]  │
│ 5. Visit Type                   (•) Walk-In  ( ) Meeting  ( ) Contractor    │
│ 6. Company / Branch             [ Dubai Headquarters (UAE)         ▾ ]      │
│ 7. Department *                 [ Operations & Marine              ▾ ]      │
│ 8. Purpose of Visit *           [ Meeting ] [ Audit ] [ Delivery ] [Other]  │
│ 9. Person to Meet / Host        [ 🪪 Eng. Tariq Mansoor             ] [ ✕ ]  │
│                                                                             │
│ [✓] I have read and agree to follow all facility Safety Rules & QHSE *     │
│                                                                             │
│ [                  REGISTER VISITOR & ISSUE PASS (CTA)                    ] │
│                                                                             │
│ 🔒 Enterprise Encrypted • Auto-synced to Cloud Firestore • Instant Badge Pass│
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.1 Hero Header Section (`HeroHeader`)
- **Logo Container**: Solid White rounded box (`52dp x 52dp`, radius `12dp`, `1dp` border) containing the company logo.
- **Facility Title**: Active branch name in `20sp` Bold.
- **Subtitle**: `branch.facilityLocation` or `"${branch.city} • ${branch.subtitle}"` in `13sp` neutral slate.
- **Safety Induction CTA Button**:
  - Full-width outlined button with green shield icon: `"View Facility Safety Guidelines & Emergency Exit Drawing"`.
  - Accessible to **ALL users** (Visitors on Kiosk, Receptionists, and Admins). Single-tap opens `SafetyInfoDialog`.

### 4.2 Repeat Visitor Quick-Fill Bar (Staff Modes Only)
- Horizontal scrolling carousel of past visitor chips (`ElevatedFilterChip`).
- Shows visitor name and company.
- Single tap auto-populates Full Name, Mobile, Email, and Company fields.

### 4.3 Form Input Specifications & Type-Case Rules
Every field includes explicit capitalization, keyboard type, rounded corners (`12dp`), clear buttons, and live validation:

1. **Full Name Field**:
   - Label: `"Full Name *"`
   - Type Case: `KeyboardCapitalization.Words` (Title Case: each word capitalized).
   - Keyboard: `KeyboardType.Text`.
   - Leading Icon: `Icons.Default.Person`.
   - Trailing Action: One-touch Clear icon (`✕`).
   - Validation: Required; min 2 characters.
2. **Country Code Selector & Mobile Number Row**:
   - Layout: Side-by-side aligned row.
   - **Country Selector Box**:
     - Auto-defaults to the country corresponding to the active branch (e.g., `+971` for UAE, `+966` for Saudi, `+32` for Belgium, `+91` for India, `+1` for USA/Canada).
     - Displays Country Flag emoji + Dial Code + Dropdown Arrow (`▾`).
     - Tap action: Opens `CountryCodePickerDialog` with priority countries and search.
   - **Mobile Number Field**:
     - Label: `"Mobile Number *"`.
     - Keyboard: `KeyboardType.Phone`.
     - Leading Icon: `Icons.Default.Phone`.
     - Trailing Action: One-touch Clear icon (`✕`).
     - Validation: Required; min 6 digits.
3. **Email ID Field (Mandatory)**:
   - Label: `"Email ID *"`.
   - Keyboard: `KeyboardType.Email`.
   - Leading Icon: `Icons.Default.Email`.
   - Trailing Action: One-touch Clear icon (`✕`).
   - Helper Text: `"Required for digital pass & safety copy delivery"`.
   - Validation: Mandatory; must match standard email pattern (`name@domain.ext`). Displays red error border and message if blank or malformed.
4. **Company / Organization Name**:
   - Label: `"Company / Organization *"`.
   - Type Case: `KeyboardCapitalization.Words`.
   - Leading Icon: `Icons.Default.Business`.
   - Trailing Action: One-touch Clear icon (`✕`).
   - Validation: Required.
5. **Visit Type Selector**:
   - Segmented buttons / filter chips: `"Walk-In"`, `"Scheduled Meeting"`, `"Contractor / Vendor"`, `"Delivery / Logistics"`.
6. **Branch Selector** (Staff Modes Only):
   - Outlined dropdown allowing receptionists or admins to log visitors into alternative regional facilities. (Locked to current branch in Kiosk mode).
7. **Host Department Selector**:
   - Outlined dropdown populated dynamically from `activeBranch.defaultDepartments` (e.g., *Projects, QHSE, Operations & Marine, Management, IT Systems, Procurement, Workshop*).
8. **Purpose of Visit Selector**:
   - Horizontal wrap of chips populated dynamically from global purpose registry (e.g., *Business Meeting, Technical Discussion, Site Audit & Inspection, Material Delivery, Client Interview, Maintenance, HSE Training, Other*).
   - If `"Other"` is selected, dynamically renders an additional `OutlinedTextField` for custom purpose entry.
9. **Person to Meet / Host**:
   - Label: `"Person to Meet / Host Name"`.
   - Type Case: `KeyboardCapitalization.Words`.
   - Leading Icon: `Icons.Default.Badge`.
   - Trailing Action: One-touch Clear icon (`✕`).

### 4.4 Mandatory Safety Induction Checkbox Card
- Elevated card with subtle green tint (`StatusSuccess.copy(alpha = 0.08f)`).
- Checkbox: Emerald green accent (`#10B981`).
- Label: `"I have read and agree to follow all facility Safety Rules, PPE guidelines, and Emergency Evacuation instructions *"`.
- Text includes clickable link opening the Safety Information modal.
- Validation: Registration cannot proceed unless checked.

### 4.5 Registration CTA & Loading States
- **Primary Button**: Large CTA (`52dp` height, `RoundedCornerShape(14.dp)`).
  - Normal State: Navy fill, White text: `"Register Visitor & Issue Pass"`.
  - Loading State: Replaced by circular progress indicator (`24dp`) with text `"Generating Pass & Dispatching Email..."`.
  - Disabled State: Subtle slate opacity if mandatory fields are missing.

### 4.5 Touchless Mobile QR Registration & Contactless Kiosk Station (`TouchlessKioskView`)

To prevent cross-contamination and eliminate waiting queues at the reception desk, the kiosk features a zero-touch registration mode. Visitors can complete their entire check-in workflow on their personal smartphone without touching the physical tablet glass.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ TOUCHLESS KIOSK STATION: [White Logo] SAIT GROUP — Dubai HQ (UAE)           │
│ Silicon Oasis Tech Park, Facility Bldg B • [ 🟢 TOUCHLESS CHECK-IN STATION ] │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                     SCAN WITH YOUR PHONE CAMERA                             │
│       Point your camera at this QR code to register on your phone           │
│                     without touching this screen.                           │
│                                                                             │
│                            ┌─────────────┐                                  │
│                            │ █▀▀▀█ █ █▀▀▀█ │                                  │
│                            │ █ █ █ ▄ █ █ █ │                                  │
│                            │ █▀▀▀█ █ █▀▀▀█ │                                  │
│                            │   [SAIT]    │                                  │
│                            │ ▀▀ ▀▀▀█▀  ▀▀█ │                                  │
│                            └─────────────┘                                  │
│                                                                             │
│                (•) Kiosk Ready • Listening for Mobile Check-in              │
│                 Session: KS-DXB-9482  [🔄 Refresh] [📋 Copy]                 │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│ 1. 📷 Scan QR Code          Point camera at QR to open secure form.         │
│ 2. 📱 Enter Details on Phone Name, email ID, phone & host on mobile screen. │
│ 3. 🎟️ Pass Appears Instantly Kiosk detects check-in & issues badge + email. │
├─────────────────────────────────────────────────────────────────────────────┤
│ [ 🛡️ Safety Rules ]                      [ 👆 Use Touchscreen Instead ]     │
│ [ ⚡ Simulate Phone Registration (Instant Demo & Test Flow) ]                │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### How the Touchless Architecture Works:
1. **Embedded Kiosk Local Web Server & Private Hotspot (Primary & Recommended)**:
   - The Android Kiosk tablet runs a zero-dependency, lightweight HTTP web server (`KioskLocalWebServer`) listening on `0.0.0.0:8080`.
   - The device creates a local Wi-Fi Hotspot (or operates across the office LAN):
     * **Step 1: Wi-Fi Hotspot Connect QR (`WIFI:S:...;T:WPA;P:...;;`)**: The visitor scans this QR with their iPhone or Android camera, which automatically prompts "Join Network". Tapping it connects their phone to the kiosk's private hotspot in one second without typing passwords!
     * **Step 2: Local WebUI QR (`http://<ip>:8080/`)**: Scanning this opens the responsive SAIT Group visitor registration page served straight from the tablet's local memory.
     * **100% Offline & Zero Cloud Server Dependency**: Works even without internet. Impossible to produce 404 or "Site Not Found" errors!
     * When submitted, the phone sends `POST /api/register` locally to the tablet. The tablet registers the visitor, writes to SQLite & Cloud Firestore, triggers an immediate pass popup on the kiosk screen, and responds with HTTP 200 + pass details to the phone.
2. **Alternative Zero-Server QR Modes**:
   - Offers additional instant check-in methods selectable via top chips:
     * **✉️ Direct Email (Zero Error / Zero 404)**: Scans directly into the phone's native Email app with all registration fields pre-filled (`Name`, `Company`, `Host`, `Branch`). Tap Send to register without any web server.
     * **💬 WhatsApp Direct**: Opens WhatsApp directly to the facility reception helpline with pre-formatted registration template.
     * **📋 Google Form**: Opens your facility's Google Form for quick visitor entry.
     * **🌐 Custom Web Form**: Opens `mobile_portal.html` once deployed to Firebase Hosting or GitHub Pages.
3. **Autonomous Kiosk Detection & Auto-Pass Issuance (Zero Touches)**:
   - When a visitor completes registration on their mobile phone, the kiosk tablet immediately catches the check-in.
   - The tablet instantly transitions from the QR code screen to the **Official Visitor Pass Modal**, displays `"Welcome to SAIT! Hotspot Mobile Check-in Verified"`, dispatches automated confirmation emails with badge & QHSE guidelines, and counts down (15s) before resetting for the next visitor.
4. **Touchscreen Fallback & In-App Simulator**:
   - Visitors without smartphones or low battery can tap `"Use Touchscreen"` at any time.
   - Admin & Receptionists can tap `"Simulate Phone Registration"` to demo and test the live touchless round-trip without a second physical device.

---

## 5. Screen 2: Official Visitor Pass Modal Dialog

Rendered as an enterprise badge pass with an authentic lanyard slot punch hole and high-contrast digital badging.

```
┌─────────────────────────────────────────────────────────────┐
│                         [ ====== ]                      [✕] │  <-- Lanyard Cutout Slot & Close
├─────────────────────────────────────────────────────────────┤
│  SAIT GROUP — DUBAI HEADQUARTERS (UAE)                      │  <-- Solid White Logo + Navy Gradient
│  OFFICIAL VISITOR PASS                                      │
├─────────────────────────────────────────────────────────────┤
│                 [ 🟢 ACTIVE ON PREMISES ]                   │  <-- Status Pill
│                                                             │
│                      JOHNATHAN DOE                          │  <-- Visitor Name (22sp Bold)
│               🏢 McDermott Middle East                      │  <-- Company
│                                                             │
│ ─────────────────────────────────────────────────────────── │
│  Branch:          Dubai Headquarters (UAE)                  │
│  Pass ID:         SAIT-DXB-260001 (Monospace)               │
│  Email ID:        j.doe@clientenergy.com                    │
│  Visit Type:      Scheduled Meeting                         │
│  Department:      Operations & Marine                       │
│  Purpose:         Facility Audit & Inspection               │
│  Person to Meet:  Capt. Tariq Mansoor                       │
│  Registered At:   03 Nov 2026, 10:24 AM                     │
│ ─────────────────────────────────────────────────────────── │
│                                                             │
│                     ┌───────────────┐                       │
│                     │  QR CODE (80) │                       │  <-- High-Res QR Box with Monospace
│                     │  SAIT-DXB-26  │                       │      Pass Number
│                     └───────────────┘                       │
│                                                             │
│  ℹ️ Official pass & safety briefing have been sent to       │
│     j.doe@clientenergy.com. Please proceed to reception.    │
├─────────────────────────────────────────────────────────────┤
│  ⏱️ Kiosk resets for next visitor in 5s... [████████░░░░░]   │  <-- Live Circular / Progress Timer
├─────────────────────────────────────────────────────────────┤
│ [            NEXT VISITOR REGISTRATION (CTA)              ] │  <-- Full-width Kiosk Button
│      (Or [Share Pass Details] / [Done] in Staff Mode)       │
└─────────────────────────────────────────────────────────────┘
```

### Detailed Elements:
1. **Lanyard Cutout Punch Hole**: Centered rounded slot (`60dp x 10dp`, `5dp` corner radius) mimicking a physical PVC credential badge.
2. **Top Banner**: Deep navy horizontal gradient (`#0F2942` to `#0369A1`) containing:
   - Pure white logo container (`44dp x 44dp`, radius `10dp`).
   - `"SAIT GROUP"` in 20sp Black uppercase with `2sp` letter spacing.
   - Branch Name in Sky Cyan (`#BAE6FD`).
   - `"OFFICIAL VISITOR PASS"` badge title.
3. **Status Pill**: Emerald green container (`StatusSuccess`) with `Icons.Default.CheckCircle` and `"ACTIVE ON PREMISES"`.
4. **Metadata Table (`PassDetailRow`)**: Key-value rows with subtle dividers displaying Branch, Pass ID in bold monospace, Email ID, Visit Type, Department, Purpose, Host Name, and Formatted Timestamp.
5. **QR Code Container**: Pure white box with outline border containing a high-contrast QR matrix graphic and the serial ID printed in monospace underneath.
6. **Automated Email Dispatch Notice**: Informs the visitor that a copy of the pass, barcode, and emergency safety guidelines have been sent to their email ID.
7. **Auto-Reset Countdown Bar (Kiosk Mode)**:
   - Displays live decreasing countdown (`"Kiosk resets for next visitor in 5s"`).
   - Resets the registration form and dismisses dialog automatically upon timer expiry.
8. **Action Buttons**:
   - In Kiosk: Full-width `"Next Visitor Registration"` button.
   - In Staff Mode: `"Share"` button (triggering Android system share sheet) and `"Done"` button.

---

## 6. Screen 3: Visitor Logbook & Executive Analytics Screen

The Logbook adapts between an enterprise multi-branch executive console (for Admin) and a focused single-facility register (for Receptionist).

```
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│ ADMIN COMPREHENSIVE DASHBOARD:                                                            │
│ ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐          │
│ │ 3,420           │ │ 48              │ │ 12              │ │ 1h 45m          │          │
│ │ Total Check-ins │ │ Active Today    │ │ On Premises Now │ │ Avg Duration    │          │
│ └─────────────────┘ └─────────────────┘ └─────────────────┘ └─────────────────┘          │
│ ┌───────────────────────────────────────┐ ┌─────────────────────────────────────────────┐│
│ │ 7-DAY VISITOR TRAFFIC TREND (CANVAS)  │ │ BRANCH VOLUME COMPARISON                    ││
│ │ [Mon] [Tue] [Wed] [Thu] [Fri] [Sat]   │ │ Dubai HQ (UAE)      ███████████████ 45%     ││
│ │ Peak: Wednesday (68 visitors)         │ │ Dammam Base (KSA)   ████████░░░░░░░ 28%     ││
│ └───────────────────────────────────────┘ └─────────────────────────────────────────────┘│
│ ┌───────────────────────────────────────┐ ┌─────────────────────────────────────────────┐│
│ │ DEPARTMENT TRAFFIC BREAKDOWN          │ │ TOP 5 VISITING COMPANIES                    ││
│ │ Operations: 38% | QHSE: 24% | Eng: 18%│ │ 1. Saudi Aramco   2. McDermott   3. Technip ││
│ └───────────────────────────────────────┘ └─────────────────────────────────────────────┘│
├───────────────────────────────────────────────────────────────────────────────────────────┤
│ SEARCH & MULTI-FILTER BAR:                                                                │
│ [ 🔍 Search by Name, Mobile, Company, Pass ID...                 ] [ ✕ Clear ]            │
│ Branch:     [All Branches ▾]  [Dubai HQ]  [Dammam Base]  [Doha Marine]  [Antwerp Base]    │
│ Department: [All]  [Operations]  [QHSE]  [Engineering]  [Management]  [Warehouse]        │
│ Purpose:    [All]  [Meeting]  [Audit]  [Inspection]  [Delivery]  [Maintenance]            │
│ Date Range: [All Time]  [Today]  [This Week]  [This Month]           [ 📤 Export CSV ]    │
├───────────────────────────────────────────────────────────────────────────────────────────┤
│ VISITOR RECORDS LIST (Interactive Expandable Cards):                                      │
│                                                                                           │
│ ┌───────────────────────────────────────────────────────────────────────────────────────┐ │
│ │ SAIT-DXB-260001   Alexander Wright                  OFFSHORE ENG LTD     [ON SITE 🟢] │ │
│ │ 🏢 Operations & Marine   🎯 Facility Audit & Inspection   🕒 10:24 AM    [ ▼ Expand ] │ │
│ ├───────────────────────────────────────────────────────────────────────────────────────┤ │
│ │ EXPANDED DETAILS:                                                                     │ │
│ │ • Email ID: alexander.wright@clientcorp.com           [ ✉️ Send Email ]               │ │
│ │ • Mobile: 🇦🇪 +971 50 123 4567                         [ 📞 Call Host / Visitor ]      │ │
│ │ • Host / Person to Meet: Capt. Tariq Mansoor (Marine Operations Lead)                 │ │
│ │ • Facility: Dubai Headquarters (UAE) • Plot 42, JAFZA South                           │ │
│ │ • On-Premises Duration: 2h 15m (Active)                                               │ │
│ │ • Safety Induction Status: [✓] QHSE Rules Acknowledged                                │ │
│ │                                                                                       │ │
│ │ [ 🪪 View Digital Pass Badge ]                 [ 🗑️ Delete Record (Cascade Sync) ]   │ │
│ └───────────────────────────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────────────────────────────┘
```

### 6.1 Admin Comprehensive Dashboard Components
1. **6-Tile KPI Metrics Grid**:
   - Total Check-ins (All-time volume across all branches).
   - Active Today (Registered today).
   - Currently on Premises (Active badges not checked out).
   - Average Visit Duration (Calculated from timestamp telemetry).
   - International Branches Active.
   - Safety Compliance Rate (100% Induction Signed).
2. **7-Day Trend Canvas**:
   - Custom `androidx.compose.foundation.Canvas` rendering daily bar charts with interactive date labels, accent highlights, and peak visit markers.
3. **Branch Distribution Panel**:
   - Compares visitor distribution across Dubai, Abu Dhabi, Dammam, Doha, Antwerp, Kuwait, Muscat, Bahrain, Mumbai, London, Algiers, and Houston.
4. **Department & Purpose Breakdown Cards**:
   - Progress bar visualizations ranking departments by traffic and purposes by frequency.
5. **Top Companies & Peak Hours Panel**:
   - Top 5 client and contractor corporations visiting facilities.
   - Peak hour analysis bar chart highlighting morning rush (08:00–10:30) vs afternoon deliveries (13:30–15:00).

### 6.2 Receptionist Scoped Logbook Header
- Dedicated facility banner: Displays current terminal branch name, address, and live local status.
- Local metrics tiles: `"Active on Site"` counter and `"Today's Check-ins"` counter.
- Quick search bar and purpose chips scoped strictly to the receptionist's branch.
- Dedicated `"Export Branch Log (CSV)"` button.

### 6.3 Expandable Visitor Record Card (`VisitorLogCard`)
- **Collapsed View**: Monospace pass number badge, Full name, Company, Department tag, Purpose tag, Check-in timestamp with clock icon, status badge (`ON SITE` in emerald green or `DEPARTED` in grey), and expand chevron.
- **Expanded View**:
  - Email ID with one-tap email intent launcher (`mailto:`).
  - Formatted phone number with country flag and one-tap dial launcher (`tel:`).
  - Host / Person to Meet.
  - Facility Location text.
  - Active duration spent on premises.
  - Safety Induction verified badge (`"QHSE Rules Acknowledged"`).
  - Quick-action buttons: `"View Pass Badge"` (re-opens `VisitorPassDialog`) and `"Delete Record"` (Admin only).

### 6.4 Delete Record Confirmation Dialog (`AlertDialog`)
- Triggered exclusively by Admin.
- Icon: Red trash icon (`Icons.Default.DeleteOutline`).
- Title: `"Delete Visitor Record?"`.
- Text: Explains that deleting permanently purges the record from both local SQLite storage and remote Cloud Firestore database.
- Actions: Destructive `"Delete"` button and `"Cancel"` button.

### 6.5 Empty States (`EmptyLogbookState`)
- **Search No-Match State**: Illustrated magnifying glass graphic, `"No matching records found"`, `"Try adjusting your keywords or clearing filters"`, with a `"Reset All Filters"` CTA.
- **Empty Database State**: Illustrated visitor credential graphic, `"Visitor Logbook is Empty"`, `"Registered visitors will appear here automatically"`.

---

## 7. Screen 4: Enterprise Admin & Cloud Dashboard

The nerve centre of the application. Handles multi-branch configurations, SMTP mail routing, Google Sign-In, and security policies.

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│ ENTERPRISE ADMIN & CLOUD MANAGEMENT CONSOLE                                                 │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│ 1. ADMIN AUTHENTICATION & TERMINAL MODE:                                                    │
│    Logged in as: Administrator (Master Key)                   [ Switch to Kiosk / Logout ]  │
│    Active Operating Branch: [ Dubai Headquarters (UAE)                                  ▾ ] │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│ 2. APPEARANCE & DISPLAY THEME:                                                              │
│    Current Theme: [ Light Mode ☀️ ]  [ Dark Mode 🌙 ]  [ Follow System ⚙️ ]                  │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│ 3. FIREBASE CLOUD FIRESTORE & GOOGLE SIGN-IN:                                               │
│    Status: [ 🟢 CLOUD SYNCHRONIZED ]   User: mohamedrihsab@gmail.com                        │
│    [ 🔄 Sync Now ]     [ 🔐 Sign Out from Google ]                                          │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│ 4. BRANCH & COMPANY REGISTRY MANAGEMENT:                              [ ＋ Add New Branch ] │
│                                                                                             │
│  ┌────────────────────────────────────────────────────────────────────────────────────────┐ │
│  │ [White Logo] SAIT FZCO — Dubai Headquarters (UAE)                     Short Code: DXB   │ │
│  │ Facility: Plot 42, Jebel Ali Free Zone (JAFZA South), Dubai, UAE       Country: +971 🇦🇪 │ │
│  │ Helpline: +971 4 881 0000 • Eng. Tariq Mansoor (QHSE Lead)                             │ │
│  │ Departments: [PROJECTS ✕] [QHSE ✕] [OPERATIONS ✕] [MANAGEMENT ✕] [ ＋ Add Dept ]       │ │
│  │                                                                                        │ │
│  │ [ ✏️ Edit Details ]   [ 🛡️ Edit Safety & Map ]   [ 🖼️ Upload Logo ]   [ 🗺️ Upload Map ] │ │
│  │                                                      [ 🗑️ Delete Branch ]              │ │
│  └────────────────────────────────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│ 5. PURPOSE OF VISIT OPTIONS MANAGER:                                  [ ＋ Add Purpose ]    │
│    [Business Meeting ✕] [Facility Audit ✕] [Material Delivery ✕] [Site Inspection ✕]        │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│ 6. AUTOMATED DEPARTMENT EMAIL ROUTING GROUPS:                                               │
│    • QHSE & Safety: qhse@saitgroup.com, safetydesk@saitgroup.com      [ ✏️ Edit Recipients ] │
│    • Operations: opslead@saitgroup.com, yardcontrol@saitgroup.com     [ ✏️ Edit Recipients ] │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│ 7. OUTGOING MAIL SERVER (SMTP) & PASS DISPATCH ENGINE:                                      │
│    Server Host: [ smtp.gmail.com          ]   Server Port: [ 587 ]                          │
│    Security:    (•) TLS   ( ) SSL   ( ) None                                                │
│    Username:    [ visitorcontrol@saitgroup.com                                            ] │
│    Password:    [ •••••••••••••••••                                                       ] │
│    Sender Name: [ SAIT Group Visitor Control                                              ] │
│    Sender Mail: [ visitorcontrol@saitgroup.com                                            ] │
│    [✓] Automatically email Visitor Pass & Safety Briefing on registration                   │
│                                                                                             │
│    [ ⚡ RUN SMTP LIVE CONNECTION TEST ]  ==> [ 🟢 Handshake & Auth OK (342ms) ]             │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│ 8. GOOGLE DRIVE BACKUP & EXPORT:                                                            │
│    Drive Status: Connected (Google Workspace)           [ 📤 Backup All Records to Drive ] │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│ 9. SECURITY PIN CONTROLS & TERMINAL MAINTENANCE:                                            │
│    [ 🔑 Change Admin PIN (6-Digit) ]          [ 🪪 Change Receptionist PIN (6-Digit) ]       │
│    [ ⚠️ Purge All Visitor Records (Destructive Local & Cloud Sync) ]                        │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. Comprehensive Admin & Configuration Dialogs Catalog

The following is an exhaustive inventory of every configuration modal and dialog available in the application:

### 8.1 Add Branch Dialog (`AddBranchDialog`)
- **Trigger**: Click `[＋ Add New Branch]` in Admin Dashboard.
- **Fields**:
  1. `Company / Branch Name *`: Outlined text input (`KeyboardCapitalization.Words`).
  2. `Short Code *`: Upper-case code (`KeyboardCapitalization.Characters`, e.g. `SAIT-SHJ`).
  3. `Country Code Dropdown (`BranchCountryDropdownField`)`: Full searchable dropdown containing **all world countries** with dial codes and flags.
  4. `City / Country *`: Text field auto-populated upon country selection (e.g. `"Sharjah / UAE"`).
  5. `Subtitle / Facility Type`: Descriptive text (e.g. `"Marine Engineering Yard"`).
  6. `Facility Location Text`: Complete postal / industrial zone location (e.g. `"Sharjah • Marine Engineering Yard"`).
- **Validation**: Requires Firebase login; Name, Short Code, and City cannot be blank.
- **Actions**: `"Create Branch"` (Primary CTA) and `"Cancel"`.

### 8.2 Edit Branch Details Dialog (`EditBranchDetailsDialog`)
- **Trigger**: Click `[✏️ Edit Details]` on any branch card.
- **Pre-populated Fields**:
  1. `Company / Branch Name *`
  2. `Short Code *` (Pass serial prefix city code)
  3. `Country Dropdown`: Searchable all-countries selector.
  4. `City / Country *`
  5. `Subtitle / Facility Type`
  6. `Facility Location Text`
- **Actions**: `"Save Changes"` (updates local state and syncs to Cloud Firestore) and `"Cancel"`.

### 8.3 Delete Branch Confirmation Dialog (`DeleteBranchDialog`)
- **Trigger**: Click `[🗑️ Delete Branch]` on any branch card.
- **Content**: Warning alert explaining that removing the branch deletes it from the international terminal registry and synchronizes the deletion to Cloud Firestore.
- **Actions**: Destructive `"Delete Branch"` and `"Cancel"`.

### 8.4 Add Department Dialog (`AddDepartmentDialog`)
- **Trigger**: Click `[＋ Add Dept]` on a branch card.
- **Field**: `Department Name *` (`KeyboardCapitalization.Characters`, e.g., `"FABRICATION YARD"`).
- **Actions**: `"Add Department"` and `"Cancel"`.

### 8.5 Add Purpose of Visit Dialog (`AddPurposeDialog`)
- **Trigger**: Click `[＋ Add Purpose]` in Purpose Manager section.
- **Field**: `Purpose Name *` (`KeyboardCapitalization.Words`, e.g., `"Regulatory Authority Audit"`).
- **Actions**: `"Add Purpose"` and `"Cancel"`.

### 8.6 Edit Department Emails Dialog (`EditDepartmentEmailsDialog`)
- **Trigger**: Click `[✏️ Edit Recipients]` on any department routing card.
- **Content**:
  - Displays Department Name (e.g., `"QHSE & Safety"`).
  - Multi-line text field for comma-separated email addresses (`qhse@saitgroup.com, lead@saitgroup.com`).
  - Helper note: `"Staff listed here receive automatic visitor check-in email alerts when visitors arrive for this department."`
- **Actions**: `"Save Email List"` and `"Cancel"`.

### 8.7 Change Admin PIN Dialog (`ChangePinDialog`)
- **Trigger**: Click `[🔑 Change Admin PIN]` in Security Controls.
- **Input**: Masked 4 to 6-digit numeric input with `PasswordVisualTransformation`.
- **Validation**: Minimum 4 digits, maximum 6 digits, numbers only.
- **Actions**: `"Update Admin PIN"` and `"Cancel"`.

### 8.8 Change Receptionist PIN Dialog (`ChangeReceptionistPinDialog`)
- **Trigger**: Click `[🪪 Change Receptionist PIN]` in Security Controls.
- **Input**: Masked 4 to 6-digit numeric input with `PasswordVisualTransformation`.
- **Validation**: Minimum 4 digits, maximum 6 digits, numbers only.
- **Actions**: `"Update Receptionist PIN"` and `"Cancel"`.

### 8.9 Clear Records Confirmation Dialog (`ClearRecordsDialog`)
- **Trigger**: Click `[⚠️ Purge All Visitor Records]`.
- **Icon**: Red warning alert triangle.
- **Content**: `"Are you sure you want to delete ALL visitor logbook records? This action is permanent and wipes all local and Cloud Firestore visitor data across all branches."`
- **Actions**: Destructive `"Clear All Records"` (`#EF4444`) and `"Cancel"`.

### 8.10 Edit Safety Guidelines & Evacuation Plan Dialog (`EditSafetyGuidelinesDialog`)
- **Trigger**: Click `[🛡️ Edit Safety & Map]` on any branch card.
- **Fields**:
  1. `Facility Location *`: Full text address for safety induction cards.
  2. `Safety Rules & Guidelines *`: Multiline editor (3-4 lines) for company HSE policies.
  3. `Emergency Helpline Numbers *`: Phone number input with phone icon (e.g. `+971 4 881 2345 / 999`).
  4. `Contact Person Name (Optional)`: Name and title of duty HSE officer (e.g. `Duty HSE Lead / Security Desk`).
  5. `Emergency Evacuation Plan Drawing`:
     - Displays current drawing status (`"Custom floor plan active"` or `"Preferred: PNG/JPG landscape 1280x720 px"`).
     - Action button: `"Change Map"` (launches Android Photo Picker `PickVisualMedia`).
- **Actions**: `"Save Safety Details"` and `"Cancel"`.

### 8.11 SMTP Live Connection Test Result State
- **Trigger**: Click `[⚡ Run SMTP Live Connection Test]`.
- **Loading State**: Circular progress indicator with label `"Connecting to SMTP server and validating credentials..."`.
- **Outcome Alert Card**:
  - Success: Emerald green card with `Icons.Default.CheckCircle`: `"SMTP Connection Successful! Handshake complete, TLS authentication verified (Response time: 342ms)."`.
  - Failure: Crimson card with `Icons.Default.Error`: `"SMTP Connection Failed: Authentication failed (535 5.7.8 Username and Password not accepted). Check App Password."`.

---

## 9. Screen 5: Safety Information & Emergency Evacuation Screen / Dialog

Visible to **ALL users** (Visitors, Receptionists, and Admins). Available both as a standalone full screen (`SafetyInfoScreen`) in staff navigation and as an instant modal dialog (`SafetyInfoDialog`) from the Kiosk registration screen.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ COMPANY & FACILITY LOCATION:                                                │
│ [White Logo] SAIT FZCO — Dubai Headquarters (UAE)                           │
│ Plot 42, Jebel Ali Free Zone (JAFZA South), Dubai, UAE                      │
│ ─────────────────────────────────────────────────────────────────────────── │
│ 📍 Facility Location:  Plot 42, JAFZA South, Dubai, UAE                     │
│ 🛡️ QHSE Compliance:    Quality, Health, Safety & Environment Enforced       │
│ 📞 Helpline & Lead:    Eng. Tariq Mansoor (QHSE Lead) • +971 4 881 0000     │
├─────────────────────────────────────────────────────────────────────────────┤
│ 24/7 EMERGENCY HELPLINE (DISPLAY ONLY — NO CALL BUTTON):                    │
│ [ 📞 ] 24/7 Emergency Helpline (DXB)                                       │
│        Contact Person: Eng. Tariq Mansoor (QHSE Lead)                       │
│        +971 4 881 0000 / +971 50 123 4567                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│ FACILITY SAFETY GUIDELINES & QHSE RULES:                                    │
│ Standard QHSE induction rules apply. All visitors must remain in designated │
│ walkways. Steel toe footwear and PPE required past the operational line.    │
│                                                                             │
│ [ 🪪 ] Visitor Badge Must Be Visible at all times on company premises      │
│ [ 🦺 ] Mandatory PPE (Steel-toe shoes, hi-vis vest, hard hat) in yards     │
│ [ 📷 ] Photography & Filming Strictly Prohibited without authorization     │
│ [ 🚭 ] Designated Smoking Areas Only — Smoking prohibited indoors          │
│ [ 🚨 ] Emergency & Evacuation: Follow green exit signs to Assembly Point 1  │
├─────────────────────────────────────────────────────────────────────────────┤
│ EMERGENCY EXIT & EVACUATION BLUEPRINT DRAWING:                              │
│ Follow green illuminated exit markers to designated Emergency Assembly Pt 1 │
│                                                                             │
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │ ┌──────────┐  Exit A (Green)  ┌──────────┐   Exit B (Green)             │ │
│ │ │ WORKSHOP │  ======🚪======  │ OFFICES  │   ======🚪======             │ │
│ │ │ ZONE 1   │                  │ ZONE 2   │                              │ │
│ │ │          │ - - - - - - - - -│ - - - - -│                              │ │
│ │ └──────────┘  Evacuation Path └──────────┘  Evacuation Path             │ │
│ │                                                                         │ │
│ │                 =====> [ 🟢 ASSEMBLY POINT 1 ] <=====                  │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ (Renders uploaded high-res image, or dark-slate interactive Canvas diagram) │
├─────────────────────────────────────────────────────────────────────────────┤
│ SECURE CLOUD & EMAIL PASS DELIVERY:                                         │
│ [ ☁️ ] Every registered visitor automatically receives their digital pass   │
│        and safety induction briefing by email. Logs synced to Firestore.    │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Detailed Specifications:
1. **Helpline Display Rule**: The 24/7 Helpline card displays contact numbers and the contact person's name prominently in bold, with **NO call button** (per corporate security protocol, display only).
2. **Interactive Architectural Evacuation Plan Canvas (`EmergencyEvacuationPlanCanvas`)**:
   - Background: Dark Blueprint Navy Slate (`#0F172A`).
   - Outer Facility Perimeter: Rounded boundary (`#334155`).
   - Workshop Zone 1 & Office Zone 2: Solid containers (`#1E293B`).
   - Evacuation Pathways: Emerald Green dashed vector path lines (`#34D399`) indicating egress routes.
   - Emergency Exit Doors: Illuminated green blocks (`#10B981`) marked North Exit A and North Exit B.
   - Assembly Point: External muster point icon (`#10B981`) indicating Assembly Point 1.

---

## 10. Global Utility Dialogs & System Overlays

### 10.1 Role PIN Authentication Dialog (`AdminPinDialog`)
Used to elevate privileges from Kiosk to Staff or Receptionist to Admin:
- **Title**: `"Enter Staff PIN"` or `"Enter Admin PIN"`.
- **Visual PIN Indicators**: 6 circular dots. Unfilled dots are soft grey; filled dots light up in primary navy.
- **Numeric Keypad**: 3x4 grid of rounded buttons (1-9, Backspace, 0, Clear).
- **Auto-Verification**: As soon as the 6th digit is tapped, automatically validates against `adminPin` and `receptionistPin`.
- **Error Shake & Feedback**: Displays red error text (`"Incorrect Admin PIN"`) and clears entered digits on mismatch.

### 10.2 Country Code Picker Dialog (`CountryCodePickerDialog`)
- **Trigger**: Click country selector in registration form.
- **Search Bar**: Sticky search field filtering by country name or dial code, with a clear icon.
- **Priority Countries Header**: Prominently highlights key operating nations:
  - 🇦🇪 United Arab Emirates (`+971`)
  - 🇸🇦 Saudi Arabia (`+966`)
  - 🇶🇦 Qatar (`+974`)
  - 🇰🇼 Kuwait (`+965`)
  - 🇴🇲 Oman (`+968`)
  - 🇧🇭 Bahrain (`+973`)
  - 🇮🇳 India (`+91`)
  - 🇧🇪 Belgium (`+32`)
  - 🇬🇧 United Kingdom (`+44`)
  - 🇩🇿 Algeria (`+213`)
  - 🇨🇦 Canada (`+1`)
  - 🇺🇸 United States (`+1`)
- **All Countries List**: Alphabetical listing of all world nations with flag emojis, full country names, and dial codes.
- **Item Tap**: Immediately selects country, closes dialog, and shifts focus to the mobile number input.

### 10.3 Notification Center Modal (`NotificationCenterDialog`)
- **Trigger**: Click bell icon in Top App Bar.
- **Header**: `"Notification Alerts"`, unread event counter, and Close icon.
- **Actions Bar**: `"Mark All Read"` (blue text button) and `"Clear History"` (red text button).
- **Categorized Event Cards**:
  1. `EMAIL_SENT`: Blue envelope icon (`#0284C7`) — `"Visitor Pass Emailed to [Email]"`.
  2. `EMAIL_FAILED`: Red error icon (`#EF4444`) — `"Failed to deliver pass to [Email]. Check SMTP"`.
  3. `VISITOR_CHECK_IN`: Green registration icon (`#10B981`) — `"[Name] checked in at [Department]"`.
  4. `VISITOR_CHECK_OUT`: Teal checkout icon (`#06B6D4`) — `"[Name] departed facility"`.
  5. `CLOUD_SYNC`: Cyan cloud icon (`#0EA5E9`) — `"Cloud Firestore Synchronized"`.
  6. `SYSTEM_ALERT`: Amber warning icon (`#F59E0B`) — `"Terminal offline / cache updated"`.
- **Card States**: Unread cards have solid surface backgrounds and an unread dot; read cards have 50% opacity.
- **Empty State**: Centered bell outline icon, `"No Notifications"`, `"New alerts will appear here"`.

---

## 11. Design Tokens, Typography & Brand Guidelines

### 11.1 Enterprise Color Palette

| Token Name | Light Theme Hex | Dark Theme Hex | Component Usage |
| :--- | :--- | :--- | :--- |
| `SaitPrimary` | `#0F2942` | `#38BDF8` | Top app bar, primary CTAs, active tab icons |
| `SaitSecondary` | `#0284C7` | `#0EA5E9` | Interactive accents, progress timers, badges |
| `SaitBackground` | `#F8FAFC` | `#090E1A` | Main window surface, anti-glare kiosk background |
| `SaitSurface` | `#FFFFFF` | `#111827` | Cards, modal sheets, input containers |
| `SaitSurfaceVariant` | `#F1F5F9` | `#1F2937` | Input field backgrounds, dividers, chips |
| `SaitOutline` | `#CBD5E1` | `#334155` | Borders, outlines, dialog dividers |
| `StatusSuccess` | `#10B981` | `#34D399` | Check-in badges, 100% safety compliance, QR boxes |
| `StatusWarning` | `#F59E0B` | `#FBBF24` | Offline indicators, unsynced count pills |
| `StatusError` | `#EF4444` | `#F87171` | Delete buttons, validation error outlines, alerts |
| **Logo Surface** | **`#FFFFFF`** | **`#FFFFFF`** | **Mandatory pure white container for company logos** |

### 11.2 Typography Tokens
- **Display Large (`28sp - 32sp`, ExtraBold, Monospace)**: Pass serial badges (`SAIT-DXB-260001`).
- **Headline Medium (`20sp - 24sp`, Bold)**: Hero titles, modal headers, executive metrics.
- **Title Medium (`16sp - 18sp`, SemiBold)**: Section titles, top app bar brand text, form field labels.
- **Body Large (`15sp - 16sp`, Regular/Medium)**: Text input entries, visitor names in lists.
- **Body Medium (`13sp - 14sp`, Regular)**: Descriptions, safety rules, email notifications.
- **Label Small (`10sp - 12sp`, Bold/Medium)**: Status pills, dial codes, timestamp tags.

---

## 12. Serial Number Generation Formula

Visitor serial numbers are deterministically generated to provide international industrial traceability:
$$\mathbf{Prefix - CityCode - YYXXXX}$$

1. **Prefix**: Corporate group code (Default: `SAIT`).
2. **CityCode**: 3-letter uppercase branch city identifier:
   - `DXB` = Dubai Headquarters (UAE)
   - `AUH` = Abu Dhabi Operations (UAE)
   - `DMM` = Dammam Marine Base (Saudi Arabia)
   - `DOH` = Doha Marine Facility (Qatar)
   - `ANT` = Antwerp Logistics Base (Belgium)
   - `KWI` = Kuwait City Facility (Kuwait)
   - `MCT` = Muscat Operations (Oman)
   - `BAH` = Manama Facility (Bahrain)
   - `BOM` = Mumbai Engineering Centre (India)
   - `LON` = London Corporate (UK)
   - `ALG` = Algiers Yard (Algeria)
   - `HOU` = Houston Operations (USA)
3. **YY**: Two-digit calendar year (e.g., `26` for 2026).
4. **XXXX**: 4-digit sequential integer zero-padded starting from `0001` (e.g. `0001`, `0002`, `0145`).

*Example Serial Number*: `SAIT-DXB-260001`

---

## 13. Complete Data Models & Schemas

### 13.1 Visitor Record (`VisitorEntity` / Firestore Document)
```json
{
  "id": "c7a8b9e0-1234-4567-89ab-cdef01234567",
  "passNumber": "SAIT-DXB-260001",
  "fullName": "Alexander Wright",
  "countryCode": "+971",
  "mobileNumber": "501234567",
  "email": "alexander.wright@clientcorp.com",
  "companyName": "Offshore Engineering Ltd",
  "visitType": "Scheduled Meeting",
  "department": "Operations & Marine",
  "purpose": "Facility Audit & Inspection",
  "customPurpose": "",
  "personToMeet": "Capt. Tariq Mansoor",
  "branchName": "Dubai Headquarters (UAE)",
  "branchCode": "DXB",
  "status": "CHECKED_IN",
  "timestamp": 1772678400000,
  "checkInFormattedTime": "03 Nov 2026, 10:24 AM",
  "checkOutTime": null,
  "isSynced": true,
  "emailDispatched": true
}
```

### 13.2 Branch Configuration (`Branch` / Firestore Document)
```json
{
  "id": "branch_dubai_hq",
  "name": "Dubai Headquarters (UAE)",
  "shortCode": "DXB",
  "city": "Dubai",
  "country": "United Arab Emirates",
  "countryCode": "+971",
  "facilityLocation": "Plot 42, Jebel Ali Free Zone (JAFZA South), Dubai, UAE",
  "logoUri": "https://assets.saitgroup.com/logos/sait_white.png",
  "emergencyExitDrawingUri": "https://assets.saitgroup.com/safety/dxb_evac_plan.png",
  "safetyGuidelines": "Standard QHSE induction rules apply. Steel toe footwear required past yellow line.",
  "safetyContactName": "Eng. Tariq Mansoor (QHSE Lead)",
  "safetyHelpline": "+971 4 881 0000 / +971 50 123 4567",
  "defaultDepartments": [
    "PROJECTS",
    "QHSE & SAFETY",
    "OPERATIONS & MARINE",
    "MANAGEMENT",
    "FINANCE & ACCOUNTS",
    "PROCUREMENT & WAREHOUSE"
  ]
}
```

---

## 14. Google Stitch UI Screen-by-Screen Prompt Blueprints

When building new layouts in Google Stitch, use the following exact prompt blueprints for each visual screen:

### Prompt 1: Visitor Self-Service Kiosk (Touch Terminal)
> *"Design a modern industrial Android Kiosk registration screen for 'SAIT Group Visitor Pass'. Tablet landscape orientation (1280x800). The top app bar features a solid white rounded container holding the company logo, the branch title 'Dubai Headquarters (UAE)' without any kiosk text, and a lock icon in the far right that operates on a 3-second touch-and-hold gesture. Center a clean registration card with: Hero facility header and a prominent green button 'View Facility Safety Guidelines & Emergency Exit Drawing'. Form fields in outlined Material 3 style: Full Name (Word capitalized), aligned side-by-side row with Country Selector (+971 with UAE flag) and Mobile Number, mandatory Email ID with helper note 'Required for visitor pass & safety copy delivery', Company Name, Visit Type chips, Department dropdown, Purpose of Visit chips, and Person to Meet. At the bottom, a green safety induction agreement checkbox card and a large primary CTA button 'Register Visitor & Issue Pass'. Dark navy and sky cyan color palette on clean slate background."*

### Prompt 2: Digital Visitor Pass Credential Dialog
> *"Design an enterprise digital visitor badge dialog with a realistic lanyard cutout hole at the top. The badge card features a navy-to-cyan gradient header with a solid white company logo container, 'SAIT GROUP', and 'OFFICIAL VISITOR PASS'. Below the header: an emerald green 'ACTIVE ON PREMISES' status pill, visitor name 'Alexander Wright' in 22sp bold, company name 'Offshore Engineering Ltd', and a structured metadata table showing Pass ID 'SAIT-DXB-260001' in bold monospace, Email ID, Department, Purpose, Host Name, and Registration Timestamp. Center a white QR code container with monospace ID below it. Include a confirmation note that pass & safety briefing were emailed to visitor. At the bottom, show a live animated countdown bar 'Kiosk resets for next visitor in 5s' and a full-width button 'Next Visitor Registration'."*

### Prompt 3: Receptionist Front-Desk Console
> *"Design a front-desk receptionist console for SAIT Group. Bottom navigation with 3 tabs: Manual Entry, Visitor Logbook, and Safety. Top app bar includes solid white logo container, branch title, notification bell with unread badge, light/dark theme toggle, and an 'AdminPanelSettings' icon button to elevate to admin. In the Logbook tab, display a scoped header for Dubai Headquarters with 'Active on Site' (12) and 'Today's Total' (48) counters, a search bar, purpose filter chips, and an 'Export Branch Log (CSV)' button. Below, show expandable visitor cards displaying monospace Pass IDs, visitor details, status pills, phone dialer shortcut, email button, and safety induction verification badge."*

### Prompt 4: Enterprise Admin & Cloud Analytics Console
> *"Design an enterprise admin dashboard for SAIT Group Visitor Pass. 4-tab bottom navigation (Register, Logbook, Cloud & Admin, Safety). The Logbook tab features an executive KPI grid (Total Check-ins, Active Today, On Premises, Avg Duration), a 7-day visitor traffic Canvas trend chart, branch volume comparison bars, and multi-criteria filter chips. The Cloud & Admin tab features: Google Sign-In status with Cloud Sync badge, international branch manager with edit/delete buttons and photo pickers for company logos and evacuation drawings, custom Purpose of Visit chips manager, department email alert routing matrix, outgoing SMTP mail server configuration card with a live 'Run SMTP Test' button and success badge, Google Drive cloud backup card, and PIN change security settings."*

### Prompt 5: Facility Safety & Emergency Evacuation Screen
> *"Design an industrial QHSE Safety Information screen for SAIT Group. Header card with solid white logo container, facility address, and QHSE compliance badge. 24/7 Helpline card displaying duty contact person name and emergency phone numbers in bold text with NO call button. Safety guidelines card listing 5 mandatory rules with icons: Visible Visitor Badge, Mandatory PPE in Yards, Photography Restricted, Smoking Shelters Only, and Emergency Routes. Emergency exit card featuring a dark-slate blueprint vector architectural evacuation plan with Workshop Zone, Office Zone, green dashed escape routes, illuminated emergency exits, and Assembly Point 1."*
