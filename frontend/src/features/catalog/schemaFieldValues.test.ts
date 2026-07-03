import { describe, it, expect } from "vitest";
import {
  buildInitialDynamicValues,
  validateAndBuildSchemaFieldValues,
  type DynamicFieldValues,
} from "./schemaFieldValues";
import type { CategoryFormFieldResponse, SchemaFieldValueResponse } from "@/api/generated/types.ts";

function makeField(overrides: Partial<CategoryFormFieldResponse>): CategoryFormFieldResponse {
  return {
    fieldUuid: "field-1",
    key: "brand",
    label: "Brand",
    fieldType: "TEXT",
    required: false,
    searchable: false,
    filterable: false,
    sortable: false,
    displayOrder: 0,
    options: [],
    ...overrides,
  };
}

describe("validateAndBuildSchemaFieldValues", () => {
  it("builds a request for a populated TEXT field", () => {
    const fields = [makeField({ fieldUuid: "f1", key: "brand", fieldType: "TEXT" })];
    const values: DynamicFieldValues = { f1: { valueText: "Acme" } };

    const result = validateAndBuildSchemaFieldValues(fields, values);

    expect(result.errors).toEqual({});
    expect(result.requests).toEqual([{ fieldUuid: "f1", valueText: "Acme" }]);
  });

  it("builds a request for a populated NUMBER field", () => {
    const fields = [makeField({ fieldUuid: "f2", key: "weight", fieldType: "NUMBER" })];
    const values: DynamicFieldValues = { f2: { valueNumber: 2.5 } };

    const result = validateAndBuildSchemaFieldValues(fields, values);

    expect(result.requests).toEqual([{ fieldUuid: "f2", valueNumber: 2.5 }]);
  });

  it("builds a request for a populated BOOLEAN field", () => {
    const fields = [makeField({ fieldUuid: "f3", key: "isNew", fieldType: "BOOLEAN" })];
    const values: DynamicFieldValues = { f3: { valueBoolean: true } };

    const result = validateAndBuildSchemaFieldValues(fields, values);

    expect(result.requests).toEqual([{ fieldUuid: "f3", valueBoolean: true }]);
  });

  it("builds a request for a populated DATE field", () => {
    const fields = [makeField({ fieldUuid: "f4", key: "purchaseDate", fieldType: "DATE" })];
    const values: DynamicFieldValues = { f4: { valueDate: "2024-01-15" } };

    const result = validateAndBuildSchemaFieldValues(fields, values);

    expect(result.requests).toEqual([{ fieldUuid: "f4", valueDate: "2024-01-15" }]);
  });

  it("builds a request for a populated SINGLE_SELECT field", () => {
    const fields = [makeField({ fieldUuid: "f5", key: "color", fieldType: "SINGLE_SELECT" })];
    const values: DynamicFieldValues = { f5: { optionUuid: "opt-red" } };

    const result = validateAndBuildSchemaFieldValues(fields, values);

    expect(result.requests).toEqual([{ fieldUuid: "f5", optionUuid: "opt-red" }]);
  });

  it("builds a request for a populated MULTI_SELECT field", () => {
    const fields = [makeField({ fieldUuid: "f6", key: "sizes", fieldType: "MULTI_SELECT" })];
    const values: DynamicFieldValues = { f6: { optionUuids: ["opt-s", "opt-m"] } };

    const result = validateAndBuildSchemaFieldValues(fields, values);

    expect(result.requests).toEqual([{ fieldUuid: "f6", optionUuids: ["opt-s", "opt-m"] }]);
  });

  it("reports an error for a missing required field", () => {
    const fields = [makeField({ fieldUuid: "f1", key: "brand", fieldType: "TEXT", required: true })];

    const result = validateAndBuildSchemaFieldValues(fields, {});

    expect(result.errors).toEqual({ f1: "validation.schemaFieldRequired" });
    expect(result.requests).toEqual([]);
  });

  it("omits values for optional fields that are left empty", () => {
    const fields = [
      makeField({ fieldUuid: "f1", key: "brand", fieldType: "TEXT", required: false }),
      makeField({ fieldUuid: "f2", key: "weight", fieldType: "NUMBER", required: false }),
    ];

    const result = validateAndBuildSchemaFieldValues(fields, { f1: { valueText: "  " } });

    expect(result.errors).toEqual({});
    expect(result.requests).toEqual([]);
  });
});

describe("buildInitialDynamicValues", () => {
  it("returns an empty map when no existing values are provided", () => {
    expect(buildInitialDynamicValues(undefined)).toEqual({});
    expect(buildInitialDynamicValues(null)).toEqual({});
  });

  it("maps TEXT/NUMBER/BOOLEAN/DATE response values back to local form state", () => {
    const existing: SchemaFieldValueResponse[] = [
      { fieldUuid: "f1", key: "brand", label: "Brand", fieldType: "TEXT", valueText: "Acme", options: [] },
      { fieldUuid: "f2", key: "weight", label: "Weight", fieldType: "NUMBER", valueNumber: 3.2, options: [] },
    ];

    const result = buildInitialDynamicValues(existing);

    expect(result.f1).toEqual({ valueText: "Acme" });
    expect(result.f2).toEqual({ valueNumber: 3.2 });
  });

  it("maps SINGLE_SELECT response options to optionUuid", () => {
    const existing: SchemaFieldValueResponse[] = [
      {
        fieldUuid: "f5",
        key: "color",
        label: "Color",
        fieldType: "SINGLE_SELECT",
        options: [{ optionUuid: "opt-red", value: "red", label: "Red", displayOrder: 0 }],
      },
    ];

    const result = buildInitialDynamicValues(existing);

    expect(result.f5).toEqual({ optionUuid: "opt-red" });
  });

  it("maps MULTI_SELECT response options to optionUuids", () => {
    const existing: SchemaFieldValueResponse[] = [
      {
        fieldUuid: "f6",
        key: "sizes",
        label: "Sizes",
        fieldType: "MULTI_SELECT",
        options: [
          { optionUuid: "opt-s", value: "S", label: "S", displayOrder: 0 },
          { optionUuid: "opt-m", value: "M", label: "M", displayOrder: 1 },
        ],
      },
    ];

    const result = buildInitialDynamicValues(existing);

    expect(result.f6).toEqual({ optionUuids: ["opt-s", "opt-m"] });
  });
});

