import { useParams, useNavigate, Link } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { useItemDetail, useUpdateItem } from "./useCatalog";
import { ItemForm, type ItemFormValues } from "./ItemForm";
import { Spinner } from "../../components/ui/Spinner";
import { EmptyState } from "../../components/ui/EmptyState";
import { Button } from "../../components/ui/Button";
import { toast } from "sonner";

export function EditItemPage() {
  const { uuid } = useParams<{ uuid: string }>();
  const navigate = useNavigate();
  const { data: item, isLoading, isError } = useItemDetail(uuid ?? "");
  const updateMutation = useUpdateItem(uuid ?? "");

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
    <div className="max-w-2xl mx-auto">
      <Link
        to="/my-items"
        className="inline-flex items-center gap-1 text-sm text-slate-600 dark:text-slate-400 hover:text-indigo-600 dark:hover:text-indigo-400 mb-6"
      >
        <ArrowLeft className="size-4" />
        Back to My Items
      </Link>

      <h1 className="text-3xl font-bold text-slate-900 dark:text-white mb-8">Edit Item</h1>

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
    </div>
  );
}

