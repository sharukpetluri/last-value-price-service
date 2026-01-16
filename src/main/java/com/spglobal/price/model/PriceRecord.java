package com.spglobal.price.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable representation of a price for a financial instrument.
 * The 'asOf' timestamp determines price ordering.
 */
public final class PriceRecord {

    private final String instrumentId;
    private final Instant asOf;
    private final Object payload;

    public PriceRecord(String instrumentId, Instant asOf, Object payload) {
        this.instrumentId = Objects.requireNonNull(instrumentId, "instrumentId must not be null");
        this.asOf = Objects.requireNonNull(asOf, "asOf must not be null");
        this.payload = payload; // payload is intentionally flexible
    }

    public String getInstrumentId() {
        return instrumentId;
    }

    public Instant getAsOf() {
        return asOf;
    }

    public Object getPayload() {
        return payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriceRecord)) return false;
        PriceRecord that = (PriceRecord) o;
        return instrumentId.equals(that.instrumentId) &&
                asOf.equals(that.asOf) &&
                Objects.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instrumentId, asOf, payload);
    }

    @Override
    public String toString() {
        return "PriceRecord{" +
                "instrumentId='" + instrumentId + '\'' +
                ", asOf=" + asOf +
                ", payload=" + payload +
                '}';
    }
}
