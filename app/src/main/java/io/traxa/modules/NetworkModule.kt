package io.traxa.modules

import android.content.Context
import io.traxa.repositories.PlayerTokenRepository
import io.traxa.services.network.AwsService
import io.traxa.services.network.StardustService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val networkModule = module {
    single { provideAwsApi(androidContext()) }
    single { PlayerTokenRepository(androidContext()) }
    single { StardustService() }
}

fun provideAwsApi(context: Context) = AwsService(context)