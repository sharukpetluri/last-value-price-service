package com.spglobal.price.api;

import com.spglobal.price.model.PriceRecord;

import java.util.Optional;

/**
 * API for consumers to retrieve the last price for an instrument.
 */
public interface PriceConsumerService {

    /**
     * Returns the last available price for the given instrument.
     *
     * @param instrumentId instrument identifier
     * @return optional price record
     */
    Optional<PriceRecord> getLastPrice(String instrumentId);
}
