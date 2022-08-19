package io.traxa.modules

import android.content.Context
import androidx.work.WorkManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single { provideWorkManager(androidContext()) }
}

fun provideWorkManager(context: Context) = WorkManager.getInstance(context)
