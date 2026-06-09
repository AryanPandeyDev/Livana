package com.livana.backend.common.config;

import com.livana.backend.auth.entity.User;
import com.livana.backend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Converts a verified Clerk JWT into a Spring Security authentication token
 * with the user's role from our database.
 *
 * Implements "lazy user creation" — on first API call, if the user doesn't
 * exist in our DB, we create them using claims from the JWT:
 *   - sub → clerkId (always present, e.g. "user_2abc123")
 *   - primaryEmail → email (custom claim, added in Clerk Dashboard)
 *   - fullName → displayName (custom claim, added in Clerk Dashboard)
 *
 * This eliminates the need for Clerk webhooks and tunneling during local dev.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClerkJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String clerkId = jwt.getSubject();
        String email = jwt.getClaimAsString("primaryEmail");
        String fullName = jwt.getClaimAsString("fullName");

        // Lazy create or update the user from JWT claims
        User user = getOrCreateUser(clerkId, email, fullName);

        if (user == null) {
            // Cannot authenticate — missing required claims and user doesn't exist
            // Returning null causes Spring Security to reject the request with 401
            return null;
        }

        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole())
        );

        return new JwtAuthenticationToken(jwt, authorities);
    }

    private User getOrCreateUser(String clerkId, String email, String fullName) {
        return userRepository.findByClerkId(clerkId)
                .map(existing -> {
                    // Update email/name if they changed in Clerk
                    boolean updated = false;
                    if (email != null && !email.equals(existing.getEmail())) {
                        existing.setEmail(email);
                        updated = true;
                    }
                    if (fullName != null && !fullName.equals(existing.getDisplayName())) {
                        existing.setDisplayName(fullName);
                        updated = true;
                    }
                    if (updated) {
                        log.debug("Updated user from JWT claims: clerkId={}", clerkId);
                        return userRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    if (email == null) {
                        // User doesn't exist and we can't create them without email.
                        // This means the Clerk session token is missing the custom claims.
                        // Log an error but still return a default so the request proceeds
                        // with a 401 or the endpoint handles USER_NOT_FOUND appropriately.
                        log.error("Cannot create user: no 'primaryEmail' claim in JWT for clerkId={}. " +
                                "Configure custom claims in Clerk Dashboard → Sessions → Customize session token: " +
                                "{{\"primaryEmail\": \"{{{{user.primary_email_address}}}}\", \"fullName\": \"{{{{user.full_name}}}}\"}}",
                                clerkId);
                        return null;
                    }
                    User newUser = User.builder()
                            .clerkId(clerkId)
                            .email(email)
                            .displayName(fullName)
                            .role("USER")
                            .build();
                    log.info("Created user via lazy creation: clerkId={}, email={}", clerkId, email);
                    return userRepository.save(newUser);
                });
    }
}
