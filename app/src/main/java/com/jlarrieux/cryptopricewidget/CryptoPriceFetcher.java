package com.jlarrieux.cryptopricewidget;

import android.util.Log;

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
    private final OkHttpClient client;
    private final String apiKey;

    public CryptoPriceFetcher(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient();
    }

    public List<CryptoPriceRecord> fetchPrices(List<String> coins) throws IOException, JSONException {
        String coinIds = URLEncoder.encode(String.join(",", coins), StandardCharsets.UTF_8.toString());
        String url = BASE_URL + "/simple/price?ids=" + coinIds + "&vs_currencies=usd" + "&x_cg_pro_api_key=" + apiKey;
        Log.i("CryptoPriceWidget", "Request URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                // Remove the header since we're using query parameter
                //.addHeader("x-cg-pro-api-key", apiKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected response: " + response);

            String responseBody = response.body().string();
            return parsePrices(responseBody, coins);
        }
    }


    private List<CryptoPriceRecord> parsePrices(String jsonStr, List<String> coins) throws JSONException {
        List<CryptoPriceRecord> prices = new ArrayList<>();
        JSONObject json = new JSONObject(jsonStr);

        for (String coin : coins) {
            if (json.has(coin)) {
                JSONObject coinData = json.getJSONObject(coin);
                double price = coinData.getDouble("usd");
                prices.add(new CryptoPriceRecord(coin, price));
            }
        }
        return prices;
    }
}
