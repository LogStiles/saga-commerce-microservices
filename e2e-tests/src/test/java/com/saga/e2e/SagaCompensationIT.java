package com.saga.e2e;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the same three scenarios documented in the README's "Try the saga" section, but through
 * the real stack (docker compose up --build) instead of by hand: real Kafka, real Debezium CDC,
 * real payment-service/inventory-service. Requires the stack to already be running — see the
 * module's pom.xml for how this is kept out of a plain `mvn install`.
 */
class SagaCompensationIT {

    private static final URI ORDERS_URI = URI.create("http://localhost:8080/api/v1/orders");
    private static final Path E2E_PAYLOADS = Path.of("..", "e2e");
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Pattern STATUS_FIELD = Pattern.compile("\"status\"\\s*:\\s*\"(\\w+)\"");

    private record OrderResult(UUID orderId, String status) {}

    @Test
    void happyPath_completesAndDecrementsStock() throws Exception {
        long stockBefore = queryStock(1);

        var result = placeOrderAndAwaitTerminalStatus("order-placement.json");

        assertThat(result.status()).isEqualTo("SUCCEED");
        assertThat(queryPaymentType(result.orderId())).isEqualTo("REQUEST");
        assertThat(queryStock(1)).isEqualTo(stockBefore - 2); // order-placement.json orders 2x item 1
    }

    @Test
    void paymentFailure_failsWithoutTouchingInventory() throws Exception {
        long stockBefore = queryStock(1);

        var result = placeOrderAndAwaitTerminalStatus("invalid-payment.json");

        assertThat(result.status()).isEqualTo("FAILED");
        // failed at step one (card ends 1234) -- nothing was ever compensated
        assertThat(queryPaymentType(result.orderId())).isEqualTo("REQUEST");
        assertThat(queryStock(1)).isEqualTo(stockBefore); // saga never reached the inventory step
    }

    @Test
    void insufficientStock_compensatesThePayment() throws Exception {
        var result = placeOrderAndAwaitTerminalStatus("insufficient-stock.json");

        assertThat(result.status()).isEqualTo("FAILED");
        // payment succeeded, inventory rejected (item 3 has 0 stock), saga walked backwards
        assertThat(queryPaymentType(result.orderId())).isEqualTo("CANCEL");
        assertThat(queryStock(3)).isZero();
    }

    private OrderResult placeOrderAndAwaitTerminalStatus(String payloadFile) throws Exception {
        var body = Files.readString(E2E_PAYLOADS.resolve(payloadFile));
        var postRequest = HttpRequest.newBuilder(ORDERS_URI)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        var postResponse = HTTP.send(postRequest, HttpResponse.BodyHandlers.discarding());
        assertThat(postResponse.statusCode()).isEqualTo(202);
        var location = postResponse.headers().firstValue("Location").orElseThrow();
        var orderId = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

        var statusUri = URI.create(ORDERS_URI + "/" + orderId);
        var deadline = Instant.now().plusSeconds(30);
        String status;
        do {
            Thread.sleep(500);
            var getResponse = HTTP.send(
                    HttpRequest.newBuilder(statusUri).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            var matcher = STATUS_FIELD.matcher(getResponse.body());
            status = matcher.find() ? matcher.group(1) : null;
        } while ("PENDING".equals(status) && Instant.now().isBefore(deadline));

        assertThat(status).as("saga did not reach a terminal status within 30s").isNotEqualTo("PENDING");
        return new OrderResult(orderId, status);
    }

    private long queryStock(int itemId) throws Exception {
        try (var conn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5434/inventorydb", "inventoryuser", "inventorysecret");
             var stmt = conn.prepareStatement("select stock_amount from inventory where id = ?")) {
            stmt.setInt(1, itemId);
            try (var rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong("stock_amount");
            }
        }
    }

    private String queryPaymentType(UUID purchaseId) throws Exception {
        try (var conn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5433/paymentdb", "paymentuser", "paymentsecret");
             var stmt = conn.prepareStatement("select type from payment where purchase_id = ?")) {
            stmt.setObject(1, purchaseId);
            try (var rs = stmt.executeQuery()) {
                rs.next();
                return rs.getString("type");
            }
        }
    }
}
