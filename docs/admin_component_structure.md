# Admin Dashboard Component Structure

This document outlines the modular React architecture for the HatchTracker Admin Panel.

## 1. Core Layout
- `App.tsx`: Auth State Provider & Router.
- `Layout/`: Sidebar, Navbar, and Breadcrumbs.
- `ProtectedRoute.tsx`: Route guard checking for `isAdmin` claim.

## 2. Feature Views
- **`DashboardHome/`**: Aggregated stats (Total Users, Sales this Month, Active Incubations).
- **`UserManagement/`**: 
    - `UserTable.tsx`: Searchable list of all users with quick-status (Tier, Role).
    - `UserDetailsDrawer.tsx`: Detailed view for granting tester access and modifying roles.
- **`FinancialAudit/`**:
    - `InvoiceList.tsx`: Global search for invoices.
    - `VatSummary.tsx`: Monthly/Quarterly VAT breakdown for EU compliance reporting.
- **`Moderation/`**:
    - `ModerationQueue.tsx`: List of user reports and flagged content.

## 3. Component Philosophy
- **`ActionModal.tsx`**: Reusable component for destructive actions (downgrades, bans) with built-in confirmation logic.
- **`StatCard.tsx`**: Consistent UI for displaying high-level usage analytics.
- **`ReadOnlyField.tsx`**: Standard field for displaying data that is not locally editable.
