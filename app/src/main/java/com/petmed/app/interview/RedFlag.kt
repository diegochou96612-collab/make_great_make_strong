package com.petmed.app.interview

/**
 * 緊急紅旗。**只收「犬貓有出處」的項目**：Merck Veterinary Manual 飼主版（犬貓緊急處置，審閱 2025/12）
 * 與 AVMA Pet First Aid（2025）。詳見 `中化AI問診資料/整理/01_緊急紅旗清單.md`。
 *
 * 注意：
 * - 中文提示為依來源重述，**尚未經獸醫審閱**。
 * - 這是「提示就醫」，不是診斷。
 * - 原本程式碼中的「劇烈嘔吐」「血便」「血尿」在犬貓來源中沒有列為緊急，已移除。
 */
enum class RedFlag(
    val title: String,
    val guidance: String,
    val source: String,
    val keywords: List<String>
) {
    POISON(
        "疑似中毒或誤食有害物",
        "請立刻聯絡獸醫、最近的急診動物醫院或毒物諮詢專線。\n" +
            "• 準備好產品或容器，供獸醫參考\n" +
            "• 收集嘔吐物或嚼碎的東西，密封裝袋帶去醫院\n" +
            "• 未經獸醫或毒物中心指示，請不要催吐，也不要給任何藥物",
        "AVMA Pet First Aid 2025；Merck 犬貓緊急處置",
        listOf("中毒", "誤食", "誤吞", "鼠藥", "老鼠藥")
    ),
    BREATHING(
        "呼吸困難",
        "呼吸困難屬於需要立即獸醫處理的緊急狀況。請先打電話給獸醫或急診醫院說明狀況，讓他們先做準備，並盡快前往。",
        "Merck 犬貓緊急處置（Trouble breathing）",
        listOf("呼吸困難", "喘不過氣", "呼吸很吃力", "呼吸吃力", "張口呼吸", "無法呼吸", "沒辦法呼吸", "呼吸很急促")
    ),
    BLEEDING(
        "嚴重出血",
        "• 用乾淨的布或毛巾，直接在出血處用力按壓\n" +
            "• 至少按壓三分鐘再查看，不要一直打開看\n" +
            "• 血滲透毛巾時不要拿掉，直接在上面再加毛巾\n" +
            "• 出血嚴重請立即前往最近的動物醫院",
        "AVMA Pet First Aid 2025；Merck 犬貓緊急處置",
        listOf("大量出血", "流血不止", "血流不止", "止不住血", "出血不止", "一直流血")
    ),
    FRACTURE(
        "疑似骨折或肢體無法活動",
        "請盡快就醫。需要移動時，盡量減少頭、頸、脊椎的活動，用平整的硬板、厚紙板或折好的毯子支撐。受傷的動物可能咬人或抓人，請先保護自己。",
        "Merck 犬貓緊急處置",
        listOf("骨折", "腳斷", "腿斷", "腳不能動", "腳無法動", "腿不能動")
    ),
    SEIZURE(
        "抽搐",
        "• 移開周圍的其他寵物、家具和可能造成受傷的東西\n" +
            "• 不要壓制牠，也不要試圖把牠嚇醒\n" +
            "• 手不要放進牠的嘴巴或附近\n" +
            "• 有辦法的話，用手錶計時並錄影，之後給獸醫看\n" +
            "• 抽搐結束後讓牠安靜，並立即聯絡獸醫或急診醫院",
        "AVMA Pet First Aid 2025",
        listOf("抽搐", "癲癇", "痙攣")
    ),
    UNCONSCIOUS(
        "失去意識",
        "請立即聯絡獸醫或急診醫院，依指示處理。可先看胸口有沒有起伏，手放在胸口感受有沒有心跳。需要的話，獸醫會電話指導你怎麼做胸部按壓。",
        "Merck 犬貓緊急處置",
        listOf("昏迷", "失去意識", "昏倒", "叫不醒", "意識不清", "沒有意識")
    ),
    CHOKING(
        "疑似噎住",
        "• 若牠還能呼吸，讓牠保持冷靜，並立即就醫\n" +
            "• 不要把手指伸進牠嘴巴，可能會被咬\n" +
            "• 噎住的徵兆包括咳嗽、流口水、乾嘔、用爪子抓嘴、嘴唇或舌頭帶藍色",
        "AVMA Pet First Aid 2025；Merck 犬貓緊急處置",
        listOf("噎到", "噎住", "噎著", "卡到喉嚨", "卡住喉嚨", "異物卡住")
    ),
    HEATSTROKE(
        "疑似中暑",
        "• 盡快前往最近的動物醫院，同時立刻開始降溫\n" +
            "• 移到陰涼處或涼爽的室內\n" +
            "• 用室溫的水浸濕毛巾，輕輕放在頸部、腋下和鼠蹊，每幾分鐘重新浸濕更換\n" +
            "• 不要把牠泡進冷水或冰水裡，可能讓情況更糟\n" +
            "• 有風扇的話，直接對著牠吹",
        "AVMA Pet First Aid 2025；Merck 犬貓緊急處置",
        listOf("中暑", "熱衰竭", "熱中暑")
    ),
    EYE_INJURY(
        "眼睛外傷",
        "眼睛外傷屬於需要立即獸醫處理的緊急狀況，請盡快就醫。",
        "Merck 犬貓緊急處置（Eye injuries）",
        listOf("眼睛受傷", "眼睛被抓", "眼睛流血")
    ),
    SEVERE_PAIN(
        "劇烈疼痛",
        "劇烈疼痛屬於需要立即獸醫處理的緊急狀況。疼痛的動物可能咬人或抓人，請先保護好自己。",
        "Merck 犬貓緊急處置（Severe pain）",
        listOf("劇痛", "痛到慘叫", "一直慘叫", "一直哀號")
    ),
    BLUE_MOUTH(
        "嘴唇或舌頭發藍紫",
        "嘴唇或舌頭帶藍色是噎住的徵兆之一，牙齦顏色異常是中暑的徵兆之一。請立即聯絡獸醫或急診醫院。",
        "AVMA Pet First Aid 2025",
        listOf("舌頭發紫", "舌頭發藍", "嘴唇發紫", "嘴唇發藍", "牙齦發紫", "牙齦發藍", "舌頭變紫", "舌頭變藍")
    );

    companion object {
        const val DISCLAIMER =
            "以上是依 AVMA、Merck Veterinary Manual 整理的緊急處置提示，不是診斷，也不能取代獸醫。請先打電話給獸醫或急診醫院，讓他們先準備好。"
    }
}

object RedFlagDetector {

    private val negations = listOf("沒有", "沒", "不是", "並非", "不", "無", "未", "不會")

    /** 疑問或推測句式：「不知道有沒有誤食」「懷疑中毒」要視為肯定，寧可多提示，不可漏報。 */
    private val uncertainForms = listOf("有沒有", "是不是", "會不會", "可不可能", "有可能", "可能", "懷疑", "疑似", "好像", "怕")

    /**
     * 偵測文字中的紅旗關鍵字。
     * 關鍵字緊接在否定詞後面（例如「沒有抽搐」）才略過；但若前面是疑問或推測句式則一律視為肯定。
     */
    fun detect(text: String): List<RedFlag> {
        if (text.isBlank()) return emptyList()
        return RedFlag.values().filter { flag ->
            flag.keywords.any { keyword -> containsAffirmed(text, keyword) }
        }
    }

    private fun containsAffirmed(text: String, keyword: String): Boolean {
        var from = 0
        while (true) {
            val i = text.indexOf(keyword, from)
            if (i < 0) return false
            val near = text.substring(maxOf(0, i - 4), i)
            val uncertain = uncertainForms.any { near.contains(it) }
            val negated = !uncertain && negations.any { near.endsWith(it) }
            if (!negated) return true
            from = i + keyword.length
        }
    }
}
