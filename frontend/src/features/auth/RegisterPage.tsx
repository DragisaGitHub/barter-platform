import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useForm, FormProvider, useFormContext } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { AxiosError } from "axios";
import { Eye, EyeOff } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useAuth } from "../../auth/AuthContext";
import { Button } from "../../components/ui/Button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/Card";
import { FormInput } from "../../components/forms/FormInput";
import type { CurrentUserResponse, ErrorResponse } from "@/api/generated/types.ts";
import {
  buildPathWithQuery,
  getSafeRedirectPath,
  routePaths,
} from "@/routes/routePaths.ts";

type RegisterFormData = {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
};

const USERNAME_PATTERN = /^[A-Za-z0-9](?:[A-Za-z0-9_.-]{1,78}[A-Za-z0-9])?$/;
const USERNAME_PATTERN_SOURCE = USERNAME_PATTERN.source;

function requiresEmailVerification(user: CurrentUserResponse): boolean {
  return !(user.emailVerified || user.status === "ACTIVE");
}

function shouldUseFriendlyUsernameMessage(field: keyof RegisterFormData, message?: string): boolean {
  if (field !== "username" || !message) {
    return false;
  }

  const normalizedMessage = message.toLowerCase();

  return (
    message.includes(USERNAME_PATTERN_SOURCE) ||
    normalizedMessage.includes("must match") ||
    normalizedMessage.includes("must not contain blank spaces")
  );
}

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
  const { t } = useTranslation(["auth", "common"]);
  const {
    register,
    formState: { errors },
  } = useFormContext<RegisterFormData>();

  const error = errors[name]?.message as string | undefined;

  return (
    <div className="w-full">
      <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">
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
            "disabled:cursor-not-allowed disabled:bg-slate-50 disabled:text-slate-500",
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
          aria-label={showPassword ? t("auth:hidePassword") : t("auth:showPassword")}
        >
          {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
        </button>
      </div>
      {error && <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">{error}</p>}
    </div>
  );
}

export function RegisterPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { register: registerUser } = useAuth();
  const { t } = useTranslation(["auth", "common"]);
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const redirectPath = getSafeRedirectPath(searchParams.get("redirect"));
  const loginHref = buildPathWithQuery(routePaths.login, { redirect: redirectPath });

  const registerSchema = z
    .object({
      username: z
        .string()
        .min(3, t("auth:usernameMinLength"))
        .max(50, t("auth:usernameMaxLength"))
        .regex(USERNAME_PATTERN, t("auth:usernameInvalidPattern")),
      email: z.string().email(t("auth:invalidEmail")),
      password: z.string().min(8, t("auth:passwordMinLength")),
      confirmPassword: z.string(),
    })
    .refine((data) => data.password === data.confirmPassword, {
      message: t("auth:passwordsDoNotMatch"),
      path: ["confirmPassword"],
    });

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
      const registeredUser = await registerUser({
        username: data.username,
        email: data.email,
        password: data.password,
      });

      if (requiresEmailVerification(registeredUser)) {
        toast.success(t("auth:accountCreated"));
        navigate(
          buildPathWithQuery(routePaths.verifyEmail, {
            email: registeredUser.email || data.email,
            redirect: redirectPath,
          })
        );
        return;
      }

      toast.success(t("auth:accountReady"));
      navigate(
        buildPathWithQuery(routePaths.login, {
          redirect: redirectPath,
          registered: "true",
        })
      );
    } catch (error) {
      if (error instanceof AxiosError) {
        const errorData = error.response?.data as ErrorResponse | undefined;

        if (errorData?.fieldErrors) {
          errorData.fieldErrors.forEach((fieldError) => {
            const fieldName = fieldError.field as keyof RegisterFormData;

            methods.setError(
              fieldName,
              {
                message: shouldUseFriendlyUsernameMessage(fieldName, fieldError.message)
                  ? t("auth:usernameInvalidPattern")
                  : fieldError.message,
              }
            );
          });
        } else if (errorData?.message) {
          toast.error(errorData.message);
        } else {
          toast.error(t("auth:registrationFailed"));
        }
      } else {
        toast.error(t("auth:unexpectedError"));
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
              {t("auth:marketplaceOnboarding")}
            </div>
            <CardTitle className="text-center">{t("auth:createAccount")}</CardTitle>
            <p className="text-center text-sm text-slate-600 dark:text-slate-400 mt-2">
              {redirectPath
                ? t("auth:redirectRegisterCta")
                : t("auth:registerCta")}
            </p>
          </CardHeader>
          <CardContent>
            {redirectPath && (
              <div className="mb-4 rounded-lg border border-violet-200 bg-violet-50/80 p-3 text-sm text-violet-800 dark:border-violet-900/60 dark:bg-violet-950/30 dark:text-violet-200">
                {t("auth:finishSignup")}
              </div>
            )}

          <FormProvider {...methods}>
            <form onSubmit={methods.handleSubmit(onSubmit)} className="space-y-4">
              <FormInput
                name="username"
                label={t("auth:username")}
                type="text"
                placeholder={t("auth:usernamePlaceholder")}
                autoComplete="username"
              />

              <FormInput
                name="email"
                label={t("common:email")}
                type="email"
                placeholder={t("auth:emailPlaceholder")}
                autoComplete="email"
              />

              <PasswordField
                name="password"
                label={t("common:password")}
                placeholder={t("auth:createPassword")}
                autoComplete="new-password"
                showPassword={showPassword}
                onToggle={() => setShowPassword((current) => !current)}
              />

              <PasswordField
                name="confirmPassword"
                label={t("common:confirmPassword")}
                placeholder={t("common:confirmPassword")}
                autoComplete="new-password"
                showPassword={showConfirmPassword}
                onToggle={() => setShowConfirmPassword((current) => !current)}
              />

              <Button type="submit" fullWidth isLoading={isLoading}>
                {t("auth:createAccountButton")}
              </Button>

              <p className="text-center text-xs leading-5 text-slate-500 dark:text-slate-400">
                {t("auth:verificationHint")}
              </p>
            </form>
          </FormProvider>

          <div className="mt-6 text-center text-sm">
            <span className="text-slate-600 dark:text-slate-400">
              {t("auth:alreadyHaveAccount")} {" "}
            </span>
            <Link
              to={loginHref}
              className="font-medium text-indigo-600 hover:text-indigo-500 dark:text-indigo-400"
            >
              {t("auth:signIn")}
            </Link>
          </div>

          <div className="mt-4 text-center">
            <Link
              to={routePaths.marketplace}
              className="text-sm text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-100"
            >
              {t("common:backToMarketplace")}
            </Link>
          </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
