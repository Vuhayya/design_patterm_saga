import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { SCENARIOS } from "@/lib/saga";
import { FailureToggles } from "./FailureToggles";
import { ScenarioPicker } from "./ScenarioPicker";
import { NO_FAILURES } from "@/lib/saga";

describe("ScenarioPicker", () => {
  it("renders every preset and marks the active one", () => {
    render(<ScenarioPicker activeId={3} disabled={false} onPick={() => {}} />);
    expect(screen.getAllByRole("button")).toHaveLength(SCENARIOS.length);
    expect(screen.getByRole("button", { name: /3 · Payment fails/ })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByRole("button", { name: /1 · Happy path/ })).toHaveAttribute("aria-pressed", "false");
  });

  it("reports the picked scenario", async () => {
    const onPick = vi.fn();
    render(<ScenarioPicker activeId={undefined} disabled={false} onPick={onPick} />);
    await userEvent.click(screen.getByRole("button", { name: /5 · Compensation fails/ }));
    expect(onPick).toHaveBeenCalledWith(SCENARIOS[4]);
  });

  it("cannot be used while a saga runs", () => {
    render(<ScenarioPicker activeId={1} disabled onPick={() => {}} />);
    for (const b of screen.getAllByRole("button")) expect(b).toBeDisabled();
  });
});

describe("FailureToggles", () => {
  it("toggles a single flag without touching the others", async () => {
    const onChange = vi.fn();
    render(<FailureToggles value={NO_FAILURES} disabled={false} onChange={onChange} />);
    await userEvent.click(screen.getByLabelText("Fail Process Payment"));
    expect(onChange).toHaveBeenCalledWith({ ...NO_FAILURES, failProcessPayment: true });
  });

  it("offers all seven steps", () => {
    render(<FailureToggles value={NO_FAILURES} disabled={false} onChange={() => {}} />);
    expect(screen.getAllByRole("checkbox")).toHaveLength(7);
  });
});
