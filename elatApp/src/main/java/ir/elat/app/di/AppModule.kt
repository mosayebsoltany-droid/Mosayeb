package ir.elat.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.elat.app.data.InMemoryNewsRepository
import ir.elat.app.domain.NewsRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    abstract fun bindNewsRepository(impl: InMemoryNewsRepository): NewsRepository
}
