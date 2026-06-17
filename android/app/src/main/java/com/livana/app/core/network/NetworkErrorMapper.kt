package com.livana.app.core.network

import com.livana.app.core.common.DomainError
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.serialization.SerializationException

fun Throwable.toDomainError(): DomainError = when (this) {
    is SocketTimeoutException -> DomainError.Timeout(cause = this)
    is UnknownHostException,
    is ConnectException,
    is SocketException,
    -> DomainError.Network(cause = this)
    is SerializationException -> DomainError.Serialization(cause = this)
    else -> DomainError.Unknown(message = message, cause = this)
}
