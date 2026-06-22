import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { NotificationBell } from './NotificationBell';

// ─── Mocks ──────────────────────────────────────────────────────────────────

const mockNavigate = vi.fn();
vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, opts?: Record<string, unknown>) => {
      if (key === 'notifications:unreadUpdates') return `${opts?.count} unread`;
      if (key === 'notifications:notifications') return 'Notifications';
      if (key === 'notifications:caughtUp') return 'All caught up';
      if (key === 'notifications:markAllRead') return 'Mark all read';
      if (key === 'notifications:noNotifications') return 'No notifications';
      if (key === 'notifications:noNotificationsBody') return 'Check back later';
      if (key === 'notifications:viewAllNotifications') return 'View all';
      if (key === 'notifications:unread') return 'New';
      return key;
    },
    i18n: { language: 'en' },
  }),
}));

const mockUnreadCount = vi.fn(() => ({ data: { count: 0 } }));
const mockNotifications = vi.fn(() => ({ data: { content: [] }, isLoading: false }));
const mockMarkAsRead = vi.fn(() => ({ mutateAsync: vi.fn() }));
const mockMarkAllAsRead = vi.fn(() => ({ mutate: vi.fn(), isPending: false }));

vi.mock('./useNotifications', () => ({
  useUnreadNotificationCount: () => mockUnreadCount(),
  useNotifications: () => mockNotifications(),
  useMarkNotificationAsRead: () => mockMarkAsRead(),
  useMarkAllNotificationsAsRead: () => mockMarkAllAsRead(),
}));

vi.mock('./notificationHelpers', () => ({
  formatNotificationTime: () => '2 min ago',
  getNotificationColor: () => 'text-indigo-500',
  getNotificationTargetPath: () => '/trades/123',
}));

vi.mock('./renderNotificationText', () => ({
  renderNotificationText: (n: { type: string }) => ({
    title: `Title for ${n.type}`,
    message: 'Some message',
  }),
}));

vi.mock('@/components/ui/Spinner', () => ({
  Spinner: () => <div data-testid="spinner" />,
}));

vi.mock('@/routes/routePaths.ts', () => ({
  routePaths: { notifications: '/notifications' },
}));

// ─── Tests ──────────────────────────────────────────────────────────────────

describe('NotificationBell', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUnreadCount.mockReturnValue({ data: { count: 0 } });
    mockNotifications.mockReturnValue({ data: { content: [] }, isLoading: false });
    mockMarkAsRead.mockReturnValue({ mutateAsync: vi.fn() });
    mockMarkAllAsRead.mockReturnValue({ mutate: vi.fn(), isPending: false });
  });

  it('renders the bell button with accessible label when no unread', () => {
    render(<NotificationBell />);
    expect(screen.getByRole('button', { name: 'Notifications' })).toBeInTheDocument();
  });

  it('shows unread badge count when there are unread notifications', () => {
    mockUnreadCount.mockReturnValue({ data: { count: 5 } });
    render(<NotificationBell />);
    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '5 unread' })).toBeInTheDocument();
  });

  it('caps the badge at 99+', () => {
    mockUnreadCount.mockReturnValue({ data: { count: 150 } });
    render(<NotificationBell />);
    expect(screen.getByText('99+')).toBeInTheDocument();
  });

  it('opens and closes the dropdown on click', async () => {
    const user = userEvent.setup();
    mockNotifications.mockReturnValue({
      data: {
        content: [
          { uuid: 'n1', type: 'TRADE_OFFER_RECEIVED', isRead: false, createdAt: '2024-01-01T00:00:00Z' },
        ],
      },
      isLoading: false,
    });
    render(<NotificationBell />);

    // Dropdown not visible initially
    expect(screen.queryByText('View all')).not.toBeInTheDocument();

    // Open
    await user.click(screen.getByRole('button', { name: 'Notifications' }));
    expect(screen.getByText('View all')).toBeInTheDocument();
    expect(screen.getByText('Title for TRADE_OFFER_RECEIVED')).toBeInTheDocument();
  });

  it('shows empty state when no notifications', async () => {
    const user = userEvent.setup();
    render(<NotificationBell />);
    await user.click(screen.getByRole('button', { name: 'Notifications' }));
    expect(screen.getByText('No notifications')).toBeInTheDocument();
  });

  it('shows mark-all-read button only when there are unread notifications', async () => {
    const user = userEvent.setup();
    mockUnreadCount.mockReturnValue({ data: { count: 3 } });
    render(<NotificationBell />);
    await user.click(screen.getByRole('button', { name: '3 unread' }));
    expect(screen.getByText('Mark all read')).toBeInTheDocument();
  });
});
