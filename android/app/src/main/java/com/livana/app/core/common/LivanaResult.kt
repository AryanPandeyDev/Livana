package com.livana.app.core.common

sealed interface LivanaResult<out T> {
    data class Success<T>(val value: T) : LivanaResult<T>

    data class Failure(val error: DomainError) : LivanaResult<Nothing>
}
