import { describe, expect, it } from "vitest";

import { loginSchema, registerSchema } from "./validation";

describe("authentication validation", () => {
  it("accepts backend-compatible login credentials", () => {
    expect(loginSchema.safeParse({ email: "agent@flowpilot.test", password: "StrongPass123" }).success).toBe(true);
  });

  it("rejects malformed credentials before calling the backend", () => {
    expect(loginSchema.safeParse({ email: "not-an-email", password: "short" }).success).toBe(false);
  });

  it("allows only backend-supported roles", () => {
    const base = { email: "admin@flowpilot.test", displayName: "Admin User", password: "StrongPass123" };
    expect(registerSchema.safeParse({ ...base, role: "ADMIN" }).success).toBe(true);
    expect(registerSchema.safeParse({ ...base, role: "OWNER" }).success).toBe(false);
  });
});
