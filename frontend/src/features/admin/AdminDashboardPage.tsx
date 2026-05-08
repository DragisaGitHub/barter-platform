import { Link } from "react-router-dom";
import { Users, KeyRound, Lock, Settings } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/Card";

export function AdminDashboardPage() {
  return (
    <div className="max-w-7xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-900 dark:text-white">
          Admin Dashboard
        </h1>
        <p className="text-slate-600 dark:text-slate-400 mt-2">
          Manage users, roles, permissions, and system settings
        </p>
      </div>

      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
        <Link to="/admin/users">
          <Card className="hover:shadow-lg transition-shadow cursor-pointer h-full">
            <CardHeader>
              <div className="bg-indigo-100 dark:bg-indigo-900/30 rounded-lg p-3 w-fit mb-2">
                <Users className="size-6 text-indigo-600 dark:text-indigo-400" />
              </div>
              <CardTitle>Users</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-slate-600 dark:text-slate-400">
                Manage user accounts and permissions
              </p>
            </CardContent>
          </Card>
        </Link>

        <Link to="/admin/roles">
          <Card className="hover:shadow-lg transition-shadow cursor-pointer h-full">
            <CardHeader>
              <div className="bg-emerald-100 dark:bg-emerald-900/30 rounded-lg p-3 w-fit mb-2">
                <KeyRound className="size-6 text-emerald-600 dark:text-emerald-400" />
              </div>
              <CardTitle>Roles</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-slate-600 dark:text-slate-400">
                View and manage user roles
              </p>
            </CardContent>
          </Card>
        </Link>

        <Link to="/admin/permissions">
          <Card className="hover:shadow-lg transition-shadow cursor-pointer h-full">
            <CardHeader>
              <div className="bg-violet-100 dark:bg-violet-900/30 rounded-lg p-3 w-fit mb-2">
                <Lock className="size-6 text-violet-600 dark:text-violet-400" />
              </div>
              <CardTitle>Permissions</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-slate-600 dark:text-slate-400">
                View system permissions
              </p>
            </CardContent>
          </Card>
        </Link>

        <Link to="/admin/system">
          <Card className="hover:shadow-lg transition-shadow cursor-pointer h-full">
            <CardHeader>
              <div className="bg-amber-100 dark:bg-amber-900/30 rounded-lg p-3 w-fit mb-2">
                <Settings className="size-6 text-amber-600 dark:text-amber-400" />
              </div>
              <CardTitle>System</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-slate-600 dark:text-slate-400">
                System health and configuration
              </p>
            </CardContent>
          </Card>
        </Link>
      </div>
    </div>
  );
}
