import { describe, expect, it } from "vitest";
import { NO_FAILURES, SCENARIOS, matchScenario, outcomeOf, stockRestored } from "./saga";
import type { SagaDetail, SystemState } from "./types";

const state = (reserved: number): SystemState => ({
  label: "x",
  productId: "P1",
  productName: "Pizza",
  inventoryTotalUnits: 100,
  inventoryReservedUnits: reserved,
  inventoryAvailableUnits: 100 - reserved,
  paymentStatus: "NOT_PROCESSED",
  paymentReference: null,
  orderStatus: "NOT_CREATED",
  capturedAt: "2026-01-01T00:00:00Z",
});

const saga = (over: Partial<SagaDetail>): SagaDetail =>
  ({
    status: "COMPLETED",
    finished: true,
    manualRecoveryRequired: false,
    stateBefore: state(0),
    stateAfter: state(0),
    steps: [],
    ...over,
  }) as SagaDetail;

describe("matchScenario", () => {
  it("recognises every preset from its own flags", () => {
    for (const s of SCENARIOS) {
      expect(matchScenario({ ...NO_FAILURES, ...s.flags })?.id).toBe(s.id);
    }
  });

  it("returns undefined for a custom combination", () => {
    expect(matchScenario({ ...NO_FAILURES, failCreateOrder: true, failCancelOrder: true })).toBeUndefined();
  });
});

describe("outcomeOf", () => {
  it("is null before any saga exists", () => {
    expect(outcomeOf(null)).toBeNull();
  });

  it("is RUNNING while the saga is unfinished", () => {
    expect(outcomeOf(saga({ finished: false, status: "RUNNING" }))).toBe("RUNNING");
  });

  it("is SUCCESS for a completed saga", () => {
    expect(outcomeOf(saga({ status: "COMPLETED" }))).toBe("SUCCESS");
  });

  it("is RESTORED when a compensated saga really got the stock back", () => {
    expect(outcomeOf(saga({ status: "COMPENSATED", stateBefore: state(3), stateAfter: state(3) }))).toBe("RESTORED");
  });

  it("is INCONSISTENT when COMPENSATED but the stock figure differs (trust the data, not the status)", () => {
    expect(outcomeOf(saga({ status: "COMPENSATED", stateBefore: state(3), stateAfter: state(4) }))).toBe(
      "INCONSISTENT",
    );
  });

  it("is INCONSISTENT for COMPENSATION_FAILED", () => {
    expect(outcomeOf(saga({ status: "COMPENSATION_FAILED", manualRecoveryRequired: true }))).toBe("INCONSISTENT");
  });
});

describe("stockRestored", () => {
  it("needs both snapshots", () => {
    expect(stockRestored(null, state(1))).toBe(false);
    expect(stockRestored(state(1), null)).toBe(false);
    expect(stockRestored(state(1), state(1))).toBe(true);
  });
});
