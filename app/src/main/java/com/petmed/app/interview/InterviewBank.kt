package com.petmed.app.interview

enum class QType { TEXT, SINGLE, MULTI }
enum class Applies { BOTH, DOG, CAT }
enum class Species { DOG, CAT, OTHER }

/**
 * 一題問診題目。
 * - sourced = true：題目的結構或選項有文獻或表單出處（見 source）。
 * - sourced = false：我設計的補充題，**沒有出處，待獸醫審閱**。
 * - 所有中文措辭均為改寫，**尚未經獸醫審閱**。
 */
data class Question(
    val id: String,
    val module: String,
    val section: String,
    val label: String,
    val text: String,
    val type: QType,
    val options: List<String> = emptyList(),
    val source: String,
    val sourced: Boolean = true,
    val applies: Applies = Applies.BOTH,
    val hint: String = "",
    val flagOptions: Map<String, RedFlag> = emptyMap(),
    val scanRedFlag: Boolean = true,
    val showIf: ((Map<String, String>) -> Boolean)? = null,
    /** 非空代表這題適合請飼主拍照；內容是「要拍什麼」。拍照建議無文獻出處，只是方便獸醫參考。 */
    val photoHint: String = "",
    val photoTopic: String = ""
)

/** 複選答案的分隔符號。選項文字本身不可包含此符號（有單元測試檢查）。 */
const val MULTI_SEP = "；"

/** 取出 MULTI 答案為集合 */
fun Map<String, String>.picked(id: String): Set<String> =
    this[id]?.split(MULTI_SEP)?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()

fun Map<String, String>.has(id: String, option: String): Boolean = picked(id).contains(option) || this[id] == option

/**
 * 問診題庫。題目來源與完整說明見 `中化AI問診資料/整理/02～04`。
 * 出處簡稱：
 *  Merck 犬／Merck 貓＝Merck Veterinary Manual 飼主版；UTenn 年度＝U. Tennessee VMR-143；
 *  UTenn derm＝VMR075；TAMU＝Texas A&M 皮膚科 2013；Metro＝metro-vet.com 內科問卷（機構未載明）；
 *  Griffin／Bloom＝DVM360（DACVD）；WSAVA／OSU／UCD＝飲食史表；CC＝Calgary-Cambridge。
 */
object InterviewBank {

    const val SEC_COMPLAINT = "主訴與病程"
    const val SEC_SYMPTOM = "症狀細節"
    const val SEC_GENERAL = "全身狀況"
    const val SEC_DIET = "飲食與生活"
    const val SEC_MED = "用藥與預防"
    const val SEC_HISTORY = "過去病史與接觸史"
    const val SEC_OWNER = "飼主補充"

    val sectionOrder = listOf(SEC_COMPLAINT, SEC_SYMPTOM, SEC_GENERAL, SEC_DIET, SEC_MED, SEC_HISTORY, SEC_OWNER)

    // 狀況類別（依 Merck 飼主版「How can I tell if my dog/cat is sick」改寫）
    const val CAT_APPETITE = "不吃東西或食慾變差"
    const val CAT_ENERGY = "變得比較沒精神"
    const val CAT_VOMIT = "嘔吐（包含頻繁吐毛球）"
    const val CAT_STOOL = "腹瀉或排便異常"
    const val CAT_URINE = "排尿變多或變少，或喝水變多"
    const val CAT_RESP = "咳嗽、打噴嚏或呼吸有狀況"
    const val CAT_EYE = "眼睛有分泌物或看起來不舒服"
    const val CAT_EAR = "耳朵癢、甩頭或有味道"
    const val CAT_SKIN = "掉毛、很癢或皮膚有紅點"
    const val CAT_MOBILITY = "跛行或腳不敢著地"
    const val CAT_OTHER = "其他（我會自己描述）"

    private const val UNSOURCED = "無出處，我設計的補充題，待獸醫審閱"

    private fun q(
        id: String, module: String, section: String, label: String, text: String, type: QType,
        source: String, options: List<String> = emptyList(), sourced: Boolean = true,
        applies: Applies = Applies.BOTH, hint: String = "", flags: Map<String, RedFlag> = emptyMap(),
        scan: Boolean = true, showIf: ((Map<String, String>) -> Boolean)? = null
    ) = Question(id, module, section, label, text, type, options, source, sourced, applies, hint, flags, scan, showIf)

    // ───────── 開場 ─────────
    val complaint = q(
        "complaint", "INTRO", SEC_COMPLAINT, "主訴（飼主描述）",
        "請用自己的話描述一下，牠發生了什麼事？（例如：什麼時候開始、你看到了什麼）",
        QType.TEXT, "Merck 專業版（chief concern）；CC：先開放式提問"
    )

    val categories = q(
        "categories", "INTRO", SEC_COMPLAINT, "狀況類別",
        "我想確認一下，牠目前有哪些狀況？（可複選）",
        QType.MULTI, "Merck 犬／貓「How can I tell if my dog/cat is sick」清單（依其分類改寫）",
        options = listOf(
            CAT_APPETITE, CAT_ENERGY, CAT_VOMIT, CAT_STOOL, CAT_URINE, CAT_RESP,
            CAT_EYE, CAT_EAR, CAT_SKIN, CAT_MOBILITY, CAT_OTHER
        ),
        scan = false
    )

    // ───────── 全身簡篩（每次都問）─────────
    private val core = listOf(
        q("core_appetite", "CORE", SEC_GENERAL, "食慾", "最近的食慾，跟平常比起來怎麼樣？", QType.SINGLE,
            "UTenn 年度；Metro 2；OSU 12", options = listOf("正常", "增加", "減少")),
        q("core_appetite_dur", "CORE", SEC_GENERAL, "食慾變差多久", "食慾變差大概多久了？", QType.TEXT,
            "UTenn 年度；Metro 2c", showIf = { it["core_appetite"] == "減少" }),
        q("core_water", "CORE", SEC_GENERAL, "喝水量", "最近喝水量，跟平常比起來呢？", QType.SINGLE,
            "UTenn 年度；Metro 9d", options = listOf("正常", "變多", "變少")),
        q("core_energy", "CORE", SEC_GENERAL, "精神與活動力", "最近的精神和活動力呢？", QType.SINGLE,
            "UTenn 年度；Metro 8", options = listOf("比平常有精神", "正常", "比平常沒精神")),
        q("act_level", "CORE", SEC_GENERAL, "活動力約為平常的", "現在的活動力，大概是平常的百分之幾？", QType.TEXT, "Metro 8b",
            hint = "例如：50%", showIf = { it["core_energy"] == "比平常沒精神" }),
        q("act_dur", "CORE", SEC_GENERAL, "沒精神多久", "沒精神多久了？", QType.TEXT, "Metro 8c",
            showIf = { it["core_energy"] == "比平常沒精神" }),
        q("core_weight", "CORE", SEC_GENERAL, "體重變化", "有沒有「沒少吃卻變瘦」或「沒多吃卻變胖」的情況？", QType.SINGLE,
            "UTenn 年度；Metro 9c；OSU 11", options = listOf("沒有變化", "沒少吃卻變瘦", "沒多吃卻變胖")),
        q("core_bowel", "CORE", SEC_GENERAL, "大便", "最近的大便狀況？", QType.SINGLE,
            "UTenn 年度", options = listOf("正常", "軟便或腹瀉", "便秘")),
        q("core_urine", "CORE", SEC_GENERAL, "小便", "最近小便的次數或量，有變化嗎？", QType.SINGLE,
            "UTenn 年度；Metro 9f", options = listOf("沒有變化", "變多", "變少")),
        q("core_behavior", "CORE", SEC_GENERAL, "個性或行為", "個性或行為有沒有跟平常不一樣？", QType.SINGLE,
            "UTenn 年度；Metro 9g", options = listOf("沒有", "有")),
        q("core_behavior_desc", "CORE", SEC_GENERAL, "行為改變內容", "有什麼不一樣？", QType.TEXT,
            "UTenn 年度；Metro 9g", showIf = { it["core_behavior"] == "有" }),
        q("core_sleep", "CORE", SEC_GENERAL, "睡眠", "最近的睡眠有變化嗎？", QType.SINGLE,
            UNSOURCED + "（業師提出）", sourced = false, options = listOf("沒有變化", "睡比較多", "睡比較少"))
    )

    // ───────── 依狀況類別展開的模組 ─────────
    private val vomit = listOf(
        q("vomit_freq", "VOMIT", SEC_SYMPTOM, "嘔吐頻率", "大概一天或一週吐幾次？", QType.TEXT, "Metro 4b；UCD"),
        q("vomit_dur", "VOMIT", SEC_SYMPTOM, "嘔吐多久", "從什麼時候開始吐的？", QType.TEXT, "Metro 4c"),
        q("vomit_content", "VOMIT", SEC_SYMPTOM, "嘔吐物內容", "吐出來的東西像什麼？（可複選）", QType.MULTI, "Metro 4d",
            options = listOf("已消化的食物", "沒消化的食物", "泡沫", "黃綠色液體", "鮮紅色的血", "像咖啡渣", "其他")),
        q("vomit_food", "VOMIT", SEC_SYMPTOM, "吐之前一週換食物", "開始吐的前一週，有沒有換飼料或吃了新東西（含零食）？", QType.SINGLE, "Metro 4e",
            options = listOf("有", "沒有")),
        q("vomit_extra", "VOMIT", SEC_SYMPTOM, "伴隨狀況", "還有沒有這些情況？（可複選）", QType.MULTI, "Merck 犬／貓 消化系統簡介",
            options = listOf("一直流口水", "一直乾嘔", "肚子看起來脹脹的", "哀叫或姿勢怪異（可能是肚子痛）", "都沒有"))
    )

    private val stool = listOf(
        q("stool_pattern", "STOOL", SEC_SYMPTOM, "腹瀉模式", "是一直拉，還是時好時壞？", QType.SINGLE, "Metro 3b",
            options = listOf("一直在拉", "時好時壞")),
        q("stool_freq", "STOOL", SEC_SYMPTOM, "排便次數", "一天大概拉幾次？", QType.TEXT, "Metro 3b；Griffin Q16"),
        q("stool_dur", "STOOL", SEC_SYMPTOM, "腹瀉多久", "拉多久了？", QType.TEXT, "Metro 3c"),
        q("stool_type", "STOOL", SEC_SYMPTOM, "大便性狀", "大便的樣子比較像哪一種？", QType.SINGLE, "Metro 3d",
            options = listOf("水樣", "軟但還成形", "軟爛成一坨（像牛糞）", "其他")),
        q("stool_blood", "STOOL", SEC_SYMPTOM, "黏液或血", "大便裡有沒有看到黏液或血？", QType.SINGLE, "Metro 3e；Griffin Q19",
            options = listOf("沒有", "有黏液", "有鮮血", "黏液和血都有")),
        q("stool_color", "STOOL", SEC_SYMPTOM, "大便顏色", "大便是什麼顏色？是平常的顏色嗎？", QType.TEXT, "Metro 3f"),
        q("stool_food", "STOOL", SEC_SYMPTOM, "拉之前一週換食物", "開始拉的前一週，有沒有換食物或吃新東西（含零食）？", QType.SINGLE, "Metro 3g",
            options = listOf("有", "沒有")),
        q("stool_extra", "STOOL", SEC_SYMPTOM, "伴隨狀況", "還有沒有這些情況？（可複選）", QType.MULTI, "Griffin Q17,18,20；Merck 犬／貓 消化系統簡介",
            options = listOf("很常放屁", "肚子咕嚕咕嚕叫", "打嗝", "用力排便卻排不出來", "都沒有"))
    )

    private val urine = listOf(
        q("urine_volume", "URINE", SEC_SYMPTOM, "尿量", "每次尿的量有變多嗎？", QType.SINGLE, "Metro 9e", options = listOf("沒有變", "變多")),
        q("urine_signs", "URINE", SEC_SYMPTOM, "排尿狀況", "有沒有這些情況？（可複選）", QType.MULTI, "Metro 9f",
            options = listOf("尿得更頻繁", "用力排尿", "滴尿或漏尿", "尿的顏色不對", "尿的味道不對", "都沒有")),
        q("urine_dur", "URINE", SEC_SYMPTOM, "排尿異常多久", "這種情況多久了？", QType.TEXT, UNSOURCED, sourced = false)
    )

    private val resp = listOf(
        q("resp_which", "RESP", SEC_SYMPTOM, "呼吸道狀況", "有哪些情況？（可複選）", QType.MULTI, "Metro 5,6,7",
            options = listOf("咳嗽", "打噴嚏", "流鼻水或鼻血", "呼吸看起來很吃力", "都沒有"),
            flags = mapOf("呼吸看起來很吃力" to RedFlag.BREATHING)),
        q("cough_freq", "RESP", SEC_SYMPTOM, "咳嗽頻率", "一天大概咳幾次？每次會咳多久？", QType.TEXT, "Metro 5c-d",
            showIf = { it.has("resp_which", "咳嗽") }),
        q("cough_dur", "RESP", SEC_SYMPTOM, "咳嗽多久", "咳多久了？", QType.TEXT, "Metro 5e",
            showIf = { it.has("resp_which", "咳嗽") }),
        q("cough_time", "RESP", SEC_SYMPTOM, "咳嗽時段", "白天還是晚上比較嚴重？", QType.SINGLE, "Metro 5f",
            options = listOf("白天比較嚴重", "晚上比較嚴重", "差不多"), showIf = { it.has("resp_which", "咳嗽") }),
        q("cough_activity", "RESP", SEC_SYMPTOM, "咳嗽與活動", "運動時還是休息時比較嚴重？", QType.SINGLE, "Metro 5g",
            options = listOf("運動時比較嚴重", "休息時比較嚴重", "差不多"), showIf = { it.has("resp_which", "咳嗽") }),
        q("cough_sound", "RESP", SEC_SYMPTOM, "咳嗽聲音", "咳嗽的聲音比較像哪一種？", QType.SINGLE, "Metro 5h-i",
            options = listOf("輕柔", "粗啞", "像鵝叫"), showIf = { it.has("resp_which", "咳嗽") }),
        q("cough_wet", "RESP", SEC_SYMPTOM, "有無痰", "咳得出痰嗎？", QType.SINGLE, "Metro 5j；UTenn 年度",
            options = listOf("咳得出痰", "乾咳"), showIf = { it.has("resp_which", "咳嗽") }),
        q("nose_desc", "RESP", SEC_SYMPTOM, "鼻分泌物", "鼻子分泌物的顏色和濃稠度如何？有沒有血？", QType.TEXT, "Metro 6a",
            showIf = { it.has("resp_which", "流鼻水或鼻血") }),
        q("sneeze_freq", "RESP", SEC_SYMPTOM, "打噴嚏", "一天大概打幾次噴嚏？多久了？", QType.TEXT, "Metro 6d-e",
            showIf = { it.has("resp_which", "打噴嚏") }),
        q("resp_mouth", "RESP", SEC_SYMPTOM, "舌頭或牙齦顏色", "牠的舌頭或牙齦，有沒有變藍紫色？", QType.SINGLE, "Metro 7e；AVMA（噎住徵兆）",
            options = listOf("沒有", "有"), flags = mapOf("有" to RedFlag.BLUE_MOUTH))
    )

    private val eye = listOf(
        q("eye_side", "EYE", SEC_SYMPTOM, "哪一邊的眼睛", "是哪一邊的眼睛？", QType.SINGLE, UNSOURCED, sourced = false,
            options = listOf("左眼", "右眼", "兩邊")),
        q("eye_signs", "EYE", SEC_SYMPTOM, "眼睛狀況", "眼睛有哪些情況？（可複選）", QType.MULTI,
            "雪貂版 Merck 表（待確認適用犬貓）；Merck 犬貓緊急處置（眼外傷）", sourced = false,
            options = listOf("有分泌物", "一直瞇著眼", "看起來混濁", "好像看不見", "眼睛看起來受傷", "其他"),
            flags = mapOf("眼睛看起來受傷" to RedFlag.EYE_INJURY)),
        q("eye_dur", "EYE", SEC_SYMPTOM, "眼睛狀況多久", "這種情況多久了？", QType.TEXT, UNSOURCED, sourced = false)
    )

    private val ear = listOf(
        q("ear_first", "EAR", SEC_SYMPTOM, "怎麼發現耳朵有問題", "你是怎麼發現耳朵有問題的？", QType.TEXT, "Griffin Q1"),
        q("ear_signs", "EAR", SEC_SYMPTOM, "耳朵狀況", "有看到這些情況嗎？（可複選）", QType.MULTI, "Griffin Q2,3,5；UTenn 年度；Merck 貓（耳朵癢或髒）",
            options = listOf("一直甩頭", "一直抓耳朵", "耳朵有味道", "耳朵有分泌物", "都沒有")),
        q("ear_side", "EAR", SEC_SYMPTOM, "哪一邊耳朵", "是哪一邊的耳朵？", QType.SINGLE, UNSOURCED, sourced = false,
            options = listOf("左耳", "右耳", "兩邊")),
        q("ear_first_ever", "EAR", SEC_SYMPTOM, "第一次出現症狀", "耳朵的症狀，最早是什麼時候出現的？（不是這一次，是第一次）", QType.TEXT, "Bloom Q1"),
        q("ear_prev", "EAR", SEC_SYMPTOM, "以前有耳朵問題", "以前有過耳朵問題嗎？", QType.SINGLE, "Bloom Q2；TAMU", options = listOf("沒有", "有過")),
        q("ear_prev_detail", "EAR", SEC_SYMPTOM, "以前的處理", "當時是什麼時候、用了什麼藥、效果如何？", QType.TEXT, "Bloom Q2",
            showIf = { it["ear_prev"] == "有過" }),
        q("ear_clean", "EAR", SEC_SYMPTOM, "清耳朵習慣", "你會幫牠清耳朵嗎？", QType.SINGLE, "Griffin Q6-8,10-11",
            options = listOf("不會清", "定期清", "有症狀才清")),
        q("ear_meds", "EAR", SEC_SYMPTOM, "用過的耳朵產品", "用過哪些耳朵的產品？（可複選）", QType.MULTI, "Griffin Q12",
            options = listOf("清潔液", "滴耳藥水", "藥膏", "沒有用過")),
        q("ear_other_itch", "EAR", SEC_SYMPTOM, "其他地方也癢", "有沒有這些情況？（可複選）", QType.MULTI, "Griffin Q13-15",
            options = listOf("身體其他地方也癢", "一直舔腳掌", "都沒有")),
        q("ear_pet_lick", "EAR", SEC_SYMPTOM, "其他寵物舔耳朵", "有其他寵物會舔牠的耳朵嗎？", QType.SINGLE, "Griffin Q4",
            options = listOf("有", "沒有", "沒有其他寵物")),
        q("ear_today", "EAR", SEC_SYMPTOM, "今天的狀況", "今天耳朵的狀況，跟發病以來比起來？", QType.SINGLE, "Bloom Q10",
            options = listOf("是最好的", "是最差的", "差不多")),
        q("ear_season", "EAR", SEC_SYMPTOM, "季節影響", "症狀會因為季節而比較嚴重嗎？", QType.SINGLE, "Bloom Q11",
            options = listOf("某些季節比較嚴重", "全年差不多"))
    )

    private val bodyParts = listOf("臉", "耳朵", "胸", "肚子", "背", "屁股", "尾巴", "四肢", "腳掌", "其他")

    private val skin = listOf(
        q("skin_itch", "SKIN", SEC_SYMPTOM, "搔癢行為", "牠有癢的樣子嗎？怎麼表現？（可複選）", QType.MULTI, "UTenn derm；TAMU",
            options = listOf("抓", "磨蹭", "啃咬", "舔", "甩頭", "屁股拖地", "不癢")),
        q("skin_where", "SKIN", SEC_SYMPTOM, "哪些部位", "哪些部位有問題？（可複選）", QType.MULTI, "TAMU（部位清單）", options = bodyParts),
        q("skin_first_where", "SKIN", SEC_SYMPTOM, "最先出現的部位", "最先出現問題的是哪個部位？", QType.SINGLE, "TAMU", options = bodyParts),
        q("skin_onset", "SKIN", SEC_SYMPTOM, "發生方式", "是突然出現，還是慢慢出現的？", QType.SINGLE, "TAMU；Merck 專業版（onset）",
            options = listOf("突然", "慢慢出現")),
        q("skin_dur", "SKIN", SEC_SYMPTOM, "持續多久", "皮膚的問題持續多久了？", QType.TEXT, "UTenn derm"),
        q("skin_spread", "SKIN", SEC_SYMPTOM, "有無擴散", "有擴散，或樣子有變嗎？", QType.SINGLE, "TAMU", options = listOf("有", "沒有")),
        q("skin_look", "SKIN", SEC_SYMPTOM, "外觀", "皮膚或毛髮看起來怎麼了？（顏色、質地、紅點、掉毛）", QType.TEXT, "UTenn derm"),
        q("skin_season", "SKIN", SEC_SYMPTOM, "季節影響", "某些季節會比較嚴重嗎？", QType.SINGLE, "UTenn derm；TAMU；Bloom Q11",
            options = listOf("某些季節比較嚴重", "全年差不多")),
        q("skin_fleas", "SKIN", SEC_SYMPTOM, "跳蚤", "有看過跳蚤嗎？", QType.SINGLE, "TAMU", options = listOf("有看過", "沒看過")),
        q("skin_treat", "SKIN", SEC_SYMPTOM, "做過的處理", "為皮膚問題做過哪些處理？哪個效果最好？", QType.TEXT, "UTenn derm"),
        q("skin_bath", "SKIN", SEC_SYMPTOM, "洗澡習慣", "多久洗一次澡？用什麼洗毛精？", QType.TEXT, "UTenn derm"),
        q("skin_household", "SKIN", SEC_SYMPTOM, "同住者症狀", "同住的寵物或家人，有沒有類似的皮膚問題？", QType.SINGLE, "UTenn derm；Bloom Q5-6",
            options = listOf("有", "沒有"))
    )

    private val mobility = listOf(
        q("mob_limp", "MOBILITY", SEC_SYMPTOM, "跛行的腳", "是哪一隻腳？（可複選）", QType.MULTI, "UTenn 年度（RF/LF/RR/LR）",
            options = listOf("右前腳", "左前腳", "右後腳", "左後腳", "不確定")),
        q("mob_rise", "MOBILITY", SEC_SYMPTOM, "起身", "起身有困難嗎？", QType.SINGLE, "UTenn 年度", options = listOf("沒有", "有")),
        q("mob_stiff", "MOBILITY", SEC_SYMPTOM, "僵硬", "走路或起身時，看起來有僵硬感嗎？", QType.SINGLE, "Merck 貓（Stiffness）",
            options = listOf("沒有", "有"), applies = Applies.CAT),
        q("mob_dur", "MOBILITY", SEC_SYMPTOM, "跛行多久", "這種情況多久了？", QType.TEXT, UNSOURCED, sourced = false)
    )

    // ───────── 用藥與預防（每次都問）─────────
    private val meds = listOf(
        q("med_current", "MED", SEC_MED, "目前用藥", "目前有在吃或用任何藥嗎？（含成藥、保健品）", QType.SINGLE, "UTenn 年度；OSU 15；UCD",
            options = listOf("沒有", "有")),
        q("med_list", "MED", SEC_MED, "藥品名稱", "請列出藥名或產品名稱（可以看藥袋或藥盒）", QType.TEXT, "OSU 15；UCD",
            scan = false, showIf = { it["med_current"] == "有" }, hint = "之後可以拍藥袋取代打字"),
        q("med_tried", "MED", SEC_MED, "已做的處理", "為了這次的問題，你已經做過什麼處理？效果如何？", QType.TEXT, "UTenn derm；TAMU；Merck 專業版（藥物與反應）"),
        q("med_other_clinic", "MED", SEC_MED, "其他醫院治療", "這個問題，目前有在別家動物醫院治療嗎？", QType.SINGLE, "UTenn 年度",
            options = listOf("沒有", "有")),
        q("med_react", "MED", SEC_MED, "藥物或疫苗反應", "以前對任何藥物或疫苗有過不良反應嗎？", QType.SINGLE, "UTenn 年度；Metro 9k",
            options = listOf("沒有", "有過")),
        q("med_react_detail", "MED", SEC_MED, "不良反應內容", "是什麼藥或疫苗？發生什麼事？", QType.TEXT, "UTenn 年度",
            showIf = { it["med_react"] == "有過" }),
        q("prev_parasite", "MED", SEC_MED, "預防性驅蟲", "牠有定期做預防性驅蟲嗎？（跳蚤蜱蟲、心絲蟲、腸內寄生蟲）", QType.SINGLE,
            "UTenn derm；UTenn 年度（美國表單，台灣適用性待確認）", sourced = false,
            options = listOf("有定期做", "不定期", "沒有", "不確定")),
        q("prev_parasite_detail", "MED", SEC_MED, "驅蟲產品", "用什麼產品？上次是什麼時候？", QType.TEXT, "UTenn derm",
            scan = false, showIf = { it["prev_parasite"] == "有定期做" || it["prev_parasite"] == "不定期" }),
        q("vaccine", "MED", SEC_MED, "疫苗", "疫苗接種的狀況？", QType.SINGLE, "UTenn 年度；Merck 專業版（僅限神經症狀患者）", sourced = false,
            options = listOf("有按時接種", "有接種，但不確定是否過期", "沒有", "不確定")),
        q("vaccine_last", "MED", SEC_MED, "最近一次疫苗", "最近一次打疫苗大約是什麼時候？（可以看疫苗手冊）", QType.TEXT, "UTenn 年度",
            scan = false, showIf = { it["vaccine"] == "有按時接種" || it["vaccine"] == "有接種，但不確定是否過期" },
            hint = "之後可以拍疫苗手冊取代打字")
    )

    // ───────── 飲食簡篩（WSAVA）─────────
    private val dietScreen = listOf(
        q("diet_activity", "DIET", SEC_DIET, "好動程度", "您的寵物好動嗎？", QType.SINGLE, "WSAVA 飲食記錄簡表（繁中原文）",
            options = listOf("非常好動", "好動", "不怎麼好動")),
        q("diet_weight", "DIET", SEC_DIET, "體重自評", "您認為您的寵物體重…", QType.SINGLE, "WSAVA 飲食記錄簡表（繁中原文）",
            options = listOf("過重", "正常", "過輕")),
        q("diet_housing", "DIET", SEC_DIET, "居住環境", "您的寵物大部分時間待在哪裡？", QType.SINGLE, "WSAVA（繁中原文）；OSU 1；UCD",
            options = listOf("屋內", "屋外", "屋內及屋外")),
        q("diet_main", "DIET", SEC_DIET, "主食類型", "牠平常吃的東西，有沒有包含這些？（可複選）", QType.MULTI, "WSAVA 營養評估檢核表（非典型飲食）",
            options = listOf("一般市售飼料或罐頭", "自製食物", "生肉", "素食", "不常見的食物")),
        q("diet_list", "DIET", SEC_DIET, "目前吃的所有東西",
            "請列出牠現在吃的所有東西：品牌或名稱、大概每次多少、一天幾次。包括零食、人吃的食物、潔牙棒，以及用來包藥的食物。",
            QType.TEXT, "WSAVA（繁中原文）；OSU 14；UCD", scan = false, hint = "之後可以拍包裝取代打字"),
        q("diet_treat", "DIET", SEC_DIET, "零食與人吃的食物比例", "零食和人吃的食物，大概佔牠每天吃的多少？", QType.SINGLE,
            "WSAVA 營養評估檢核表（>10% 為風險因子；飼主版選項為我改寫）",
            options = listOf("幾乎沒有", "少量", "大約一成", "超過一成")),
        q("diet_supp", "DIET", SEC_DIET, "營養品", "有餵營養品嗎？（維他命、關節保養品、脂肪酸等）", QType.SINGLE, "WSAVA（繁中原文）；OSU 9；UCD",
            options = listOf("沒有", "有")),
        q("diet_supp_list", "DIET", SEC_DIET, "營養品品牌與用量", "請列出商品名與用量", QType.TEXT, "WSAVA（繁中原文）",
            scan = false, showIf = { it["diet_supp"] == "有" })
    )

    // ───────── 飲食深問（觸發 WSAVA 風險因子才問）─────────
    private val dietDeep = listOf(
        q("dd_change", "DIET_DEEP", SEC_DIET, "近四週飲食改變", "最近 4 週有換過飲食嗎？", QType.SINGLE, "OSU 13", options = listOf("沒有", "有")),
        q("dd_change_detail", "DIET_DEEP", SEC_DIET, "換了什麼與原因", "換了什麼？為什麼換？", QType.TEXT, "OSU 13",
            scan = false, showIf = { it["dd_change"] == "有" }),
        q("dd_past", "DIET_DEEP", SEC_DIET, "以前吃過的食物", "以前吃過、後來沒再吃的食物有哪些？為什麼停？", QType.TEXT, "OSU 17；UCD", scan = false),
        q("dd_times", "DIET_DEEP", SEC_DIET, "餵食次數", "一天餵幾次？", QType.SINGLE, "OSU 7",
            options = listOf("一次", "兩次", "三次", "超過三次", "整天放著")),
        q("dd_finish", "DIET_DEEP", SEC_DIET, "吃得完嗎", "給的食物，牠吃得完嗎？", QType.SINGLE, "OSU 8", options = listOf("吃得完", "常常剩下")),
        q("dd_others", "DIET_DEEP", SEC_DIET, "其他寵物與食物", "家裡其他寵物，和牠吃飯的情況？", QType.SINGLE, "OSU 3,4",
            options = listOf("沒有其他寵物", "有其他寵物，但吃不到彼此的食物", "有其他寵物，而且會吃到彼此的食物")),
        q("dd_unmonitored", "DIET_DEEP", SEC_DIET, "沒人看管的食物", "牠有機會吃到沒人看管的食物嗎？（例如鄰居給的、其他寵物的飼料）", QType.SINGLE, "UCD",
            options = listOf("沒有", "有")),
        q("dd_chew", "DIET_DEEP", SEC_DIET, "咀嚼吞嚥", "吃東西時，咀嚼或吞嚥有困難嗎？", QType.SINGLE, "OSU 10；UCD",
            options = listOf("沒有", "咀嚼有困難", "吞嚥有困難", "兩者都有"))
    )

    // ───────── 過去病史與接觸史 ─────────
    private val history = listOf(
        q("hist_conditions", "HIST", SEC_HISTORY, "過去與目前的病", "牠目前或以前有過什麼病嗎？有沒有已經好了？", QType.TEXT, "UCD；UTenn derm"),
        q("hist_neutered", "HIST", SEC_HISTORY, "結紮", "有結紮嗎？", QType.SINGLE, "Metro 1j；UTenn 年度",
            options = listOf("已結紮", "未結紮", "不確定")),
        q("hist_surgery", "HIST", SEC_HISTORY, "手術史", "除了結紮之外，有動過手術嗎？什麼手術、什麼時候？", QType.TEXT, "Metro 1k"),
        q("hist_30d", "HIST", SEC_HISTORY, "近 30 天受傷或手術", "過去 30 天內，有受傷或動手術嗎？", QType.SINGLE, "UTenn 年度",
            options = listOf("沒有", "有受傷", "有動手術", "兩者都有")),
        q("exp_boarding", "HIST", SEC_HISTORY, "寄宿或住院", "最近一個月，有寄宿或住院過嗎？", QType.SINGLE, "Metro 1d",
            options = listOf("沒有", "寄宿", "住院")),
        q("exp_travel", "HIST", SEC_HISTORY, "旅行", "最近有去過其他縣市或國外嗎？去哪裡、什麼時候？", QType.TEXT, "Metro 9j；TAMU",
            scan = false),
        q("exp_contact", "HIST", SEC_HISTORY, "接觸環境", "牠最近有這些情況嗎？（可複選）", QType.MULTI, "UTenn 年度（生活型態清單）；Bloom Q7",
            options = listOf("接觸其他動物", "去美容或訓練班", "戶外活動（公園、草叢、水邊）", "都沒有")),
        q("exp_toxin", "HIST", SEC_HISTORY, "毒物或有害物", "牠有沒有可能吃到或碰到不該碰的東西？", QType.SINGLE, "Merck 專業版（toxins）；AVMA（疑似即聯絡獸醫）",
            options = listOf("沒有", "有可能", "不確定"), flags = mapOf("有可能" to RedFlag.POISON)),
        q("exp_household", "HIST", SEC_HISTORY, "同住者症狀", "同住的其他動物或家人，有類似症狀嗎？", QType.SINGLE, "UTenn derm；Bloom Q5-6",
            options = listOf("有", "沒有", "不確定"))
    )

    // ───────── 飼主補充（Calgary-Cambridge：飼主觀點）─────────
    private val closing = listOf(
        q("own_concern", "CLOSE", SEC_OWNER, "飼主最擔心的事", "你最擔心的是什麼？", QType.TEXT, "CC：飼主觀點（patient's perspective）"),
        q("own_extra", "CLOSE", SEC_OWNER, "想讓獸醫知道的事", "還有什麼想讓獸醫知道的事嗎？", QType.TEXT, "CC：飼主觀點（patient's perspective）")
    )

    /**
     * 哪些題目適合請飼主拍照（題目 id → 要拍什麼 to 相簿主題）。
     * 依據：AVMA 提到毒物要保留產品容器與嘔吐物；其餘為便利性建議，無文獻，不是診斷。
     */
    private val photoMap = mapOf(
        "vomit_content" to ("嘔吐物（如果還留著）" to "嘔吐物"),
        "stool_color" to ("大便" to "糞便"),
        "skin_look" to ("皮膚或毛髮有狀況的地方" to "皮膚或毛髮"),
        "ear_signs" to ("耳朵" to "耳朵"),
        "eye_signs" to ("眼睛" to "眼睛"),
        "med_list" to ("藥袋或藥盒上的標籤" to "藥袋或標籤"),
        "vaccine_last" to ("疫苗手冊" to "疫苗手冊"),
        "diet_list" to ("飼料與零食的包裝" to "飼料或零食包裝"),
        "diet_supp_list" to ("營養品的標籤" to "藥袋或標籤")
    )

    private fun decorate(q: Question): Question =
        photoMap[q.id]?.let { q.copy(photoHint = it.first, photoTopic = it.second) } ?: q

    val all: List<Question> = (listOf(complaint, categories) + core + vomit + stool + urine + resp + eye + ear + skin +
        mobility + meds + dietScreen + dietDeep + history + closing).map { decorate(it) }

    private val byId: Map<String, Question> = all.associateBy { it.id }
    fun get(id: String): Question? = byId[id]

    /** 依欄位所屬分區與題庫順序計算穩定的排序值 */
    fun sortOrderOf(q: Question): Int =
        sectionOrder.indexOf(q.section).coerceAtLeast(0) * 1000 + all.indexOf(q)

    private fun modulesFor(selected: Set<String>): List<List<Question>> {
        val list = mutableListOf<List<Question>>()
        if (CAT_VOMIT in selected) list += vomit
        if (CAT_STOOL in selected) list += stool
        if (CAT_URINE in selected) list += urine
        if (CAT_RESP in selected) list += resp
        if (CAT_EYE in selected) list += eye
        if (CAT_EAR in selected) list += ear
        if (CAT_SKIN in selected) list += skin
        if (CAT_MOBILITY in selected) list += mobility
        return list
    }

    /** WSAVA「必須進一步評估」的風險因子（其中飼主可回答者） */
    fun dietDeepTriggered(a: Map<String, String>): Boolean {
        val cats = a.picked("categories")
        val gi = CAT_VOMIT in cats || CAT_STOOL in cats || a["core_bowel"] == "軟便或腹瀉" || a["core_bowel"] == "便秘"
        val meds = a["med_current"] == "有" || a["diet_supp"] == "有"
        val unconventional = a.picked("diet_main").any { it != "一般市售飼料或罐頭" }
        val treats = a["diet_treat"] == "超過一成"
        return gi || meds || unconventional || treats
    }

    private fun appliesTo(q: Question, species: Species) = when (q.applies) {
        Applies.BOTH -> true
        Applies.DOG -> species == Species.DOG
        Applies.CAT -> species == Species.CAT
    }

    /**
     * 依目前答案，產生「完整的題目計畫」（含已答題目）。
     * 開場兩題之後，才展開全身簡篩、各系統模組、用藥、飲食、病史與飼主補充。
     */
    fun plan(answers: Map<String, String>, species: Species): List<Question> {
        val result = mutableListOf<Question>()
        result += complaint
        result += categories
        if (!answers.containsKey("categories")) return result
        val selected = answers.picked("categories")
        result += core
        modulesFor(selected).forEach { result += it }
        result += meds
        result += dietScreen
        if (dietDeepTriggered(answers)) result += dietDeep
        result += history
        result += closing
        return result.filter { q ->
            appliesTo(q, species) && (q.showIf?.invoke(answers) ?: true)
        }.map { decorate(it) }
    }
}
