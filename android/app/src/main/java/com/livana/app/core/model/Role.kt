package com.livana.app.core.model

/**
 * Server-assigned user role (api ref §1). Roles are authoritative on the backend and cannot be
 * changed by the client. Unknown/unexpected wire values fall back to [USER], the default role.
 */
enum class Role {
    USER,
    NGO,
    ADMIN,
    ;

    companion object {
        fun fromApi(value: String): Role =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: USER
    }
}
