import React, { useEffect, useMemo, useState } from "react";
import { AxiosError } from "axios";
import { Archive, RefreshCw, Search, Tag, Pencil, Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import type { AdminTagResponse, CreateTagRequest, ErrorResponse, UpdateTagRequest } from "@/api/generated/types.ts";
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
  useAdminTag,
  useAdminTags,
  useCreateAdminTag,
  useDeleteAdminTag,
  useUpdateAdminTag,
} from "./useAdminTags";
import { AdminPageShell, AdminSurface, AdminToolbar } from "./components/AdminPageShell";
import { useTranslation } from "react-i18next";

type SortField = "name" | "slug" | "createdAt";
type SortState = { field: SortField; direction: "asc" | "desc" };
type TagModalMode = "create" | "edit" | null;

interface TagFormValues {
  name: string;
  slug: string;
}

interface TagFormErrors {
  name?: string;
  slug?: string;
  form?: string;
}

const DEFAULT_PAGE_SIZE = 20;
const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const EMPTY_FORM_VALUES: TagFormValues = {
  name: "",
  slug: "",
};

function mapTagToFormValues(tag?: AdminTagResponse | null): TagFormValues {
  if (!tag) {
    return EMPTY_FORM_VALUES;
  }

  return {
    name: tag.name,
    slug: tag.slug,
  };
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "—";
  }

  return new Date(value).toLocaleString();
}

function mapApiErrors(error: unknown): TagFormErrors {
  const nextErrors: TagFormErrors = {};

  if (error instanceof AxiosError) {
    const response = error.response?.data as ErrorResponse | undefined;
    if (response?.fieldErrors?.length) {
      response.fieldErrors.forEach((fieldError) => {
        const field = fieldError.field as keyof TagFormErrors;
        if (["name", "slug", "form"].includes(field)) {
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

function validateTagForm(values: TagFormValues): { errors: TagFormErrors; payload?: CreateTagRequest | UpdateTagRequest } {
  const errors: TagFormErrors = {};
  const name = values.name.trim();
  const slug = values.slug.trim();

  if (!name) {
    errors.name = "Name is required.";
  }

  if (Object.keys(errors).length > 0) {
    return { errors };
  }

  return {
    errors: {},
    payload: {
      name,
      slug: slug || "",
    },
  };
}

export function AdminTagsPage() {
  const { t } = useTranslation(["admin", "common", "catalog"]);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [searchInput, setSearchInput] = useState("");
  const [query, setQuery] = useState("");
  const [includeDeleted, setIncludeDeleted] = useState(false);
  const [sort, setSort] = useState<SortState>({ field: "name", direction: "asc" });
  const [modalMode, setModalMode] = useState<TagModalMode>(null);
  const [editingTagUuid, setEditingTagUuid] = useState<string | null>(null);
  const [formValues, setFormValues] = useState<TagFormValues>(EMPTY_FORM_VALUES);
  const [formErrors, setFormErrors] = useState<TagFormErrors>({});
  const [deleteCandidate, setDeleteCandidate] = useState<AdminTagResponse | null>(null);

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

  const tagsQuery = useAdminTags(queryParams);
  const createMutation = useCreateAdminTag();
  const updateMutation = useUpdateAdminTag();
  const deleteMutation = useDeleteAdminTag();
  const editTagQuery = useAdminTag(editingTagUuid ?? "", modalMode === "edit" && !!editingTagUuid);

  const currentEditTag = useMemo(() => {
    if (!editingTagUuid) {
      return null;
    }

    return editTagQuery.data ?? tagsQuery.data?.content.find((tag) => tag.uuid === editingTagUuid) ?? null;
  }, [editTagQuery.data, editingTagUuid, tagsQuery.data?.content]);

  useEffect(() => {
    if (modalMode === "edit" && currentEditTag) {
      setFormValues(mapTagToFormValues(currentEditTag));
      setFormErrors({});
    }
  }, [currentEditTag, modalMode]);

  const data = tagsQuery.data;
  const tags = data?.content ?? [];
  const totalTags = data?.totalElements ?? 0;

  const openCreateModal = () => {
    setModalMode("create");
    setEditingTagUuid(null);
    setFormValues(EMPTY_FORM_VALUES);
    setFormErrors({});
  };

  const openEditModal = (tag: AdminTagResponse) => {
    setModalMode("edit");
    setEditingTagUuid(tag.uuid);
    setFormValues(mapTagToFormValues(tag));
    setFormErrors({});
  };

  const closeFormModal = () => {
    setModalMode(null);
    setEditingTagUuid(null);
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

  const handleFormFieldChange = (field: keyof TagFormValues, value: string) => {
    setFormValues((previous) => ({ ...previous, [field]: value }));
    setFormErrors((previous) => ({ ...previous, [field]: undefined, form: undefined }));
  };

  const handleSubmitForm = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const { errors, payload } = validateTagForm(formValues);
    if (!payload) {
      setFormErrors(errors);
      return;
    }

    if (modalMode === "create") {
      createMutation.mutate(payload as CreateTagRequest, {
        onSuccess: (tag) => {
          toast.success(`Tag \"${tag.name}\" created.`);
          closeFormModal();
        },
        onError: (error) => {
          const nextErrors = mapApiErrors(error);
          setFormErrors(nextErrors);
          toast.error(nextErrors.form ?? "Failed to create tag.");
        },
      });
      return;
    }

    if (!editingTagUuid) {
      return;
    }

    updateMutation.mutate(
      {
        tagUuid: editingTagUuid,
        data: payload as UpdateTagRequest,
      },
      {
        onSuccess: (tag) => {
          toast.success(`Tag \"${tag.name}\" updated.`);
          closeFormModal();
        },
        onError: (error) => {
          const nextErrors = mapApiErrors(error);
          setFormErrors(nextErrors);
          toast.error(nextErrors.form ?? "Failed to update tag.");
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
        toast.success(`Tag \"${deleteCandidate.name}\" archived.`);
        setDeleteCandidate(null);
      },
      onError: (error) => {
        toast.error(parseApiError(error));
      },
    });
  };

  const isSubmittingForm = createMutation.isPending || updateMutation.isPending;
  const isInitialLoading = tagsQuery.isLoading;
  const isFormLoading = modalMode === "edit" && editTagQuery.isLoading && !currentEditTag;

  return (
    <>
      <AdminPageShell
        title={t("admin:tags")}
        description={t("admin:tagsPage.description")}
        badges={
          <>
            <Badge variant="primary">{t("admin:tagsPage.metadata")}</Badge>
            <Badge>{t("admin:tagsPage.tagCount", { count: totalTags })}</Badge>
            {includeDeleted && <Badge variant="warning">{t("admin:archivedVisible")}</Badge>}
          </>
        }
        actions={
          <Button type="button" onClick={openCreateModal}>
            <Plus className="size-4" />
            {t("admin:tagsPage.createTag")}
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
                placeholder={t("admin:tagsPage.searchPlaceholder")}
                className="pl-9"
                aria-label="Search tags"
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
                {includeDeleted ? t("admin:hideArchived") : t("admin:includeArchived")}
              </Button>

              <label className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600 dark:border-slate-700 dark:bg-slate-800/60 dark:text-slate-300">
                <span>{t("admin:rows")}</span>
                <select
                  value={pageSize}
                  onChange={(event) => {
                    setPage(0);
                    setPageSize(Number(event.target.value));
                  }}
                  className="bg-transparent text-sm font-medium text-slate-900 outline-none dark:text-slate-100"
                  aria-label="Tags per page"
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
            <Button type="button" variant="outline" onClick={() => tagsQuery.refetch()}>
              <RefreshCw className="size-4" />
              {t("admin:refresh")}
            </Button>
          </div>
        </AdminToolbar>

        <AdminSurface
          title={t("admin:tagsPage.directoryTitle")}
          description={t("admin:tagsPage.directoryDescription")}
          contentClassName="space-y-0"
        >
          {isInitialLoading ? (
            <div className="space-y-3">
              {[...Array(6)].map((_, index) => (
                <Skeleton key={index} className="h-16 w-full" />
              ))}
            </div>
          ) : tagsQuery.isError || !data ? (
            <EmptyState
              icon={<Tag className="size-16" />}
              title={t("admin:tagsPage.loadErrorTitle")}
              description={parseApiError(tagsQuery.error)}
              action={
                <Button type="button" variant="outline" onClick={() => tagsQuery.refetch()}>
                  {t("common:tryAgain")}
                </Button>
              }
            />
          ) : tags.length === 0 ? (
            <EmptyState
              icon={<Tag className="size-16" />}
              title={query || includeDeleted ? t("admin:tagsPage.emptyFilteredTitle") : t("admin:tagsPage.emptyTitle")}
              description={
                query || includeDeleted
                  ? t("admin:tagsPage.emptyFilteredDescription")
                  : t("admin:tagsPage.emptyDescription")
              }
              action={
                <Button type="button" onClick={openCreateModal}>
                  <Plus className="size-4" />
                  {t("admin:tagsPage.createTag")}
                </Button>
              }
            />
          ) : (
            <>
              <DataTable
                columns={[
                  {
                    key: "name",
                    label: t("admin:tagsPage.tag"),
                    sortable: true,
                    render: (tag: AdminTagResponse) => (
                      <div>
                        <p className="font-semibold text-slate-900 dark:text-slate-100">{tag.name}</p>
                        <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{t("admin:tagsPage.reusableLabel")}</p>
                      </div>
                    ),
                  },
                  {
                    key: "slug",
                    label: t("admin:slug"),
                    sortable: true,
                    render: (tag: AdminTagResponse) => (
                      <code className="rounded-lg bg-slate-100 px-2 py-1 text-xs font-medium text-slate-700 dark:bg-slate-800 dark:text-slate-300">
                        {tag.slug}
                      </code>
                    ),
                  },
                  {
                    key: "createdAt",
                    label: t("admin:created"),
                    sortable: true,
                    render: (tag: AdminTagResponse) => (
                      <div className="whitespace-normal">
                        <p>{formatDateTime(tag.createdAt)}</p>
                        <p className="text-xs text-slate-500 dark:text-slate-400">
                          {tag.updatedAt ? t("admin:updatedAt", { date: formatDateTime(tag.updatedAt) }) : t("admin:noUpdatesRecorded")}
                        </p>
                      </div>
                    ),
                  },
                  {
                    key: "status",
                    label: t("catalog:fields.status"),
                    sortable: false,
                    render: (tag: AdminTagResponse) => (
                      <div className="flex flex-wrap gap-2 whitespace-normal">
                        <Badge variant={tag.deleted ? "warning" : "success"}>{tag.deleted ? t("catalog:status.archived") : t("catalog:status.active")}</Badge>
                        {tag.deletedAt && <Badge variant="default">{formatDateTime(tag.deletedAt)}</Badge>}
                      </div>
                    ),
                  },
                  {
                    key: "actions",
                    label: t("common:actions"),
                    sortable: false,
                    render: (tag: AdminTagResponse) => (
                      <div className="flex flex-wrap items-center gap-2">
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          onClick={(event) => {
                            event.stopPropagation();
                            openEditModal(tag);
                          }}
                        >
                          <Pencil className="size-4" />
                          {t("common:edit")}
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          disabled={tag.deleted}
                          onClick={(event) => {
                            event.stopPropagation();
                            setDeleteCandidate(tag);
                          }}
                        >
                          <Trash2 className="size-4" />
                          {t("admin:archive")}
                        </Button>
                      </div>
                    ),
                  },
                ]}
                data={tags}
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
                      {t("admin:sortStatus", { field: sort.field, direction: sort.direction })}
                    </span>
                    {tagsQuery.isFetching && !isInitialLoading && (
                      <span className="rounded-full bg-indigo-50 px-3 py-1 text-sm text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-300">
                        {t("admin:refreshing")}
                      </span>
                    )}
                  </>
                }
              />
            </>
          )}
        </AdminSurface>
      </AdminPageShell>

      <Modal isOpen={modalMode !== null} onClose={closeFormModal} size="md" title={modalMode === "create" ? t("admin:tagsPage.createTag") : t("admin:tagsPage.editTag")}>
        {isFormLoading ? (
          <div className="space-y-3">
            {[...Array(3)].map((_, index) => (
              <Skeleton key={index} className="h-12 w-full" />
            ))}
          </div>
        ) : (
          <form onSubmit={handleSubmitForm} className="space-y-4">
            <Input
              label={t("admin:name")}
              value={formValues.name}
              onChange={(event) => handleFormFieldChange("name", event.target.value)}
              error={formErrors.name}
              placeholder={t("admin:tagsPage.namePlaceholder")}
              autoFocus
            />

            <Input
              label={t("admin:slug")}
              value={formValues.slug}
              onChange={(event) => handleFormFieldChange("slug", event.target.value)}
              error={formErrors.slug}
              placeholder={t("admin:optionalGeneratedSlug")}
            />

            {formErrors.form && (
              <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-300">
                {formErrors.form}
              </div>
            )}

            <div className="flex flex-col-reverse gap-2 pt-2 sm:flex-row sm:justify-end">
              <Button type="button" variant="outline" onClick={closeFormModal}>
                {t("common:cancel")}
              </Button>
              <Button type="submit" isLoading={isSubmittingForm}>
                {modalMode === "create" ? (
                  <>
                    <Plus className="size-4" />
                    {t("admin:tagsPage.createTag")}
                  </>
                ) : (
                  <>
                    <Pencil className="size-4" />
                    {t("admin:saveChanges")}
                  </>
                )}
              </Button>
            </div>
          </form>
        )}
      </Modal>

      <Modal isOpen={!!deleteCandidate} onClose={() => setDeleteCandidate(null)} size="sm" title={t("admin:tagsPage.archiveTag")}>
        <div className="space-y-4">
          <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800 dark:border-amber-900/50 dark:bg-amber-950/30 dark:text-amber-200">
            {t("admin:tagsPage.archiveSoftDelete")}
          </div>

          <div>
            <p className="text-sm text-slate-700 dark:text-slate-300">
              {t("admin:archivePromptPrefix")} <span className="font-semibold text-slate-900 dark:text-slate-100">{deleteCandidate?.name}</span>?
            </p>
            <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
              {t("admin:tagsPage.archiveHelper")}
            </p>
          </div>

          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <Button type="button" variant="outline" onClick={() => setDeleteCandidate(null)}>
              {t("common:cancel")}
            </Button>
            <Button type="button" variant="danger" isLoading={deleteMutation.isPending} onClick={confirmDelete}>
              <Archive className="size-4" />
              {t("admin:tagsPage.archiveTag")}
            </Button>
          </div>
        </div>
      </Modal>
    </>
  );
}

