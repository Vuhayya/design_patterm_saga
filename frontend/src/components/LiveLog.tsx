"use client";

import { useEffect, useRef } from "react";
import { formatTime } from "@/lib/format";
import type { SagaDetail } from "@/lib/types";
import { Card } from "./Card";

const LEVEL_STYLE: Record<string, string> = {
  INFO: "text-slate-300",
  SUCCESS: "text-emerald-300",
  WARN: "text-amber-300",
  ERROR: "text-red-300",
};

export function LiveLog({ saga }: { saga: SagaDetail | null }) {
  const logs = saga?.logs ?? [];
  const containerRef = useRef<HTMLDivElement>(null);

  // Keep the newest line in view without scrolling the whole page.
  useEffect(() => {
    const el = containerRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [logs.length]);

  return (
    <Card title="Live Log" subtitle="What the orchestrator is deciding, as it happens">
      <div
        ref={containerRef}
        role="log"
        aria-label="Saga log"
        tabIndex={0}
        className="h-[420px] overflow-y-auto rounded-lg bg-black/60 p-3 font-mono text-xs leading-relaxed focus-visible:outline-2 focus-visible:outline-sky-400"
      >
        {logs.length === 0 && <p className="text-slate-500">Waiting for a saga...</p>}
        {logs.map((l) => (
          <div key={l.sequence} className={LEVEL_STYLE[l.level] ?? "text-slate-300"}>
            <span className="text-slate-500">{formatTime(l.timestamp)} </span>
            <span className={`mr-2 font-bold ${l.phase === "COMPENSATION" ? "text-amber-500" : "text-sky-500"}`}>
              [{l.phase === "COMPENSATION" ? "COMP" : "EXEC"}]
            </span>
            {l.message}
          </div>
        ))}
      </div>
    </Card>
  );
}
