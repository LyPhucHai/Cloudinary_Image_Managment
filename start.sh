#!/usr/bin/env bash
# Cài đặt và chạy Cloudinary Media Lab bằng Docker (Linux / macOS / Git Bash)
set -euo pipefail
cd "$(dirname "$0")"

echo "============================================"
echo "  Cloudinary Media Lab - cài đặt và chạy"
echo "============================================"

command -v docker >/dev/null 2>&1 || { echo "[LỖI] Chưa cài Docker. Tải tại https://www.docker.com/products/docker-desktop"; exit 1; }
docker info >/dev/null 2>&1 || { echo "[LỖI] Docker chưa chạy. Hãy mở Docker rồi chạy lại."; exit 1; }

if [ ! -f .env ]; then
  echo "Lần đầu chạy: nhập khóa Cloudinary (console.cloudinary.com > Settings > API Keys)"
  read -r -p "Cloud name: " CN
  read -r -p "API key: " AK
  read -r -s -p "API secret (không hiện khi gõ): " AS; echo
  CN="${CN// /}"; AK="${AK// /}"; AS="${AS// /}"
  if [ -z "$CN" ] || [ -z "$AK" ] || [ -z "$AS" ]; then echo "[LỖI] Bạn chưa nhập đủ 3 giá trị."; exit 1; fi
  printf 'CLOUDINARY_CLOUD_NAME=%s\nCLOUDINARY_API_KEY=%s\nCLOUDINARY_API_SECRET=%s\n' "$CN" "$AK" "$AS" > .env
  echo "Đã tạo file .env"
fi

echo "Đang build và khởi động. Lần đầu có thể mất vài phút..."
docker compose up --build -d

echo "Đang chờ ứng dụng sẵn sàng..."
ok=0
for _ in $(seq 1 90); do
  if curl -fs -o /dev/null http://localhost:8090/api/images; then ok=1; break; fi
  sleep 2
done
[ "$ok" = 1 ] || { echo "[LỖI] Ứng dụng chưa sẵn sàng. Xem log: docker compose logs"; exit 1; }

echo "Xong! Mở http://localhost:8090"
(xdg-open http://localhost:8090 || open http://localhost:8090) >/dev/null 2>&1 || true
echo "Để dừng: docker compose down"
