"use client";

import { useEffect, useRef, useState } from "react";

/**
 * Calls `fn` immediately and then every `intervalMs`. Pass `enabled=false` to pause, and change
 * `refreshKey` to force an immediate re-read (e.g. when something it depends on just changed).
 */
export function usePolling<T>(fn: () => Promise<T>, intervalMs: number, enabled = true, refreshKey?: unknown) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const fnRef = useRef(fn);
  useEffect(() => {
    fnRef.current = fn;
  });

  useEffect(() => {
    if (!enabled) return;
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout>;

    const tick = async () => {
      try {
        const result = await fnRef.current();
        if (!cancelled) {
          setData(result);
          setError(null);
        }
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : String(e));
      }
      if (!cancelled) timer = setTimeout(tick, intervalMs);
    };
    tick();

    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [intervalMs, enabled, refreshKey]);

  return { data, error };
}
