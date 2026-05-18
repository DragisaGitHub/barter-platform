import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useForm, FormProvider } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { AxiosError } from "axios";
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

function requiresEmailVerification(user: CurrentUserResponse): boolean {
  return !(user.emailVerified === true || user.status === "ACTIVE");
}

export function RegisterPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { register: registerUser } = useAuth();
  const { t } = useTranslation(["auth", "common"]);
  const [isLoading, setIsLoading] = useState(false);
  const redirectPath = getSafeRedirectPath(searchParams.get("redirect"));
  const loginHref = buildPathWithQuery(routePaths.login, { redirect: redirectPath });

  const registerSchema = z
    .object({
      username: z
        .string()
        .min(3, t("auth:usernameMinLength"))
        .max(50, t("auth:usernameMaxLength")),
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
            methods.setError(
              fieldError.field as keyof RegisterFormData,
              { message: fieldError.message }
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

              <FormInput
                name="password"
                label={t("common:password")}
                type="password"
                placeholder={t("auth:createPassword")}
                autoComplete="new-password"
              />

              <FormInput
                name="confirmPassword"
                label={t("common:confirmPassword")}
                type="password"
                placeholder={t("common:confirmPassword")}
                autoComplete="new-password"
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
