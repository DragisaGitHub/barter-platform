import { Link } from "react-router-dom";
import {
  ArrowRight,
  FolderTree,
  KeyRound,
  Lock,
  Settings,
  ShieldCheck,
  Users,
  type LucideIcon,
} from "lucide-react";
import { Badge } from "../../components/ui/Badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../../components/ui/Card";
import { routePaths } from "@/routes/routePaths.ts";
import { AdminPageShell, AdminSurface } from "./components/AdminPageShell";

interface AdminModule {
  title: string;
  description: string;
  to: string;
  icon: LucideIcon;
  tone: string;
  status?: string;
}

const adminModules: AdminModule[] = [
  {
    title: "Users",
    description: "Review accounts, investigate access, and open individual user records.",
    to: routePaths.admin.users,
    icon: Users,
    tone: "bg-indigo-100 text-indigo-600 dark:bg-indigo-900/30 dark:text-indigo-300",
  },
  {
    title: "Roles",
    description: "Inspect platform role definitions and role assignments.",
    to: routePaths.admin.roles,
    icon: KeyRound,
    tone: "bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-300",
  },
  {
    title: "Permissions",
    description: "Audit permission coverage used across the authorization model.",
    to: routePaths.admin.permissions,
    icon: Lock,
    tone: "bg-violet-100 text-violet-600 dark:bg-violet-900/30 dark:text-violet-300",
  },
  {
    title: "System",
    description: "Check platform status and review operational configuration surfaces.",
    to: routePaths.admin.system,
    icon: Settings,
    tone: "bg-amber-100 text-amber-600 dark:bg-amber-900/30 dark:text-amber-300",
  },
  {
    title: "Categories",
    description: "Prepare for category management without enabling CRUD in this phase.",
    to: routePaths.admin.categories,
    icon: FolderTree,
    tone: "bg-slate-200 text-slate-700 dark:bg-slate-800 dark:text-slate-300",
    status: "Coming next",
  },
];

export function AdminDashboardPage() {
  return (
    <AdminPageShell
      title="Admin Control Panel"
      description="Platform operations, moderation, and configuration now live in a dedicated admin workspace separated from the regular marketplace experience."
      badges={
        <>
          <Badge variant="primary">Operations</Badge>
          <Badge>Separated workspace</Badge>
        </>
      }
      actions={
        <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600 dark:border-slate-800 dark:bg-slate-950 dark:text-slate-300">
          Use this control panel for access review, moderation workflows, and safe platform-level configuration.
        </div>
      }
    >
      <AdminSurface
        title="Operational modules"
        description="Open existing admin areas without mixing them into the marketplace dashboard or user navigation."
        contentClassName="grid gap-4 md:grid-cols-2 xl:grid-cols-3"
      >
        {adminModules.map((module) => {
          const content = (
            <Card className="flex h-full flex-col justify-between border-slate-200 bg-gradient-to-br from-white to-slate-50/70 transition-all hover:-translate-y-0.5 hover:shadow-md dark:border-slate-800 dark:from-slate-900 dark:to-slate-950">
              <CardHeader>
                <div className="mb-3 flex items-start justify-between gap-3">
                  <div className={`flex size-12 items-center justify-center rounded-xl ${module.tone}`}>
                    <module.icon className="size-5" />
                  </div>
                  {module.status ? <Badge variant="warning">{module.status}</Badge> : <Badge variant="success">Ready</Badge>}
                </div>
                <CardTitle>{module.title}</CardTitle>
                <CardDescription>{module.description}</CardDescription>
              </CardHeader>
              <CardContent className="mt-auto flex items-center justify-between pt-2 text-sm font-medium text-indigo-600 dark:text-indigo-300">
                <span>{module.status ? "Preview module" : "Open module"}</span>
                <ArrowRight className="size-4" />
              </CardContent>
            </Card>
          );

          return (
            <Link
              key={module.to}
              to={module.to}
              className="block h-full rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 dark:focus:ring-offset-slate-950"
            >
              {content}
            </Link>
          );
        })}
      </AdminSurface>

      <AdminSurface
        title="Foundation status"
        description="This phase focuses on admin separation, navigation clarity, and consistent control-panel UX without changing backend contracts."
        contentClassName="grid gap-4 lg:grid-cols-[minmax(0,1.2fr)_minmax(0,0.8fr)]"
      >
        <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5 dark:border-slate-800 dark:bg-slate-950">
          <div className="flex items-start gap-3">
            <div className="flex size-11 items-center justify-center rounded-xl bg-indigo-100 text-indigo-600 dark:bg-indigo-900/30 dark:text-indigo-300">
              <ShieldCheck className="size-5" />
            </div>
            <div>
              <h3 className="text-base font-semibold text-slate-900 dark:text-slate-100">
                Dedicated admin experience
              </h3>
              <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
                Admin routes now live inside the dedicated admin shell, while category management remains safely staged as a placeholder module for the next phase.
              </p>
            </div>
          </div>
        </div>

        <div className="grid gap-3">
          <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-950">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">Current scope</p>
            <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
              Users, roles, permissions, system health, and category placeholder coverage.
            </p>
          </div>
          <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-950">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">Preserved behavior</p>
            <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
              Existing admin pages stay in place, backend APIs stay untouched, and no unsupported CRUD flows were introduced.
            </p>
          </div>
        </div>
      </AdminSurface>
    </AdminPageShell>
  );
}
