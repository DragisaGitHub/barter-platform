import { useParams, useNavigate, Link } from "react-router-dom";
import { ArrowLeft, Images, PencilLine, ShieldCheck } from "lucide-react";
import { useItemDetail, useUpdateItem } from "./useCatalog";
import { ItemForm, type ItemFormValues } from "./ItemForm";
import { ItemStatusBadge } from "./ItemBadges";
import { Spinner } from "../../components/ui/Spinner";
import { EmptyState } from "../../components/ui/EmptyState";
import { Button } from "../../components/ui/Button";
import { Card, CardContent } from "../../components/ui/Card";
import { Badge } from "../../components/ui/Badge";
import { toast } from "sonner";
import { useItemImages } from "./useItemImages";
import { ImageUploader } from "./components/ImageUploader";
import { ItemImageGallery } from "./components/ItemImageGallery";

export function EditItemPage() {
  const { uuid } = useParams<{ uuid: string }>();
  const navigate = useNavigate();
  const { data: item, isLoading, isError } = useItemDetail(uuid ?? "");
  const updateMutation = useUpdateItem(uuid ?? "");
  const { data: images = [] } = useItemImages(uuid ?? "");

  const handleSubmit = (data: ItemFormValues) => {
    updateMutation.mutate(
      {
        title: data.title,
        description: data.description || undefined,
        categoryUuid: data.categoryUuid,
        tagUuids: data.tagUuids?.length ? data.tagUuids : undefined,
        condition: data.condition,
        status: data.status,
      },
      {
        onSuccess: () => {
          toast.success("Item updated successfully");
          navigate("/my-items");
        },
        onError: () => {
          toast.error("Failed to update item");
        },
      }
    );
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Spinner size="lg" />
      </div>
    );
  }

  if (isError || !item) {
    return (
      <div className="max-w-2xl mx-auto">
        <EmptyState
          title="Item not found"
          description="This item may have been removed or you don't have access to edit it."
          action={
            <Link to="/my-items">
              <Button variant="outline">Back to My Items</Button>
            </Link>
          }
        />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl space-y-6">
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
        <div className="bg-gradient-to-r from-indigo-500/10 via-slate-100 to-sky-500/10 px-5 py-6 dark:from-indigo-500/10 dark:via-slate-800 dark:to-sky-500/10 sm:px-6 lg:px-8">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
            <div className="max-w-2xl">
              <div className="flex flex-wrap items-center gap-2">
                <Badge variant="primary">Seller management</Badge>
                <ItemStatusBadge status={item.status} />
              </div>
              <h1 className="mt-4 text-3xl font-bold tracking-tight text-slate-900 dark:text-white sm:text-4xl">
                Edit Item
              </h1>
              <p className="mt-3 text-sm leading-6 text-slate-600 dark:text-slate-300 sm:text-base">
                Keep this listing accurate and up to date. Refresh details, improve presentation, and manage images so the item stays marketplace-ready.
              </p>
            </div>

            <div className="grid gap-3 sm:grid-cols-2 lg:w-[24rem] lg:grid-cols-1">
              <div className="rounded-2xl border border-white/60 bg-white/80 p-4 shadow-sm backdrop-blur dark:border-slate-700/80 dark:bg-slate-900/50">
                <div className="flex items-start gap-3">
                  <div className="flex size-10 items-center justify-center rounded-2xl bg-indigo-100 text-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-300">
                    <PencilLine className="size-5" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-slate-900 dark:text-slate-100">Refine listing details</p>
                    <p className="mt-1 text-xs leading-5 text-slate-500 dark:text-slate-400">
                      Update copy, condition, or status whenever the listing changes.
                    </p>
                  </div>
                </div>
              </div>
              <div className="rounded-2xl border border-white/60 bg-white/80 p-4 shadow-sm backdrop-blur dark:border-slate-700/80 dark:bg-slate-900/50">
                <div className="flex items-start gap-3">
                  <div className="flex size-10 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-600 dark:bg-emerald-950/40 dark:text-emerald-300">
                    <ShieldCheck className="size-5" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-slate-900 dark:text-slate-100">Stay marketplace ready</p>
                    <p className="mt-1 text-xs leading-5 text-slate-500 dark:text-slate-400">
                      Accurate statuses and strong photos make your listing easier to trust.
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
          <div className="mb-6 flex items-start gap-3">
            <div className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-indigo-100 text-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-300">
              <Images className="size-5" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">Listing images</h2>
              <p className="mt-1 text-sm leading-6 text-slate-600 dark:text-slate-400">
                Add or update images to make the listing more trustworthy and easier to evaluate. You currently have {images.length} image{images.length === 1 ? "" : "s"} attached.
              </p>
            </div>
          </div>

          <div className="space-y-4 rounded-2xl border border-slate-200 bg-slate-50/70 p-4 dark:border-slate-700 dark:bg-slate-900/40 sm:p-5">
            <ImageUploader itemUuid={uuid ?? ""} currentImageCount={images.length} />
            <ItemImageGallery itemUuid={uuid ?? ""} images={images} />
          </div>
        </CardContent>
      </Card>

      <Card className="rounded-3xl p-0 shadow-sm">
        <CardContent className="p-5 sm:p-6 lg:p-8">
          <div className="mb-6">
            <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">Listing details</h2>
            <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
              Review the information below and save changes when the listing is ready.
            </p>
          </div>

          <ItemForm
            defaultValues={{
              title: item.title,
              description: item.description ?? "",
              categoryUuid: item.category.uuid,
              tagUuids: item.tags.map((t) => t.uuid),
              condition: item.condition,
              status: item.status,
            }}
            onSubmit={handleSubmit}
            isSubmitting={updateMutation.isPending}
            submitLabel="Update Item"
          />
        </CardContent>
      </Card>
    </div>
  );
}

