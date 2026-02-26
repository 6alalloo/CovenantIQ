import { Link } from "react-router-dom";
import { BrandLogo } from "../components/BrandLogo";

export function ForbiddenPage() {
  return (
    <div className="grid min-h-screen place-items-center p-6">
      <div className="card w-full max-w-lg p-8 text-center">
        <div className="mb-6 flex justify-center">
          <BrandLogo size="sm" />
        </div>
        <p className="font-numeric text-xs uppercase tracking-[0.16em] text-[var(--text-secondary)]">403</p>
        <h1 className="mt-2 text-3xl font-semibold">Access Forbidden</h1>
        <p className="mt-3 text-sm text-[var(--text-secondary)]">
          Your current role does not have access to this route.
        </p>
        <Link to="/app/dashboard" className="btn-primary mt-6 inline-block">
          Back to Dashboard
        </Link>
      </div>
    </div>
  );
}
