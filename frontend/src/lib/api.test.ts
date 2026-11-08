import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, api } from "./api";

function mockFetch(impl: () => Promise<Partial<Response>>) {
  vi.stubGlobal("fetch", vi.fn(impl));
}

afterEach(() => vi.unstubAllGlobals());

describe("api client", () => {
  it("returns parsed JSON on success", async () => {
    mockFetch(async () => ({ ok: true, json: async () => [{ productId: "P1" }] }));
    await expect(api.menu()).resolves.toEqual([{ productId: "P1" }]);
  });

  it("turns a network failure into a friendly ApiError", async () => {
    mockFetch(async () => {
      throw new TypeError("Failed to fetch");
    });
    const err = await api.menu().catch((e) => e);
    expect(err).toBeInstanceOf(ApiError);
    expect(err.network).toBe(true);
    expect(err.message).toMatch(/Cannot reach the API Gateway/);
  });

  it("surfaces the server's message on an HTTP error", async () => {
    mockFetch(async () => ({
      ok: false,
      status: 400,
      statusText: "Bad Request",
      json: async () => ({ message: "quantity must be at least 1" }),
    }));
    const err = await api.startSaga({} as never).catch((e) => e);
    expect(err.status).toBe(400);
    expect(err.message).toBe("400 quantity must be at least 1");
  });

  it("falls back to the status text when the error body is not JSON", async () => {
    mockFetch(async () => ({
      ok: false,
      status: 502,
      statusText: "Bad Gateway",
      json: async () => {
        throw new Error("not json");
      },
    }));
    const err = await api.health().catch((e) => e);
    expect(err.message).toBe("502 Bad Gateway");
  });

  it("encodes the saga id in the URL", async () => {
    const spy = vi.fn<(url: string) => Promise<Partial<Response>>>(
      async () => ({ ok: true, json: async () => ({}) }) as Partial<Response>,
    );
    vi.stubGlobal("fetch", spy);
    await api.getSaga("A/B");
    expect(String(spy.mock.calls[0][0])).toContain("/api/sagas/A%2FB");
  });
});
