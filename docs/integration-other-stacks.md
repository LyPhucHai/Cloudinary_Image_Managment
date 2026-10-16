# Tích hợp Cloudinary với FastAPI và Node.js

Kiến trúc SOA không phụ thuộc ngôn ngữ: Media Service có thể viết bằng Spring Boot, FastAPI hay Node.js, các service khác chỉ gọi REST và lưu `publicId`.

## FastAPI (Python)

```bash
pip install fastapi uvicorn python-multipart cloudinary
```

```python
# main.py  -- chạy: uvicorn main:app --reload
import os
import cloudinary
import cloudinary.uploader
from fastapi import FastAPI, UploadFile, HTTPException

cloudinary.config(
    cloud_name=os.environ["CLOUDINARY_CLOUD_NAME"],
    api_key=os.environ["CLOUDINARY_API_KEY"],
    api_secret=os.environ["CLOUDINARY_API_SECRET"],
    secure=True,
)

app = FastAPI()
ALLOWED = {"image/jpeg", "image/png", "image/webp", "image/gif"}

@app.post("/api/images")
async def upload(file: UploadFile):
    if file.content_type not in ALLOWED:
        raise HTTPException(400, "Chỉ chấp nhận JPEG, PNG, WEBP, GIF")
    res = cloudinary.uploader.upload(file.file, folder="soa/demo", resource_type="image")
    return {"publicId": res["public_id"], "url": res["secure_url"]}

@app.delete("/api/images")
async def delete(public_id: str):
    cloudinary.uploader.destroy(public_id, invalidate=True)
    return {"deleted": public_id}
```

## Node.js (Express)

```bash
npm i express multer cloudinary
```

```js
// server.js  -- chạy: node server.js
const express = require("express");
const multer = require("multer");
const { v2: cloudinary } = require("cloudinary");

cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
  secure: true,
});

const upload = multer({ storage: multer.memoryStorage(), limits: { fileSize: 5 * 1024 * 1024 } });
const app = express();

app.post("/api/images", upload.single("file"), (req, res) => {
  if (!req.file) return res.status(400).json({ error: "Chưa chọn file" });
  cloudinary.uploader
    .upload_stream({ folder: "soa/demo", resource_type: "image" }, (err, result) => {
      if (err) return res.status(502).json({ error: err.message });
      res.status(201).json({ publicId: result.public_id, url: result.secure_url });
    })
    .end(req.file.buffer);
});

app.listen(3000, () => console.log("http://localhost:3000"));
```

## Service khác gọi Media Service (ví dụ Product Service, Spring Boot)

```java
@FeignClient(name = "media-service", url = "${media.service.url}")
public interface MediaClient {
    @PostMapping(value = "/api/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ImageResponse upload(@RequestPart("file") MultipartFile file);
}
```

Product Service chỉ lưu `imagePublicId` (và `imageUrl`) trong bảng `products`, không lưu file.
