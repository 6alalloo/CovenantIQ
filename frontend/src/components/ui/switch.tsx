import * as React from "react";
import { cn } from "../../lib/utils";

type SwitchProps = {
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  className?: string;
};

export function Switch({ checked, onCheckedChange, className }: SwitchProps) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      className={cn("switch", className)}
      data-on={checked}
      onClick={() => onCheckedChange(!checked)}
    />
  );
}
