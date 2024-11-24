package com.jlarrieux.cryptopricewidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class CryptoWidgetProvider extends AppWidgetProvider {
    private static final String API_KEY = BuildConfig.COINGECKO_API_KEY;
    public static final String REFRESH_ACTION = "com.jlarrieux.cryptowidget.REFRESH";
    private static final String SETTINGS_ACTION = "com.jlarrieux.cryptowidget.SETTINGS";

    private static final Executor executor = Executors.newSingleThreadExecutor();
    private CryptoPriceFetcher cryptoPriceFetcher;
    private PreferencesManager preferencesManager;

    private CryptoPriceFetcher getCryptoPriceFetcher(String apiKey) {
        if (cryptoPriceFetcher == null) {
            cryptoPriceFetcher = new CryptoPriceFetcher(apiKey);
        }
        return cryptoPriceFetcher;
    }

    private PreferencesManager getPreferencesManager(Context context) {
        if (preferencesManager == null) {
            preferencesManager = new PreferencesManager(context);
        }
        return preferencesManager;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, true);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(CryptoPriceWidgetConstants.CRYPTO_PRICE_WIDGET, String.format("OnReceive called with action: %s", intent.getAction()));
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


    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, boolean animate) {
        Log.i(CryptoPriceWidgetConstants.CRYPTO_PRICE_WIDGET, "About to update widget");
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
            views.setViewVisibility(R.id.price_container, View.GONE);
            views.setViewVisibility(R.id.progress_bar, View.VISIBLE);
        }

        // Update widget immediately to show loading state
        appWidgetManager.updateAppWidget(appWidgetId, views);

        // Get saved coin list
        List<String> coins = getPreferencesManager(context).getWatchlistCoins();

        executor.execute(() -> {
            // Update widget to show error message
            RemoteViews errorViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            try {
                List<CryptoPriceRecord> prices = getCryptoPriceFetcher(API_KEY).fetchPrices(coins);

                // Create new RemoteViews for the update
                RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

                // Set up both button click handlers again
                updateViews.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent);
                updateViews.setOnClickPendingIntent(R.id.settings_button, settingsPendingIntent);

                // Hide loading indicators
                updateViews.setViewVisibility(R.id.progress_bar, View.GONE);
                updateViews.setViewVisibility(R.id.price_container, View.VISIBLE);

                // Update price views
                updateViews.removeAllViews(R.id.price_container);
                for (CryptoPriceRecord price : prices) {
                    RemoteViews priceView = new RemoteViews(context.getPackageName(), R.layout.price_item);
                    priceView.setTextViewText(R.id.symbol, price.symbol().toUpperCase());
                    priceView.setTextViewText(R.id.price, CryptoPriceWidgetUtils.formatPrice(price.price()));
                    updateViews.addView(R.id.price_container, priceView);
                }

                //clear any previous error message on successful loading.
                errorViews.setTextViewText(R.id.error_text, "");
                appWidgetManager.updateAppWidget(appWidgetId, errorViews);
                appWidgetManager.updateAppWidget(appWidgetId, updateViews);
            } catch (Exception e) {
                // Log the exception
                Log.e("CryptoWidgetProvider", String.format("Error updating widget: %s", e.toString()), e);


                errorViews.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent);
                errorViews.setOnClickPendingIntent(R.id.settings_button, settingsPendingIntent);
                errorViews.setViewVisibility(R.id.progress_bar, View.GONE);
                errorViews.setViewVisibility(R.id.price_container, View.GONE);
                errorViews.setViewVisibility(R.id.error_text, View.VISIBLE);
                errorViews.setTextViewText(R.id.error_text, "Error: " + e.getMessage());

                appWidgetManager.updateAppWidget(appWidgetId, errorViews);
            }

        });
    }

}
