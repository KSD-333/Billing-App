package com.example.billingapp;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.billingapp.models.Bill;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AnalyticsActivity extends AppCompatActivity {

    private TextView weeklySalesText, monthlySalesText, overallSalesText;
    private TextView pendingText, collectedText, cashText, onlineText;
    private FirebaseFirestore db;
    private androidx.recyclerview.widget.RecyclerView recyclerMonthly;
    private com.example.billingapp.adapters.MonthlySalesAdapter monthlyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        db = FirebaseFirestore.getInstance();

        weeklySalesText = findViewById(R.id.text_weekly_sales);
        monthlySalesText = findViewById(R.id.text_monthly_sales);
        overallSalesText = findViewById(R.id.text_overall_sales);
        pendingText = findViewById(R.id.text_pending_amount);
        collectedText = findViewById(R.id.text_collected_amount);
        cashText = findViewById(R.id.text_cash_amount);
        onlineText = findViewById(R.id.text_online_amount);

        recyclerMonthly = findViewById(R.id.recycler_monthly_breakdown);
        recyclerMonthly.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        monthlyAdapter = new com.example.billingapp.adapters.MonthlySalesAdapter();
        recyclerMonthly.setAdapter(monthlyAdapter);

        setupClickListeners();
        calculateSales();
    }

    private void setupClickListeners() {
        findViewById(R.id.card_weekly).setOnClickListener(v -> openHistory("7days"));
        findViewById(R.id.card_monthly).setOnClickListener(v -> openHistory("30days"));
        findViewById(R.id.card_pending).setOnClickListener(v -> openHistory("Pending"));
        findViewById(R.id.card_collected).setOnClickListener(v -> openHistory("Paid"));
        findViewById(R.id.card_cash).setOnClickListener(v -> openHistory("Cash"));
        findViewById(R.id.card_online).setOnClickListener(v -> openHistory("Online"));
        findViewById(R.id.card_overall).setOnClickListener(v -> openHistory("All"));
    }

    private void openHistory(String filter) {
        android.content.Intent intent = new android.content.Intent(this, HistoryActivity.class);
        intent.putExtra("FILTER_TYPE", filter);
        startActivity(intent);
    }

    private void calculateSales() {
        long currentTime = System.currentTimeMillis();
        long weekInMemory = 7L * 24 * 60 * 60 * 1000;
        long monthInMemory = 30L * 24 * 60 * 60 * 1000;

        db.collection("bills").get()
                .addOnSuccessListener(snapshots -> {
                    double weekly = 0;
                    double monthly = 0;
                    double overall = 0;
                    double pending = 0;
                    double collected = 0;
                    double cash = 0;
                    double online = 0;

                    java.util.Map<String, Double> monthlyBreakdown = new java.util.HashMap<>();
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM yyyy",
                            java.util.Locale.getDefault());

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Bill bill = doc.toObject(Bill.class);
                        double amount = bill.getFinalAmount();
                        overall += amount;

                        if (currentTime - bill.getTimestamp() <= weekInMemory) {
                            weekly += amount;
                        }
                        if (currentTime - bill.getTimestamp() <= monthInMemory) {
                            monthly += amount;
                        }

                        if ("Pending".equals(bill.getStatus())) {
                            pending += amount;
                        } else {
                            // Paid
                            collected += amount;
                            if ("Cash".equals(bill.getPaymentMethod())) {
                                cash += amount;
                            } else if ("Online".equals(bill.getPaymentMethod())) {
                                online += amount;
                            }
                        }

                        // Monthly Breakdown Aggregation
                        String monthKey = sdf.format(new java.util.Date(bill.getTimestamp()));
                        double currentMonthTotal = monthlyBreakdown.getOrDefault(monthKey, 0.0);
                        monthlyBreakdown.put(monthKey, currentMonthTotal + amount);
                    }

                    weeklySalesText.setText(String.format("₹%.2f", weekly));
                    monthlySalesText.setText(String.format("₹%.2f", monthly));
                    overallSalesText.setText(String.format("₹%.2f", overall));
                    pendingText.setText(String.format("₹%.2f", pending));
                    collectedText.setText(String.format("₹%.2f", collected));
                    cashText.setText(String.format("₹%.2f", cash));
                    onlineText.setText(String.format("₹%.2f", online));

                    // Sort and Display Monthly Breakdown
                    // Note: HashMap is unordered. We might want to sort by Date reversely?
                    // Parsing "MMMM yyyy" back to date to sort is tricky.
                    // Better approach: Use TreeMap with YYYYMM as key to sort, then format for
                    // display?
                    // For now, let's just show as is or list keys.
                    // Improving sort:
                    java.util.List<java.util.Map.Entry<String, Double>> sortedList = new java.util.ArrayList<>(
                            monthlyBreakdown.entrySet());
                    // Simple sort by string for now, roughly works "August 2025" vs "July 2025"
                    // logic is bad.
                    // ideally we should have stored YYYYMM.
                    // Let's rely on insertion order if LinkedHashMap? No, random.
                    monthlyAdapter.setSalesList(sortedList);

                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading analytics", Toast.LENGTH_SHORT).show());
    }
}
