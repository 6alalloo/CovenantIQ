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
          "inline-flex items-center justify-center gap-2 rounded-sm px-4 py-2 text-sm font-medium transition disabled:pointer-events-none disabled:opacity-50",
          variant === "default" && "btn-primary",
          variant === "outline" && "btn-secondary",
          variant === "ghost" &&
            "border border-transparent bg-transparent text-[var(--text-secondary)] hover:border-[var(--border-default)] hover:text-[var(--text-primary)]",
          className
        )}
        {...props}
      />
    );
  }
);

Button.displayName = "Button";
