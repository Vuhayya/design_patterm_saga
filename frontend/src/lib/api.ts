import { API_BASE, REQUEST_TIMEOUT_MS } from "./config";
import type {
  MenuItem,
  SagaDefinition,
  SagaDetail,
  SagaSummary,
  ServiceHealth,
  StartSagaRequest,
  StartSagaResponse,
} from "./types";

/** A failed API call, with a message that is safe to show to the audience. */
export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number | null,
    readonly network = false,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers: { "Content-Type": "application/json", ...(init?.headers ?? {}) },
      cache: "no-store",
      signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
    });
  } catch {
    throw new ApiError(`Cannot reach the API Gateway at ${API_BASE}. Is the backend running?`, null, true);
  }

  if (!res.ok) {
    let detail = res.statusText;
    try {
      const body = await res.json();
      detail = body.message ?? body.error ?? detail;
    } catch {
      /* the error body was not JSON */
    }
    throw new ApiError(`${res.status} ${detail}`.trim(), res.status);
  }
  return (await res.json()) as T;
}

export const api = {
  menu: () => request<MenuItem[]>("/api/sagas/menu"),
  health: () => request<ServiceHealth[]>("/api/sagas/system/health"),
  definition: () => request<SagaDefinition>("/api/sagas/definition"),
  startSaga: (body: StartSagaRequest) =>
    request<StartSagaResponse>("/api/sagas/orders", { method: "POST", body: JSON.stringify(body) }),
  getSaga: (id: string) => request<SagaDetail>(`/api/sagas/${encodeURIComponent(id)}`),
  listSagas: (limit = 15) => request<SagaSummary[]>(`/api/sagas?limit=${limit}`),
  reset: () => request<Record<string, unknown>>("/api/sagas/reset", { method: "DELETE" }),
};
