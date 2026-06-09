package com.livana.backend.common.validation;

import com.livana.backend.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddressValidatorTest {

    @Test
    void validLowercaseAddress_returnsLowercase() {
        String address = "0x1234567890abcdef1234567890abcdef12345678";
        String result = AddressValidator.validateAndNormalize(address, "testParam");
        assertThat(result).isEqualTo(address);
    }

    @Test
    void validMixedCaseAddress_returnsLowercase() {
        String address = "0xABCDEF1234567890ABCDEF1234567890ABCDEF12";
        String result = AddressValidator.validateAndNormalize(address, "testParam");
        assertThat(result).isEqualTo(address.toLowerCase());
    }

    @Test
    void nullAddress_throwsApiException() {
        assertThatThrownBy(() -> AddressValidator.validateAndNormalize(null, "walletAddress"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getErrorCode()).isEqualTo("INVALID_ADDRESS");
                    assertThat(apiEx.getMessage()).isEqualTo("walletAddress must not be empty");
                });
    }

    @Test
    void blankAddress_throwsApiException() {
        assertThatThrownBy(() -> AddressValidator.validateAndNormalize("   ", "poolAddress"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getErrorCode()).isEqualTo("INVALID_ADDRESS");
                    assertThat(apiEx.getMessage()).isEqualTo("poolAddress must not be empty");
                });
    }

    @Test
    void addressTooShort_throwsApiException() {
        assertThatThrownBy(() -> AddressValidator.validateAndNormalize("0x1234", "addr"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getErrorCode()).isEqualTo("INVALID_ADDRESS");
                    assertThat(apiEx.getMessage()).contains("42-character hex string starting with 0x");
                });
    }

    @Test
    void addressMissingPrefix_throwsApiException() {
        assertThatThrownBy(() -> AddressValidator.validateAndNormalize("1234567890abcdef1234567890abcdef12345678ab", "addr"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getErrorCode()).isEqualTo("INVALID_ADDRESS");
                });
    }

    @Test
    void addressWithNonHexChars_throwsApiException() {
        assertThatThrownBy(() -> AddressValidator.validateAndNormalize("0xGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG", "addr"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getErrorCode()).isEqualTo("INVALID_ADDRESS");
                });
    }
}
