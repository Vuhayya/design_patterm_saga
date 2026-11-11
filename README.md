# Saga Pattern Demo - Online Food Ordering (MSS301)

Demo của **Saga Pattern (kiểu Orchestration)** trên 4 microservice Spring Boot, 1 API Gateway và một giao diện
Next.js. Chạy hoàn toàn trên máy local, **không cần Docker, không cần cài database**.

Câu hỏi mà demo trả lời: *khi mỗi service có database riêng, một đơn hàng gồm "tạo đơn → giữ hàng → thanh toán →
xác nhận" không thể nằm trong một transaction. Nếu bước thanh toán lỗi thì làm sao hoàn lại phần đã làm?*

- Tóm tắt: **mỗi bước là một transaction cục bộ đã commit; khi có bước lỗi, orchestrator gọi các
  _compensating transaction_ để hoàn tác các bước đã xong, theo thứ tự ngược lại.**
- Ghi chú thuyết trình: [`docs/presentation-notes.md`](docs/presentation-notes.md)

**Mục lục**

1. [Kiến trúc](#1-kiến-trúc)
2. [Saga trong demo](#2-saga-trong-demo)
3. [Hướng dẫn cài đặt và chạy](#3-hướng-dẫn-cài-đặt-và-chạy)
4. [Năm kịch bản demo](#4-năm-kịch-bản-demo)
5. [API chính](#5-api-chính-qua-gateway-8080)
6. [Kiểm thử](#6-kiểm-thử)
7. [Xử lý sự cố thường gặp](#7-xử-lý-sự-cố-thường-gặp)
8. [Giới hạn của bản demo](#8-giới-hạn-của-bản-demo)
9. [Cấu trúc thư mục](#9-cấu-trúc-thư-mục)

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

- Mỗi service có **H2 database riêng** (dạng file), không service nào đọc DB của service khác.
- Chỉ **orchestrator** biết toàn bộ luồng. Các service con không biết nhau.
- Frontend không bao giờ gọi trực tiếp service con.

| Service             | Port | Vai trò                                                    | Test       |
| ------------------- | ---- | ---------------------------------------------------------- | ---------- |
| `api-gateway`       | 8080 | Cổng vào duy nhất, định tuyến `/api/**`, CORS              | -          |
| `order-service`     | 8081 | Tạo, xác nhận, huỷ đơn                                     | 8          |
| `inventory-service` | 8082 | Giữ và trả lại tồn kho                                     | 8          |
| `payment-service`   | 8083 | Thanh toán và hoàn tiền                                    | 7          |
| `saga-orchestrator` | 8084 | Điều phối saga, compensation, ghi log, snapshot trạng thái | 8          |
| `frontend`          | 3000 | Control room                                               | 27 + 8 e2e |

## 2. Saga trong demo

| Bước | Local transaction (forward) | Service   | Compensating transaction | Service   |
| ---- | --------------------------- | --------- | ------------------------ | --------- |
| T1   | `CREATE_ORDER`              | order     | `CANCEL_ORDER`           | order     |
| T2   | `RESERVE_INVENTORY`         | inventory | `RELEASE_INVENTORY`      | inventory |
| T3   | `PROCESS_PAYMENT`           | payment   | `REFUND_PAYMENT`         | payment   |
| T4   | `CONFIRM_ORDER`             | order     | _không có_ (bước cuối)   | -         |

- Compensation chạy theo **thứ tự ngược** bằng một stack: hoàn tiền trước, trả hàng sau, huỷ đơn cuối.
- Mỗi compensation được **retry** (mặc định 2 lần) và đều **idempotent**, nên retry an toàn.
- Nếu một compensation vẫn lỗi, saga kết thúc ở `COMPENSATION_FAILED` và đánh dấu cần xử lý thủ công. Các
  compensation còn lại vẫn được thử (best effort).
- Trạng thái saga: `PENDING → RUNNING → COMPLETED` hoặc `FAILED → COMPENSATING → COMPENSATED | COMPENSATION_FAILED`.

Code đáng đọc nhất: `saga-orchestrator/.../service/impl/SagaOrchestratorServiceImpl.java` (vòng lặp forward + stack
compensation) và `saga/SagaStep.java` (mỗi bước biết cách làm và cách hoàn tác của chính nó).

---

## 3. Hướng dẫn cài đặt và chạy

Hướng dẫn viết cho **Windows** (PowerShell). Trên macOS/Linux các lệnh `mvn` và `npm` giống hệt, chỉ khác
cách đặt biến môi trường và không dùng được các script `.ps1` (xem [3.5 Cách B](#cách-b-chạy-thủ-công-từng-service)).

### 3.1. Phần mềm cần có

| Phần mềm | Phiên bản         | Kiểm tra bằng lệnh | Ghi chú                                      |
| -------- | ----------------- | ------------------ | -------------------------------------------- |
| JDK      | **21**            | `java -version`    | Bắt buộc 21. JDK 8/11/17 sẽ không build được |
| Maven    | 3.9 trở lên       | `mvn -version`     | Dòng `Java version` trong output phải là 21  |
| Node.js  | **20.9** trở lên  | `node -v`          | Next.js 16 yêu cầu Node ≥ 20.9 (khuyên dùng 22 LTS) |
| npm      | đi kèm Node       | `npm -v`           |                                              |
| Git      | bất kỳ            | `git --version`    | Để clone repo                                |

Chưa có thì cài nhanh bằng `winget` (mở PowerShell):

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
winget install Apache.Maven
winget install OpenJS.NodeJS.LTS
winget install Git.Git
```

Cài xong **đóng và mở lại terminal** để PATH mới có hiệu lực.

> Không cần cài MySQL/PostgreSQL hay Docker. Mỗi service tự tạo file database H2 trong thư mục `data/` của nó
> ở lần chạy đầu tiên.

### 3.2. Cấu hình `JAVA_HOME` (quan trọng)

Maven dùng JDK mà biến `JAVA_HOME` trỏ tới, **không phải** `java` trên PATH. Nhiều máy có sẵn JDK 8 trên PATH,
nên hãy kiểm tra:

```powershell
mvn -version
# ...
# Java version: 21.0.x, vendor: Eclipse Adoptium, runtime: C:\Program Files\Eclipse Adoptium\jdk-21...
```

Nếu dòng `Java version` không phải 21, đặt lại `JAVA_HOME` (thay đường dẫn cho đúng máy bạn):

```powershell
# Đặt vĩnh viễn cho user hiện tại, rồi mở terminal mới
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot"

# Hoặc chỉ cho cửa sổ hiện tại
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot"
```

Cũng có thể đặt qua *System Properties → Environment Variables*.

### 3.3. Lấy mã nguồn

```powershell
git clone https://github.com/Vuhayya/design_patterm_saga.git
cd design_patterm_saga
```

> Nên để project ở đường dẫn **không có dấu tiếng Việt**. Dấu cách thì không sao.

### 3.4. Build lần đầu

Bước này tải toàn bộ thư viện về (lần đầu mất vài phút, cần mạng) và kiểm tra code biên dịch được.

```powershell
# Backend: build 5 service, bỏ qua test cho nhanh
mvn clean install -DskipTests

# Frontend: cài thư viện
cd frontend
npm install
cd ..
```

Kết quả đúng: Maven in `BUILD SUCCESS` với 6 dòng `SUCCESS` (parent + 5 module), `npm install` không báo `ERR!`.

### 3.5. Chạy backend

Có hai cách. **Cách A** tiện nhất trên Windows, **Cách B** dùng khi muốn hiểu rõ hoặc không chạy được script.

#### Cách A: dùng script (Windows)

Từ thư mục gốc của project:

```powershell
.\scripts\start-backend.ps1
```

Script mở **5 cửa sổ PowerShell**, mỗi cửa sổ một service (tiêu đề cửa sổ là tên service), theo thứ tự:
`order-service → inventory-service → payment-service → saga-orchestrator → api-gateway`.

Đợi khoảng **1 phút** đến khi **cả 5 cửa sổ** đều có dòng dạng:

```
Started OrderServiceApplication in 6.123 seconds
```

> Nếu PowerShell báo `running scripts is disabled on this system`, chạy lệnh sau một lần rồi thử lại:
> ```powershell
> Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
> ```
> Hoặc chạy không cần đổi policy: `powershell -ExecutionPolicy Bypass -File .\scripts\start-backend.ps1`

#### Cách B: chạy thủ công từng service

Mở **5 terminal riêng**, tất cả đứng ở **thư mục gốc** của project, mỗi terminal chạy một lệnh. Nên chạy theo
đúng thứ tự dưới đây (service con trước, orchestrator sau, gateway cuối):

```powershell
mvn -pl order-service     spring-boot:run   # terminal 1 -> :8081
mvn -pl inventory-service spring-boot:run   # terminal 2 -> :8082
mvn -pl payment-service   spring-boot:run   # terminal 3 -> :8083
mvn -pl saga-orchestrator spring-boot:run   # terminal 4 -> :8084
mvn -pl api-gateway       spring-boot:run   # terminal 5 -> :8080
```

Cách này chạy được trên cả macOS/Linux. Có thể thêm `-q` để bớt log của Maven.

> **Đừng chạy bằng `java -jar`**: `java` trên PATH có thể là JDK 8. `mvn spring-boot:run` luôn dùng
> `JAVA_HOME`, nên an toàn hơn.
>
> Phải đứng ở **thư mục gốc** khi chạy (vì dùng `-pl`). File database sẽ nằm ở `<service>/data/*.mv.db`.

#### Kiểm tra backend đã lên đủ chưa

```powershell
curl.exe http://localhost:8080/api/sagas/system/health
```

Kết quả là một danh sách 4 service (order, inventory, payment, orchestrator), mỗi service phải có
`"status":"ONLINE"`. Kiểm tra lẻ từng
service:

| Service           | URL kiểm tra                              |
| ----------------- | ----------------------------------------- |
| API Gateway       | http://localhost:8080/actuator/health     |
| Order Service     | http://localhost:8081/api/orders/health   |
| Inventory Service | http://localhost:8082/api/inventory/health|
| Payment Service   | http://localhost:8083/api/payments/health |
| Saga Orchestrator | http://localhost:8084/api/sagas/health    |

Bảng định tuyến của gateway: http://localhost:8080/actuator/gateway/routes

### 3.6. Chạy frontend

Backend phải đang chạy trước. Mở **một terminal mới**:

```powershell
.\scripts\start-frontend.ps1
```

hoặc làm tay:

```powershell
cd frontend
npm install        # chỉ cần lần đầu
npm run dev
```

Mở **http://localhost:3000**. Thanh trên cùng phải hiện **4 service online** (màu xanh). Nếu có service màu đỏ,
xem lại cửa sổ của service đó.

**Đổi địa chỉ gateway (không bắt buộc).** Mặc định frontend gọi `http://localhost:8080`. Muốn đổi thì:

```powershell
cd frontend
copy .env.example .env.local
# sửa NEXT_PUBLIC_API_URL trong .env.local, rồi chạy lại npm run dev
```

**Chạy bản production (không bắt buộc)** - nhanh và mượt hơn khi thuyết trình:

```powershell
cd frontend
npm run build
npm run start      # vẫn ở http://localhost:3000
```

### 3.7. Dừng hệ thống

```powershell
.\scripts\stop-backend.ps1
```

Script tắt mọi tiến trình đang nghe ở các port 3000, 8080-8084 (cả frontend). Nếu chạy theo Cách B, có thể
bấm `Ctrl + C` trong từng terminal.

### 3.8. Đưa dữ liệu về trạng thái ban đầu

- **Cách nhanh:** trên giao diện bấm **Reset data**. Lịch sử saga bị xoá và tồn kho mọi món về **100**.
  Hoặc gọi API: `curl.exe -X DELETE http://localhost:8080/api/sagas/reset`
- **Xoá sạch hoàn toàn:** tắt backend, xoá các file trong 4 thư mục `order-service/data`,
  `inventory-service/data`, `payment-service/data`, `saga-orchestrator/data`, rồi chạy lại. Database sẽ được tạo
  mới.

> **Trước giờ thuyết trình:** bấm **Reset data** một lần.

### 3.9. Tuỳ chỉnh cấu hình (không bắt buộc)

Mọi port, URL và độ trễ đều đọc từ biến môi trường, có giá trị mặc định trong `application.yml` của từng
service. Đặt biến trong terminal **trước khi** chạy service đó, ví dụ:

```powershell
$env:SAGA_STEP_DELAY_MS = "0"            # chạy saga hết tốc lực, không dừng giữa các bước
mvn -pl saga-orchestrator spring-boot:run
```

| Biến môi trường                       | Mặc định                | Ý nghĩa                                               |
| ------------------------------------- | ----------------------- | ----------------------------------------------------- |
| `API_GATEWAY_PORT`                    | `8080`                  | Port của gateway                                      |
| `ORDER_SERVICE_PORT`                  | `8081`                  | Port của order-service                                |
| `INVENTORY_SERVICE_PORT`              | `8082`                  | Port của inventory-service                            |
| `PAYMENT_SERVICE_PORT`                | `8083`                  | Port của payment-service                              |
| `SAGA_ORCHESTRATOR_PORT`              | `8084`                  | Port của orchestrator                                 |
| `ORDER_SERVICE_URL`, `INVENTORY_SERVICE_URL`, `PAYMENT_SERVICE_URL`, `SAGA_ORCHESTRATOR_URL` | `http://localhost:808x` | Địa chỉ service mà gateway/orchestrator gọi tới. Đổi port service nào thì phải đổi URL tương ứng ở gateway và orchestrator |
| `SAGA_STEP_DELAY_MS`                  | `700`                   | Thời gian dừng trước mỗi bước, để khán giả kịp theo dõi |
| `SAGA_COMPENSATION_RETRY_ATTEMPTS`    | `2`                     | Số lần thử mỗi compensation                           |
| `SAGA_CONNECT_TIMEOUT_MS` / `SAGA_READ_TIMEOUT_MS` | `3000` / `15000` | Timeout khi orchestrator gọi service con         |
| `ORDER_PROCESSING_DELAY_MS`, `INVENTORY_PROCESSING_DELAY_MS`, `PAYMENT_PROCESSING_DELAY_MS` | `150`, `150`, `250` | Độ trễ giả lập trong mỗi service |
| `*_DB_URL`, `*_DB_USERNAME`, `*_DB_PASSWORD` | H2 file, `sa`, rỗng | Kết nối database của từng service (`ORDER_`, `INVENTORY_`, `PAYMENT_`, `SAGA_`) |
| `NEXT_PUBLIC_API_URL` (frontend)      | `http://localhost:8080` | Địa chỉ gateway mà trình duyệt gọi tới                |

### 3.10. Tóm tắt nhanh (cho lần chạy thứ hai trở đi)

```powershell
.\scripts\start-backend.ps1      # đợi 5 cửa sổ hiện "Started"
.\scripts\start-frontend.ps1     # mở http://localhost:3000
# ... demo ...
.\scripts\stop-backend.ps1
```

---

## 4. Năm kịch bản demo

Chọn ở cột trái của Control Room rồi bấm **START SAGA**.

| #   | Kịch bản           | Kết quả mong đợi                                                                             |
| --- | ------------------ | -------------------------------------------------------------------------------------------- |
| 1   | Happy path         | 4 bước `COMPLETED`, đơn `CONFIRMED`, tồn kho giảm 1. Saga `COMPLETED`.                       |
| 2   | Inventory fails    | Bước 2 `FAILED`, chỉ `CANCEL_ORDER` chạy. Saga `COMPENSATED`.                                |
| 3   | Payment fails      | Bước 3 `FAILED`, `RELEASE_INVENTORY` rồi `CANCEL_ORDER`. Tồn kho về như cũ. `COMPENSATED`.   |
| 4   | Confirm fails      | Bước 4 `FAILED`, `REFUND_PAYMENT` → `RELEASE_INVENTORY` → `CANCEL_ORDER`. `COMPENSATED`.     |
| 5   | Compensation fails | Payment lỗi, rồi `RELEASE_INVENTORY` cũng lỗi (retry 2 lần). Saga `COMPENSATION_FAILED`, tồn kho **không** được trả lại, banner "inconsistent". |

Có thể tự bật lỗi cho từng bước ở phần **Failure simulation (custom)**, kể cả các bước compensation.

Giao diện gồm: **Saga Timeline** (từng ô đổi màu theo bước), **State Comparison** (số liệu đọc lại từ service
thật trước và sau), **Service Communication** (mọi lời gọi HTTP, method, endpoint, status, thời gian), **Live Log**,
**Saga History** (bấm để xem lại), và trang **How Saga Works** (bảng T/C lấy trực tiếp từ code của orchestrator).

## 5. API chính (qua gateway :8080)

| Method | Đường dẫn                  | Mô tả                                                                          |
| ------ | -------------------------- | ------------------------------------------------------------------------------ |
| POST   | `/api/sagas/orders`        | Bắt đầu saga (chạy nền, trả về `sagaId`). `?async=false` để chờ saga kết thúc. |
| GET    | `/api/sagas/{sagaId}`      | Toàn bộ lịch sử thực thi: steps, serviceCalls, logs, stateBefore/After         |
| GET    | `/api/sagas?limit=N`       | Danh sách saga gần nhất                                                        |
| GET    | `/api/sagas/menu`          | Món ăn và tồn kho hiện tại                                                     |
| GET    | `/api/sagas/system/health` | Trạng thái 4 service                                                           |
| GET    | `/api/sagas/definition`    | Định nghĩa saga lấy từ code đang chạy                                          |
| DELETE | `/api/sagas/reset`         | Xoá dữ liệu mọi service và nạp lại tồn kho                                     |

Món có sẵn: `BURGER-01`, `PHO-01`, `PIZZA-01`, `SUSHI-01`.

Ví dụ chạy saga và bật lỗi thanh toán (PowerShell, dùng `curl.exe`):

```powershell
curl.exe -X POST "http://localhost:8080/api/sagas/orders?async=false" `
  -H "Content-Type: application/json" `
  -d '{\"customerName\":\"A\",\"productId\":\"BURGER-01\",\"productName\":\"Burger\",\"quantity\":1,\"unitPrice\":85000,\"failureSimulation\":{\"failProcessPayment\":true}}'
```

Trên Git Bash / macOS / Linux:

```bash
curl -X POST "http://localhost:8080/api/sagas/orders?async=false" -H "Content-Type: application/json" \
  -d '{"customerName":"A","productId":"BURGER-01","productName":"Burger","quantity":1,"unitPrice":85000,
       "failureSimulation":{"failProcessPayment":true}}'
```

Các cờ lỗi: `failCreateOrder`, `failReserveInventory`, `failProcessPayment`, `failConfirmOrder`,
`failReleaseInventory`, `failRefundPayment`, `failCancelOrder`.

**Xem database của từng service** (để chứng minh dữ liệu nằm ở các DB riêng): mở
`http://localhost:808x/h2-console` (x = 1..4), điền:

| Service           | JDBC URL                       | User | Password |
| ----------------- | ------------------------------ | ---- | -------- |
| order-service     | `jdbc:h2:file:./data/orderdb`     | `sa` | (trống)  |
| inventory-service | `jdbc:h2:file:./data/inventorydb` | `sa` | (trống)  |
| payment-service   | `jdbc:h2:file:./data/paymentdb`   | `sa` | (trống)  |
| saga-orchestrator | `jdbc:h2:file:./data/sagadb`      | `sa` | (trống)  |

## 6. Kiểm thử

```powershell
mvn test                         # backend: 31 test (23 ở 3 service con + 8 ở orchestrator), không cần backend chạy
cd frontend; npm test            # 27 unit test, không cần backend
cd frontend; npm run check       # lint + typecheck + unit test + build (tắt dev server trước)
cd frontend; npm run e2e         # 8 test Playwright trên hệ thống thật (cần backend đang chạy)
```

- Test orchestrator dùng step giả để kiểm tra đúng **thứ tự** gọi forward/compensation, retry, và việc compensation
  lỗi vẫn không chặn các compensation còn lại.
- E2E dùng trình duyệt **Microsoft Edge** có sẵn trên Windows. Dùng Chrome: `$env:E2E_CHANNEL="chrome"`.
- Bộ e2e **reset dữ liệu demo**, đừng chạy ngay trước giờ thuyết trình.

## 7. Xử lý sự cố thường gặp

| Hiện tượng | Nguyên nhân | Cách xử lý |
| ---------- | ----------- | ---------- |
| `invalid target release: 21` hoặc `release version 21 not supported` | Maven đang dùng JDK cũ | Đặt `JAVA_HOME` về JDK 21 ([3.2](#32-cấu-hình-java_home-quan-trọng)), mở terminal mới, kiểm tra `mvn -version` |
| `'mvn' is not recognized...` | Chưa cài Maven hoặc chưa có trong PATH | Cài Maven, mở terminal mới |
| `running scripts is disabled on this system` | Execution Policy của PowerShell | `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` |
| `Port 808x was already in use` | Còn tiến trình cũ chạy ngầm | `.\scripts\stop-backend.ps1`, hoặc tìm tiến trình: `netstat -ano \| findstr :8081` rồi `taskkill /PID <pid> /F` |
| `Database may be already in use` (H2) | Service đó đang chạy ở cửa sổ khác | Tắt bản đang chạy rồi mới chạy lại |
| Frontend báo service **offline** / lỗi kết nối | Backend chưa lên xong hoặc gateway chưa chạy | Đợi đủ 5 cửa sổ "Started", mở http://localhost:8080/api/sagas/system/health |
| Health có 1 service `OFFLINE` | Service đó bị lỗi khi khởi động | Xem log trong cửa sổ của service đó |
| Lỗi CORS trên trình duyệt | Mở frontend bằng tên máy/IP khác `localhost` | Dùng `http://localhost:3000` (gateway chỉ cho phép `localhost` và `127.0.0.1`) |
| `npm install` lỗi / Next.js báo Node quá cũ | Node < 20.9 | Cài Node 22 LTS, xoá `frontend/node_modules` rồi `npm install` lại |
| `npm run build` lỗi khi `npm run dev` đang chạy | Hai tiến trình dùng chung thư mục `.next` | Tắt dev server trước khi build |
| Build Maven lần đầu rất lâu / lỗi tải thư viện | Mạng chậm hoặc bị chặn | Chạy lại `mvn clean install -DskipTests` khi có mạng ổn định |
| Dữ liệu lạ, tồn kho sai sau nhiều lần demo | Dữ liệu cũ còn trong H2 | Bấm **Reset data** hoặc xoá thư mục `data/` ([3.8](#38-đưa-dữ-liệu-về-trạng-thái-ban-đầu)) |

## 8. Giới hạn của bản demo

Nên nói thẳng khi thuyết trình:

- **Lỗi là giả lập** bằng cờ, không phải lỗi hạ tầng thật.
- **Gọi đồng bộ qua HTTP.** Hệ thống thật thường dùng message broker (Kafka/RabbitMQ). Orchestrator đã có sẵn
  điểm nối `SagaEventPublisher`, hiện chỉ ghi log.
- **Orchestrator chưa tự phục hồi** nếu nó bị sập giữa chừng: saga đang chạy dở sẽ không được tiếp tục tự động. Id
  của các bước đã commit được lưu trong SAGA DB nên có thể làm tiếp, nhưng phần recovery này chưa được cài đặt.
- **Không có isolation.** Người khác có thể thấy trạng thái trung gian (đã giữ hàng nhưng đơn chưa xác nhận).
- **Gateway chưa có test riêng**, và các service trả HTTP 500 thay vì 404 cho đường dẫn không tồn tại (không ảnh
  hưởng demo).
- Dữ liệu lưu trong H2 dạng file, chỉ phù hợp để demo.

## 9. Cấu trúc thư mục

```
pom.xml                  aggregator (Spring Boot 3.5, Java 21) - chỉ để build chung, các service vẫn độc lập
api-gateway/             định tuyến + CORS
order-service/           inventory-service/    payment-service/
saga-orchestrator/       saga/ (SagaStep, 4 step), service/ (interface) + service/impl/ (cài đặt), client/, entity/
                         (cả 4 service đều chia tầng service: interface XxxService + impl/XxxServiceImpl)
frontend/                Next.js + TypeScript + Tailwind (xem frontend/README.md)
scripts/                 start-backend.ps1, start-frontend.ps1, stop-backend.ps1
docs/                    presentation-notes.md
<service>/data/          file database H2 (tự sinh khi chạy, không đưa lên git)
```
