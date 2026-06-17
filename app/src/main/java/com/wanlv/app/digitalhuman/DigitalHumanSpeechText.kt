package com.wanlv.app.digitalhuman

fun sanitizeDigitalHumanSpeechText(text: String): String =
    removeEmojiForSpeech(text)
        .replace(Regex("\\*\\*|\\*|#|\\[|\\]|\\(|\\)"), "")
        .replace(Regex("[ \\t]{2,}"), " ")
        .replace(Regex("\\s+\\n"), "\n")
        .trim()

private fun removeEmojiForSpeech(text: String): String {
    val builder = StringBuilder(text.length)
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        // 重点：数字人播报前过滤 emoji、肤色修饰符、国旗区域符号和零宽连接符，避免 TTS 读出异常字符。
        if (!isSpeechEmojiCodePoint(codePoint)) {
            builder.appendCodePoint(codePoint)
        }
        index += Character.charCount(codePoint)
    }
    return builder.toString()
}

private fun isSpeechEmojiCodePoint(codePoint: Int): Boolean =
    when {
        codePoint == 0xFE0F || codePoint == 0x200D -> true
        codePoint in 0x1F1E6..0x1F1FF -> true
        codePoint in 0x1F3FB..0x1F3FF -> true
        codePoint in 0x1F000..0x1FAFF -> true
        codePoint in 0x2600..0x27BF -> true
        codePoint in 0x2300..0x23FF -> true
        codePoint in 0x2B00..0x2BFF -> true
        codePoint in 0x2194..0x21AA -> true
        codePoint == 0x00A9 || codePoint == 0x00AE -> true
        codePoint == 0x203C || codePoint == 0x2049 || codePoint == 0x2122 || codePoint == 0x2139 -> true
        codePoint == 0x3030 || codePoint == 0x303D || codePoint == 0x3297 || codePoint == 0x3299 -> true
        else -> false
    }
