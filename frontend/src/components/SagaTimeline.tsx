import { COMPENSATION_STEPS, FORWARD_STEPS, SERVICE_LABEL, STEP_LABEL } from "@/lib/saga";
import { formatDuration } from "@/lib/format";
import { styleFor } from "@/lib/status";
import type { SagaDetail, SagaStep } from "@/lib/types";
import { Card } from "./Card";
import { OutcomeBanner } from "./OutcomeBanner";
import { StatusBadge } from "./StatusBadge";

function StepBox({
  index,
  name,
  service,
  row,
  note,
}: {
  index: number;
  name: string;
  service: string;
  row?: SagaStep;
  note?: string;
}) {
  const status = row?.status ?? "PENDING";
  const s = styleFor(status);
  return (
    <li
      data-testid={`step-${name}`}
      data-status={status}
      className={`flex-1 rounded-xl border-2 p-4 transition-colors duration-500 ${s.bg} ${s.border}`}
    >
      <div className="mb-2 flex items-center justify-between gap-2">
        <span className="text-xs font-bold text-slate-500">#{index}</span>
        <StatusBadge status={status} />
      </div>
      <p className={`text-base font-extrabold ${s.text}`}>{STEP_LABEL[name]}</p>
      <p className="text-xs text-slate-400">{SERVICE_LABEL[service]}</p>
      {note && <p className="mt-1 text-xs text-slate-500">{note}</p>}
      {row && row.durationMs > 0 && (
        <p className="mt-2 text-xs text-slate-400">
          {formatDuration(row.durationMs)}
          {row.attempts > 1 ? ` · ${row.attempts} attempts` : ""}
        </p>
      )}
      {row?.errorMessage && (
        <p className="mt-2 line-clamp-3 text-xs text-red-300" title={row.errorMessage}>
          {row.errorMessage}
        </p>
      )}
    </li>
  );
}

/** A step row: stacked on phones, left-to-right from `md` up. */
function StepRow({
  children,
  dimmed = false,
  label,
}: {
  children: React.ReactNode[];
  dimmed?: boolean;
  label: string;
}) {
  return (
    <ol aria-label={label} className={`flex flex-col items-stretch gap-2 md:flex-row ${dimmed ? "opacity-40" : ""}`}>
      {children.flatMap((child, i) =>
        i === children.length - 1
          ? [child]
          : [
              child,
              <li
                key={`arrow-${i}`}
                aria-hidden
                className="flex items-center justify-center text-2xl text-slate-600 md:px-0"
              >
                <span className="rotate-90 md:rotate-0">→</span>
              </li>,
            ],
      )}
    </ol>
  );
}

export function SagaTimeline({ saga }: { saga: SagaDetail | null }) {
  const find = (name: string, type: "FORWARD" | "COMPENSATION") =>
    saga?.steps.find((s) => s.step === name && s.type === type);
  const compensating = saga?.steps.some((s) => s.type === "COMPENSATION") ?? false;

  return (
    <Card
      title="Saga Timeline"
      subtitle="Each box is one local transaction in a different microservice"
      right={saga && <StatusBadge status={saga.status} large />}
    >
      <OutcomeBanner saga={saga} />

      <p className="mt-3 mb-2 text-xs font-bold tracking-wide text-slate-500 uppercase">Forward transactions</p>
      <StepRow label="Forward transactions">
        {FORWARD_STEPS.map((f, i) => (
          <StepBox key={f.step} index={i + 1} name={f.step} service={f.service} row={find(f.step, "FORWARD")} />
        ))}
      </StepRow>

      <p
        className={`mt-5 mb-2 text-xs font-bold tracking-wide uppercase ${compensating ? "text-amber-400" : "text-slate-500"}`}
      >
        Compensating transactions (reverse order)
      </p>
      <StepRow label="Compensating transactions" dimmed={!compensating}>
        {COMPENSATION_STEPS.map((c, i) => (
          <StepBox
            key={c.step}
            index={i + 1}
            name={c.step}
            service={c.service}
            row={find(c.step, "COMPENSATION")}
            note={`undoes ${STEP_LABEL[c.undoes]}`}
          />
        ))}
      </StepRow>

      {saga?.failureReason && (
        <p className="mt-4 rounded-lg border border-red-800 bg-red-950/40 px-3 py-2 text-sm text-red-200">
          <b>Why it failed:</b> {saga.failureReason}
        </p>
      )}
      {saga?.manualRecoveryRequired && saga.recoveryHint && (
        <p className="mt-3 rounded-lg border border-fuchsia-600 bg-fuchsia-950/40 px-3 py-2 text-sm text-fuchsia-200">
          <b>⚠ Manual recovery required:</b> {saga.recoveryHint}
        </p>
      )}
    </Card>
  );
}
