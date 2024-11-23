package com.jlarrieux.cryptopricewidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CryptoWidgetProvider extends AppWidgetProvider {
    private static final String BASE_URL = "https://pro-api.coingecko.com/api/v3";
    private static final String API_KEY = BuildConfig.COINGECKO_API_KEY;
    public static final String REFRESH_ACTION = "com.jlarrieux.cryptowidget.REFRESH";
    private static final String SETTINGS_ACTION = "com.jlarrieux.cryptowidget.SETTINGS";
    private static final String PREFS_NAME = "com.jlarrieux.cryptowidget.CryptoWidget";
    private static final String PREF_COINS = "watchlist_coins";
    private static final Executor executor = Executors.newSingleThreadExecutor();
    private static final OkHttpClient client = new OkHttpClient();
    private static final int REFRESH_ANIMATION_DURATION = 1500; // 1 second

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, true);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case REFRESH_ACTION -> {
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(intent.getComponent());
                    for (int appWidgetId : appWidgetIds) {
                        updateWidget(context, appWidgetManager, appWidgetId, true);
                    }
                }
                case SETTINGS_ACTION -> {
                    // Launch settings activity with FLAG_ACTIVITY_NEW_TASK
                    Intent settingsIntent = new Intent(context, WidgetConfigActivity.class);
                    settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(settingsIntent);
                }
            }
        }
    }

    private static String formatPrice(double price) {
        // Handle different price ranges appropriately
        if (price >= 1.0) {
            // For prices $1 and above, show 2 decimal places
            // Example: 1,234.56
            return String.format("$%,.2f", price);
        } else if (price >= 0.01) {
            // For prices between $0.01 and $0.99, show 3 decimal places
            // Example: 0.123
            return String.format("$%.3f", price);
        } else {
            // For very small prices, show 4 decimal places
            // Example: 0.0012
            return String.format("$%.4f", price);
        }
    }


    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, boolean animate) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        // Set up refresh button
        Intent refreshIntent = new Intent(context, CryptoWidgetProvider.class);
        refreshIntent.setAction(REFRESH_ACTION);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                context, 0, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent);

        // Set up settings button
        Intent settingsIntent = new Intent(context, CryptoWidgetProvider.class);
        settingsIntent.setAction(SETTINGS_ACTION);
        PendingIntent settingsPendingIntent = PendingIntent.getBroadcast(
                context, 1, settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.settings_button, settingsPendingIntent);

        if (animate) {
            // Show loading state
            views.setViewVisibility(R.id.error_text, View.GONE);
            views.setViewVisibility(R.id.price_container, View.GONE);
            views.setViewVisibility(R.id.progress_bar, View.VISIBLE);
        }

        // Update widget immediately to show loading state
        appWidgetManager.updateAppWidget(appWidgetId, views);

        // Get saved coin list
        List<String> coins = getWatchlistCoins(context);

        executor.execute(() -> {
            try {
                List<CryptoPrice> prices = fetchPrices(coins);

                RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

                // Set up button click handlers
                updateViews.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent);
                updateViews.setOnClickPendingIntent(R.id.settings_button, settingsPendingIntent);

                // Hide loading indicators
                updateViews.setViewVisibility(R.id.progress_bar, View.GONE);
                updateViews.setViewVisibility(R.id.error_text, View.GONE);
                updateViews.setViewVisibility(R.id.price_container, View.VISIBLE);

                // Update price views with formatted prices
                updateViews.removeAllViews(R.id.price_container);
                for (CryptoPrice price : prices) {
                    RemoteViews priceView = new RemoteViews(context.getPackageName(), R.layout.price_item);
                    priceView.setTextViewText(R.id.symbol, price.symbol.toUpperCase());
                    priceView.setTextViewText(R.id.price, formatPrice(price.price));
                    updateViews.addView(R.id.price_container, priceView);
                }

                appWidgetManager.updateAppWidget(appWidgetId, updateViews);
            } catch (Exception e) {
                // Create new RemoteViews for error state
                RemoteViews errorViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

                // Set up both button click handlers in error state too
                errorViews.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent);
                errorViews.setOnClickPendingIntent(R.id.settings_button, settingsPendingIntent);

                // Show error state
                errorViews.setViewVisibility(R.id.progress_bar, View.GONE);
                errorViews.setViewVisibility(R.id.price_container, View.GONE);
                errorViews.setViewVisibility(R.id.error_text, View.VISIBLE);

                appWidgetManager.updateAppWidget(appWidgetId, errorViews);
            }
        });
    }

    static List<String> getWatchlistCoins(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String coinsStr = prefs.getString(PREF_COINS, "bitcoin,ethereum,cardano");
        return Arrays.asList(coinsStr.split(","));
    }

    public static void setWatchlistCoins(Context context, List<String> coins) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(PREF_COINS, String.join(",", coins));
        editor.apply();
    }

    private List<CryptoPrice> fetchPrices(List<String> coins) throws IOException, JSONException {
        String coinIds = String.join(",", coins);
        String url = BASE_URL + "/simple/price" +
                "?ids=" + coinIds +
                "&vs_currencies=usd" +
                "&x_cg_pro_api_key=" + API_KEY;

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected response " + response);

            List<CryptoPrice> prices = new ArrayList<>();
            JSONObject json = new JSONObject(response.body().string());

            for (String coin : coins) {
                if (json.has(coin)) {
                    JSONObject coinData = json.getJSONObject(coin);
                    double price = coinData.getDouble("usd");
                    prices.add(new CryptoPrice(coin, price));
                }
            }
            return prices;
        }
    }

    private record CryptoPrice(String symbol, double price) {
    }
}
