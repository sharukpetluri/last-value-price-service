package com.spglobal.price.service;

import com.spglobal.price.api.PriceConsumerService;
import com.spglobal.price.api.PriceProducerService;
import com.spglobal.price.model.BatchStatus;
import com.spglobal.price.model.PriceRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of the Last Value Price Service.
 *
 * Design highlights:
 * - Atomic visibility of batch data
 * - Clear separation between committed and staged prices
 * - Thread-safe with minimal synchronization
 * - Graceful handling of invalid producer call sequences
 */
public class InMemoryLastValuePriceService
        implements PriceProducerService, PriceConsumerService {

    /**
     * Stores prices visible to consumers.
     * Key: instrumentId
     * Value: last price based on asOf timestamp
     */
    private final Map<String, PriceRecord> committedPrices = new ConcurrentHashMap<>();

    /**
     * Only one active batch is allowed at a time (documented assumption).
     */
    private BatchContext activeBatch;

    /**
     * Global lock to protect batch lifecycle operations.
     */
    private final Object batchLock = new Object();

    // =========================
    // Producer API
    // =========================

    @Override
    public UUID startBatch() {
        synchronized (batchLock) {
            if (activeBatch != null &&
                    activeBatch.getStatus() != BatchStatus.COMPLETED &&
                    activeBatch.getStatus() != BatchStatus.CANCELLED) {
                throw new IllegalStateException("Another batch is already active");
            }

            UUID batchId = UUID.randomUUID();
            activeBatch = new BatchContext(batchId);
            return batchId;
        }
    }

    @Override
    public void publishPrices(UUID batchId, List<PriceRecord> prices) {
        if (prices == null || prices.isEmpty()) {
            return; // no-op
        }

        if (prices.size() > 1000) {
            throw new IllegalArgumentException("Batch chunk size exceeds 1000 records");
        }

        synchronized (batchLock) {
            validateActiveBatch(batchId);

            activeBatch.setStatus(BatchStatus.IN_PROGRESS);

            for (PriceRecord price : prices) {
                activeBatch.getStagedPrices().merge(
                        price.getInstrumentId(),
                        price,
                        this::selectLatestPrice
                );
            }
        }
    }

    @Override
    public void completeBatch(UUID batchId) {
        synchronized (batchLock) {
            validateActiveBatch(batchId);

            // Atomically merge staged prices into committed store
            for (PriceRecord stagedPrice : activeBatch.getStagedPrices().values()) {
                committedPrices.merge(
                        stagedPrice.getInstrumentId(),
                        stagedPrice,
                        this::selectLatestPrice
                );
            }

            activeBatch.setStatus(BatchStatus.COMPLETED);
            activeBatch = null; // discard batch context
        }
    }

    @Override
    public void cancelBatch(UUID batchId) {
        synchronized (batchLock) {
            validateActiveBatch(batchId);
            activeBatch.setStatus(BatchStatus.CANCELLED);
            activeBatch = null; // discard staged data
        }
    }

    // =========================
    // Consumer API
    // =========================

    @Override
    public Optional<PriceRecord> getLastPrice(String instrumentId) {
        return Optional.ofNullable(committedPrices.get(instrumentId));
    }

    // =========================
    // Helper Methods
    // =========================

    /**
     * Validates that the given batchId refers to the currently active batch.
     */
    private void validateActiveBatch(UUID batchId) {
        if (activeBatch == null) {
            throw new IllegalStateException("No active batch found");
        }
        if (!activeBatch.getBatchId().equals(batchId)) {
            throw new IllegalArgumentException("Invalid batchId");
        }
        if (activeBatch.getStatus() == BatchStatus.COMPLETED ||
                activeBatch.getStatus() == BatchStatus.CANCELLED) {
            throw new IllegalStateException("Batch is already closed");
        }
    }

    /**
     * Selects the price with the latest 'asOf' timestamp.
     */
    private PriceRecord selectLatestPrice(PriceRecord existing, PriceRecord incoming) {
        return incoming.getAsOf().isAfter(existing.getAsOf()) ? incoming : existing;
    }
}
