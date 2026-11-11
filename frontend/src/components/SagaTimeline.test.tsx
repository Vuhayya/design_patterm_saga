import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { SagaDetail, SagaStep } from "@/lib/types";
import { SagaTimeline } from "./SagaTimeline";

const step = (
  name: string,
  type: SagaStep["type"],
  status: SagaStep["status"],
  extra: Partial<SagaStep> = {},
): SagaStep => ({
  step: name,
  type,
  status,
  targetService: "x",
  compensationFor: null,
  sequence: 1,
  startedAt: null,
  completedAt: null,
  durationMs: 0,
  attempts: 1,
  errorMessage: null,
  ...extra,
});

const base = {
  sagaId: "SAGA-1",
  finished: true,
  manualRecoveryRequired: false,
  failureReason: null,
  recoveryHint: null,
  stateBefore: null,
  stateAfter: null,
} as unknown as SagaDetail;

describe("SagaTimeline", () => {
  it("shows all seven boxes as PENDING before any saga starts", () => {
    render(<SagaTimeline saga={null} />);
    for (const name of [
      "CREATE_ORDER",
      "RESERVE_INVENTORY",
      "PROCESS_PAYMENT",
      "CONFIRM_ORDER",
      "REFUND_PAYMENT",
      "RELEASE_INVENTORY",
      "CANCEL_ORDER",
    ]) {
      expect(screen.getByTestId(`step-${name}`)).toHaveAttribute("data-status", "PENDING");
    }
  });

  it("maps real step rows onto the right boxes after a payment failure", () => {
    const saga = {
      ...base,
      status: "COMPENSATED",
      failedStep: "PROCESS_PAYMENT",
      failureReason: "payment declined",
      steps: [
        step("CREATE_ORDER", "FORWARD", "COMPLETED"),
        step("RESERVE_INVENTORY", "FORWARD", "COMPLETED"),
        step("PROCESS_PAYMENT", "FORWARD", "FAILED", { errorMessage: "declined" }),
        step("CONFIRM_ORDER", "FORWARD", "SKIPPED"),
        step("RELEASE_INVENTORY", "COMPENSATION", "COMPENSATED"),
        step("CANCEL_ORDER", "COMPENSATION", "COMPENSATED"),
      ],
    } as SagaDetail;

    render(<SagaTimeline saga={saga} />);

    expect(screen.getByTestId("step-PROCESS_PAYMENT")).toHaveAttribute("data-status", "FAILED");
    expect(screen.getByTestId("step-CONFIRM_ORDER")).toHaveAttribute("data-status", "SKIPPED");
    expect(screen.getByTestId("step-RELEASE_INVENTORY")).toHaveAttribute("data-status", "COMPENSATED");
    // never executed -> stays pending instead of being invented
    expect(screen.getByTestId("step-REFUND_PAYMENT")).toHaveAttribute("data-status", "PENDING");
    expect(screen.getByText(/payment declined/)).toBeInTheDocument();
  });

  it("does not confuse a forward and a compensation row that share a name", () => {
    const saga = { ...base, status: "COMPLETED", steps: [step("CREATE_ORDER", "FORWARD", "COMPLETED")] } as SagaDetail;
    render(<SagaTimeline saga={saga} />);
    expect(screen.getByTestId("step-CANCEL_ORDER")).toHaveAttribute("data-status", "PENDING");
  });

  it("calls out manual recovery for COMPENSATION_FAILED", () => {
    const saga = {
      ...base,
      status: "COMPENSATION_FAILED",
      manualRecoveryRequired: true,
      recoveryHint: "fix inventory by hand",
      steps: [step("RELEASE_INVENTORY", "COMPENSATION", "COMPENSATION_FAILED")],
    } as SagaDetail;
    render(<SagaTimeline saga={saga} />);
    expect(screen.getByText(/Cần xử lý thủ công/)).toBeInTheDocument();
    const outcome = screen.getByTestId("outcome");
    expect(outcome).toHaveAttribute("data-outcome", "INCONSISTENT");
    expect(within(outcome).getByText(/bất nhất/)).toBeInTheDocument();
  });
});
