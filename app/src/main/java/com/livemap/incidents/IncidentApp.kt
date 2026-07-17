package com.livemap.incidents

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. [HiltAndroidApp] triggers Hilt's code generation and
 * creates the app-level dependency container that survives the whole process.
 */
@HiltAndroidApp
class IncidentApp : Application()
