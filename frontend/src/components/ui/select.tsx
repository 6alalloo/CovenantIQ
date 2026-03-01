import * as React from "react";
import { cn } from "../../lib/utils";

export const Select = React.forwardRef<HTMLSelectElement, React.SelectHTMLAttributes<HTMLSelectElement>>(
  ({ className, children, style, ...props }, ref) => (
    <select
      ref={ref}
      className={cn(
        "input appearance-none bg-[length:14px] bg-[right_0.75rem_center] bg-no-repeat pr-9",
        className
      )}
      style={{
        backgroundImage:
          "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath d='m2.2 4.1 3.8 3.8 3.8-3.8' fill='none' stroke='%239aa7ba' stroke-width='1.4' stroke-linecap='round'/%3E%3C/svg%3E\")",
        ...style,
      }}
      {...props}
    >
      {children}
    </select>
  )
);

Select.displayName = "Select";
