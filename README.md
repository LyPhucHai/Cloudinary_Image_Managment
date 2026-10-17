# Cloudinary Media Lab

Demo tích hợp **Cloudinary** vào một microservice **Spring Boot**, gồm 3 nội dung:

- **Image upload**: tải ảnh lên qua backend, hoặc gửi thẳng lên Cloudinary bằng chữ ký (signed upload).
- **Image optimization**: tối ưu và biến đổi ảnh chỉ bằng cách đổi URL (`f_auto`, `q_auto`, resize, crop...).
- **CDN delivery**: ảnh được phát từ CDN của Cloudinary, không đi qua server của bạn.

Project có trang web demo, Swagger UI, Postman collection và unit test.

## Yêu cầu

- **Docker Desktop** (cách chạy đơn giản nhất, không cần cài Java hay Maven).
- Tài khoản **Cloudinary** miễn phí tại https://cloudinary.com.

## Cài đặt và chạy

### 1. Tải project

```bash
git clone <link-repo-cua-ban>
cd Cloudinary_Image_Managment
```

Hoặc tải file ZIP từ GitHub rồi giải nén.

### 2. Lấy khóa Cloudinary

1. Đăng nhập https://console.cloudinary.com.
2. Vào **Settings → API Keys**. **Cloud name** hiện ở đầu trang.
3. Bấm **Generate New API Key**, đặt tên (ví dụ `soa-demo`), chọn **Standard Development**.
4. Copy **API key** và **API secret** ngay khi hiện ra (secret thường chỉ hiện đầy đủ một lần).

> `API secret` giống mật khẩu: không đưa lên GitHub, không gửi cho người khác.

### 3. Tạo file `.env`

Sao chép file mẫu ra `.env` (đừng sửa trực tiếp `.env.example`):

```powershell
# Windows PowerShell
Copy-Item .env.example .env
```
```bash
# Linux / macOS
cp .env.example .env
```

Mở `.env` và điền giá trị thật (không có dấu cách quanh dấu `=`, không dùng ngoặc kép):

```
CLOUDINARY_CLOUD_NAME=ten_cloud_cua_ban
CLOUDINARY_API_KEY=123456789012345
CLOUDINARY_API_SECRET=chuoi_secret_cua_ban
```

### 4. Chạy

Mở Docker Desktop, chờ báo "Engine running", rồi:

```bash
docker compose up --build
```

Lần đầu mất vài phút. Khi thấy dòng `Started MediaServiceApplication` là xong.

### 5. Sử dụng

| Việc | Địa chỉ |
|---|---|
| Trang demo | http://localhost:8090 |
| Swagger UI | http://localhost:8090/swagger-ui.html |

Trên trang demo: chọn một ảnh lớn → **Tải lên** → thử **Đo dung lượng**, khu **Transformation** và panel **CDN delivery**.

Dừng ứng dụng: nhấn `Ctrl+C`, hoặc chạy `docker compose down`.

> Dữ liệu danh sách ảnh lưu trong bộ nhớ (H2) nên mất khi khởi động lại. Ảnh trên Cloudinary vẫn còn.

## Chạy không dùng Docker (tùy chọn)

Cần **JDK 17+** và **Maven 3.9+**. Chạy script có sẵn, script tự nạp file `.env`:

```bash
./run-local.sh          # Linux / macOS / Git Bash
```
```powershell
.\run-local.ps1         # Windows PowerShell
```

Cách này dùng cổng **8080**: mở http://localhost:8080.

## Chạy test

```bash
cd media-service
mvn test
```

## Lỗi thường gặp

| Triệu chứng | Cách xử lý |
|---|---|
| `Could not resolve placeholder 'CLOUDINARY_...'` | Chưa có file `.env`, hoặc `.env` không nằm cạnh `docker-compose.yml` |
| Upload báo `Invalid api_key` / `Invalid Signature` | Sai key hoặc thừa khoảng trắng trong `.env`. Copy lại từ Cloudinary |
| Vào `localhost:8090` thấy trang lạ (ví dụ nginx) | Cổng đang bị ứng dụng khác chiếm. Đổi `"8090:8080"` trong `docker-compose.yml` thành cổng khác, ví dụ `"8095:8080"` |
| Docker build đứng rất lâu | Kiểm tra ổ đĩa còn trống ít nhất 10GB, chạy `wsl --shutdown` rồi mở lại Docker Desktop |
| Sửa `index.html` hoặc code mà không thấy thay đổi | Chạy lại `docker compose up --build -d`, rồi nhấn `Ctrl+F5` trên trình duyệt |

## Cấu trúc thư mục

```
├── docker-compose.yml
├── .env.example              # file mẫu, copy thành .env
├── run-local.sh / .ps1       # chạy không dùng Docker
├── media-service/            # Spring Boot (Java 17)
│   └── src/main/resources/static/index.html   # trang demo
├── postman/                  # Postman collection
└── docs/                     # kịch bản demo, bảng tham số transformation
```
