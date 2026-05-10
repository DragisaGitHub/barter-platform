import { Link } from "react-router-dom";
import { Store, Package, MessageSquare, Shield } from "lucide-react";
import { useAuth } from "../../auth/AuthContext";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/Card";
import { Button } from "../../components/ui/Button";

export function DashboardPage() {
  const { user, hasRole } = useAuth();
  const isAdmin = hasRole("ADMIN");

  return (
    <div className="max-w-7xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-900 dark:text-white">
          Welcome back, {user?.username}!
        </h1>
        <p className="text-slate-600 dark:text-slate-400 mt-2">
          Ready to start trading? Check out what's happening on your account.
        </p>
      </div>

      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3 mb-8">
        <Link to="/marketplace">
          <Card className="hover:shadow-lg transition-shadow cursor-pointer h-full">
            <CardHeader>
              <div className="bg-indigo-100 dark:bg-indigo-900/30 rounded-lg p-3 w-fit mb-2">
                <Store className="size-6 text-indigo-600 dark:text-indigo-400" />
              </div>
              <CardTitle>Browse Marketplace</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-slate-600 dark:text-slate-400">
                Discover items available for trade in your area
              </p>
            </CardContent>
          </Card>
        </Link>

        <Link to="/offers">
          <Card className="hover:shadow-lg transition-shadow cursor-pointer h-full">
            <CardHeader>
              <div className="bg-emerald-100 dark:bg-emerald-900/30 rounded-lg p-3 w-fit mb-2">
                <Package className="size-6 text-emerald-600 dark:text-emerald-400" />
              </div>
              <CardTitle>My Offers</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-slate-600 dark:text-slate-400">
                Manage your active listings and trade offers
              </p>
            </CardContent>
          </Card>
        </Link>

        <Link to="/messages">
          <Card className="hover:shadow-lg transition-shadow cursor-pointer h-full">
            <CardHeader>
              <div className="bg-violet-100 dark:bg-violet-900/30 rounded-lg p-3 w-fit mb-2">
                <MessageSquare className="size-6 text-violet-600 dark:text-violet-400" />
              </div>
              <CardTitle>Messages</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-slate-600 dark:text-slate-400">
                Chat with other traders about potential exchanges
              </p>
            </CardContent>
          </Card>
        </Link>
      </div>

      {isAdmin && (
        <Card className="border-indigo-200 dark:border-indigo-800">
          <CardHeader>
            <div className="flex items-center gap-3">
              <div className="bg-indigo-100 dark:bg-indigo-900/30 rounded-lg p-2">
                <Shield className="size-5 text-indigo-600 dark:text-indigo-400" />
              </div>
              <div>
                <CardTitle>Admin Access</CardTitle>
                <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
                  You have administrator privileges
                </p>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <Link to="/admin">
              <Button>Go to Admin Dashboard</Button>
            </Link>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
