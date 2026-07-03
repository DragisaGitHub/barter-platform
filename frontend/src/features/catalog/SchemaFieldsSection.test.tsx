import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { SchemaFieldsSection } from "./SchemaFieldsSection";
import type { CategoryFormFieldResponse } from "@/api/generated/types.ts";

const t = (key: string) => key;

function textField(overrides: Partial<CategoryFormFieldResponse> = {}): CategoryFormFieldResponse {
  return {
    fieldUuid: "f1",
    key: "brand",
    label: "Brand",
    fieldType: "TEXT",
    required: true,
    searchable: false,
    filterable: false,
    sortable: false,
    displayOrder: 0,
    options: [],
    ...overrides,
  };
}

describe("SchemaFieldsSection", () => {
  it("renders nothing when no category is selected", () => {
    const { container } = render(
      <SchemaFieldsSection
        fields={[]}
        isLoading={false}
        hasCategory={false}
        values={{}}
        errors={{}}
        onChange={vi.fn()}
        t={t}
      />
    );
    expect(container).toBeEmptyDOMElement();
  });

  it("shows a loading state while the schema is loading", () => {
    render(
      <SchemaFieldsSection
        fields={[]}
        isLoading={true}
        hasCategory={true}
        values={{}}
        errors={{}}
        onChange={vi.fn()}
        t={t}
      />
    );
    expect(screen.getByTestId("schema-fields-loading")).toBeInTheDocument();
  });

  it("shows an empty state when the category has no active schema", () => {
    render(
      <SchemaFieldsSection
        fields={[]}
        isLoading={false}
        hasCategory={true}
        values={{}}
        errors={{}}
        onChange={vi.fn()}
        t={t}
      />
    );
    expect(screen.getByTestId("schema-fields-empty")).toBeInTheDocument();
  });

  it("renders a TEXT field and reports changes via onChange", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(
      <SchemaFieldsSection
        fields={[textField()]}
        isLoading={false}
        hasCategory={true}
        values={{}}
        errors={{}}
        onChange={onChange}
        t={t}
      />
    );

    const input = screen.getByTestId("schema-field-brand");
    await user.type(input, "A");

    expect(onChange).toHaveBeenCalledWith("f1", { valueText: "A" });
  });

  it("shows a required-field validation error", () => {
    render(
      <SchemaFieldsSection
        fields={[textField()]}
        isLoading={false}
        hasCategory={true}
        values={{}}
        errors={{ f1: "validation.schemaFieldRequired" }}
        onChange={vi.fn()}
        t={t}
      />
    );

    expect(screen.getByText("catalog:validation.schemaFieldRequired")).toBeInTheDocument();
  });

  it("renders a SINGLE_SELECT field with its options", () => {
    const field: CategoryFormFieldResponse = {
      ...textField(),
      fieldUuid: "f5",
      key: "color",
      label: "Color",
      fieldType: "SINGLE_SELECT",
      required: false,
      options: [
        { optionUuid: "opt-red", value: "red", label: "Red", displayOrder: 0 },
        { optionUuid: "opt-blue", value: "blue", label: "Blue", displayOrder: 1 },
      ],
    };

    render(
      <SchemaFieldsSection
        fields={[field]}
        isLoading={false}
        hasCategory={true}
        values={{}}
        errors={{}}
        onChange={vi.fn()}
        t={t}
      />
    );

    expect(screen.getByText("Red")).toBeInTheDocument();
    expect(screen.getByText("Blue")).toBeInTheDocument();
  });

  it("renders a MULTI_SELECT field and toggles selection", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    const field: CategoryFormFieldResponse = {
      ...textField(),
      fieldUuid: "f6",
      key: "sizes",
      label: "Sizes",
      fieldType: "MULTI_SELECT",
      required: false,
      options: [
        { optionUuid: "opt-s", value: "S", label: "S", displayOrder: 0 },
        { optionUuid: "opt-m", value: "M", label: "M", displayOrder: 1 },
      ],
    };

    render(
      <SchemaFieldsSection
        fields={[field]}
        isLoading={false}
        hasCategory={true}
        values={{ f6: { optionUuids: ["opt-s"] } }}
        errors={{}}
        onChange={onChange}
        t={t}
      />
    );

    await user.click(screen.getByText("M"));
    expect(onChange).toHaveBeenCalledWith("f6", { optionUuids: ["opt-s", "opt-m"] });

    await user.click(screen.getByText("S"));
    expect(onChange).toHaveBeenCalledWith("f6", { optionUuids: [] });
  });
});

