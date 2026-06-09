package com.livana.backend.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Verifies Ethereum personal_sign (EIP-191) signatures server-side using web3j.
 *
 * Uses Sign.signedPrefixedMessageToKey() which is the correct web3j method
 * for recovering the signer from a personal_sign signature. This method
 * internally applies the "\x19Ethereum Signed Message:\n{length}" prefix
 * before recovery.
 *
 * Reference: https://docs.web3j.io/4.14.0/advanced/message_signing/
 */
@Service
@Slf4j
public class SignatureVerificationService {

    /**
     * Verify that a personal_sign (EIP-191) signature was produced by the claimed wallet address.
     *
     * @param claimedAddress The wallet address the user claims to own (0x...)
     * @param originalMessage The original message that was signed (without prefix)
     * @param signatureHex The signature produced by personal_sign (0x... 65 bytes hex)
     * @return true if the signature was produced by the claimed address
     */
    public boolean verifyPersonalSign(String claimedAddress, String originalMessage, String signatureHex) {
        try {
            byte[] signatureBytes = Numeric.hexStringToByteArray(signatureHex);
            if (signatureBytes.length != 65) {
                log.warn("Invalid signature length: {} (expected 65)", signatureBytes.length);
                return false;
            }

            // Decompose signature into r, s, v
            byte v = signatureBytes[64];
            // EIP-155: some wallets return v as 0/1, others as 27/28
            if (v < 27) {
                v += 27;
            }

            byte[] r = Arrays.copyOfRange(signatureBytes, 0, 32);
            byte[] s = Arrays.copyOfRange(signatureBytes, 32, 64);

            SignatureData signatureData = new SignatureData(v, r, s);

            // Use signedPrefixedMessageToKey — this is the correct method for
            // personal_sign / EIP-191 recovery. It internally applies the
            // "\x19Ethereum Signed Message:\n{length}" prefix before ecrecover.
            byte[] messageBytes = originalMessage.getBytes(StandardCharsets.UTF_8);
            BigInteger publicKey = Sign.signedPrefixedMessageToKey(messageBytes, signatureData);

            // Derive address from recovered public key
            String recoveredAddress = "0x" + Keys.getAddress(publicKey);

            // Compare case-insensitively (addresses are hex)
            boolean matches = recoveredAddress.equalsIgnoreCase(claimedAddress);
            if (!matches) {
                log.debug("Signature verification failed: recovered={}, claimed={}",
                        recoveredAddress, claimedAddress);
            }
            return matches;
        } catch (Exception e) {
            log.warn("Signature verification error: {}", e.getMessage());
            return false;
        }
    }
}
