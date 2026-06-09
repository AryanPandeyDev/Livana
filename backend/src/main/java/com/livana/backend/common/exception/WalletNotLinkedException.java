package com.livana.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an endpoint requires the user to have a linked wallet address,
 * but the authenticated user has not linked one yet.
 *
 * From the PRD:
 * "Endpoints that require an on-chain action (donate, create pool, submit proof)
 *  check that the authenticated user has a walletAddress linked. If not, they
 *  return 403 with error code WALLET_NOT_LINKED."
 */
public class WalletNotLinkedException extends ApiException {

    public WalletNotLinkedException() {
        super(HttpStatus.FORBIDDEN, "WALLET_NOT_LINKED",
                "You must link a wallet address before performing this action");
    }
}
