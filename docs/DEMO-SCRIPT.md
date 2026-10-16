# Kịch bản demo live (12 phút)

Chuẩn bị trước: backend đang chạy ở `http://localhost:8080`, có sẵn 1 ảnh 3–5MB (ảnh phong cảnh hoặc chân dung), mở sẵn Cloudinary Media Library, Swagger UI và DevTools → Network.

| Phút | Việc làm | Điều cần nói |
|---|---|---|
| 0–1 | Mở `http://localhost:8080` | Đây là Media Service, tách riêng khỏi các service khác. |
| 1–3 | Upload ảnh bằng chế độ **A** | Ảnh đi qua backend, backend kiểm tra loại file rồi gửi lên Cloudinary và lưu `publicId` vào DB. |
| 3–4 | Mở **Media Library** trên Cloudinary | Ảnh gốc nằm trên cloud ở folder `soa/demo`, server không giữ file. |
| 4–6 | Bấm **Đo dung lượng và thời gian** | So sánh ảnh gốc và `f_auto,q_auto`: dung lượng giảm mạnh, đây là bài toán trang web chậm. |
| 6–8 | Sân chơi transformation: kéo width/height, bật đen trắng, bo tròn | Mọi thứ chỉ là đổi chữ trong URL. Một ảnh gốc, vô số biến thể, tạo theo yêu cầu và cache ở CDN. |
| 8–9 | DevTools → Network, tải lại ảnh | Chỉ vào header `cache-control`, `server-timing` để chứng minh CDN. |
| 9–11 | Upload bằng chế độ **B** | Backend chỉ ký chữ ký, file đi thẳng lên Cloudinary. Server không phải gánh băng thông. |
| 11–12 | Bấm **Xóa ảnh**, kiểm tra Media Library | Xóa cả Cloudinary và DB để không có ảnh mồ côi. |

## Lệnh curl dự phòng (nếu giao diện lỗi)

```bash
# Upload
curl -F "file=@./photo.jpg" -F "folder=soa/demo" http://localhost:8080/api/images

# Danh sách
curl http://localhost:8080/api/images

# Biến thể
curl http://localhost:8080/api/images/1/variants

# URL tối ưu
curl "http://localhost:8080/api/images/optimized?publicId=soa/demo/abc&w=400&h=300&crop=fill"

# Chữ ký cho upload trực tiếp
curl http://localhost:8080/api/images/signature

# Xóa
curl -X DELETE http://localhost:8080/api/images/1

# Thử file sai định dạng (kỳ vọng 400)
curl -F "file=@./doc.pdf" http://localhost:8080/api/images
```

## Câu hỏi thầy có thể hỏi

- **Vì sao lưu `publicId` thay vì URL?** Có `publicId` thì sinh được mọi biến thể sau này, đổi cloud hay domain cũng không phải sửa dữ liệu.
- **Mô hình A hay B tốt hơn?** A dễ kiểm soát và validate. B nhẹ server hơn nhưng phải ký chữ ký và đăng ký lại ảnh. File lớn hoặc lượng upload cao thì chọn B.
- **Lộ `api_secret` thì sao?** Vào Dashboard tạo key mới và vô hiệu key cũ. Secret chỉ để ở biến môi trường hoặc secret manager.
- **Khác gì S3/Firebase Storage?** Cloudinary có sẵn xử lý ảnh/video và CDN. S3/Firebase là lưu trữ thô, phải tự thêm bước xử lý.
- **Upload nhiều ảnh cùng lúc?** Upload song song hoặc đẩy vào hàng đợi (RabbitMQ/Kafka).
