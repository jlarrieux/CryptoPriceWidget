package com.jlarrieux.cryptopricewidget.providerhelper;


import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.RemoteViews;

import com.jlarrieux.cryptopricewidget.CryptoPriceWidgetUtils;
import com.jlarrieux.cryptopricewidget.R;

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

    public static RemoteViews createSuccessView(Context context,
                                                List<CoinAnalysisRecord> analyses,
                                                PendingIntentFactory pendingIntentFactory) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background);
        setupButtonIntents(views, pendingIntentFactory);
        views.setViewVisibility(R.id.progress_bar, View.GONE);
        views.setViewVisibility(R.id.price_container, View.VISIBLE);
        views.removeAllViews(R.id.price_container);

        for (CoinAnalysisRecord analysis : analyses) {
            RemoteViews priceView = new RemoteViews(context.getPackageName(), R.layout.price_item);

            // Set basic price info
            priceView.setTextViewText(R.id.symbol, analysis.price().symbol().toUpperCase());
            priceView.setTextViewText(R.id.price, CryptoPriceWidgetUtils.formatPrice(analysis.price().price()));

            // Set analysis info if available
            if (analysis.analysis() != null) {
                // Ensure we're using String.format to avoid null values
                String diff24h = String.format("%.1f%%", analysis.analysis().diff24h());
                String diff4h = String.format("%.1f%%", analysis.analysis().diff4h());
                String diff1h = String.format("%.1f%%", analysis.analysis().diff1h());

                // Set text with null checks
                if (diff24h != null) priceView.setTextViewText(R.id.diff_24h, diff24h);
                if (diff4h != null) priceView.setTextViewText(R.id.diff_4h, diff4h);
                if (diff1h != null) priceView.setTextViewText(R.id.diff_1h, diff1h);

                // Set colors with null checks
                setDiffColor(priceView, R.id.diff_24h, analysis.analysis().diff24h());
                setDiffColor(priceView, R.id.diff_4h, analysis.analysis().diff4h());
                setDiffColor(priceView, R.id.diff_1h, analysis.analysis().diff1h());
            }

            views.addView(R.id.price_container, priceView);
        }
        return views;
    }

    private static void setDiffColor(RemoteViews views, int viewId, double value) {
        try {
            int color = value >= 0 ? Color.parseColor("#00FF00") : Color.parseColor("#FF4444");
            views.setTextColor(viewId, color);
        } catch (Exception e) {
            // If there's any error setting color, default to white
            views.setTextColor(viewId, Color.WHITE);
        }
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

