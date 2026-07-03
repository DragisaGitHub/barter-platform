import { describe, it, expect } from "vitest";
import {
  formatSchemaFieldValue,
  getDisplayableSchemaFieldValues,
  getSchemaFieldLabel,
  hasSchemaFieldValue,
} from "./itemSpecifications";
import type { SchemaFieldValueResponse } from "@/api/generated/types.ts";

function makeValue(overrides: Partial<SchemaFieldValueResponse>): SchemaFieldValueResponse {
  return {
    fieldUuid: "field-1",
    key: "brand",
    label: "Brand",
    fieldType: "TEXT",
    displayOrder: 0,
    options: [],
    ...overrides,
  };
}

describe("hasSchemaFieldValue", () => {
  it("returns true for a non-blank TEXT value", () => {
    expect(hasSchemaFieldValue(makeValue({ fieldType: "TEXT", valueText: "Acme" }))).toBe(true);
  });

  it("returns false for a blank TEXT value", () => {
    expect(hasSchemaFieldValue(makeValue({ fieldType: "TEXT", valueText: "   " }))).toBe(false);
    expect(hasSchemaFieldValue(makeValue({ fieldType: "TEXT", valueText: undefined }))).toBe(false);
  });

  it("returns true only when NUMBER is present (including 0)", () => {
    expect(hasSchemaFieldValue(makeValue({ fieldType: "NUMBER", valueNumber: 0 }))).toBe(true);
    expect(hasSchemaFieldValue(makeValue({ fieldType: "NUMBER", valueNumber: undefined }))).toBe(false);
  });

  it("returns true only when BOOLEAN is present (including false)", () => {
    expect(hasSchemaFieldValue(makeValue({ fieldType: "BOOLEAN", valueBoolean: false }))).toBe(true);
    expect(hasSchemaFieldValue(makeValue({ fieldType: "BOOLEAN", valueBoolean: undefined }))).toBe(false);
  });

  it("returns true only when DATE is present", () => {
    expect(hasSchemaFieldValue(makeValue({ fieldType: "DATE", valueDate: "2024-01-15" }))).toBe(true);
    expect(hasSchemaFieldValue(makeValue({ fieldType: "DATE", valueDate: undefined }))).toBe(false);
  });

  it("returns true for SINGLE_SELECT/MULTI_SELECT only when options are present", () => {
    expect(
      hasSchemaFieldValue(
        makeValue({
          fieldType: "SINGLE_SELECT",
          options: [{ optionUuid: "o1", value: "red", label: "Red", displayOrder: 0 }],
        })
      )
    ).toBe(true);
    expect(hasSchemaFieldValue(makeValue({ fieldType: "SINGLE_SELECT", options: [] }))).toBe(false);
    expect(hasSchemaFieldValue(makeValue({ fieldType: "MULTI_SELECT", options: [] }))).toBe(false);
  });
});

describe("getSchemaFieldLabel", () => {
  it("uses label for non-Serbian locale", () => {
    const value = makeValue({ label: "Brand", labelSr: "Marka" });
    expect(getSchemaFieldLabel(value, "en")).toBe("Brand");
  });

  it("uses labelSr for Serbian locale when present", () => {
    const value = makeValue({ label: "Brand", labelSr: "Marka" });
    expect(getSchemaFieldLabel(value, "sr")).toBe("Marka");
  });

  it("falls back to label when labelSr is missing, even in Serbian locale", () => {
    const value = makeValue({ label: "Brand", labelSr: undefined });
    expect(getSchemaFieldLabel(value, "sr")).toBe("Brand");
  });
});

describe("formatSchemaFieldValue", () => {
  it("formats TEXT values", () => {
    expect(formatSchemaFieldValue(makeValue({ fieldType: "TEXT", valueText: "Acme" }))).toBe("Acme");
  });

  it("formats NUMBER values with a unit when present", () => {
    expect(formatSchemaFieldValue(makeValue({ fieldType: "NUMBER", valueNumber: 2.5, unit: "kg" }))).toBe("2.5 kg");
  });

  it("formats NUMBER values without a unit", () => {
    expect(formatSchemaFieldValue(makeValue({ fieldType: "NUMBER", valueNumber: 42 }))).toBe("42");
  });

  it("formats BOOLEAN values as Yes/No in EN", () => {
    expect(
      formatSchemaFieldValue(makeValue({ fieldType: "BOOLEAN", valueBoolean: true }), {
        locale: "en",
        yesLabel: "Yes",
        noLabel: "No",
      })
    ).toBe("Yes");
    expect(
      formatSchemaFieldValue(makeValue({ fieldType: "BOOLEAN", valueBoolean: false }), {
        locale: "en",
        yesLabel: "Yes",
        noLabel: "No",
      })
    ).toBe("No");
  });

  it("formats BOOLEAN values as Da/Ne in SR", () => {
    expect(
      formatSchemaFieldValue(makeValue({ fieldType: "BOOLEAN", valueBoolean: true }), {
        locale: "sr",
        yesLabel: "Da",
        noLabel: "Ne",
      })
    ).toBe("Da");
    expect(
      formatSchemaFieldValue(makeValue({ fieldType: "BOOLEAN", valueBoolean: false }), {
        locale: "sr",
        yesLabel: "Da",
        noLabel: "Ne",
      })
    ).toBe("Ne");
  });

  it("formats DATE values as a localized date", () => {
    const formatted = formatSchemaFieldValue(makeValue({ fieldType: "DATE", valueDate: "2024-01-15" }), {
      locale: "en",
    });
    expect(formatted).toContain("2024");
    expect(formatted).toMatch(/Jan/);
  });

  it("formats SINGLE_SELECT as the selected option's label", () => {
    const value = makeValue({
      fieldType: "SINGLE_SELECT",
      options: [{ optionUuid: "o1", value: "red", label: "Red", labelSr: "Crvena", displayOrder: 0 }],
    });
    expect(formatSchemaFieldValue(value, { locale: "en" })).toBe("Red");
    expect(formatSchemaFieldValue(value, { locale: "sr" })).toBe("Crvena");
  });

  it("formats MULTI_SELECT as a comma-separated list of option labels", () => {
    const value = makeValue({
      fieldType: "MULTI_SELECT",
      options: [
        { optionUuid: "o1", value: "s", label: "S", displayOrder: 0 },
        { optionUuid: "o2", value: "m", label: "M", displayOrder: 1 },
      ],
    });
    expect(formatSchemaFieldValue(value, { locale: "en" })).toBe("S, M");
  });

  it("returns an empty string for missing values", () => {
    expect(formatSchemaFieldValue(makeValue({ fieldType: "TEXT", valueText: undefined }))).toBe("");
    expect(formatSchemaFieldValue(makeValue({ fieldType: "NUMBER", valueNumber: undefined }))).toBe("");
    expect(formatSchemaFieldValue(makeValue({ fieldType: "DATE", valueDate: undefined }))).toBe("");
  });
});

describe("getDisplayableSchemaFieldValues", () => {
  it("returns an empty array when there are no schema field values", () => {
    expect(getDisplayableSchemaFieldValues(undefined)).toEqual([]);
    expect(getDisplayableSchemaFieldValues(null)).toEqual([]);
    expect(getDisplayableSchemaFieldValues([])).toEqual([]);
  });

  it("filters out values with nothing meaningful to display", () => {
    const values = [
      makeValue({ fieldUuid: "f1", fieldType: "TEXT", valueText: "Acme", displayOrder: 1 }),
      makeValue({ fieldUuid: "f2", fieldType: "TEXT", valueText: undefined, displayOrder: 0 }),
      makeValue({ fieldUuid: "f3", fieldType: "NUMBER", valueNumber: undefined, displayOrder: 2 }),
    ];
    const result = getDisplayableSchemaFieldValues(values);
    expect(result).toHaveLength(1);
    expect(result[0].fieldUuid).toBe("f1");
  });

  it("orders displayable values by displayOrder", () => {
    const values = [
      makeValue({ fieldUuid: "f1", fieldType: "TEXT", valueText: "B", displayOrder: 2 }),
      makeValue({ fieldUuid: "f2", fieldType: "TEXT", valueText: "A", displayOrder: 0 }),
      makeValue({ fieldUuid: "f3", fieldType: "TEXT", valueText: "C", displayOrder: 1 }),
    ];
    const result = getDisplayableSchemaFieldValues(values);
    expect(result.map((v) => v.fieldUuid)).toEqual(["f2", "f3", "f1"]);
  });
});

