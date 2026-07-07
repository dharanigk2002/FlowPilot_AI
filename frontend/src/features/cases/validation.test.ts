import { describe, expect, it } from "vitest";

import { createCaseSchema } from "./validation";

const validCase = {
  title: "Premium delivery delayed",
  description: "The order is five days late.",
  customerName: "Rahul Sharma",
  customerEmail: "rahul@example.com",
  customerTier: "PREMIUM",
  orderReference: "SWC-85000",
  orderValue: "85000.00",
  priority: "HIGH",
};

describe("case validation", () => {
  it("accepts a valid case payload", () => {
    expect(createCaseSchema.safeParse(validCase).success).toBe(true);
  });

  it("rejects negative amounts and invalid email addresses", () => {
    const result = createCaseSchema.safeParse({ ...validCase, customerEmail: "invalid", orderValue: "-1" });
    expect(result.success).toBe(false);
  });
});
