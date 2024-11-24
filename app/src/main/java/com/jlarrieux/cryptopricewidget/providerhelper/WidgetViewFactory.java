package com.jlarrieux.cryptopricewidget.providerhelper;


import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;

import com.jlarrieux.cryptopricewidget.CryptoPriceWidgetUtils;
import com.jlarrieux.cryptopricewidget.R;
import com.jlarrieux.cryptopricewidget.record.CryptoPriceRecord;

import java.util.List;

public class WidgetViewFactory {

    public static RemoteViews createLoadingView(Context context, PendingIntentFactory pendingIntentFactory) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background);
        setupButtonIntents(views, pendingIntentFactory);
        views.setViewVisibility(R.id.price_container, View.GONE);
        views.setViewVisibility(R.id.progress_bar, View.VISIBLE);
        return views;
    }

    public static RemoteViews createSuccessView(Context context, List<CryptoPriceRecord> prices,
                                                PendingIntentFactory pendingIntentFactory) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background);
        setupButtonIntents(views, pendingIntentFactory);
        views.setViewVisibility(R.id.progress_bar, View.GONE);
        views.setViewVisibility(R.id.price_container, View.VISIBLE);
        views.removeAllViews(R.id.price_container);

        for (CryptoPriceRecord price : prices) {
            RemoteViews priceView = new RemoteViews(context.getPackageName(), R.layout.price_item);
            priceView.setTextViewText(R.id.symbol, price.symbol().toUpperCase());
            priceView.setTextViewText(R.id.price, CryptoPriceWidgetUtils.formatPrice(price.price()));
            views.addView(R.id.price_container, priceView);
        }
        return views;
    }

    public static RemoteViews createErrorView(Context context, String errorMessage,
                                              PendingIntentFactory pendingIntentFactory) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background);
        setupButtonIntents(views, pendingIntentFactory);
        views.setViewVisibility(R.id.progress_bar, View.GONE);
        views.setViewVisibility(R.id.price_container, View.GONE);
        views.setViewVisibility(R.id.error_text, View.VISIBLE);
        views.setTextViewText(R.id.error_text, "Error: " + errorMessage);
        return views;
    }

    private static void setupButtonIntents(RemoteViews views, PendingIntentFactory pendingIntentFactory) {
        views.setOnClickPendingIntent(R.id.refresh_button, pendingIntentFactory.createRefreshPendingIntent());
        views.setOnClickPendingIntent(R.id.settings_button, pendingIntentFactory.createSettingsPendingIntent());
    }
}

