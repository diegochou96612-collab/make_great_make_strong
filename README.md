# 動物飼養助手 (PetMedApp)

寵物健康管理 Android App，整合動物保健知識庫，提供 AI 問答、用藥紀錄、醫院查詢等功能。

## 技術棧

- **語言**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **導航**: Navigation Compose
- **AI**: Groq API（Llama 3.3 70B）
- **資料庫**: Firebase Firestore + Room
- **最低 SDK**: Android 8.0 (API 24)

## 主要功能

| 畫面 | 功能 |
|------|------|
| 首頁 | 用藥提醒卡片、快速功能入口 |
| AI 問答 | 基於動物保健知識庫的智慧問答，支援多輪對話 |
| 用藥紀錄 | 歷史用藥清單與狀態追蹤 |
| 動物醫院查詢 | 依地區或名稱搜尋獸醫院 |
| 個人設定 | 寵物資料管理 |

## 開啟專案

1. 使用 **Android Studio** 開啟本資料夾
2. 在 `local.properties` 加入 Groq API 金鑰：
3. 等待 Gradle sync 完成
4. 連接裝置或啟動模擬器，按 **Run** 即可

## 配色規範

| 色彩 | Hex | 用途 |
|------|-----|------|
| 橘色 | `#F5874F` | 主色、首頁標題、使用者訊息 |
| 綠色 | `#7BC8A4` | AI 回覆訊息泡泡 |
| 藍色 | `#7BB8D4` | 醫院查詢頁面 |
| 奶油色 | `#FAF6F1` | 背景 |