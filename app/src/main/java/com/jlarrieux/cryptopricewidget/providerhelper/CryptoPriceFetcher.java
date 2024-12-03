package com.jlarrieux.cryptopricewidget.providerhelper;

import android.util.Log;

import com.jlarrieux.cryptopricewidget.record.CryptoPriceRecord;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CryptoPriceFetcher {
    private static final String BASE_URL = "https://pro-api.coingecko.com/api/v3";
    protected static final String X_CG_PRO_API_KEY = "&x_cg_pro_api_key=";
    private final OkHttpClient client;
    private final String apiKey;

    public CryptoPriceFetcher(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient();
    }

    public List<CryptoPriceRecord> fetchPrices(List<String> coins) throws JSONException, IOException {
        // Encode coin IDs (only once)
        String coinIds = URLEncoder.encode(String.join(",", coins), StandardCharsets.UTF_8);

        // Construct the full URL
        String url = String.format(
                "%s/simple/price?ids=%s&vs_currencies=usd%s%s",
                BASE_URL, coinIds, X_CG_PRO_API_KEY, apiKey
        );

        Log.i(CryptoPriceWidgetConstants.CRYPTO_PRICE_WIDGET, String.format("url: %s", url));

        // Make the HTTP request and parse the response
        String responseBody = makeRequest(url);
        return parsePrices(responseBody, coins);
    }



    public String fetchOHLC(String coin) throws IOException {
        String url = BASE_URL + "/coins/" + coin + "/ohlc?" + "vs_currency=usd" + "&days=1&interval=hourly" + X_CG_PRO_API_KEY + apiKey;
        Log.i(CryptoPriceWidgetConstants.CRYPTO_PRICE_WIDGET, String.format("url: %s", url));

        String responseBody = makeRequest(url);
        Log.i(CryptoPriceWidgetConstants.CRYPTO_PRICE_WIDGET, String.format("responsebody from OHLC", responseBody));
        return responseBody;
    }


    private List<CryptoPriceRecord> parsePrices(String jsonStr, List<String> coins) throws JSONException {
        List<CryptoPriceRecord> prices = new ArrayList<>();
        JSONObject json = new JSONObject(jsonStr);

        for (String coin : coins) {
            if (json.has(coin)) {
                JSONObject coinData = json.getJSONObject(coin);
                Log.i(CryptoPriceWidgetConstants.CRYPTO_PRICE_WIDGET, String.format("Logging coinData: %s and coin: %s", coinData, coin));
                double price = coinData.getDouble("usd");
                prices.add(new CryptoPriceRecord(coin, price));
            }
        }
        return prices;
    }

    private String makeRequest(String url) throws IOException {

        try {
            return executeRequest(url);
        } catch (Exception e) {
            Log.e(CryptoPriceWidgetConstants.CRYPTO_PRICE_WIDGET, String.format("error: %s", e.getMessage()), e);
            // Check if the error is related to DNS resolution
            if (e.getMessage() != null && e.getMessage().contains("Unable to resolve host")) {
                Log.w(CryptoPriceWidgetConstants.CRYPTO_PRICE_WIDGET, "DNS resolution failed. Retrying with proxy.");

                // Use the proxy if DNS resolution fails
                String proxyBaseUrl = "https://caaqkesk23.execute-api.us-east-1.amazonaws.com/dev/proxy";
                String encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8);
                String proxyUrl = String.format("%s?url=%s", proxyBaseUrl, encodedUrl);

                return executeRequest(proxyUrl);
            } else {
                // Rethrow other exceptions
                throw e;
            }
        }
    }

    private String executeRequest(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected response: " + response);

            assert response.body() != null;
            String responseBody = response.body().string();
            Log.i(CryptoPriceWidgetConstants.CRYPTO_PRICE_WIDGET, String.format("responseBody: %s", responseBody));
            return responseBody;
        }
    }
}
