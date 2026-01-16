# Last Value Price Service

## Overview

This project implements an **in-memory Java service** for maintaining and serving the **last (latest) price** of financial instruments. It is designed as a clean, production-quality solution that demonstrates how to translate business requirements into a robust technical design.

The service supports:

* **Producers** publishing prices in controlled **batch runs**
* **Consumers** retrieving the **latest available price** for a given instrument

The solution focuses on correctness, clarity, and maintainability rather than persistence or distributed concerns.

---

## Business Problem

Financial price data is produced in bulk and must be published **atomically** so that consumers never see partial or inconsistent data.

Key challenges addressed:

* Ensuring prices become visible **only after a batch is fully completed**
* Determining the *last price* using the producer-provided `asOf` timestamp
* Safely handling incorrect producer call sequences
* Allowing consumers to read prices while a batch is being processed

---

## Core Concepts

### Price Record

Each price record contains:

* `instrumentId` – Identifier of the financial instrument
* `asOf` – Timestamp indicating when the price was determined
* `payload` – Flexible structure holding the price data

The payload is treated as an opaque object, allowing future extensibility.

### Batch Run

Prices are published in **batch runs** with the following lifecycle:

1. Start batch
2. Upload price records in chunks (maximum 1000 records per call)
3. Complete or cancel batch

Only completed batches affect the visible price data.

---

## Business Rules

1. **Atomic Visibility**
   Prices from a batch become visible to consumers only after successful batch completion.

2. **Cancellation Handling**
   Cancelled batches are discarded entirely and have no effect on visible prices.

3. **Last Price Determination**
   For each instrument, the last price is determined by the latest `asOf` timestamp, not by arrival order.

4. **Fault Tolerance**
   The service is resilient to incorrect producer call order and concurrent consumer access.

5. **Isolation**
   Ongoing batch uploads never affect prices currently visible to consumers.

---

## Assumptions

The following assumptions are explicitly made:

* Only **one active batch run** is allowed at a time
* The service runs within a **single JVM**
* All data is stored **in memory** (no persistence)
* Thread safety is required
* Input payload validation is out of scope

All assumptions are documented to keep the design transparent and easy to evolve.

---

## Technical Design

### API Design

The service exposes two Java APIs:

* **Producer API** – Used to manage batch lifecycle and publish prices
* **Consumer API** – Used to retrieve the last available price for an instrument

This separation enforces clear responsibilities and improves testability.

### Internal Storage Model

The service maintains two isolated in-memory stores:

1. **Committed Store**
   Holds prices visible to consumers, keeping only the latest price per instrument.

2. **Batch Staging Store**
   Temporarily holds prices for the active batch and is never visible to consumers.

On batch completion, staged prices are merged atomically into the committed store.

### Thread Safety

* Batch lifecycle operations are synchronized using a single lock
* Consumer reads are lock-free
* This approach provides correctness with minimal complexity for an in-memory solution

---

## Project Structure

```text
last-value-price-service/
 ├── src/main/java
 │   └── com/spglobal/price
 │       ├── api
 │       │   ├── PriceProducerService.java
 │       │   └── PriceConsumerService.java
 │       ├── model
 │       │   ├── PriceRecord.java
 │       │   └── BatchStatus.java
 │       └── service
 │           └── InMemoryLastValuePriceService.java
 │
 ├── src/test/java
 │   └── com/spglobal/price/
 │       └── InMemoryLastValuePriceServiceTest.java
 │
 ├── pom.xml
 └── README.md
```

---

## Testing Strategy

Unit tests are written using **JUnit 5** and cover:

* Batch lifecycle behavior
* Atomic visibility of batch data
* Correct `asOf`-based price selection
* Handling of invalid producer call sequences
* Consumer access during active batch processing

Minimal logging is added to unit tests to demonstrate **inputs and outputs** during execution.

---

## How to Run

### Prerequisites

* Java 8+
* Maven 3+

### Run Tests

```bash
mvn clean test
```

---

## Performance Considerations

* Lookup of last prices is **O(1)** per instrument
* Batch processing is linear to the number of records in the batch
* Only the latest price per instrument is stored, minimizing memory usage

---

## Possible Enhancements

The following improvements are intentionally out of scope but could be added:

* Support for multiple concurrent batch runs
* Persistent storage
* Price history tracking
* Distributed or REST-based access

---

## Conclusion

This project demonstrates a clean and robust approach to converting business requirements into a maintainable Java solution. The design emphasizes correctness, clarity, and testability while remaining intentionally simpl
