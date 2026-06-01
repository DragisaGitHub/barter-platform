import { useNavigate, Link } from "react-router-dom";
import { ArrowLeft, BadgeCheck, Sparkles, Store } from "lucide-react";
import { useCreateItem } from "./useCatalog";
import { ItemForm, type ItemFormValues } from "./ItemForm";
import { Card, CardContent } from "../../components/ui/Card";
import { Badge } from "../../components/ui/Badge";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";
import { buildTemplateMetadataRequest } from "./listingTemplates";

export function CreateItemPage() {
  const navigate = useNavigate();
  const createMutation = useCreateItem();
  const { t } = useTranslation(["catalog", "common"]);

  const handleSubmit = (data: ItemFormValues) => {
    createMutation.mutate(
      {
        title: data.title,
        description: data.description || undefined,
        exchangeCity: data.exchangeCity?.trim() || undefined,
        exchangeArea: data.exchangeArea?.trim() || undefined,
        exchangeLocation: data.exchangeLocation?.trim() || undefined,
        categoryUuid: data.categoryUuid,
        tagUuids: data.tagUuids?.length ? data.tagUuids : undefined,
        condition: data.condition,
        listingTemplateType: data.listingTemplateType,
        templateMetadata: buildTemplateMetadataRequest(data),
        listingMode: data.listingMode,
        entries:
          data.listingMode === "SINGLE"
            ? undefined
            : data.entries?.map((entry) => ({
                title: entry.title.trim(),
                description: entry.description?.trim() || undefined,
                quantity: entry.quantity,
              })),
        status: data.status,
      },
      {
        onSuccess: (item) => {
          toast.success(t("catalog:createItem.toast.success"));
          navigate(`/my-items/${item.uuid}/edit`);
        },
        onError: () => {
          toast.error(t("catalog:createItem.toast.error"));
        },
      }
    );
  };

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <Link
          to="/my-items"
          className="inline-flex items-center gap-1 text-sm text-slate-600 transition-colors hover:text-indigo-600 dark:text-slate-400 dark:hover:text-indigo-400"
        >
          <ArrowLeft className="size-4" />
          {t("catalog:myItems.backToMyItems")}
        </Link>
      </div>

      <section className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-800">
        <div className="bg-linear-to-r from-indigo-500/10 via-slate-100 to-emerald-500/10 px-5 py-6 dark:from-indigo-500/10 dark:via-slate-800 dark:to-emerald-500/10 sm:px-6 lg:px-8">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
            <div className="max-w-2xl">
              <div className="flex flex-wrap items-center gap-2">
                <Badge variant="primary">{t("catalog:createItem.badges.newListing")}</Badge>
                <Badge variant="default">{t("catalog:createItem.badges.sellerWorkflow")}</Badge>
              </div>
              <h1 className="mt-4 text-3xl font-bold tracking-tight text-slate-900 dark:text-white sm:text-4xl">
                {t("catalog:createItem.title")}
              </h1>
              <p className="mt-3 text-sm leading-6 text-slate-600 dark:text-slate-300 sm:text-base">
                {t("catalog:createItem.subtitle")}
              </p>
            </div>

            <div className="grid gap-3 sm:grid-cols-3 lg:w-md lg:grid-cols-1">
              <div className="rounded-2xl border border-white/60 bg-white/80 p-4 shadow-sm backdrop-blur dark:border-slate-700/80 dark:bg-slate-900/50">
                <div className="flex items-start gap-3">
                  <div className="flex size-10 items-center justify-center rounded-2xl bg-indigo-100 text-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-300">
                    <Sparkles className="size-5" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                      {t("catalog:createItem.tips.clarity.title")}
                    </p>
                    <p className="mt-1 text-xs leading-5 text-slate-500 dark:text-slate-400">
                      {t("catalog:createItem.tips.clarity.description")}
                    </p>
                  </div>
                </div>
              </div>
              <div className="rounded-2xl border border-white/60 bg-white/80 p-4 shadow-sm backdrop-blur dark:border-slate-700/80 dark:bg-slate-900/50">
                <div className="flex items-start gap-3">
                  <div className="flex size-10 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-600 dark:bg-emerald-950/40 dark:text-emerald-300">
                    <BadgeCheck className="size-5" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                      {t("catalog:createItem.tips.status.title")}
                    </p>
                    <p className="mt-1 text-xs leading-5 text-slate-500 dark:text-slate-400">
                      {t("catalog:createItem.tips.status.description")}
                    </p>
                  </div>
                </div>
              </div>
              <div className="rounded-2xl border border-white/60 bg-white/80 p-4 shadow-sm backdrop-blur dark:border-slate-700/80 dark:bg-slate-900/50">
                <div className="flex items-start gap-3">
                  <div className="flex size-10 items-center justify-center rounded-2xl bg-amber-100 text-amber-600 dark:bg-amber-950/40 dark:text-amber-300">
                    <Store className="size-5" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                      {t("catalog:createItem.tips.images.title")}
                    </p>
                    <p className="mt-1 text-xs leading-5 text-slate-500 dark:text-slate-400">
                      {t("catalog:createItem.tips.images.description")}
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
          <div className="mb-6">
            <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">
              {t("catalog:listingDetails")}
            </h2>
            <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
              {t("catalog:createItem.formDescription")}
            </p>
          </div>

          <ItemForm
            onSubmit={handleSubmit}
            isSubmitting={createMutation.isPending}
            submitLabel={t("catalog:createItem.submit")}
          />
        </CardContent>
      </Card>
    </div>
  );
}
