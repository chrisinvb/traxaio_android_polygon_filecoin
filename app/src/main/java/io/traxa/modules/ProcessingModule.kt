package io.traxa.modules

import io.traxa.services.FFmpeg
import io.traxa.services.FileProcessingService
import org.koin.dsl.module

val processingModule = module {
    single { provideFileProcessingService() }
    single { provideFFmpeg() }
}

fun provideFFmpeg() = FFmpeg()
fun provideFileProcessingService() = FileProcessingService()
