const numberFormat = new Intl.NumberFormat("en-IN", { maximumFractionDigits: 2 });

/** Missing values stay distinct from measured zero. */
export function formatMetric(value: number | string | null | undefined): string {
  if (value == null || (typeof value === "string" && value.trim() === "")) return "—";
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numberFormat.format(numeric) : "—";
}
