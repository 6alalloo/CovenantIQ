import * as React from "react";
import { cn } from "../../lib/utils";

export const Select = React.forwardRef<HTMLSelectElement, React.SelectHTMLAttributes<HTMLSelectElement>>(
  ({ className, children, ...props }, ref) => (
    <select ref={ref} className={cn("input pr-8", className)} {...props}>
      {children}
    </select>
  )
);

Select.displayName = "Select";
