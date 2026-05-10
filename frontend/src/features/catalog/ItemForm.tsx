import { useForm, FormProvider } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useCategories, useTags } from "./useCatalog";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import type { ItemCondition, ItemStatus } from "@/api/generated/types.ts";

const CONDITIONS: { value: ItemCondition; label: string }[] = [
  { value: "NEW", label: "New" },
  { value: "LIKE_NEW", label: "Like New" },
  { value: "GOOD", label: "Good" },
  { value: "USED", label: "Used" },
  { value: "FOR_PARTS", label: "For Parts" },
];

const STATUSES: { value: ItemStatus; label: string }[] = [
  { value: "DRAFT", label: "Draft" },
  { value: "ACTIVE", label: "Active" },
];

const itemFormSchema = z.object({
  title: z.string().min(1, "Title is required").max(255),
  description: z.string().optional(),
  categoryUuid: z.string().min(1, "Category is required"),
  tagUuids: z.array(z.string()).optional(),
  condition: z.enum(["NEW", "LIKE_NEW", "GOOD", "USED", "FOR_PARTS"] as const, {
    message: "Condition is required",
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
  submitLabel = "Save",
}: ItemFormProps) {
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

  return (
    <FormProvider {...methods}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <Input
          label="Title"
          {...register("title")}
          error={errors.title?.message}
          placeholder="What are you listing?"
        />

        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">
            Description
          </label>
          <textarea
            {...register("description")}
            className="w-full rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm text-slate-900 placeholder:text-slate-400 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
            rows={4}
            placeholder="Describe your item..."
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">
            Category
          </label>
          <select
            {...register("categoryUuid")}
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            disabled={categoriesLoading}
          >
            <option value="">Select a category</option>
            {categories?.map((cat) => (
              <option key={cat.uuid} value={cat.uuid}>
                {cat.name}
              </option>
            ))}
          </select>
          {errors.categoryUuid && (
            <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">
              {errors.categoryUuid.message}
            </p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">
            Condition
          </label>
          <select
            {...register("condition")}
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          >
            <option value="">Select condition</option>
            {CONDITIONS.map((c) => (
              <option key={c.value} value={c.value}>
                {c.label}
              </option>
            ))}
          </select>
          {errors.condition && (
            <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">
              {errors.condition.message}
            </p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">
            Status
          </label>
          <select
            {...register("status")}
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          >
            {STATUSES.map((s) => (
              <option key={s.value} value={s.value}>
                {s.label}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">
            Tags
          </label>
          {tagsLoading ? (
            <p className="text-sm text-slate-500">Loading tags...</p>
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
                <p className="text-sm text-slate-500">No tags available</p>
              )}
            </div>
          )}
        </div>

        <div className="flex justify-end gap-3 pt-4">
          <Button type="submit" isLoading={isSubmitting}>
            {submitLabel}
          </Button>
        </div>
      </form>
    </FormProvider>
  );
}

