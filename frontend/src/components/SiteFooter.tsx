import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { routePaths } from "@/routes/routePaths";

export function SiteFooter() {
  const { t } = useTranslation("legal");

  return (
    <footer className="border-t border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-950">
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <nav aria-label={t("footer.ariaLabel")} className="flex flex-wrap gap-x-6 gap-y-2">
          <Link
            to={routePaths.legal.terms}
            className="text-sm text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-200"
          >
            {t("nav.termsOfService")}
          </Link>
          <Link
            to={routePaths.legal.privacy}
            className="text-sm text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-200"
          >
            {t("nav.privacyPolicy")}
          </Link>
          <Link
            to={routePaths.legal.communityGuidelines}
            className="text-sm text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-200"
          >
            {t("nav.communityGuidelines")}
          </Link>
          <Link
            to={routePaths.legal.prohibitedItems}
            className="text-sm text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-200"
          >
            {t("nav.prohibitedItems")}
          </Link>
          <Link
            to={routePaths.legal.safetyTips}
            className="text-sm text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-200"
          >
            {t("nav.safetyTips")}
          </Link>
        </nav>
        <p className="mt-4 text-xs text-slate-500 dark:text-slate-500">
          © {new Date().getFullYear()} Barter Platform
        </p>
      </div>
    </footer>
  );
}

