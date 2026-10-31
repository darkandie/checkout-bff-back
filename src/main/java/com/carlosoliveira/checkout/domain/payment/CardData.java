package com.carlosoliveira.checkout.domain.payment;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public record CardData(String holderName, String number, String expiry, String cvv) {

    private static final DateTimeFormatter EXPIRY_FORMAT = DateTimeFormatter.ofPattern("MM/yy");
    private static final int VISIBLE_DIGITS = 4;

    public String lastDigits() {
        if (number == null || number.length() < VISIBLE_DIGITS) {
            return "";
        }
        return number.substring(number.length() - VISIBLE_DIGITS);
    }

    public boolean endsWith(String suffix) {
        return number != null && number.endsWith(suffix);
    }

    public Optional<YearMonth> expiryMonth() {
        try {
            return Optional.of(YearMonth.parse(expiry, EXPIRY_FORMAT));
        } catch (DateTimeParseException | NullPointerException e) {
            return Optional.empty();
        }
    }

    public boolean isExpiredAt(YearMonth reference) {
        return expiryMonth().map(month -> month.isBefore(reference)).orElse(true);
    }

    @Override
    public String toString() {
        return "CardData[holderName=***, number=****" + lastDigits() + ", expiry=**/**, cvv=***]";
    }
}
