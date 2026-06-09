package com.livana.backend.pool.preparation.service;

import com.livana.backend.auth.entity.User;
import com.livana.backend.auth.service.UserService;
import com.livana.backend.common.exception.ApiException;
import com.livana.backend.ngo.entity.ApplicationStatus;
import com.livana.backend.ngo.repository.NgoApplicationRepository;
import com.livana.backend.pool.preparation.exception.NgoNotApprovedException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Runs the three checks the pool preparation endpoints share, in order:
 *   1. Resolve user from JWT subject (401 USER_NOT_FOUND if missing)
 *   2. Require linked wallet (403 WALLET_NOT_LINKED, surfaced by UserService)
 *   3. Require an APPROVED NgoApplication for the (lowercased) wallet
 *
 * Returns the User on success.
 *
 * Note: UserService.getAuthenticatedUserWithWallet currently throws 404
 * USER_NOT_FOUND when the JWT subject does not resolve to a user. Requirement
 * 5.4 specifies 401 for that case, so we catch and re-throw with the right
 * status without changing UserService semantics for other consumers.
 */
@Service
@RequiredArgsConstructor
public class NgoAuthorizationService {

    private final UserService userService;
    private final NgoApplicationRepository ngoApplicationRepository;

    public User requireApprovedNgo(Jwt jwt) {
        String clerkId = jwt.getSubject();
        User user;
        try {
            user = userService.getAuthenticatedUserWithWallet(clerkId);
        } catch (ApiException ex) {
            if ("USER_NOT_FOUND".equals(ex.getErrorCode())) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", ex.getMessage());
            }
            throw ex;
        }

        String wallet = user.getWalletAddress().toLowerCase(Locale.ROOT);
        ngoApplicationRepository
                .findByWalletAddressAndStatus(wallet, ApplicationStatus.APPROVED)
                .orElseThrow(NgoNotApprovedException::new);
        return user;
    }
}
