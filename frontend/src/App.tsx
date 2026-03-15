import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "./components/AppShell";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { AdminLoanImportsPage } from "./pages/AdminLoanImportsPage";
import { AdminUsersPage } from "./pages/AdminUsersPage";
import { AlertsPage } from "./pages/AlertsPage";
import { ChangeControlPage } from "./pages/ChangeControlPage";
import { DashboardPage } from "./pages/DashboardPage";
import { ForbiddenPage } from "./pages/ForbiddenPage";
import { IntegrationsPage } from "./pages/IntegrationsPage";
import { LoanActivityPage } from "./pages/LoanActivityPage";
import { LoanAlertsPage } from "./pages/LoanAlertsPage";
import { LoanCollateralExceptionsPage } from "./pages/LoanCollateralExceptionsPage";
import { LoanCommentsPage } from "./pages/LoanCommentsPage";
import { LoanDetailLayoutPage } from "./pages/LoanDetailLayoutPage";
import { LoanDocumentsPage } from "./pages/LoanDocumentsPage";
import { LoanOverviewPage } from "./pages/LoanOverviewPage";
import { LoanResultsPage } from "./pages/LoanResultsPage";
import { LoanStatementsPage } from "./pages/LoanStatementsPage";
import { LoansPage } from "./pages/LoansPage";
import { LoginPage } from "./pages/LoginPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { PortfolioPage } from "./pages/PortfolioPage";
import { ReportsPage } from "./pages/ReportsPage";
import { SettingsPage } from "./pages/SettingsPage";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/forbidden" element={<ForbiddenPage />} />
      <Route
        path="/app"
        element={
          <ProtectedRoute>
            <AppShell />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/app/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route
          path="portfolio"
          element={
            <ProtectedRoute allowRoles={["RISK_LEAD", "ADMIN"]}>
              <PortfolioPage />
            </ProtectedRoute>
          }
        />
        <Route path="loans" element={<LoansPage />} />
        <Route path="loans/:loanId" element={<LoanDetailLayoutPage />}>
          <Route index element={<Navigate to="overview" replace />} />
          <Route path="overview" element={<LoanOverviewPage />} />
          <Route path="statements" element={<LoanStatementsPage />} />
          <Route path="results" element={<LoanResultsPage />} />
          <Route path="alerts" element={<LoanAlertsPage />} />
          <Route path="collateral" element={<LoanCollateralExceptionsPage />} />
          <Route path="documents" element={<LoanDocumentsPage />} />
          <Route path="comments" element={<LoanCommentsPage />} />
          <Route path="activity" element={<LoanActivityPage />} />
        </Route>
        <Route path="alerts" element={<AlertsPage />} />
        <Route
          path="integrations"
          element={
            <ProtectedRoute allowRoles={["ADMIN"]}>
              <IntegrationsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="change-control"
          element={
            <ProtectedRoute allowRoles={["RISK_LEAD", "ADMIN"]}>
              <ChangeControlPage />
            </ProtectedRoute>
          }
        />
        <Route path="reports" element={<ReportsPage />} />
        <Route
          path="admin/users"
          element={
            <ProtectedRoute allowRoles={["ADMIN"]}>
              <AdminUsersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/loan-imports"
          element={
            <ProtectedRoute allowRoles={["ADMIN"]}>
              <AdminLoanImportsPage />
            </ProtectedRoute>
          }
        />
        <Route path="settings" element={<SettingsPage />} />
      </Route>
      <Route path="/" element={<Navigate to="/app/dashboard" replace />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
