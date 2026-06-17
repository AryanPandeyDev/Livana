package com.livana.app.core.network

import com.livana.app.core.common.BackendErrorCode
import com.livana.app.core.common.DomainError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiErrorParserTest {
    @Test
    fun `all documented backend error codes map to backend domain errors`() {
        BackendErrorCode.entries.forEach { code ->
            val error = ApiErrorParser.parse(
                httpStatus = code.defaultHttpStatus,
                errorBody = """
                    {
                      "errorCode": "${code.wireValue}",
                      "message": "message for ${code.wireValue}",
                      "timestamp": "2026-06-07T15:30:00.000+00:00"
                    }
                """.trimIndent(),
            )

            assertBackendError(
                error = error,
                code = code,
                httpStatus = code.defaultHttpStatus,
                message = "message for ${code.wireValue}",
            )
        }
    }

    @Test
    fun `parses validation error`() {
        val error = ApiErrorParser.parse(
            httpStatus = 400,
            errorBody = """
                {
                  "errorCode": "VALIDATION_ERROR",
                  "message": "title is required",
                  "timestamp": "2026-06-07T15:30:00.000+00:00"
                }
            """.trimIndent(),
        )

        assertBackendError(
            error = error,
            code = BackendErrorCode.ValidationError,
            httpStatus = 400,
            message = "title is required",
        )
    }

    @Test
    fun `parses wallet link error`() {
        val error = ApiErrorParser.parse(
            httpStatus = 409,
            errorBody = """
                {
                  "errorCode": "WALLET_ALREADY_LINKED",
                  "message": "Wallet already linked to another account",
                  "timestamp": "2026-06-07T15:30:00.000+00:00"
                }
            """.trimIndent(),
        )

        assertBackendError(
            error = error,
            code = BackendErrorCode.WalletAlreadyLinked,
            httpStatus = 409,
            message = "Wallet already linked to another account",
        )
    }

    @Test
    fun `parses upstream pinata error`() {
        val error = ApiErrorParser.parse(
            httpStatus = 502,
            errorBody = """
                {
                  "errorCode": "PINATA_UPSTREAM_ERROR",
                  "message": "Pinata returned a server error",
                  "timestamp": "2026-06-07T15:30:00.000+00:00"
                }
            """.trimIndent(),
        )

        assertBackendError(
            error = error,
            code = BackendErrorCode.PinataUpstreamError,
            httpStatus = 502,
            message = "Pinata returned a server error",
        )
    }

    @Test
    fun `unknown error code falls back to http error`() {
        val error = ApiErrorParser.parse(
            httpStatus = 418,
            errorBody = """
                {
                  "errorCode": "TEAPOT_MODE",
                  "message": "Unexpected backend code",
                  "timestamp": "2026-06-07T15:30:00.000+00:00"
                }
            """.trimIndent(),
        )

        assertTrue(error is DomainError.Http)
        val httpError = error as DomainError.Http
        assertEquals(418, httpError.httpStatus)
        assertEquals("TEAPOT_MODE", httpError.errorCode)
        assertEquals("Unexpected backend code", httpError.message)
        assertEquals("2026-06-07T15:30:00.000+00:00", httpError.timestamp)
    }

    @Test
    fun `plain http error falls back to http status`() {
        val error = ApiErrorParser.parse(
            httpStatus = 500,
            errorBody = "Internal Server Error",
        )

        assertTrue(error is DomainError.Http)
        val httpError = error as DomainError.Http
        assertEquals(500, httpError.httpStatus)
        assertEquals("Internal Server Error", httpError.message)
        assertNull(httpError.errorCode)
    }

    private fun assertBackendError(
        error: DomainError,
        code: BackendErrorCode,
        httpStatus: Int,
        message: String,
    ) {
        assertTrue(error is DomainError.Backend)
        val backendError = error as DomainError.Backend
        assertEquals(code, backendError.code)
        assertEquals(httpStatus, backendError.httpStatus)
        assertEquals(message, backendError.message)
        assertEquals("2026-06-07T15:30:00.000+00:00", backendError.timestamp)
    }
}
