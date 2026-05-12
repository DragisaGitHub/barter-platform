import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useForm, FormProvider, useFormContext } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { AxiosError } from "axios";
import { Eye, EyeOff, ShieldCheck } from "lucide-react";
import { resetPassword } from "@/api/authApi.ts";
import { Button } from "../../components/ui/Button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/Card";
import { parseApiError } from "@/utils";
import type { ErrorResponse } from "@/api/generated/types.ts";
import { routePaths } from "@/routes/routePaths.ts";

const resetPasswordSchema = z
  .object({
    password: z.string().min(8, "Password must be at least 8 characters"),
    confirmPassword: z.string().min(1, "Confirm your password"),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
  });

type ResetPasswordFormData = z.infer<typeof resetPasswordSchema>;

function PasswordField({
  name,
  label,
  placeholder,
  autoComplete,
  showPassword,
  onToggle,
}: {
  name: "password" | "confirmPassword";
  label: string;
  placeholder: string;
  autoComplete: string;
  showPassword: boolean;
  onToggle: () => void;
}) {
  const {
    register,
    formState: { errors },
  } = useFormContext<ResetPasswordFormData>();

  const error = errors[name]?.message as string | undefined;

  return (
    <div className="w-full">
      <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">
        {label}
      </label>
      <div className="relative">
        <input
          {...register(name)}
          type={showPassword ? "text" : "password"}
          placeholder={placeholder}
          autoComplete={autoComplete}
          className={[
            "w-full rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm text-slate-900 placeholder:text-slate-400 transition-colors duration-150",
            "focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20",
            "disabled:bg-slate-50 disabled:text-slate-500 disabled:cursor-not-allowed",
            "dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder:text-slate-500",
            "dark:focus:border-indigo-400 dark:focus:ring-indigo-400/20",
            error && "border-red-500 focus:border-red-500 focus:ring-red-500/20",
            "pr-12",
          ].join(" ")}
        />
        <button
          type="button"
          onClick={onToggle}
          className="absolute inset-y-0 right-0 flex items-center px-3 text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200"
          aria-label={showPassword ? `Hide ${label.toLowerCase()}` : `Show ${label.toLowerCase()}`}
        >
          {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
        </button>
      </div>
      {error && <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">{error}</p>}
    </div>
  );
}

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const email = searchParams.get("email") ?? "";
  const token = searchParams.get("token") ?? "";
  const hasValidLink = Boolean(email && token);

  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const methods = useForm<ResetPasswordFormData>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: {
      password: "",
      confirmPassword: "",
    },
  });

  const formError = methods.formState.errors.root?.message as string | undefined;

  const showFormError = (message: string) => {
    methods.setError("root", { type: "manual", message });
  };

  const onSubmit = async (data: ResetPasswordFormData) => {
    methods.clearErrors("root");

    if (!hasValidLink) {
      showFormError("This password reset link is incomplete. Please request a new password reset email.");
      return;
    }

    setIsLoading(true);
    try {
      await resetPassword(email, token, data.password);
      setIsSuccess(true);
    } catch (error) {
      if (error instanceof AxiosError) {
        const errorData = error.response?.data as ErrorResponse | undefined;
        if (errorData?.fieldErrors?.length) {
          errorData.fieldErrors.forEach((fieldError) => {
            methods.setError(fieldError.field as keyof ResetPasswordFormData, {
              message: fieldError.message,
            });
          });
          showFormError(errorData.message ?? "Please correct the highlighted errors.");
        } else {
          const message = parseApiError(error);
          showFormError(message);
        }
      } else {
        showFormError("An unexpected error occurred");
      }
    } finally {
      setIsLoading(false);
    }
  };

  const onInvalid = () => {
    const firstErrorMessage =
      methods.formState.errors.password?.message ??
      methods.formState.errors.confirmPassword?.message ??
      "Please fix the errors below.";

    showFormError(firstErrorMessage as string);
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-12">
      <Card className="w-full max-w-md">
        <CardHeader>
          <div className="flex justify-center mb-3">
            <div className="rounded-full bg-indigo-100 dark:bg-indigo-900/30 p-3">
              <ShieldCheck className="h-6 w-6 text-indigo-600 dark:text-indigo-400" />
            </div>
          </div>
          <CardTitle className="text-center">Reset password</CardTitle>
          <p className="text-center text-sm text-slate-600 dark:text-slate-400 mt-2">
            Choose a new password for your account.
          </p>
        </CardHeader>

        <CardContent>
          {isSuccess ? (
            <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800 dark:border-emerald-900/60 dark:bg-emerald-950/30 dark:text-emerald-200">
              <p className="font-medium">Your password has been reset successfully.</p>
              <p className="mt-1">You can now sign in with your new password.</p>
              <div className="mt-4">
                <Link
                  to={routePaths.login}
                  className="inline-flex items-center justify-center rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2"
                >
                  Back to sign in
                </Link>
              </div>
            </div>
          ) : (
            <FormProvider {...methods}>
              <form noValidate onSubmit={methods.handleSubmit(onSubmit, onInvalid)} className="space-y-4">
                {formError && (
                  <div className="rounded-lg border border-red-300 bg-red-50 p-4 text-sm text-red-800 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-200">
                    {formError}
                  </div>
                )}

                {!hasValidLink && (
                  <div className="rounded-lg border border-amber-300 bg-amber-50 p-4 text-sm text-amber-800 dark:border-amber-800 dark:bg-amber-950/30 dark:text-amber-200">
                    This reset link is missing required details. Please request a new password reset email.
                  </div>
                )}

                <PasswordField
                  name="password"
                  label="New password"
                  placeholder="Enter a new password"
                  autoComplete="new-password"
                  showPassword={showPassword}
                  onToggle={() => setShowPassword((current) => !current)}
                />

                <PasswordField
                  name="confirmPassword"
                  label="Confirm password"
                  placeholder="Confirm your new password"
                  autoComplete="new-password"
                  showPassword={showPassword}
                  onToggle={() => setShowPassword((current) => !current)}
                />

                <Button type="submit" fullWidth isLoading={isLoading}>
                  Reset password
                </Button>
              </form>
            </FormProvider>
          )}

          {!isSuccess && (
            <div className="mt-6 text-center text-sm">
              <Link
                to={routePaths.login}
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
