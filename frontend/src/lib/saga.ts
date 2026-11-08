/**
 * Domain knowledge about the demo saga, kept in ONE place so components stay presentational.
 * The authoritative definition lives in the orchestrator (GET /api/sagas/definition) and is shown
 * on the "How Saga Works" page; the constants below only drive the fixed layout of the timeline.
 */
import type { FailureSimulation, SagaDetail, SystemState } from "./types";

export const STEP_LABEL: Record<string, string> = {
  CREATE_ORDER: "Create Order",
  RESERVE_INVENTORY: "Reserve Inventory",
  PROCESS_PAYMENT: "Process Payment",
  CONFIRM_ORDER: "Confirm Order",
  CANCEL_ORDER: "Cancel Order",
  RELEASE_INVENTORY: "Release Inventory",
  REFUND_PAYMENT: "Refund Payment",
};

export const SERVICE_LABEL: Record<string, string> = {
  "order-service": "Order Service",
  "inventory-service": "Inventory Service",
  "payment-service": "Payment Service",
  "saga-orchestrator": "Saga Orchestrator",
};

export const FORWARD_STEPS = [
  { step: "CREATE_ORDER", service: "order-service" },
  { step: "RESERVE_INVENTORY", service: "inventory-service" },
  { step: "PROCESS_PAYMENT", service: "payment-service" },
  { step: "CONFIRM_ORDER", service: "order-service" },
] as const;

/** In the order they run: the last committed step is undone first. */
export const COMPENSATION_STEPS = [
  { step: "REFUND_PAYMENT", service: "payment-service", undoes: "PROCESS_PAYMENT" },
  { step: "RELEASE_INVENTORY", service: "inventory-service", undoes: "RESERVE_INVENTORY" },
  { step: "CANCEL_ORDER", service: "order-service", undoes: "CREATE_ORDER" },
] as const;

export const NO_FAILURES: FailureSimulation = {
  failCreateOrder: false,
  failReserveInventory: false,
  failProcessPayment: false,
  failConfirmOrder: false,
  failReleaseInventory: false,
  failRefundPayment: false,
  failCancelOrder: false,
};

export interface Scenario {
  id: number;
  title: string;
  hint: string;
  flags: Partial<FailureSimulation>;
}

export const SCENARIOS: readonly Scenario[] = [
  { id: 1, title: "Happy path", hint: "All 4 steps commit", flags: {} },
  { id: 2, title: "Inventory fails", hint: "Cancel the order", flags: { failReserveInventory: true } },
  { id: 3, title: "Payment fails", hint: "Release stock, cancel order", flags: { failProcessPayment: true } },
  { id: 4, title: "Confirm fails", hint: "Refund, release, cancel", flags: { failConfirmOrder: true } },
  {
    id: 5,
    title: "Compensation fails",
    hint: "Payment fails, then the release fails",
    flags: { failProcessPayment: true, failReleaseInventory: true },
  },
];

export const FAILURE_TOGGLES: readonly {
  key: keyof FailureSimulation;
  label: string;
  group: "forward" | "compensation";
}[] = [
  { key: "failCreateOrder", label: "Create Order", group: "forward" },
  { key: "failReserveInventory", label: "Reserve Inventory", group: "forward" },
  { key: "failProcessPayment", label: "Process Payment", group: "forward" },
  { key: "failConfirmOrder", label: "Confirm Order", group: "forward" },
  { key: "failRefundPayment", label: "Refund Payment", group: "compensation" },
  { key: "failReleaseInventory", label: "Release Inventory", group: "compensation" },
  { key: "failCancelOrder", label: "Cancel Order", group: "compensation" },
];

/** Which preset (if any) exactly matches the current set of failure flags. */
export function matchScenario(flags: FailureSimulation): Scenario | undefined {
  const keys = Object.keys(NO_FAILURES) as (keyof FailureSimulation)[];
  return SCENARIOS.find((s) => keys.every((k) => (s.flags[k] ?? false) === flags[k]));
}

export type Outcome = "RUNNING" | "SUCCESS" | "RESTORED" | "INCONSISTENT";

/**
 * Plain-language verdict of a saga, derived from the data read back from the real services:
 * a rolled-back saga only counts as "restored" if the stock figure really went back.
 */
export function outcomeOf(saga: SagaDetail | null): Outcome | null {
  if (!saga) return null;
  if (!saga.finished) return "RUNNING";
  if (saga.status === "COMPLETED") return "SUCCESS";
  if (saga.status === "COMPENSATION_FAILED" || saga.manualRecoveryRequired) return "INCONSISTENT";
  return stockRestored(saga.stateBefore, saga.stateAfter) ? "RESTORED" : "INCONSISTENT";
}

export function stockRestored(before: SystemState | null, after: SystemState | null): boolean {
  return before != null && after != null && before.inventoryReservedUnits === after.inventoryReservedUnits;
}
