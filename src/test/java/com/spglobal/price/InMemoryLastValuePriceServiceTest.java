package com.spglobal.price;

import com.spglobal.price.model.PriceRecord;
import com.spglobal.price.service.InMemoryLastValuePriceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryLastValuePriceServiceTest {

    private static final Logger log =
            LoggerFactory.getLogger(InMemoryLastValuePriceServiceTest.class);

    private InMemoryLastValuePriceService service;

    @BeforeEach
    void setUp() {
        service = new InMemoryLastValuePriceService();
    }

    @Test
    void shouldReturnEmptyWhenNoPriceExists() {
        log.info("INPUT: getLastPrice(AAPL)");

        Optional<PriceRecord> result = service.getLastPrice("AAPL");

        log.info("OUTPUT: {}", result.orElse(null));
        assertFalse(result.isPresent());
    }

    @Test
    void shouldMakePricesVisibleOnlyAfterBatchCompletion() {
        UUID batchId = service.startBatch();

        PriceRecord price = new PriceRecord(
                "AAPL",
                Instant.parse("2024-01-01T10:00:00Z"),
                150.25
        );

        log.info("INPUT: publishPrices batchId={}, price={}", batchId, price);
        service.publishPrices(batchId, Arrays.asList(price));

        assertFalse(service.getLastPrice("AAPL").isPresent());

        service.completeBatch(batchId);

        Optional<PriceRecord> result = service.getLastPrice("AAPL");
        log.info("OUTPUT: {}", result.orElse(null));

        assertTrue(result.isPresent());
        assertEquals(price, result.get());
    }

    @Test
    void shouldDiscardPricesWhenBatchIsCancelled() {
        UUID batchId = service.startBatch();

        PriceRecord price = new PriceRecord(
                "IBM",
                Instant.now(),
                125.50
        );

        log.info("INPUT: publishPrices batchId={}, price={}", batchId, price);
        service.publishPrices(batchId, Arrays.asList(price));

        service.cancelBatch(batchId);

        Optional<PriceRecord> result = service.getLastPrice("IBM");
        log.info("OUTPUT: {}", result.orElse(null));

        assertFalse(result.isPresent());
    }

    @Test
    void shouldSelectLatestPriceBasedOnAsOfTimestamp() {
        UUID batchId = service.startBatch();

        PriceRecord older = new PriceRecord(
                "GOOG",
                Instant.parse("2024-01-01T09:00:00Z"),
                2700.00
        );

        PriceRecord newer = new PriceRecord(
                "GOOG",
                Instant.parse("2024-01-01T11:00:00Z"),
                2750.00
        );

        log.info("INPUT: publishPrices batchId={}, prices=[{}, {}]", batchId, older, newer);
        service.publishPrices(batchId, Arrays.asList(older, newer));

        service.completeBatch(batchId);

        Optional<PriceRecord> result = service.getLastPrice("GOOG");
        log.info("OUTPUT: {}", result.orElse(null));

        assertTrue(result.isPresent());
        assertEquals(newer, result.get());
    }

    @Test
    void shouldHandleMultipleChunksInSameBatch() {
        UUID batchId = service.startBatch();

        PriceRecord p1 = new PriceRecord(
                "MSFT",
                Instant.parse("2024-01-01T08:00:00Z"),
                300.00
        );

        PriceRecord p2 = new PriceRecord(
                "MSFT",
                Instant.parse("2024-01-01T12:00:00Z"),
                310.00
        );

        log.info("INPUT: publishPrices batchId={}, price={}", batchId, p1);
        service.publishPrices(batchId, Arrays.asList(p1));

        log.info("INPUT: publishPrices batchId={}, price={}", batchId, p2);
        service.publishPrices(batchId, Arrays.asList(p2));

        service.completeBatch(batchId);

        Optional<PriceRecord> result = service.getLastPrice("MSFT");
        log.info("OUTPUT: {}", result.orElse(null));

        assertTrue(result.isPresent());
        assertEquals(p2, result.get());
    }

    @Test
    void shouldThrowExceptionWhenPublishingWithoutActiveBatch() {
        PriceRecord price = new PriceRecord(
                "TSLA",
                Instant.now(),
                800.00
        );

        log.info("INPUT: publishPrices without active batch, price={}", price);

        assertThrows(
                IllegalStateException.class,
                () -> service.publishPrices(UUID.randomUUID(), Arrays.asList(price))
        );
    }

    @Test
    void shouldThrowExceptionWhenCompletingWithInvalidBatchId() {
        UUID batchId = service.startBatch();
        UUID invalidBatchId = UUID.randomUUID();

        log.info("INPUT: completeBatch invalidBatchId={}", invalidBatchId);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.completeBatch(invalidBatchId)
        );
    }

    @Test
    void consumerShouldSeeOldPricesWhileBatchIsInProgress() {
        UUID firstBatch = service.startBatch();

        PriceRecord initial = new PriceRecord(
                "NFLX",
                Instant.parse("2024-01-01T08:00:00Z"),
                500.00
        );

        service.publishPrices(firstBatch, Arrays.asList(initial));
        service.completeBatch(firstBatch);

        UUID secondBatch = service.startBatch();
        PriceRecord updated = new PriceRecord(
                "NFLX",
                Instant.parse("2024-01-01T12:00:00Z"),
                520.00
        );

        log.info("INPUT: publishPrices batchId={}, price={}", secondBatch, updated);
        service.publishPrices(secondBatch, Arrays.asList(updated));

        Optional<PriceRecord> visible = service.getLastPrice("NFLX");
        log.info("OUTPUT (during batch): {}", visible.orElse(null));

        assertTrue(visible.isPresent());
        assertEquals(initial, visible.get());
    }
}
