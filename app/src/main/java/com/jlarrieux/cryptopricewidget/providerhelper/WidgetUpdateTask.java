package com.jlarrieux.cryptopricewidget.providerhelper;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;

import com.jlarrieux.cryptopricewidget.record.CryptoPriceRecord;
import com.jlarrieux.cryptopricewidget.record.PercentDifferencesRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
            // First get current prices
            List<CryptoPriceRecord> prices = cryptoPriceFetcher.fetchPrices(coins);

            // For each coin, fetch OHLC and calculate differences
            List<CompletableFuture<PercentDifferencesRecord>> ohlcFutures = new ArrayList<>();
            for(CryptoPriceRecord price : prices) {
                CompletableFuture<PercentDifferencesRecord> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        String ohlcData = cryptoPriceFetcher.fetchOHLC(price.symbol());
                        return OHLCAnalysis.computePercentDifferences(ohlcData, price.price());
                    } catch (Exception e) {
                        Log.e("WidgetUpdateTask", "Error fetching OHLC for " + price.symbol(), e);
                        return null;
                    }
                });
                ohlcFutures.add(future);
            }

            // Wait for all OHLC data to be fetched
            CompletableFuture.allOf(ohlcFutures.toArray(new CompletableFuture[0])).join();

            // Combine price records with their analysis
            List<CoinAnalysisRecord> analysisResults = new ArrayList<>();
            for(int i = 0; i < prices.size(); i++) {
                CryptoPriceRecord price = prices.get(i);
                PercentDifferencesRecord diffs = ohlcFutures.get(i).get();
                analysisResults.add(new CoinAnalysisRecord(price, diffs));
            }
            // clear error view first
            RemoteViews errorViews = WidgetViewFactory.createErrorView(context, "", pendingIntentFactory);
            appWidgetManager.updateAppWidget(appWidgetId, errorViews);

            // update view
            RemoteViews updateViews = WidgetViewFactory.createSuccessView(context, analysisResults, pendingIntentFactory);
            appWidgetManager.updateAppWidget(appWidgetId, updateViews);
        } catch (Exception e) {
            Log.e("WidgetUpdateTask", String.format("Error updating widget: %s", e.getMessage()), e);
            RemoteViews errorViews = WidgetViewFactory.createErrorView(context, e.getMessage(), pendingIntentFactory);
            appWidgetManager.updateAppWidget(appWidgetId, errorViews);
        }
    }
}

