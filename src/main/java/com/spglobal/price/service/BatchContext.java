package com.spglobal.price.service;

import com.spglobal.price.model.BatchStatus;
import com.spglobal.price.model.PriceRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Holds the state and staged data for an active batch run.
 * This data is NOT visible to consumers until the batch is completed.
 */
class BatchContext {

    private final UUID batchId;
    private BatchStatus status;
    private final Map<String, PriceRecord> stagedPrices = new HashMap<>();

    BatchContext(UUID batchId) {
        this.batchId = batchId;
        this.status = BatchStatus.STARTED;
    }

    UUID getBatchId() {
        return batchId;
    }

    BatchStatus getStatus() {
        return status;
    }

    void setStatus(BatchStatus status) {
        this.status = status;
    }

    Map<String, PriceRecord> getStagedPrices() {
        return stagedPrices;
    }
}
