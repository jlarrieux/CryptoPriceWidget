package com.jlarrieux.cryptopricewidget.providerhelper;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.jlarrieux.cryptopricewidget.CryptoPriceWidgetUtils;
import com.jlarrieux.cryptopricewidget.R;
import com.jlarrieux.cryptopricewidget.record.PercentDifferenceMonthlyRecord;
import com.jlarrieux.cryptopricewidget.record.PercentDifferencesDailyRecord;

import java.util.List;

public class WidgetViewFactory {

    public static final String PercentFormat = "%.1f%%";

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
                handleDailyPercent(analysis.analysis().dailyRecord(), priceView);
                handleMonthlyPercent(analysis.analysis().monthlyRecord(), priceView);

            }

            views.addView(R.id.price_container, priceView);
        }
        return views;
    }

    @SuppressLint("DefaultLocale")
    private static void handleDailyPercent(PercentDifferencesDailyRecord dailyRecord, RemoteViews priceView) {

        // Ensure we're using String.format to avoid null values

        String diff24h = String.format(PercentFormat, dailyRecord.diff24h());
        String diff4h = String.format(PercentFormat, dailyRecord.diff4h());
        String diff1h = String.format(PercentFormat, dailyRecord.diff1h());

        // Set text with null checks
        priceView.setTextViewText(R.id.diff_24h, diff24h);
        priceView.setTextViewText(R.id.diff_4h, diff4h);
        priceView.setTextViewText(R.id.diff_1h, diff1h);

        // Set colors with null checks
        setDiffColor(priceView, R.id.diff_24h, dailyRecord.diff24h());
        setDiffColor(priceView, R.id.diff_4h, dailyRecord.diff4h());
        setDiffColor(priceView, R.id.diff_1h, dailyRecord.diff1h());
    }

    @SuppressLint("DefaultLocale")
    private static void handleMonthlyPercent(PercentDifferenceMonthlyRecord monthlyRecord, RemoteViews priceView){
        String diff1m = String.format(PercentFormat, monthlyRecord.diff1Month());
        String diff1w = String.format(PercentFormat, monthlyRecord.diff1Week());

        priceView.setTextViewText(R.id.diff_1m, diff1m);
        priceView.setTextViewText(R.id.diff_1w, diff1w);

        setDiffColor(priceView, R.id.diff_1m, monthlyRecord.diff1Month());
        setDiffColor(priceView, R.id.diff_1w, monthlyRecord.diff1Week());
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
        Log.i(CryptoPriceWidgetConstants.CRYPTO_PRICE_WIDGET, String.format("jeannius e: %s", errorMessage));
        Log.i(CryptoPriceWidgetConstants.CRYPTO_PRICE_WIDGET, String.format("jeannius message: %s", errorMessage));
        views.setTextViewText(R.id.error_text, errorMessage);
        return views;
    }

    private static void setupButtonIntents(RemoteViews views, PendingIntentFactory pendingIntentFactory) {
        views.setOnClickPendingIntent(R.id.refresh_button, pendingIntentFactory.createRefreshPendingIntent());
        views.setOnClickPendingIntent(R.id.settings_button, pendingIntentFactory.createSettingsPendingIntent());
    }
}

