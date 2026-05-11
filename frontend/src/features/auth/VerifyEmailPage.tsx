import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useForm, FormProvider } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { AxiosError } from "axios";
import { Mail, RefreshCw } from "lucide-react";
import { verifyEmail, resendVerificationCode } from "@/api/authApi.ts";
import { Button } from "../../components/ui/Button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/Card";
import { FormInput } from "../../components/forms/FormInput";
import { parseApiError } from "@/utils";
import type { ErrorResponse } from "@/api/generated/types.ts";

const verifyEmailSchema = z.object({
  email: z.string().email("Enter a valid email address"),
  code: z
    .string()
    .length(6, "Verification code must be 6 digits")
    .regex(/^\d{6}$/, "Code must be 6 digits"),
});

type VerifyEmailFormData = z.infer<typeof verifyEmailSchema>;

export function VerifyEmailPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const emailFromParam = searchParams.get("email") ?? "";

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isResending, setIsResending] = useState(false);

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
      toast.success(result.message ?? "Email verified successfully!");
      navigate("/login");
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
        toast.error("An unexpected error occurred");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleResend = async () => {
    const email = currentEmail.trim();
    if (!email) {
      methods.setError("email", { message: "Enter your email to resend the code" });
      return;
    }

    setIsResending(true);
    try {
      const result = await resendVerificationCode(email);
      toast.success(result.message ?? "Verification code resent!");
    } catch (error) {
      toast.error(parseApiError(error));
    } finally {
      setIsResending(false);
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
          <CardTitle className="text-center">Check your email</CardTitle>
          <p className="text-center text-sm text-slate-600 dark:text-slate-400 mt-2">
            {emailFromParam
              ? <>We sent a 6-digit verification code to <span className="font-medium text-slate-800 dark:text-slate-200">{emailFromParam}</span>.</>
              : "Enter your email and the 6-digit code we sent you."}
          </p>
        </CardHeader>

        <CardContent>
          <FormProvider {...methods}>
            <form onSubmit={methods.handleSubmit(onSubmit)} className="space-y-4">
              {/* Show email field only when not pre-filled from query param */}
              {!emailFromParam && (
                <FormInput
                  name="email"
                  label="Email"
                  type="email"
                  placeholder="Enter your email"
                  autoComplete="email"
                />
              )}

              <FormInput
                name="code"
                label="Verification Code"
                type="text"
                placeholder="Enter 6-digit code"
                autoComplete="one-time-code"
                inputMode="numeric"
                maxLength={6}
              />

              <Button type="submit" fullWidth isLoading={isSubmitting}>
                Verify Email
              </Button>
            </form>
          </FormProvider>

          {/* Resend section */}
          <div className="mt-5 flex flex-col items-center gap-2">
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Didn't receive a code?
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
              Resend code
            </Button>
          </div>

          <div className="mt-5 text-center">
            <Link
              to="/login"
              className="text-sm text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-100"
            >
              ← Back to sign in
            </Link>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

