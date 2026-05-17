import { type ButtonHTMLAttributes, type ReactNode } from "react";
import { cn } from "@/utils";
import { Loader2 } from "lucide-react";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  children: ReactNode;
  variant?: "primary" | "secondary" | "outline" | "ghost" | "danger";
  size?: "sm" | "md" | "lg";
  isLoading?: boolean;
  fullWidth?: boolean;
}

export function Button({
  children,
  variant = "primary",
  size = "md",
  isLoading = false,
  fullWidth = false,
  className,
  disabled,
  ...props
}: ButtonProps) {
  return (
    <button
      className={cn(
        "inline-flex items-center justify-center gap-1.5 rounded-lg font-medium transition-colors duration-150 focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50",
        {
          "bg-indigo-600 text-white hover:bg-indigo-700 focus:ring-indigo-500":
            variant === "primary",
          "bg-slate-200 text-slate-900 hover:bg-slate-300 focus:ring-slate-400 dark:bg-slate-700 dark:text-slate-100 dark:hover:bg-slate-600":
            variant === "secondary",
          "border border-slate-300 bg-white text-slate-700 hover:bg-slate-50 focus:ring-slate-400 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700":
            variant === "outline",
          "text-slate-700 hover:bg-slate-100 focus:ring-slate-400 dark:text-slate-300 dark:hover:bg-slate-800":
            variant === "ghost",
          "bg-red-600 text-white hover:bg-red-700 focus:ring-red-500":
            variant === "danger",
          "px-2.5 py-1.5 text-sm": size === "sm",
          "px-3.5 py-2 text-sm": size === "md",
          "px-5 py-2.5 text-sm": size === "lg",
          "w-full": fullWidth,
        },
        className
      )}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading && <Loader2 className="size-4 animate-spin" />}
      {children}
    </button>
  );
}
