package com.livana.app.core.common

enum class BackendErrorCode(
    val wireValue: String,
    val defaultHttpStatus: Int,
) {
    ValidationError("VALIDATION_ERROR", 400),
    InvalidAddress("INVALID_ADDRESS", 400),
    InvalidParameter("INVALID_PARAMETER", 400),
    InvalidRequest("INVALID_REQUEST", 400),
    ImageFileRequired("IMAGE_FILE_REQUIRED", 400),
    ImageTypeNotAllowed("IMAGE_TYPE_NOT_ALLOWED", 400),
    ImageTooLarge("IMAGE_TOO_LARGE", 400),
    PinataKeyRequired("PINATA_KEY_REQUIRED", 400),
    NoChallenge("NO_CHALLENGE", 400),
    ChallengeExpired("CHALLENGE_EXPIRED", 400),
    ChallengeMismatch("CHALLENGE_MISMATCH", 400),
    EmailNotVerified("EMAIL_NOT_VERIFIED", 400),
    UserNotFound("USER_NOT_FOUND", 404),
    PoolNotFound("POOL_NOT_FOUND", 404),
    ApplicationNotFound("APPLICATION_NOT_FOUND", 404),
    NoApplicationFound("NO_APPLICATION_FOUND", 404),
    NoActiveApplication("NO_ACTIVE_APPLICATION", 404),
    SignatureInvalid("SIGNATURE_INVALID", 403),
    WalletNotLinked("WALLET_NOT_LINKED", 403),
    NgoNotApproved("NGO_NOT_APPROVED", 403),
    ApplicationAlreadyExists("APPLICATION_ALREADY_EXISTS", 409),
    InvalidStatusTransition("INVALID_STATUS_TRANSITION", 409),
    WalletAlreadyLinked("WALLET_ALREADY_LINKED", 409),
    ClerkConfigError("CLERK_CONFIG_ERROR", 500),
    ClerkAuthError("CLERK_AUTH_ERROR", 500),
    ClerkUserNotFound("CLERK_USER_NOT_FOUND", 500),
    AiConfigKeyMissing("AI_CONFIG_KEY_MISSING", 500),
    AiConfigDecryptError("AI_CONFIG_DECRYPT_ERROR", 500),
    InternalError("INTERNAL_ERROR", 500),
    ClerkEmailCheckFailed("CLERK_EMAIL_CHECK_FAILED", 503),
    PinataUnauthorized("PINATA_UNAUTHORIZED", 400),
    PinataRejectedRequest("PINATA_REJECTED_REQUEST", 400),
    PinataUnreachable("PINATA_UNREACHABLE", 502),
    PinataUpstreamError("PINATA_UPSTREAM_ERROR", 502),
    PinataInvalidResponse("PINATA_INVALID_RESPONSE", 502);

    companion object {
        private val byWireValue = entries.associateBy(BackendErrorCode::wireValue)

        fun fromWireValue(wireValue: String): BackendErrorCode? = byWireValue[wireValue]
    }
}
