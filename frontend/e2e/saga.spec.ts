import { expect, test, type Page } from "@playwright/test";

const SHOTS = process.env.E2E_SHOTS; // optional folder for review screenshots

async function shot(page: Page, name: string) {
  if (SHOTS) await page.screenshot({ path: `${SHOTS}/${name}.png`, fullPage: true });
}

async function resetData(page: Page) {
  await page.goto("/");
  await page.getByRole("button", { name: "Reset data" }).click();
  await expect(page.getByText("No sagas yet.")).toBeVisible();
}

async function runScenario(page: Page, scenario: RegExp) {
  await page.getByRole("button", { name: scenario }).click();
  await page.getByRole("button", { name: /START SAGA/ }).click();
}

const step = (page: Page, name: string) => page.getByTestId(`step-${name}`);
const outcome = (page: Page) => page.getByTestId("outcome");

test.describe.configure({ mode: "serial" });

test.beforeAll(async ({ browser }) => {
  const page = await browser.newPage();
  await resetData(page);
  await page.close();
});

test("control room shows all services online", async ({ page }) => {
  await page.goto("/");
  const health = page.getByRole("list", { name: "Service health" });
  await expect(health.getByRole("listitem")).toHaveCount(4);
  await expect(health.getByText(/online/).first()).toBeVisible();
  await shot(page, "01-idle");
});

test("scenario 1: happy path completes all four steps", async ({ page }) => {
  await page.goto("/");
  await runScenario(page, /1 · Happy path/);

  await expect(outcome(page)).toHaveAttribute("data-outcome", "SUCCESS", { timeout: 20_000 });
  for (const s of ["CREATE_ORDER", "RESERVE_INVENTORY", "PROCESS_PAYMENT", "CONFIRM_ORDER"]) {
    await expect(step(page, s)).toHaveAttribute("data-status", "COMPLETED");
  }
  await expect(step(page, "CANCEL_ORDER")).toHaveAttribute("data-status", "PENDING");
  await expect(page.getByText(/^4 calls$/)).toBeVisible();
  await shot(page, "02-happy-path");
});

test("scenario 3: payment fails, stock is released and the order cancelled", async ({ page }) => {
  await page.goto("/");
  await runScenario(page, /3 · Payment fails/);

  await expect(outcome(page)).toHaveAttribute("data-outcome", "RESTORED", { timeout: 25_000 });
  await expect(step(page, "PROCESS_PAYMENT")).toHaveAttribute("data-status", "FAILED");
  await expect(step(page, "CONFIRM_ORDER")).toHaveAttribute("data-status", "SKIPPED");
  await expect(step(page, "RELEASE_INVENTORY")).toHaveAttribute("data-status", "COMPENSATED");
  await expect(step(page, "CANCEL_ORDER")).toHaveAttribute("data-status", "COMPENSATED");
  await expect(step(page, "REFUND_PAYMENT")).toHaveAttribute("data-status", "PENDING"); // payment never committed
  await shot(page, "03-payment-fails");
});

test("scenario 5: failed compensation is reported as inconsistent", async ({ page }) => {
  await page.goto("/");
  await runScenario(page, /5 · Compensation fails/);

  await expect(outcome(page)).toHaveAttribute("data-outcome", "INCONSISTENT", { timeout: 30_000 });
  await expect(step(page, "RELEASE_INVENTORY")).toHaveAttribute("data-status", "COMPENSATION_FAILED");
  await expect(page.getByText(/Manual recovery required/)).toBeVisible();
  await shot(page, "04-compensation-fails");
});

test("history lists the sagas and replays an old one", async ({ page }) => {
  await page.goto("/");
  const history = page.getByRole("table", { name: /Most recent sagas/ });
  await expect(history.getByRole("row")).toHaveCount(4); // header + 3 sagas

  const rows = history.getByRole("row");
  await rows.nth(3).getByRole("button").click(); // oldest = the happy path
  await expect(outcome(page)).toHaveAttribute("data-outcome", "SUCCESS");
  await shot(page, "05-history-replay");
});

test("form is locked while a saga is running", async ({ page }) => {
  await page.goto("/");
  await runScenario(page, /1 · Happy path/);
  await expect(page.getByRole("button", { name: /Saga running/ })).toBeDisabled();
  await expect(page.getByRole("button", { name: "Reset data" })).toBeDisabled();
  await expect(outcome(page)).toHaveAttribute("data-outcome", "SUCCESS", { timeout: 20_000 });
  await expect(page.getByRole("button", { name: /START SAGA/ })).toBeEnabled();
});

test("how-it-works page renders the definition from the orchestrator", async ({ page }) => {
  await page.goto("/how-it-works");
  await expect(page.getByRole("heading", { name: "What is the Saga pattern?" })).toBeVisible();
  await expect(page.getByRole("cell", { name: "Refund Payment" })).toBeVisible();
  await expect(page.getByText("ORCHESTRATION", { exact: true })).toBeVisible();
  await shot(page, "06-how-it-works");
});

test("unknown route shows the not-found page", async ({ page }) => {
  await page.goto("/nope");
  await expect(page.getByRole("heading", { name: "Page not found" })).toBeVisible();
});
