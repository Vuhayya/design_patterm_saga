"use client";

import { Card } from "@/components/Card";
import { api } from "@/lib/api";
import { DEFINITION_POLL_MS } from "@/lib/config";
import { usePolling } from "@/hooks/usePolling";
import { SERVICE_LABEL, STEP_LABEL } from "@/lib/saga";

function Point({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="rounded-lg border border-slate-800 bg-slate-950/50 p-4">
      <h3 className="mb-1 font-bold text-slate-100">{title}</h3>
      <p className="text-sm leading-relaxed text-slate-400">{children}</p>
    </div>
  );
}

export default function HowSagaWorksPage() {
  // Served from the running orchestrator, so this page can never disagree with the code.
  const { data: def, error } = usePolling(api.definition, DEFINITION_POLL_MS);

  return (
    <div className="mx-auto max-w-[1100px] space-y-5 px-6 py-6">
      <Card title="Saga pattern là gì?" subtitle="Giao dịch phân tán mà không cần khoá phân tán">
        <p className="mb-4 text-slate-300">
          Trong một ứng dụng monolith, một transaction của database giúp việc tạo đơn, giữ hàng và thanh toán xảy ra
          theo kiểu &quot;tất cả hoặc không gì cả&quot;. Với microservice, mỗi service sở hữu database riêng nên không
          có transaction nào trải qua được tất cả. <b className="text-sky-300">Saga</b> thay transaction lớn đó bằng một
          chuỗi <b className="text-sky-300">local transaction</b>. Nếu một bước lỗi, Saga chạy các{" "}
          <b className="text-amber-300">compensating transaction</b> để hoàn tác (về mặt nghiệp vụ) những bước đã commit.
        </p>
        <div className="grid gap-3 md:grid-cols-3">
          <Point title="1 · Local transaction">
            Mỗi bước commit ngay trong service sở hữu dữ liệu. Không giữ transaction mở, không khoá dữ liệu xuyên
            service.
          </Point>
          <Point title="2 · Bù trừ, không phải rollback">
            Thay đổi đã commit thì không rollback được. Nó được hoàn tác bằng một hành động nghiệp vụ mới: huỷ đơn, trả
            hàng, hoàn tiền.
          </Point>
          <Point title="3 · Thứ tự ngược">
            Orchestrator ghi các bước thành công vào một stack và hoàn tác theo kiểu vào sau, ra trước (LIFO): hoàn tiền
            trước, trả hàng sau, huỷ đơn cuối cùng.
          </Point>
        </div>
      </Card>

      <Card
        title="Saga trong demo này"
        subtitle="Đọc trực tiếp từ code của orchestrator (GET /api/sagas/definition)"
        right={
          <span className="rounded-full bg-sky-900/60 px-3 py-1 text-xs font-bold text-sky-300">
            {def?.sagaStyle ?? "..."}
          </span>
        }
      >
        {error && !def && <p className="text-sm text-red-300">Không kết nối được backend: {error}</p>}
        {def && (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs tracking-wide text-slate-500 uppercase">
                  <th className="pb-2">#</th>
                  <th className="pb-2">Local transaction</th>
                  <th className="pb-2">Service</th>
                  <th className="pb-2">Compensating transaction</th>
                  <th className="pb-2">Service</th>
                </tr>
              </thead>
              <tbody>
                {def.forwardSteps.map((s, i) => (
                  <tr key={s.step} className="border-t border-slate-800">
                    <td className="py-3 font-mono text-slate-500">T{i + 1}</td>
                    <td className="py-3 font-bold text-sky-300">{STEP_LABEL[s.step] ?? s.step}</td>
                    <td className="py-3 text-slate-300">{SERVICE_LABEL[s.targetService] ?? s.targetService}</td>
                    <td className="py-3 font-bold text-amber-300">
                      {s.hasCompensation ? (
                        <>
                          <span className="mr-2 font-mono text-slate-500">C{i + 1}</span>
                          {STEP_LABEL[s.compensation] ?? s.compensation}
                        </>
                      ) : (
                        <span className="font-normal text-slate-500">không có - bước cuối, không có gì để hoàn tác</span>
                      )}
                    </td>
                    <td className="py-3 text-slate-300">
                      {s.hasCompensation ? (SERVICE_LABEL[s.compensationService] ?? s.compensationService) : ""}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="mt-4 grid gap-3 md:grid-cols-2">
              <div className="rounded-lg bg-slate-950/60 p-3 font-mono text-xs text-slate-300">
                <div className="mb-1 font-sans text-xs font-bold text-slate-500 uppercase">Thành công (happy path)</div>
                T1 → T2 → T3 → T4 → COMPLETED
              </div>
              <div className="rounded-lg bg-slate-950/60 p-3 font-mono text-xs text-amber-200">
                <div className="mb-1 font-sans text-xs font-bold text-slate-500 uppercase">Nếu T3 lỗi</div>
                T1 → T2 → T3 ✕ → C2 → C1 → COMPENSATED
              </div>
            </div>
          </div>
        )}
      </Card>

      <Card title="Các trạng thái của Saga" subtitle="Vòng đời của một saga">
        <div className="flex flex-wrap items-center gap-2 text-sm">
          {(def?.sagaStates ?? []).map((s, i) => (
            <span key={s} className="flex items-center gap-2">
              {i > 0 && <span className="text-slate-600">·</span>}
              <span className="rounded-md bg-slate-800 px-2 py-1 font-mono text-xs text-slate-200">{s}</span>
            </span>
          ))}
        </div>
        <p className="mt-3 text-sm text-slate-400">
          <b className="text-fuchsia-300">COMPENSATION_FAILED</b> là kết cục trung thực: nếu chính bước hoàn tác bị lỗi
          thì hệ thống bất nhất và phải có người sửa thủ công. Hãy thử kịch bản 5 trong Control Room.
        </p>
      </Card>

      <div className="grid gap-5 md:grid-cols-2">
        <Card title="Orchestration và Choreography">
          <div className="space-y-3 text-sm text-slate-400">
            <p>
              <b className="text-sky-300">Orchestration (demo này):</b> một orchestrator trung tâm bảo từng service làm
              gì và hoàn tác gì. Toàn bộ luồng nằm trong một class nên dễ đọc, dễ theo dõi và dễ debug.
            </p>
            <p>
              <b className="text-slate-200">Choreography:</b> các service phản ứng với event của nhau, không có
              &quot;bộ não&quot; trung tâm. Ít phụ thuộc lẫn nhau hơn, nhưng luồng bị rải khắp các service nên khó theo
              dõi.
            </p>
            <p className="text-slate-500">Đánh đổi: orchestrator là điểm điều phối nên bản thân nó phải đáng tin cậy.</p>
          </div>
        </Card>

        <Card title="Những giới hạn cần nói rõ">
          <ul className="list-disc space-y-2 pl-5 text-sm text-slate-400">
            <li>
              <b className="text-slate-200">Không có isolation.</b> Người dùng khác có thể thấy trạng thái trung gian
              trong chốc lát (hàng đã giữ nhưng đơn chưa được xác nhận).
            </li>
            <li>
              <b className="text-slate-200">Compensation phải idempotent</b> vì có thể bị retry.
            </li>
            <li>
              <b className="text-slate-200">Không đảm bảo hoàn tác được.</b> Compensation bị lỗi thì cần xử lý thủ
              công.
            </li>
            <li>
              <b className="text-slate-200">Phạm vi demo.</b> Lỗi được giả lập bằng cờ, các lời gọi là HTTP đồng bộ. Hệ
              thống thật sẽ dùng message broker (orchestrator đã có sẵn chỗ publish event cho việc này).
            </li>
          </ul>
        </Card>
      </div>
    </div>
  );
}
