# Share Bill – App (Expo, chạy dạng web app trên iPhone)

> Muốn dùng ở mọi nơi (không cần bật máy tính)? Xem [../DEPLOY.md](../DEPLOY.md).

App viết bằng Expo SDK 58 + Expo Router + TypeScript. Bản web được build vào backend Spring Boot,
nên iPhone chỉ cần mở bằng **Safari** rồi **Thêm vào MH chính**. Không cần App Store hay Expo Go.

## Dùng trên iPhone

1. Build app web vào backend (lần đầu và mỗi khi sửa code app). Chạy trong thư mục `Share-bill/mobile`:
   ```bash
   npm run build:web
   ```
   Lệnh này xuất file vào `../src/main/resources/static`.
2. Chạy backend trong thư mục `Share-bill`:
   ```bash
   .\mvnw.cmd spring-boot:run
   ```
3. Trên iPhone (cùng Wi-Fi với máy tính), mở **Safari** và vào `http://<IP máy tính>:8686`,
   ví dụ `http://192.168.0.101:8686`. Xem IP bằng lệnh `ipconfig` (dòng *IPv4 Address*).
4. Bấm nút **Chia sẻ** (hình vuông có mũi tên lên), chọn **Thêm vào MH chính**, rồi bấm **Thêm**.
5. Từ giờ mở app **Share Bill** từ màn hình chính. App chạy toàn màn hình, không còn thanh địa chỉ.

### Lưu ý

- App chỉ dùng được khi **máy tính đang bật, backend đang chạy** và iPhone **cùng Wi-Fi**.
- Nếu IP máy tính thay đổi (router cấp IP mới), icon trên màn hình chính sẽ không vào được nữa.
  Khi đó cần xóa icon cũ rồi thêm lại với IP mới. Cách tránh: đặt **IP cố định** (DHCP reservation) cho máy tính trong cài đặt router.
- Nếu Safari không vào được: Windows Firewall có thể đang chặn Java. Khi Windows hỏi thì bấm **Allow** cho mạng Private,
  hoặc vào *Windows Security → Firewall → Allow an app through firewall* và cho phép Java.
- Dữ liệu lưu trên máy tính, trong `Share-bill/data/`.

## Phát triển

Khi sửa code, chạy chế độ dev để thấy thay đổi ngay mà không cần build lại:

```bash
npx expo start --web     # mở http://localhost:8081, gọi API ở cổng 8686
```

Kiểm tra code:

```bash
npx tsc --noEmit
npx expo lint
```

Sửa xong thì chạy `npm run build:web` để cập nhật bản dùng trên iPhone. App trên màn hình chính sẽ tự lấy bản mới khi mở lại.

Muốn thử qua Expo Go thay vì Safari: chạy `npx expo start`, rồi quét mã QR bằng Camera của iPhone.

## Cấu trúc

```
public/                         index.html (meta iOS), manifest.json, icon
src/
├── app/                        Màn hình (Expo Router: mỗi file là một màn hình)
│   ├── _layout.tsx             Stack navigator
│   ├── index.tsx               Danh sách nhóm
│   ├── new-group.tsx           Tạo nhóm
│   └── group/[id]/
│       ├── index.tsx           Chi tiết nhóm: Ai trả ai / Số dư / Khoản chi
│       └── add-expense.tsx     Thêm khoản chi (nhiều người trả, 3 kiểu chia)
├── components/ui.tsx           Button, Card, Input, Chip, Segmented
└── lib/
    ├── api.ts                  Gọi REST API + kiểu dữ liệu
    └── format.ts               Định dạng tiền (1.200.000đ), đọc "350k", "1,2tr"
```
