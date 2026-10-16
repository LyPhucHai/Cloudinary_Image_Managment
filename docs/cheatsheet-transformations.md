# Cheatsheet: Cloudinary Transformation URL

Cấu trúc:

```
https://res.cloudinary.com/<cloud_name>/image/upload/<transformations>/<version>/<public_id>.<ext>
```

Nhiều tham số cách nhau bằng dấu phẩy `,` (cùng một bước). Nhiều bước nối bằng `/` (chain).

| Tham số | Ý nghĩa | Ví dụ |
|---|---|---|
| `w_` / `h_` | Chiều rộng / cao (px) | `w_400,h_300` |
| `c_` | Kiểu cắt/resize | `c_fill`, `c_fit`, `c_scale`, `c_limit`, `c_thumb`, `c_pad` |
| `g_` | Điểm lấy nét khi cắt | `g_auto` (AI chọn vùng quan trọng), `g_face`, `g_center` |
| `q_` | Chất lượng | `q_auto` (tự chọn), `q_80` |
| `f_` | Định dạng | `f_auto` (WebP/AVIF tuỳ trình duyệt), `f_jpg`, `f_png` |
| `r_` | Bo góc | `r_20`, `r_max` (tròn/ellipse) |
| `e_` | Hiệu ứng | `e_grayscale`, `e_blur:300`, `e_sepia`, `e_sharpen` |
| `dpr_` | Mật độ điểm ảnh | `dpr_auto`, `dpr_2.0` |
| `l_` | Overlay (watermark) | `l_logo,g_south_east,w_100` |
| `o_` | Độ mờ (opacity) | `o_50` |

## Công thức hay dùng

| Mục đích | Transformation |
|---|---|
| Thumbnail sản phẩm | `c_fill,g_auto,w_300,h_300,f_auto,q_auto` |
| Avatar tròn theo khuôn mặt | `c_thumb,g_face,w_200,h_200,r_max,f_auto,q_auto` |
| Banner rộng | `c_fill,g_auto,w_1200,h_400,f_auto,q_auto` |
| Không bao giờ phóng to quá gốc | `c_limit,w_1200,f_auto,q_auto` |
| Ảnh mờ làm placeholder (LQIP) | `w_30,e_blur:1000,q_auto` |
| Ảnh responsive nhiều kích thước | tạo `srcset` với `w_480`, `w_800`, `w_1200` |

## `<img srcset>` mẫu

```html
<img
  src="https://res.cloudinary.com/CLOUD/image/upload/c_limit,w_800,f_auto,q_auto/PUBLIC_ID"
  srcset="https://res.cloudinary.com/CLOUD/image/upload/c_limit,w_480,f_auto,q_auto/PUBLIC_ID 480w,
          https://res.cloudinary.com/CLOUD/image/upload/c_limit,w_800,f_auto,q_auto/PUBLIC_ID 800w,
          https://res.cloudinary.com/CLOUD/image/upload/c_limit,w_1200,f_auto,q_auto/PUBLIC_ID 1200w"
  sizes="(max-width: 600px) 480px, 800px"
  loading="lazy" alt="Mô tả ảnh">
```

Tài liệu đầy đủ: https://cloudinary.com/documentation/image_transformations
