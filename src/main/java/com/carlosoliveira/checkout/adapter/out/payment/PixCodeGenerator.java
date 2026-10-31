package com.carlosoliveira.checkout.adapter.out.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

class PixCodeGenerator {

    private static final String PAYLOAD_FORMAT = "00";
    private static final String MERCHANT_ACCOUNT = "26";
    private static final String MERCHANT_CATEGORY = "52";
    private static final String TRANSACTION_CURRENCY = "53";
    private static final String TRANSACTION_AMOUNT = "54";
    private static final String COUNTRY_CODE = "58";
    private static final String MERCHANT_NAME = "59";
    private static final String MERCHANT_CITY = "60";
    private static final String ADDITIONAL_DATA = "62";
    private static final String CRC = "63";

    private static final String PIX_GUI = "br.gov.bcb.pix";
    private static final String BRL_NUMERIC_CODE = "986";
    private static final int MAX_NAME_LENGTH = 25;
    private static final int MAX_CITY_LENGTH = 15;
    private static final int MAX_TXID_LENGTH = 25;

    private final String merchantName;
    private final String merchantCity;

    PixCodeGenerator(String merchantName, String merchantCity) {
        this.merchantName = sanitize(merchantName, MAX_NAME_LENGTH);
        this.merchantCity = sanitize(merchantCity, MAX_CITY_LENGTH);
    }

    String generate(String pixKey, BigDecimal amount, String transactionId) {
        String merchantAccount = field("00", PIX_GUI) + field("01", pixKey);
        String txid = transactionId.replaceAll("[^A-Za-z0-9]", "");
        txid = txid.substring(0, Math.min(txid.length(), MAX_TXID_LENGTH));

        String payload = field(PAYLOAD_FORMAT, "01")
                + field(MERCHANT_ACCOUNT, merchantAccount)
                + field(MERCHANT_CATEGORY, "0000")
                + field(TRANSACTION_CURRENCY, BRL_NUMERIC_CODE)
                + field(TRANSACTION_AMOUNT, amount.setScale(2, RoundingMode.HALF_EVEN).toPlainString())
                + field(COUNTRY_CODE, "BR")
                + field(MERCHANT_NAME, merchantName)
                + field(MERCHANT_CITY, merchantCity)
                + field(ADDITIONAL_DATA, field("05", txid))
                + CRC + "04";

        return payload + crc16(payload);
    }

    String randomKey() {
        return UUID.randomUUID().toString();
    }

    private static String field(String id, String value) {
        return id + String.format("%02d", value.length()) + value;
    }

    private static String sanitize(String value, int maxLength) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9 ]", "")
                .toUpperCase(Locale.ROOT)
                .trim();
        return normalized.substring(0, Math.min(normalized.length(), maxLength));
    }

    private static String crc16(String payload) {
        int crc = 0xFFFF;
        for (byte b : payload.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }
}
