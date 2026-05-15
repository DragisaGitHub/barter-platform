import { type ReactNode } from "react";
import { Badge } from "@/components/ui/Badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/Card";
import { cn } from "@/utils";

interface AdminPageShellProps {
  title: string;
  description: ReactNode;
  badges?: ReactNode;
  actions?: ReactNode;
  toolbar?: ReactNode;
  children: ReactNode;
  className?: string;
  contentClassName?: string;
}

export function AdminPageShell({
  title,
  description,
  badges,
  actions,
  toolbar,
  children,
  className,
  contentClassName,
}: AdminPageShellProps) {
  return (
    <div className={cn("mx-auto flex w-full max-w-7xl flex-col gap-6", className)}>
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
          <div className="min-w-0 flex-1">
            <div className="mb-3 flex flex-wrap items-center gap-2">
              {badges ?? (
                <>
                  <Badge variant="primary">Admin</Badge>
                  <Badge>Control panel</Badge>
                </>
              )}
            </div>
            <h1 className="text-3xl font-bold tracking-tight text-slate-950 dark:text-white">{title}</h1>
            <p className="mt-2 max-w-3xl text-sm text-slate-600 dark:text-slate-400 sm:text-base">
              {description}
            </p>
          </div>

          {actions && (
            <div className="flex w-full min-w-0 flex-wrap items-center gap-3 xl:w-auto xl:max-w-md xl:justify-end">
              {actions}
            </div>
          )}
        </div>
      </section>

      {toolbar}

      <div className={cn("flex flex-col gap-6", contentClassName)}>{children}</div>
    </div>
  );
}

interface AdminSurfaceProps {
  title?: ReactNode;
  description?: ReactNode;
  actions?: ReactNode;
  children: ReactNode;
  className?: string;
  contentClassName?: string;
}

export function AdminSurface({
  title,
  description,
  actions,
  children,
  className,
  contentClassName,
}: AdminSurfaceProps) {
  return (
    <Card className={cn("rounded-2xl border-slate-200 shadow-sm dark:border-slate-800 dark:bg-slate-900", className)}>
      {(title || description || actions) && (
        <CardHeader className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
          <div>
            {title && <CardTitle>{title}</CardTitle>}
            {description && <CardDescription>{description}</CardDescription>}
          </div>
          {actions && <div className="flex shrink-0 items-center gap-2">{actions}</div>}
        </CardHeader>
      )}
      <CardContent className={contentClassName}>{children}</CardContent>
    </Card>
  );
}

interface AdminToolbarProps {
  children: ReactNode;
  className?: string;
}

export function AdminToolbar({ children, className }: AdminToolbarProps) {
  return (
    <div
      className={cn(
        "flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm md:flex-row md:items-center md:justify-between dark:border-slate-800 dark:bg-slate-900",
        className
      )}
    >
      {children}
    </div>
  );
}

