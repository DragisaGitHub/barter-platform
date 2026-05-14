import React, { useEffect, useMemo, useState } from "react";
import { AxiosError } from "axios";
import { Archive, FolderTree, Pencil, Plus, RefreshCw, Search, Trash2 } from "lucide-react";
import { toast } from "sonner";
import type { ErrorResponse, AdminCategoryResponse, CreateCategoryRequest, UpdateCategoryRequest } from "@/api/generated/types.ts";
import { DataTable } from "@/components/data/DataTable";
import { Pagination } from "@/components/data/Pagination";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Skeleton } from "@/components/ui/Skeleton";
import { parseApiError } from "@/utils";
import {
  useAdminCategories,
  useAdminCategory,
  useCreateAdminCategory,
  useDeleteAdminCategory,
  useUpdateAdminCategory,
} from "./useAdminCategories";
import { AdminPageShell, AdminSurface, AdminToolbar } from "./components/AdminPageShell";

type SortField = "name" | "slug" | "sortOrder" | "createdAt";
type SortState = { field: SortField; direction: "asc" | "desc" };
type CategoryModalMode = "create" | "edit" | null;

interface CategoryFormValues {
  name: string;
  slug: string;
  description: string;
  parentUuid: string;
  sortOrder: string;
}

interface CategoryFormErrors {
  name?: string;
  slug?: string;
  description?: string;
  parentUuid?: string;
  sortOrder?: string;
  form?: string;
}

const DEFAULT_PAGE_SIZE = 20;
const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const EMPTY_FORM_VALUES: CategoryFormValues = {
  name: "",
  slug: "",
  description: "",
  parentUuid: "",
  sortOrder: "0",
};

function mapCategoryToFormValues(category?: AdminCategoryResponse | null): CategoryFormValues {
  if (!category) {
    return EMPTY_FORM_VALUES;
  }

  return {
    name: category.name,
    slug: category.slug,
    description: category.description ?? "",
    parentUuid: category.parentUuid ?? "",
    sortOrder: String(category.sortOrder ?? 0),
  };
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "—";
  }

  return new Date(value).toLocaleString();
}

function mapApiErrors(error: unknown): CategoryFormErrors {
  const nextErrors: CategoryFormErrors = {};

  if (error instanceof AxiosError) {
    const response = error.response?.data as ErrorResponse | undefined;
    if (response?.fieldErrors?.length) {
      response.fieldErrors.forEach((fieldError) => {
        const field = fieldError.field as keyof CategoryFormErrors;
        if (["name", "slug", "description", "parentUuid", "sortOrder", "form"].includes(field)) {
          nextErrors[field] = fieldError.message;
        } else {
          nextErrors.form = nextErrors.form ? `${nextErrors.form} ${fieldError.message}` : fieldError.message;
        }
      });
    }
  }

  if (!nextErrors.form) {
    nextErrors.form = parseApiError(error);
  }

  return nextErrors;
}

function validateCategoryForm(
  values: CategoryFormValues,
  currentCategory?: AdminCategoryResponse | null
): { errors: CategoryFormErrors; payload?: CreateCategoryRequest | UpdateCategoryRequest } {
  const errors: CategoryFormErrors = {};
  const name = values.name.trim();
  const slug = values.slug.trim();
  const description = values.description.trim();
  const sortOrderValue = values.sortOrder.trim();
  const parsedSortOrder = Number(sortOrderValue);

  if (!name) {
    errors.name = "Name is required.";
  }

  if (!sortOrderValue) {
    errors.sortOrder = "Sort order is required.";
  } else if (!Number.isInteger(parsedSortOrder)) {
    errors.sortOrder = "Sort order must be a whole number.";
  }

  if (currentCategory?.uuid && values.parentUuid === currentCategory.uuid) {
    errors.parentUuid = "A category cannot be its own parent.";
  }

  if (currentCategory?.parentUuid && !values.parentUuid) {
    errors.parentUuid = "Removing a parent is not currently supported by the backend API.";
  }

  if (Object.keys(errors).length > 0) {
    return { errors };
  }

  const payload: CreateCategoryRequest | UpdateCategoryRequest = {
    name,
    slug: slug || "",
    description: description || "",
    sortOrder: parsedSortOrder,
    ...(values.parentUuid ? { parentUuid: values.parentUuid } : {}),
  };

  return { errors: {}, payload };
}

export function AdminCategoriesPage() {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [searchInput, setSearchInput] = useState("");
  const [query, setQuery] = useState("");
  const [includeDeleted, setIncludeDeleted] = useState(false);
  const [sort, setSort] = useState<SortState>({ field: "sortOrder", direction: "asc" });
  const [modalMode, setModalMode] = useState<CategoryModalMode>(null);
  const [editingCategoryUuid, setEditingCategoryUuid] = useState<string | null>(null);
  const [formValues, setFormValues] = useState<CategoryFormValues>(EMPTY_FORM_VALUES);
  const [formErrors, setFormErrors] = useState<CategoryFormErrors>({});
  const [deleteCandidate, setDeleteCandidate] = useState<AdminCategoryResponse | null>(null);

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      setPage(0);
      setQuery(searchInput.trim());
    }, 300);

    return () => window.clearTimeout(timeout);
  }, [searchInput]);

  const sortParam = `${sort.field},${sort.direction}`;
  const queryParams = useMemo(
    () => ({
      page,
      size: pageSize,
      sort: sortParam,
      q: query || undefined,
      includeDeleted,
    }),
    [includeDeleted, page, pageSize, query, sortParam]
  );

  const categoriesQuery = useAdminCategories(queryParams);
  const createMutation = useCreateAdminCategory();
  const updateMutation = useUpdateAdminCategory();
  const deleteMutation = useDeleteAdminCategory();
  const isFormModalOpen = modalMode !== null;

  const parentCategoriesQuery = useAdminCategories(
    {
      page: 0,
      size: 100,
      sort: "name,asc",
      includeDeleted: false,
    },
    isFormModalOpen
  );

  const editCategoryQuery = useAdminCategory(editingCategoryUuid ?? "", modalMode === "edit" && !!editingCategoryUuid);

  const currentEditCategory = useMemo(() => {
    if (!editingCategoryUuid) {
      return null;
    }

    return (
      editCategoryQuery.data ??
      categoriesQuery.data?.content.find((category) => category.uuid === editingCategoryUuid) ??
      null
    );
  }, [categoriesQuery.data?.content, editCategoryQuery.data, editingCategoryUuid]);

  useEffect(() => {
    if (modalMode === "edit" && currentEditCategory) {
      setFormValues(mapCategoryToFormValues(currentEditCategory));
      setFormErrors({});
    }
  }, [currentEditCategory, modalMode]);

  const parentOptions = useMemo(() => {
    return (parentCategoriesQuery.data?.content ?? []).filter((category) => category.uuid !== editingCategoryUuid);
  }, [editingCategoryUuid, parentCategoriesQuery.data?.content]);

  const data = categoriesQuery.data;
  const categories = data?.content ?? [];
  const totalCategories = data?.totalElements ?? 0;

  const openCreateModal = () => {
    setModalMode("create");
    setEditingCategoryUuid(null);
    setFormValues(EMPTY_FORM_VALUES);
    setFormErrors({});
  };

  const openEditModal = (category: AdminCategoryResponse) => {
    setModalMode("edit");
    setEditingCategoryUuid(category.uuid);
    setFormValues(mapCategoryToFormValues(category));
    setFormErrors({});
  };

  const closeFormModal = () => {
    setModalMode(null);
    setEditingCategoryUuid(null);
    setFormValues(EMPTY_FORM_VALUES);
    setFormErrors({});
  };

  const handleSort = (field: string) => {
    const nextField = field as SortField;
    setPage(0);
    setSort((previous) => {
      if (previous.field === nextField) {
        return {
          field: nextField,
          direction: previous.direction === "asc" ? "desc" : "asc",
        };
      }

      return {
        field: nextField,
        direction: "asc",
      };
    });
  };

  const handleFormFieldChange = (field: keyof CategoryFormValues, value: string) => {
    setFormValues((previous) => ({ ...previous, [field]: value }));
    setFormErrors((previous) => ({ ...previous, [field]: undefined, form: undefined }));
  };

  const handleSubmitForm = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const { errors, payload } = validateCategoryForm(formValues, currentEditCategory);
    if (!payload) {
      setFormErrors(errors);
      return;
    }

    if (modalMode === "create") {
      createMutation.mutate(payload as CreateCategoryRequest, {
        onSuccess: (category) => {
          toast.success(`Category \"${category.name}\" created.`);
          closeFormModal();
        },
        onError: (error) => {
          const nextErrors = mapApiErrors(error);
          setFormErrors(nextErrors);
          toast.error(nextErrors.form ?? "Failed to create category.");
        },
      });
      return;
    }

    if (!editingCategoryUuid) {
      return;
    }

    updateMutation.mutate(
      {
        categoryUuid: editingCategoryUuid,
        data: payload as UpdateCategoryRequest,
      },
      {
        onSuccess: (category) => {
          toast.success(`Category \"${category.name}\" updated.`);
          closeFormModal();
        },
        onError: (error) => {
          const nextErrors = mapApiErrors(error);
          setFormErrors(nextErrors);
          toast.error(nextErrors.form ?? "Failed to update category.");
        },
      }
    );
  };

  const confirmDelete = () => {
    if (!deleteCandidate) {
      return;
    }

    deleteMutation.mutate(deleteCandidate.uuid, {
      onSuccess: () => {
        toast.success(`Category \"${deleteCandidate.name}\" archived.`);
        setDeleteCandidate(null);
      },
      onError: (error) => {
        toast.error(parseApiError(error));
      },
    });
  };

  const isSubmittingForm = createMutation.isPending || updateMutation.isPending;
  const isInitialLoading = categoriesQuery.isLoading;
  const isFormLoading = modalMode === "edit" && editCategoryQuery.isLoading && !currentEditCategory;

  const shellBadges = (
    <>
      <Badge variant="primary">Taxonomy</Badge>
      <Badge>{totalCategories} categories</Badge>
      {includeDeleted && <Badge variant="warning">Archived visible</Badge>}
    </>
  );

  return (
    <>
      <AdminPageShell
        title="Categories"
        description="Manage the platform taxonomy with real backend search, sorting, pagination, and archive-aware category administration."
        badges={shellBadges}
        actions={
          <Button type="button" onClick={openCreateModal}>
            <Plus className="size-4" />
            Create category
          </Button>
        }
      >
        <AdminToolbar>
          <div className="flex flex-1 flex-col gap-3 lg:flex-row lg:items-center">
            <div className="relative w-full max-w-xl">
              <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
              <Input
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
                placeholder="Search categories by name or slug"
                className="pl-9"
                aria-label="Search categories"
              />
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <Button
                type="button"
                variant={includeDeleted ? "secondary" : "outline"}
                onClick={() => {
                  setPage(0);
                  setIncludeDeleted((previous) => !previous);
                }}
              >
                <Archive className="size-4" />
                {includeDeleted ? "Hide archived" : "Include archived"}
              </Button>

              <label className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600 dark:border-slate-700 dark:bg-slate-800/60 dark:text-slate-300">
                <span>Rows</span>
                <select
                  value={pageSize}
                  onChange={(event) => {
                    setPage(0);
                    setPageSize(Number(event.target.value));
                  }}
                  className="bg-transparent text-sm font-medium text-slate-900 outline-none dark:text-slate-100"
                  aria-label="Categories per page"
                >
                  {PAGE_SIZE_OPTIONS.map((size) => (
                    <option key={size} value={size}>
                      {size}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <Button type="button" variant="outline" onClick={() => categoriesQuery.refetch()}>
              <RefreshCw className="size-4" />
              Refresh
            </Button>
          </div>
        </AdminToolbar>

        <AdminSurface
          title="Category directory"
          description="All list interactions use the live admin category APIs, including query filtering, backend sorting, pagination, and archived visibility."
          contentClassName="space-y-0"
        >
          {isInitialLoading ? (
            <div className="space-y-3">
              {[...Array(6)].map((_, index) => (
                <Skeleton key={index} className="h-16 w-full" />
              ))}
            </div>
          ) : categoriesQuery.isError || !data ? (
            <EmptyState
              icon={<FolderTree className="size-16" />}
              title="Unable to load categories"
              description={parseApiError(categoriesQuery.error)}
              action={
                <Button type="button" variant="outline" onClick={() => categoriesQuery.refetch()}>
                  Retry
                </Button>
              }
            />
          ) : categories.length === 0 ? (
            <EmptyState
              icon={<FolderTree className="size-16" />}
              title={query || includeDeleted ? "No categories match this view" : "No categories created yet"}
              description={
                query || includeDeleted
                  ? "Try a different search term or change the archived filter to widen the category result set."
                  : "Create the first category to start managing the platform taxonomy from this control panel."
              }
              action={
                <Button type="button" onClick={openCreateModal}>
                  <Plus className="size-4" />
                  Create category
                </Button>
              }
            />
          ) : (
            <>
              <DataTable
                columns={[
                  {
                    key: "name",
                    label: "Category",
                    sortable: true,
                    render: (category: AdminCategoryResponse) => (
                      <div className="min-w-0 whitespace-normal">
                        <p className="font-semibold text-slate-900 dark:text-slate-100">{category.name}</p>
                        <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                          {category.description?.trim() || "No description provided."}
                        </p>
                      </div>
                    ),
                  },
                  {
                    key: "slug",
                    label: "Slug",
                    sortable: true,
                    render: (category: AdminCategoryResponse) => (
                      <code className="rounded-lg bg-slate-100 px-2 py-1 text-xs font-medium text-slate-700 dark:bg-slate-800 dark:text-slate-300">
                        {category.slug}
                      </code>
                    ),
                  },
                  {
                    key: "parent",
                    label: "Parent category",
                    sortable: false,
                    render: (category: AdminCategoryResponse) =>
                      category.parentName ? (
                        <div>
                          <p className="font-medium text-slate-900 dark:text-slate-100">{category.parentName}</p>
                          <p className="text-xs text-slate-500 dark:text-slate-400">Nested category</p>
                        </div>
                      ) : (
                        <Badge variant="default">No parent</Badge>
                      ),
                  },
                  {
                    key: "sortOrder",
                    label: "Sort order",
                    sortable: true,
                    render: (category: AdminCategoryResponse) => (
                      <span className="font-medium text-slate-900 dark:text-slate-100">{category.sortOrder}</span>
                    ),
                  },
                  {
                    key: "createdAt",
                    label: "Created",
                    sortable: true,
                    render: (category: AdminCategoryResponse) => (
                      <div className="whitespace-normal">
                        <p>{formatDateTime(category.createdAt)}</p>
                        <p className="text-xs text-slate-500 dark:text-slate-400">
                          {category.updatedAt ? `Updated ${formatDateTime(category.updatedAt)}` : "No updates recorded"}
                        </p>
                      </div>
                    ),
                  },
                  {
                    key: "status",
                    label: "Status",
                    sortable: false,
                    render: (category: AdminCategoryResponse) => (
                      <div className="flex flex-col gap-2 whitespace-normal">
                        <div className="flex flex-wrap gap-2">
                          <Badge variant={category.deleted ? "warning" : "success"}>
                            {category.deleted ? "Archived" : "Active"}
                          </Badge>
                          {category.deletedAt && <Badge variant="default">{formatDateTime(category.deletedAt)}</Badge>}
                        </div>
                      </div>
                    ),
                  },
                  {
                    key: "actions",
                    label: "Actions",
                    sortable: false,
                    render: (category: AdminCategoryResponse) => (
                      <div className="flex flex-wrap items-center gap-2">
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          onClick={(event) => {
                            event.stopPropagation();
                            openEditModal(category);
                          }}
                        >
                          <Pencil className="size-4" />
                          Edit
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          disabled={category.deleted}
                          onClick={(event) => {
                            event.stopPropagation();
                            setDeleteCandidate(category);
                          }}
                        >
                          <Trash2 className="size-4" />
                          Archive
                        </Button>
                      </div>
                    ),
                  },
                ]}
                data={categories}
                currentSort={sort}
                onSort={handleSort}
              />

              <Pagination
                currentPage={data.page ?? 0}
                totalPages={Math.max(data.totalPages ?? 0, 1)}
                onPageChange={setPage}
                statusContent={
                  <>
                    <span className="rounded-full bg-slate-100 px-3 py-1 text-sm text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                      Sort: {sort.field} ({sort.direction})
                    </span>
                    {categoriesQuery.isFetching && !isInitialLoading && (
                      <span className="rounded-full bg-indigo-50 px-3 py-1 text-sm text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-300">
                        Refreshing…
                      </span>
                    )}
                  </>
                }
              />
            </>
          )}
        </AdminSurface>
      </AdminPageShell>

      <Modal
        isOpen={isFormModalOpen}
        onClose={closeFormModal}
        size="lg"
        title={modalMode === "create" ? "Create category" : "Edit category"}
      >
        {isFormLoading ? (
          <div className="space-y-3">
            {[...Array(5)].map((_, index) => (
              <Skeleton key={index} className="h-12 w-full" />
            ))}
          </div>
        ) : (
          <form onSubmit={handleSubmitForm} className="space-y-4">
            <div className="grid gap-4 md:grid-cols-2">
              <Input
                label="Name"
                value={formValues.name}
                onChange={(event) => handleFormFieldChange("name", event.target.value)}
                error={formErrors.name}
                placeholder="e.g. Board Games"
                autoFocus
              />

              <Input
                label="Slug"
                value={formValues.slug}
                onChange={(event) => handleFormFieldChange("slug", event.target.value)}
                error={formErrors.slug}
                placeholder="Optional — generated by backend if blank"
              />
            </div>

            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">
                Description
              </label>
              <textarea
                value={formValues.description}
                onChange={(event) => handleFormFieldChange("description", event.target.value)}
                rows={4}
                placeholder="Optional internal context for administrators"
                className="w-full rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm text-slate-900 placeholder:text-slate-400 transition-colors duration-150 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:border-indigo-400 dark:focus:ring-indigo-400/20"
              />
              {formErrors.description && (
                <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">{formErrors.description}</p>
              )}
            </div>

            <div className="grid gap-4 md:grid-cols-[minmax(0,1fr)_180px]">
              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">
                  Parent category
                </label>
                <select
                  value={formValues.parentUuid}
                  onChange={(event) => handleFormFieldChange("parentUuid", event.target.value)}
                  className="w-full rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm text-slate-900 outline-none transition-colors focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:focus:border-indigo-400 dark:focus:ring-indigo-400/20"
                  aria-label="Select parent category"
                >
                  <option value="">No parent</option>
                  {parentOptions.map((category) => (
                    <option key={category.uuid} value={category.uuid}>
                      {category.name}
                    </option>
                  ))}
                </select>
                {formErrors.parentUuid && (
                  <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">{formErrors.parentUuid}</p>
                )}
                {parentCategoriesQuery.data && parentCategoriesQuery.data.totalElements > parentOptions.length && (
                  <p className="mt-1.5 text-xs text-slate-500 dark:text-slate-400">
                    Parent picker currently shows the first {parentOptions.length} active categories sorted by name.
                  </p>
                )}
              </div>

              <Input
                label="Sort order"
                type="number"
                inputMode="numeric"
                value={formValues.sortOrder}
                onChange={(event) => handleFormFieldChange("sortOrder", event.target.value)}
                error={formErrors.sortOrder}
                placeholder="0"
              />
            </div>

            {formErrors.form && (
              <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-300">
                {formErrors.form}
              </div>
            )}

            <div className="flex flex-col-reverse gap-2 pt-2 sm:flex-row sm:justify-end">
              <Button type="button" variant="outline" onClick={closeFormModal}>
                Cancel
              </Button>
              <Button type="submit" isLoading={isSubmittingForm}>
                {modalMode === "create" ? (
                  <>
                    <Plus className="size-4" />
                    Create category
                  </>
                ) : (
                  <>
                    <Pencil className="size-4" />
                    Save changes
                  </>
                )}
              </Button>
            </div>
          </form>
        )}
      </Modal>

      <Modal
        isOpen={!!deleteCandidate}
        onClose={() => setDeleteCandidate(null)}
        size="sm"
        title="Archive category"
      >
        <div className="space-y-4">
          <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800 dark:border-amber-900/50 dark:bg-amber-950/30 dark:text-amber-200">
            This action performs a soft delete. The category will be archived and will remain visible when archived results are included.
          </div>

          <div>
            <p className="text-sm text-slate-700 dark:text-slate-300">
              Archive <span className="font-semibold text-slate-900 dark:text-slate-100">{deleteCandidate?.name}</span>?
            </p>
            <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
              Use this when a category should be retired from active administration without permanently removing its record.
            </p>
          </div>

          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <Button type="button" variant="outline" onClick={() => setDeleteCandidate(null)}>
              Cancel
            </Button>
            <Button type="button" variant="danger" isLoading={deleteMutation.isPending} onClick={confirmDelete}>
              <Archive className="size-4" />
              Archive category
            </Button>
          </div>
        </div>
      </Modal>
    </>
  );
}

