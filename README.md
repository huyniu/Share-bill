# Share Bill

App chia tiền cho nhóm, hỗ trợ **nhiều người cùng trả** một hóa đơn và **tối giản số lần chuyển khoản** để cả nhóm hết nợ.

- Backend: Java 17, Spring Boot 4, Spring Data JPA; H2 ở máy tính, PostgreSQL (Neon) trên mạng
- App: Expo + React Native Web trong [mobile/](mobile/README.md), dùng trên iPhone qua Safari
- **Đưa lên mạng miễn phí: xem [DEPLOY.md](DEPLOY.md)**
- Mọi số tiền là **số nguyên, đơn vị đồng** (không dùng số thực nên không có sai số)

## Chạy ở máy tính

```bash
cd mobile && npm install && npm run build:web && cd ..   # build giao diện (làm lại khi sửa app)
./mvnw spring-boot:run      # Windows PowerShell: .\mvnw.cmd spring-boot:run
./mvnw test                 # chạy test (gồm cả test trên PostgreSQL nhúng)
```

Ở máy tính không cần mật khẩu. Muốn thử màn hình mật khẩu thì đặt biến môi trường `APP_PASSWORD` trước khi chạy.

Server chạy tại `http://localhost:8686`. Nó vừa phục vụ API, vừa phục vụ app web
(bản build của [mobile/](mobile/README.md), nằm trong `src/main/resources/static`).
Trên iPhone, mở `http://<IP máy tính>:8686` bằng Safari rồi chọn **Thêm vào MH chính**.

Xem database tại `http://localhost:8686/h2-console`
(JDBC URL: `jdbc:h2:file:./data/sharebill`, user `sa`, không có mật khẩu).

Trong IntelliJ: mở thư mục này, chạy `ShareBillApplication`.

## Cách tính

1. Mỗi khoản chi lưu **ai đã trả bao nhiêu** và **ai phải chịu bao nhiêu**.
2. Số dư của mỗi người = đã trả − phải chịu + đã chuyển trả nợ − đã nhận trả nợ.
   Dương: còn được nhận lại; âm: còn phải trả.
3. `DebtSimplifier` ghép người nợ nhiều nhất với người được nhận nhiều nhất cho đến khi hết.
   Với n người thì cần tối đa n − 1 lần chuyển khoản.

Kiểu chia (`splitType`):

| Kiểu     | `splitAmong[].value` | Ý nghĩa |
|----------|----------------------|---------|
| `EQUAL`  | bỏ qua               | Chia đều. Để trống `splitAmong` thì chia cho cả nhóm |
| `EXACT`  | số tiền              | Mỗi người chịu đúng số tiền ghi, tổng phải bằng hóa đơn |
| `SHARES` | số phần              | Chia theo tỉ lệ, ví dụ A 2 phần, B 1 phần |

Nếu chia không hết (ví dụ 100.000đ / 3 người), từng đồng dư được chia cho người có phần lẻ lớn nhất,
nên tổng các phần luôn khớp đúng số tiền hóa đơn.

## API

| Method | Đường dẫn | Mô tả |
|--------|-----------|-------|
| POST   | `/api/groups` | Tạo nhóm `{"name","members":[...]}` |
| GET    | `/api/groups` | Danh sách nhóm |
| GET    | `/api/groups/{id}` | Chi tiết nhóm, thành viên, tổng chi |
| DELETE | `/api/groups/{id}` | Xóa nhóm |
| POST   | `/api/groups/{id}/members` | Thêm thành viên `{"name"}` |
| DELETE | `/api/groups/{id}/members/{memberId}` | Xóa thành viên (chỉ khi chưa có giao dịch) |
| POST   | `/api/groups/{id}/expenses` | Thêm khoản chi |
| GET    | `/api/groups/{id}/expenses` | Danh sách khoản chi |
| DELETE | `/api/groups/{id}/expenses/{expenseId}` | Xóa khoản chi |
| GET    | `/api/groups/{id}/balances` | Số dư từng người |
| GET    | `/api/groups/{id}/settlements` | **Ai cần chuyển cho ai bao nhiêu** |
| POST   | `/api/groups/{id}/payments` | Đánh dấu đã trả `{"fromMemberId","toMemberId","amount"}` |
| GET    | `/api/groups/{id}/payments` | Lịch sử trả nợ |
| DELETE | `/api/groups/{id}/payments/{paymentId}` | Hủy một lần trả |

Ví dụ thêm khoản chi: An trả 800k, Bình trả 400k, chia đều cho cả nhóm:

```json
POST /api/groups/1/expenses
{
  "description": "Lẩu",
  "amount": 1200000,
  "paidBy": [
    { "memberId": 1, "amount": 800000 },
    { "memberId": 2, "amount": 400000 }
  ],
  "splitType": "EQUAL"
}
```

Nếu chỉ một người trả toàn bộ, có thể bỏ `amount` trong `paidBy`: `"paidBy": [{ "memberId": 3 }]`.

Lỗi trả về theo chuẩn RFC 9457, ví dụ:
`{"status":400,"detail":"Tổng tiền những người trả (90000) phải bằng số tiền hóa đơn (100000)"}`

## Cấu trúc

```
src/main/java/com/sharebill/
├── core/        Thuật toán thuần Java: SplitCalculator, DebtSimplifier
├── model/       Entity JPA: BillGroup, Member, Expense, Payment
├── repository/  Spring Data repository
├── service/     GroupService: nghiệp vụ, kiểm tra dữ liệu
└── web/         REST controller, DTO, xử lý lỗi, CORS, mật khẩu, phục vụ app web
```

## Biến môi trường

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `PORT` | `8686` | Cổng server (Render tự đặt) |
| `DATABASE_URL` | (trống: dùng H2) | Chuỗi kết nối Neon `postgresql://...` |
| `APP_PASSWORD` | (trống: không cần mật khẩu) | Mật khẩu chung để vào app |
| `H2_CONSOLE` | `true` | Bật trang /h2-console |
| `CORS_ORIGINS` | `http://localhost:8081,...` | Địa chỉ web khác được gọi API (dev) |

## Việc tiếp theo

- Tạo mã VietQR cho từng lần chuyển khoản
- Mỗi nhóm một link mời riêng thay cho mật khẩu chung
