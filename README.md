# Cloudinary Media Service – Demo Seminar SOA (Topic 2, Group 1)

Demo tích hợp **Cloudinary** vào một microservice **Spring Boot**: upload ảnh, tối ưu ảnh, phân phối qua CDN.

Sau khi làm theo README này, bạn có:

- Một **Media Service** chạy ở `http://localhost:8080` với REST API đầy đủ (upload, danh sách, biến thể, xóa).
- **Hai mô hình upload**: A (qua backend) và B (signed upload, client gửi thẳng lên Cloudinary).
- Một **trang web demo** có bảng so sánh dung lượng và "sân chơi" transformation.
- **Swagger UI**, **Postman collection**, **unit test**, **Dockerfile** và **docker-compose**.

---

## 1. Cloudinary giải quyết bài toán gì?

| Vấn đề khi tự lưu ảnh | Cloudinary xử lý |
|---|---|
| Ảnh gốc 3–10MB làm trang chậm | `f_auto,q_auto` tự chọn định dạng (WebP/AVIF) và nén |
| Phải tự viết code resize/crop cho từng thiết bị | Resize, crop, bo tròn… bằng cách đổi URL |
| Server tốn dung lượng và băng thông | Ảnh nằm trên cloud, server chỉ giữ `publicId` |
| Người dùng ở xa tải chậm | CDN toàn cầu cache ảnh ở edge gần người dùng |

---

## 2. Kiến trúc

```
Mô hình A: upload qua backend
  Browser --multipart--> Media Service --upload--> Cloudinary
                              |  lưu publicId + url vào DB
  Browser <-- JSON (url) -----+
  Browser --------- tải ảnh (đã tối ưu) --------> Cloudinary CDN

Mô hình B: signed upload trực tiếp
  1. Browser -> Media Service : GET /api/images/signature
  2. Media Service -> Browser : chữ ký + timestamp + folder (api_secret KHÔNG gửi đi)
  3. Browser -> Cloudinary    : upload file kèm chữ ký
  4. Browser -> Media Service : POST /api/images/register {publicId}
  5. Media Service -> Cloudinary : hỏi lại metadata (đáng tin cậy) rồi lưu DB
```

Transformation nằm ngay trong URL:

```
https://res.cloudinary.com/<cloud>/image/upload/c_fill,g_auto,w_400,h_400,f_auto,q_auto/<public_id>
```

Ảnh gốc chỉ lưu một lần. Mỗi biến thể được tạo khi có người yêu cầu lần đầu, sau đó cache ở CDN.

---

## 3. Cấu trúc project

```
cloudinary-demo/
├── README.md
├── .env.example                  # mẫu biến môi trường (copy thành .env)
├── .gitignore                    # đã loại trừ .env
├── docker-compose.yml
├── run-local.sh / run-local.ps1  # chạy nhanh trên máy, tự nạp .env
├── media-service/                # Spring Boot 3.3, Java 17
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/java/com/soa/media/
│       │   ├── MediaServiceApplication.java
│       │   ├── config/CloudinaryConfig.java      # bean Cloudinary
│       │   ├── controller/ImageController.java   # REST API
│       │   ├── service/ImageService.java         # toàn bộ logic Cloudinary
│       │   ├── entity/ImageAsset.java            # bảng image_assets
│       │   ├── repository/ImageAssetRepository.java
│       │   ├── dto/                              # ImageResponse, SignatureResponse, RegisterRequest
│       │   └── exception/                        # xử lý lỗi chuẩn RFC 7807
│       ├── main/resources/application.yml
│       ├── main/resources/static/index.html      # trang demo
│       └── test/java/.../ImageServiceTest.java
├── postman/Cloudinary.postman_collection.json
└── docs/
    ├── DEMO-SCRIPT.md                  # kịch bản demo 12 phút + câu hỏi Q&A
    ├── cheatsheet-transformations.md   # bảng tham số c_, w_, h_, q_, f_, g_, e_
    └── integration-other-stacks.md     # mẫu FastAPI, Node.js, Feign client
```

---

## 4. Yêu cầu cài đặt

| Công cụ | Phiên bản | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9+ | Hoặc dùng Docker, không cần Maven |
| Docker Desktop | tuỳ chọn | Cho cách chạy bằng Docker |
| Tài khoản Cloudinary | gói Free | Đăng ký tại https://cloudinary.com |
| Postman | tuỳ chọn | Để import collection |

---

## 5. Hướng dẫn từng bước

### Bước 1. Lấy khóa Cloudinary

1. Đăng ký/đăng nhập https://cloudinary.com.
2. Vào **Dashboard** (hoặc **Settings → API Keys**).
3. Ghi lại ba giá trị: **Cloud name**, **API key**, **API secret**.

> `API secret` giống mật khẩu. Không đưa lên GitHub, không gửi cho frontend.

### Bước 2. Tạo file `.env`

Linux/macOS/Git Bash:

```bash
cp .env.example .env
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Mở `.env` và điền giá trị thật:

```
CLOUDINARY_CLOUD_NAME=abc123
CLOUDINARY_API_KEY=123456789012345
CLOUDINARY_API_SECRET=xxxxxxxxxxxxxxxxxxxx
```

Không đặt dấu cách quanh dấu `=`.

### Bước 3. Chạy service (chọn một cách)

**Cách 1: Maven trên máy (có script nạp `.env` sẵn)**

```bash
./run-local.sh          # Linux/macOS/Git Bash
```
```powershell
.\run-local.ps1         # Windows PowerShell (nếu bị chặn script: Set-ExecutionPolicy -Scope Process Bypass)
```

**Cách 2: Docker Compose (không cần cài Maven/JDK)**

```bash
docker compose up --build
```

**Cách 3: IntelliJ/VS Code**

Mở thư mục `media-service`, thêm 3 biến môi trường ở trên vào Run Configuration, chạy `MediaServiceApplication`.

Khi thấy dòng `Started MediaServiceApplication` là xong. Lần đầu Maven cần tải thư viện nên chờ vài phút.

### Bước 4. Thử ngay

| Việc | Địa chỉ |
|---|---|
| Trang demo (upload, so sánh, sân chơi) | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Danh sách ảnh (JSON) | http://localhost:8080/api/images |

Trên trang demo: chọn một ảnh lớn → **Tải lên** → bấm **Đo dung lượng và thời gian** để thấy phần trăm dung lượng giảm.

Thử bằng curl:

```bash
curl -F "file=@./photo.jpg" -F "folder=soa/demo" http://localhost:8080/api/images
```

Kết quả mẫu:

```json
{
  "id": 1,
  "publicId": "soa/demo/abcd1234",
  "url": "https://res.cloudinary.com/<cloud>/image/upload/v1/soa/demo/abcd1234.jpg",
  "format": "jpg", "width": 4000, "height": 3000, "bytes": 4823011,
  "originalFilename": "photo.jpg", "createdAt": "2026-10-12T08:00:00Z"
}
```

### Bước 5. Chạy test

```bash
cd media-service
mvn test
```

Test không cần mạng hay key thật: kiểm tra URL transformation, chữ ký upload, validate loại file và tên folder.

### Bước 6. Postman

1. Postman → **Import** → chọn `postman/Cloudinary.postman_collection.json`.
2. Ở request **1. Upload ảnh**, tab **Body → file**, chọn một ảnh.
3. Chạy lần lượt 1 → 9. Request 1 tự lưu `imageId` và `publicId` cho các request sau.

---

## 6. API

| Method | Đường dẫn | Mô tả |
|---|---|---|
| POST | `/api/images` | Upload qua backend (`file`, `folder` tuỳ chọn). Trả 201 |
| GET | `/api/images` | Danh sách ảnh, mới nhất trước |
| GET | `/api/images/{id}` | Chi tiết một ảnh |
| GET | `/api/images/{id}/variants` | Các URL thumbnail, card, banner, avatar, grayscale, blur-placeholder |
| GET | `/api/images/optimized?publicId=&w=&h=&crop=` | URL tối ưu theo kích thước tuỳ ý |
| GET | `/api/images/signature` | Mô hình B: xin chữ ký upload trực tiếp |
| POST | `/api/images/register` | Mô hình B: đăng ký ảnh vừa upload trực tiếp |
| DELETE | `/api/images/{id}` | Xóa ảnh trên Cloudinary rồi xóa DB |

Lỗi trả về theo chuẩn ProblemDetail: `400` (file/folder không hợp lệ), `404` (không có ảnh), `413` (quá 5MB), `502` (Cloudinary lỗi).

---

## 7. Giải thích các đoạn code quan trọng

**Bean Cloudinary** (`CloudinaryConfig`): đọc ba biến môi trường, bật `secure=true` để luôn dùng HTTPS.

**Upload** (`ImageService.upload`):

```java
Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
        "folder", targetFolder, "resource_type", "image",
        "unique_filename", true, "overwrite", false));
```

Trước đó có kiểm tra loại file (whitelist) và tên folder (regex) để tránh upload sai hoặc path lạ.

**Tối ưu ảnh** (`ImageService.optimizedUrl`):

```java
cloudinary.url().transformation(new Transformation()
        .width(w).height(h).crop("fill").gravity("auto")
        .quality("auto").fetchFormat("auto"))
    .secure(true).generate(publicId);
```

Hàm này chỉ ghép chuỗi URL, không gọi mạng và không tốn băng thông của backend.

**Signed upload** (`ImageService.createUploadSignature`): backend ký `timestamp`, `folder`, `allowed_formats` bằng `api_secret`. Client phải gửi đúng các giá trị đã ký, nếu sửa Cloudinary sẽ từ chối. Backend không bao giờ gửi `api_secret` xuống client.

**Register** (`ImageService.register`): thay vì tin dữ liệu client báo, backend gọi lại Cloudinary Admin API để lấy kích thước, định dạng, dung lượng thật.

---

## 8. Tích hợp vào dự án của bạn

**Cách 1: dùng như một service riêng (đúng tinh thần SOA).**
Chạy `media-service` thành container riêng. Các service khác (Product, User…) gọi REST `POST /api/images` (qua API Gateway hoặc Feign) và chỉ lưu `publicId` + `url`. Mẫu Feign nằm trong `docs/integration-other-stacks.md`.

**Cách 2: copy vào service có sẵn.** Sao chép 5 file sau và thêm dependency + cấu hình:

1. `config/CloudinaryConfig.java`
2. `service/ImageService.java` (bỏ phần repository nếu bạn tự lưu DB)
3. `controller/ImageController.java`
4. Dependency `cloudinary-http45` trong `pom.xml`
5. Khối `cloudinary:` trong `application.yml` + 3 biến môi trường

**Cách 3: dự án không dùng Java.** Xem mẫu FastAPI và Node.js trong `docs/integration-other-stacks.md`. Cloudinary có SDK cho Python, Node, PHP, .NET, Ruby…

**Lưu DB thế nào:** trong bảng `products` chỉ cần cột `image_public_id` (và `image_url`). Hiển thị ảnh thì tạo URL từ `publicId` với kích thước cần dùng.

### Dự án nào phù hợp?

E-commerce (ảnh sản phẩm), mạng xã hội (avatar, bài đăng), đặt phòng/du lịch, quản lý sự kiện, tin tức/blog, food delivery, tuyển dụng (logo, avatar ứng viên).

---

## 9. Best practices

- `api_secret` chỉ ở backend, nạp bằng biến môi trường hoặc secret manager. Không commit `.env`.
- Luôn validate loại file và dung lượng ở backend (demo đã làm).
- Dùng `f_auto,q_auto` cho mọi ảnh hiển thị.
- Lưu `publicId`, sinh URL khi cần.
- Chia folder theo mục đích: `soa/users`, `soa/products`.
- Frontend dùng `loading="lazy"` và `srcset` (xem `docs/cheatsheet-transformations.md`).
- Xóa ảnh trên Cloudinary khi xóa bản ghi trong DB.
- Dữ liệu nhạy cảm: dùng chế độ upload `authenticated`/`private`.

## 10. Hạn chế

- Gói Free tính theo credit (lưu trữ, băng thông, transformation) và có giới hạn kích thước file. Hãy xem giới hạn hiện hành tại trang pricing của Cloudinary.
- Phụ thuộc nhà cung cấp bên thứ ba (vendor lock-in).
- Upload qua backend (mô hình A) làm file đi qua server, với file lớn nên chọn mô hình B.
- Mỗi biến thể transformation mới tốn credit ở lần tạo đầu tiên.
- Demo dùng H2 in-memory nên dữ liệu DB mất khi restart. Ảnh trên Cloudinary vẫn còn, bạn có thể xóa trong Media Library.

---

## 11. Xử lý lỗi thường gặp

| Triệu chứng | Nguyên nhân và cách sửa |
|---|---|
| App không chạy, báo `Could not resolve placeholder 'CLOUDINARY_CLOUD_NAME'` | Chưa nạp biến môi trường. Dùng `run-local` hoặc thêm biến vào Run Configuration |
| Upload trả `502` kèm `Invalid api_key` / `Invalid Signature` | Sai key hoặc có khoảng trắng thừa trong `.env`. Copy lại từ Dashboard |
| `413` khi upload | File lớn hơn 5MB. Đổi `spring.servlet.multipart.max-file-size` trong `application.yml` |
| Mô hình B báo lỗi từ Cloudinary | Đồng hồ máy lệch nhiều so với thực tế, hoặc dùng sai `folder`/`allowed_formats` so với chữ ký |
| Nút "Đo dung lượng" báo trình duyệt chặn | Mở DevTools → Network để xem Size và Time của ảnh |
| `docker compose` không đọc được key | Chắc chắn file `.env` nằm cùng thư mục `docker-compose.yml` |
| Cổng 8080 bị chiếm | Đổi `server.port` hoặc đổi mapping `"8081:8080"` trong `docker-compose.yml` |
| Maven không tải được thư viện | Kiểm tra mạng/proxy. Kiểm tra phiên bản `cloudinary-http45` mới nhất trên Maven Central |

---

## 12. Mở rộng gợi ý

- Thêm Spring Security/JWT cho API upload và xóa.
- Đổi H2 sang MySQL/PostgreSQL (thêm service vào `docker-compose.yml`).
- Upload nhiều ảnh bằng hàng đợi RabbitMQ (liên hệ Topic 7).
- Dùng upload preset để giới hạn định dạng/kích thước ở phía Cloudinary.
- Webhook thông báo của Cloudinary để xử lý bất đồng bộ.

## 13. Tài liệu tham khảo

- Cloudinary Java SDK: https://cloudinary.com/documentation/java_integration
- Transformation URL: https://cloudinary.com/documentation/image_transformations
- Upload API và chữ ký: https://cloudinary.com/documentation/authentication_signatures
