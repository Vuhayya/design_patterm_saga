# Saga Pattern Demo - Online Food Ordering (MSS301)

Demo của **Saga Pattern (kiểu Orchestration)** trên 4 microservice Spring Boot và một giao diện Next.js.
Chạy hoàn toàn trên máy local, không cần Docker hay deploy.

Câu hỏi mà demo trả lời: *khi mỗi service có database riêng, một đơn hàng gồm "tạo đơn → giữ hàng → thanh toán →
xác nhận" không thể nằm trong một transaction. Nếu bước thanh toán lỗi thì làm sao hoàn lại phần đã làm?*

- Tiếng Việt ngắn gọn: **mỗi bước là một transaction cục bộ đã commit; khi có bước lỗi, orchestrator gọi các
  _compensating transaction_ để hoàn tác các bước đã xong, theo thứ tự ngược lại.**
- Ghi chú thuyết trình: [`docs/presentation-notes.md`](docs/presentation-notes.md)

---

## 1. Kiến trúc

```
                 Browser (Next.js :3000)
                          │  chỉ gọi qua gateway
                          ▼
                 API Gateway :8080  (Spring Cloud Gateway, CORS tập trung)
                          │
        ┌─────────────────┼─────────────────┬──────────────────┐
        ▼                 ▼                 ▼                  ▼
  Saga Orchestrator   Order Service    Inventory Service   Payment Service
       :8084              :8081             :8082              :8083
    (SAGA DB)          (ORDER DB)       (INVENTORY DB)      (PAYMENT DB)
        │                 ▲                 ▲                  ▲
        └── HTTP ─────────┴─────────────────┴──────────────────┘
          orchestrator gọi từng service theo thứ tự và ghi lại mọi lời gọi
```

- Mỗi service có **H2 database riêng** (file), không service nào đọc DB của service khác.
- Chỉ **orchestrator** biết toàn bộ luồng. Các service con không biết nhau.
- Frontend không bao giờ gọi trực tiếp service con.

| Service            | Port | Vai trò                                                    | Test |
| ------------------ | ---- | ---------------------------------------------------------- | ---- |
| `api-gateway`      | 8080 | Cổng vào duy nhất, định tuyến `/api/**`, CORS              | -    |
| `order-service`    | 8081 | Tạo, xác nhận, huỷ đơn                                     | 8    |
| `inventory-service`| 8082 | Giữ và trả lại tồn kho                                     | 8    |
| `payment-service`  | 8083 | Thanh toán và hoàn tiền                                    | 7    |
| `saga-orchestrator`| 8084 | Điều phối saga, compensation, ghi log, snapshot trạng thái | 8    |
| `frontend`         | 3000 | Control room                                               | 27 + 8 e2e |

## 2. Saga trong demo

| Bước | Local transaction (forward)    | Service   | Compensating transaction | Service   |
| ---- | ------------------------------ | --------- | ------------------------ | --------- |
| T1   | `CREATE_ORDER`                 | order     | `CANCEL_ORDER`           | order     |
| T2   | `RESERVE_INVENTORY`            | inventory | `RELEASE_INVENTORY`      | inventory |
| T3   | `PROCESS_PAYMENT`              | payment   | `REFUND_PAYMENT`         | payment   |
| T4   | `CONFIRM_ORDER`                | order     | _không có_ (bước cuối)   | -         |

- Compensation chạy theo **thứ tự ngược** bằng một stack: hoàn tiền trước, trả hàng sau, huỷ đơn cuối.
- Mỗi compensation được **retry** (mặc định 2 lần) và đều **idempotent**, nên retry an toàn.
- Nếu một compensation vẫn lỗi, saga kết thúc ở `COMPENSATION_FAILED` và đánh dấu cần xử lý thủ công. Các
  compensation còn lại vẫn được thử (best effort).
- Trạng thái saga: `PENDING → RUNNING → COMPLETED` hoặc `FAILED → COMPENSATING → COMPENSATED | COMPENSATION_FAILED`.

Code đáng đọc nhất: `saga-orchestrator/.../service/SagaOrchestratorService.java` (vòng lặp forward + stack
compensation) và `saga/SagaStep.java` (mỗi bước biết cách làm và cách hoàn tác của chính nó).

## 3. Chạy demo

Yêu cầu: JDK 21 (qua `JAVA_HOME`), Maven, Node 20+.

> `java` trên PATH có thể là JDK 8. Các script dùng `mvn spring-boot:run` nên Maven sẽ dùng `JAVA_HOME`.
> Đừng chạy bằng `java -jar`.

```powershell
scripts\start-backend.ps1     # mở 5 cửa sổ, đợi khoảng 1 phút cho tới khi cả 5 hiện "Started"
scripts\start-frontend.ps1    # http://localhost:3000
scripts\stop-backend.ps1      # tắt tất cả (cả :3000)
```

Kiểm tra nhanh: mở http://localhost:3000, thanh trên cùng phải hiện **4 service online** (màu xanh).

**Trước giờ thuyết trình:** bấm **Reset data** để xoá lịch sử và đưa tồn kho về 100.

## 4. Năm kịch bản

Chọn ở cột trái của Control Room rồi bấm **START SAGA**.

| #   | Kịch bản                       | Kết quả mong đợi                                                                             |
| --- | ------------------------------ | -------------------------------------------------------------------------------------------- |
| 1   | Happy path                     | 4 bước `COMPLETED`, đơn `CONFIRMED`, tồn kho giảm 1. Saga `COMPLETED`.                       |
| 2   | Inventory fails                | Bước 2 `FAILED`, chỉ `CANCEL_ORDER` chạy. Saga `COMPENSATED`.                                |
| 3   | Payment fails                  | Bước 3 `FAILED`, `RELEASE_INVENTORY` rồi `CANCEL_ORDER`. Tồn kho về như cũ. `COMPENSATED`.   |
| 4   | Confirm fails                  | Bước 4 `FAILED`, `REFUND_PAYMENT` → `RELEASE_INVENTORY` → `CANCEL_ORDER`. `COMPENSATED`.     |
| 5   | Compensation fails             | Payment lỗi, rồi `RELEASE_INVENTORY` cũng lỗi (retry 2 lần). Saga `COMPENSATION_FAILED`, tồn kho **không** được trả lại, banner "inconsistent". |

Có thể tự bật lỗi cho từng bước ở phần **Failure simulation (custom)**, kể cả các bước compensation.

Giao diện gồm: **Saga Timeline** (từng ô đổi màu theo bước), **State Comparison** (số liệu đọc lại từ service
thật trước và sau), **Service Communication** (mọi lời gọi HTTP, method, endpoint, status, thời gian), **Live Log**,
**Saga History** (bấm để xem lại), và trang **How Saga Works** (bảng T/C lấy trực tiếp từ code của orchestrator).

## 5. API chính (qua gateway :8080)

| Method | Đường dẫn                    | Mô tả                                                                                 |
| ------ | ---------------------------- | ------------------------------------------------------------------------------------- |
| POST   | `/api/sagas/orders`          | Bắt đầu saga (chạy nền, trả về `sagaId`). `?async=false` để chờ saga kết thúc.         |
| GET    | `/api/sagas/{sagaId}`        | Toàn bộ lịch sử thực thi: steps, serviceCalls, logs, stateBefore/After                |
| GET    | `/api/sagas?limit=N`         | Danh sách saga gần nhất                                                               |
| GET    | `/api/sagas/menu`            | Món ăn và tồn kho hiện tại                                                            |
| GET    | `/api/sagas/system/health`   | Trạng thái 4 service                                                                  |
| GET    | `/api/sagas/definition`      | Định nghĩa saga lấy từ code đang chạy                                                 |
| DELETE | `/api/sagas/reset`           | Xoá dữ liệu mọi service và nạp lại tồn kho                                            |

Ví dụ bật lỗi thanh toán:

```bash
curl -X POST "http://localhost:8080/api/sagas/orders?async=false" -H "Content-Type: application/json" \
  -d '{"customerName":"A","productId":"BURGER-01","productName":"Burger","quantity":1,"unitPrice":85000,
       "failureSimulation":{"failProcessPayment":true}}'
```

Các cờ lỗi: `failCreateOrder`, `failReserveInventory`, `failProcessPayment`, `failConfirmOrder`,
`failReleaseInventory`, `failRefundPayment`, `failCancelOrder`.

Mỗi service còn có H2 console (`http://localhost:808x/h2-console`, user `sa`, không mật khẩu) để chứng minh dữ
liệu thật nằm ở từng DB riêng.

## 6. Kiểm thử

```powershell
mvn test                        # backend: 31 test (23 ở 3 service con + 8 ở orchestrator)
cd frontend; npm run check      # lint + typecheck + 27 unit test + build   (tắt dev server trước)
cd frontend; npm run e2e        # 8 test Playwright trên hệ thống thật (cần backend đang chạy)
```

- Test orchestrator dùng step giả để kiểm tra đúng **thứ tự** gọi forward/compensation, retry, và việc compensation
  lỗi vẫn không chặn các compensation còn lại.
- Bộ e2e **reset dữ liệu demo**, đừng chạy ngay trước giờ thuyết trình.

## 7. Giới hạn của bản demo (nên nói thẳng khi thuyết trình)

- **Lỗi là giả lập** bằng cờ, không phải lỗi hạ tầng thật.
- **Gọi đồng bộ qua HTTP.** Hệ thống thật thường dùng message broker (Kafka/RabbitMQ). Orchestrator đã có sẵn
  điểm nối `SagaEventPublisher`, hiện chỉ ghi log.
- **Orchestrator chưa tự phục hồi** nếu nó bị sập giữa chừng: saga đang chạy dở sẽ không được tiếp tục tự động. Id
  của các bước đã commit được lưu trong SAGA DB nên có thể làm tiếp, nhưng phần recovery này chưa được cài đặt.
- **Không có isolation.** Người khác có thể thấy trạng thái trung gian (đã giữ hàng nhưng đơn chưa xác nhận).
- **Gateway chưa có test riêng**, và các service trả HTTP 500 thay vì 404 cho đường dẫn không tồn tại (không ảnh
  hưởng demo).
- Dữ liệu lưu trong H2 dạng file, chỉ phù hợp để demo.

## 8. Cấu trúc thư mục

```
pom.xml                  aggregator (Spring Boot 3.5, Java 21)
api-gateway/             định tuyến + CORS
order-service/           inventory-service/    payment-service/
saga-orchestrator/       saga/ (SagaStep, 4 step), service/ (SagaOrchestratorService), client/, entity/
frontend/                Next.js + TypeScript + Tailwind (xem frontend/README.md)
scripts/                 start-backend.ps1, start-frontend.ps1, stop-backend.ps1
docs/                    presentation-notes.md
```
