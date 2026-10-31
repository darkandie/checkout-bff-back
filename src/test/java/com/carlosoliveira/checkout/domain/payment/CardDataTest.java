package com.carlosoliveira.checkout.domain.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class CardDataTest {

    private static final YearMonth OCTOBER_2026 = YearMonth.of(2026, 10);

    @Test
    @DisplayName("exposes only the last four digits")
    void lastDigits() {
        CardData card = new CardData("Ana Souza", "4111111111114242", "12/28", "123");

        assertThat(card.lastDigits()).isEqualTo("4242");
        assertThat(card.endsWith("4242")).isTrue();
        assertThat(card.endsWith("0002")).isFalse();
    }

    @Test
    @DisplayName("never prints holder, number, expiry or CVV")
    void masksToString() {
        CardData card = new CardData("Ana Souza", "4111111111114242", "12/28", "987");

        String printed = card.toString();

        assertThat(printed).doesNotContain("Ana Souza");
        assertThat(printed).doesNotContain("4111111111114242");
        assertThat(printed).doesNotContain("12/28");
        assertThat(printed).doesNotContain("987");
        assertThat(printed).contains("****4242");
    }

    @Test
    @DisplayName("is valid until the end of its expiry month")
    void validUntilExpiryMonth() {
        assertThat(new CardData("A", "4111111111114242", "10/26", "123").isExpiredAt(OCTOBER_2026)).isFalse();
        assertThat(new CardData("A", "4111111111114242", "12/28", "123").isExpiredAt(OCTOBER_2026)).isFalse();
    }

    @Test
    @DisplayName("is expired after its expiry month")
    void expiredAfterExpiryMonth() {
        assertThat(new CardData("A", "4111111111114242", "09/26", "123").isExpiredAt(OCTOBER_2026)).isTrue();
    }

    @Test
    @DisplayName("treats an unparseable expiry as expired")
    void invalidExpiryIsExpired() {
        assertThat(new CardData("A", "4111111111114242", "13/40", "123").isExpiredAt(OCTOBER_2026)).isTrue();
        assertThat(new CardData("A", "4111111111114242", null, "123").isExpiredAt(OCTOBER_2026)).isTrue();
    }

    @Test
    @DisplayName("handles numbers too short to mask")
    void shortNumber() {
        assertThat(new CardData("A", "12", "12/28", "123").lastDigits()).isEmpty();
        assertThat(new CardData("A", null, "12/28", "123").lastDigits()).isEmpty();
    }
}
