"use client";

import { ControlPanel } from "@/components/ControlPanel";
import { LiveLog } from "@/components/LiveLog";
import { SagaHistory } from "@/components/SagaHistory";
import { SagaTimeline } from "@/components/SagaTimeline";
import { ServiceCommunication } from "@/components/ServiceCommunication";
import { ServiceHealthBar } from "@/components/ServiceHealthBar";
import { StateComparison } from "@/components/StateComparison";
import { useSagaRun } from "@/hooks/useSagaRun";

export default function ControlRoomPage() {
  const { saga, selectedId, running, error, select, clear } = useSagaRun();

  return (
    <div className="mx-auto max-w-[1500px] space-y-5 px-4 py-5 sm:px-6">
      <ServiceHealthBar />

      {error && (
        <p role="alert" className="rounded-lg border border-red-700 bg-red-950/60 px-4 py-2 text-sm text-red-200">
          Lost contact with the orchestrator: {error}
        </p>
      )}

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-[380px_1fr]">
        <ControlPanel busy={running} onStarted={select} onReset={clear} />
        <div className="space-y-5">
          <SagaTimeline saga={saga} />
          <StateComparison saga={saga} />
        </div>
      </div>

      <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
        <ServiceCommunication saga={saga} />
        <LiveLog saga={saga} />
      </div>

      <SagaHistory selectedId={selectedId} onSelect={select} refreshKey={`${saga?.sagaId}:${saga?.status}`} />
    </div>
  );
}
