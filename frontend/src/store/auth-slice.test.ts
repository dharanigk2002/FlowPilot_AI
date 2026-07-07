import { describe, expect, it } from "vitest";

import reducer, { authenticated, checkingSession, unauthenticated } from "./auth-slice";

const user = {
  id: 7,
  email: "agent@flowpilot.test",
  displayName: "Support Agent",
  role: "SUPPORT_AGENT" as const,
};

describe("auth reducer", () => {
  it("moves from checking to authenticated without storing a token", () => {
    const state = reducer(undefined, authenticated(user));
    expect(state).toEqual({ user, status: "authenticated" });
    expect(state).not.toHaveProperty("accessToken");
  });

  it("clears the user when the session becomes unauthenticated", () => {
    const loggedIn = reducer(undefined, authenticated(user));
    expect(reducer(loggedIn, unauthenticated())).toEqual({ user: null, status: "unauthenticated" });
  });

  it("can mark an existing session for revalidation", () => {
    const loggedIn = reducer(undefined, authenticated(user));
    expect(reducer(loggedIn, checkingSession()).status).toBe("checking");
  });
});
