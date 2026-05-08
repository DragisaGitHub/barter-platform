import { type ReactNode } from "react";
import { cn } from "../../utils";

interface BadgeProps {
  children: ReactNode;
  variant?: "default" | "primary" | "success" | "warning" | "danger" | "secondary";
  className?: string;
}

export function Badge({ children, variant = "default", className }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium",
        {
          "bg-slate-100 text-slate-700 dark:bg-slate-700 dark:text-slate-300":
            variant === "default",
          "bg-indigo-100 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-300":
            variant === "primary",
          "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300":
            variant === "success",
          "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300":
            variant === "warning",
          "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300":
            variant === "danger",
          "bg-violet-100 text-violet-700 dark:bg-violet-900/30 dark:text-violet-300":
            variant === "secondary",
        },
        className
      )}
    >
      {children}
    </span>
  );
}
