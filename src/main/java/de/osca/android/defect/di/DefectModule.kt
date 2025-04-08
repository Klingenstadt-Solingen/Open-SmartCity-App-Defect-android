package de.osca.android.defect.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.osca.android.defect.data.DefectApiService
import de.osca.android.essentials.data.client.OSCAHttpClient
import javax.inject.Singleton

/**
 * The dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
class DefectModule {

    @Singleton
    @Provides
    fun defectApiService(oscaHttpClient: OSCAHttpClient): DefectApiService =
        oscaHttpClient.create(DefectApiService::class.java)

}