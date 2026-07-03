import { render, screen } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { ItemSpecificationsSection } from "./ItemSpecificationsSection";
import type { SchemaFieldValueResponse } from "@/api/generated/types.ts";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => {
      const map: Record<string, string> = {
        "catalog:itemDetail.specifications": "Specifications",
        "catalog:fields.booleanYes": "Yes",
        "catalog:fields.booleanNo": "No",
      };
      return map[key] ?? key;
    },
    i18n: { language: "en" },
  }),
}));

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

describe("ItemSpecificationsSection", () => {
  it("renders nothing when schemaFieldValues is undefined", () => {
    const { container } = render(<ItemSpecificationsSection schemaFieldValues={undefined} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("renders nothing when schemaFieldValues is an empty array", () => {
    const { container } = render(<ItemSpecificationsSection schemaFieldValues={[]} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("renders nothing when no values are meaningful", () => {
    const { container } = render(
      <ItemSpecificationsSection
        schemaFieldValues={[makeValue({ fieldType: "TEXT", valueText: undefined })]}
      />
    );
    expect(container).toBeEmptyDOMElement();
  });

  it("renders the Specifications heading and field label/value pairs", () => {
    render(
      <ItemSpecificationsSection
        schemaFieldValues={[
          makeValue({ fieldUuid: "f1", key: "brand", label: "Brand", valueText: "Acme", displayOrder: 0 }),
          makeValue({
            fieldUuid: "f2",
            key: "weight",
            label: "Weight",
            fieldType: "NUMBER",
            valueNumber: 2.5,
            unit: "kg",
            displayOrder: 1,
          }),
        ]}
      />
    );

    expect(screen.getByText("Specifications")).toBeInTheDocument();
    expect(screen.getByText("Brand")).toBeInTheDocument();
    expect(screen.getByText("Acme")).toBeInTheDocument();
    expect(screen.getByText("Weight")).toBeInTheDocument();
    expect(screen.getByText("2.5 kg")).toBeInTheDocument();
  });
});

