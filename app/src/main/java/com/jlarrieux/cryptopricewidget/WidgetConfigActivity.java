package com.jlarrieux.cryptopricewidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.jlarrieux.cryptopricewidget.providerhelper.PreferencesManager;

import java.util.Arrays;
import java.util.List;

public class WidgetConfigActivity extends AppCompatActivity {
    private EditText coinsEditText;
    private PreferencesManager preferencesManager;

    private PreferencesManager getPreferencesManager(){
        if(preferencesManager == null){
            preferencesManager = new PreferencesManager(this);
        }
        return preferencesManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_config);

        coinsEditText = findViewById(R.id.coins_edit_text);

        // Load current coins
        List<String> currentCoins = getPreferencesManager().getWatchlistCoins();
        coinsEditText.setText(String.join(",", currentCoins));

        findViewById(R.id.save_button).setOnClickListener(v -> saveConfiguration());
    }


    private void saveConfiguration() {
        String coinsText = coinsEditText.getText().toString().trim();

        if (coinsText.isEmpty()) {
            Toast.makeText(this, "Please enter at least one coin", Toast.LENGTH_SHORT).show();
            return;
        }

        // Split by comma and clean up each coin id
        List<String> coins = Arrays.asList(coinsText.split(","));
        coins.replaceAll(String::trim);
        coins.replaceAll(String::toLowerCase);

        // Save the coins
        getPreferencesManager().setWatchlistCoins(coins);

        // Update all widgets
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName provider = new ComponentName(this, CryptoWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(provider);

        // Send broadcast to update all widgets
        Intent updateIntent = new Intent(this, CryptoWidgetProvider.class);
        updateIntent.setAction(CryptoWidgetProvider.REFRESH_ACTION);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        sendBroadcast(updateIntent);

        Toast.makeText(this, "Coins updated successfully", Toast.LENGTH_SHORT).show();
        finish();
    }

}