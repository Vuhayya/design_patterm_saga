"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { SAGA_POLL_MS } from "@/lib/config";
import type { SagaDetail } from "@/lib/types";

export interface SagaRun {
  /** The saga currently on screen (null until one is started or picked from the history). */
  saga: SagaDetail | null;
  selectedId: string | null;
  /** True while the selected saga has not reached an end state. */
  running: boolean;
  /** Last polling error, cleared by the next successful read. */
  error: string | null;
  select: (sagaId: string) => void;
  clear: () => void;
}

/**
 * Follows one saga until it reaches an end state.
 *
 * The orchestrator executes sagas in the background, so the UI re-reads the saga every
 * {@link SAGA_POLL_MS}; that is what makes the timeline fill in box by box. Polling stops by itself
 * once `finished` is true and restarts when another saga is selected.
 */
export function useSagaRun(): SagaRun {
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [saga, setSaga] = useState<SagaDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!selectedId) return;
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout> | undefined;

    const tick = async () => {
      try {
        const detail = await api.getSaga(selectedId);
        if (cancelled) return;
        setSaga(detail);
        setError(null);
        if (detail.finished) return;
      } catch (e) {
        if (cancelled) return;
        setError(e instanceof Error ? e.message : String(e));
      }
      timer = setTimeout(tick, SAGA_POLL_MS);
    };
    void tick();

    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [selectedId]);

  const select = useCallback((sagaId: string) => {
    setSelectedId((current) => {
      if (current !== sagaId) setSaga(null); // never show the previous saga under a new id
      return sagaId;
    });
  }, []);

  const clear = useCallback(() => {
    setSelectedId(null);
    setSaga(null);
    setError(null);
  }, []);

  return { saga, selectedId, running: saga != null && !saga.finished, error, select, clear };
}
