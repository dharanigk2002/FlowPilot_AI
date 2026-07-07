import { describe, expect, it } from "vitest";

import { loginSchema, registerSchema } from "./validation";

describe("authentication validation", () => {
  it("accepts backend-compatible login credentials", () => {
    expect(loginSchema.safeParse({ email: "agent@flowpilot.test", password: "StrongPass123" }).success).toBe(true);
  });

  it("rejects malformed credentials before calling the backend", () => {
    expect(loginSchema.safeParse({ email: "not-an-email", password: "short" }).success).toBe(false);
  });

  it("validates public registration without accepting a role choice", () => {
    expect(registerSchema.safeParse({
      email: "agent@flowpilot.test",
      displayName: "Agent User",
      password: "StrongPass123",
    }).success).toBe(true);
  });
});
