#!/usr/bin/env bash
# Chạy Media Service trên máy (Linux/macOS/Git Bash): nạp .env rồi start Spring Boot
set -euo pipefail
cd "$(dirname "$0")"
[ -f .env ] || { echo "Chưa có file .env. Hãy chạy: cp .env.example .env và điền key Cloudinary."; exit 1; }
set -a; source .env; set +a
cd media-service
mvn spring-boot:run
