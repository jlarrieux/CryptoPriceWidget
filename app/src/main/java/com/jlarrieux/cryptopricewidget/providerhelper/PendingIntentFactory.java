package com.jlarrieux.cryptopricewidget.providerhelper;



import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.jlarrieux.cryptopricewidget.CryptoWidgetProvider;

public class PendingIntentFactory {
    private final Context context;

    public PendingIntentFactory(Context context) {
        this.context = context;
    }

    public PendingIntent createRefreshPendingIntent() {
        Intent refreshIntent = new Intent(context, CryptoWidgetProvider.class);
        refreshIntent.setAction(CryptoWidgetProvider.REFRESH_ACTION);
        return PendingIntent.getBroadcast(
                context, 0, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    public PendingIntent createSettingsPendingIntent() {
        Intent settingsIntent = new Intent(context, CryptoWidgetProvider.class);
        settingsIntent.setAction(CryptoWidgetProvider.SETTINGS_ACTION);
        return PendingIntent.getBroadcast(
                context, 1, settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
