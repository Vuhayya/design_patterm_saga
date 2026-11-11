import { outcomeOf, type Outcome } from "@/lib/saga";
import type { SagaDetail } from "@/lib/types";

const COPY: Record<Outcome, { text: string; style: string }> = {
  RUNNING: { text: "Saga đang chạy...", style: "border-sky-700 bg-sky-950/40 text-sky-200" },
  SUCCESS: {
    text: "Cả bốn local transaction đã commit - đơn hàng đã được xác nhận.",
    style: "border-emerald-700 bg-emerald-950/40 text-emerald-200",
  },
  RESTORED: {
    text: "Một bước bị lỗi, mọi bước đã commit đều được hoàn tác - hệ thống nhất quán trở lại.",
    style: "border-amber-600 bg-amber-950/40 text-amber-200",
  },
  INCONSISTENT: {
    text: "Compensation thất bại - hệ thống bị bất nhất và cần xử lý thủ công.",
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
