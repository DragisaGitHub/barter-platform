import { useNavigate, Link } from "react-router-dom";
import { ArrowLeft, BadgeCheck, Sparkles, Store } from "lucide-react";
import { useCreateItem } from "./useCatalog";
import { ItemForm, type ItemFormValues } from "./ItemForm";
import { Card, CardContent } from "../../components/ui/Card";
import { Badge } from "../../components/ui/Badge";
import { toast } from "sonner";

export function CreateItemPage() {
  const navigate = useNavigate();
  const createMutation = useCreateItem();

  const handleSubmit = (data: ItemFormValues) => {
    createMutation.mutate(
      {
        title: data.title,
        description: data.description || undefined,
        categoryUuid: data.categoryUuid,
        tagUuids: data.tagUuids?.length ? data.tagUuids : undefined,
        condition: data.condition,
        status: data.status,
      },
      {
        onSuccess: (item) => {
          toast.success("Item created! You can now add images.");
          navigate(`/my-items/${item.uuid}/edit`);
        },
        onError: () => {
          toast.error("Failed to create item");
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
          Back to My Items
        </Link>
      </div>

      <section className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-800">
        <div className="bg-gradient-to-r from-indigo-500/10 via-slate-100 to-emerald-500/10 px-5 py-6 dark:from-indigo-500/10 dark:via-slate-800 dark:to-emerald-500/10 sm:px-6 lg:px-8">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
            <div className="max-w-2xl">
              <div className="flex flex-wrap items-center gap-2">
                <Badge variant="primary">New listing</Badge>
                <Badge variant="default">Seller workflow</Badge>
              </div>
              <h1 className="mt-4 text-3xl font-bold tracking-tight text-slate-900 dark:text-white sm:text-4xl">
                Create New Item
              </h1>
              <p className="mt-3 text-sm leading-6 text-slate-600 dark:text-slate-300 sm:text-base">
                Create a detailed listing to attract better trade offers. Clear
                titles, accurate condition details, and the right category help
                your item get noticed faster.
              </p>
            </div>

            <div className="grid gap-3 sm:grid-cols-3 lg:w-[28rem] lg:grid-cols-1">
              <div className="rounded-2xl border border-white/60 bg-white/80 p-4 shadow-sm backdrop-blur dark:border-slate-700/80 dark:bg-slate-900/50">
                <div className="flex items-start gap-3">
                  <div className="flex size-10 items-center justify-center rounded-2xl bg-indigo-100 text-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-300">
                    <Sparkles className="size-5" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                      Lead with clarity
                    </p>
                    <p className="mt-1 text-xs leading-5 text-slate-500 dark:text-slate-400">
                      Use a title and description that quickly explain why the item
                      is worth trading for.
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
                      Choose the right status
                    </p>
                    <p className="mt-1 text-xs leading-5 text-slate-500 dark:text-slate-400">
                      Save as draft if you want to refine the listing first, or
                      publish when it is ready.
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
                      Images come next
                    </p>
                    <p className="mt-1 text-xs leading-5 text-slate-500 dark:text-slate-400">
                      After saving, you will move straight into editing so you can
                      upload product photos.
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
              Listing details
            </h2>
            <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
              Add the essential information buyers and traders need before you
              publish your item.
            </p>
          </div>

          <ItemForm
            onSubmit={handleSubmit}
            isSubmitting={createMutation.isPending}
            submitLabel="Create Item"
          />
        </CardContent>
      </Card>
    </div>
  );
}
