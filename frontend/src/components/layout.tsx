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
    <section className="page-enter">
      <div className="mb-3 flex items-end justify-between gap-3">
        <div>
          <h1 className="text-[23px] font-semibold tracking-[-0.01em]">{title}</h1>
          {subtitle ? <p className="mt-1 text-xs text-[var(--text-secondary)]">{subtitle}</p> : null}
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
    <article className="card p-4">
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
