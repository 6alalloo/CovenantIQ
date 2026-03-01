import * as React from "react";
import { cn } from "../../lib/utils";

type TabsContextValue = {
  value: string;
  onValueChange: (value: string) => void;
};

const TabsContext = React.createContext<TabsContextValue | null>(null);

export function Tabs({
  value,
  onValueChange,
  className,
  children,
}: {
  value: string;
  onValueChange: (value: string) => void;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <TabsContext.Provider value={{ value, onValueChange }}>
      <div className={cn(className)}>{children}</div>
    </TabsContext.Provider>
  );
}

export function TabsList({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={cn("flex flex-wrap gap-2 border-b border-[var(--border-default)] pb-3", className)} {...props} />
  );
}

export function TabsTrigger({
  value,
  className,
  children,
  onClick,
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & {
  value: string;
}) {
  const context = React.useContext(TabsContext);
  if (!context) return null;
  const active = context.value === value;
  return (
    <button
      type="button"
      className={cn("seg-btn", className)}
      data-active={active}
      onClick={(event) => {
        onClick?.(event);
        context.onValueChange(value);
      }}
      {...props}
    >
      {children}
    </button>
  );
}
