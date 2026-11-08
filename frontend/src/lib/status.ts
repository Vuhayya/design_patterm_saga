import type { SagaStatus, StepStatus } from "./types";

export interface StatusStyle {
  text: string;
  bg: string;
  border: string;
  dot: string;
}

const STYLES: Record<string, StatusStyle> = {
  PENDING: { text: "text-slate-400", bg: "bg-slate-800/60", border: "border-slate-700", dot: "bg-slate-500" },
  RUNNING: {
    text: "text-sky-300",
    bg: "bg-sky-950/60",
    border: "border-sky-500",
    dot: "bg-sky-400 motion-safe:animate-pulse",
  },
  COMPLETED: { text: "text-emerald-300", bg: "bg-emerald-950/50", border: "border-emerald-600", dot: "bg-emerald-400" },
  FAILED: { text: "text-red-300", bg: "bg-red-950/60", border: "border-red-500", dot: "bg-red-400" },
  COMPENSATING: {
    text: "text-amber-300",
    bg: "bg-amber-950/50",
    border: "border-amber-500",
    dot: "bg-amber-400 motion-safe:animate-pulse",
  },
  COMPENSATED: { text: "text-amber-300", bg: "bg-amber-950/50", border: "border-amber-600", dot: "bg-amber-400" },
  COMPENSATION_FAILED: {
    text: "text-fuchsia-300",
    bg: "bg-fuchsia-950/50",
    border: "border-fuchsia-500",
    dot: "bg-fuchsia-400",
  },
  SKIPPED: {
    text: "text-slate-500",
    bg: "bg-slate-900/60",
    border: "border-slate-800 border-dashed",
    dot: "bg-slate-600",
  },
};

export function styleFor(status: SagaStatus | StepStatus | string): StatusStyle {
  return STYLES[status] ?? STYLES.PENDING;
}

/** "COMPENSATION_FAILED" -> "COMPENSATION FAILED" (the old replace() only fixed the first underscore). */
export function humanizeStatus(status: string): string {
  return status.replaceAll("_", " ");
}
