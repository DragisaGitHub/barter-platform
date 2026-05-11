import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useForm, FormProvider } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { AxiosError } from "axios";
import { useAuth } from "../../auth/AuthContext";
import { Button } from "../../components/ui/Button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/Card";
import { FormInput } from "../../components/forms/FormInput";
import { parseApiError } from "@/utils";
import type { ErrorResponse } from "@/api/generated/types.ts";

const loginSchema = z.object({
  identifier: z.string().min(1, "Email or username is required"),
  password: z.string().min(1, "Password is required"),
});

type LoginFormData = z.infer<typeof loginSchema>;

export function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [isLoading, setIsLoading] = useState(false);
  // null = no banner; string = show banner (empty string means email unknown)
  const [unverifiedEmail, setUnverifiedEmail] = useState<string | null>(null);

  const methods = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      identifier: "",
      password: "",
    },
  });

  const onSubmit = async (data: LoginFormData) => {
    setIsLoading(true);
    setUnverifiedEmail(null);
    try {
      await login(data);
      toast.success("Login successful");
      navigate("/dashboard");
    } catch (error) {
      if (error instanceof AxiosError) {
        const status = error.response?.status;
        const errorData = error.response?.data as ErrorResponse | undefined;
        const msg = errorData?.message ?? "";

        // 403 FORBIDDEN — treat as email-not-verified
        if (
          status === 403 &&
          (msg.toLowerCase().includes("verif") || msg.toLowerCase().includes("email"))
        ) {
          const identifier = data.identifier;
          const isEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(identifier);
          // store email if we have it, empty string if only username was provided
          setUnverifiedEmail(isEmail ? identifier : "");
          return;
        }

        toast.error(parseApiError(error));
      } else {
        toast.error(parseApiError(error));
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle className="text-center">Welcome Back</CardTitle>
          <p className="text-center text-sm text-slate-600 dark:text-slate-400 mt-2">
            Sign in to your account to continue
          </p>
        </CardHeader>
        <CardContent>
          {/* Email verification required banner */}
          {unverifiedEmail !== null && (
            <div className="mb-4 rounded-lg border border-amber-300 bg-amber-50 dark:bg-amber-900/20 dark:border-amber-700 p-4 text-sm">
              <p className="font-medium text-amber-800 dark:text-amber-300 mb-1">
                Email verification required
              </p>
              {unverifiedEmail ? (
                <p className="text-amber-700 dark:text-amber-400">
                  Your account (<span className="font-medium">{unverifiedEmail}</span>) has not
                  been verified yet. Check your inbox for the 6-digit code.
                </p>
              ) : (
                <p className="text-amber-700 dark:text-amber-400">
                  Your account email is not verified. Please go to the verification
                  page and enter your email address.
                </p>
              )}
              <div className="mt-3">
                <Link
                  to={
                    unverifiedEmail
                      ? `/verify-email?email=${encodeURIComponent(unverifiedEmail)}`
                      : "/verify-email"
                  }
                  className="inline-block rounded-md bg-amber-600 hover:bg-amber-700 text-white px-3 py-1.5 text-sm font-medium transition-colors"
                >
                  Verify email →
                </Link>
              </div>
            </div>
          )}

          <FormProvider {...methods}>
            <form onSubmit={methods.handleSubmit(onSubmit)} className="space-y-4">
              <FormInput
                name="identifier"
                label="Email or Username"
                type="text"
                placeholder="Enter your email or username"
                autoComplete="username"
              />

              <FormInput
                name="password"
                label="Password"
                type="password"
                placeholder="Enter your password"
                autoComplete="current-password"
              />

              <Button type="submit" fullWidth isLoading={isLoading}>
                Sign In
              </Button>
            </form>
          </FormProvider>

          <div className="mt-6 text-center text-sm">
            <span className="text-slate-600 dark:text-slate-400">
              Don't have an account?{" "}
            </span>
            <Link
              to="/register"
              className="font-medium text-indigo-600 hover:text-indigo-500 dark:text-indigo-400"
            >
              Sign up
            </Link>
          </div>

          <div className="mt-4 text-center">
            <Link
              to="/"
              className="text-sm text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-100"
            >
              Back to home
            </Link>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
