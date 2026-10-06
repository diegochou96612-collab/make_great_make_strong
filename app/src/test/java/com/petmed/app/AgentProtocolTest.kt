package com.petmed.app

import com.petmed.app.interview.AgentProtocol
import com.petmed.app.interview.InterviewBank
import com.petmed.app.interview.MULTI_SEP
import com.petmed.app.interview.Species
import com.petmed.app.interview.Transcript
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentProtocolTest {

    private val dogCatalog = AgentProtocol.catalogFor(Species.DOG)

    private fun parse(json: String) = AgentProtocol.parse(json, dogCatalog)

    // ───── 一句話拆填多個欄位 ─────

    @Test fun `一句話可以填多個欄位，選項合法才收`() {
        val r = parse("""{"reply":"好的，我記下了。","updates":[
            {"id":"vomit_dur","value":"三天"},
            {"id":"vomit_freq","value":"一天三次"},
            {"id":"vomit_content","value":["泡沫","黃綠色液體"]},
            {"id":"categories","value":["嘔吐（包含頻繁吐毛球）"]}],
            "notes":[],"vet_questions":[]}""")!!
        assertEquals(4, r.updates.size)
        val byId = r.updates.associateBy { it.question.id }
        assertEquals("三天", byId["vomit_dur"]!!.value)
        assertEquals("泡沫${MULTI_SEP}黃綠色液體", byId["vomit_content"]!!.value)
        assertEquals(InterviewBank.CAT_VOMIT, byId["categories"]!!.value)
        assertTrue(r.notes.isEmpty())
    }

    // ───── 驗證層：AI 亂來也不會污染欄位 ─────

    @Test fun `不存在的欄位 id 直接忽略`() {
        val r = parse("""{"reply":"ok","updates":[{"id":"not_a_field","value":"x"},{"id":"core_water","value":"變多"}],"notes":[],"vet_questions":[]}""")!!
        assertEquals(listOf("core_water"), r.updates.map { it.question.id })
    }

    @Test fun `單選題給了不在選項內的值，不填欄位，改存成補充說明（不遺失）`() {
        val r = parse("""{"reply":"ok","updates":[{"id":"core_appetite","value":"吃很少很少"}],"notes":[],"vet_questions":[]}""")!!
        assertTrue(r.updates.isEmpty())
        assertEquals(listOf("食慾：吃很少很少"), r.notes)
    }

    @Test fun `複選題：合法的留下，不合法的轉成補充說明`() {
        val r = parse("""{"reply":"ok","updates":[{"id":"vomit_content","value":["泡沫","綠色的水"]}],"notes":[],"vet_questions":[]}""")!!
        assertEquals("泡沫", r.updates.single().value)
        assertTrue(r.notes.any { it.contains("綠色的水") })
    }

    @Test fun `同一欄位出現多次，只留最後一次（更正）`() {
        val r = parse("""{"reply":"ok","updates":[{"id":"vomit_dur","value":"三天"},{"id":"vomit_dur","value":"兩天前才開始"}],"notes":[],"vet_questions":[]}""")!!
        assertEquals("兩天前才開始", r.updates.single().value)
    }

    @Test fun `文字題限制長度`() {
        val long = "字".repeat(1000)
        val r = parse("""{"reply":"ok","updates":[{"id":"complaint","value":"$long"}],"notes":[],"vet_questions":[]}""")!!
        assertEquals(300, r.updates.single().value.length)
    }

    // ───── 安全：回覆含診斷或用藥用語就丟掉 ─────

    @Test fun `回覆含診斷或推測用語時，改用安全回覆`() {
        listOf("牠可能是耳朵發炎", "疑似感染", "建議使用耳藥", "這個劑量要小心", "應該是過敏").forEach { bad ->
            val r = parse("""{"reply":"$bad","updates":[],"notes":[],"vet_questions":[]}""")!!
            assertEquals("「$bad」應被替換", AgentProtocol.SAFE_REPLY, r.reply)
        }
    }

    @Test fun `正常回覆保留`() {
        val r = parse("""{"reply":"了解，三天前開始的，我幫你記下來。","updates":[],"notes":[],"vet_questions":[]}""")!!
        assertEquals("了解，三天前開始的，我幫你記下來。", r.reply)
    }

    @Test fun `想問獸醫的問題與補充說明會被保留`() {
        val r = parse("""{"reply":"我不能判斷，會幫你記下來帶去問獸醫。","updates":[],"notes":["飼主說牠最近常躲在沙發下"],"vet_questions":["這是什麼原因？"]}""")!!
        assertEquals(listOf("這是什麼原因？"), r.vetQuestions)
        assertEquals(listOf("飼主說牠最近常躲在沙發下"), r.notes)
    }

    @Test fun `壞掉的輸出回傳 null，由呼叫端退回固定流程`() {
        assertNull(parse("完全不是 JSON"))
        assertNull(parse("{壞掉"))
    }

    @Test fun `JSON 前後有多餘文字也能解析`() {
        assertNotNull(parse("好的：\n{\"reply\":\"ok\",\"updates\":[],\"notes\":[],\"vet_questions\":[]}\n以上"))
    }

    @Test fun `缺少欄位時使用預設值`() {
        val r = parse("""{"reply":"ok"}""")!!
        assertTrue(r.updates.isEmpty() && r.notes.isEmpty() && r.vetQuestions.isEmpty())
    }

    // ───── 欄位清單與提示詞 ─────

    @Test fun `狗的清單不含貓專屬題，貓的清單含`() {
        assertFalse(AgentProtocol.catalogFor(Species.DOG).any { it.id == "mob_stiff" })
        assertTrue(AgentProtocol.catalogFor(Species.CAT).any { it.id == "mob_stiff" })
    }

    @Test fun `欄位清單文字包含 id、名稱、題型與選項`() {
        val text = AgentProtocol.catalogText(dogCatalog)
        assertTrue(text.contains("core_appetite｜食慾｜單選｜正常／增加／減少"))
        assertTrue(text.contains("complaint｜"))
    }

    @Test fun `提示詞包含必要區段，且明確禁止診斷`() {
        assertTrue(AgentProtocol.SYSTEM_PROMPT.contains("絕對不可以"))
        assertTrue(AgentProtocol.SYSTEM_PROMPT.contains("vet_questions"))
        val u = AgentProtocol.userPrompt("名字：小白", InterviewBank.get("core_appetite"), listOf("主訴" to "抓耳朵"), dogCatalog, listOf("助理：你好"), "牠最近吃很少")
        listOf("【寵物】", "【App 目前正在問的題目】", "【已記錄的內容】", "【欄位清單", "【最近的對話】", "【飼主剛說的話】", "牠最近吃很少")
            .forEach { assertTrue("缺少 $it", u.contains(it)) }
    }

    @Test fun `問診結束後的提示詞說明沒有固定題目`() {
        val u = AgentProtocol.userPrompt("x", null, emptyList(), dogCatalog, emptyList(), "其實是兩天前")
        assertTrue(u.contains("問診已結束"))
    }

    // ───── App 產生的確認文字 ─────

    @Test fun `確認文字：新增、更正、補充、問獸醫`() {
        val t = AgentProtocol.confirmation(
            listOf(Triple("嘔吐多久", null, "三天"), Triple("食慾", "正常", "減少")), notes = 1, vetQuestions = 2
        )!!
        assertTrue(t.contains("已記下：嘔吐多久＝三天"))
        assertTrue(t.contains("已更正：食慾：正常 → 減少"))
        assertTrue(t.contains("1 則補充說明"))
        assertTrue(t.contains("2 個要問獸醫的問題"))
    }

    @Test fun `沒有任何變動就不產生確認文字`() {
        assertNull(AgentProtocol.confirmation(emptyList(), 0, 0))
    }

    // ───── 是在發問還是在回答 ─────

    @Test fun `判斷飼主是否在發問`() {
        listOf("這是什麼病？", "要怎麼辦", "請問可以吃什麼", "牠會不會有事嗎", "為什麼一直吐").forEach {
            assertTrue("$it 應判定為發問", AgentProtocol.looksLikeQuestion(it))
        }
        listOf("三天前開始的", "一天吐三次", "泡沫", "牠最近都沒精神").forEach {
            assertFalse("$it 應判定為回答", AgentProtocol.looksLikeQuestion(it))
        }
    }

    // ───── 對話還原 ─────

    @Test fun `還原自由對話紀錄`() {
        val u = Transcript.parseChat("U｜其實是兩天前才開始")
        assertEquals(true, u!!.fromUser); assertEquals("其實是兩天前才開始", u.text)
        val b = Transcript.parseChat("B｜好的，我幫你更正。")
        assertEquals(false, b!!.fromUser)
        assertNull(Transcript.parseChat("X｜亂寫"))
    }
}
