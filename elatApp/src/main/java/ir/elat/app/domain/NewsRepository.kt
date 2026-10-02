package ir.elat.app.domain

interface NewsRepository {
    suspend fun latest(): List<NewsArticle>
}
