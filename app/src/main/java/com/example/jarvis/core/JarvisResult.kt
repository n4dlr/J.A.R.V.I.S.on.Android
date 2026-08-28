package com.example.jarvis.core

sealed class JarvisResult<out T> {
    data class Success<out T>(val data: T) : JarvisResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : JarvisResult<Nothing>()
    data object Loading : JarvisResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data
    fun getOrDefault(defaultValue: @UnsafeVariance T): T = (this as? Success)?.data ?: defaultValue
}
