package com.example.billingapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialCardView cardStore = findViewById(R.id.card_store);
        MaterialCardView cardBilling = findViewById(R.id.card_billing);
        MaterialCardView cardAnalytics = findViewById(R.id.card_analytics);

        if (cardStore != null) {
            cardStore.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, StoreActivity.class)));
        }
        if (cardBilling != null) {
            cardBilling.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, BillingActivity.class)));
        }
        if (cardAnalytics != null) {
            cardAnalytics
                    .setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AnalyticsActivity.class)));
        }

        MaterialCardView cardHistory = findViewById(R.id.card_history);
        if (cardHistory != null) {
            cardHistory.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, HistoryActivity.class)));
        }

        android.view.View btnTheme = findViewById(R.id.btn_theme_toggle);
        if (btnTheme != null) {
            btnTheme.setOnClickListener(v -> {
                int currentNightMode = getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                    androidx.appcompat.app.AppCompatDelegate
                            .setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                } else {
                    androidx.appcompat.app.AppCompatDelegate
                            .setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                }
            });
        }

        MaterialCardView cardCustomers = findViewById(R.id.card_customers);
        if (cardCustomers != null) {
            cardCustomers.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CustomerActivity.class)));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_dark_mode) {
            int currentNightMode = getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                androidx.appcompat.app.AppCompatDelegate
                        .setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                androidx.appcompat.app.AppCompatDelegate
                        .setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}