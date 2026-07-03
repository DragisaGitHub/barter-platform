import type {
  CategoryFormFieldResponse,
  SchemaFieldValueRequest,
  SchemaFieldValueResponse,
} from "@/api/generated/types.ts";

/**
 * Local, form-friendly representation of a single dynamic schema field value.
 * Only the properties relevant to the field's fieldType are expected to be populated.
 */
export interface DynamicFieldValue {
  valueText?: string;
  valueNumber?: number;
  valueBoolean?: boolean;
  valueDate?: string;
  optionUuid?: string;
  optionUuids?: string[];
}

export type DynamicFieldValues = Record<string, DynamicFieldValue>;

/** Builds the initial local dynamic field value map from a previously persisted item's values (edit prefill). */
export function buildInitialDynamicValues(
  existing?: SchemaFieldValueResponse[] | null
): DynamicFieldValues {
  const result: DynamicFieldValues = {};
  if (!existing) {
    return result;
  }

  for (const value of existing) {
    const entry: DynamicFieldValue = {};
    if (value.valueText != null) entry.valueText = value.valueText;
    if (value.valueNumber != null) entry.valueNumber = value.valueNumber;
    if (value.valueBoolean != null) entry.valueBoolean = value.valueBoolean;
    if (value.valueDate != null) entry.valueDate = value.valueDate;

    if (value.fieldType === "SINGLE_SELECT") {
      entry.optionUuid = value.options?.[0]?.optionUuid;
    } else if (value.fieldType === "MULTI_SELECT") {
      entry.optionUuids = value.options?.map((option) => option.optionUuid) ?? [];
    }

    result[value.fieldUuid] = entry;
  }
  return result;
}

export interface SchemaFieldValidationResult {
  errors: Record<string, string>;
  requests: SchemaFieldValueRequest[];
}

/**
 * Validates the local dynamic field values against the active category schema fields (required
 * fields, basic type presence) and builds the SchemaFieldValueRequest[] payload for submission.
 * Mirrors the backend's validation semantics at a high level; the backend remains the source of truth.
 */
export function validateAndBuildSchemaFieldValues(
  fields: CategoryFormFieldResponse[],
  values: DynamicFieldValues
): SchemaFieldValidationResult {
  const errors: Record<string, string> = {};
  const requests: SchemaFieldValueRequest[] = [];

  for (const field of fields) {
    const value = values[field.fieldUuid] ?? {};
    let hasValue = false;
    const request: SchemaFieldValueRequest = { fieldUuid: field.fieldUuid };

    switch (field.fieldType) {
      case "TEXT": {
        const trimmed = value.valueText?.trim();
        if (trimmed) {
          request.valueText = trimmed;
          hasValue = true;
        }
        break;
      }
      case "NUMBER": {
        if (value.valueNumber !== undefined && value.valueNumber !== null && !Number.isNaN(value.valueNumber)) {
          request.valueNumber = value.valueNumber;
          hasValue = true;
        }
        break;
      }
      case "BOOLEAN": {
        if (value.valueBoolean !== undefined) {
          request.valueBoolean = value.valueBoolean;
          hasValue = true;
        }
        break;
      }
      case "DATE": {
        if (value.valueDate) {
          request.valueDate = value.valueDate;
          hasValue = true;
        }
        break;
      }
      case "SINGLE_SELECT": {
        if (value.optionUuid) {
          request.optionUuid = value.optionUuid;
          hasValue = true;
        }
        break;
      }
      case "MULTI_SELECT": {
        if (value.optionUuids && value.optionUuids.length > 0) {
          request.optionUuids = value.optionUuids;
          hasValue = true;
        }
        break;
      }
      default:
        break;
    }

    if (!hasValue) {
      if (field.required) {
        errors[field.fieldUuid] = "validation.schemaFieldRequired";
      }
      continue;
    }

    requests.push(request);
  }

  return { errors, requests };
}

