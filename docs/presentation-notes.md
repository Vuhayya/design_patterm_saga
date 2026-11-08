# Ghi chú thuyết trình - Saga Pattern (MSS301)

Thời lượng gợi ý: **12-15 phút** (4 phút lý thuyết, 7 phút demo, 2-4 phút hỏi đáp).
Mọi con số, tên bước, tên service dưới đây khớp với demo trong repo.

---

## 0. Chuẩn bị trước giờ (làm trước 10 phút)

- [ ] `scripts\start-backend.ps1`, đợi cả 5 cửa sổ hiện "Started" (khoảng 1 phút)
- [ ] `scripts\start-frontend.ps1`, mở http://localhost:3000
- [ ] Thanh trên cùng hiện **4 service online**
- [ ] Bấm **Reset data**, History phải trống, món "Double Cheese Burger" còn 100
- [ ] Chạy thử **1 lần kịch bản 1** để chắc chắn mọi thứ thông, rồi **Reset data** lại
- [ ] Trình duyệt zoom 110-125% nếu chiếu máy chiếu; tắt thông báo
- [ ] Mở sẵn tab thứ 2: http://localhost:3000/how-it-works
- [ ] Có **video quay màn hình dự phòng** (xem mục 6)

Không chạy `npm run e2e` hay `npm run check` ngay trước giờ: e2e xoá dữ liệu demo, check build chung thư mục với dev server.

---

## 1. Mở đầu - vấn đề (2 phút)

**Ý chính:** trong monolith, một transaction cho cả đơn hàng; trong microservice thì không.

Nói:
> "Đặt một đơn đồ ăn cần 4 việc: tạo đơn, giữ món trong kho, thanh toán, xác nhận. Trong monolith, ta bọc cả 4
> trong một transaction, lỗi ở đâu thì rollback hết. Nhưng khi tách thành microservice, mỗi service có database
> riêng, nên **không có transaction nào bao trùm được cả bốn**. Nếu thanh toán lỗi mà kho đã bị giữ hàng thì
> sao?"

Gợi ý hỏi khán giả: "Vì sao không dùng 2PC (two-phase commit)?" Trả lời ngắn: giữ lock qua nhiều service,
coordinator là điểm nghẽn, không hợp với microservice và các DB khác loại.

## 2. Giải pháp - Saga (2 phút)

Nói 3 ý, đúng như trang **How Saga Works**:

1. **Local transaction:** mỗi bước commit ngay trong service sở hữu dữ liệu. Không giữ lock xuyên service.
2. **Compensation, không phải rollback:** thứ đã commit thì không rollback được, ta hoàn tác bằng một hành động
   nghiệp vụ mới (huỷ đơn, trả hàng, hoàn tiền).
3. **Thứ tự ngược:** hoàn tiền trước, trả hàng sau, huỷ đơn cuối.

Bảng cần chiếu (có sẵn ở `/how-it-works`):

| Bước | Làm                 | Hoàn tác            |
| ---- | ------------------- | ------------------- |
| T1   | `CREATE_ORDER`      | `CANCEL_ORDER`      |
| T2   | `RESERVE_INVENTORY` | `RELEASE_INVENTORY` |
| T3   | `PROCESS_PAYMENT`   | `REFUND_PAYMENT`    |
| T4   | `CONFIRM_ORDER`     | không có (bước cuối) |

**Orchestration vs Choreography** (30 giây): bản demo dùng orchestration, một orchestrator ra lệnh cho từng
service, toàn bộ luồng nằm trong một class, dễ đọc và debug. Choreography: các service phản ứng với event của
nhau, ít phụ thuộc hơn nhưng luồng bị rải ra nhiều nơi. Đánh đổi: orchestrator là điểm điều phối cần đáng tin cậy.

## 3. Kiến trúc (1 phút)

Chỉ vào sơ đồ trong README: Browser → Gateway :8080 → Orchestrator :8084 → 3 service con, **mỗi service một DB
riêng**. Nhấn mạnh: các service con **không biết nhau**, chỉ orchestrator biết luồng.

---

## 4. Demo (7 phút)

Mẹo: sau mỗi kịch bản, **chỉ vào 3 chỗ**: (a) timeline, (b) State Comparison, (c) Service Communication.

### Kịch bản 1 - Happy path (1 phút)

Chọn "1 · Happy path" → **START SAGA**.
- Chỉ timeline: 4 ô lần lượt chuyển xanh (mỗi bước cách nhau ~0,7 giây là **chủ ý**, để khán giả nhìn kịp).
- State Comparison: đơn `NOT_CREATED → CONFIRMED`, reserved `0 → 1`, thanh toán `COMPLETED`.
- Service Communication: 4 lời gọi HTTP thật, 201/200.

> "4 local transaction, 4 service, 4 database, không có transaction chung nào."

### Kịch bản 3 - Payment fails (2 phút) - **kịch bản quan trọng nhất**

Chọn "3 · Payment fails" → **START SAGA**.
- Bước 3 đỏ `FAILED`, bước 4 xám `SKIPPED`.
- Hàng **Compensating transactions** sáng lên: `RELEASE_INVENTORY` rồi `CANCEL_ORDER`.
- `REFUND_PAYMENT` vẫn **PENDING**: hỏi khán giả vì sao? → vì thanh toán chưa từng commit, không có gì để hoàn.
- Banner: "every committed step was undone - the system is consistent again". State Comparison: reserved về lại
  như cũ, đơn `CANCELLED`.
- Mở Live Log: có dòng `STARTING COMPENSATION - 2 committed step(s) ... RELEASE_INVENTORY <- CREATE_ORDER`.
  Đây chính là **stack**: hệ thống biết cần hoàn tác gì vì nó nhớ những bước đã thành công.

### Kịch bản 4 - Confirm fails (1 phút, bỏ qua nếu thiếu giờ)

Cho thấy cả **ba** compensation chạy ngược: `REFUND_PAYMENT → RELEASE_INVENTORY → CANCEL_ORDER`.
Ý: bước cuối lỗi vẫn phải hoàn tác mọi thứ trước đó, kể cả tiền đã trừ.

### Kịch bản 5 - Compensation fails (2 phút) - **điểm ăn tiền**

Chọn "5 · Compensation fails" → **START SAGA**.
- Payment lỗi, rồi `RELEASE_INVENTORY` cũng lỗi, thử lại 2 lần ("2 attempts"), vẫn lỗi.
- `CANCEL_ORDER` vẫn chạy (best effort), nhưng saga kết thúc `COMPENSATION FAILED`, banner tím
  "inconsistent", khung "Manual recovery required".
- State Comparison: **reserved tăng thêm 1 mà không được trả lại**.

> "Đây là giới hạn thật của Saga: nó **không đảm bảo rollback** như ACID. Khi chính bước hoàn tác lỗi, hệ thống
> không tự sửa được và cần người can thiệp. Trong thực tế ta dùng retry, dead-letter queue và cảnh báo."

### Kết demo (30 giây)

- Cuộn xuống **Saga History**, bấm một dòng cũ để xem lại: mọi saga đều được lưu lại, kể cả log và từng lời gọi.
- (Tuỳ chọn) mở H2 console của 2 service để chứng minh dữ liệu nằm ở **hai database khác nhau**.

---

## 5. Hỏi đáp - câu có thể gặp

**Vì sao compensation phải idempotent?**
Vì orchestrator có thể retry (demo retry 2 lần). Gọi `release` hai lần không được trả hàng hai lần. Trong code mỗi
service kiểm tra trạng thái trước khi làm, ví dụ confirm một đơn đã confirm là no-op chứ không phải lỗi.

**Làm sao hệ thống biết cần hoàn tác bước nào?**
Orchestrator giữ một stack các bước đã thành công (`completedSteps`), khi lỗi thì pop từng bước và gọi
`compensate()` của bước đó. Ánh xạ bước → hoàn tác nằm tập trung ở enum `CompensationAction`.

**Nếu orchestrator sập giữa chừng thì sao?** *(câu khó, trả lời thẳng)*
Bản demo **chưa tự phục hồi**. Orchestrator lưu trạng thái saga và id của từng bước vào SAGA DB, nên về nguyên tắc
có thể đọc lại và tiếp tục hoàn tác sau khi khởi động lại, nhưng phần recovery đó chưa được cài đặt. Hệ thống
production sẽ có tiến trình quét các saga dở dang, hoặc dùng engine workflow bền vững.

**Orchestration hay Choreography, khi nào dùng cái nào?**
Orchestration hợp khi luồng nghiệp vụ dài/phức tạp, cần nhìn và debug tập trung (như demo). Choreography hợp khi
luồng đơn giản, ít service, cần ít phụ thuộc. Chọn orchestration ở đây vì dễ trình bày và quan sát.

**Có cô lập (isolation) không?**
Không. Saga thiếu chữ "I" của ACID: người khác có thể thấy trạng thái trung gian (hàng đã giữ, đơn chưa xác
nhận). Cách giảm: semantic lock (trạng thái `PENDING`), thao tác commutative, đọc lại trước khi quyết định.

**Vì sao gọi HTTP đồng bộ mà không dùng Kafka?**
Để demo dễ theo dõi và dễ debug trên một máy. Hệ thống thật nên dùng message broker để giảm coupling và có
retry bền vững; orchestrator đã có sẵn điểm nối `SagaEventPublisher` (hiện chỉ ghi log).

**Lỗi trong demo có thật không?**
Là **giả lập** bằng cờ (`failProcessPayment`...), nhưng đường đi của lời gọi, commit và compensation là thật:
dữ liệu thay đổi thật trong từng database, và ta đọc lại để so sánh trước/sau.

**Tại sao có độ trễ 0,7 giây giữa các bước?**
Chỉ để khán giả thấy timeline chạy từng ô; không liên quan tới Saga. Chỉnh bằng `SAGA_STEP_DELAY_MS`.

---

## 6. Phương án dự phòng

| Sự cố                                  | Xử lý                                                                                  |
| -------------------------------------- | -------------------------------------------------------------------------------------- |
| Một service chưa lên / báo OFFLINE     | Đợi thêm; nếu vẫn đỏ, `stop-backend.ps1` rồi `start-backend.ps1` lại (mất ~1 phút).   |
| Trang báo "Cannot reach the API Gateway" | Gateway :8080 chưa chạy. Kiểm tra cửa sổ `api-gateway`.                              |
| Kết quả demo lệch (tồn kho không còn 100) | Bấm **Reset data**.                                                                  |
| Máy chiếu lỗi font/chữ nhỏ             | Zoom trình duyệt (Ctrl +).                                                             |
| Mọi thứ hỏng                           | Phát **video quay màn hình** đã chuẩn bị, hoặc dùng Postman gọi `POST /api/sagas/orders?async=false`. |

**Nên quay sẵn video** của kịch bản 1, 3 và 5 (khoảng 2 phút) trước ngày thuyết trình.

---

## 7. Điều nên nhớ khi nói

- Luôn nói "**compensation**" (hoàn tác nghiệp vụ), không nói "rollback".
- Nói rõ đây là **mô phỏng lỗi**; không giấu giới hạn, vì kịch bản 5 chính là phần thể hiện hiểu biết sâu.
- Mỗi kịch bản: chỉ vào timeline → State Comparison → Service Communication, đừng đọc log dài.
- Nếu hết giờ: giữ **kịch bản 1, 3, 5**; bỏ 2 và 4.
