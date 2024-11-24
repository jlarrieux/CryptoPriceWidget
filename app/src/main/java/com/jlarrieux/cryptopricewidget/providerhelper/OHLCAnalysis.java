package com.jlarrieux.cryptopricewidget.providerhelper;

import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlarrieux.cryptopricewidget.record.OHLCEntryRecord;
import com.jlarrieux.cryptopricewidget.record.PercentDifferencesRecord;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class OHLCAnalysis {

    /**
     * Parses the raw JSON string response into a list of OHLCEntry records.
     *
     * @param jsonResponse JSON response as a string.
     * @return List of OHLCEntry records.
     */
    public static List<OHLCEntryRecord> parseOHLCData(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<List<Object>> rawResponse = mapper.readValue(jsonResponse, new TypeReference<>() {
            });
            return rawResponse.stream()
                    .map(entry -> new OHLCEntryRecord(
                            ((Number) entry.get(0)).longValue(),  // timestamp
                            ((Number) entry.get(1)).doubleValue(), // open
                            ((Number) entry.get(2)).doubleValue(), // high
                            ((Number) entry.get(3)).doubleValue(), // low
                            ((Number) entry.get(4)).doubleValue()  // close
                    )).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON response", e);
        }
    }

    /**
     * Finds the highest "high" value from the provided OHLC entries.
     *
     * @param ohlcEntries List of OHLCEntry records.
     * @return Highest "high" value.
     */
    private static double findHighestHigh(List<OHLCEntryRecord> ohlcEntries) {
        return ohlcEntries.stream()
                .max(Comparator.comparingDouble(OHLCEntryRecord::high))
                .map(OHLCEntryRecord::high)
                .orElseThrow(() -> new IllegalArgumentException("No OHLC data available"));
    }

    /**
     * Calculates the percent difference.
     *
     * @param current  The current value.
     * @param baseline The baseline value to compare against.
     * @return Percent difference as a double.
     */
    private static double calculatePercentDifference(double current, double baseline) {
        return ((current - baseline) / baseline) * 100;
    }

    /**
     * Computes percent differences for 24h, 4h, and 1h intervals using the specified criteria.
     *
     * @param rawResponse  List of raw OHLC data arrays.
     * @param currentValue The current price value.
     * @return A PercentDifferences record containing the percent differences.
     */
    public static PercentDifferencesRecord computePercentDifferences(String rawResponse, double currentValue) {
        Log.i(CryptoPriceWidgetConstants.CRYPTO_PRICE_WIDGET, String.format("Analysis currentPrice: %f", currentValue));
        // Parse the OHLC data
        List<OHLCEntryRecord> ohlcEntries = parseOHLCData(rawResponse);

        // Find the highest price in the last 24 hours
        double high24h = findHighestHigh(ohlcEntries);

        // Find the highest price in the last 4 hours
        List<OHLCEntryRecord> last4Hours = ohlcEntries.subList(Math.max(ohlcEntries.size() - 4, 0), ohlcEntries.size());
        double high4h = findHighestHigh(last4Hours);

        // Find the highest price in the last 1 hour
        double high1h = ohlcEntries.get(ohlcEntries.size() - 1).high();

        // Calculate percent differences
        double diff24h = calculatePercentDifference(currentValue, high24h);
        double diff4h = calculatePercentDifference(currentValue, high4h);
        double diff1h = calculatePercentDifference(currentValue, high1h);

        return new PercentDifferencesRecord(diff24h, diff4h, diff1h);
    }
}
