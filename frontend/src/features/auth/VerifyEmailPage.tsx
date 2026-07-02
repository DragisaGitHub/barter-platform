import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useForm, FormProvider } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { AxiosError } from "axios";
import { Mail, RefreshCw } from "lucide-react";
import { useTranslation } from "react-i18next";
import { verifyEmail, resendVerificationCode } from "@/api/authApi.ts";
import { useAuth } from "@/auth/AuthContext";
import { Button } from "../../components/ui/Button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/Card";
import { FormInput } from "../../components/forms/FormInput";
import { parseApiError } from "@/utils";
import type { ErrorResponse } from "@/api/generated/types.ts";
import {
  buildPathWithQuery,
  getSafeRedirectPath,
  routePaths,
} from "@/routes/routePaths.ts";

type VerifyEmailFormData = {
  email: string;
  code: string;
};

export function VerifyEmailPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { t } = useTranslation(["auth", "common"]);
  const { loginWithTokens } = useAuth();
  const emailFromParam = searchParams.get("email") ?? "";
  const redirectPath = getSafeRedirectPath(searchParams.get("redirect"));
  const loginHref = buildPathWithQuery(routePaths.login, { redirect: redirectPath });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isResending, setIsResending] = useState(false);

  const verifyEmailSchema = z.object({
    email: z.string().email(t("auth:invalidEmail")),
    code: z
      .string()
      .length(6, t("auth:verificationCodeLength"))
      .regex(/^\d{6}$/, t("auth:verificationCodeDigits")),
  });

  const methods = useForm<VerifyEmailFormData>({
    resolver: zodResolver(verifyEmailSchema),
    defaultValues: {
      email: emailFromParam,
      code: "",
    },
  });

  const currentEmail = methods.watch("email");

  const onSubmit = async (data: VerifyEmailFormData) => {
    setIsSubmitting(true);
    try {
      const result = await verifyEmail(data.email, data.code);

      if (result.accessToken && result.refreshToken) {
        // Newly verified — auto sign-in and redirect into the app
        await loginWithTokens(result.accessToken, result.refreshToken, result.user);
        toast.success(result.message ?? t("auth:verificationSuccess"));
        navigate(redirectPath ?? routePaths.dashboard, { replace: true });
      } else {
        // Already verified — no tokens issued; send user to login
        toast.success(result.message ?? t("auth:verificationSuccess"));
        navigate(loginHref, { replace: true });
      }
    } catch (error) {
      if (error instanceof AxiosError) {
        const errorData = error.response?.data as ErrorResponse | undefined;
        if (errorData?.fieldErrors && errorData.fieldErrors.length > 0) {
          errorData.fieldErrors.forEach((fe) => {
            methods.setError(fe.field as keyof VerifyEmailFormData, {
              message: fe.message,
            });
          });
        } else {
          toast.error(parseApiError(error));
        }
      } else {
        toast.error(t("auth:unexpectedError"));
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleResend = async () => {
    const email = currentEmail.trim();
    if (!email) {
      methods.setError("email", { message: t("auth:enterEmailToResend") });
      return;
    }

    setIsResending(true);
    try {
      const result = await resendVerificationCode(email);
      toast.success(result.message ?? t("auth:verificationResent"));
    } catch (error) {
      toast.error(parseApiError(error));
    } finally {
      setIsResending(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 px-4 py-10 dark:bg-slate-950">
      <div className="mx-auto flex min-h-[calc(100vh-5rem)] w-full max-w-md items-center">
        <Card className="w-full max-w-md border-slate-200/80 shadow-xl shadow-slate-200/50 dark:border-slate-800 dark:bg-slate-900/90 dark:shadow-none">
          <CardHeader>
            <div className="flex justify-center mb-3">
              <div className="rounded-full bg-indigo-100 dark:bg-indigo-900/30 p-3">
                <Mail className="h-6 w-6 text-indigo-600 dark:text-indigo-400" />
              </div>
            </div>
            <div className="mx-auto mb-1 inline-flex items-center rounded-full border border-violet-200 bg-violet-50 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-violet-700 dark:border-violet-900/70 dark:bg-violet-950/40 dark:text-violet-300">
              {t("auth:verifyAccount")}
            </div>
            <CardTitle className="text-center">{t("auth:checkYourEmail")}</CardTitle>
            <p className="text-center text-sm text-slate-600 dark:text-slate-400 mt-2">
              {emailFromParam ? (
                t("auth:verificationSentTo", { email: emailFromParam })
              ) : (
                t("auth:verificationPrompt")
              )}
            </p>
          </CardHeader>

        <CardContent>
          {redirectPath && (
            <div className="mb-4 rounded-lg border border-violet-200 bg-violet-50/80 p-3 text-sm text-violet-800 dark:border-violet-900/60 dark:bg-violet-950/30 dark:text-violet-200">
              {t("auth:verifyRedirectInfo")}
            </div>
          )}

          <FormProvider {...methods}>
            <form onSubmit={methods.handleSubmit(onSubmit)} className="space-y-4">
              {/* Show email field only when not pre-filled from query param */}
              {!emailFromParam && (
                <FormInput
                  name="email"
                  label={t("common:email")}
                  type="email"
                  placeholder={t("auth:emailPlaceholder")}
                  autoComplete="email"
                />
              )}

              <FormInput
                name="code"
                label={t("auth:verificationCode")}
                type="text"
                placeholder={t("auth:verificationCodePlaceholder")}
                autoComplete="one-time-code"
                inputMode="numeric"
                maxLength={6}
              />

              <Button type="submit" fullWidth isLoading={isSubmitting}>
                {t("auth:verifyEmail")}
              </Button>
            </form>
          </FormProvider>

          {/* Resend section */}
          <div className="mt-5 flex flex-col items-center gap-2">
            <p className="text-sm text-slate-500 dark:text-slate-400">
              {t("auth:didNotReceive")}
            </p>
            <Button
              variant="ghost"
              size="sm"
              onClick={handleResend}
              isLoading={isResending}
              disabled={isResending}
              className="flex items-center gap-2"
            >
              <RefreshCw className="h-4 w-4" />
              {t("auth:resendCode")}
            </Button>
          </div>

          <div className="mt-5 text-center">
            <Link
              to={loginHref}
              className="text-sm text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-100"
            >
              ← {t("common:backToSignIn")}
            </Link>
          </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

