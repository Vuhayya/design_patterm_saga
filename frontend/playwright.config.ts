import { defineConfig } from "@playwright/test";

/**
 * End-to-end tests drive the REAL stack: they need the backend (scripts/start-backend.ps1) and the
 * frontend dev server. They use the locally installed Edge, so no browser download is required.
 * Set E2E_CHANNEL=chrome to use Chrome instead.
 */
export default defineConfig({
  testDir: "./e2e",
  timeout: 60_000,
  fullyParallel: false, // sagas share one set of databases
  workers: 1,
  reporter: [["list"]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:3000",
    channel: process.env.E2E_CHANNEL ?? "msedge",
    viewport: { width: 1600, height: 1000 },
    screenshot: "only-on-failure",
  },
  webServer: {
    command: "npm run dev",
    url: "http://localhost:3000",
    reuseExistingServer: true,
    timeout: 120_000,
  },
});
