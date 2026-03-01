import { cn } from "../lib/utils";

export function BrandLogo({
  className,
  size = "md",
  withText = true,
}: {
  className?: string;
  size?: "sm" | "md" | "lg";
  withText?: boolean;
}) {
  const iconSize = size === "sm" ? "h-8 w-8" : size === "lg" ? "h-12 w-12" : "h-10 w-10";
  const textSize = size === "sm" ? "text-xl" : size === "lg" ? "text-5xl" : "text-3xl";

  return (
    <div className={cn("flex items-center gap-3", className)}>
      <span
        className={cn(
          "grid place-items-center rounded-md border border-[var(--border-default)] bg-[linear-gradient(145deg,var(--bg-surface-3),var(--bg-surface-2))]",
          iconSize
        )}
      >
        <ShieldMark className={size === "lg" ? "h-8 w-8" : size === "sm" ? "h-5 w-5" : "h-7 w-7"} />
      </span>
      {withText ? (
        <span className={cn("font-semibold tracking-[-0.02em] text-[var(--text-primary)]", textSize)}>CovenantIQ</span>
      ) : null}
    </div>
  );
}

function ShieldMark({ className }: { className?: string }) {
  return (
    <svg className={cn("text-[var(--text-primary)]", className)} viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M12 2.75c2.2 1.93 5.2 3.1 8.34 3.26v5.47c0 5.24-3.2 8.83-8.34 9.77-5.14-.94-8.34-4.53-8.34-9.77V6.01C6.8 5.85 9.8 4.68 12 2.75Z"
        stroke="currentColor"
        strokeWidth="1.6"
      />
      <path
        d="M12 6.2c1.53 1.22 3.43 2 5.47 2.2v3.2c0 3.5-2 5.9-5.47 6.66-3.47-.75-5.47-3.16-5.47-6.65V8.4c2.04-.2 3.94-.98 5.47-2.2Z"
        fill="currentColor"
        fillOpacity="0.15"
      />
    </svg>
  );
}
