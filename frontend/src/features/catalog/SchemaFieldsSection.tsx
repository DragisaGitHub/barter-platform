import type { CategoryFormFieldResponse } from "@/api/generated/types.ts";
import type { DynamicFieldValue, DynamicFieldValues } from "./schemaFieldValues";

interface SchemaFieldsSectionProps {
  fields: CategoryFormFieldResponse[];
  isLoading: boolean;
  hasCategory: boolean;
  values: DynamicFieldValues;
  errors: Record<string, string>;
  onChange: (fieldUuid: string, value: DynamicFieldValue) => void;
  t: (key: string, options?: Record<string, unknown>) => string;
}

/**
 * Renders the dynamic category-schema fields section below the basic item form fields.
 * Handles loading and empty (no active schema) states generically, with no category-specific logic.
 */
export function SchemaFieldsSection({
  fields,
  isLoading,
  hasCategory,
  values,
  errors,
  onChange,
  t,
}: SchemaFieldsSectionProps) {
  if (!hasCategory) {
    return null;
  }

  if (isLoading) {
    return (
      <div
        className="rounded-2xl border border-teal-100 bg-teal-50/60 p-4 text-sm text-slate-500 dark:border-teal-900/40 dark:bg-teal-950/20 dark:text-slate-400"
        data-testid="schema-fields-loading"
      >
        {t("catalog:itemForm.schemaFields.loading")}
      </div>
    );
  }

  if (fields.length === 0) {
    return (
      <div
        className="rounded-2xl border border-dashed border-slate-200 bg-slate-50/50 p-4 text-sm text-slate-500 dark:border-slate-700 dark:bg-slate-900/20 dark:text-slate-400"
        data-testid="schema-fields-empty"
      >
        {t("catalog:itemForm.schemaFields.empty")}
      </div>
    );
  }

  return (
    <div
      className="rounded-2xl border border-teal-100 bg-teal-50/60 p-4 dark:border-teal-900/40 dark:bg-teal-950/20 space-y-4"
      data-testid="schema-fields-section"
    >
      <div>
        <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">
          {t("catalog:itemForm.schemaFields.title")}
        </h3>
        <p className="mt-1 text-sm leading-6 text-slate-600 dark:text-slate-300">
          {t("catalog:itemForm.schemaFields.helper")}
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        {fields.map((field) => (
          <SchemaField
            key={field.fieldUuid}
            field={field}
            value={values[field.fieldUuid] ?? {}}
            error={errors[field.fieldUuid]}
            onChange={(value) => onChange(field.fieldUuid, value)}
            t={t}
          />
        ))}
      </div>
    </div>
  );
}

const inputClassName =
  "w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-500/20";
const labelClassName = "block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5";

function SchemaField({
  field,
  value,
  error,
  onChange,
  t,
}: {
  field: CategoryFormFieldResponse;
  value: DynamicFieldValue;
  error?: string;
  onChange: (value: DynamicFieldValue) => void;
  t: (key: string, options?: Record<string, unknown>) => string;
}) {
  const labelText = `${field.label}${field.required ? " *" : ""}${field.unit ? ` (${field.unit})` : ""}`;
  const errorMessage = error ? t(`catalog:${error}`) : undefined;
  const testId = `schema-field-${field.key}`;

  switch (field.fieldType) {
    case "TEXT":
      return (
        <div>
          <label className={labelClassName} htmlFor={testId}>
            {labelText}
          </label>
          <input
            id={testId}
            data-testid={testId}
            type="text"
            className={inputClassName}
            value={value.valueText ?? ""}
            onChange={(e) => onChange({ valueText: e.target.value })}
          />
          {field.helpText ? (
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{field.helpText}</p>
          ) : null}
          {errorMessage ? <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">{errorMessage}</p> : null}
        </div>
      );

    case "NUMBER":
      return (
        <div>
          <label className={labelClassName} htmlFor={testId}>
            {labelText}
          </label>
          <input
            id={testId}
            data-testid={testId}
            type="number"
            className={inputClassName}
            value={value.valueNumber ?? ""}
            onChange={(e) =>
              onChange({ valueNumber: e.target.value === "" ? undefined : Number(e.target.value) })
            }
          />
          {field.helpText ? (
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{field.helpText}</p>
          ) : null}
          {errorMessage ? <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">{errorMessage}</p> : null}
        </div>
      );

    case "BOOLEAN":
      return (
        <div className="flex flex-col justify-center">
          <label className="inline-flex items-center gap-2 text-sm font-medium text-slate-700 dark:text-slate-300">
            <input
              data-testid={testId}
              type="checkbox"
              className="size-4 rounded border-slate-300 text-teal-600 focus:ring-teal-500"
              checked={value.valueBoolean ?? false}
              onChange={(e) => onChange({ valueBoolean: e.target.checked })}
            />
            {labelText}
          </label>
          {field.helpText ? (
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{field.helpText}</p>
          ) : null}
          {errorMessage ? <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">{errorMessage}</p> : null}
        </div>
      );

    case "DATE":
      return (
        <div>
          <label className={labelClassName} htmlFor={testId}>
            {labelText}
          </label>
          <input
            id={testId}
            data-testid={testId}
            type="date"
            className={inputClassName}
            value={value.valueDate ?? ""}
            onChange={(e) => onChange({ valueDate: e.target.value || undefined })}
          />
          {field.helpText ? (
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{field.helpText}</p>
          ) : null}
          {errorMessage ? <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">{errorMessage}</p> : null}
        </div>
      );

    case "SINGLE_SELECT":
      return (
        <div>
          <label className={labelClassName} htmlFor={testId}>
            {labelText}
          </label>
          <select
            id={testId}
            data-testid={testId}
            className={inputClassName}
            value={value.optionUuid ?? ""}
            onChange={(e) => onChange({ optionUuid: e.target.value || undefined })}
          >
            <option value="">{t("catalog:itemForm.schemaFields.selectOption")}</option>
            {field.options.map((option) => (
              <option key={option.optionUuid} value={option.optionUuid}>
                {option.label}
              </option>
            ))}
          </select>
          {field.options.length === 0 ? (
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
              {t("catalog:itemForm.schemaFields.noOptions")}
            </p>
          ) : null}
          {field.helpText ? (
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{field.helpText}</p>
          ) : null}
          {errorMessage ? <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">{errorMessage}</p> : null}
        </div>
      );

    case "MULTI_SELECT": {
      const selected = value.optionUuids ?? [];
      const toggle = (optionUuid: string) => {
        if (selected.includes(optionUuid)) {
          onChange({ optionUuids: selected.filter((id) => id !== optionUuid) });
        } else {
          onChange({ optionUuids: [...selected, optionUuid] });
        }
      };
      return (
        <div data-testid={testId}>
          <label className={labelClassName}>{labelText}</label>
          <div className="flex flex-wrap gap-2">
            {field.options.map((option) => (
              <button
                key={option.optionUuid}
                type="button"
                onClick={() => toggle(option.optionUuid)}
                className={`rounded-full border px-3 py-1.5 text-sm transition-colors ${
                  selected.includes(option.optionUuid)
                    ? "border-teal-600 bg-teal-600 text-white"
                    : "border-slate-300 bg-white text-slate-700 hover:border-teal-400 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-300"
                }`}
              >
                {option.label}
              </button>
            ))}
            {field.options.length === 0 ? (
              <p className="text-xs text-slate-500 dark:text-slate-400">
                {t("catalog:itemForm.schemaFields.noOptions")}
              </p>
            ) : null}
          </div>
          {field.helpText ? (
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{field.helpText}</p>
          ) : null}
          {errorMessage ? <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">{errorMessage}</p> : null}
        </div>
      );
    }

    default:
      return null;
  }
}

