const vnd = new Intl.NumberFormat("vi-VN");

export function formatVnd(value: number | null | undefined): string {
  return value == null ? "-" : `${vnd.format(value)} ₫`;
}

/** HH:mm:ss.SSS - milliseconds matter when watching a saga step by step. */
export function formatTime(iso: string | null | undefined): string {
  if (!iso) return "-";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "-";
  const hms = d.toLocaleTimeString("en-GB", { hour12: false });
  return `${hms}.${String(d.getMilliseconds()).padStart(3, "0")}`;
}

export function formatDuration(ms: number): string {
  return ms >= 1000 ? `${(ms / 1000).toFixed(1)} s` : `${ms} ms`;
}
