package io.traxa.modules

import android.content.Context
import androidx.room.Room
import io.traxa.persistence.AppDatabase
import io.traxa.services.Prefs
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val persistenceModule = module {
    single { Prefs(androidContext()) }
    single { provideRoom(androidContext()) }
}

fun provideRoom(context: Context) = Room.databaseBuilder(
    context,
    AppDatabase::class.java, "traxa"
).fallbackToDestructiveMigration()
    .build()
