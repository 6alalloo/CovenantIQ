import * as React from "react";
import { cn } from "../../lib/utils";

export function Badge({ className, ...props }: React.HTMLAttributes<HTMLSpanElement>) {
  const content = typeof props.children === "string" ? props.children : "";
  const normalized = content.toUpperCase().replace(/\s+/g, "_");
  const semanticClass =
    normalized === "HIGH" || normalized === "BREACH" || normalized === "OPEN"
      ? "border-[color:rgb(217_83_79_/_0.5)] bg-[color:rgb(217_83_79_/_0.13)] text-[var(--risk-high)]"
      : normalized === "MEDIUM" || normalized === "UNDER_REVIEW" || normalized === "ACKNOWLEDGED"
        ? "border-[color:rgb(217_164_65_/_0.5)] bg-[color:rgb(217_164_65_/_0.13)] text-[var(--risk-medium)]"
        : normalized === "LOW" || normalized === "RESOLVED" || normalized === "PASS"
          ? "border-[color:rgb(62_167_107_/_0.45)] bg-[color:rgb(62_167_107_/_0.14)] text-[var(--risk-low)]"
          : "";

  return <span className={cn("chip", semanticClass, className)} {...props} />;
}
