package com.livemap.incidents.core

import javax.inject.Qualifier

/**
 * Qualifiers so Hilt can inject specific [kotlinx.coroutines.CoroutineDispatcher]s.
 * Injecting dispatchers (instead of hard-coding `Dispatchers.IO`) keeps code testable —
 * tests swap in a deterministic dispatcher.
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IoDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class DefaultDispatcher

/**
 * A [kotlinx.coroutines.CoroutineScope] that lives as long as the process. Used for work
 * that must outlive any single screen, such as the live incident feed.
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ApplicationScope
