import { useEffect, useMemo, useState } from "react";
import {
  Archive,
  CheckCircle2,
  ListChecks,
  Pencil,
  Plus,
  Settings2,
  Trash2,
} from "lucide-react";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";
import type { CategorySchemaFieldResponse, CategorySchemaResponse } from "@/api/generated/types.ts";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Modal } from "@/components/ui/Modal";
import { Skeleton } from "@/components/ui/Skeleton";
import { parseApiError } from "@/utils";
import { useAdminCategories } from "@/features/admin/useAdminCategories.ts";
import {
  useActivateAdminCategorySchema,
  useAdminCategorySchema,
  useAdminCategorySchemas,
  useDeleteAdminCategorySchema,
  useDeleteAdminCategorySchemaField,
} from "@/features/admin/useAdminCategorySchemas.ts";
import { AdminPageShell, AdminSurface, AdminToolbar } from "./components/AdminPageShell";
import { SchemaFormModal } from "./categorySchemas/SchemaFormModal";
import { FieldFormModal } from "./categorySchemas/FieldFormModal";
import { FieldOptionsModal } from "./categorySchemas/FieldOptionsModal";

const SELECT_FIELD_TYPES = new Set(["SINGLE_SELECT", "MULTI_SELECT"]);

function statusBadgeVariant(status: CategorySchemaResponse["status"]): "success" | "secondary" | "default" {
  if (status === "ACTIVE") return "success";
  if (status === "DRAFT") return "secondary";
  return "default";
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "—";
  }
  return new Date(value).toLocaleString();
}

export function AdminCategorySchemasPage() {
  const { t } = useTranslation(["admin", "common", "catalog"]);

  const [selectedCategoryUuid, setSelectedCategoryUuid] = useState<string>("");
  const [selectedSchemaUuid, setSelectedSchemaUuid] = useState<string | null>(null);
  const [schemaModalMode, setSchemaModalMode] = useState<"create" | "edit" | null>(null);
  const [editingSchema, setEditingSchema] = useState<CategorySchemaResponse | null>(null);
  const [deleteSchemaCandidate, setDeleteSchemaCandidate] = useState<CategorySchemaResponse | null>(null);
  const [activateCandidate, setActivateCandidate] = useState<CategorySchemaResponse | null>(null);

  const [fieldModalMode, setFieldModalMode] = useState<"create" | "edit" | null>(null);
  const [editingField, setEditingField] = useState<CategorySchemaFieldResponse | null>(null);
  const [deleteFieldCandidate, setDeleteFieldCandidate] = useState<CategorySchemaFieldResponse | null>(null);
  const [optionsField, setOptionsField] = useState<CategorySchemaFieldResponse | null>(null);

  const categoriesQuery = useAdminCategories({ page: 0, size: 100, sort: "name,asc", includeDeleted: false });
  const categories = categoriesQuery.data?.content ?? [];

  const schemasQuery = useAdminCategorySchemas(
    { categoryUuid: selectedCategoryUuid || undefined, size: 50, sort: "version,desc" },
    !!selectedCategoryUuid
  );
  const schemas = schemasQuery.data?.content ?? [];

  const schemaDetailQuery = useAdminCategorySchema(selectedSchemaUuid ?? "", !!selectedSchemaUuid);
  const selectedSchema = useMemo(() => {
    if (!selectedSchemaUuid) {
      return null;
    }
    return schemaDetailQuery.data ?? schemas.find((schema) => schema.uuid === selectedSchemaUuid) ?? null;
  }, [schemaDetailQuery.data, schemas, selectedSchemaUuid]);

  useEffect(() => {
    setSelectedSchemaUuid(null);
  }, [selectedCategoryUuid]);

  const deleteSchemaMutation = useDeleteAdminCategorySchema();
  const activateSchemaMutation = useActivateAdminCategorySchema();
  const deleteFieldMutation = useDeleteAdminCategorySchemaField();

  const openCreateSchemaModal = () => {
    setSchemaModalMode("create");
    setEditingSchema(null);
  };

  const openEditSchemaModal = (schema: CategorySchemaResponse) => {
    setSchemaModalMode("edit");
    setEditingSchema(schema);
  };

  const closeSchemaModal = () => {
    setSchemaModalMode(null);
    setEditingSchema(null);
  };

  const confirmDeleteSchema = () => {
    if (!deleteSchemaCandidate) {
      return;
    }
    deleteSchemaMutation.mutate(deleteSchemaCandidate.uuid, {
      onSuccess: () => {
        toast.success(t("admin:categorySchemasPage.schemaDeleted", { name: deleteSchemaCandidate.name }));
        if (selectedSchemaUuid === deleteSchemaCandidate.uuid) {
          setSelectedSchemaUuid(null);
        }
        setDeleteSchemaCandidate(null);
      },
      onError: (error) => {
        toast.error(parseApiError(error));
      },
    });
  };

  const confirmActivateSchema = () => {
    if (!activateCandidate) {
      return;
    }
    activateSchemaMutation.mutate(activateCandidate.uuid, {
      onSuccess: (schema) => {
        toast.success(t("admin:categorySchemasPage.activated", { name: schema.name }));
        setActivateCandidate(null);
      },
      onError: (error) => {
        toast.error(parseApiError(error));
      },
    });
  };

  const openCreateFieldModal = () => {
    setFieldModalMode("create");
    setEditingField(null);
  };

  const openEditFieldModal = (field: CategorySchemaFieldResponse) => {
    setFieldModalMode("edit");
    setEditingField(field);
  };

  const closeFieldModal = () => {
    setFieldModalMode(null);
    setEditingField(null);
  };

  const confirmDeleteField = () => {
    if (!deleteFieldCandidate || !selectedSchemaUuid) {
      return;
    }
    deleteFieldMutation.mutate(
      { schemaUuid: selectedSchemaUuid, fieldUuid: deleteFieldCandidate.uuid },
      {
        onSuccess: () => {
          toast.success(t("admin:categorySchemasPage.fieldDeleted", { label: deleteFieldCandidate.label }));
          setDeleteFieldCandidate(null);
        },
        onError: (error) => {
          toast.error(parseApiError(error));
        },
      }
    );
  };

  const sortedFields = useMemo(
    () => (selectedSchema ? [...selectedSchema.fields].sort((a, b) => a.displayOrder - b.displayOrder) : []),
    [selectedSchema]
  );

  const isFieldFormModalOpen = fieldModalMode !== null && !!selectedSchemaUuid;
  const isSchemaFormModalOpen = schemaModalMode !== null && !!selectedCategoryUuid;

  return (
    <>
      <AdminPageShell
        title={t("admin:categorySchemasPage.title")}
        description={t("admin:categorySchemasPage.description")}
        badges={
          <>
            <Badge variant="primary">{t("admin:categorySchemasPage.taxonomy")}</Badge>
            {selectedCategoryUuid && (
              <Badge>{t("admin:categorySchemasPage.schemaCount", { count: schemas.length })}</Badge>
            )}
          </>
        }
        actions={
          <Button type="button" onClick={openCreateSchemaModal} disabled={!selectedCategoryUuid}>
            <Plus className="size-4" />
            {t("admin:categorySchemasPage.createSchema")}
          </Button>
        }
      >
        <AdminToolbar>
          <div className="flex w-full flex-col gap-3 md:max-w-md">
            <label className="text-sm font-medium text-slate-700 dark:text-slate-300">
              {t("admin:categorySchemasPage.selectCategoryLabel")}
            </label>
            <select
              value={selectedCategoryUuid}
              onChange={(event) => setSelectedCategoryUuid(event.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm text-slate-900 outline-none transition-colors focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:focus:border-indigo-400 dark:focus:ring-indigo-400/20"
              aria-label={t("admin:categorySchemasPage.selectCategoryLabel")}
            >
              <option value="">{t("admin:categorySchemasPage.selectCategoryPlaceholder")}</option>
              {categories.map((category) => (
                <option key={category.uuid} value={category.uuid}>
                  {category.name}
                </option>
              ))}
            </select>
          </div>
        </AdminToolbar>

        <AdminSurface
          title={t("admin:categorySchemasPage.directoryTitle")}
          description={t("admin:categorySchemasPage.directoryDescription")}
        >
          {!selectedCategoryUuid ? (
            <EmptyState
              icon={<Settings2 className="size-16" />}
              title={t("admin:categorySchemasPage.selectCategoryPrompt")}
              description={t("admin:categorySchemasPage.selectCategoryPromptDescription")}
            />
          ) : schemasQuery.isLoading ? (
            <div className="space-y-3">
              {[...Array(3)].map((_, index) => (
                <Skeleton key={index} className="h-24 w-full" />
              ))}
            </div>
          ) : schemasQuery.isError ? (
            <EmptyState
              icon={<Settings2 className="size-16" />}
              title={t("admin:categorySchemasPage.loadErrorTitle")}
              description={parseApiError(schemasQuery.error)}
              action={
                <Button type="button" variant="outline" onClick={() => schemasQuery.refetch()}>
                  {t("common:tryAgain")}
                </Button>
              }
            />
          ) : schemas.length === 0 ? (
            <EmptyState
              icon={<Settings2 className="size-16" />}
              title={t("admin:categorySchemasPage.emptyTitle")}
              description={t("admin:categorySchemasPage.emptyDescription")}
              action={
                <Button type="button" onClick={openCreateSchemaModal}>
                  <Plus className="size-4" />
                  {t("admin:categorySchemasPage.createSchema")}
                </Button>
              }
            />
          ) : (
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {schemas.map((schema) => {
                const isSelected = schema.uuid === selectedSchemaUuid;
                const isActive = schema.status === "ACTIVE";
                return (
                  <button
                    type="button"
                    key={schema.uuid}
                    onClick={() => setSelectedSchemaUuid(schema.uuid)}
                    className={`flex flex-col gap-3 rounded-2xl border p-4 text-left transition-colors ${
                      isSelected
                        ? "border-indigo-400 bg-indigo-50 dark:border-indigo-500 dark:bg-indigo-950/30"
                        : "border-slate-200 bg-white hover:border-indigo-300 dark:border-slate-800 dark:bg-slate-900 dark:hover:border-indigo-500/60"
                    }`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0">
                        <p className="truncate font-semibold text-slate-900 dark:text-slate-100">{schema.name}</p>
                        <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                          {t("admin:categorySchemasPage.version")} {schema.version}
                        </p>
                      </div>
                      <Badge variant={statusBadgeVariant(schema.status)}>
                        {t(`admin:categorySchemasPage.statusLabels.${schema.status}`)}
                      </Badge>
                    </div>

                    <p className="text-xs text-slate-500 dark:text-slate-400">
                      {t("admin:categorySchemasPage.fieldCount", { count: schema.fields.length })}
                    </p>
                    <p className="text-xs text-slate-400 dark:text-slate-500">{formatDateTime(schema.createdAt)}</p>

                    <div className="mt-1 flex flex-wrap items-center gap-2" onClick={(event) => event.stopPropagation()}>
                      <Button type="button" variant="ghost" size="sm" onClick={() => openEditSchemaModal(schema)}>
                        <Pencil className="size-4" />
                        {t("common:edit")}
                      </Button>
                      {!isActive && (
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          onClick={() => setActivateCandidate(schema)}
                        >
                          <CheckCircle2 className="size-4" />
                          {t("admin:categorySchemasPage.activate")}
                        </Button>
                      )}
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        disabled={isActive}
                        title={isActive ? t("admin:categorySchemasPage.deleteDisabledActiveTooltip") : undefined}
                        onClick={() => setDeleteSchemaCandidate(schema)}
                      >
                        <Trash2 className="size-4" />
                        {t("admin:categorySchemasPage.deleteSchema")}
                      </Button>
                    </div>
                  </button>
                );
              })}
            </div>
          )}
        </AdminSurface>

        {selectedSchema && (
          <AdminSurface
            title={t("admin:categorySchemasPage.fieldsPanelTitle", { name: selectedSchema.name })}
            description={t("admin:categorySchemasPage.fieldsPanelDescription")}
            actions={
              <Button type="button" size="sm" onClick={openCreateFieldModal}>
                <Plus className="size-4" />
                {t("admin:categorySchemasPage.addField")}
              </Button>
            }
            contentClassName="space-y-0"
          >
            {sortedFields.length === 0 ? (
              <EmptyState
                icon={<ListChecks className="size-16" />}
                title={t("admin:categorySchemasPage.noFieldsYet")}
                description={t("admin:categorySchemasPage.noFieldsYetDescription")}
                action={
                  <Button type="button" onClick={openCreateFieldModal}>
                    <Plus className="size-4" />
                    {t("admin:categorySchemasPage.addField")}
                  </Button>
                }
              />
            ) : (
              <div className="overflow-x-auto rounded-2xl border border-slate-200 dark:border-slate-800">
                <table className="w-full min-w-[820px]">
                  <thead className="bg-slate-50 dark:bg-slate-800/50">
                    <tr>
                      <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-700 dark:text-slate-300">
                        {t("admin:categorySchemasPage.fieldLabel")}
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-700 dark:text-slate-300">
                        {t("admin:categorySchemasPage.fieldKey")}
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-700 dark:text-slate-300">
                        {t("admin:categorySchemasPage.fieldType")}
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-700 dark:text-slate-300">
                        {t("admin:categorySchemasPage.fieldFlags")}
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-700 dark:text-slate-300">
                        {t("common:actions")}
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200 bg-white dark:divide-slate-800 dark:bg-slate-900">
                    {sortedFields.map((field) => {
                      const isSelectType = SELECT_FIELD_TYPES.has(field.fieldType);
                      return (
                        <tr key={field.uuid}>
                          <td className="px-4 py-3 text-sm">
                            <p className="font-medium text-slate-900 dark:text-slate-100">{field.label}</p>
                            {field.labelSr && (
                              <p className="text-xs text-slate-500 dark:text-slate-400">{field.labelSr}</p>
                            )}
                          </td>
                          <td className="px-4 py-3 text-sm">
                            <code className="rounded bg-slate-100 px-1.5 py-0.5 text-xs dark:bg-slate-800">
                              {field.key}
                            </code>
                          </td>
                          <td className="px-4 py-3 text-sm">
                            <Badge>{t(`admin:categorySchemasPage.fieldTypeLabels.${field.fieldType}`)}</Badge>
                          </td>
                          <td className="px-4 py-3 text-xs text-slate-600 dark:text-slate-400">
                            <div className="flex flex-wrap gap-1">
                              {field.required && (
                                <Badge variant="warning">{t("admin:categorySchemasPage.fieldRequired")}</Badge>
                              )}
                              {field.searchable && <Badge>{t("admin:categorySchemasPage.fieldSearchable")}</Badge>}
                              {field.filterable && <Badge>{t("admin:categorySchemasPage.fieldFilterable")}</Badge>}
                              {field.sortable && <Badge>{t("admin:categorySchemasPage.fieldSortable")}</Badge>}
                            </div>
                          </td>
                          <td className="px-4 py-3">
                            <div className="flex flex-wrap items-center gap-1">
                              <Button type="button" variant="ghost" size="sm" onClick={() => openEditFieldModal(field)}>
                                <Pencil className="size-4" />
                              </Button>
                              {isSelectType && (
                                <Button
                                  type="button"
                                  variant="ghost"
                                  size="sm"
                                  onClick={() => setOptionsField(field)}
                                >
                                  <ListChecks className="size-4" />
                                  {t("admin:categorySchemasPage.manageOptions")}
                                </Button>
                              )}
                              <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                onClick={() => setDeleteFieldCandidate(field)}
                              >
                                <Trash2 className="size-4" />
                              </Button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </AdminSurface>
        )}
      </AdminPageShell>

      {isSchemaFormModalOpen && (
        <SchemaFormModal
          isOpen={isSchemaFormModalOpen}
          onClose={closeSchemaModal}
          categoryUuid={selectedCategoryUuid}
          schema={schemaModalMode === "edit" ? editingSchema : null}
        />
      )}

      {isFieldFormModalOpen && selectedSchemaUuid && (
        <FieldFormModal
          isOpen={isFieldFormModalOpen}
          onClose={closeFieldModal}
          schemaUuid={selectedSchemaUuid}
          field={fieldModalMode === "edit" ? editingField : null}
        />
      )}

      {selectedSchemaUuid && (
        <FieldOptionsModal
          isOpen={!!optionsField}
          onClose={() => setOptionsField(null)}
          schemaUuid={selectedSchemaUuid}
          field={optionsField}
        />
      )}

      <Modal
        isOpen={!!deleteSchemaCandidate}
        onClose={() => setDeleteSchemaCandidate(null)}
        size="sm"
        title={t("admin:categorySchemasPage.deleteSchema")}
      >
        <div className="space-y-4">
          <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800 dark:border-amber-900/50 dark:bg-amber-950/30 dark:text-amber-200">
            {t("admin:categorySchemasPage.deleteSchemaHelper")}
          </div>
          <p className="text-sm text-slate-700 dark:text-slate-300">
            {t("admin:categorySchemasPage.deleteSchemaConfirm", { name: deleteSchemaCandidate?.name })}
          </p>
          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <Button type="button" variant="outline" onClick={() => setDeleteSchemaCandidate(null)}>
              {t("common:cancel")}
            </Button>
            <Button
              type="button"
              variant="danger"
              isLoading={deleteSchemaMutation.isPending}
              onClick={confirmDeleteSchema}
            >
              <Archive className="size-4" />
              {t("admin:categorySchemasPage.deleteSchema")}
            </Button>
          </div>
        </div>
      </Modal>

      <Modal
        isOpen={!!activateCandidate}
        onClose={() => setActivateCandidate(null)}
        size="sm"
        title={t("admin:categorySchemasPage.activateConfirmTitle")}
      >
        <div className="space-y-4">
          <p className="text-sm text-slate-700 dark:text-slate-300">
            {t("admin:categorySchemasPage.activateConfirmDescription", { name: activateCandidate?.name })}
          </p>
          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <Button type="button" variant="outline" onClick={() => setActivateCandidate(null)}>
              {t("common:cancel")}
            </Button>
            <Button
              type="button"
              isLoading={activateSchemaMutation.isPending}
              onClick={confirmActivateSchema}
            >
              <CheckCircle2 className="size-4" />
              {t("admin:categorySchemasPage.activate")}
            </Button>
          </div>
        </div>
      </Modal>

      <Modal
        isOpen={!!deleteFieldCandidate}
        onClose={() => setDeleteFieldCandidate(null)}
        size="sm"
        title={t("admin:categorySchemasPage.deleteField")}
      >
        <div className="space-y-4">
          <p className="text-sm text-slate-700 dark:text-slate-300">
            {t("admin:categorySchemasPage.deleteFieldConfirm", { label: deleteFieldCandidate?.label })}
          </p>
          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <Button type="button" variant="outline" onClick={() => setDeleteFieldCandidate(null)}>
              {t("common:cancel")}
            </Button>
            <Button
              type="button"
              variant="danger"
              isLoading={deleteFieldMutation.isPending}
              onClick={confirmDeleteField}
            >
              <Trash2 className="size-4" />
              {t("admin:categorySchemasPage.deleteField")}
            </Button>
          </div>
        </div>
      </Modal>
    </>
  );
}

