import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "./components/AppShell";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { AdminUsersPage } from "./pages/AdminUsersPage";
import { AlertsPage } from "./pages/AlertsPage";
import { DashboardPage } from "./pages/DashboardPage";
import { ForbiddenPage } from "./pages/ForbiddenPage";
import { LoanActivityPage } from "./pages/LoanActivityPage";
import { LoanAlertsPage } from "./pages/LoanAlertsPage";
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
          <Route path="documents" element={<LoanDocumentsPage />} />
          <Route path="comments" element={<LoanCommentsPage />} />
          <Route path="activity" element={<LoanActivityPage />} />
        </Route>
        <Route path="alerts" element={<AlertsPage />} />
        <Route path="reports" element={<ReportsPage />} />
        <Route
          path="admin/users"
          element={
            <ProtectedRoute allowRoles={["ADMIN"]}>
              <AdminUsersPage />
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
