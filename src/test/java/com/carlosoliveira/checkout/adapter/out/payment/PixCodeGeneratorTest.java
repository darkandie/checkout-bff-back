package com.carlosoliveira.checkout.adapter.out.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PixCodeGeneratorTest {

    private static final String PIX_KEY = "123e4567-e12b-12d1-a456-426655440000";

    private final PixCodeGenerator generator = new PixCodeGenerator("Checkout Assinaturas", "Belém");

    @Test
    @DisplayName("builds an EMV BR Code with the Pix key, amount, merchant and transaction id")
    void buildsBrCode() {
        String code = generator.generate(PIX_KEY, new BigDecimal("39.9"), "tx-123");

        assertThat(code).startsWith("000201");
        assertThat(code).contains("0014br.gov.bcb.pix0136" + PIX_KEY);
        assertThat(code).contains("5303986");
        assertThat(code).contains("540539.90");
        assertThat(code).contains("5802BR");
        assertThat(code).contains("5920CHECKOUT ASSINATURAS");
        assertThat(code).contains("6005BELEM");
        assertThat(code).contains("62090505tx123");
        assertThat(code).matches(".*6304[0-9A-F]{4}");
    }

    @Test
    @DisplayName("ends with a CRC16 checksum that matches the payload")
    void appendsValidChecksum() {
        String code = generator.generate(PIX_KEY, new BigDecimal("59.90"), "abc");

        String payload = code.substring(0, code.length() - 4);
        String checksum = code.substring(code.length() - 4);

        assertThat(checksum).isEqualTo(crc16(payload));
    }

    @Test
    @DisplayName("computes the CRC16 of the Banco Central reference payload")
    void matchesReferenceChecksum() {
        String reference = "00020126580014br.gov.bcb.pix0136123e4567-e12b-12d1-a456-426655440000"
                + "5204000053039865802BR5913Fulano de Tal6008BRASILIA62070503***6304";

        assertThat(crc16(reference)).isEqualTo("1D3D");
    }

    @Test
    @DisplayName("truncates merchant name, city and transaction id to the BR Code limits")
    void truncatesFields() {
        PixCodeGenerator longNames = new PixCodeGenerator(
                "Empresa de Assinaturas Digitais do Brasil", "São Miguel do Guamá");

        String code = longNames.generate(PIX_KEY, BigDecimal.TEN, "a".repeat(40));

        assertThat(code).contains("5925EMPRESA DE ASSINATURAS DI");
        assertThat(code).contains("6015SAO MIGUEL DO G6229");
        assertThat(code).contains("0525" + "a".repeat(25));
    }

    @Test
    @DisplayName("generates a random Pix key for each charge")
    void generatesRandomKeys() {
        assertThat(generator.randomKey()).isNotEqualTo(generator.randomKey());
        assertThat(generator.randomKey()).hasSize(36);
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
