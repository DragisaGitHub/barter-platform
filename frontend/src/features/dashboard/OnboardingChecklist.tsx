import { CheckCircle2, Circle, ExternalLink, MessageSquareQuote, Store } from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/Button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";

export interface OnboardingChecklistItem {
  key: string;
  title: string;
  description: string;
  completed: boolean;
  href?: string;
  ctaLabel?: string;
}

interface OnboardingChecklistProps {
  title: string;
  description: string;
  completedLabel: string;
  items: OnboardingChecklistItem[];
  feedbackTitle: string;
  feedbackDescription: string;
  feedbackCta: string;
  feedbackHref: string;
}

export function OnboardingChecklist({
  title,
  description,
  completedLabel,
  items,
  feedbackTitle,
  feedbackDescription,
  feedbackCta,
  feedbackHref,
}: OnboardingChecklistProps) {
  const completedCount = items.filter((item) => item.completed).length;
  const percent = items.length === 0 ? 0 : Math.round((completedCount / items.length) * 100);

  return (
    <Card className="mb-8 overflow-hidden rounded-3xl border-violet-200/80 bg-white shadow-sm dark:border-violet-900/40 dark:bg-slate-900">
      <CardHeader className="mb-0 border-b border-slate-200/80 bg-linear-to-r from-violet-500/8 via-white to-sky-500/8 px-6 py-5 dark:border-slate-800 dark:from-violet-500/10 dark:via-slate-900 dark:to-sky-500/10">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div className="max-w-2xl">
            <div className="mb-2 inline-flex items-center rounded-full border border-violet-200 bg-violet-50 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-violet-700 dark:border-violet-900/60 dark:bg-violet-950/30 dark:text-violet-300">
              {completedLabel}
            </div>
            <CardTitle className="text-xl sm:text-2xl">{title}</CardTitle>
            <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-400">{description}</p>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white/90 px-4 py-3 shadow-sm dark:border-slate-800 dark:bg-slate-950/70">
            <div className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500 dark:text-slate-400">
              {completedCount}/{items.length}
            </div>
            <div className="mt-2 h-2.5 w-48 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-800">
              <div className="h-full rounded-full bg-violet-500 transition-all" style={{ width: `${percent}%` }} />
            </div>
            <div className="mt-2 text-sm font-medium text-slate-700 dark:text-slate-300">{percent}%</div>
          </div>
        </div>
      </CardHeader>

      <CardContent className="grid gap-6 px-6 py-6 lg:grid-cols-[minmax(0,1fr)_19rem]">
        <div className="space-y-3">
          {items.map((item) => (
            <div
              key={item.key}
              className="flex flex-col gap-3 rounded-2xl border border-slate-200/90 bg-slate-50/70 p-4 dark:border-slate-800 dark:bg-slate-950/40 sm:flex-row sm:items-start sm:justify-between"
            >
              <div className="flex gap-3">
                <div className="mt-0.5 shrink-0 text-violet-600 dark:text-violet-300">
                  {item.completed ? <CheckCircle2 className="size-5" /> : <Circle className="size-5" />}
                </div>
                <div>
                  <p className="text-sm font-semibold text-slate-900 dark:text-slate-100">{item.title}</p>
                  <p className="mt-1 text-sm leading-6 text-slate-600 dark:text-slate-400">{item.description}</p>
                </div>
              </div>
              {item.href && item.ctaLabel ? (
                <Link to={item.href} className="shrink-0">
                  <Button variant={item.completed ? "outline" : "primary"} size="sm">
                    {item.ctaLabel}
                    {!item.completed ? <ExternalLink className="size-4" /> : null}
                  </Button>
                </Link>
              ) : null}
            </div>
          ))}
        </div>

        <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-950/60">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-violet-100 text-violet-600 dark:bg-violet-950/40 dark:text-violet-300">
            <MessageSquareQuote className="size-6" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-slate-900 dark:text-slate-100">{feedbackTitle}</h3>
          <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-400">{feedbackDescription}</p>
          <Link to={feedbackHref} className="mt-4 inline-flex">
            <Button size="sm">
              {feedbackCta}
              <Store className="size-4" />
            </Button>
          </Link>
        </div>
      </CardContent>
    </Card>
  );
}

