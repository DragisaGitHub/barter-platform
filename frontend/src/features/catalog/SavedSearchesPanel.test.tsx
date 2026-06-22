import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { SavedSearchesPanel } from './SavedSearchesPanel';

// ─── Mocks ──────────────────────────────────────────────────────────────────

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, opts?: Record<string, unknown>) => {
      if (key === 'catalog:savedSearches.emptyTitle') return 'No saved searches';
      if (key === 'catalog:savedSearches.emptyDescription') return 'Save a search to see it here';
      if (key === 'catalog:savedSearches.emptyCompact') return 'No saved searches yet';
      if (key === 'catalog:savedSearches.apply') return 'Apply';
      if (key === 'catalog:savedSearches.delete') return 'Delete';
      if (key === 'catalog:savedSearches.loadError') return 'Could not load saved searches';
      if (key === 'catalog:savedSearches.criteria.query') return `"${opts?.query}"`;
      if (key === 'catalog:savedSearches.criteria.categoryNamed') return `Category: ${opts?.name}`;
      if (key === 'catalog:savedSearches.criteria.tagsNamed') return `Tags: ${opts?.names}`;
      if (key === 'catalog:savedSearches.criteria.condition') return `Condition: ${opts?.condition}`;
      if (key === 'catalog:savedSearches.criteria.location') return `Location: ${opts?.location}`;
      if (key === 'catalog:savedSearches.criteria.catalogFilters') return 'All items';
      if (key === 'catalog:savedSearches.criteria.categoryFallback') return 'Unknown';
      if (key === 'catalog:savedSearches.criteria.tagFallback') return 'tag';
      return key;
    },
  }),
}));

vi.mock('sonner', () => ({ toast: { error: vi.fn() } }));
vi.mock('@/utils', () => ({ parseApiError: (e: unknown) => String(e) }));
vi.mock('../../components/ui/Button', () => ({
  Button: ({ children, ...props }: React.PropsWithChildren<React.ButtonHTMLAttributes<HTMLButtonElement>>) => (
    <button {...props}>{children}</button>
  ),
}));
vi.mock('../../components/ui/EmptyState', () => ({
  EmptyState: ({ title, description }: { title: string; description: string }) => (
    <div data-testid="empty-state">
      <p>{title}</p>
      <p>{description}</p>
    </div>
  ),
}));
vi.mock('../../components/ui/Spinner', () => ({
  Spinner: () => <div data-testid="spinner" />,
}));

const mockSavedSearches = vi.fn();
const mockDeleteMutation = vi.fn(() => ({ mutate: vi.fn() }));

vi.mock('./useSavedSearches', () => ({
  useSavedSearches: () => mockSavedSearches(),
  useDeleteSavedSearch: () => mockDeleteMutation(),
}));

vi.mock('./useCatalog', () => ({
  useCategories: () => ({
    data: [{ uuid: 'cat-1', name: 'Electronics' }],
  }),
  useTags: () => ({
    data: [{ uuid: 'tag-1', name: 'Vintage' }],
  }),
}));

// ─── Tests ──────────────────────────────────────────────────────────────────

describe('SavedSearchesPanel', () => {
  const onApply = vi.fn();

  it('shows empty state when no searches exist', () => {
    mockSavedSearches.mockReturnValue({ data: { content: [] }, isLoading: false, isError: false });
    render(<SavedSearchesPanel onApply={onApply} />);
    expect(screen.getByTestId('empty-state')).toBeInTheDocument();
    expect(screen.getByText('No saved searches')).toBeInTheDocument();
  });

  it('shows compact empty message when compact and no searches', () => {
    mockSavedSearches.mockReturnValue({ data: { content: [] }, isLoading: false, isError: false });
    render(<SavedSearchesPanel compact onApply={onApply} />);
    expect(screen.getByText('No saved searches yet')).toBeInTheDocument();
  });

  it('renders saved search names and criteria labels', () => {
    mockSavedSearches.mockReturnValue({
      data: {
        content: [
          {
            uuid: 'ss-1',
            name: 'Gaming Laptops',
            criteria: { q: 'laptop', categoryUuid: 'cat-1', tagUuids: ['tag-1'] },
          },
          {
            uuid: 'ss-2',
            name: 'Local Bikes',
            criteria: { location: 'Berlin' },
          },
        ],
      },
      isLoading: false,
      isError: false,
    });

    render(<SavedSearchesPanel onApply={onApply} />);
    expect(screen.getByText('Gaming Laptops')).toBeInTheDocument();
    expect(screen.getByText('Local Bikes')).toBeInTheDocument();
    // Criteria formatting
    expect(screen.getByText(/Electronics/)).toBeInTheDocument();
    expect(screen.getByText(/Vintage/)).toBeInTheDocument();
    expect(screen.getByText(/Berlin/)).toBeInTheDocument();
  });

  it('shows a loading spinner', () => {
    mockSavedSearches.mockReturnValue({ data: undefined, isLoading: true, isError: false });
    render(<SavedSearchesPanel onApply={onApply} />);
    expect(screen.getByTestId('spinner')).toBeInTheDocument();
  });

  it('shows error state', () => {
    mockSavedSearches.mockReturnValue({ data: undefined, isLoading: false, isError: true });
    render(<SavedSearchesPanel onApply={onApply} />);
    expect(screen.getByText('Could not load saved searches')).toBeInTheDocument();
  });
});

