import { formatDuration, formatTime } from "@/lib/format";
import { SERVICE_LABEL, STEP_LABEL } from "@/lib/saga";
import type { SagaDetail } from "@/lib/types";
import { Card } from "./Card";

export function ServiceCommunication({ saga }: { saga: SagaDetail | null }) {
  const calls = saga?.serviceCalls ?? [];

  return (
    <Card
      title="Service Communication"
      subtitle="Every real HTTP call the orchestrator made, in order"
      right={<span className="text-sm text-slate-400">{calls.length} calls</span>}
    >
      {calls.length === 0 ? (
        <p className="py-6 text-center text-sm text-slate-500">No calls yet - start a saga.</p>
      ) : (
        <ol className="max-h-[420px] space-y-2 overflow-y-auto pr-1" tabIndex={0} aria-label="HTTP calls">
          {calls.map((c) => {
            const ok = c.outcome === "SUCCESS";
            const compensation = c.stepType === "COMPENSATION";
            return (
              <li
                key={c.sequence}
                className={`rounded-lg border px-3 py-2 text-sm ${
                  ok ? "border-slate-700 bg-slate-800/40" : "border-red-700 bg-red-950/40"
                }`}
              >
                <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
                  <span className="font-mono text-xs text-slate-500">{c.sequence}</span>
                  <span className="font-semibold text-slate-200">
                    {SERVICE_LABEL[c.sourceService] ?? c.sourceService}
                  </span>
                  <span
                    aria-label={compensation ? "compensates" : "calls"}
                    className={compensation ? "text-amber-400" : "text-sky-400"}
                  >
                    {compensation ? "⟲" : "→"}
                  </span>
                  <span className="font-semibold text-slate-200">
                    {SERVICE_LABEL[c.targetService] ?? c.targetService}
                  </span>
                  <span
                    className={`ml-auto rounded px-2 py-0.5 font-mono text-xs font-bold ${
                      ok ? "bg-emerald-900/60 text-emerald-300" : "bg-red-900/60 text-red-300"
                    }`}
                  >
                    {c.responseStatus ?? "ERR"}
                  </span>
                </div>
                <p className="mt-1 font-mono text-xs break-all text-slate-400">
                  <span className="font-bold text-slate-300">{c.method}</span> {c.endpoint}
                </p>
                <p className="mt-1 flex flex-wrap items-center gap-x-3 text-xs text-slate-500">
                  {c.step && (
                    <span className={compensation ? "text-amber-400" : "text-sky-400"}>
                      {STEP_LABEL[c.step] ?? c.step}
                    </span>
                  )}
                  <span>{formatTime(c.timestamp)}</span>
                  <span>{formatDuration(c.durationMs)}</span>
                </p>
                {!ok && c.responseSummary && (
                  <p className="mt-1 text-xs break-words text-red-300">{c.responseSummary}</p>
                )}
              </li>
            );
          })}
        </ol>
      )}
    </Card>
  );
}
