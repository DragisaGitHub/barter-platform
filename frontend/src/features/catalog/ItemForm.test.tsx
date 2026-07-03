import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { ItemForm } from "./ItemForm";

// ─── Mocks ──────────────────────────────────────────────────────────────────

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { language: "en" },
  }),
}));

const mockUseCategories = vi.fn();
const mockUseCategoryFormSchema = vi.fn();

vi.mock("./useCatalog", () => ({
  useCategories: (...args: unknown[]) => mockUseCategories(...args),
  useCategoryFormSchema: (...args: unknown[]) => mockUseCategoryFormSchema(...args),
}));

const categories = [
  { uuid: "cat-electronics", name: "Electronics" },
  { uuid: "cat-books", name: "Books" },
];

function renderForm(props: Partial<React.ComponentProps<typeof ItemForm>> = {}) {
  const onSubmit = vi.fn();
  const utils = render(<ItemForm onSubmit={onSubmit} {...props} />);
  return { onSubmit, ...utils };
}

describe("ItemForm", () => {
  beforeEach(() => {
    mockUseCategories.mockReturnValue({ data: categories, isLoading: false });
  });

  it("does not render the old global tag picker", () => {
    mockUseCategoryFormSchema.mockReturnValue({ data: undefined, isLoading: false });
    renderForm();

    expect(screen.queryByText("catalog:tags")).not.toBeInTheDocument();
    expect(screen.queryByText("catalog:itemForm.loadingTags")).not.toBeInTheDocument();
    expect(screen.queryByText("catalog:itemForm.noTags")).not.toBeInTheDocument();
  });

  it("renders dynamic schema fields when the selected category has an active schema", async () => {
    const user = userEvent.setup();
    mockUseCategoryFormSchema.mockReturnValue({
      data: {
        categoryUuid: "cat-electronics",
        schemaUuid: "schema-1",
        schemaVersion: 1,
        fields: [
          {
            fieldUuid: "field-brand",
            key: "brand",
            label: "Brand",
            fieldType: "TEXT",
            required: false,
            displayOrder: 0,
            options: [],
          },
        ],
      },
      isLoading: false,
    });

    renderForm();

    await user.selectOptions(screen.getAllByRole("combobox")[0], "cat-electronics");

    expect(await screen.findByTestId("schema-fields-section")).toBeInTheDocument();
    expect(screen.getByTestId("schema-field-brand")).toBeInTheDocument();

    // Still no old global tag picker even when a schema is present.
    expect(screen.queryByText("catalog:tags")).not.toBeInTheDocument();
  });

  it("still allows the create/edit flow to work end to end", async () => {
    const user = userEvent.setup();
    mockUseCategoryFormSchema.mockReturnValue({ data: { fields: [] }, isLoading: false });

    const { onSubmit } = renderForm();

    await user.type(screen.getByLabelText("catalog:fields.title"), "My item");
    const [categorySelect, conditionSelect] = screen.getAllByRole("combobox");
    await user.selectOptions(categorySelect, "cat-books");
    await user.selectOptions(conditionSelect, "GOOD");

    await user.click(screen.getByRole("button", { name: "common:save" }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        title: "My item",
        categoryUuid: "cat-books",
        condition: "GOOD",
        schemaFieldValues: [],
      })
    );
  });
});

