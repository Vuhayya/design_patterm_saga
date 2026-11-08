"use client";

import { api } from "@/lib/api";
import { HISTORY_POLL_MS } from "@/lib/config";
import { formatDuration, formatTime, formatVnd } from "@/lib/format";
import { STEP_LABEL } from "@/lib/saga";
import { usePolling } from "@/hooks/usePolling";
import { Card } from "./Card";
import { StatusBadge } from "./StatusBadge";

export function SagaHistory({
  selectedId,
  onSelect,
  refreshKey,
}: {
  selectedId: string | null;
  onSelect: (id: string) => void;
  /** Changes whenever the open saga changes state, so the table never disagrees with the timeline. */
  refreshKey?: unknown;
}) {
  const { data } = usePolling(() => api.listSagas(12), HISTORY_POLL_MS, true, refreshKey);
  const sagas = data ?? [];

  return (
    <Card title="Saga History" subtitle="Select a saga to replay its timeline, calls and logs">
      {sagas.length === 0 ? (
        <p className="py-4 text-center text-sm text-slate-500">No sagas yet.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <caption className="sr-only">Most recent sagas, newest first</caption>
            <thead>
              <tr className="text-left text-xs tracking-wide text-slate-400 uppercase">
                <th scope="col" className="pb-2">
                  Saga
                </th>
                <th scope="col" className="pb-2">
                  Customer
                </th>
                <th scope="col" className="pb-2">
                  Item
                </th>
                <th scope="col" className="pb-2">
                  Total
                </th>
                <th scope="col" className="pb-2">
                  Status
                </th>
                <th scope="col" className="pb-2">
                  Failed at
                </th>
                <th scope="col" className="pb-2">
                  Started
                </th>
                <th scope="col" className="pb-2 text-right">
                  Duration
                </th>
              </tr>
            </thead>
            <tbody>
              {sagas.map((s) => (
                <tr
                  key={s.sagaId}
                  className={`border-t border-slate-800 hover:bg-slate-800/60 ${s.sagaId === selectedId ? "bg-sky-950/40" : ""}`}
                >
                  <th scope="row" className="py-2 text-left font-normal">
                    <button
                      type="button"
                      onClick={() => onSelect(s.sagaId)}
                      aria-pressed={s.sagaId === selectedId}
                      className="rounded px-1 font-mono text-xs text-sky-300 underline-offset-2 hover:underline focus-visible:outline-2 focus-visible:outline-sky-400"
                    >
                      {s.sagaId}
                    </button>
                  </th>
                  <td className="py-2 text-slate-300">{s.customerName}</td>
                  <td className="py-2 text-slate-300">
                    {s.quantity} × {s.productName}
                  </td>
                  <td className="py-2 text-slate-300">{formatVnd(s.totalAmount)}</td>
                  <td className="py-2">
                    <StatusBadge status={s.status} />
                  </td>
                  <td className="py-2 text-xs text-red-300">{s.failedStep ? STEP_LABEL[s.failedStep] : ""}</td>
                  <td className="py-2 text-xs text-slate-400">{formatTime(s.startedAt)}</td>
                  <td className="py-2 text-right text-xs text-slate-400">{formatDuration(s.durationMs)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
}
