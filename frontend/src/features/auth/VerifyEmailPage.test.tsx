import { AxiosError, AxiosHeaders, type AxiosResponse } from "axios";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { VerifyEmailPage } from "./VerifyEmailPage";

const mocks = vi.hoisted(() => ({
  verifyEmail: vi.fn(),
  resendVerificationCode: vi.fn(),
  loginWithTokens: vi.fn(),
  toastError: vi.fn(),
  toastSuccess: vi.fn(),
}));

vi.mock("@/api/authApi.ts", () => ({
  verifyEmail: mocks.verifyEmail,
  resendVerificationCode: mocks.resendVerificationCode,
}));

vi.mock("@/auth/AuthContext", () => ({
  useAuth: () => ({ loginWithTokens: mocks.loginWithTokens }),
}));

vi.mock("sonner", () => ({
  toast: { error: mocks.toastError, success: mocks.toastSuccess },
}));

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, options?: { seconds?: number }) =>
      options?.seconds === undefined ? key : `${key}:${options.seconds}`,
  }),
}));

function LocationDisplay() {
  const location = useLocation();
  return <div data-testid="location">{location.pathname}</div>;
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/verify-email?email=person%40example.com"]}>
      <Routes>
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route path="/dashboard" element={<LocationDisplay />} />
        <Route path="/login" element={<LocationDisplay />} />
      </Routes>
    </MemoryRouter>,
  );
}

function apiError(code: string, status: number, headers: Record<string, string> = {}) {
  const response = {
    data: { code },
    status,
    statusText: "Request failed",
    headers: new AxiosHeaders(headers),
    config: { headers: new AxiosHeaders() },
  } as AxiosResponse;
  return new AxiosError("Request failed", undefined, undefined, undefined, response);
}

async function submitCode() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText("auth:verificationCode"), "123456");
  await user.click(screen.getByRole("button", { name: "auth:verifyEmail" }));
}

describe("VerifyEmailPage security feedback", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("auto-authenticates and redirects after successful verification", async () => {
    mocks.verifyEmail.mockResolvedValue({
      message: "Email verified successfully.",
      accessToken: "access",
      refreshToken: "refresh",
      user: { uuid: "user-1" },
    });
    mocks.loginWithTokens.mockResolvedValue(undefined);

    renderPage();
    await submitCode();

    await waitFor(() => expect(mocks.loginWithTokens).toHaveBeenCalledWith(
      "access",
      "refresh",
      { uuid: "user-1" },
    ));
    expect(await screen.findByTestId("location")).toHaveTextContent("/dashboard");
  });

  it.each([
    ["VERIFICATION_CODE_LOCKED", "auth:verificationCodeLocked"],
    ["VERIFICATION_CODE_EXPIRED", "auth:verificationCodeExpired"],
  ])("shows the specific message for %s", async (code, messageKey) => {
    mocks.verifyEmail.mockRejectedValue(apiError(code, 400));

    renderPage();
    await submitCode();

    await waitFor(() => expect(mocks.toastError).toHaveBeenCalledWith(messageKey));
    expect(mocks.loginWithTokens).not.toHaveBeenCalled();
  });

  it("shows the Retry-After wait time when verification is rate limited", async () => {
    mocks.verifyEmail.mockRejectedValue(apiError("RATE_LIMITED", 429, { "retry-after": "17" }));

    renderPage();
    await submitCode();

    await waitFor(() => expect(mocks.toastError).toHaveBeenCalledWith("auth:verificationRateLimited:17"));
  });

  it("uses the same rate-limit feedback for a resend response", async () => {
    mocks.resendVerificationCode.mockRejectedValue(apiError("RATE_LIMITED", 429, { "retry-after": "42" }));

    renderPage();
    fireEvent.click(screen.getByRole("button", { name: "auth:resendCode" }));

    await waitFor(() => expect(mocks.toastError).toHaveBeenCalledWith("auth:verificationRateLimited:42"));
    expect(mocks.resendVerificationCode).toHaveBeenCalledWith("person@example.com");
  });
});
