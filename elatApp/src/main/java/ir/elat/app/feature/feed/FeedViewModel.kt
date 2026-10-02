package ir.elat.app.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.elat.app.domain.NewsArticle
import ir.elat.app.domain.NewsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedState(
    val loading: Boolean = true,
    val articles: List<NewsArticle> = emptyList()
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: NewsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(FeedState())
    val state: StateFlow<FeedState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = FeedState(loading = false, articles = repository.latest())
        }
    }
}
