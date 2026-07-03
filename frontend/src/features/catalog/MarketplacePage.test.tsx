import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { MarketplacePage } from "./MarketplacePage";

// ─── Mocks ──────────────────────────────────────────────────────────────────

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, opts?: Record<string, unknown>) => {
      if (opts && typeof opts === "object") {
        const withValues = Object.entries(opts)
          .map(([k, v]) => `${k}=${v}`)
          .join(",");
        return `${key}(${withValues})`;
      }
      return key;
    },
    i18n: { language: "en" },
  }),
}));

vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }));
vi.mock("@/utils", () => ({ parseApiError: (e: unknown) => String(e) }));
vi.mock("@/features/onboarding/onboardingState", () => ({
  markMarketplaceVisited: vi.fn(),
}));

vi.mock("@/routes/routePaths", () => ({
  routePaths: {
    marketplace: "/marketplace",
    marketplaceCategories: "/marketplace/categories",
    marketplaceItem: (id: string) => `/marketplace/${id}`,
    login: "/login",
    register: "/register",
    savedSearches: "/saved-searches",
  },
  buildPathWithQuery: (path: string) => path,
}));

vi.mock("../../auth/AuthContext", () => ({
  useAuth: () => ({ user: { uuid: "user-1", username: "tester" }, isAuthenticated: true }),
}));

vi.mock("../../components/ui/Button", () => ({
  Button: ({ children, ...props }: React.PropsWithChildren<React.ButtonHTMLAttributes<HTMLButtonElement>>) => (
    <button {...props}>{children}</button>
  ),
}));
vi.mock("../../components/ui/EmptyState", () => ({
  EmptyState: ({ title }: { title: string }) => <div data-testid="empty-state">{title}</div>,
}));
vi.mock("../../components/ui/Modal", () => ({
  Modal: ({ isOpen, children, title }: { isOpen: boolean; children: React.ReactNode; title: string }) =>
    isOpen ? (
      <div data-testid="modal">
        <p>{title}</p>
        {children}
      </div>
    ) : null,
}));
vi.mock("../../components/ui/Spinner", () => ({
  Spinner: () => <div data-testid="spinner" />,
}));

vi.mock("./MarketplaceUserMenu", () => ({
  MarketplaceUserMenu: () => <div data-testid="user-menu" />,
}));
vi.mock("./RecommendationsSection", () => ({
  RecommendationsSection: () => <div data-testid="recommendations" />,
}));
vi.mock("./SavedSearchesPanel", () => ({
  SavedSearchesPanel: () => <div data-testid="saved-searches-panel" />,
}));
vi.mock("./CategoryFiltersPanel", () => ({
  CategoryFiltersPanel: ({ categoryUuid }: { categoryUuid?: string }) => (
    <div data-testid="category-filters-panel">{categoryUuid}</div>
  ),
}));
vi.mock("./useSavedSearches", () => ({
  useCreateSavedSearch: () => ({ mutate: vi.fn(), isPending: false }),
}));

const mockUseSearchItems = vi.fn();
const mockUseCategories = vi.fn();
const mockUsePopularCategories = vi.fn();
const mockUseFavoriteItems = vi.fn();

vi.mock("./useCatalog", () => ({
  useCategories: (...args: unknown[]) => mockUseCategories(...args),
  useFavoriteItem: () => ({ mutate: vi.fn() }),
  useFavoriteItems: (...args: unknown[]) => mockUseFavoriteItems(...args),
  usePopularCategories: (...args: unknown[]) => mockUsePopularCategories(...args),
  useSearchItems: (...args: unknown[]) => mockUseSearchItems(...args),
  useUnfavoriteItem: () => ({ mutate: vi.fn() }),
}));

const categories = [
  { uuid: "cat-1", name: "Electronics", slug: "electronics", sortOrder: 0, activeItemCount: 5 },
  { uuid: "cat-2", name: "Books", slug: "books", sortOrder: 1, activeItemCount: 2 },
];

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/marketplace"]}>
      <MarketplacePage />
    </MemoryRouter>
  );
}

describe("MarketplacePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseSearchItems.mockReturnValue({
      data: { content: [], page: 0, last: true },
      isLoading: false,
      isFetching: false,
      isError: false,
    });
    mockUseCategories.mockReturnValue({ data: categories });
    mockUsePopularCategories.mockReturnValue({ data: categories, isLoading: false });
    mockUseFavoriteItems.mockReturnValue({ data: { content: [] } });
  });

  it("does not render the old global tags cloud/sidebar", () => {
    renderPage();

    // The old feature rendered a "catalog:tags" heading with a cloud of tag buttons.
    expect(screen.queryByText("catalog:tags")).not.toBeInTheDocument();
  });

  it("still renders the categories sidebar and search input", () => {
    renderPage();

    expect(screen.getAllByText("Electronics").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Books").length).toBeGreaterThan(0);
    expect(screen.getByPlaceholderText("catalog:searchPlaceholder")).toBeInTheDocument();
  });

  it("renders dynamic category filters once a category is selected", async () => {
    const user = userEvent.setup();
    renderPage();

    expect(screen.queryByTestId("category-filters-panel")).not.toBeInTheDocument();

    await user.click(screen.getAllByText("Electronics")[0]);

    const panel = await screen.findByTestId("category-filters-panel");
    expect(within(panel).getByText("cat-1")).toBeInTheDocument();
  });

  it("keeps saved search behavior available", () => {
    renderPage();

    expect(screen.getByTestId("saved-searches-panel")).toBeInTheDocument();
  });
});

