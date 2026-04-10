package com.arijit.budgettracker.sms

import java.text.Normalizer

object CategoryClassifier {

    private val CATEGORY_KEYWORDS = mapOf(
        "Ăn uống" to listOf(
            "an uong", "nha hang", "cafe", "coffee", "com", "bun", "pho",
            "food", "restaurant", "tra sua", "banh", "lau", "nuong"
        ),
        "Di chuyển" to listOf(
            "grab", "taxi", "xang", "gojek", "be", "parking", "giu xe",
            "uber", "bus", "ve xe", "toll"
        ),
        "Mua sắm" to listOf(
            "shopee", "lazada", "tiki", "sendo", "mua sam", "bach hoa",
            "vinmart", "winmart", "minimart", "store"
        ),
        "Giải trí" to listOf(
            "game", "netflix", "spotify", "phim", "cgv", "lotte", "karaoke",
            "music", "youtube", "cinema"
        ),
        "Hóa đơn" to listOf(
            "dien", "nuoc", "internet", "thue", "hoa don", "bill",
            "fpt", "vnpt", "viettel", "mobile", "dien thoai"
        ),
        "Sức khỏe" to listOf(
            "benh vien", "thuoc", "pharmacy", "kham", "nha thuoc",
            "hospital", "clinic", "bao hiem"
        ),
        "Giáo dục" to listOf(
            "hoc phi", "sach", "truong", "course", "hoc", "school",
            "university", "dai hoc", "lop"
        )
    )

    fun classify(smsContent: String): String {
        val normalized = removeDiacritics(smsContent.lowercase())

        for ((category, keywords) in CATEGORY_KEYWORDS) {
            for (keyword in keywords) {
                if (normalized.contains(keyword)) {
                    return category
                }
            }
        }

        return "Khác"
    }

    private fun removeDiacritics(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace("đ", "d").replace("Đ", "D")
    }
}
