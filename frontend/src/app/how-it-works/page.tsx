"use client";

import { Card } from "@/components/Card";
import { api } from "@/lib/api";
import { DEFINITION_POLL_MS } from "@/lib/config";
import { usePolling } from "@/hooks/usePolling";
import { SERVICE_LABEL, STEP_LABEL } from "@/lib/saga";

function Point({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="rounded-lg border border-slate-800 bg-slate-950/50 p-4">
      <h3 className="mb-1 font-bold text-slate-100">{title}</h3>
      <p className="text-sm leading-relaxed text-slate-400">{children}</p>
    </div>
  );
}

export default function HowSagaWorksPage() {
  // Served from the running orchestrator, so this page can never disagree with the code.
  const { data: def, error } = usePolling(api.definition, DEFINITION_POLL_MS);

  return (
    <div className="mx-auto max-w-[1100px] space-y-5 px-6 py-6">
      <Card title="What is the Saga pattern?" subtitle="A distributed transaction without a distributed lock">
        <p className="mb-4 text-slate-300">
          In a monolith one database transaction makes an order, a stock reservation and a payment all-or-nothing. With
          microservices each service owns its own database, so no single transaction can span them. A{" "}
          <b className="text-sky-300">Saga</b> replaces that one big transaction with a sequence of{" "}
          <b className="text-sky-300">local transactions</b>. If one fails, the Saga runs{" "}
          <b className="text-amber-300">compensating transactions</b> that semantically undo the ones that already
          committed.
        </p>
        <div className="grid gap-3 md:grid-cols-3">
          <Point title="1 · Local transactions">
            Every step commits immediately in the service that owns the data. Nothing is held open and nothing is locked
            across services.
          </Point>
          <Point title="2 · Compensation, not rollback">
            A committed change cannot be rolled back. It is undone by a new business action: cancel the order, release
            the stock, refund the payment.
          </Point>
          <Point title="3 · Reverse order">
            The orchestrator remembers the steps that succeeded on a stack and undoes them last-in, first-out: refund
            before releasing stock before cancelling the order.
          </Point>
        </div>
      </Card>

      <Card
        title="This demo's saga"
        subtitle="Loaded live from the orchestrator's code (GET /api/sagas/definition)"
        right={
          <span className="rounded-full bg-sky-900/60 px-3 py-1 text-xs font-bold text-sky-300">
            {def?.sagaStyle ?? "..."}
          </span>
        }
      >
        {error && !def && <p className="text-sm text-red-300">Backend not reachable: {error}</p>}
        {def && (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs tracking-wide text-slate-500 uppercase">
                  <th className="pb-2">#</th>
                  <th className="pb-2">Local transaction</th>
                  <th className="pb-2">Owner</th>
                  <th className="pb-2">Compensating transaction</th>
                  <th className="pb-2">Owner</th>
                </tr>
              </thead>
              <tbody>
                {def.forwardSteps.map((s, i) => (
                  <tr key={s.step} className="border-t border-slate-800">
                    <td className="py-3 font-mono text-slate-500">T{i + 1}</td>
                    <td className="py-3 font-bold text-sky-300">{STEP_LABEL[s.step] ?? s.step}</td>
                    <td className="py-3 text-slate-300">{SERVICE_LABEL[s.targetService] ?? s.targetService}</td>
                    <td className="py-3 font-bold text-amber-300">
                      {s.hasCompensation ? (
                        <>
                          <span className="mr-2 font-mono text-slate-500">C{i + 1}</span>
                          {STEP_LABEL[s.compensation] ?? s.compensation}
                        </>
                      ) : (
                        <span className="font-normal text-slate-500">none - last step, nothing of its own to undo</span>
                      )}
                    </td>
                    <td className="py-3 text-slate-300">
                      {s.hasCompensation ? (SERVICE_LABEL[s.compensationService] ?? s.compensationService) : ""}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="mt-4 grid gap-3 md:grid-cols-2">
              <div className="rounded-lg bg-slate-950/60 p-3 font-mono text-xs text-slate-300">
                <div className="mb-1 font-sans text-xs font-bold text-slate-500 uppercase">Happy path</div>
                T1 → T2 → T3 → T4 → COMPLETED
              </div>
              <div className="rounded-lg bg-slate-950/60 p-3 font-mono text-xs text-amber-200">
                <div className="mb-1 font-sans text-xs font-bold text-slate-500 uppercase">If T3 fails</div>
                T1 → T2 → T3 ✕ → C2 → C1 → COMPENSATED
              </div>
            </div>
          </div>
        )}
      </Card>

      <Card title="Saga states" subtitle="The life cycle of one saga">
        <div className="flex flex-wrap items-center gap-2 text-sm">
          {(def?.sagaStates ?? []).map((s, i) => (
            <span key={s} className="flex items-center gap-2">
              {i > 0 && <span className="text-slate-600">·</span>}
              <span className="rounded-md bg-slate-800 px-2 py-1 font-mono text-xs text-slate-200">{s}</span>
            </span>
          ))}
        </div>
        <p className="mt-3 text-sm text-slate-400">
          <b className="text-fuchsia-300">COMPENSATION_FAILED</b> is the honest ending: if an undo itself fails, the
          system is inconsistent and a human must repair it. Try scenario 5 in the control room.
        </p>
      </Card>

      <div className="grid gap-5 md:grid-cols-2">
        <Card title="Orchestration vs Choreography">
          <div className="space-y-3 text-sm text-slate-400">
            <p>
              <b className="text-sky-300">Orchestration (this demo):</b> one central orchestrator tells each service
              what to do and what to undo. The whole flow is in one class, which is easy to read, monitor and debug.
            </p>
            <p>
              <b className="text-slate-200">Choreography:</b> services react to each other&apos;s events with no central
              brain. It is more decoupled, but the flow is spread across services and harder to follow.
            </p>
            <p className="text-slate-500">
              Trade-off: the orchestrator is a coordination point that must itself be reliable.
            </p>
          </div>
        </Card>

        <Card title="Limits to be honest about">
          <ul className="list-disc space-y-2 pl-5 text-sm text-slate-400">
            <li>
              <b className="text-slate-200">No isolation.</b> Other users can briefly see intermediate states (stock
              reserved but the order not yet confirmed).
            </li>
            <li>
              <b className="text-slate-200">Compensations must be idempotent</b> because they may be retried.
            </li>
            <li>
              <b className="text-slate-200">Not guaranteed to roll back.</b> A failing compensation needs manual
              recovery.
            </li>
            <li>
              <b className="text-slate-200">Demo scope.</b> Failures are simulated with flags, and calls are synchronous
              HTTP. A production system would use a message broker (the orchestrator already publishes events through a
              seam for this).
            </li>
          </ul>
        </Card>
      </div>
    </div>
  );
}
