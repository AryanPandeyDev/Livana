package com.livana.app.core.common

sealed interface DomainError {
    val message: String?

    data class Network(
        override val message: String? = "Network unavailable",
        val cause: Throwable? = null,
    ) : DomainError

    data class Timeout(
        override val message: String? = "Request timed out",
        val cause: Throwable? = null,
    ) : DomainError

    data class Backend(
        val code: BackendErrorCode,
        val httpStatus: Int,
        override val message: String,
        val timestamp: String?,
    ) : DomainError

    data class Http(
        val httpStatus: Int,
        override val message: String? = null,
        val errorCode: String? = null,
        val timestamp: String? = null,
    ) : DomainError

    data class Serialization(
        override val message: String? = "Could not parse server response",
        val cause: Throwable? = null,
    ) : DomainError

    data class Unknown(
        override val message: String? = "Unexpected error",
        val cause: Throwable? = null,
    ) : DomainError
}
