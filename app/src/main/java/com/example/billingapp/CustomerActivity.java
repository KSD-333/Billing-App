package com.example.billingapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.billingapp.adapters.CustomerAdapter;
import com.example.billingapp.models.Bill;
import com.example.billingapp.models.Customer;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CustomerAdapter adapter;
    private ProgressBar progressBar;
    private TextInputEditText searchInput;
    private FirebaseFirestore db;
    private List<CustomerAdapter.CustomerSummary> allCustomers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_list);

        db = FirebaseFirestore.getInstance();

        initViews();
        loadCustomersFromBills();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_customers);
        progressBar = findViewById(R.id.progress_customer);
        searchInput = findViewById(R.id.input_search_customer);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CustomerAdapter(customer -> {
            Intent intent = new Intent(CustomerActivity.this, CustomerDetailsActivity.class);
            intent.putExtra("CUSTOMER_PHONE", customer.phoneNumber);
            intent.putExtra("CUSTOMER_NAME", customer.name);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCustomers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadCustomersFromBills() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("bills").get().addOnSuccessListener(snapshots -> {
            Map<String, CustomerAdapter.CustomerSummary> customerMap = new HashMap<>();

            for (QueryDocumentSnapshot doc : snapshots) {
                Bill bill = doc.toObject(Bill.class);

                String phone = bill.getCustomerNumber();
                String name = bill.getCustomerName() != null ? bill.getCustomerName() : "Unknown";
                String key = null;
                boolean isNameKey = false;

                if (phone != null && !phone.trim().isEmpty()) {
                    key = phone;
                } else if (!"Unknown".equals(name) && !name.trim().isEmpty()) {
                    key = "NAME_KEY:" + name;
                    isNameKey = true;
                }

                if (key != null) {
                    CustomerAdapter.CustomerSummary summary = customerMap.get(key);
                    if (summary == null) {
                        // If it's a Name Key, we pass empty phone, but we might want to display "No
                        // Number"
                        // But intent expects empty or null for logic check.
                        String displayPhone = isNameKey ? "" : phone;
                        summary = new CustomerAdapter.CustomerSummary(name, displayPhone, 0, 0);
                        customerMap.put(key, summary);
                    }

                    // Update totals
                    if ("Pending".equalsIgnoreCase(bill.getStatus())) {
                        summary.totalPending += bill.getFinalAmount();
                    } else {
                        summary.totalPaid += bill.getFinalAmount();
                    }

                    // Update Name if it's a phone-based key and we found a valid name
                    if (!isNameKey && bill.getCustomerName() != null && !bill.getCustomerName().isEmpty()) {
                        summary.name = bill.getCustomerName();
                    }
                }
            }

            allCustomers = new ArrayList<>(customerMap.values());
            // Sort by Pending Amount Descending
            allCustomers.sort((c1, c2) -> Double.compare(c2.totalPending, c1.totalPending));

            adapter.setCustomers(allCustomers);
            progressBar.setVisibility(View.GONE);
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
        });
    }

    private void filterCustomers(String query) {
        if (query.isEmpty()) {
            adapter.setCustomers(allCustomers);
            return;
        }

        List<CustomerAdapter.CustomerSummary> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (CustomerAdapter.CustomerSummary c : allCustomers) {
            if (c.name.toLowerCase().contains(lowerQuery) || c.phoneNumber.contains(lowerQuery)) {
                filtered.add(c);
            }
        }
        adapter.setCustomers(filtered);
    }
}
