import { useForm, FormProvider } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useCategories, useTags } from "./useCatalog";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import type { ItemCondition, ItemStatus } from "@/api/generated/types.ts";
import { useTranslation } from "react-i18next";

const CONDITIONS: { value: ItemCondition; labelKey: string }[] = [
  { value: "NEW", labelKey: "condition.new" },
  { value: "LIKE_NEW", labelKey: "condition.likeNew" },
  { value: "GOOD", labelKey: "condition.good" },
  { value: "USED", labelKey: "condition.used" },
  { value: "FOR_PARTS", labelKey: "condition.forParts" },
];

const STATUSES: { value: ItemStatus; labelKey: string }[] = [
  { value: "DRAFT", labelKey: "status.draft" },
  { value: "ACTIVE", labelKey: "status.active" },
];

const itemFormSchema = z.object({
  title: z.string().min(1, "validation.titleRequired").max(255, "validation.titleTooLong"),
  description: z.string().optional(),
  categoryUuid: z.string().min(1, "validation.categoryRequired"),
  tagUuids: z.array(z.string()).optional(),
  condition: z.enum(["NEW", "LIKE_NEW", "GOOD", "USED", "FOR_PARTS"] as const, {
    message: "validation.conditionRequired",
  }),
  status: z.enum(["DRAFT", "ACTIVE", "RESERVED", "ARCHIVED", "REMOVED"] as const).optional(),
});

export type ItemFormValues = z.infer<typeof itemFormSchema>;

interface ItemFormProps {
  defaultValues?: Partial<ItemFormValues>;
  onSubmit: (data: ItemFormValues) => void;
  isSubmitting?: boolean;
  submitLabel?: string;
}

export function ItemForm({
  defaultValues,
  onSubmit,
  isSubmitting = false,
  submitLabel,
}: ItemFormProps) {
  const { t } = useTranslation(["catalog", "common"]);
  const { data: categories, isLoading: categoriesLoading } = useCategories();
  const { data: tags, isLoading: tagsLoading } = useTags();

  const methods = useForm<ItemFormValues>({
    resolver: zodResolver(itemFormSchema),
    defaultValues: {
      title: "",
      description: "",
      categoryUuid: "",
      tagUuids: [],
      condition: undefined,
      status: "DRAFT",
      ...defaultValues,
    },
  });

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = methods;

  const handleTagToggle = (tagUuid: string) => {
    const current = methods.getValues("tagUuids") ?? [];
    if (current.includes(tagUuid)) {
      methods.setValue(
        "tagUuids",
        current.filter((t) => t !== tagUuid)
      );
    } else {
      methods.setValue("tagUuids", [...current, tagUuid]);
    }
  };

  const selectedTags = methods.watch("tagUuids") ?? [];

  const translateError = (message?: string) => (message ? t(`catalog:${message}`) : undefined);

  return (
    <FormProvider {...methods}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <Input
          label={t("catalog:fields.title")}
          {...register("title")}
          error={translateError(errors.title?.message)}
          placeholder={t("catalog:itemForm.titlePlaceholder")}
        />

        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">
            {t("catalog:fields.description")}
          </label>
          <textarea
            {...register("description")}
            className="w-full rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm text-slate-900 placeholder:text-slate-400 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
            rows={4}
            placeholder={t("catalog:itemForm.descriptionPlaceholder")}
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">
            {t("catalog:fields.category")}
          </label>
          <select
            {...register("categoryUuid")}
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            disabled={categoriesLoading}
          >
            <option value="">{t("catalog:itemForm.selectCategory")}</option>
            {categories?.map((cat) => (
              <option key={cat.uuid} value={cat.uuid}>
                {cat.name}
              </option>
            ))}
          </select>
          {errors.categoryUuid && (
            <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">
              {translateError(errors.categoryUuid.message)}
            </p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">
            {t("catalog:fields.condition")}
          </label>
          <select
            {...register("condition")}
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          >
            <option value="">{t("catalog:itemForm.selectCondition")}</option>
            {CONDITIONS.map((c) => (
              <option key={c.value} value={c.value}>
                {t(`catalog:${c.labelKey}`)}
              </option>
            ))}
          </select>
          {errors.condition && (
            <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">
              {translateError(errors.condition.message)}
            </p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">
            {t("catalog:fields.status")}
          </label>
          <select
            {...register("status")}
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          >
            {STATUSES.map((s) => (
              <option key={s.value} value={s.value}>
                {t(`catalog:${s.labelKey}`)}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">
            {t("catalog:tags")}
          </label>
          {tagsLoading ? (
            <p className="text-sm text-slate-500">{t("catalog:itemForm.loadingTags")}</p>
          ) : (
            <div className="flex flex-wrap gap-2">
              {tags?.map((tag) => (
                <button
                  key={tag.uuid}
                  type="button"
                  onClick={() => handleTagToggle(tag.uuid)}
                  className={`px-3 py-1.5 text-sm rounded-full border transition-colors ${
                    selectedTags.includes(tag.uuid)
                      ? "bg-indigo-600 text-white border-indigo-600"
                      : "bg-white text-slate-700 border-slate-300 hover:border-indigo-400 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-600"
                  }`}
                >
                  {tag.name}
                </button>
              ))}
              {tags?.length === 0 && (
                <p className="text-sm text-slate-500">{t("catalog:itemForm.noTags")}</p>
              )}
            </div>
          )}
        </div>

        <div className="flex justify-end gap-3 pt-4">
          <Button type="submit" isLoading={isSubmitting}>
            {submitLabel ?? t("common:save")}
          </Button>
        </div>
      </form>
    </FormProvider>
  );
}

