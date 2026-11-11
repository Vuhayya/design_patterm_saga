"use client";

import { api } from "@/lib/api";
import { HEALTH_POLL_MS } from "@/lib/config";
import { usePolling } from "@/hooks/usePolling";

export function ServiceHealthBar() {
  const { data, error } = usePolling(api.health, HEALTH_POLL_MS);

  if (error && !data) {
    return (
      <div role="alert" className="rounded-lg border border-red-700 bg-red-950/60 px-4 py-2 text-sm text-red-200">
        {error} Hãy khởi động bằng <code className="font-mono">scripts/start-backend.ps1</code>.
      </div>
    );
  }

  return (
    <ul aria-label="Tình trạng service" className="flex flex-wrap gap-3">
      {(data ?? []).map((svc) => {
        const online = svc.status === "ONLINE";
        return (
          <li
            key={svc.service}
            title={svc.error ?? svc.database}
            className={`flex items-center gap-2 rounded-lg border px-3 py-1.5 text-sm ${
              online
                ? "border-emerald-700 bg-emerald-950/40 text-emerald-200"
                : "border-red-600 bg-red-950/50 text-red-200"
            }`}
          >
            <span aria-hidden className={`h-2.5 w-2.5 rounded-full ${online ? "bg-emerald-400" : "bg-red-500"}`} />
            <span className="font-semibold">{svc.displayName}</span>
            <span className="text-xs opacity-80">{online ? `online · ${svc.responseTimeMs} ms` : "OFFLINE"}</span>
          </li>
        );
      })}
    </ul>
  );
}
