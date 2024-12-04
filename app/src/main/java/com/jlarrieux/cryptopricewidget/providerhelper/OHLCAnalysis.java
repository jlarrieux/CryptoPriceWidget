package com.jlarrieux.cryptopricewidget.providerhelper;

import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlarrieux.cryptopricewidget.record.OHLCEntryRecord;
import com.jlarrieux.cryptopricewidget.record.PercentDifferenceMonthlyRecord;
import com.jlarrieux.cryptopricewidget.record.PercentDifferencesDailyRecord;

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
    public static PercentDifferencesDailyRecord computePercentDifferencesForDaily(String rawResponse, double currentValue) {
        Log.i(CryptoPriceWidgetConstants.CRYPTO_PRICE_WIDGET, String.format("Analysis currentPrice: %f", currentValue));
        // Parse the OHLC data
        List<OHLCEntryRecord> ohlcEntries = parseOHLCData(rawResponse);

        // Ensure data is sorted from oldest to newest
        ohlcEntries.sort(Comparator.comparingLong(OHLCEntryRecord::timestamp));

        int totalEntries = ohlcEntries.size();

        // Calculate indices for 24h, 4h, and 1h intervals
        int index24h = Math.max(totalEntries - 24, 0);
        int index4h = Math.max(totalEntries - 4, 0);
        int index1h = Math.max(totalEntries - 1, 0);

        // Get the closing prices at the start of each interval
        double close24h = ohlcEntries.get(index24h).close();
        double close4h = ohlcEntries.get(index4h).close();
        double close1h = ohlcEntries.get(index1h).close();

        // Calculate percent differences
        double diff24h = calculatePercentDifference(currentValue, close24h);
        double diff4h = calculatePercentDifference(currentValue, close4h);
        double diff1h = calculatePercentDifference(currentValue, close1h);

        return new PercentDifferencesDailyRecord(diff24h, diff4h, diff1h);
    }

    /**
     * Computes percent differences for 1month and 1week intervals using the specified criteria.
     *
     * @param rawResponse list of OHLC data arrays
     * @param currentValue the current price value
     * @return a PercentDifferences record containing the percent differences
     */
    public static PercentDifferenceMonthlyRecord computePercentDifferencesForMonthly(String rawResponse, double currentValue) {
        List<OHLCEntryRecord> ohlcEntries = parseOHLCData(rawResponse);
        ohlcEntries.sort(Comparator.comparingLong(OHLCEntryRecord::timestamp));

        int totalEntries = ohlcEntries.size();
        int index1m = Math.max(totalEntries - 30, 0); // Approx. 1 month
        int index1w = Math.max(totalEntries - 7, 0);  // Approx. 1 week

        double close1m = ohlcEntries.get(index1m).close();
        double close1w = ohlcEntries.get(index1w).close();

        double diff1m = calculatePercentDifference(currentValue, close1m);
        double diff1w = calculatePercentDifference(currentValue, close1w);

        return new PercentDifferenceMonthlyRecord(diff1m, diff1w);
    }


}
