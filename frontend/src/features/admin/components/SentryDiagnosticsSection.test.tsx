import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { SentryDiagnosticsSection } from "./SentryDiagnosticsSection";

// Mock i18n
vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => {
      const map: Record<string, string> = {
        "sentryDiagnostics.title": "Sentry Diagnostics",
        "sentryDiagnostics.description": "Manually send a test event to Sentry.",
        "sentryDiagnostics.statusActive": "Active",
        "sentryDiagnostics.statusDisabled": "Disabled",
        "sentryDiagnostics.sendTestEvent": "Send test event",
        "sentryDiagnostics.successTitle": "Test event sent",
        "sentryDiagnostics.successDescription": "Check your Sentry project.",
        "sentryDiagnostics.disabledTitle": "Sentry is not active",
        "sentryDiagnostics.disabledDescription": "VITE_SENTRY_DSN is not configured.",
        "sentryDiagnostics.metadataSent": "Metadata sent with event",
        "sentryDiagnostics.capturedAtSend": "captured at send",
        "sentryDiagnostics.privacyNote": "No private user data is included.",
        "admin": "Admin",
        "controlPanel": "Control panel",
      };
      return map[key] ?? key;
    },
  }),
}));

// Mock Sentry lib
const mockCaptureException = vi.fn();
const mockWithScope = vi.fn((cb: (scope: unknown) => void) => {
  cb({ setTag: vi.fn(), setContext: vi.fn() });
});

vi.mock("@sentry/react", () => ({
  captureException: (...args: unknown[]) => mockCaptureException(...args),
  withScope: (cb: (scope: unknown) => void) => mockWithScope(cb),
}));

// Control isSentryEnabled
const mockIsSentryEnabled = vi.fn(() => true);
vi.mock("@/lib/sentry", () => ({
  isSentryEnabled: () => mockIsSentryEnabled(),
}));

function renderSection() {
  return render(
    <MemoryRouter initialEntries={["/admin/system"]}>
      <SentryDiagnosticsSection />
    </MemoryRouter>
  );
}

describe("SentryDiagnosticsSection", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockIsSentryEnabled.mockReturnValue(true);
  });

  it("renders the section title", () => {
    renderSection();
    expect(screen.getByText("Sentry Diagnostics")).toBeInTheDocument();
  });

  it("shows Active badge when Sentry is enabled", () => {
    renderSection();
    expect(screen.getByText("Active")).toBeInTheDocument();
  });

  it("shows the send test event button when Sentry is enabled", () => {
    renderSection();
    expect(screen.getByRole("button", { name: /send test event/i })).toBeInTheDocument();
  });

  it("calls Sentry.withScope and captureException when button is clicked", () => {
    renderSection();
    fireEvent.click(screen.getByRole("button", { name: /send test event/i }));
    expect(mockWithScope).toHaveBeenCalledTimes(1);
    expect(mockCaptureException).toHaveBeenCalledTimes(1);
    const error = mockCaptureException.mock.calls[0][0];
    expect(error).toBeInstanceOf(Error);
    expect(error.message).toBe("Manual Sentry diagnostics test");
  });

  it("shows a success message after the button is clicked", () => {
    renderSection();
    fireEvent.click(screen.getByRole("button", { name: /send test event/i }));
    expect(screen.getByRole("status")).toBeInTheDocument();
    expect(screen.getByText("Test event sent")).toBeInTheDocument();
  });

  it("shows disabled state when Sentry is not enabled", () => {
    mockIsSentryEnabled.mockReturnValue(false);
    renderSection();
    expect(screen.getByText("Disabled")).toBeInTheDocument();
    expect(screen.getByText("Sentry is not active")).toBeInTheDocument();
    expect(screen.getByText("VITE_SENTRY_DSN is not configured.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /send test event/i })).not.toBeInTheDocument();
  });

  it("shows privacy note when Sentry is enabled", () => {
    renderSection();
    expect(screen.getByText("No private user data is included.")).toBeInTheDocument();
  });
});

