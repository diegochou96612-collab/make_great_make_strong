package com.petmed.app

import com.petmed.app.interview.InterviewBank
import com.petmed.app.interview.MULTI_SEP
import com.petmed.app.interview.QType
import com.petmed.app.interview.RedFlag
import com.petmed.app.interview.RedFlagDetector
import com.petmed.app.interview.Species
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterviewLogicTest {

    private fun ids(a: Map<String, String>, s: Species = Species.DOG) = InterviewBank.plan(a, s).map { it.id }

    // ───── 題庫結構 ─────

    @Test fun `題目 id 不重複`() {
        val ids = InterviewBank.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun `選擇題都有選項，旗標選項必須存在於選項中`() {
        InterviewBank.all.forEach { q ->
            if (q.type != QType.TEXT) assertTrue("${q.id} 缺少選項", q.options.isNotEmpty())
            q.flagOptions.keys.forEach { k -> assertTrue("${q.id} 的旗標選項「$k」不在選項內", k in q.options) }
        }
    }

    @Test fun `選項文字不可包含複選分隔符號`() {
        InterviewBank.all.forEach { q ->
            q.options.forEach { assertFalse("${q.id} 的選項「$it」含分隔符號", it.contains(MULTI_SEP)) }
        }
    }

    @Test fun `每題都有出處說明`() {
        InterviewBank.all.forEach { assertTrue("${it.id} 沒有出處", it.source.isNotBlank()) }
    }

    @Test fun `排序值唯一`() {
        val orders = InterviewBank.all.map { InterviewBank.sortOrderOf(it) }
        assertEquals(orders.size, orders.toSet().size)
    }

    // ───── 流程 ─────

    @Test fun `未回答狀況類別前只出現開場兩題`() {
        assertEquals(listOf("complaint", "categories"), ids(emptyMap()))
    }

    @Test fun `選嘔吐只展開嘔吐模組，不展開腹瀉`() {
        val a = mapOf("complaint" to "吐", "categories" to InterviewBank.CAT_VOMIT)
        val p = ids(a)
        assertTrue("vomit_freq" in p)
        assertFalse("stool_freq" in p)
        assertTrue("core_appetite" in p)
        assertTrue("med_current" in p)
    }

    @Test fun `只選沒精神不會被問到哪隻腳跛行`() {
        val a = mapOf("complaint" to "沒精神", "categories" to InterviewBank.CAT_ENERGY, "core_energy" to "比平常沒精神")
        val p = ids(a)
        assertFalse("mob_limp" in p)
        assertTrue("act_level" in p)
    }

    @Test fun `食慾減少才追問多久`() {
        val base = mapOf("complaint" to "x", "categories" to InterviewBank.CAT_APPETITE)
        assertFalse("core_appetite_dur" in ids(base + ("core_appetite" to "正常")))
        assertTrue("core_appetite_dur" in ids(base + ("core_appetite" to "減少")))
    }

    @Test fun `複選答案可以正確拆解`() {
        val a = mapOf("categories" to listOf(InterviewBank.CAT_VOMIT, InterviewBank.CAT_EAR).joinToString(MULTI_SEP))
        val p = ids(a + ("complaint" to "x"))
        assertTrue("vomit_freq" in p)
        assertTrue("ear_first" in p)
    }

    @Test fun `貓專屬題只對貓出現`() {
        val a = mapOf("complaint" to "x", "categories" to InterviewBank.CAT_MOBILITY)
        assertTrue("mob_stiff" in ids(a, Species.CAT))
        assertFalse("mob_stiff" in ids(a, Species.DOG))
    }

    @Test fun `飲食深問：單純案例不觸發，有腸胃症狀或用藥或自製食物才觸發`() {
        val plain = mapOf("complaint" to "x", "categories" to InterviewBank.CAT_EAR, "core_bowel" to "正常",
            "med_current" to "沒有", "diet_supp" to "沒有", "diet_main" to "一般市售飼料或罐頭", "diet_treat" to "少量")
        assertFalse(InterviewBank.dietDeepTriggered(plain))
        assertTrue(InterviewBank.dietDeepTriggered(plain + ("categories" to InterviewBank.CAT_VOMIT)))
        assertTrue(InterviewBank.dietDeepTriggered(plain + ("med_current" to "有")))
        assertTrue(InterviewBank.dietDeepTriggered(plain + ("diet_main" to "一般市售飼料或罐頭${MULTI_SEP}自製食物")))
        assertTrue(InterviewBank.dietDeepTriggered(plain + ("diet_treat" to "超過一成")))
    }

    @Test fun `飲食深問題目只在觸發時出現`() {
        val a = mapOf("complaint" to "x", "categories" to InterviewBank.CAT_EAR, "core_bowel" to "正常",
            "med_current" to "沒有", "diet_supp" to "沒有", "diet_main" to "一般市售飼料或罐頭", "diet_treat" to "少量")
        assertFalse("dd_change" in ids(a))
        assertTrue("dd_change" in ids(a + ("med_current" to "有")))
    }

    // ───── 緊急紅旗 ─────

    @Test fun `肯定句會觸發`() {
        assertEquals(listOf(RedFlag.SEIZURE), RedFlagDetector.detect("牠剛剛抽搐了一下"))
        assertTrue(RedFlag.BREATHING in RedFlagDetector.detect("牠好像呼吸困難"))
        assertTrue(RedFlag.POISON in RedFlagDetector.detect("我懷疑牠誤食了東西"))
    }

    @Test fun `否定句不觸發`() {
        assertTrue(RedFlagDetector.detect("沒有抽搐，也沒有呼吸困難").isEmpty())
        assertTrue(RedFlagDetector.detect("牠不是中毒").isEmpty())
    }

    @Test fun `疑問或推測句式一律觸發，不可漏報`() {
        assertTrue(RedFlag.POISON in RedFlagDetector.detect("不知道有沒有誤食"))
        assertTrue(RedFlag.POISON in RedFlagDetector.detect("是不是中毒了"))
        assertTrue(RedFlag.SEIZURE in RedFlagDetector.detect("疑似抽搐"))
    }

    @Test fun `沒有犬貓出處的項目不應觸發緊急`() {
        assertTrue(RedFlagDetector.detect("牠劇烈嘔吐").isEmpty())
        assertTrue(RedFlagDetector.detect("拉血便").isEmpty())
    }

    @Test fun `第一個肯定出現之後的否定不會蓋掉`() {
        assertTrue(RedFlag.SEIZURE in RedFlagDetector.detect("昨天沒有抽搐，但今天又抽搐了"))
    }

    @Test fun `每個紅旗都有關鍵字、處置與出處`() {
        RedFlag.values().forEach {
            assertTrue(it.keywords.isNotEmpty())
            assertTrue(it.guidance.isNotBlank())
            assertTrue(it.source.isNotBlank())
        }
    }
}
