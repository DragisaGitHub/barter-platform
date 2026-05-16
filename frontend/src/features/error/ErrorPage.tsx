import { AlertTriangle, LayoutDashboard, LogIn, RefreshCw, Store, UserPlus } from "lucide-react";
import { Link, isRouteErrorResponse, useNavigate, useRouteError } from "react-router-dom";
import { useAuth } from "../../auth/AuthContext";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "../../components/ui/Card";
import { Button } from "../../components/ui/Button";
import { routePaths } from "@/routes/routePaths.ts";
import { useTranslation } from "react-i18next";

interface ErrorPageProps {
  description?: string;
}

const DEFAULT_DESCRIPTION_KEY = "errors:generic.description";

export function ErrorPage({ description }: ErrorPageProps) {
  const navigate = useNavigate();
  const { isAuthenticated, isLoading } = useAuth();
  const { t } = useTranslation(["errors", "common"]);

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 dark:bg-slate-900 dark:text-slate-100">
      <header className="sticky top-0 z-30 border-b border-slate-200/80 bg-white/95 backdrop-blur-sm dark:border-slate-800 dark:bg-slate-950/90">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-4 sm:px-6 lg:px-8">
          <Link to={routePaths.marketplace} className="flex items-center gap-3">
            <div className="flex size-9 items-center justify-center rounded-xl bg-violet-500 shadow-sm shadow-violet-500/30">
              <span className="text-base font-semibold text-white">⇄</span>
            </div>
            <div>
              <div className="text-sm font-semibold tracking-tight text-slate-900 dark:text-slate-100">
                {t("common:appName")}
              </div>
              <div className="text-xs text-slate-500 dark:text-slate-400">{t("common:marketplace")}</div>
            </div>
          </Link>

          <nav className="flex flex-wrap items-center justify-end gap-2 sm:gap-3">
            <Link to={routePaths.marketplace} className="inline-flex">
              <Button variant="ghost" size="sm" type="button">
                <Store className="size-4" />
                {t("common:marketplace")}
              </Button>
            </Link>

            {!isLoading && isAuthenticated && (
              <Link to={routePaths.dashboard} className="inline-flex">
                <Button variant="outline" size="sm" type="button">
                  <LayoutDashboard className="size-4" />
                  {t("common:dashboard")}
                </Button>
              </Link>
            )}

            {!isLoading && !isAuthenticated && (
              <>
                <Link to={routePaths.login} className="inline-flex">
                  <Button variant="ghost" size="sm" type="button">
                    <LogIn className="size-4" />
                    {t("common:login")}
                  </Button>
                </Link>
                <Link to={routePaths.register} className="inline-flex">
                  <Button size="sm" type="button">
                    <UserPlus className="size-4" />
                    {t("common:signUp")}
                  </Button>
                </Link>
              </>
            )}
          </nav>
        </div>
      </header>

      <main className="px-4 py-12 sm:px-6 sm:py-16 lg:px-8">
        <div className="mx-auto max-w-3xl">
          <Card className="overflow-hidden border-slate-200/80 bg-white shadow-xl shadow-slate-200/40 dark:border-slate-700/80 dark:bg-slate-800 dark:shadow-none">
            <div className="h-1.5 w-full bg-gradient-to-r from-violet-500 via-violet-400 to-indigo-400" />

            <CardHeader className="px-6 pb-4 pt-8 text-center sm:px-10 sm:pt-10">
              <div className="mx-auto mb-5 flex size-16 items-center justify-center rounded-2xl bg-violet-50 text-violet-600 ring-1 ring-violet-100 dark:bg-violet-500/15 dark:text-violet-300 dark:ring-violet-500/20">
                <AlertTriangle className="size-8" />
              </div>

              <CardTitle className="text-3xl font-bold tracking-tight text-slate-900 dark:text-slate-100 sm:text-4xl">
                {t("errors:generic.title")}
              </CardTitle>

              <CardDescription className="mx-auto mt-4 max-w-2xl text-base leading-7 text-slate-600 dark:text-slate-300">
                {description ?? t(DEFAULT_DESCRIPTION_KEY)}
              </CardDescription>
            </CardHeader>

            <CardContent className="px-6 pb-8 sm:px-10 sm:pb-10">
              <div className="rounded-2xl border border-slate-200 bg-slate-50/80 p-4 text-sm leading-6 text-slate-600 dark:border-slate-700 dark:bg-slate-900/40 dark:text-slate-300">
                {t("errors:generic.helper")}
              </div>

              <div className="mt-6 flex flex-col justify-center gap-3 sm:flex-row">
                <Button
                  type="button"
                  size="lg"
                  className="sm:min-w-56"
                  onClick={() => navigate(routePaths.marketplace)}
                >
                  <Store className="size-4" />
                  {t("common:backToMarketplace")}
                </Button>

                <Button
                  type="button"
                  variant="outline"
                  size="lg"
                  className="sm:min-w-44"
                  onClick={() => window.location.reload()}
                >
                  <RefreshCw className="size-4" />
                  {t("common:tryAgain")}
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  );
}

export function RouteErrorPage() {
  const error = useRouteError();

  const { t } = useTranslation("errors");
  let description = t("generic.description");

  if (isRouteErrorResponse(error) && error.status === 404) {
    description = t("generic.notFoundDescription");
  }

  return <ErrorPage description={description} />;
}

