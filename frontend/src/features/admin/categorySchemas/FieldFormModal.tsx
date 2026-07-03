import React, { useEffect, useState } from "react";
import { AxiosError } from "axios";
import { Pencil, Plus } from "lucide-react";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";
import type {
  CategorySchemaFieldResponse,
  CategorySchemaFieldType,
  ErrorResponse,
} from "@/api/generated/types.ts";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { parseApiError } from "@/utils";
import {
  useCreateAdminCategorySchemaField,
  useUpdateAdminCategorySchemaField,
} from "@/features/admin/useAdminCategorySchemas.ts";

const FIELD_TYPES: CategorySchemaFieldType[] = [
  "TEXT",
  "NUMBER",
  "BOOLEAN",
  "SINGLE_SELECT",
  "MULTI_SELECT",
  "DATE",
];

interface FieldFormValues {
  key: string;
  label: string;
  labelSr: string;
  helpText: string;
  fieldType: CategorySchemaFieldType;
  required: boolean;
  searchable: boolean;
  filterable: boolean;
  sortable: boolean;
  unit: string;
  displayOrder: string;
}

interface FieldFormErrors {
  key?: string;
  label?: string;
  labelSr?: string;
  helpText?: string;
  fieldType?: string;
  unit?: string;
  displayOrder?: string;
  form?: string;
}

const EMPTY_VALUES: FieldFormValues = {
  key: "",
  label: "",
  labelSr: "",
  helpText: "",
  fieldType: "TEXT",
  required: false,
  searchable: false,
  filterable: false,
  sortable: false,
  unit: "",
  displayOrder: "0",
};

function mapFieldToFormValues(field?: CategorySchemaFieldResponse | null): FieldFormValues {
  if (!field) {
    return EMPTY_VALUES;
  }
  return {
    key: field.key,
    label: field.label,
    labelSr: field.labelSr ?? "",
    helpText: field.helpText ?? "",
    fieldType: field.fieldType,
    required: field.required,
    searchable: field.searchable,
    filterable: field.filterable,
    sortable: field.sortable,
    unit: field.unit ?? "",
    displayOrder: String(field.displayOrder ?? 0),
  };
}

function mapApiErrors(error: unknown): FieldFormErrors {
  const nextErrors: FieldFormErrors = {};

  if (error instanceof AxiosError) {
    const response = error.response?.data as ErrorResponse | undefined;
    if (response?.fieldErrors?.length) {
      response.fieldErrors.forEach((fieldError) => {
        const field = fieldError.field as keyof FieldFormErrors;
        if (["key", "label", "labelSr", "helpText", "fieldType", "unit", "displayOrder", "form"].includes(field)) {
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

interface FieldFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  schemaUuid: string;
  field?: CategorySchemaFieldResponse | null;
}

export function FieldFormModal({ isOpen, onClose, schemaUuid, field }: FieldFormModalProps) {
  const { t } = useTranslation(["admin", "common"]);
  const isEditMode = !!field;
  const [values, setValues] = useState<FieldFormValues>(EMPTY_VALUES);
  const [errors, setErrors] = useState<FieldFormErrors>({});

  const createMutation = useCreateAdminCategorySchemaField();
  const updateMutation = useUpdateAdminCategorySchemaField();

  useEffect(() => {
    if (isOpen) {
      setValues(mapFieldToFormValues(field));
      setErrors({});
    }
  }, [isOpen, field]);

  const isSubmitting = createMutation.isPending || updateMutation.isPending;

  const handleChange = <K extends keyof FieldFormValues>(field: K, value: FieldFormValues[K]) => {
    setValues((previous) => ({ ...previous, [field]: value }));
    setErrors((previous) => ({ ...previous, [field]: undefined, form: undefined }));
  };

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const key = values.key.trim();
    const label = values.label.trim();
    const labelSr = values.labelSr.trim();
    const helpText = values.helpText.trim();
    const unit = values.unit.trim();
    const displayOrderValue = values.displayOrder.trim();
    const parsedDisplayOrder = Number(displayOrderValue);
    const nextErrors: FieldFormErrors = {};

    if (!isEditMode && !key) {
      nextErrors.key = t("admin:categorySchemasPage.fieldKeyRequired");
    } else if (!isEditMode && !/^[a-z][a-z0-9_]*$/.test(key)) {
      nextErrors.key = t("admin:categorySchemasPage.fieldKeyPattern");
    }

    if (!label) {
      nextErrors.label = t("admin:categorySchemasPage.fieldLabelRequired");
    }

    if (!displayOrderValue || !Number.isInteger(parsedDisplayOrder)) {
      nextErrors.displayOrder = t("admin:categorySchemasPage.fieldDisplayOrderInvalid");
    }

    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors);
      return;
    }

    if (isEditMode && field) {
      updateMutation.mutate(
        {
          schemaUuid,
          fieldUuid: field.uuid,
          data: {
            label,
            labelSr: labelSr || null,
            helpText: helpText || null,
            required: values.required,
            searchable: values.searchable,
            filterable: values.filterable,
            sortable: values.sortable,
            unit: unit || null,
            displayOrder: parsedDisplayOrder,
          },
        },
        {
          onSuccess: () => {
            toast.success(t("admin:categorySchemasPage.fieldUpdated", { label }));
            onClose();
          },
          onError: (error) => {
            const nextErrs = mapApiErrors(error);
            setErrors(nextErrs);
            toast.error(nextErrs.form ?? t("admin:categorySchemasPage.fieldUpdateError"));
          },
        }
      );
      return;
    }

    createMutation.mutate(
      {
        schemaUuid,
        data: {
          key,
          label,
          labelSr: labelSr || null,
          helpText: helpText || null,
          fieldType: values.fieldType,
          required: values.required,
          searchable: values.searchable,
          filterable: values.filterable,
          sortable: values.sortable,
          unit: unit || null,
          displayOrder: parsedDisplayOrder,
        },
      },
      {
        onSuccess: () => {
          toast.success(t("admin:categorySchemasPage.fieldCreated", { label }));
          onClose();
        },
        onError: (error) => {
          const nextErrs = mapApiErrors(error);
          setErrors(nextErrs);
          toast.error(nextErrs.form ?? t("admin:categorySchemasPage.fieldCreateError"));
        },
      }
    );
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      size="lg"
      title={isEditMode ? t("admin:categorySchemasPage.editField") : t("admin:categorySchemasPage.addField")}
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid gap-4 md:grid-cols-2">
          <Input
            label={t("admin:categorySchemasPage.fieldKey")}
            value={values.key}
            onChange={(event) => handleChange("key", event.target.value)}
            error={errors.key}
            placeholder="e.g. brand"
            disabled={isEditMode}
            autoFocus={!isEditMode}
          />

          <div>
            <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">
              {t("admin:categorySchemasPage.fieldType")}
            </label>
            <select
              value={values.fieldType}
              onChange={(event) => handleChange("fieldType", event.target.value as CategorySchemaFieldType)}
              disabled={isEditMode}
              className="w-full rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm text-slate-900 outline-none transition-colors focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 disabled:cursor-not-allowed disabled:bg-slate-50 disabled:text-slate-500 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:focus:border-indigo-400 dark:focus:ring-indigo-400/20"
              aria-label="Field type"
            >
              {FIELD_TYPES.map((type) => (
                <option key={type} value={type}>
                  {t(`admin:categorySchemasPage.fieldTypeLabels.${type}`)}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <Input
            label={t("admin:categorySchemasPage.fieldLabel")}
            value={values.label}
            onChange={(event) => handleChange("label", event.target.value)}
            error={errors.label}
            placeholder="e.g. Brand"
            autoFocus={isEditMode}
          />

          <Input
            label={t("admin:categorySchemasPage.fieldLabelSr")}
            value={values.labelSr}
            onChange={(event) => handleChange("labelSr", event.target.value)}
            error={errors.labelSr}
            placeholder="npr. Brend"
          />
        </div>

        <div>
          <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">
            {t("admin:categorySchemasPage.fieldHelpText")}
          </label>
          <textarea
            value={values.helpText}
            onChange={(event) => handleChange("helpText", event.target.value)}
            rows={2}
            placeholder={t("admin:categorySchemasPage.fieldHelpTextPlaceholder")}
            className="w-full rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm text-slate-900 placeholder:text-slate-400 transition-colors duration-150 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:border-indigo-400 dark:focus:ring-indigo-400/20"
          />
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <Input
            label={t("admin:categorySchemasPage.fieldUnit")}
            value={values.unit}
            onChange={(event) => handleChange("unit", event.target.value)}
            error={errors.unit}
            placeholder="e.g. kg"
          />

          <Input
            label={t("admin:categorySchemasPage.fieldDisplayOrder")}
            type="number"
            inputMode="numeric"
            value={values.displayOrder}
            onChange={(event) => handleChange("displayOrder", event.target.value)}
            error={errors.displayOrder}
            placeholder="0"
          />
        </div>

        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          {(
            [
              ["required", t("admin:categorySchemasPage.fieldRequired")],
              ["searchable", t("admin:categorySchemasPage.fieldSearchable")],
              ["filterable", t("admin:categorySchemasPage.fieldFilterable")],
              ["sortable", t("admin:categorySchemasPage.fieldSortable")],
            ] as const
          ).map(([field, labelText]) => (
            <label
              key={field}
              className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-700 dark:border-slate-700 dark:bg-slate-800/60 dark:text-slate-300"
            >
              <input
                type="checkbox"
                checked={values[field]}
                onChange={(event) => handleChange(field, event.target.checked)}
                className="size-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
              />
              {labelText}
            </label>
          ))}
        </div>

        {errors.form && (
          <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-300">
            {errors.form}
          </div>
        )}

        <div className="flex flex-col-reverse gap-2 pt-2 sm:flex-row sm:justify-end">
          <Button type="button" variant="outline" onClick={onClose}>
            {t("common:cancel")}
          </Button>
          <Button type="submit" isLoading={isSubmitting}>
            {isEditMode ? (
              <>
                <Pencil className="size-4" />
                {t("admin:saveChanges")}
              </>
            ) : (
              <>
                <Plus className="size-4" />
                {t("admin:categorySchemasPage.addField")}
              </>
            )}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

