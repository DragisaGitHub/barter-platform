import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useForm, FormProvider } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { AxiosError } from "axios";
import { useAuth } from "../../auth/AuthContext";
import { Button } from "../../components/ui/Button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/Card";
import { FormInput } from "../../components/forms/FormInput";
import type { ErrorResponse } from "@/api/generated/types.ts";
import {
  buildPathWithQuery,
  getSafeRedirectPath,
  routePaths,
} from "@/routes/routePaths.ts";

const registerSchema = z
  .object({
    username: z
      .string()
      .min(3, "Username must be at least 3 characters")
      .max(50, "Username must be less than 50 characters"),
    email: z.string().email("Invalid email address"),
    password: z.string().min(8, "Password must be at least 8 characters"),
    confirmPassword: z.string(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
  });

type RegisterFormData = z.infer<typeof registerSchema>;

export function RegisterPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { register: registerUser } = useAuth();
  const [isLoading, setIsLoading] = useState(false);
  const redirectPath = getSafeRedirectPath(searchParams.get("redirect"));
  const loginHref = buildPathWithQuery(routePaths.login, { redirect: redirectPath });

  const methods = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      username: "",
      email: "",
      password: "",
      confirmPassword: "",
    },
  });

  const onSubmit = async (data: RegisterFormData) => {
    setIsLoading(true);
    try {
      await registerUser({
        username: data.username,
        email: data.email,
        password: data.password,
      });
      toast.success("Account created! Please verify your email.");
      navigate(
        buildPathWithQuery(routePaths.verifyEmail, {
          email: data.email,
          redirect: redirectPath,
        })
      );
    } catch (error) {
      if (error instanceof AxiosError) {
        const errorData = error.response?.data as ErrorResponse | undefined;

        if (errorData?.fieldErrors) {
          errorData.fieldErrors.forEach((fieldError) => {
            methods.setError(
              fieldError.field as keyof RegisterFormData,
              { message: fieldError.message }
            );
          });
        } else if (errorData?.message) {
          toast.error(errorData.message);
        } else {
          toast.error("Registration failed. Please try again.");
        }
      } else {
        toast.error("An unexpected error occurred");
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 px-4 py-10 dark:bg-slate-950">
      <div className="mx-auto flex min-h-[calc(100vh-5rem)] w-full max-w-md items-center">
        <Card className="w-full max-w-md border-slate-200/80 shadow-xl shadow-slate-200/50 dark:border-slate-800 dark:bg-slate-900/90 dark:shadow-none">
          <CardHeader>
            <div className="mx-auto mb-3 inline-flex items-center rounded-full border border-violet-200 bg-violet-50 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-violet-700 dark:border-violet-900/70 dark:bg-violet-950/40 dark:text-violet-300">
              Marketplace onboarding
            </div>
            <CardTitle className="text-center">Create Account</CardTitle>
            <p className="text-center text-sm text-slate-600 dark:text-slate-400 mt-2">
              {redirectPath
                ? "Create your account to unlock trade offers and jump back into the listing you selected."
                : "Join the marketplace to list items, send offers, and trade with confidence."}
            </p>
          </CardHeader>
          <CardContent>
            {redirectPath && (
              <div className="mb-4 rounded-lg border border-violet-200 bg-violet-50/80 p-3 text-sm text-violet-800 dark:border-violet-900/60 dark:bg-violet-950/30 dark:text-violet-200">
                Finish sign up now and we’ll keep your marketplace destination ready for after login.
              </div>
            )}

          <FormProvider {...methods}>
            <form onSubmit={methods.handleSubmit(onSubmit)} className="space-y-4">
              <FormInput
                name="username"
                label="Username"
                type="text"
                placeholder="Choose a username"
                autoComplete="username"
              />

              <FormInput
                name="email"
                label="Email"
                type="email"
                placeholder="Enter your email"
                autoComplete="email"
              />

              <FormInput
                name="password"
                label="Password"
                type="password"
                placeholder="Create a password"
                autoComplete="new-password"
              />

              <FormInput
                name="confirmPassword"
                label="Confirm Password"
                type="password"
                placeholder="Confirm your password"
                autoComplete="new-password"
              />

              <Button type="submit" fullWidth isLoading={isLoading}>
                Create Account
              </Button>

              <p className="text-center text-xs leading-5 text-slate-500 dark:text-slate-400">
                We’ll email you a 6-digit verification code before your first sign in.
              </p>
            </form>
          </FormProvider>

          <div className="mt-6 text-center text-sm">
            <span className="text-slate-600 dark:text-slate-400">
              Already have an account?{" "}
            </span>
            <Link
              to={loginHref}
              className="font-medium text-indigo-600 hover:text-indigo-500 dark:text-indigo-400"
            >
              Sign in
            </Link>
          </div>

          <div className="mt-4 text-center">
            <Link
              to={routePaths.marketplace}
              className="text-sm text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-100"
            >
              Back to marketplace
            </Link>
          </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
