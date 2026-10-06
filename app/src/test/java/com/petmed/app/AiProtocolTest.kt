package com.petmed.app

import com.google.gson.JsonParser
import com.petmed.app.interview.AiConfig
import com.petmed.app.interview.AiKeyStore
import com.petmed.app.interview.AiProtocol
import com.petmed.app.interview.AiProvider
import com.petmed.app.interview.InterviewBank
import com.petmed.app.interview.Transcript
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiProtocolTest {

    private val claude = AiConfig(AiProvider.ANTHROPIC, "sk-ant-test", "claude-sonnet-5-5", AiProvider.ANTHROPIC.defaultUrl)
    private val groq = AiConfig(AiProvider.GROQ, "gsk_test", "openai/gpt-oss-20b", AiProvider.GROQ.defaultUrl)
    private val custom = AiConfig(AiProvider.CUSTOM, "k", "some-model", "https://example.com/v1/chat/completions")

    // ───── 請求格式（對照官方文件）─────

    @Test fun `Claude 請求：必要欄位齊全，system 獨立、messages 只有 user，不帶 temperature`() {
        val body = JsonParser.parseString(AiProtocol.requestBody(claude, "系統提示", "使用者內容", 800)).asJsonObject
        assertEquals("claude-sonnet-5-5", body.get("model").asString)
        assertTrue("思考 token 計入 max_tokens，必須給足", body.get("max_tokens").asInt >= 4096)
        assertEquals("系統提示", body.get("system").asString)
        val msgs = body.getAsJsonArray("messages")
        assertEquals(1, msgs.size())
        assertEquals("user", msgs[0].asJsonObject.get("role").asString)
        assertEquals("使用者內容", msgs[0].asJsonObject.get("content").asString)
        assertFalse(body.has("temperature"))
        assertFalse(body.has("reasoning_effort"))
    }

    @Test fun `Claude 標頭：x-api-key 與 anthropic-version`() {
        val h = AiProtocol.headers(claude)
        assertEquals("sk-ant-test", h["x-api-key"])
        assertEquals("2023-06-01", h["anthropic-version"])
        assertFalse(h.containsKey("Authorization"))
    }

    @Test fun `Groq 與自訂：OpenAI 格式，system 放在 messages 內`() {
        val g = JsonParser.parseString(AiProtocol.requestBody(groq, "S", "U", 800)).asJsonObject
        assertEquals("system", g.getAsJsonArray("messages")[0].asJsonObject.get("role").asString)
        assertEquals(800, g.get("max_completion_tokens").asInt)
        assertEquals("low", g.get("reasoning_effort").asString)
        assertEquals("Bearer gsk_test", AiProtocol.headers(groq)["Authorization"])

        val c = JsonParser.parseString(AiProtocol.requestBody(custom, "S", "U", 800)).asJsonObject
        assertTrue("思考型模型的思考 token 也計入上限", c.get("max_tokens").asInt >= 4096)
        assertFalse(c.has("max_completion_tokens"))
        assertFalse(c.has("reasoning_effort"))
    }

    @Test fun `Gemini：OpenAI 相容格式，預留思考額度，網址與官方文件一致`() {
        val gemini = AiConfig(AiProvider.GEMINI, "AIza-test", "gemini-2.5-flash", AiProvider.GEMINI.defaultUrl)
        val body = JsonParser.parseString(AiProtocol.requestBody(gemini, "S", "U", 800)).asJsonObject
        assertTrue(body.get("max_tokens").asInt >= 4096)
        assertEquals("system", body.getAsJsonArray("messages")[0].asJsonObject.get("role").asString)
        assertEquals("Bearer AIza-test", AiProtocol.headers(gemini)["Authorization"])
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
            AiProvider.GEMINI.defaultUrl
        )
        assertEquals("好", AiProtocol.parseText(AiProvider.GEMINI, """{"choices":[{"message":{"content":"好"}}]}"""))
    }

    @Test fun `Gemini 的 limit 0 要說明成沒有免費額度`() {
        val body = """{"error":{"code":429,"message":"Quota exceeded for metric ... limit: 0, model: gemini-1.5-flash"}}"""
        assertTrue(AiProtocol.explainError(AiProvider.GEMINI, 429, body).contains("沒有免費額度"))
    }

    @Test fun `特殊字元與換行會被正確跳脫`() {
        val tricky = "他說：「\"引號\"」\n第二行\\反斜線"
        val body = JsonParser.parseString(AiProtocol.requestBody(claude, "S", tricky, 100)).asJsonObject
        assertEquals(tricky, body.getAsJsonArray("messages")[0].asJsonObject.get("content").asString)
    }

    // ───── 回應解析 ─────

    @Test fun `Claude 回應：跳過 thinking 區塊，只取 text`() {
        val resp = """{"id":"m","type":"message","role":"assistant","content":[
            {"type":"thinking","thinking":"先想一下","signature":"abc"},
            {"type":"text","text":"這是摘要。"}],"stop_reason":"end_turn"}"""
        assertEquals("這是摘要。", AiProtocol.parseText(AiProvider.ANTHROPIC, resp))
    }

    @Test fun `Claude 回應：只有 thinking 沒有 text 代表被截斷，回傳 null`() {
        val resp = """{"content":[{"type":"thinking","thinking":"想到一半","signature":"x"}],"stop_reason":"max_tokens"}"""
        assertNull(AiProtocol.parseText(AiProvider.ANTHROPIC, resp))
    }

    @Test fun `Claude 回應：多個 text 區塊會合併`() {
        val resp = """{"content":[{"type":"text","text":"甲"},{"type":"text","text":"乙"}]}"""
        assertEquals("甲乙", AiProtocol.parseText(AiProvider.ANTHROPIC, resp))
    }

    @Test fun `OpenAI 格式回應`() {
        val resp = """{"choices":[{"message":{"role":"assistant","content":" 好 "},"finish_reason":"stop"}]}"""
        assertEquals("好", AiProtocol.parseText(AiProvider.GROQ, resp))
        assertNull(AiProtocol.parseText(AiProvider.GROQ, """{"choices":[{"message":{"content":null}}]}"""))
        assertNull(AiProtocol.parseText(AiProvider.GROQ, "不是 JSON"))
    }

    // ───── 錯誤說明 ─────

    @Test fun `Claude 錯誤：認證、餘額、限流、模型`() {
        val auth = """{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}"""
        assertTrue(AiProtocol.explainError(AiProvider.ANTHROPIC, 401, auth).contains("無效"))
        val credit = """{"type":"error","error":{"type":"invalid_request_error","message":"Your credit balance is too low"}}"""
        assertTrue(AiProtocol.explainError(AiProvider.ANTHROPIC, 400, credit).contains("餘額"))
        assertTrue(AiProtocol.explainError(AiProvider.ANTHROPIC, 429, """{"error":{"type":"rate_limit_error"}}""").contains("上限"))
        assertTrue(AiProtocol.explainError(AiProvider.ANTHROPIC, 404, """{"error":{"type":"not_found_error"}}""").contains("模型"))
        assertTrue(AiProtocol.explainError(AiProvider.ANTHROPIC, 529, """{"error":{"type":"overloaded_error"}}""").contains("太忙"))
    }

    @Test fun `過期金鑰優先於一般的認證失敗說明`() {
        val body = """{"error":{"message":"Invalid API Key","code":"expired_api_key"}}"""
        assertTrue(AiProtocol.explainError(AiProvider.GROQ, 401, body).contains("過期"))
    }

    // ───── 選項對應的輸出解析 ─────

    private val single = InterviewBank.get("core_appetite")!!
    private val multi = InterviewBank.get("vomit_content")!!

    @Test fun `選項對應：合法輸出`() {
        val m = AiProtocol.parseMapped("""{"matched":["減少"],"other":""}""", single)
        assertNotNull(m)
        assertEquals(listOf("減少"), m!!.matched)
    }

    @Test fun `選項對應：模型自創選項一律視為無效`() {
        assertNull(AiProtocol.parseMapped("""{"matched":["吃很少"],"other":""}""", single))
    }

    @Test fun `選項對應：單選最多留一個，複選全留`() {
        assertEquals(1, AiProtocol.parseMapped("""{"matched":["正常","減少"],"other":""}""", single)!!.matched.size)
        assertEquals(2, AiProtocol.parseMapped("""{"matched":["泡沫","其他"],"other":""}""", multi)!!.matched.size)
    }

    @Test fun `選項對應：前後有多餘文字也能取出 JSON，壞掉的輸出回傳 null`() {
        assertNotNull(AiProtocol.parseMapped("好的：\n{\"matched\":[\"增加\"],\"other\":\"\"}\n以上", single))
        assertNull(AiProtocol.parseMapped("完全不是 JSON", single))
    }

    // ───── 供應商金鑰格式 ─────

    @Test fun `Claude 金鑰格式檢查`() {
        assertNull(AiKeyStore.formatProblem("sk-ant-" + "a".repeat(50), AiProvider.ANTHROPIC))
        assertTrue(AiKeyStore.formatProblem("gsk_" + "a".repeat(52), AiProvider.ANTHROPIC)!!.contains("sk-ant-"))
        assertTrue(AiKeyStore.formatProblem("sk-ant-" + "a".repeat(52), AiProvider.GROQ)!!.contains("gsk_"))
        assertNull(AiKeyStore.formatProblem("any-key-format", AiProvider.CUSTOM))
    }

    @Test fun `預設模型與網址都有填，Claude 用不會太快退役的型號`() {
        AiProvider.values().filter { it != AiProvider.CUSTOM }.forEach {
            assertTrue(it.defaultModel.isNotBlank()); assertTrue(it.defaultUrl.startsWith("https://"))
        }
        assertEquals("claude-sonnet-5-5", AiProvider.ANTHROPIC.defaultModel)
        assertEquals("https://api.anthropic.com/v1/messages", AiProvider.ANTHROPIC.defaultUrl)
    }

    // ───── 接續問診：對話還原 ─────

    @Test fun `解析稽核事件還原對話`() {
        val t = Transcript.parseAnswered("core_appetite｜最近的食慾，跟平常比起來怎麼樣？｜減少｜出處：UTenn 年度；Metro 2；OSU 12")
        assertNotNull(t)
        assertEquals("core_appetite", t!!.questionId)
        assertEquals("最近的食慾，跟平常比起來怎麼樣？", t.question)
        assertEquals("減少", t.answer)
    }

    @Test fun `答案裡含分隔符號也能正確還原`() {
        val t = Transcript.parseAnswered("complaint｜請描述｜牠抓耳朵｜甩頭｜出處：Merck")
        assertEquals("牠抓耳朵｜甩頭", t!!.answer)
    }

    @Test fun `不是答題事件的內容回傳 null`() {
        assertNull(Transcript.parseAnswered("加入照片｜耳朵｜左耳"))
        assertNull(Transcript.parseAnswered("亂寫"))
    }
}
