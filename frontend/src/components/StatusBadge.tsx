import { humanizeStatus, styleFor } from "@/lib/status";

export function StatusBadge({ status, large = false }: { status: string; large?: boolean }) {
  const s = styleFor(status);
  return (
    <span
      className={`inline-flex items-center gap-2 rounded-full border px-3 py-1 font-semibold tracking-wide whitespace-nowrap ${s.text} ${s.bg} ${s.border} ${large ? "text-base" : "text-xs"}`}
    >
      <span aria-hidden className={`h-2 w-2 rounded-full ${s.dot}`} />
      {humanizeStatus(status)}
    </span>
  );
}
