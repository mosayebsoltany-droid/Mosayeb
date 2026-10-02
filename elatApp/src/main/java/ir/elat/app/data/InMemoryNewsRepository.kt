package ir.elat.app.data

import ir.elat.app.domain.NewsArticle
import ir.elat.app.domain.NewsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryNewsRepository @Inject constructor() : NewsRepository {
    override suspend fun latest(): List<NewsArticle> = listOf(
        NewsArticle(
            id = "oil",
            title = "تحلیل بازار جهانی نفت در مسیر تغییر",
            summary = "فرصت‌ها، ریسک‌ها و سناریوهای پیش‌رو برای فعالان اقتصادی",
            category = "نفت و انرژی",
            minutes = 5,
            featured = true
        ),
        NewsArticle("exports", "رشد صادرات غیرنفتی ایران در نیمه نخست سال", "بررسی اثر تجارت خارجی بر بازار و تولید", "اقتصاد ایران", 2),
        NewsArticle("gold", "قیمت جهانی طلا به رکورد تازه رسید", "چه عواملی روند فلزات گران‌بها را تغییر می‌دهند؟", "ارز و طلا", 2),
        NewsArticle("bank", "تحولات سیاست پولی و نرخ بهره", "تحلیل پیامدهای تصمیمات بانکی برای کسب‌وکارها", "بانک و بیمه", 3)
    )
}
