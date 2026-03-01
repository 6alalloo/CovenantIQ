import * as React from "react";
import { cn } from "../../lib/utils";

type ButtonVariant = "default" | "outline" | "ghost";

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = "default", ...props }, ref) => {
    return (
      <button
        ref={ref}
        className={cn(
          "inline-flex items-center justify-center gap-2 rounded-md px-4 text-sm font-semibold transition-[background,border,color,filter] duration-150 disabled:pointer-events-none disabled:opacity-50",
          variant === "default" && "btn-primary",
          variant === "outline" && "btn-secondary",
          variant === "ghost" &&
            "h-10 rounded-md border border-transparent bg-transparent text-[var(--text-secondary)] hover:border-[var(--border-default)] hover:bg-[var(--bg-surface-2)] hover:text-[var(--text-primary)]",
          className
        )}
        {...props}
      />
    );
  }
);

Button.displayName = "Button";
