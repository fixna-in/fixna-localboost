"use client";

import { useRouter } from "next/navigation";
import Link from "next/link";
import { AuthLayout } from "@/components/auth-layout";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { RegisterSchema, type RegisterInput } from "@/lib/auth-api";
import { useAuth } from "@/lib/auth-context";
import { ErrorState } from "../providers";

export default function RegisterPage() {
  const router = useRouter();
  const { register: signup } = useAuth();
  const [error, setError] = useState<unknown>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterInput>({ resolver: zodResolver(RegisterSchema) });

  return (
    <AuthLayout><section className="auth-card" aria-labelledby="register-heading">
      <p className="eyebrow">MAKE YOUR NEXT LOCAL MOVE</p>
      <h1 id="register-heading">Let’s grow local.</h1>
      <p className="auth-description">Create your account and business workspace.</p>
      <form
        onSubmit={handleSubmit(async (values) => {
          setError(null);
          try {
            await signup(values);
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
          Password (min 8)
          <input
            type="password"
            autoComplete="new-password"
            {...register("password")}
          />
        </label>
        {errors.password ? <p role="alert">{errors.password.message}</p> : null}
        <label>
          First name
          <input {...register("firstName")} />
        </label>
        <label>
          Last name
          <input {...register("lastName")} />
        </label>
        <label>
          Business / tenant name
          <input {...register("tenantName")} />
        </label>
        {errors.tenantName ? (
          <p role="alert">{errors.tenantName.message}</p>
        ) : null}
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Creating…" : "Create account"}
        </button>
      </form>
      {error ? <ErrorState error={error} /> : null}
      <p className="auth-footer">Already have an account? <Link href="/login">Sign in <span aria-hidden="true">↗</span></Link></p>
    </section></AuthLayout>
  );
}
