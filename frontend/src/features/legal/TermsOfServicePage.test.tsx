import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { TermsOfServicePage } from './TermsOfServicePage';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => {
      if (key === 'termsOfService.title') return 'Terms of Service';
      if (key === 'betaNotice') return 'This is a beta version.';
      return key;
    },
  }),
}));

describe('TermsOfServicePage', () => {
  it('renders the page title and beta notice', () => {
    render(
      <MemoryRouter>
        <TermsOfServicePage />
      </MemoryRouter>
    );
    expect(screen.getByText('Terms of Service')).toBeInTheDocument();
    expect(screen.getByText('This is a beta version.')).toBeInTheDocument();
  });
});
