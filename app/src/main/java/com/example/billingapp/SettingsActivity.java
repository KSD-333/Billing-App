package com.example.billingapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends AppCompatActivity {

    private TextInputEditText nameInput, mobileInput, gstInput, addressInput;
    private Button saveBtn;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        nameInput = findViewById(R.id.input_store_name);
        mobileInput = findViewById(R.id.input_store_mobile);
        gstInput = findViewById(R.id.input_store_gst);
        addressInput = findViewById(R.id.input_store_address);
        saveBtn = findViewById(R.id.btn_save_settings);

        prefs = getSharedPreferences("StorePrefs", MODE_PRIVATE);

        loadSettings();

        saveBtn.setOnClickListener(v -> saveSettings());
    }

    private void loadSettings() {
        nameInput.setText(prefs.getString("store_name", ""));
        mobileInput.setText(prefs.getString("store_mobile", ""));
        gstInput.setText(prefs.getString("store_gst", ""));
        addressInput.setText(prefs.getString("store_address", ""));
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("store_name", nameInput.getText().toString().trim());
        editor.putString("store_mobile", mobileInput.getText().toString().trim());
        editor.putString("store_gst", gstInput.getText().toString().trim());
        editor.putString("store_address", addressInput.getText().toString().trim());
        editor.apply();

        Toast.makeText(this, "Settings Saved Successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}
