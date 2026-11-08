import { FAILURE_TOGGLES } from "@/lib/saga";
import type { FailureSimulation } from "@/lib/types";

const GROUPS = [
  { id: "forward", label: "Forward steps" },
  { id: "compensation", label: "Compensating steps" },
] as const;

export function FailureToggles({
  value,
  disabled,
  onChange,
}: {
  value: FailureSimulation;
  disabled: boolean;
  onChange: (next: FailureSimulation) => void;
}) {
  return (
    <fieldset disabled={disabled}>
      <legend className="mb-2 text-sm font-bold text-slate-200">Failure simulation (custom)</legend>
      {GROUPS.map((group) => (
        <div key={group.id} className="mb-2">
          <p className="mb-1 text-xs tracking-wide text-slate-500 uppercase">{group.label}</p>
          <div className="flex flex-wrap gap-2">
            {FAILURE_TOGGLES.filter((t) => t.group === group.id).map((t) => (
              <label
                key={t.key}
                className={`cursor-pointer rounded-md border px-2.5 py-1 text-xs font-semibold select-none focus-within:outline-2 focus-within:outline-offset-2 focus-within:outline-sky-400 ${
                  value[t.key]
                    ? "border-red-500 bg-red-950/60 text-red-200"
                    : "border-slate-700 bg-slate-800/40 text-slate-400"
                }`}
              >
                <input
                  type="checkbox"
                  className="sr-only"
                  checked={value[t.key]}
                  onChange={(e) => onChange({ ...value, [t.key]: e.target.checked })}
                />
                <span aria-hidden>{value[t.key] ? "✕ " : ""}</span>
                Fail {t.label}
              </label>
            ))}
          </div>
        </div>
      ))}
    </fieldset>
  );
}
