import { useFieldArray, useForm, FormProvider } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useCategories, useTags } from "./useCatalog";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import type { ItemCondition, ItemStatus, ListingMode } from "@/api/generated/types.ts";
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

const LISTING_MODES: { value: ListingMode; labelKey: string; helperKey: string }[] = [
  { value: "SINGLE", labelKey: "listingMode.single", helperKey: "itemForm.listingMode.singleHelper" },
  { value: "BUNDLE", labelKey: "listingMode.bundle", helperKey: "itemForm.listingMode.bundleHelper" },
  { value: "PICK_ANY", labelKey: "listingMode.pickAny", helperKey: "itemForm.listingMode.pickAnyHelper" },
];

const entrySchema = z.object({
  title: z.string().trim().min(1, "validation.entryTitleRequired").max(200, "validation.entryTitleTooLong"),
  description: z.string().optional(),
  quantity: z.preprocess(
    (value) => {
      if (value === "" || value == null || (typeof value === "number" && Number.isNaN(value))) {
        return undefined;
      }
      return value;
    },
    z.number().int().min(1, "validation.entryQuantityMin").optional()
  ),
});

const itemFormSchema = z
  .object({
    title: z.string().min(1, "validation.titleRequired").max(255, "validation.titleTooLong"),
    description: z.string().optional(),
    exchangeCity: z.string().max(120, "validation.exchangeCityTooLong").optional(),
    exchangeArea: z.string().max(120, "validation.exchangeAreaTooLong").optional(),
    exchangeLocation: z.string().max(255, "validation.exchangeLocationTooLong").optional(),
    categoryUuid: z.string().min(1, "validation.categoryRequired"),
    tagUuids: z.array(z.string()).optional(),
    condition: z.enum(["NEW", "LIKE_NEW", "GOOD", "USED", "FOR_PARTS"] as const, {
      message: "validation.conditionRequired",
    }),
    status: z.enum(["DRAFT", "ACTIVE", "RESERVED", "ARCHIVED", "REMOVED"] as const).optional(),
    listingMode: z.enum(["SINGLE", "BUNDLE", "PICK_ANY"] as const),
    entries: z.array(entrySchema).max(20, "validation.entriesTooMany").optional(),
  })
  .superRefine((value, ctx) => {
    if (value.listingMode !== "SINGLE" && (!value.entries || value.entries.length === 0)) {
      ctx.addIssue({
        code: "custom",
        path: ["entries"],
        message: "validation.entriesRequiredForMultiItem",
      });
    }
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
      exchangeCity: "",
      exchangeArea: "",
      exchangeLocation: "",
      categoryUuid: "",
      tagUuids: [],
      condition: undefined,
      status: "DRAFT",
      listingMode: "SINGLE",
      entries: [],
      ...defaultValues,
    },
  });

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = methods;
  const { fields: entryFields, append: appendEntry, remove: removeEntry } = useFieldArray({
    control,
    name: "entries",
  });

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
  const selectedListingMode = methods.watch("listingMode") ?? "SINGLE";
  const showEntries = selectedListingMode !== "SINGLE";
  const selectedListingModeOption = LISTING_MODES.find((mode) => mode.value === selectedListingMode);

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

        <div className="rounded-2xl border border-indigo-100 bg-indigo-50/70 p-4 dark:border-indigo-900/50 dark:bg-indigo-950/20">
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">
            {t("catalog:fields.listingMode")}
          </label>
          <select
            {...register("listingMode")}
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          >
            {LISTING_MODES.map((mode) => (
              <option key={mode.value} value={mode.value}>
                {t(`catalog:${mode.labelKey}`)}
              </option>
            ))}
          </select>
          <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">
            {selectedListingModeOption ? t(`catalog:${selectedListingModeOption.helperKey}`) : null}
          </p>

          {showEntries ? (
            <div className="mt-4 space-y-3">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                    {t("catalog:itemForm.entriesTitle")}
                  </h3>
                  <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                    {t("catalog:itemForm.entriesHelper")}
                  </p>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  disabled={entryFields.length >= 20}
                  onClick={() => appendEntry({ title: "", description: "", quantity: undefined })}
                >
                  {t("catalog:itemForm.addEntry")}
                </Button>
              </div>

              {entryFields.map((field, index) => (
                <div
                  key={field.id}
                  className="rounded-xl border border-slate-200 bg-white p-3 dark:border-slate-700 dark:bg-slate-900/50"
                >
                  <div className="mb-3 flex items-center justify-between gap-3">
                    <p className="text-sm font-medium text-slate-700 dark:text-slate-200">
                      {t("catalog:itemForm.entryNumber", { number: index + 1 })}
                    </p>
                    <button
                      type="button"
                      onClick={() => removeEntry(index)}
                      className="text-xs font-medium text-red-600 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300"
                    >
                      {t("catalog:itemForm.removeEntry")}
                    </button>
                  </div>
                  <div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_8rem]">
                    <Input
                      label={t("catalog:itemForm.entryTitle")}
                      {...register(`entries.${index}.title`)}
                      error={translateError(errors.entries?.[index]?.title?.message)}
                      placeholder={t("catalog:itemForm.entryTitlePlaceholder")}
                    />
                    <Input
                      type="number"
                      min={1}
                      label={t("catalog:itemForm.entryQuantity")}
                      {...register(`entries.${index}.quantity`, { valueAsNumber: true })}
                      error={translateError(errors.entries?.[index]?.quantity?.message)}
                      placeholder="1"
                    />
                  </div>
                  <div className="mt-3">
                    <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">
                      {t("catalog:itemForm.entryDescription")}
                    </label>
                    <textarea
                      {...register(`entries.${index}.description`)}
                      className="w-full rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm text-slate-900 placeholder:text-slate-400 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
                      rows={2}
                      placeholder={t("catalog:itemForm.entryDescriptionPlaceholder")}
                    />
                  </div>
                </div>
              ))}

              {entryFields.length === 0 ? (
                <p className="rounded-lg border border-dashed border-slate-300 px-3 py-4 text-center text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400">
                  {t("catalog:itemForm.noEntries")}
                </p>
              ) : null}

              {errors.entries && !Array.isArray(errors.entries) ? (
                <p className="text-sm text-red-600 dark:text-red-400">
                  {translateError(errors.entries.message)}
                </p>
              ) : null}
            </div>
          ) : null}
        </div>

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

        <div className="rounded-2xl border border-emerald-100 bg-emerald-50/70 p-4 dark:border-emerald-900/50 dark:bg-emerald-950/20">
          <div className="mb-4">
            <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">
              {t("catalog:itemForm.exchangeLocationTitle")}
            </h3>
            <p className="mt-1 text-sm leading-6 text-slate-600 dark:text-slate-300">
              {t("catalog:itemForm.exchangeLocationHelper")}
            </p>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Input
              label={t("catalog:fields.exchangeCity")}
              {...register("exchangeCity")}
              error={translateError(errors.exchangeCity?.message)}
              placeholder={t("catalog:itemForm.exchangeCityPlaceholder")}
            />

            <Input
              label={t("catalog:fields.exchangeArea")}
              {...register("exchangeArea")}
              error={translateError(errors.exchangeArea?.message)}
              placeholder={t("catalog:itemForm.exchangeAreaPlaceholder")}
            />
          </div>

          <div className="mt-4">
            <Input
              label={t("catalog:fields.exchangeLocation")}
              {...register("exchangeLocation")}
              error={translateError(errors.exchangeLocation?.message)}
              placeholder={t("catalog:itemForm.exchangeLocationPlaceholder")}
            />
          </div>
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

