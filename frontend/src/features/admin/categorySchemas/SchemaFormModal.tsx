import React, { useEffect, useState } from "react";
import { AxiosError } from "axios";
import { Pencil, Plus } from "lucide-react";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";
import type { CategorySchemaResponse, ErrorResponse } from "@/api/generated/types.ts";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { parseApiError } from "@/utils";
import {
  useCreateAdminCategorySchema,
  useUpdateAdminCategorySchema,
} from "@/features/admin/useAdminCategorySchemas.ts";

interface SchemaFormValues {
  name: string;
  description: string;
}

interface SchemaFormErrors {
  name?: string;
  description?: string;
  form?: string;
}

const EMPTY_VALUES: SchemaFormValues = { name: "", description: "" };

function mapSchemaToFormValues(schema?: CategorySchemaResponse | null): SchemaFormValues {
  if (!schema) {
    return EMPTY_VALUES;
  }
  return {
    name: schema.name,
    description: schema.description ?? "",
  };
}

function mapApiErrors(error: unknown): SchemaFormErrors {
  const nextErrors: SchemaFormErrors = {};

  if (error instanceof AxiosError) {
    const response = error.response?.data as ErrorResponse | undefined;
    if (response?.fieldErrors?.length) {
      response.fieldErrors.forEach((fieldError) => {
        const field = fieldError.field as keyof SchemaFormErrors;
        if (["name", "description", "form"].includes(field)) {
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

interface SchemaFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  categoryUuid: string;
  schema?: CategorySchemaResponse | null;
}

export function SchemaFormModal({ isOpen, onClose, categoryUuid, schema }: SchemaFormModalProps) {
  const { t } = useTranslation(["admin", "common"]);
  const isEditMode = !!schema;
  const [values, setValues] = useState<SchemaFormValues>(EMPTY_VALUES);
  const [errors, setErrors] = useState<SchemaFormErrors>({});

  const createMutation = useCreateAdminCategorySchema();
  const updateMutation = useUpdateAdminCategorySchema();

  useEffect(() => {
    if (isOpen) {
      setValues(mapSchemaToFormValues(schema));
      setErrors({});
    }
  }, [isOpen, schema]);

  const isSubmitting = createMutation.isPending || updateMutation.isPending;

  const handleChange = (field: keyof SchemaFormValues, value: string) => {
    setValues((previous) => ({ ...previous, [field]: value }));
    setErrors((previous) => ({ ...previous, [field]: undefined, form: undefined }));
  };

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const name = values.name.trim();
    const description = values.description.trim();
    const nextErrors: SchemaFormErrors = {};

    if (!name) {
      nextErrors.name = t("admin:categorySchemasPage.nameRequired");
    }

    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors);
      return;
    }

    const payload = { name, description: description || null };

    if (isEditMode && schema) {
      updateMutation.mutate(
        { schemaUuid: schema.uuid, data: payload },
        {
          onSuccess: (result) => {
            toast.success(t("admin:categorySchemasPage.schemaUpdated", { name: result.name }));
            onClose();
          },
          onError: (error) => {
            const nextErrs = mapApiErrors(error);
            setErrors(nextErrs);
            toast.error(nextErrs.form ?? t("admin:categorySchemasPage.schemaUpdateError"));
          },
        }
      );
      return;
    }

    createMutation.mutate(
      { categoryUuid, data: payload },
      {
        onSuccess: (result) => {
          toast.success(t("admin:categorySchemasPage.schemaCreated", { name: result.name }));
          onClose();
        },
        onError: (error) => {
          const nextErrs = mapApiErrors(error);
          setErrors(nextErrs);
          toast.error(nextErrs.form ?? t("admin:categorySchemasPage.schemaCreateError"));
        },
      }
    );
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      size="md"
      title={isEditMode ? t("admin:categorySchemasPage.editSchema") : t("admin:categorySchemasPage.createSchema")}
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label={t("admin:categorySchemasPage.schemaName")}
          value={values.name}
          onChange={(event) => handleChange("name", event.target.value)}
          error={errors.name}
          placeholder={t("admin:categorySchemasPage.schemaNamePlaceholder")}
          autoFocus
        />

        <div>
          <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">
            {t("admin:categorySchemasPage.schemaDescription")}
          </label>
          <textarea
            value={values.description}
            onChange={(event) => handleChange("description", event.target.value)}
            rows={3}
            placeholder={t("admin:categorySchemasPage.schemaDescriptionPlaceholder")}
            className="w-full rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm text-slate-900 placeholder:text-slate-400 transition-colors duration-150 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:border-indigo-400 dark:focus:ring-indigo-400/20"
          />
          {errors.description && (
            <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">{errors.description}</p>
          )}
        </div>

        {!isEditMode && (
          <p className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-xs text-slate-600 dark:border-slate-700 dark:bg-slate-800/60 dark:text-slate-300">
            {t("admin:categorySchemasPage.newSchemaDraftHelper")}
          </p>
        )}

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
                {t("admin:categorySchemasPage.createSchema")}
              </>
            )}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

