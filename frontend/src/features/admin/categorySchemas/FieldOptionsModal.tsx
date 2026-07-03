import React, { useEffect, useState } from "react";
import { AxiosError } from "axios";
import { Pencil, Plus, Trash2, X } from "lucide-react";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";
import type { CategorySchemaFieldResponse, ErrorResponse, FieldOptionResponse } from "@/api/generated/types.ts";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { parseApiError } from "@/utils";
import {
  useCreateAdminCategorySchemaFieldOption,
  useDeleteAdminCategorySchemaFieldOption,
  useUpdateAdminCategorySchemaFieldOption,
} from "@/features/admin/useAdminCategorySchemas.ts";

interface OptionFormValues {
  value: string;
  label: string;
  labelSr: string;
  displayOrder: string;
}

const EMPTY_VALUES: OptionFormValues = { value: "", label: "", labelSr: "", displayOrder: "0" };

function mapOptionToFormValues(option?: FieldOptionResponse | null): OptionFormValues {
  if (!option) {
    return EMPTY_VALUES;
  }
  return {
    value: option.value,
    label: option.label,
    labelSr: option.labelSr ?? "",
    displayOrder: String(option.displayOrder ?? 0),
  };
}

interface FieldOptionsModalProps {
  isOpen: boolean;
  onClose: () => void;
  schemaUuid: string;
  field: CategorySchemaFieldResponse | null;
}

export function FieldOptionsModal({ isOpen, onClose, schemaUuid, field }: FieldOptionsModalProps) {
  const { t } = useTranslation(["admin", "common"]);
  const [editingOption, setEditingOption] = useState<FieldOptionResponse | null>(null);
  const [isAdding, setIsAdding] = useState(false);
  const [values, setValues] = useState<OptionFormValues>(EMPTY_VALUES);
  const [formError, setFormError] = useState<string | undefined>(undefined);
  const [deleteCandidate, setDeleteCandidate] = useState<FieldOptionResponse | null>(null);

  const createMutation = useCreateAdminCategorySchemaFieldOption();
  const updateMutation = useUpdateAdminCategorySchemaFieldOption();
  const deleteMutation = useDeleteAdminCategorySchemaFieldOption();

  useEffect(() => {
    if (!isOpen) {
      setIsAdding(false);
      setEditingOption(null);
      setValues(EMPTY_VALUES);
      setFormError(undefined);
      setDeleteCandidate(null);
    }
  }, [isOpen]);

  if (!field) {
    return null;
  }

  const options = [...field.options].sort((a, b) => a.displayOrder - b.displayOrder);
  const isFormOpen = isAdding || !!editingOption;
  const isSubmitting = createMutation.isPending || updateMutation.isPending;

  const openAddForm = () => {
    setIsAdding(true);
    setEditingOption(null);
    setValues(EMPTY_VALUES);
    setFormError(undefined);
  };

  const openEditForm = (option: FieldOptionResponse) => {
    setEditingOption(option);
    setIsAdding(false);
    setValues(mapOptionToFormValues(option));
    setFormError(undefined);
  };

  const closeForm = () => {
    setIsAdding(false);
    setEditingOption(null);
    setValues(EMPTY_VALUES);
    setFormError(undefined);
  };

  const handleChange = (fieldName: keyof OptionFormValues, value: string) => {
    setValues((previous) => ({ ...previous, [fieldName]: value }));
    setFormError(undefined);
  };

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const value = values.value.trim();
    const label = values.label.trim();
    const labelSr = values.labelSr.trim();
    const displayOrderValue = values.displayOrder.trim();
    const parsedDisplayOrder = Number(displayOrderValue);

    if (!value || !label) {
      setFormError(t("admin:categorySchemasPage.optionValueLabelRequired"));
      return;
    }

    if (!displayOrderValue || !Number.isInteger(parsedDisplayOrder)) {
      setFormError(t("admin:categorySchemasPage.fieldDisplayOrderInvalid"));
      return;
    }

    if (editingOption) {
      updateMutation.mutate(
        {
          schemaUuid,
          optionUuid: editingOption.uuid,
          data: { value, label, labelSr: labelSr || null, displayOrder: parsedDisplayOrder },
        },
        {
          onSuccess: () => {
            toast.success(t("admin:categorySchemasPage.optionUpdated", { label }));
            closeForm();
          },
          onError: (error) => {
            const message =
              (error instanceof AxiosError && (error.response?.data as ErrorResponse | undefined)?.message) ||
              parseApiError(error);
            setFormError(message);
            toast.error(message);
          },
        }
      );
      return;
    }

    createMutation.mutate(
      {
        schemaUuid,
        fieldUuid: field.uuid,
        data: { value, label, labelSr: labelSr || null, displayOrder: parsedDisplayOrder },
      },
      {
        onSuccess: () => {
          toast.success(t("admin:categorySchemasPage.optionCreated", { label }));
          closeForm();
        },
        onError: (error) => {
          const message =
            (error instanceof AxiosError && (error.response?.data as ErrorResponse | undefined)?.message) ||
            parseApiError(error);
          setFormError(message);
          toast.error(message);
        },
      }
    );
  };

  const confirmDelete = () => {
    if (!deleteCandidate) {
      return;
    }

    deleteMutation.mutate(
      { schemaUuid, optionUuid: deleteCandidate.uuid },
      {
        onSuccess: () => {
          toast.success(t("admin:categorySchemasPage.optionDeleted", { label: deleteCandidate.label }));
          setDeleteCandidate(null);
        },
        onError: (error) => {
          toast.error(parseApiError(error));
        },
      }
    );
  };

  return (
    <>
      <Modal
        isOpen={isOpen}
        onClose={onClose}
        size="lg"
        title={t("admin:categorySchemasPage.optionsModalTitle", { label: field.label })}
      >
        <div className="space-y-4">
          <p className="text-sm text-slate-600 dark:text-slate-400">
            {t("admin:categorySchemasPage.optionsModalDescription")}
          </p>

          {!isFormOpen && (
            <Button type="button" onClick={openAddForm} size="sm">
              <Plus className="size-4" />
              {t("admin:categorySchemasPage.addOption")}
            </Button>
          )}

          {isFormOpen && (
            <form
              onSubmit={handleSubmit}
              className="space-y-3 rounded-xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-800/60"
            >
              <div className="grid gap-3 md:grid-cols-2">
                <Input
                  label={t("admin:categorySchemasPage.optionValue")}
                  value={values.value}
                  onChange={(event) => handleChange("value", event.target.value)}
                  placeholder="e.g. blue"
                  autoFocus
                />
                <Input
                  label={t("admin:categorySchemasPage.optionDisplayOrder")}
                  type="number"
                  inputMode="numeric"
                  value={values.displayOrder}
                  onChange={(event) => handleChange("displayOrder", event.target.value)}
                  placeholder="0"
                />
              </div>
              <div className="grid gap-3 md:grid-cols-2">
                <Input
                  label={t("admin:categorySchemasPage.optionLabel")}
                  value={values.label}
                  onChange={(event) => handleChange("label", event.target.value)}
                  placeholder="e.g. Blue"
                />
                <Input
                  label={t("admin:categorySchemasPage.optionLabelSr")}
                  value={values.labelSr}
                  onChange={(event) => handleChange("labelSr", event.target.value)}
                  placeholder="npr. Plava"
                />
              </div>

              {formError && (
                <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-300">
                  {formError}
                </div>
              )}

              <div className="flex justify-end gap-2">
                <Button type="button" variant="outline" size="sm" onClick={closeForm}>
                  {t("common:cancel")}
                </Button>
                <Button type="submit" size="sm" isLoading={isSubmitting}>
                  {editingOption ? t("admin:saveChanges") : t("admin:categorySchemasPage.addOption")}
                </Button>
              </div>
            </form>
          )}

          <div className="overflow-hidden rounded-xl border border-slate-200 dark:border-slate-800">
            <table className="w-full">
              <thead className="bg-slate-50 dark:bg-slate-800/50">
                <tr>
                  <th className="px-4 py-2 text-left text-xs font-medium uppercase tracking-wider text-slate-700 dark:text-slate-300">
                    {t("admin:categorySchemasPage.optionValue")}
                  </th>
                  <th className="px-4 py-2 text-left text-xs font-medium uppercase tracking-wider text-slate-700 dark:text-slate-300">
                    {t("admin:categorySchemasPage.optionLabel")}
                  </th>
                  <th className="px-4 py-2 text-left text-xs font-medium uppercase tracking-wider text-slate-700 dark:text-slate-300">
                    {t("admin:categorySchemasPage.optionDisplayOrder")}
                  </th>
                  <th className="px-4 py-2 text-left text-xs font-medium uppercase tracking-wider text-slate-700 dark:text-slate-300">
                    {t("common:actions")}
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 bg-white dark:divide-slate-800 dark:bg-slate-900">
                {options.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="px-4 py-6 text-center text-sm text-slate-500 dark:text-slate-400">
                      {t("admin:categorySchemasPage.noOptionsYet")}
                    </td>
                  </tr>
                ) : (
                  options.map((option) => (
                    <tr key={option.uuid}>
                      <td className="px-4 py-3 text-sm">
                        <code className="rounded bg-slate-100 px-1.5 py-0.5 text-xs dark:bg-slate-800">
                          {option.value}
                        </code>
                      </td>
                      <td className="px-4 py-3 text-sm text-slate-900 dark:text-slate-100">
                        <p>{option.label}</p>
                        {option.labelSr && (
                          <p className="text-xs text-slate-500 dark:text-slate-400">{option.labelSr}</p>
                        )}
                      </td>
                      <td className="px-4 py-3 text-sm text-slate-700 dark:text-slate-300">{option.displayOrder}</td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-1">
                          <Button type="button" variant="ghost" size="sm" onClick={() => openEditForm(option)}>
                            <Pencil className="size-4" />
                          </Button>
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={() => setDeleteCandidate(option)}
                          >
                            <Trash2 className="size-4" />
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          <div className="flex justify-end">
            <Button type="button" variant="outline" onClick={onClose}>
              <X className="size-4" />
              {t("common:close")}
            </Button>
          </div>
        </div>
      </Modal>

      <Modal
        isOpen={!!deleteCandidate}
        onClose={() => setDeleteCandidate(null)}
        size="sm"
        title={t("admin:categorySchemasPage.deleteOption")}
      >
        <div className="space-y-4">
          <p className="text-sm text-slate-700 dark:text-slate-300">
            {t("admin:categorySchemasPage.deleteOptionConfirm", { label: deleteCandidate?.label })}
          </p>
          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <Button type="button" variant="outline" onClick={() => setDeleteCandidate(null)}>
              {t("common:cancel")}
            </Button>
            <Button type="button" variant="danger" isLoading={deleteMutation.isPending} onClick={confirmDelete}>
              <Trash2 className="size-4" />
              {t("admin:categorySchemasPage.deleteOption")}
            </Button>
          </div>
        </div>
      </Modal>
    </>
  );
}

