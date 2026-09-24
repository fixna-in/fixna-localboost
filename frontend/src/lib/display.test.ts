import { describe, expect, it } from "vitest";
import { formatMetric } from "./display";

describe("dashboard metric presentation", () => {
  it.each([undefined, null, "", "  ", "not a number", NaN, Infinity])(
    "shows a dash for missing or invalid values: %s",
    (value) => expect(formatMetric(value)).toBe("—"),
  );

  it("preserves measured zero rather than displaying missing data", () => {
    expect(formatMetric(0)).toBe("0");
    expect(formatMetric("0")).toBe("0");
  });

  it("uses Indian digit grouping and rounds to two decimal places", () => {
    expect(formatMetric(1234567.125)).toBe("12,34,567.13");
    expect(formatMetric("1250.50")).toBe("1,250.5");
  });
});
