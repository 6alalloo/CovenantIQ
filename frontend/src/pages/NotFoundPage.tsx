import { Link } from "react-router-dom";
import { BrandLogo } from "../components/BrandLogo";

export function NotFoundPage() {
  return (
    <div className="grid min-h-screen place-items-center p-6">
      <div className="card w-full max-w-lg p-9 text-center">
        <div className="mb-6 flex justify-center">
          <BrandLogo size="sm" />
        </div>
        <p className="font-numeric text-xs uppercase tracking-[0.16em] text-[var(--text-secondary)]">404</p>
        <h1 className="mt-2 text-3xl font-semibold">Route Not Found</h1>
        <Link to="/app/dashboard" className="btn-primary mt-6 inline-block">
          Open Dashboard
        </Link>
      </div>
    </div>
  );
}
