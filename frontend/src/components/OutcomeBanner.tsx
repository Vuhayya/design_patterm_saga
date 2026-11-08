import { outcomeOf, type Outcome } from "@/lib/saga";
import type { SagaDetail } from "@/lib/types";

const COPY: Record<Outcome, { text: string; style: string }> = {
  RUNNING: { text: "Saga in progress...", style: "border-sky-700 bg-sky-950/40 text-sky-200" },
  SUCCESS: {
    text: "All four local transactions committed - the order is confirmed.",
    style: "border-emerald-700 bg-emerald-950/40 text-emerald-200",
  },
  RESTORED: {
    text: "A step failed, every committed step was undone - the system is consistent again.",
    style: "border-amber-600 bg-amber-950/40 text-amber-200",
  },
  INCONSISTENT: {
    text: "Compensation failed - the system is left inconsistent and needs manual recovery.",
    style: "border-fuchsia-600 bg-fuchsia-950/40 text-fuchsia-200",
  },
};

/**
 * One-sentence verdict, announced to screen readers (aria-live) and readable from the back of a room.
 * It is derived from the data read back from the services, not from the saga status alone.
 */
export function OutcomeBanner({ saga }: { saga: SagaDetail | null }) {
  const outcome = outcomeOf(saga);
  return (
    <div role="status" aria-live="polite" className="min-h-[2.75rem]">
      {outcome && (
        <p
          data-testid="outcome"
          data-outcome={outcome}
          className={`rounded-lg border px-3 py-2 text-sm font-semibold ${COPY[outcome].style}`}
        >
          {COPY[outcome].text}
        </p>
      )}
    </div>
  );
}
