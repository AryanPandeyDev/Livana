package com.livana.app.core.data.repository

import com.livana.app.core.common.DomainError
import com.livana.app.core.common.LivanaResult
import com.livana.app.core.network.ApiErrorParser
import retrofit2.Response

internal inline fun <Network : Any, Domain> Response<Network>.toLivanaResult(
    mapper: (Network) -> Domain,
): LivanaResult<Domain> {
    if (!isSuccessful) {
        return LivanaResult.Failure(
            ApiErrorParser.parse(
                httpStatus = code(),
                errorBody = errorBody()?.string(),
            ),
        )
    }

    val responseBody = body()
        ?: return LivanaResult.Failure(DomainError.Serialization(message = "Response body was empty"))

    return LivanaResult.Success(mapper(responseBody))
}
