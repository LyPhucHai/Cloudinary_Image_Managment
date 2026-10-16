# Chạy Media Service trên Windows PowerShell: nạp .env rồi start Spring Boot
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
if (-not (Test-Path ".env")) {
  Write-Error "Chưa có file .env. Hãy chạy: Copy-Item .env.example .env và điền key Cloudinary."
}
Get-Content .env | ForEach-Object {
  if ($_ -match '^\s*([^#=\s]+)\s*=\s*(.*)\s*$') {
    [Environment]::SetEnvironmentVariable($matches[1], $matches[2].Trim('"'), 'Process')
  }
}
Set-Location media-service
mvn spring-boot:run
