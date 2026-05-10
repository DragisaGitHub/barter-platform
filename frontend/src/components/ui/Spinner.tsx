import { Loader2 } from "lucide-react";
import { cn } from "../../utils";

interface SpinnerProps {
  size?: "sm" | "md" | "lg";
  className?: string;
}

export function Spinner({ size = "md", className }: SpinnerProps) {
  return (
    <Loader2
      className={cn(
        "animate-spin text-indigo-600 dark:text-indigo-400",
        {
          "size-4": size === "sm",
          "size-8": size === "md",
          "size-12": size === "lg",
        },
        className
      )}
    />
  );
}

export function LoadingScreen() {
  return (
    <div className="flex items-center justify-center min-h-screen bg-slate-50 dark:bg-slate-900">
      <div className="text-center">
        <Spinner size="lg" />
        <p className="mt-4 text-sm text-slate-600 dark:text-slate-400">Loading...</p>
      </div>
    </div>
  );
}
