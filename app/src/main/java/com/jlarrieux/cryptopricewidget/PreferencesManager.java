package com.jlarrieux.cryptopricewidget;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.List;

public class PreferencesManager {
    private static final String PREFS_NAME = "com.jlarrieux.cryptowidget.CryptoWidget";
    private static final String PREF_COINS = "watchlist_coins";
    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<String> getWatchlistCoins() {
        String coinsStr = prefs.getString(PREF_COINS, "bitcoin,ethereum,cardano");
        return Arrays.asList(coinsStr.split(","));
    }

    public void setWatchlistCoins(List<String> coins) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_COINS, String.join(",", coins));
        editor.apply();
    }
}
