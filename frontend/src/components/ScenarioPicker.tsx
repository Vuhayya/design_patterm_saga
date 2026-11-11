import { SCENARIOS, type Scenario } from "@/lib/saga";

export function ScenarioPicker({
  activeId,
  disabled,
  onPick,
}: {
  activeId: number | undefined;
  disabled: boolean;
  onPick: (scenario: Scenario) => void;
}) {
  return (
    <fieldset disabled={disabled} className="grid grid-cols-1 gap-2">
      <legend className="mb-2 text-sm font-bold text-slate-200">Kịch bản</legend>
      {SCENARIOS.map((s) => {
        const active = activeId === s.id;
        return (
          <button
            key={s.id}
            type="button"
            aria-pressed={active}
            onClick={() => onPick(s)}
            className={`rounded-lg border px-3 py-2 text-left transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-400 disabled:opacity-50 ${
              active ? "border-sky-500 bg-sky-950/60" : "border-slate-700 bg-slate-800/40 hover:border-slate-500"
            }`}
          >
            <span className="block text-sm font-bold text-slate-100">
              {s.id} · {s.title}
            </span>
            <span className="block text-xs text-slate-400">{s.hint}</span>
          </button>
        );
      })}
    </fieldset>
  );
}
