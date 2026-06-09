package com.livana.backend.pool.preparation.service;

import com.livana.backend.pool.preparation.exception.PinataKeyRequiredException;
import jakarta.servlet.http.HttpServletRequest;

import java.net.http.HttpRequest;
import java.util.Arrays;

/**
 * Request-scoped value type carrying the NGO's Pinata API credentials.
 *
 * <p>Pinata's dashboard exposes three values when an NGO generates a key:
 * the API key, the API secret, and a JWT. We accept the API-key/secret pair
 * because that is the form NGO operators are familiar with from the Pinata
 * "Keys" page and the form a frontend prompt naturally asks for. The JWT
 * form is documented by Pinata but is rarely surfaced to non-developer users,
 * so we don't ask for it here.
 *
 * <p>If Pinata ever deprecates legacy api-key/secret authentication, add a
 * {@code Bearer JWT} branch to {@link #applyTo} and an additional header
 * source in {@link #fromRequest}.
 *
 * <p>The credentials arrive as {@code String} from the servlet container and
 * are stored here as {@code char[]} to support {@link #clear()} as a hygiene
 * measure. The operative security guarantee enforced by this package is that
 * the values are never persisted, never logged, and never echoed in response
 * bodies (Requirement 7). The {@code char[]} wipe does not, on its own,
 * eliminate every in-memory copy created by the JDK or servlet container.
 */
public final class PinataHeaders {

    public static final String API_KEY_HEADER = "X-Pinata-Api-Key";
    public static final String SECRET_KEY_HEADER = "X-Pinata-Secret-Api-Key";

    private char[] apiKey;
    private char[] secretKey;

    private PinataHeaders(char[] apiKey, char[] secretKey) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
    }

    /**
     * Reads Pinata credentials from the inbound request. Throws
     * {@link PinataKeyRequiredException} if either header is missing or blank.
     */
    public static PinataHeaders fromRequest(HttpServletRequest request) {
        String api = request.getHeader(API_KEY_HEADER);
        String secret = request.getHeader(SECRET_KEY_HEADER);
        if (api == null || api.isBlank()) {
            throw new PinataKeyRequiredException(API_KEY_HEADER);
        }
        if (secret == null || secret.isBlank()) {
            throw new PinataKeyRequiredException(SECRET_KEY_HEADER);
        }
        return new PinataHeaders(api.toCharArray(), secret.toCharArray());
    }

    /**
     * Add the Pinata credential headers to the supplied outbound request builder.
     * Forwards verbatim as Pinata's documented {@code pinata_api_key} and
     * {@code pinata_secret_api_key} request headers.
     */
    public void applyTo(HttpRequest.Builder builder) {
        builder.header("pinata_api_key", new String(apiKey));
        builder.header("pinata_secret_api_key", new String(secretKey));
    }

    /**
     * Wipe the credential bytes. Safe to call multiple times.
     */
    public void clear() {
        if (apiKey != null) Arrays.fill(apiKey, '\0');
        if (secretKey != null) Arrays.fill(secretKey, '\0');
        apiKey = null;
        secretKey = null;
    }
}
