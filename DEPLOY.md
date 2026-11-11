# Đưa Share Bill lên mạng (miễn phí)

Sau khi làm xong các bước dưới đây, app sẽ có địa chỉ dạng `https://share-bill-xxxx.onrender.com`. Bạn có thể mở ở bất cứ đâu,
và **mỗi lần push code lên GitHub, app tự cập nhật** sau khoảng 5–10 phút.

| Dịch vụ | Dùng để | Giá |
|---|---|---|
| GitHub | Lưu code | Miễn phí |
| Neon | Database PostgreSQL | Miễn phí (1 GB) |
| Render | Chạy app | Miễn phí (ngủ sau 15 phút không dùng, mở lại phải chờ khoảng 1 phút) |

> Chính sách miễn phí có thể thay đổi. Hãy xem lại trang giá của Neon và Render trước khi đăng ký.

---

## Bước 1: Đẩy code lên GitHub

1. Tạo tài khoản tại https://github.com (nếu chưa có).
2. Bấm **New repository**:
   - Repository name: `share-bill`
   - Chọn **Private** (giữ code và email trong commit ở chế độ riêng tư)
   - **Không** tích "Add a README", vì code đã có sẵn
   - Bấm **Create repository**
3. Mở terminal ở thư mục `Share-bill` và chạy 2 lệnh (thay `TEN_GITHUB` bằng tên tài khoản của bạn):
   ```bash
   git remote add origin https://github.com/TEN_GITHUB/share-bill.git
   ```
   ```bash
   git push -u origin main
   ```
   Lần đầu, Windows sẽ mở trình duyệt để bạn đăng nhập GitHub. Đăng nhập xong, quay lại terminal là xong.

## Bước 2: Tạo database trên Neon

1. Vào https://neon.com, đăng ký bằng tài khoản GitHub.
2. Tạo project mới:
   - Project name: `share-bill`
   - Region: **AWS Asia Pacific (Singapore)**, gần Việt Nam và cùng khu vực với Render
3. Ở trang **Connection Details** (hoặc nút **Connect**), copy **Connection string**. Chuỗi có dạng:
   ```
   postgresql://neondb_owner:xxxxxxxx@ep-xxx-xxx.ap-southeast-1.aws.neon.tech/neondb?sslmode=require
   ```
   Giữ chuỗi này cho Bước 3, và **không gửi cho ai**.

## Bước 3: Chạy app trên Render

1. Vào https://render.com, đăng ký bằng tài khoản GitHub.
2. Bấm **New → Blueprint**, cho phép Render truy cập repo `share-bill`, rồi chọn repo đó.
3. Render đọc file `render.yaml` và hỏi 2 giá trị:
   - **DATABASE_URL**: dán chuỗi kết nối Neon ở Bước 2
   - **APP_PASSWORD**: tự đặt một mật khẩu (sau này gửi cho bạn bè để vào app)
4. Bấm **Apply**. Lần build đầu mất khoảng 5–10 phút. Bạn có thể bấm vào service `share-bill` để xem log.
5. Khi trạng thái chuyển sang **Live**, copy địa chỉ ở đầu trang, dạng `https://share-bill-xxxx.onrender.com`.

## Bước 4: Cài lên iPhone

1. Mở địa chỉ ở Bước 3 bằng **Safari**.
2. Nhập mật khẩu `APP_PASSWORD`.
3. Bấm **Chia sẻ** → **Thêm vào MH chính** → **Thêm**.

Gửi địa chỉ và mật khẩu cho bạn bè để họ cũng xem được ai nợ ai.

---

## Cập nhật app sau này

Sửa code, kiểm tra ở máy, rồi:

```bash
git add -A
```
```bash
git commit -m "Mô tả thay đổi"
```
```bash
git push
```

Render tự build và chạy bản mới. Mở lại app trên iPhone là thấy.

## Khi gặp sự cố

- **App quay vòng lâu khi mở:** server miễn phí đang ngủ, chờ khoảng 1 phút là vào được.
- **Build lỗi trên Render:** vào service `share-bill` → **Events/Logs** để xem lỗi.
  Trước khi push, nên chạy `.\mvnw.cmd test` và `cd mobile && npx tsc --noEmit` ở máy để bắt lỗi sớm.
- **Đổi mật khẩu:** Render → service `share-bill` → **Environment** → sửa `APP_PASSWORD` → **Save**.
  Mọi người sẽ được yêu cầu nhập mật khẩu mới.
- **Xem hoặc sửa dữ liệu:** dùng **SQL Editor** trên trang Neon.
