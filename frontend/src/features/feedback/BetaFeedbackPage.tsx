import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { FormProvider, useForm } from "react-hook-form";
import { z } from "zod";
import { Lightbulb, MessageSquareQuote, ShieldAlert } from "lucide-react";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";
import { useAuth } from "@/auth/AuthContext";
import { submitBetaFeedback } from "@/api/feedbackApi";
import type { BetaFeedbackCategory, BetaFeedbackRequest } from "@/api/generated/types";
import { Button } from "@/components/ui/Button";
import { Card, CardContent } from "@/components/ui/Card";
import { routePaths } from "@/routes/routePaths";
import { markBetaFeedbackSubmitted } from "@/features/onboarding/onboardingState";

type BetaFeedbackFormData = Pick<BetaFeedbackRequest, "category" | "message">;

const FEEDBACK_CATEGORIES: readonly BetaFeedbackCategory[] = [
  "ONBOARDING",
  "LISTINGS",
  "MARKETPLACE",
  "OFFERS",
  "TRUST_AND_SAFETY",
  "GENERAL",
] as const;

export function BetaFeedbackPage() {
  const { t } = useTranslation(["feedback", "common"]);
  const { user } = useAuth();
  const location = useLocation();
  const [submitted, setSubmitted] = useState(false);

  const schema = z.object({
    category: z.string().min(1, t("feedback:validation.categoryRequired")),
    message: z
      .string()
      .trim()
      .min(20, t("feedback:validation.messageMin"))
      .max(2000, t("feedback:validation.messageMax")),
  });

  const methods = useForm<BetaFeedbackFormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      category: "ONBOARDING",
      message: "",
    },
  });

  const mutation = useMutation({
    mutationFn: submitBetaFeedback,
    onSuccess: () => {
      markBetaFeedbackSubmitted(user?.uuid);
      setSubmitted(true);
      methods.reset({ category: "ONBOARDING", message: "" });
      toast.success(t("feedback:success.toast"));
    },
    onError: () => {
      toast.error(t("feedback:error.toast"));
    },
  });

  const onSubmit = methods.handleSubmit((values) => {
    mutation.mutate({
      category: values.category,
      message: values.message.trim(),
      sourcePage: location.pathname,
    });
  });

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <section className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-800">
        <div className="bg-linear-to-r from-violet-500/10 via-slate-100 to-sky-500/10 px-5 py-6 dark:from-violet-500/10 dark:via-slate-800 dark:to-sky-500/10 sm:px-6 lg:px-8">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
            <div className="max-w-2xl">
              <div className="inline-flex items-center rounded-full border border-violet-200 bg-violet-50 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-violet-700 dark:border-violet-900/60 dark:bg-violet-950/30 dark:text-violet-300">
                {t("feedback:badge")}
              </div>
              <h1 className="mt-4 text-3xl font-bold tracking-tight text-slate-900 dark:text-white sm:text-4xl">
                {t("feedback:title")}
              </h1>
              <p className="mt-3 text-sm leading-6 text-slate-600 dark:text-slate-300 sm:text-base">
                {t("feedback:subtitle")}
              </p>
            </div>

            <div className="grid gap-3 sm:grid-cols-2 lg:w-[24rem] lg:grid-cols-1">
              <div className="rounded-2xl border border-white/60 bg-white/80 p-4 shadow-sm backdrop-blur dark:border-slate-700/80 dark:bg-slate-900/50">
                <div className="flex items-start gap-3">
                  <div className="flex size-10 items-center justify-center rounded-2xl bg-indigo-100 text-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-300">
                    <Lightbulb className="size-5" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                      {t("feedback:tips.context.title")}
                    </p>
                    <p className="mt-1 text-xs leading-5 text-slate-500 dark:text-slate-400">
                      {t("feedback:tips.context.description")}
                    </p>
                  </div>
                </div>
              </div>
              <div className="rounded-2xl border border-white/60 bg-white/80 p-4 shadow-sm backdrop-blur dark:border-slate-700/80 dark:bg-slate-900/50">
                <div className="flex items-start gap-3">
                  <div className="flex size-10 items-center justify-center rounded-2xl bg-amber-100 text-amber-600 dark:bg-amber-950/40 dark:text-amber-300">
                    <ShieldAlert className="size-5" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                      {t("feedback:tips.reporting.title")}
                    </p>
                    <p className="mt-1 text-xs leading-5 text-slate-500 dark:text-slate-400">
                      {t("feedback:tips.reporting.description")}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <Card className="rounded-3xl p-0 shadow-sm">
        <CardContent className="p-5 sm:p-6 lg:p-8">
          {submitted ? (
            <div className="space-y-5 text-center sm:text-left">
              <div className="inline-flex size-14 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-600 dark:bg-emerald-950/30 dark:text-emerald-300">
                <MessageSquareQuote className="size-7" />
              </div>
              <div>
                <h2 className="text-xl font-semibold text-slate-900 dark:text-slate-100">
                  {t("feedback:success.title")}
                </h2>
                <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-400">
                  {t("feedback:success.description")}
                </p>
              </div>
              <div className="flex flex-wrap gap-3">
                <Button type="button" onClick={() => setSubmitted(false)}>
                  {t("feedback:success.sendAnother")}
                </Button>
                <Link to={routePaths.dashboard}>
                  <Button type="button" variant="outline">
                    {t("feedback:success.backToDashboard")}
                  </Button>
                </Link>
              </div>
            </div>
          ) : (
            <FormProvider {...methods}>
              <form onSubmit={onSubmit} className="space-y-6">
                <div className="rounded-2xl border border-slate-200 bg-slate-50/80 p-4 text-sm leading-6 text-slate-600 dark:border-slate-700 dark:bg-slate-900/50 dark:text-slate-300">
                  {t("feedback:callout")}
                </div>

                <div>
                  <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">
                    {t("feedback:fields.category")}
                  </label>
                  <select
                    {...methods.register("category")}
                    className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
                  >
                    {FEEDBACK_CATEGORIES.map((category) => (
                      <option key={category} value={category}>
                        {t(`feedback:categories.${category}`)}
                      </option>
                    ))}
                  </select>
                  {methods.formState.errors.category?.message ? (
                    <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">
                      {methods.formState.errors.category.message}
                    </p>
                  ) : null}
                </div>

                <div>
                  <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">
                    {t("feedback:fields.message")}
                  </label>
                  <textarea
                    {...methods.register("message")}
                    rows={8}
                    placeholder={t("feedback:fields.messagePlaceholder")}
                    className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 transition-colors duration-150 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:border-indigo-400 dark:focus:ring-indigo-400/20"
                  />
                  {methods.formState.errors.message?.message ? (
                    <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">
                      {methods.formState.errors.message.message}
                    </p>
                  ) : null}
                </div>

                <div className="rounded-2xl border border-dashed border-slate-300 px-4 py-3 text-xs leading-5 text-slate-500 dark:border-slate-700 dark:text-slate-400">
                  {t("feedback:helper")}
                </div>

                <div className="flex flex-wrap items-center gap-3">
                  <Button type="submit" isLoading={mutation.isPending}>
                    {t("feedback:submit")}
                  </Button>
                  <Link to={routePaths.dashboard}>
                    <Button type="button" variant="outline">
                      {t("common:cancel")}
                    </Button>
                  </Link>
                </div>
              </form>
            </FormProvider>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

