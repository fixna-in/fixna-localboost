"use client";

import { useRouter } from "next/navigation";
import Link from "next/link";
import { AuthLayout } from "@/components/auth-layout";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { LoginSchema, type LoginInput } from "@/lib/auth-api";
import { useAuth } from "@/lib/auth-context";
import { ErrorState } from "../providers";

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuth();
  const [error, setError] = useState<unknown>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginInput>({ resolver: zodResolver(LoginSchema) });

  return (
    <AuthLayout><section className="auth-card" aria-labelledby="login-heading">
      <p className="eyebrow">YOUR LOCAL GROWTH STARTS HERE</p>
      <h1 id="login-heading">Welcome back.</h1>
      <p className="auth-description">Sign in to your LocalBoost workspace.</p>
      <form
        onSubmit={handleSubmit(async (values) => {
          setError(null);
          try {
            await login(values);
            router.push("/dashboard");
          } catch (err) {
            setError(err);
          }
        })}
      >
        <label>
          Email
          <input type="email" autoComplete="email" {...register("email")} />
        </label>
        {errors.email ? <p role="alert">{errors.email.message}</p> : null}
        <label>
          Password
          <input
            type="password"
            autoComplete="current-password"
            {...register("password")}
          />
        </label>
        {errors.password ? <p role="alert">{errors.password.message}</p> : null}
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Signing in…" : "Sign in"}
        </button>
      </form>
      {error ? <ErrorState error={error} /> : null}
      <p className="auth-footer">New to Fixna? <Link href="/register">Create an account <span aria-hidden="true">↗</span></Link></p>
    </section></AuthLayout>
  );
}
