package com.example.billingapp;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.billingapp.adapters.HistoryAdapter;
import com.example.billingapp.models.Bill;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerHistory;
    private HistoryAdapter adapter;
    private FirebaseFirestore db;
    private List<Bill> billList = new ArrayList<>();
    private List<Bill> filteredList = new ArrayList<>();

    // UI Elements
    private android.widget.EditText searchInput;
    private boolean newestFirst = true; // State for sorting
    private String activeFilterType = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        db = FirebaseFirestore.getInstance();

        // Check for Analytics filters
        if (getIntent().hasExtra("FILTER_TYPE")) {
            activeFilterType = getIntent().getStringExtra("FILTER_TYPE");
            Toast.makeText(this, "Filter: " + activeFilterType, Toast.LENGTH_SHORT).show();
        }

        // Init UI
        searchInput = findViewById(R.id.input_history_search);
        recyclerHistory = findViewById(R.id.recycler_history);
        android.view.View sortBtn = findViewById(R.id.btn_sort);
        sortBtn.setOnClickListener(this::showSortMenu);

        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter();
        recyclerHistory.setAdapter(adapter);

        // Setup Actions
        adapter.setOnBillDeleteListener(this::confirmDeleteBill);
        adapter.setOnBillClickListener(this::editBill);
        adapter.setOnBillPaidListener(this::showPaymentMethodDialog);

        setupSearch();
        loadBills();
    }

    private void showSortMenu(android.view.View v) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, v);
        popup.getMenu().add("Newest First");
        popup.getMenu().add("Oldest First");
        popup.getMenu().add("Show Updated Only");
        popup.getMenu().add("Show All");

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Newest First")) {
                newestFirst = true;
            } else if (title.equals("Oldest First")) {
                newestFirst = false;
            } else if (title.equals("Show Updated Only")) {
                activeFilterType = "Updated";
            } else if (title.equals("Show All")) {
                activeFilterType = "All";
            }
            filterAndSortBills(searchInput.getText().toString());
            return true;
        });
        popup.show();
    }

    private void showPaymentMethodDialog(Bill bill) {
        String[] options = { "Cash", "Online" };
        new AlertDialog.Builder(this)
                .setTitle("Select Payment Method")
                .setItems(options, (dialog, which) -> {
                    String method = options[which];
                    updateBillStatus(bill, "Paid", method);
                })
                .show();
    }

    private void updateBillStatus(Bill bill, String status, String paymentMethod) {
        if (bill.getId() == null)
            return;

        db.collection("bills").document(bill.getId())
                .update("status", status, "paymentMethod", paymentMethod)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Bill Updated", Toast.LENGTH_SHORT).show();
                    loadBills(); // Refresh list
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show());
    }

    private void setupSearch() {
        // Search Input
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                filterAndSortBills(s.toString());
            }
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                // Hide keyboard
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(
                        INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });
    }

    private void filterAndSortBills(String query) {
        filteredList.clear();
        String lowerQuery = query.toLowerCase().trim();
        long currentTime = System.currentTimeMillis();

        for (Bill bill : billList) {
            boolean matchesSearch = false;
            // Search by Name or Phone
            if (bill.getCustomerName().toLowerCase().contains(lowerQuery))
                matchesSearch = true;
            else if (bill.getCustomerNumber() != null && bill.getCustomerNumber().contains(lowerQuery))
                matchesSearch = true;

            if (matchesSearch) {
                // Apply Analytics Filter
                boolean matchesFilter = true;
                if (!"All".equals(activeFilterType)) {
                    matchesFilter = checkFilterMatch(bill, activeFilterType, currentTime);
                }

                if (matchesFilter) {
                    filteredList.add(bill);
                }
            }
        }

        // Sort based on state variable
        java.util.Collections.sort(filteredList, (b1, b2) -> {
            if (newestFirst)
                return Long.compare(b2.getTimestamp(), b1.getTimestamp());
            else
                return Long.compare(b1.getTimestamp(), b2.getTimestamp());
        });

        adapter.setBills(filteredList);
    }

    private boolean checkFilterMatch(Bill bill, String filter, long currentTime) {
        switch (filter) {
            case "Pending":
                return "Pending".equalsIgnoreCase(bill.getStatus());
            case "Paid":
                return "Paid".equalsIgnoreCase(bill.getStatus());
            case "Cash":
                return "Cash".equalsIgnoreCase(bill.getPaymentMethod());
            case "Online":
                return "Online".equalsIgnoreCase(bill.getPaymentMethod());
            case "7days":
                return (currentTime - bill.getTimestamp()) <= (7L * 24 * 60 * 60 * 1000);
            case "30days":
                return (currentTime - bill.getTimestamp()) <= (30L * 24 * 60 * 60 * 1000);
            case "Updated":
                return bill.isUpdated();
            default:
                return true;
        }
    }

    private void editBill(Bill bill) {
        // To edit, we can pass the bill object to BillingActivity
        // Assuming BillingActivity can handle an Intent with a Bill object (or
        // Serializable/Parcelable)
        // Since Bill might not be Parcelable, we can pass ID or just fields.
        // For simplicity, let's just show a Toast or simple action for now, OR better,
        // pass just the ID
        // and let BillingActivity fetch it if we want "Resume/Edit" functionality.
        // OR better yet, implementing full edit is complex.
        // Let's implement "Clone/Edit" by passing data.

        /*
         * NOTE: Fully editing an existing bill (updating it in firestore) requires
         * passing the ID
         * and handling "update" logic instead of "add" logic in BillingActivity.
         * For now, I will implement opening BillingActivity with data pre-filled.
         */

        android.content.Intent intent = new android.content.Intent(this, BillingActivity.class);
        intent.putExtra("EDIT_MODE", true);
        intent.putExtra("BILL_ID", bill.getId());
        intent.putExtra("CUST_NAME", bill.getCustomerName());
        intent.putExtra("CUST_PHONE", bill.getCustomerNumber());
        // Passing list of complex objects via intent is tricky without Parcelable.
        // We can use a static temporary holder or just fetch from DB in BillingActivity
        // if ID is passed.
        // Let's iterate and pass arrays for simplicity or fetching.
        // FETCHING in BillingActivity is safest.

        startActivity(intent);
        // Removed finish() to allow back navigation to HistoryActivity
    }

    private void loadBills() {
        db.collection("bills")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    billList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Bill bill = doc.toObject(Bill.class);
                        bill.setId(doc.getId()); // Ensure ID is set for deletion
                        billList.add(bill);
                    }
                    filterAndSortBills(searchInput.getText().toString()); // Initial display
                })
                .addOnFailureListener(
                        e -> Toast.makeText(this, "Error loading bills: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void confirmDeleteBill(Bill bill) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Bill")
                .setMessage("Are you sure you want to delete this bill?")
                .setPositiveButton("Delete", (dialog, which) -> deleteBill(bill))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteBill(Bill bill) {
        if (bill.getId() == null)
            return;

        db.collection("bills").document(bill.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Bill Deleted", Toast.LENGTH_SHORT).show();
                    loadBills();
                })
                .addOnFailureListener(
                        e -> Toast.makeText(this, "Error deleting bill: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
