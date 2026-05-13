import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useForm, FormProvider, useFormContext } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { AxiosError } from "axios";
import { Eye, EyeOff } from "lucide-react";
import { useAuth } from "../../auth/AuthContext";
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

const loginSchema = z.object({
  identifier: z.string().min(1, "Email or username is required"),
  password: z.string().min(8, "Password must be at least 8 characters"),
});

type LoginFormData = z.infer<typeof loginSchema>;

function PasswordField({
  showPassword,
  onToggle,
}: {
  showPassword: boolean;
  onToggle: () => void;
}) {
  const {
    register,
    formState: { errors },
  } = useFormContext<LoginFormData>();

  const error = errors.password?.message as string | undefined;

  return (
    <div className="w-full">
      <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">
        Password
      </label>
      <div className="relative">
        <input
          {...register("password")}
          type={showPassword ? "text" : "password"}
          placeholder="Enter your password"
          autoComplete="current-password"
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
          aria-label={showPassword ? "Hide password" : "Show password"}
        >
          {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
        </button>
      </div>
      {error && <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">{error}</p>}
    </div>
  );
}

export function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login } = useAuth();
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  // null = no banner; string = show banner (empty string means email unknown)
  const [unverifiedEmail, setUnverifiedEmail] = useState<string | null>(null);
  const redirectPath = getSafeRedirectPath(searchParams.get("redirect"));
  const registerHref = buildPathWithQuery(routePaths.register, { redirect: redirectPath });
  const forgotPasswordHref = buildPathWithQuery(routePaths.forgotPassword, {
    redirect: redirectPath,
  });
  const verifyEmailHref = buildPathWithQuery(routePaths.verifyEmail, {
    email: unverifiedEmail || undefined,
    redirect: redirectPath,
  });

  const methods = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      identifier: "",
      password: "",
    },
  });

  const formError = methods.formState.errors.root?.message as string | undefined;
  const invalidLoginMessage = "Invalid email/username or password.";

  const showFormError = (message: string) => {
    methods.setError("root", { type: "manual", message });
  };

  const onSubmit = async (data: LoginFormData) => {
    setIsLoading(true);
    setUnverifiedEmail(null);
    methods.clearErrors("root");
    try {
      await login(data);
      toast.success("Login successful");
      navigate(redirectPath ?? routePaths.dashboard);
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

        if (
          status === 400 ||
          status === 401 ||
          msg.toLowerCase().includes("invalid credentials") ||
          msg.toLowerCase().includes("request validation failed")
        ) {
          showFormError(invalidLoginMessage);
          toast.error(invalidLoginMessage);
          return;
        }

        const message = parseApiError(error);
        showFormError(message);
        toast.error(message);
      } else {
        const message = parseApiError(error);
        showFormError(message);
        toast.error(message);
      }
    } finally {
      setIsLoading(false);
    }
  };

  const onInvalid = () => {
    const firstErrorMessage =
      methods.formState.errors.identifier?.message ??
      methods.formState.errors.password?.message ??
      "Please fix the errors below.";

    showFormError(firstErrorMessage as string);
  };

  return (
    <div className="min-h-screen bg-slate-50 px-4 py-10 dark:bg-slate-950">
      <div className="mx-auto flex min-h-[calc(100vh-5rem)] w-full max-w-md items-center">
        <Card className="w-full max-w-md border-slate-200/80 shadow-xl shadow-slate-200/50 dark:border-slate-800 dark:bg-slate-900/90 dark:shadow-none">
          <CardHeader>
            <div className="mx-auto mb-3 inline-flex items-center rounded-full border border-violet-200 bg-violet-50 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-violet-700 dark:border-violet-900/70 dark:bg-violet-950/40 dark:text-violet-300">
              Marketplace sign in
            </div>
            <CardTitle className="text-center">Welcome Back</CardTitle>
            <p className="text-center text-sm text-slate-600 dark:text-slate-400 mt-2">
              {redirectPath
                ? "Sign in to get back to the listing you were viewing and continue your trade flow."
                : "Sign in to continue trading, managing listings, and following your marketplace activity."}
            </p>
          </CardHeader>
          <CardContent>
            {redirectPath && (
              <div className="mb-4 rounded-lg border border-violet-200 bg-violet-50/80 p-3 text-sm text-violet-800 dark:border-violet-900/60 dark:bg-violet-950/30 dark:text-violet-200">
                You’ll return to your selected marketplace item right after sign in.
              </div>
            )}

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
                  to={verifyEmailHref}
                  className="inline-block rounded-md bg-amber-600 hover:bg-amber-700 text-white px-3 py-1.5 text-sm font-medium transition-colors"
                >
                  Verify email →
                </Link>
              </div>
            </div>
          )}

          <FormProvider {...methods}>
            <form noValidate onSubmit={methods.handleSubmit(onSubmit, onInvalid)} className="space-y-4">
              {formError && (
                <div className="rounded-lg border border-red-300 bg-red-50 p-4 text-sm text-red-800 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-200">
                  {formError}
                </div>
              )}

              <FormInput
                name="identifier"
                label="Email or Username"
                type="text"
                placeholder="Enter your email or username"
                autoComplete="username"
              />

              <PasswordField
                showPassword={showPassword}
                onToggle={() => setShowPassword((current) => !current)}
              />

              <div className="flex justify-end -mt-2">
                <Link
                  to={forgotPasswordHref}
                  className="text-sm font-medium text-indigo-600 hover:text-indigo-500 dark:text-indigo-400"
                >
                  Forgot password?
                </Link>
              </div>

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
              to={registerHref}
              className="font-medium text-indigo-600 hover:text-indigo-500 dark:text-indigo-400"
            >
              Sign up
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
