import { Link } from "react-router-dom";
import { ArrowRight, Compass, Repeat, Shield, Store, Users } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button } from "../../components/ui/Button";
import { Card, CardContent } from "../../components/ui/Card";
import { routePaths } from "@/routes/routePaths";
import { SiteFooter } from "@/components/SiteFooter";

export function LandingPage() {
  const { t } = useTranslation(["landing", "common"]);

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950">
      <nav className="border-b border-slate-200/80 bg-white/95 backdrop-blur-sm dark:border-slate-800 dark:bg-slate-950/90">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <div>
            <h1 className="text-xl font-bold text-slate-900 dark:text-white">Barter Platform</h1>
          </div>
            <div className="flex items-center gap-3">
              <Link to={routePaths.marketplace}>
                <Button variant="ghost" size="sm">
                  {t("landing:nav.browseMarketplace")}
                </Button>
              </Link>
              <Link to={routePaths.login}>
                <Button variant="ghost" size="sm">
                  {t("landing:nav.signIn")}
                </Button>
              </Link>
              <Link to={routePaths.register}>
                <Button size="sm">{t("landing:nav.register")}</Button>
              </Link>
            </div>
          </div>
      </nav>

      <main className="pb-16">
        <section className="px-4 py-18 sm:px-6 lg:px-8 lg:py-24">
          <div className="mx-auto grid max-w-7xl gap-8 lg:grid-cols-[minmax(0,1.15fr)_24rem] lg:items-center">
            <div>
              <div className="inline-flex items-center rounded-full border border-violet-200 bg-violet-50 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-violet-700 dark:border-violet-900/60 dark:bg-violet-950/30 dark:text-violet-300">
                {t("landing:hero.eyebrow")}
              </div>
              <h2 className="mt-5 max-w-4xl text-4xl font-bold tracking-tight text-slate-900 dark:text-white lg:text-6xl">
                {t("landing:hero.title")}
              </h2>
              <p className="mt-5 max-w-3xl text-lg leading-8 text-slate-600 dark:text-slate-300">
                {t("landing:hero.description")}
              </p>
              <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:flex-wrap">
                <Link to={routePaths.register}>
                  <Button size="lg">
                    {t("landing:hero.primaryCta")}
                    <ArrowRight className="size-5" />
                  </Button>
                </Link>
                <Link to={routePaths.login}>
                  <Button variant="outline" size="lg">
                    {t("landing:hero.signInCta")}
                  </Button>
                </Link>
                <Link to={routePaths.marketplace}>
                  <Button variant="ghost" size="lg">
                    {t("landing:hero.secondaryCta")}
                  </Button>
                </Link>
              </div>
            </div>

            <Card className="rounded-3xl border-slate-200/90 shadow-xl shadow-slate-200/50 dark:border-slate-800 dark:bg-slate-900 dark:shadow-none">
              <CardContent className="space-y-5 pt-2">
                <div>
                  <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-100">
                    {t("landing:safety.title")}
                  </h3>
                </div>
                <ul className="space-y-3 text-sm leading-6 text-slate-600 dark:text-slate-300">
                  <li className="flex gap-3">
                    <Shield className="mt-0.5 size-4 shrink-0 text-emerald-500" />
                    <span>{t("landing:safety.items.publicPlace")}</span>
                  </li>
                  <li className="flex gap-3">
                    <Repeat className="mt-0.5 size-4 shrink-0 text-indigo-500" />
                    <span>{t("landing:safety.items.keepMessages")}</span>
                  </li>
                  <li className="flex gap-3">
                    <Compass className="mt-0.5 size-4 shrink-0 text-violet-500" />
                    <span>{t("landing:safety.items.protectPrivacy")}</span>
                  </li>
                </ul>
              </CardContent>
            </Card>
          </div>
        </section>

        <section className="px-4 py-6 sm:px-6 lg:px-8">
          <div className="mx-auto max-w-7xl rounded-3xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900 lg:p-8">
            <h3 className="text-2xl font-bold text-slate-900 dark:text-white">
              {t("landing:highlights.title")}
            </h3>
            <div className="mt-6 grid gap-4 lg:grid-cols-3">
              <Card className="rounded-2xl border-slate-200/90 dark:border-slate-800 dark:bg-slate-950/60">
                <CardContent className="pt-2">
                  <div className="mb-4 flex size-11 items-center justify-center rounded-2xl bg-indigo-100 text-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-300">
                    <Repeat className="size-5" />
                  </div>
                  <h4 className="text-lg font-semibold text-slate-900 dark:text-slate-100">
                    {t("landing:highlights.items.list.title")}
                  </h4>
                  <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">
                    {t("landing:highlights.items.list.description")}
                  </p>
                </CardContent>
              </Card>

              <Card className="rounded-2xl border-slate-200/90 dark:border-slate-800 dark:bg-slate-950/60">
                <CardContent className="pt-2">
                  <div className="mb-4 flex size-11 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-600 dark:bg-emerald-950/40 dark:text-emerald-300">
                    <Store className="size-5" />
                  </div>
                  <h4 className="text-lg font-semibold text-slate-900 dark:text-slate-100">
                    {t("landing:highlights.items.discover.title")}
                  </h4>
                  <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">
                    {t("landing:highlights.items.discover.description")}
                  </p>
                </CardContent>
              </Card>

              <Card className="rounded-2xl border-slate-200/90 dark:border-slate-800 dark:bg-slate-950/60">
                <CardContent className="pt-2">
                  <div className="mb-4 flex size-11 items-center justify-center rounded-2xl bg-violet-100 text-violet-600 dark:bg-violet-950/40 dark:text-violet-300">
                    <Users className="size-5" />
                  </div>
                  <h4 className="text-lg font-semibold text-slate-900 dark:text-slate-100">
                    {t("landing:highlights.items.offer.title")}
                  </h4>
                  <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">
                    {t("landing:highlights.items.offer.description")}
                  </p>
                </CardContent>
              </Card>
            </div>
          </div>
        </section>

        <section className="px-4 py-6 sm:px-6 lg:px-8">
          <div className="mx-auto max-w-7xl">
            <h3 className="text-2xl font-bold text-slate-900 dark:text-white">
              {t("landing:benefits.title")}
            </h3>
            <div className="mt-6 grid gap-4 md:grid-cols-3">
              <Card className="rounded-2xl">
                <CardContent className="pt-2">
                  <h4 className="text-lg font-semibold text-slate-900 dark:text-slate-100">
                    {t("landing:benefits.items.cashless.title")}
                  </h4>
                  <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">
                    {t("landing:benefits.items.cashless.description")}
                  </p>
                </CardContent>
              </Card>
              <Card className="rounded-2xl">
                <CardContent className="pt-2">
                  <h4 className="text-lg font-semibold text-slate-900 dark:text-slate-100">
                    {t("landing:benefits.items.local.title")}
                  </h4>
                  <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">
                    {t("landing:benefits.items.local.description")}
                  </p>
                </CardContent>
              </Card>
              <Card className="rounded-2xl">
                <CardContent className="pt-2">
                  <h4 className="text-lg font-semibold text-slate-900 dark:text-slate-100">
                    {t("landing:benefits.items.trust.title")}
                  </h4>
                  <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">
                    {t("landing:benefits.items.trust.description")}
                  </p>
                </CardContent>
              </Card>
            </div>
          </div>
        </section>

        <section className="px-4 pt-6 sm:px-6 lg:px-8">
          <div className="mx-auto max-w-7xl rounded-3xl border border-violet-200 bg-violet-50/80 p-6 dark:border-violet-900/50 dark:bg-violet-950/20 lg:p-8">
            <h3 className="text-2xl font-bold text-slate-900 dark:text-white">
              {t("landing:footer.title")}
            </h3>
            <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-600 dark:text-slate-300">
              {t("landing:footer.description")}
            </p>
            <div className="mt-6 flex flex-col gap-3 sm:flex-row">
              <Link to={routePaths.register}>
                <Button size="lg">
                  {t("landing:footer.register")}
                </Button>
              </Link>
              <Link to={routePaths.login}>
                <Button variant="outline" size="lg">
                  {t("landing:footer.signIn")}
                </Button>
              </Link>
            </div>
          </div>
        </section>
      </main>

      <SiteFooter />
    </div>
  );
}
