package com.jlarrieux.cryptopricewidget.providerhelper;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.jlarrieux.cryptopricewidget.record.CryptoPriceRecord;

import java.util.List;

public class WidgetUpdateTask implements Runnable {
    private final Context context;
    private final AppWidgetManager appWidgetManager;
    private final int appWidgetId;
    private final List<String> coins;
    private final CryptoPriceFetcher cryptoPriceFetcher;
    private final PendingIntentFactory pendingIntentFactory;

    public WidgetUpdateTask(Context context, AppWidgetManager appWidgetManager, int appWidgetId,
                            List<String> coins, CryptoPriceFetcher cryptoPriceFetcher,
                            PendingIntentFactory pendingIntentFactory) {
        this.context = context;
        this.appWidgetManager = appWidgetManager;
        this.appWidgetId = appWidgetId;
        this.coins = coins;
        this.cryptoPriceFetcher = cryptoPriceFetcher;
        this.pendingIntentFactory = pendingIntentFactory;
    }

    @Override
    public void run() {
        try {
            List<CryptoPriceRecord> prices = cryptoPriceFetcher.fetchPrices(coins);
            // Perform any additional data processing if needed

            RemoteViews updateViews = WidgetViewFactory.createSuccessView(context, prices, pendingIntentFactory);
            appWidgetManager.updateAppWidget(appWidgetId, updateViews);
        } catch (Exception e) {
            Log.e("WidgetUpdateTask", "Error updating widget", e);
            RemoteViews errorViews = WidgetViewFactory.createErrorView(context, e.getMessage(), pendingIntentFactory);
            appWidgetManager.updateAppWidget(appWidgetId, errorViews);
        }
    }
}

