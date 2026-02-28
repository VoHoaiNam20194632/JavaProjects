# Telegram Bot Server

Bot Telegram điều khiển automation test từ xa. Gõ lệnh trên Telegram → bot chạy test trên máy này → gửi kết quả + Allure Report link về Telegram.

## Yêu cầu

- Java 21
- Maven 3.9+ (`D:/Tools/apache-maven-3.9.8`)
- Allure CLI (`D:/Tools/allure-2.33.0`)
- Chrome browser (cho UI test)

## Chạy / Dừng Server

### Cách 1: Chạy foreground (thấy log trực tiếp)
```
start.bat        ← double-click để chạy, Ctrl+C để dừng
```

### Cách 2: Chạy background (chạy ngầm, đóng terminal vẫn sống)
```
start-bg.bat     ← chạy ngầm, log ghi vào logs/bot.log
stop.bat         ← tắt server
```

### Cách 3: Chạy bằng lệnh
```bash
# Build
mvn clean package -DskipTests

# Chạy
java -jar target/telegram-bot-server-1.0.0.jar

# Hoặc chạy trực tiếp (không cần build trước)
mvn spring-boot:run
```

### Kiểm tra server đang chạy
```bash
# Check port 8080
netstat -ano | findstr ":8080.*LISTENING"

# Kết quả: PID cuối dòng là process ID của server
# TCP  0.0.0.0:8080  0.0.0.0:0  LISTENING  6812
```

### Tắt server
```bash
# Dùng script
stop.bat

# Hoặc tắt bằng PID
taskkill /PID <pid> /F
```

## Lệnh Telegram

| Lệnh | Mô tả |
|-------|--------|
| `/login` | Chạy LoginTest |
| `/dashboard` | Chạy DashboardTest |
| `/smoke` | Chạy Smoke suite |
| `/smoke prod` | Chạy Smoke trên env prod |
| `/regression` | Chạy Regression suite |
| `/api` | Chạy API tests |
| `/env dev` | Đặt default env = dev |
| `/status` | Xem test đang chạy |
| `/cancel <id>` | Hủy test run |
| `/help` | Xem danh sách lệnh |

## Allure Report

Sau mỗi test run, report tự động deploy lên GitHub Pages:
- **URL**: https://vohoainam20194632.github.io/JavaProjects/

Config trong `src/main/resources/application.yml`:
```yaml
bot:
  allure:
    github-pages:
      enabled: true    # true = push lên GitHub Pages, false = xem localhost
```

## Cấu hình

File: `src/main/resources/application.yml`

| Config | Mô tả | Default |
|--------|--------|---------|
| `BOT_TOKEN` | Telegram Bot token (env var) | - |
| `bot.telegram.username` | Bot username | nam_automation_bot |
| `bot.telegram.allowed-chat-ids` | Chat ID được phép dùng bot | 6169627315 |
| `bot.runner.default-env` | Environment mặc định | dev |
| `bot.runner.headless` | Chạy Chrome headless | true |
| `bot.runner.max-concurrent-runs` | Số test chạy song song tối đa | 3 |
| `server.port` | Port HTTP server | 8080 |
