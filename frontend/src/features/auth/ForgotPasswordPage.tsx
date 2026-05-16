import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useForm, FormProvider } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { AxiosError } from "axios";
import { Mail } from "lucide-react";
import { useTranslation } from "react-i18next";
import { forgotPassword } from "@/api/authApi.ts";
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

type ForgotPasswordFormData = {
  email: string;
};

export function ForgotPasswordPage() {
  const [searchParams] = useSearchParams();
  const { t } = useTranslation(["auth", "common"]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const redirectPath = getSafeRedirectPath(searchParams.get("redirect"));
  const loginHref = buildPathWithQuery(routePaths.login, { redirect: redirectPath });

  const forgotPasswordSchema = z.object({
    email: z.string().email(t("auth:invalidEmail")),
  });

  const methods = useForm<ForgotPasswordFormData>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: {
      email: "",
    },
  });

  const onSubmit = async (data: ForgotPasswordFormData) => {
    setIsLoading(true);
    try {
      await forgotPassword(data.email);
      setIsSuccess(true);
    } catch (error) {
      if (error instanceof AxiosError) {
        const errorData = error.response?.data as ErrorResponse | undefined;
        if (errorData?.fieldErrors?.length) {
          errorData.fieldErrors.forEach((fieldError) => {
            methods.setError(fieldError.field as keyof ForgotPasswordFormData, {
              message: fieldError.message,
            });
          });
        } else {
          methods.setError("email", { message: parseApiError(error) });
        }
      } else {
          methods.setError("email", { message: t("auth:unexpectedError") });
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-12">
      <Card className="w-full max-w-md">
        <CardHeader>
          <div className="flex justify-center mb-3">
            <div className="rounded-full bg-indigo-100 dark:bg-indigo-900/30 p-3">
              <Mail className="h-6 w-6 text-indigo-600 dark:text-indigo-400" />
            </div>
          </div>
          <CardTitle className="text-center">{t("auth:forgotPasswordTitle")}</CardTitle>
          <p className="text-center text-sm text-slate-600 dark:text-slate-400 mt-2">
            {t("auth:forgotPasswordSubtitle")}
          </p>
        </CardHeader>

        <CardContent>
          {isSuccess ? (
            <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800 dark:border-emerald-900/60 dark:bg-emerald-950/30 dark:text-emerald-200">
              <p className="font-medium">
                {t("auth:forgotPasswordSuccess")}
              </p>
              <div className="mt-4">
                <Link
                  to={loginHref}
                  className="inline-flex items-center justify-center rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2"
                >
                  {t("common:backToSignIn")}
                </Link>
              </div>
            </div>
          ) : (
            <FormProvider {...methods}>
              <form onSubmit={methods.handleSubmit(onSubmit)} className="space-y-4">
                <FormInput
                  name="email"
                  label={t("common:email")}
                  type="email"
                  placeholder={t("auth:emailPlaceholder")}
                  autoComplete="email"
                />

                <Button type="submit" fullWidth isLoading={isLoading}>
                  {t("auth:sendResetLink")}
                </Button>
              </form>
            </FormProvider>
          )}

          {!isSuccess && (
            <div className="mt-6 text-center text-sm">
              <Link
                to={loginHref}
                className="font-medium text-indigo-600 hover:text-indigo-500 dark:text-indigo-400"
              >
                {t("common:backToSignIn")}
              </Link>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
