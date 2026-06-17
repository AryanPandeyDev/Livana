package com.livana.app.core.network

import com.livana.app.core.common.BackendErrorCode
import com.livana.app.core.common.DomainError
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object ApiErrorParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(
        httpStatus: Int,
        errorBody: String?,
    ): DomainError {
        val body = errorBody?.trim()
        if (body.isNullOrEmpty()) {
            return DomainError.Http(httpStatus = httpStatus)
        }

        return try {
            val apiError = json.decodeFromString<ApiErrorDto>(body)
            val knownCode = BackendErrorCode.fromWireValue(apiError.errorCode)
            if (knownCode != null) {
                DomainError.Backend(
                    code = knownCode,
                    httpStatus = httpStatus,
                    message = apiError.message,
                    timestamp = apiError.timestamp,
                )
            } else {
                DomainError.Http(
                    httpStatus = httpStatus,
                    message = apiError.message,
                    errorCode = apiError.errorCode,
                    timestamp = apiError.timestamp,
                )
            }
        } catch (_: SerializationException) {
            DomainError.Http(httpStatus = httpStatus, message = body.take(MaxFallbackMessageLength))
        } catch (_: IllegalArgumentException) {
            DomainError.Http(httpStatus = httpStatus, message = body.take(MaxFallbackMessageLength))
        }
    }

    private const val MaxFallbackMessageLength = 200
}

@Serializable
private data class ApiErrorDto(
    val errorCode: String,
    val message: String,
    val timestamp: String? = null,
)
