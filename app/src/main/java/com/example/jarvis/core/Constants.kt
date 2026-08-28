package com.example.jarvis.core

object JarvisConstants {
    const val DEFAULT_LOCALE = "az-AZ"
    const val FALLBACK_LOCALE = "en-US"
    
    const val DATABASE_NAME = "jarvis_assistant_db"
    
    const val RISK_CONFIRMATION_TIMEOUT_MS = 30_000L
    const val SPEECH_SILENCE_TIMEOUT_MS = 3_000L
    
    const val INTENT_LOCK_SCREEN = "LOCK_SCREEN"
    const val INTENT_OPEN_APP = "OPEN_APP"
    const val INTENT_READ_NOTIFICATIONS = "READ_NOTIFICATIONS"
    const val INTENT_GET_BATTERY = "GET_BATTERY"
    const val INTENT_GET_RAM = "GET_RAM"
    const val INTENT_GET_STORAGE = "GET_STORAGE"
    const val INTENT_OPEN_SETTINGS = "OPEN_SETTINGS"
    const val INTENT_SET_VOLUME = "SET_VOLUME"
    const val INTENT_TAKE_PHOTO = "TAKE_PHOTO"
    const val INTENT_CREATE_REMINDER = "CREATE_REMINDER"
    const val INTENT_TORCH = "TORCH"
    const val INTENT_GENERAL_CHAT = "GENERAL_CHAT"
    const val INTENT_UNKNOWN = "UNKNOWN"
}
