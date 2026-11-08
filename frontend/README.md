# Saga Demo - Frontend

Next.js (App Router) + TypeScript + Tailwind CSS. It is the "control room" for the Saga demo: start a
distributed transaction, break it on purpose, and watch the orchestrator compensate in real time.

The browser only ever talks to the **API Gateway** (`http://localhost:8080`), never to a microservice.

## Run

```powershell
# 1. backend (from the repo root) - wait about a minute for the 5 services
scripts\start-backend.ps1
# 2. frontend
scripts\start-frontend.ps1        # or: npm install && npm run dev
```

Open http://localhost:3000. To point at another gateway, copy `.env.example` to `.env.local`.

## Pages

| Route           | What it shows                                                                                             |
| --------------- | --------------------------------------------------------------------------------------------------------- |
| `/`             | Control room: scenarios, saga timeline, state comparison, HTTP calls between services, live log, history |
| `/how-it-works` | Saga explained, with the step/compensation table loaded live from the orchestrator                        |

## Structure

```
src/
  app/            routes, layout, error + not-found pages
  components/     presentational components (one concern each; Card, StatusBadge are shared)
  hooks/          usePolling (generic), useSagaRun (follows one saga until it finishes)
  lib/
    api.ts        typed client, ApiError with audience-friendly messages, request timeout
    types.ts      mirrors the orchestrator DTOs
    saga.ts       domain constants (steps, scenarios) and pure logic (matchScenario, outcomeOf)
    status.ts     status -> colours; format.ts  money / time formatting
    config.ts     API base URL and polling intervals
e2e/              Playwright tests against the real stack
```

Design choices worth knowing:

- **The verdict comes from data, not from a status string.** `outcomeOf()` calls a compensated saga
  "restored" only if the stock figure read back from the inventory service really matches the "before"
  snapshot; otherwise it shows "inconsistent".
- **Polling, not websockets.** The orchestrator runs sagas in the background; the page re-reads the open saga
  every 500 ms until `finished`, which is what makes the timeline fill in box by box.
- **Accessible by default:** semantic lists/tables with captions, `aria-pressed` on toggles, a live region that
  announces the outcome, visible focus rings, a skip link, and `prefers-reduced-motion` respected.

## Scripts

| Command                           | Purpose                                                        |
| --------------------------------- | -------------------------------------------------------------- |
| `npm run dev` / `build` / `start` | Next.js                                                        |
| `npm run check`                   | lint + typecheck + unit tests + production build (run before a demo) |
| `npm test`                        | Vitest unit/component tests (no backend needed)                |
| `npm run e2e`                     | Playwright against the live stack (backend must be running)    |
| `npm run format`                  | Prettier                                                       |

E2E uses the locally installed Edge (`E2E_CHANNEL=chrome` for Chrome). Set `E2E_SHOTS=<folder>` to save a
screenshot after each scenario. The e2e suite **resets the demo data** first, so do not run it right before
presenting.
