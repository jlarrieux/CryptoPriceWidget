package com.jlarrieux.cryptopricewidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import com.jlarrieux.cryptopricewidget.providerhelper.CryptoPriceFetcher;
import com.jlarrieux.cryptopricewidget.providerhelper.CryptoPriceWidgetConstants;
import com.jlarrieux.cryptopricewidget.providerhelper.PendingIntentFactory;
import com.jlarrieux.cryptopricewidget.providerhelper.PreferencesManager;
import com.jlarrieux.cryptopricewidget.providerhelper.WidgetUpdateTask;
import com.jlarrieux.cryptopricewidget.providerhelper.WidgetViewFactory;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class CryptoWidgetProvider extends AppWidgetProvider {
    private static final String API_KEY = BuildConfig.COINGECKO_API_KEY;
    public static final String REFRESH_ACTION = "com.jlarrieux.cryptowidget.REFRESH";
    public static final String SETTINGS_ACTION = "com.jlarrieux.cryptowidget.SETTINGS";
    private static final Executor executor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),  // core pool size
            Runtime.getRuntime().availableProcessors() * 2,  // max pool size
            60L,  // keep alive time
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
    );;
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
                case REFRESH_ACTION -> handleRefreshAction(context, intent);
                case SETTINGS_ACTION -> handleSettingsAction(context);
            }
        }
    }

    private void handleRefreshAction(Context context, Intent intent) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(intent.getComponent());
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, true);
        }
    }

    private void handleSettingsAction(Context context) {
        Intent settingsIntent = new Intent(context, WidgetConfigActivity.class);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(settingsIntent);
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, boolean animate) {
        PendingIntentFactory pendingIntentFactory = new PendingIntentFactory(context);

        if (animate) {
            RemoteViews loadingVIew = WidgetViewFactory.createLoadingView(context, pendingIntentFactory);
            appWidgetManager.updateAppWidget(appWidgetId, loadingVIew);
        }

        List<String> coins = getPreferencesManager(context).getWatchlistCoins();

        WidgetUpdateTask updateTask = new WidgetUpdateTask(context, appWidgetManager, appWidgetId, coins, getCryptoPriceFetcher(API_KEY), pendingIntentFactory);

        executor.execute(updateTask);
    }

}
