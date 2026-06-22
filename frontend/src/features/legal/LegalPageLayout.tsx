import { Link } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/Button";
import { routePaths } from "@/routes/routePaths";
import { SiteFooter } from "@/components/SiteFooter";

interface LegalPageLayoutProps {
  title: string;
  children: React.ReactNode;
}

export function LegalPageLayout({ title, children }: LegalPageLayoutProps) {
  const { t } = useTranslation("common");

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950">
      <nav className="border-b border-slate-200/80 bg-white/95 backdrop-blur-sm dark:border-slate-800 dark:bg-slate-950/90">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <Link to={routePaths.home} className="text-xl font-bold text-slate-900 dark:text-white">
            {t("appName")}
          </Link>
          <Link to={routePaths.home}>
            <Button variant="ghost" size="sm">
              <ArrowLeft className="size-4" />
              {t("goBack")}
            </Button>
          </Link>
        </div>
      </nav>

      <main className="mx-auto max-w-4xl px-4 py-12 sm:px-6 lg:px-8">
        <h1 className="text-3xl font-bold tracking-tight text-slate-900 dark:text-white sm:text-4xl">
          {title}
        </h1>
        <div className="mt-8 space-y-8">
          {children}
        </div>
      </main>

      <SiteFooter />
    </div>
  );
}

