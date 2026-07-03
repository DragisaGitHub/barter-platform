import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { CategoryFiltersPanel } from "./CategoryFiltersPanel";
import type { CategoryFiltersResponse } from "@/api/generated/types.ts";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { language: "en" },
  }),
}));

const mockUseCategoryFilters = vi.fn();

vi.mock("./useCatalog", () => ({
  useCategoryFilters: (...args: unknown[]) => mockUseCategoryFilters(...args),
}));

function filtersResponse(overrides: Partial<CategoryFiltersResponse> = {}): CategoryFiltersResponse {
  return {
    categoryUuid: "cat-1",
    schemaUuid: "schema-1",
    schemaVersion: 1,
    filters: [],
    ...overrides,
  };
}

describe("CategoryFiltersPanel", () => {
  it("renders nothing when no category is selected", () => {
    mockUseCategoryFilters.mockReturnValue({ data: undefined, isLoading: false });
    const { container } = render(
      <CategoryFiltersPanel categoryUuid={undefined} values={{}} onApply={vi.fn()} />
    );
    expect(container).toBeEmptyDOMElement();
  });

  it("renders nothing when the category has no filterable fields", () => {
    mockUseCategoryFilters.mockReturnValue({ data: filtersResponse({ filters: [] }), isLoading: false });
    const { container } = render(
      <CategoryFiltersPanel categoryUuid="cat-1" values={{}} onApply={vi.fn()} />
    );
    expect(container).toBeEmptyDOMElement();
  });

  it("renders a BOOLEAN filter and applies the value", async () => {
    const user = userEvent.setup();
    const onApply = vi.fn();
    mockUseCategoryFilters.mockReturnValue({
      data: filtersResponse({
        filters: [
          {
            fieldUuid: "f-5g",
            key: "has5g",
            label: "5G",
            fieldType: "BOOLEAN",
            displayOrder: 0,
            options: [],
          },
        ],
      }),
      isLoading: false,
    });

    render(<CategoryFiltersPanel categoryUuid="cat-1" values={{}} onApply={onApply} />);

    const checkbox = screen.getByRole("checkbox");
    await user.click(checkbox);
    await user.click(screen.getByText("marketplace.categoryFilters.apply"));

    expect(onApply).toHaveBeenCalledWith({ has5g: true });
  });

  it("renders a SINGLE_SELECT filter with options", () => {
    mockUseCategoryFilters.mockReturnValue({
      data: filtersResponse({
        filters: [
          {
            fieldUuid: "f-brand",
            key: "brand",
            label: "Brand",
            fieldType: "SINGLE_SELECT",
            displayOrder: 0,
            options: [
              { optionUuid: "opt-samsung", value: "samsung", label: "Samsung", displayOrder: 0 },
              { optionUuid: "opt-apple", value: "apple", label: "Apple", displayOrder: 1 },
            ],
          },
        ],
      }),
      isLoading: false,
    });

    render(<CategoryFiltersPanel categoryUuid="cat-1" values={{}} onApply={vi.fn()} />);

    expect(screen.getByText("Samsung")).toBeInTheDocument();
    expect(screen.getByText("Apple")).toBeInTheDocument();
  });

  it("renders a MULTI_SELECT filter and toggles values", async () => {
    const user = userEvent.setup();
    const onApply = vi.fn();
    mockUseCategoryFilters.mockReturnValue({
      data: filtersResponse({
        filters: [
          {
            fieldUuid: "f-color",
            key: "color",
            label: "Color",
            fieldType: "MULTI_SELECT",
            displayOrder: 0,
            options: [
              { optionUuid: "opt-red", value: "red", label: "Red", displayOrder: 0 },
              { optionUuid: "opt-blue", value: "blue", label: "Blue", displayOrder: 1 },
            ],
          },
        ],
      }),
      isLoading: false,
    });

    render(<CategoryFiltersPanel categoryUuid="cat-1" values={{}} onApply={onApply} />);

    await user.click(screen.getByText("Red"));
    await user.click(screen.getByText("marketplace.categoryFilters.apply"));

    expect(onApply).toHaveBeenCalledWith({ color: ["red"] });
  });

  it("clears applied values", async () => {
    const user = userEvent.setup();
    const onApply = vi.fn();
    mockUseCategoryFilters.mockReturnValue({
      data: filtersResponse({
        filters: [
          {
            fieldUuid: "f-brand",
            key: "brand",
            label: "Brand",
            fieldType: "TEXT",
            displayOrder: 0,
            options: [],
          },
        ],
      }),
      isLoading: false,
    });

    render(<CategoryFiltersPanel categoryUuid="cat-1" values={{ brand: "Samsung" }} onApply={onApply} />);

    await user.click(screen.getByText("clear"));

    expect(onApply).toHaveBeenCalledWith({});
  });
});

