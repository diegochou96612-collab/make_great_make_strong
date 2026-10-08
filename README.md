# PetMed 動物飼養助手

協助飼主照顧貓狗的 Android App：用藥提醒、AI 記錄助理、疫苗與檢測手冊、動物醫院查詢。

東吳大學資料科學系（周子謙、李承叡、林俊宇）為中化動藥製作的提案原型。內容為展示用，正式上線前必須由獸醫重寫與審閱。

## 設計原則

這個 App 的 AI **不診斷、不推薦藥物或產品**。

- **流程由 App 控制**：問什麼題目、什麼時候問，都是固定題庫，AI 不能自己出題。
- **AI 只做輔助**：把飼主打的字對應到選項、整理客觀摘要。AI 失效時，記錄照常運作。
- **可追溯**：每個欄位的答案、修改、緊急提示都有事件紀錄，來源標註在題目上；沒有來源的題目標示「待獸醫審閱」。
- **可控制**：飼主可以修改每一個欄位，匯出的報告會標出被修改過的內容。
- **緊急情況不靠 AI**：緊急提示（中毒、呼吸困難、癲癇等）由固定規則判斷，AI 失效也會跳出。

## 主要功能

| 功能 | 說明 |
|------|------|
| 首頁 | 下一個用藥提醒、疫苗與檢測提示、各功能入口 |
| 用藥紀錄 | 用藥提醒與歷史紀錄，滾輪式時間選擇，本機通知提醒 |
| AI 記錄助理 | 以固定題庫為骨架加上自由對話，整理成給獸醫看的報告。可中斷後接續、完成後繼續補充，有對話歷史側欄 |
| 狀況報告 | 每個欄位可編輯；每日紀錄；照片相簿（附拍攝指引）；匯出 PDF（可選天數與是否附照片） |
| 寵物手冊 | 依牠的年齡列出疫苗與檢測時程、名詞介紹小卡、記錄已打項目、可問 AI。不使用手機推播 |
| 醫院查詢 | 依地區搜尋動物醫院，附各院網站連結 |
| 保養品專區 | 日常保養品展示頁（虛構商品，與 AI 記錄助理完全分開） |
| 個人 | 寵物資料管理 |

### 寵物手冊（疫苗與檢測）

- 範圍：臺北市、貓與狗。
- 狗：核心疫苗、鉤端螺旋體疫苗、狂犬病疫苗、心絲蟲檢測。
- 貓：三合一核心疫苗、貓白血病與貓愛滋快篩、貓白血病疫苗、狂犬病疫苗、心絲蟲檢測。
- 時間判斷是 App 內的固定規則（`VaccineGuide.kt`），AI 只負責解釋，且回覆經過安全過濾。
- 貓的狂犬病疫苗依法規有例外（室內飼養、外出裝箱籠且縣市有公告），App 只說明，不替飼主判定。
- 「視情況」的項目（例如貓白血病疫苗）不會出現在「現在可以留意」。

## AI 設定

在「AI 記錄助理」頁面右上角的 AI 設定中選擇供應商並貼上金鑰，不需重新 Build。

| 供應商 | 預設模型 | 備註 |
|--------|----------|------|
| Claude（Anthropic） | `claude-sonnet-5-5` | 付費 |
| Gemini（Google） | `gemini-2.5-flash` | 有免費額度，使用 OpenAI 相容端點 |
| Groq | `openai/gpt-oss-20b` | 免費金鑰會過期 |
| 自訂 | 自行填寫 | 任何 OpenAI 相容端點 |

金鑰只存在手機的 SharedPreferences。也可以在 `local.properties` 加上 `GROQ_API_KEY=...` 作為內建預設。**不要把金鑰提交到 Git。**

## 技術

- Kotlin、Jetpack Compose、Material 3、Navigation Compose
- Room（目前 schema 版本 7，含 4→5、5→6、6→7 的遷移）、KSP
- OkHttp、Gson（AI 呼叫）；Retrofit
- Firebase Firestore（使用者同意後，匿名上傳用藥頻率）
- minSdk 24、targetSdk 36

## 專案結構

```
app/src/main/java/com/petmed/app/
├─ data/            Room 實體、DAO、資料庫、Repository
├─ interview/       題庫、紅旗偵測、AI 協定、報告與 PDF、疫苗手冊規則
├─ notification/    用藥提醒（AlarmManager）
└─ ui/
   ├─ screens/      各畫面
   ├─ components/   滾輪選擇器、照片元件
   ├─ viewmodel/    ViewModel
   └─ navigation/   路由
```

重要檔案：

| 檔案 | 內容 |
|------|------|
| `interview/InterviewBank.kt` | 固定題庫與來源標註 |
| `interview/RedFlag.kt` | 緊急提示規則（含否定句處理） |
| `interview/AgentProtocol.kt` | AI 自由對話的格式驗證，不合格的輸出一律丟棄 |
| `interview/AiProtocol.kt` | 各供應商的請求與錯誤說明 |
| `interview/Report.kt` | 報告與 PDF 匯出 |
| `interview/VaccineGuide.kt` | 疫苗與檢測時程、名詞介紹、狀態判斷 |

## 開啟與建置

### 從 GitHub 下載後，要先補兩個檔案

這兩個檔案含有金鑰與專案設定，**沒有放在倉庫裡**，缺了就無法建置：

| 檔案 | 位置 | 怎麼取得 |
|------|------|----------|
| `google-services.json` | `app/google-services.json` | 向專案成員索取，或用自己的 Firebase 專案下載 |
| `local.properties` | 專案根目錄 | Android Studio 開啟專案時會自動產生 `sdk.dir`；AI 金鑰（`GROQ_API_KEY=...`）可不填，改在 App 內設定 |

### 建置步驟

1. 使用 **Android Studio** 開啟本資料夾，等待 Gradle sync。
2. 連接手機或模擬器，按 **Run**。
3. 要產生 APK：**Build → Build APK(s)**，輸出在 `app/build/outputs/apk/debug/app-debug.apk`。

資料夾路徑含中文時，命令列建置與單元測試可能失敗；請複製到純英文路徑再執行：

```
gradlew :app:testDebugUnitTest
gradlew :app:assembleDebug
```

## 測試

單元測試共 90 項，涵蓋題庫流程、紅旗偵測、AI 協定與輸出驗證、每日紀錄、照片處理、疫苗時程判斷。

AI 呼叫、相機、相簿、PDF 含照片、分享、各畫面排版，目前**沒有自動化測試**，需要在實機手動確認。

## 資料來源

- WSAVA 2024 犬貓疫苗指引（Squires 等人，2024）
- AAFP/ISFM 2020 貓反轉錄病毒指引（Little 等人，2020）
- 美國心絲蟲協會犬貓指引（修訂版 2024）
- 農業部動植物防疫檢疫署：狂犬病相關 Q&A
- 緊急提示：Merck 獸醫手冊（飼主版）、AVMA Pet First Aid

完整引用（APA 格式）與可信度評估見資料夾 `中化AI問診資料/疫苗與篩檢/整理/`。

## 已知限制

- 所有題目、提醒與介紹文字都**尚未經獸醫審閱**。
- 貓的狂犬病免打公告（含臺北市）尚未取得官方公告表。
- 寵物手冊只認得種類含「狗、貓、犬」的寵物。
- 台灣實際可取得的疫苗品項、價格未查證，App 不顯示。
- AI 功能需要有效金鑰，且尚未用正式金鑰完整驗證過。
