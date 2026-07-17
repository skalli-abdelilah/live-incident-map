package com.livemap.incidents.di

import com.livemap.incidents.data.repository.DefaultIncidentRepository
import com.livemap.incidents.data.repository.IncidentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /** Binds the interface to its implementation so callers depend only on the abstraction. */
    @Binds
    @Singleton
    abstract fun bindIncidentRepository(impl: DefaultIncidentRepository): IncidentRepository
}
