package ir.elat.app.domain

data class NewsArticle(
    val id: String,
    val title: String,
    val summary: String,
    val category: String,
    val minutes: Int,
    val featured: Boolean = false
)

enum class ReadingLayer(val titleFa: String, val subtitleFa: String) {
    QUICK("سریع", "۳۰ ثانیه"),
    ANALYSIS("تحلیل", "۲ دقیقه"),
    DEEP("عمیق", "۵ دقیقه+")
}

enum class Reaction(val emoji: String) {
    SURPRISED("😮"),
    SAD("💔"),
    HOT("🔥"),
    THINKING("🤔"),
    CONFIRMED("✅")
}
