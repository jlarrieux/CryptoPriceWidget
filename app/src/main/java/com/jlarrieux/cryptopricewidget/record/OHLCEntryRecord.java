package com.jlarrieux.cryptopricewidget.record;

// Record to represent an OHLC entry
public record OHLCEntryRecord(long timestamp, double open, double high, double low, double close) {
}
