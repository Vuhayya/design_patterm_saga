"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { formatVnd } from "@/lib/format";
import { NO_FAILURES, matchScenario } from "@/lib/saga";
import type { FailureSimulation, MenuItem } from "@/lib/types";
import { Card } from "./Card";
import { FailureToggles } from "./FailureToggles";
import { ScenarioPicker } from "./ScenarioPicker";

interface Props {
  /** A saga is running: the form is locked so two sagas cannot be mixed up on screen. */
  busy: boolean;
  onStarted: (sagaId: string) => void;
  onReset: () => void;
}

const FIELD =
  "mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 focus-visible:outline-2 focus-visible:outline-sky-400 disabled:opacity-50";

export function ControlPanel({ busy, onStarted, onReset }: Props) {
  const [menu, setMenu] = useState<MenuItem[]>([]);
  const [productId, setProductId] = useState("");
  const [customer, setCustomer] = useState("Nguyen Van A");
  const [quantity, setQuantity] = useState(1);
  const [failures, setFailures] = useState<FailureSimulation>(NO_FAILURES);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Re-read the menu whenever a saga starts or ends so the stock numbers stay truthful.
  useEffect(() => {
    let cancelled = false;
    api
      .menu()
      .then((items) => {
        if (cancelled) return;
        setMenu(items);
        setProductId((current) => current || items[0]?.productId || "");
      })
      .catch((e: Error) => !cancelled && setError(e.message));
    return () => {
      cancelled = true;
    };
  }, [busy]);

  const product = menu.find((m) => m.productId === productId);
  const locked = busy || submitting;

  async function start() {
    if (!product) return;
    setSubmitting(true);
    setError(null);
    try {
      const res = await api.startSaga({
        customerName: customer.trim() || "Guest",
        productId: product.productId,
        productName: product.productName,
        quantity,
        unitPrice: product.unitPrice,
        failureSimulation: failures,
      });
      onStarted(res.sagaId);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setSubmitting(false);
    }
  }

  async function reset() {
    setError(null);
    try {
      await api.reset();
      onReset();
      setMenu(await api.menu());
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }

  return (
    <Card title="Control Room" subtitle="Pick a scenario, then start the distributed transaction">
      <form
        onSubmit={(e) => {
          e.preventDefault();
          void start();
        }}
      >
        <ScenarioPicker
          activeId={matchScenario(failures)?.id}
          disabled={locked}
          onPick={(s) => setFailures({ ...NO_FAILURES, ...s.flags })}
        />

        <fieldset disabled={locked} className="mt-4 space-y-3 border-t border-slate-800 pt-4">
          <legend className="sr-only">Order</legend>
          <label className="block text-sm text-slate-300">
            Customer
            <input value={customer} onChange={(e) => setCustomer(e.target.value)} className={FIELD} />
          </label>
          <div className="grid grid-cols-3 gap-3">
            <label className="col-span-2 block text-sm text-slate-300">
              Dish
              <select value={productId} onChange={(e) => setProductId(e.target.value)} className={FIELD}>
                {menu.map((m) => (
                  <option key={m.productId} value={m.productId}>
                    {m.productName} - {m.availableUnits} left
                  </option>
                ))}
              </select>
            </label>
            <label className="block text-sm text-slate-300">
              Qty
              <input
                type="number"
                min={1}
                value={quantity}
                onChange={(e) => setQuantity(Math.max(1, Number(e.target.value) || 1))}
                className={FIELD}
              />
            </label>
          </div>
          {product && (
            <p className="text-sm text-slate-400">
              Total: <span className="font-bold text-slate-100">{formatVnd(product.unitPrice * quantity)}</span>
            </p>
          )}
        </fieldset>

        <div className="mt-4 border-t border-slate-800 pt-4">
          <FailureToggles value={failures} disabled={locked} onChange={setFailures} />
        </div>

        {error && (
          <p
            role="alert"
            className="mt-3 rounded-lg border border-red-700 bg-red-950/60 px-3 py-2 text-sm text-red-200"
          >
            {error}
          </p>
        )}

        <div className="mt-5 flex gap-3">
          <button
            type="submit"
            disabled={locked || !product}
            className="flex-1 rounded-lg bg-sky-600 px-4 py-3 text-base font-extrabold text-white transition hover:bg-sky-500 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-300 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {busy ? "Saga running..." : "▶ START SAGA"}
          </button>
          <button
            type="button"
            onClick={reset}
            disabled={busy}
            title="Wipe all service databases and re-seed the stock"
            className="rounded-lg border border-slate-600 px-4 py-3 text-sm font-bold text-slate-200 hover:bg-slate-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-400 disabled:opacity-40"
          >
            Reset data
          </button>
        </div>
      </form>
    </Card>
  );
}
