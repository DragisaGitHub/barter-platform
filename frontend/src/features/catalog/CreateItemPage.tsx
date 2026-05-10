import { useNavigate, Link } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { useCreateItem } from "./useCatalog";
import { ItemForm, type ItemFormValues } from "./ItemForm";
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
        onSuccess: () => {
          toast.success("Item created successfully");
          navigate("/my-items");
        },
        onError: () => {
          toast.error("Failed to create item");
        },
      }
    );
  };

  return (
    <div className="max-w-2xl mx-auto">
      <Link
        to="/my-items"
        className="inline-flex items-center gap-1 text-sm text-slate-600 dark:text-slate-400 hover:text-indigo-600 dark:hover:text-indigo-400 mb-6"
      >
        <ArrowLeft className="size-4" />
        Back to My Items
      </Link>

      <h1 className="text-3xl font-bold text-slate-900 dark:text-white mb-8">Create New Item</h1>

      <ItemForm
        onSubmit={handleSubmit}
        isSubmitting={createMutation.isPending}
        submitLabel="Create Item"
      />
    </div>
  );
}

