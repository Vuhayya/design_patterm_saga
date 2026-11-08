import { outcomeOf } from "@/lib/saga";
import type { SagaDetail, SystemState } from "@/lib/types";
import { Card } from "./Card";

type Cell = string | number | null;
const ROWS: { label: string; get: (s: SystemState) => Cell }[] = [
  { label: "Order status", get: (s) => s.orderStatus },
  { label: "Inventory reserved", get: (s) => s.inventoryReservedUnits },
  { label: "Inventory available", get: (s) => s.inventoryAvailableUnits },
  { label: "Payment status", get: (s) => s.paymentStatus },
];

function cellColor(label: string, value: Cell): string {
  const v = String(value);
  if (v === "CONFIRMED" || v === "COMPLETED") return "text-emerald-300";
  if (v === "CANCELLED" || v === "REFUNDED") return "text-amber-300";
  if (v === "FAILED") return "text-red-300";
  if (v === "NOT_CREATED" || v === "NOT_PROCESSED") return "text-slate-400";
  return label.startsWith("Inventory") ? "text-sky-300" : "text-slate-200";
}

export function StateComparison({ saga }: { saga: SagaDetail | null }) {
  const before = saga?.stateBefore ?? null;
  // While the saga runs there is no "after" yet - show the live state instead.
  const after = saga?.stateAfter ?? saga?.stateNow ?? null;
  const afterLabel = saga?.stateAfter ? "After" : saga ? "Now (live)" : "After";
  const outcome = outcomeOf(saga);

  return (
    <Card
      title="State Comparison"
      subtitle="Data read from the real services before and after the saga"
      right={
        outcome === "INCONSISTENT" ? (
          <span className="rounded-full bg-fuchsia-900/60 px-3 py-1 text-xs font-bold text-fuchsia-300">
            Inconsistent
          </span>
        ) : outcome === "RESTORED" ? (
          <span className="rounded-full bg-amber-900/60 px-3 py-1 text-xs font-bold text-amber-300">
            Stock restored
          </span>
        ) : outcome === "SUCCESS" ? (
          <span className="rounded-full bg-emerald-900/60 px-3 py-1 text-xs font-bold text-emerald-300">
            Goal reached
          </span>
        ) : null
      }
    >
      <table className="w-full text-sm">
        <caption className="sr-only">System state before the saga and after it</caption>
        <thead>
          <tr className="text-left text-xs tracking-wide text-slate-400 uppercase">
            <th scope="col" className="pb-2 font-semibold">
              Measure
            </th>
            <th scope="col" className="pb-2 font-semibold">
              Before
            </th>
            <th scope="col" className="pb-2 font-semibold">
              {afterLabel}
            </th>
          </tr>
        </thead>
        <tbody>
          {ROWS.map((r) => {
            const b = before ? r.get(before) : null;
            const a = after ? r.get(after) : null;
            const changed = before != null && after != null && String(b) !== String(a);
            return (
              <tr key={r.label} className="border-t border-slate-800">
                <th scope="row" className="py-2.5 pr-3 text-left font-normal text-slate-400">
                  {r.label}
                </th>
                <td className={`py-2.5 font-mono font-bold ${b == null ? "text-slate-500" : cellColor(r.label, b)}`}>
                  {b ?? "-"}
                </td>
                <td
                  className={`py-2.5 font-mono font-bold ${a == null ? "text-slate-500" : cellColor(r.label, a)} ${
                    changed ? "underline decoration-sky-500 underline-offset-4" : ""
                  }`}
                >
                  {a ?? "-"}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      {outcome === "INCONSISTENT" && saga?.status === "COMPENSATION_FAILED" && (
        <p className="mt-3 text-xs text-fuchsia-300">
          Compare the reserved units: the stock was NOT given back. This is the limit of the Saga pattern.
        </p>
      )}
    </Card>
  );
}
