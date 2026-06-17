package com.livana.app.core.model

data class PagedResult<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean,
    val number: Int,
    val size: Int,
    val numberOfElements: Int,
    val empty: Boolean,
)
