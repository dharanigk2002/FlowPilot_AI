import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  apiRequest,
  clearCsrfToken,
  setUnauthorizedHandler,
} from "./client";

describe("API client", () => {
  beforeEach(() => {
    clearCsrfToken();
    setUnauthorizedHandler(null);
    vi.restoreAllMocks();
  });

  it("adds credentials and the server-provided CSRF header to unsafe requests", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: "csrf-value", headerName: "X-XSRF-TOKEN" }), { status: 200, headers: { "Content-Type": "application/json" } }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ ok: true }), { status: 200, headers: { "Content-Type": "application/json" } }));

    await apiRequest<{ ok: boolean }>("/api/test", { method: "POST", body: JSON.stringify({ value: 1 }) });

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ credentials: "include" });
    const request = fetchMock.mock.calls[1][1];
    expect(request).toMatchObject({ credentials: "include", method: "POST" });
    expect((request?.headers as Headers).get("X-XSRF-TOKEN")).toBe("csrf-value");
    expect((request?.headers as Headers).get("Content-Type")).toBe("application/json");
  });

  it("does not request or attach CSRF for safe GET requests", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(new Response(JSON.stringify({ id: 1 }), { status: 200, headers: { "Content-Type": "application/json" } }));
    await apiRequest<{ id: number }>("/api/test");
    expect(fetchMock).toHaveBeenCalledOnce();
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ credentials: "include", method: "GET" });
  });

  it("notifies the application when a request is unauthorized", async () => {
    const onUnauthorized = vi.fn();
    setUnauthorizedHandler(onUnauthorized);
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify({ message: "Unauthorized" }), {
        status: 401,
        headers: { "Content-Type": "application/json" },
      }),
    );

    await expect(apiRequest("/api/auth/me")).rejects.toMatchObject({
      status: 401,
    });
    expect(onUnauthorized).toHaveBeenCalledOnce();
  });
});
