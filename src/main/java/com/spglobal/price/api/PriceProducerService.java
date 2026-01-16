package com.spglobal.price.api;

import com.spglobal.price.model.PriceRecord;

import java.util.List;
import java.util.UUID;

/**
 * API for producers to publish price data in batch runs.
 */
public interface PriceProducerService {

    /**
     * Starts a new batch run.
     *
     * @return unique batch identifier
     */
    UUID startBatch();

    /**
     * Publishes a chunk of price records for the given batch.
     *
     * @param batchId batch identifier
     * @param prices  list of price records (max 1000 per call)
     */
    void publishPrices(UUID batchId, List<PriceRecord> prices);

    /**
     * Completes the batch and makes all prices visible atomically.
     *
     * @param batchId batch identifier
     */
    void completeBatch(UUID batchId);

    /**
     * Cancels the batch and discards all uploaded prices.
     *
     * @param batchId batch identifier
     */
    void cancelBatch(UUID batchId);
}
