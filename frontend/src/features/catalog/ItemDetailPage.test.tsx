import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ItemDetailPage } from './ItemDetailPage';

// ─── Mocks ──────────────────────────────────────────────────────────────────

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => {
      const map: Record<string, string> = {
        'catalog:addToFavorites': 'Add to favorites',
        'catalog:removeFromFavorites': 'Remove from favorites',
        'catalog:itemNotFound': 'Item not found',
        'catalog:itemDetail.notFoundDescription': 'This item does not exist',
        'common:backToMarketplace': 'Back',
        'catalog:itemDetail.listedOn': 'Listed',
        'catalog:fields.category': 'Category',
        'catalog:tags': 'Tags',
      };
      return map[key] ?? key;
    },
    i18n: { language: 'en' },
  }),
}));

vi.mock('sonner', () => ({ toast: { error: vi.fn() } }));

const mockItemDetail = vi.fn();
const mockFavoriteItems = vi.fn();
const mockFavoriteMutation = vi.fn();
const mockUnfavoriteMutation = vi.fn();

vi.mock('./useCatalog', () => ({
  useItemDetail: (...args: unknown[]) => mockItemDetail(...args),
  useFavoriteItems: (...args: unknown[]) => mockFavoriteItems(...args),
  useFavoriteItem: () => mockFavoriteMutation(),
  useUnfavoriteItem: () => mockUnfavoriteMutation(),
}));

vi.mock('@/auth/AuthContext', () => ({
  useAuth: () => ({ user: { uuid: 'user-1' }, isAuthenticated: true }),
}));

vi.mock('@/routes/routePaths.ts', () => ({
  routePaths: {
    marketplace: '/marketplace',
    myItemDetail: (id: string) => `/my-items/${id}`,
    marketplaceItem: (id: string) => `/marketplace/${id}`,
    login: '/login',
    register: '/register',
    publicProfile: (id: string) => `/profile/${id}`,
  },
}));

vi.mock('./ItemBadges', () => ({
  ItemStatusBadge: () => <span data-testid="status-badge" />,
  ItemConditionBadge: () => <span data-testid="condition-badge" />,
}));
vi.mock('./OwnerModerationPanel', () => ({
  OwnerModerationPanel: () => null,
}));
vi.mock('../../components/ui/Badge', () => ({
  Badge: ({ children }: { children: React.ReactNode }) => <span>{children}</span>,
}));
vi.mock('../../components/ui/Button', () => ({
  Button: ({ children, ...props }: React.PropsWithChildren<React.ButtonHTMLAttributes<HTMLButtonElement>>) => (
    <button {...props}>{children}</button>
  ),
}));
vi.mock('../../components/ui/Spinner', () => ({
  Spinner: () => <div data-testid="spinner" />,
}));
vi.mock('../../components/ui/EmptyState', () => ({
  EmptyState: ({ title }: { title: string }) => <div>{title}</div>,
}));
vi.mock('../../components/ui/ImageLightbox', () => ({
  ImageLightbox: () => null,
}));
vi.mock('../trade/SendOfferModal', () => ({
  SendOfferModal: () => null,
}));
vi.mock('@/features/reports/ReportTrigger', () => ({
  ReportTrigger: () => null,
}));
vi.mock('./listingTemplates', () => ({
  inferListingTemplateType: () => 'STANDARD_ITEM',
}));
vi.mock('@/utils', () => ({
  parseApiError: (e: unknown) => String(e),
  cn: (...args: unknown[]) => args.filter(Boolean).join(' '),
}));

// ─── Helpers ────────────────────────────────────────────────────────────────

const ITEM = {
  uuid: 'item-1',
  title: 'Test Item',
  description: 'A great item',
  ownerUuid: 'owner-2',
  status: 'ACTIVE',
  condition: 'GOOD',
  category: { uuid: 'cat-1', name: 'Electronics' },
  tags: [],
  images: [],
  createdAt: '2024-06-01T00:00:00Z',
  listingMode: 'SINGLE',
  owner: { uuid: 'owner-2', displayName: 'Owner' },
};

function renderWithProviders(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/marketplace/item-1']}>
        <Routes>
          <Route path="/marketplace/:uuid" element={ui} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

// ─── Tests ──────────────────────────────────────────────────────────────────

describe('ItemDetailPage – favorite behavior', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockItemDetail.mockReturnValue({ data: ITEM, isLoading: false, isError: false });
    mockFavoriteItems.mockReturnValue({ data: { content: [] } });
    mockFavoriteMutation.mockReturnValue({ mutate: vi.fn() });
    mockUnfavoriteMutation.mockReturnValue({ mutate: vi.fn() });
  });

  it('shows "Add to favorites" for authenticated user who does not own the item', () => {
    renderWithProviders(<ItemDetailPage />);
    expect(screen.getByRole('button', { name: 'Add to favorites' })).toBeInTheDocument();
  });

  it('shows "Remove from favorites" when item is already favorited', () => {
    mockFavoriteItems.mockReturnValue({ data: { content: [{ uuid: 'item-1' }] } });
    renderWithProviders(<ItemDetailPage />);
    expect(screen.getByRole('button', { name: 'Remove from favorites' })).toBeInTheDocument();
  });

  it('calls favorite mutation on click', async () => {
    const mutateFn = vi.fn();
    mockFavoriteMutation.mockReturnValue({ mutate: mutateFn });
    mockUnfavoriteMutation.mockReturnValue({ mutate: vi.fn() });
    const user = userEvent.setup();

    renderWithProviders(<ItemDetailPage />);
    await user.click(screen.getByRole('button', { name: 'Add to favorites' }));
    expect(mutateFn).toHaveBeenCalled();
  });

  it('shows loading state when data is loading', () => {
    mockItemDetail.mockReturnValue({ data: undefined, isLoading: true, isError: false });
    renderWithProviders(<ItemDetailPage />);
    expect(screen.getByTestId('spinner')).toBeInTheDocument();
  });
});
