import type { ReactNode } from "react";

export function PageSection({
  title,
  subtitle,
  action,
  children,
}: {
  title: string;
  subtitle?: string;
  action?: ReactNode;
  children: ReactNode;
}) {
  return (
    <section className="page-enter space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-[25px] leading-tight">{title}</h1>
          {subtitle ? <p className="mt-1 text-sm text-[var(--text-secondary)]">{subtitle}</p> : null}
        </div>
        {action}
      </div>
      {children}
    </section>
  );
}

export function StatCard({
  label,
  value,
  tone,
}: {
  label: string;
  value: string | number;
  tone?: "HIGH" | "MEDIUM" | "LOW";
}) {
  return (
    <article className="card p-5">
      <p className="text-[11px] uppercase tracking-[0.08em] text-[var(--text-secondary)]">{label}</p>
      <p
        className={`font-numeric mt-2 text-3xl font-semibold leading-none ${
          tone ? "" : "metric-glow text-[var(--accent-interactive)]"
        }`}
        data-risk={tone}
      >
        {value}
      </p>
    </article>
  );
}

export function Surface({ children, className = "" }: { children: ReactNode; className?: string }) {
  return <div className={`card ${className}`}>{children}</div>;
}
