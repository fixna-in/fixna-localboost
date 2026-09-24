"use client";

import { useState } from "react";
import {
  QueryClient,
  QueryClientProvider,
  useQuery,
} from "@tanstack/react-query";
import { apiClient, toApiError } from "@/lib/api-client";
import { AuthProvider } from "@/lib/auth-context";

/** Shared loading skeleton for async states. */
export function LoadingState({ label }: { label: string }) {
  return (
    <p className="loading-state" role="status" aria-live="polite">
      {label}…
    </p>
  );
}

/** Accessible error banner driven by the API error envelope. */
export function ErrorState({ error }: { error: unknown }) {
  const apiError = toApiError(error);
  return (
    <div role="alert">
      <p>
        {apiError.code}: {apiError.message}
      </p>
      {apiError.requestId ? <p>Request ID: {apiError.requestId}</p> : null}
    </div>
  );
}

function HealthProbe() {
  const { data, error, isLoading } = useQuery({
    queryKey: ["health"],
    queryFn: async () => {
      const response = await apiClient.get("/health");
      return response.data as { status: string; service: string };
    },
    retry: false,
  });
  if (isLoading) return <LoadingState label="Checking API status" />;
  if (error) return <ErrorState error={error} />;
  return <p role="status">API: {data?.status ?? "unknown"}</p>;
}

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: { staleTime: 30_000, retry: 1, refetchOnWindowFocus: false },
        },
      }),
  );
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>{children}</AuthProvider>
    </QueryClientProvider>
  );
}

export { HealthProbe };

