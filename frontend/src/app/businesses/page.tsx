"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import {
  BusinessInputSchema,
  createBusiness,
  listBusinesses,
  type BusinessInput,
} from "@/lib/tenant-business-api";
import { ErrorState, LoadingState } from "../providers";
import { PageHeader, EmptyState } from "@/components/ui";

export default function BusinessesPage() {
  const [error, setError] = useState<unknown>(null);
  const businesses = useQuery({
    queryKey: ["businesses"],
    queryFn: listBusinesses,
    retry: false,
  });
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<BusinessInput>({ resolver: zodResolver(BusinessInputSchema) });

  return (
    <section aria-label="Businesses">
      <PageHeader title="Your businesses" description="The places, people, and local stories behind your campaigns." />
      <form
        onSubmit={handleSubmit(async (values) => {
          setError(null);
          try {
            await createBusiness(values);
            reset();
            businesses.refetch();
          } catch (err) {
            setError(err);
          }
        })}
      >
        <h2>Add a business</h2>
        <label>
          Business name
          <input {...register("name")} />
        </label>
        {errors.name ? <p role="alert">{errors.name.message}</p> : null}
        <label>
          Category
          <input {...register("category")} />
        </label>
        <label>
          Phone
          <input {...register("phone")} />
        </label>
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Creating…" : "Add business"}
        </button>
      </form>
      {error ? <ErrorState error={error} /> : null}
      {businesses.isLoading ? (
        <LoadingState label="Loading businesses" />
      ) : businesses.error ? (
        <ErrorState error={businesses.error} />
      ) : (
        businesses.data?.length ? <ul className="resource-list">
          {businesses.data.map((b) => (
            <li key={b.id}>
              <span className="eyebrow">BUSINESS</span>
              <h2><Link href={`/businesses/${b.id}`}>{b.name} ↗</Link></h2>
              <p>{b.category || "No category added"}</p>
              {b.phone ? <p>{b.phone}</p> : null}
            </li>
          ))}
        </ul> : <EmptyState title="Your business belongs here" description="Add your first business using the form above, then set up its locations and campaigns." />
      )}
    </section>
  );
}
