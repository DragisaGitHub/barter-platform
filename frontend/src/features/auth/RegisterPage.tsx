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
import type { ErrorResponse } from "../../api/generated/types";

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
  const { register: registerUser } = useAuth();
  const [isLoading, setIsLoading] = useState(false);

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
      toast.success("Registration successful! Please log in.");
      navigate("/login");
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
    <div className="min-h-screen flex items-center justify-center px-4 py-12">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle className="text-center">Create Account</CardTitle>
          <p className="text-center text-sm text-slate-600 dark:text-slate-400 mt-2">
            Join our barter marketplace today
          </p>
        </CardHeader>
        <CardContent>
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
            </form>
          </FormProvider>

          <div className="mt-6 text-center text-sm">
            <span className="text-slate-600 dark:text-slate-400">
              Already have an account?{" "}
            </span>
            <Link
              to="/login"
              className="font-medium text-indigo-600 hover:text-indigo-500 dark:text-indigo-400"
            >
              Sign in
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
