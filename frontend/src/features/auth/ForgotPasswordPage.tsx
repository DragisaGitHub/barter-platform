import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useForm, FormProvider } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { AxiosError } from "axios";
import { Mail } from "lucide-react";
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

const forgotPasswordSchema = z.object({
  email: z.email("Enter a valid email address"),
});

type ForgotPasswordFormData = z.infer<typeof forgotPasswordSchema>;

export function ForgotPasswordPage() {
  const [searchParams] = useSearchParams();
  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const redirectPath = getSafeRedirectPath(searchParams.get("redirect"));
  const loginHref = buildPathWithQuery(routePaths.login, { redirect: redirectPath });

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
        methods.setError("email", { message: "An unexpected error occurred" });
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
          <CardTitle className="text-center">Forgot password</CardTitle>
          <p className="text-center text-sm text-slate-600 dark:text-slate-400 mt-2">
            Enter your email address and we’ll send you a reset link.
          </p>
        </CardHeader>

        <CardContent>
          {isSuccess ? (
            <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800 dark:border-emerald-900/60 dark:bg-emerald-950/30 dark:text-emerald-200">
              <p className="font-medium">
                If an account exists for this email, a password reset link has been sent.
              </p>
              <div className="mt-4">
                <Link
                  to={loginHref}
                  className="inline-flex items-center justify-center rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2"
                >
                  Back to sign in
                </Link>
              </div>
            </div>
          ) : (
            <FormProvider {...methods}>
              <form onSubmit={methods.handleSubmit(onSubmit)} className="space-y-4">
                <FormInput
                  name="email"
                  label="Email"
                  type="email"
                  placeholder="Enter your email"
                  autoComplete="email"
                />

                <Button type="submit" fullWidth isLoading={isLoading}>
                  Send reset link
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
                Back to sign in
              </Link>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
