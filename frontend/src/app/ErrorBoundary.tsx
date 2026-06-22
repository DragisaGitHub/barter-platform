import { Component, type ReactNode } from "react";
import * as Sentry from "@sentry/react";
import { withTranslation, type WithTranslation } from "react-i18next";
import { isSentryEnabled } from "../lib/sentry";

interface ErrorBoundaryProps extends WithTranslation {
  children: ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
}

class AppErrorBoundaryInner extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { hasError: true };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo): void {
    if (isSentryEnabled()) {
      Sentry.captureException(error, {
        contexts: {
          react: {
            componentStack: errorInfo.componentStack ?? undefined,
          },
        },
      });
    }
  }

  handleReload = (): void => {
    window.location.reload();
  };

  render() {
    if (!this.state.hasError) {
      return this.props.children;
    }

    const { t } = this.props;

    return (
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          minHeight: "100vh",
          padding: "2rem",
          textAlign: "center",
          fontFamily:
            '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
        }}
      >
        <div style={{ maxWidth: "480px" }}>
          <h1 style={{ fontSize: "1.5rem", marginBottom: "1rem", color: "#1a1a1a" }}>
            {t("errors:boundary.title")}
          </h1>
          <p style={{ fontSize: "1rem", color: "#555", marginBottom: "2rem", lineHeight: 1.6 }}>
            {t("errors:boundary.description")}
          </p>
          <button
            onClick={this.handleReload}
            style={{
              padding: "0.75rem 1.5rem",
              fontSize: "1rem",
              backgroundColor: "#2563eb",
              color: "#fff",
              border: "none",
              borderRadius: "0.5rem",
              cursor: "pointer",
              transition: "background-color 0.2s",
            }}
            onMouseOver={(e) =>
              ((e.target as HTMLButtonElement).style.backgroundColor = "#1d4ed8")
            }
            onMouseOut={(e) =>
              ((e.target as HTMLButtonElement).style.backgroundColor = "#2563eb")
            }
          >
            {t("errors:boundary.reload")}
          </button>
        </div>
      </div>
    );
  }
}

/**
 * Top-level error boundary with i18n support.
 * Catches unhandled React rendering errors and shows a safe, user-friendly fallback.
 */
export const AppErrorBoundary = withTranslation()(AppErrorBoundaryInner);

