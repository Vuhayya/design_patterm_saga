/** Runtime configuration. Everything the browser talks to goes through the API Gateway. */
export const API_BASE = (process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080").replace(/\/$/, "");

/** How often the open saga is re-read while it is still running (ms). */
export const SAGA_POLL_MS = 500;

/** Background refresh rates for the passive panels (ms). */
export const HEALTH_POLL_MS = 4_000;
export const HISTORY_POLL_MS = 3_000;
export const DEFINITION_POLL_MS = 15_000;

export const REQUEST_TIMEOUT_MS = 10_000;
