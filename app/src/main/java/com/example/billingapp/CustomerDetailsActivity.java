package com.example.billingapp;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.billingapp.adapters.CustomerBillAdapter;
import com.example.billingapp.models.Bill;
import com.example.billingapp.utils.PdfUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CustomerDetailsActivity extends AppCompatActivity {

    private RecyclerView recyclerBills;
    private CustomerBillAdapter adapter;
    private TextView nameText, phoneText, paidText, pendingText;
    private FirebaseFirestore db;
    private String customerPhone;
    private String customerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_details);

        customerPhone = getIntent().getStringExtra("CUSTOMER_PHONE");
        customerName = getIntent().getStringExtra("CUSTOMER_NAME");

        if ((customerPhone == null || customerPhone.isEmpty()) && (customerName == null || customerName.isEmpty())) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        initViews();
        loadCustomerBills();
    }

    private void initViews() {
        recyclerBills = findViewById(R.id.recycler_customer_bills);
        nameText = findViewById(R.id.detail_customer_name);
        phoneText = findViewById(R.id.detail_customer_phone);
        paidText = findViewById(R.id.detail_total_paid);
        pendingText = findViewById(R.id.detail_total_pending);

        nameText.setText(customerName);
        phoneText.setText(customerPhone);

        recyclerBills.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CustomerBillAdapter(bill -> {
            // Generate PDF and Share
            File pdfFile = PdfUtils.createPdf(this, bill);
            if (pdfFile != null) {
                PdfUtils.sharePdfToWhatsApp(this, pdfFile, bill.getCustomerNumber());
            } else {
                Toast.makeText(this, "Failed to generate PDF", Toast.LENGTH_SHORT).show();
            }
        }, bill -> {
            // Mark as Paid
            showPaymentOptionDialog(bill);
        });
        recyclerBills.setAdapter(adapter);

        findViewById(R.id.btn_remind_all).setOnClickListener(v -> remindAllPending());
        findViewById(R.id.btn_edit_customer).setOnClickListener(v -> showEditCustomerDialog());
    }

    private void showEditCustomerDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        // We will create the view programmatically below
        // Re-use layout or create dynamic?
        // We don't have dialog_add_customer. Let's create the view programmatically or
        // use a simple layout.
        // I'll create a simple Linear layout programmatically to avoid creating a file
        // if possible, or use the generate_layout tool?
        // Better: I'll use a simple programmatic view.

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final com.google.android.material.textfield.TextInputLayout nameLayout = new com.google.android.material.textfield.TextInputLayout(
                this, null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox);
        nameLayout.setHint("Customer Name");
        final com.google.android.material.textfield.TextInputEditText nameInput = new com.google.android.material.textfield.TextInputEditText(
                nameLayout.getContext());
        nameInput.setText(customerName);
        nameLayout.addView(nameInput);
        layout.addView(nameLayout);

        final com.google.android.material.textfield.TextInputLayout phoneLayout = new com.google.android.material.textfield.TextInputLayout(
                this, null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox);
        phoneLayout.setHint("Phone Number");
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = 30;
        phoneLayout.setLayoutParams(params);

        final com.google.android.material.textfield.TextInputEditText phoneInput = new com.google.android.material.textfield.TextInputEditText(
                phoneLayout.getContext());
        phoneInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        phoneInput.setFilters(new android.text.InputFilter[] { new android.text.InputFilter.LengthFilter(10) });
        phoneInput.setText(customerPhone);
        phoneLayout.addView(phoneInput);
        layout.addView(phoneLayout);

        builder.setView(layout);
        builder.setTitle("Edit Customer Details");
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = nameInput.getText().toString().trim();
            String newPhone = phoneInput.getText().toString().trim();

            if (newName.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPhone.isEmpty() && newPhone.length() != 10) {
                Toast.makeText(this, "Phone number must be 10 digits", Toast.LENGTH_SHORT).show();
                return;
            }

            updateCustomerDetails(newName, newPhone);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateCustomerDetails(String newName, String newPhone) {
        if (allBills == null || allBills.isEmpty())
            return;

        com.google.firebase.firestore.WriteBatch batch = db.batch();
        for (Bill bill : allBills) {
            batch.update(db.collection("bills").document(bill.getId()),
                    "customerName", newName,
                    "customerNumber", newPhone,
                    "isUpdated", true // Mark as updated so history reflects change
            );
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Customer details updated", Toast.LENGTH_SHORT).show();
            // Update local variables
            customerName = newName;
            customerPhone = newPhone;

            // Update UI
            nameText.setText(customerName);
            phoneText.setText(customerPhone);

            // Reload to ensure consistency (optional, but safer)
            loadCustomerBills();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to update details", Toast.LENGTH_SHORT).show();
        });
    }

    private void remindAllPending() {
        // Collect pending bills
        List<Bill> pendingBills = new ArrayList<>();
        // Current adapter bills might be mixed, we need to filter from the loaded
        // source or use adapter's list if accessible.
        // Assuming 'adapter' holds the current full list or we can re-query/re-filter
        // locally.
        // Better: store bills in a class member 'allBills'.
        // For now, I'll use the 'loadCustomerBills' logic to store them in a member
        // variable.
        if (allBills == null || allBills.isEmpty()) {
            Toast.makeText(this, "No bills found", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder message = new StringBuilder();
        message.append("Hello ").append(customerName).append(",\n\n");
        message.append("You have the following pending bills with us:\n");

        double totalPending = 0;
        int count = 0;
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault());

        for (Bill bill : allBills) {
            if ("Pending".equalsIgnoreCase(bill.getStatus())) {
                message.append("- ").append(sdf.format(new java.util.Date(bill.getTimestamp())))
                        .append(": ₹").append(String.format("%.2f", bill.getFinalAmount())).append("\n");
                totalPending += bill.getFinalAmount();
                count++;
            }
        }

        if (count == 0) {
            Toast.makeText(this, "No pending bills to remind!", Toast.LENGTH_SHORT).show();
            return;
        }

        message.append("\nTotal Pending Amount: ₹").append(String.format("%.2f", totalPending));
        message.append("\n\nPlease pay at your earliest convenience. Thank you!");

        shareTextToWhatsApp(message.toString(), customerPhone);
    }

    private void shareTextToWhatsApp(String message, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty())
            return;
        String formattedNumber = phoneNumber.replace("+", "").replace(" ", "").trim();
        if (formattedNumber.length() == 10)
            formattedNumber = "91" + formattedNumber;

        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
        try {
            String url = "https://api.whatsapp.com/send?phone=" + formattedNumber + "&text="
                    + java.net.URLEncoder.encode(message, "UTF-8");
            intent.setData(android.net.Uri.parse(url));
            intent.setPackage("com.whatsapp");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp not installed or error", Toast.LENGTH_SHORT).show();
            // Fallback
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);
            startActivity(android.content.Intent.createChooser(shareIntent, "Share Reminder"));
        }
    }

    private List<Bill> allBills = new ArrayList<>();

    private void loadCustomerBills() {
        com.google.firebase.firestore.Query query;
        if (customerPhone != null && !customerPhone.isEmpty()) {
            query = db.collection("bills").whereEqualTo("customerNumber", customerPhone);
        } else {
            // Fallback to name match
            query = db.collection("bills").whereEqualTo("customerName", customerName);
        }

        query.get()
                .addOnSuccessListener(snapshots -> {
                    allBills.clear();
                    double totalPaid = 0;
                    double totalPending = 0;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Bill bill = doc.toObject(Bill.class);
                        // Ensure ID is set
                        if (bill.getId() == null)
                            bill.setId(doc.getId());

                        allBills.add(bill);

                        if ("Pending".equalsIgnoreCase(bill.getStatus())) {
                            totalPending += bill.getFinalAmount();
                        } else {
                            totalPaid += bill.getFinalAmount();
                        }
                    }

                    // Sort by Date Descending
                    allBills.sort((b1, b2) -> Long.compare(b2.getTimestamp(), b1.getTimestamp()));

                    adapter.setBills(allBills);

                    paidText.setText(String.format("₹%.2f", totalPaid));
                    pendingText.setText(String.format("₹%.2f", totalPending));

                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading bills", Toast.LENGTH_SHORT).show();
                });
    }

    private void showPaymentOptionDialog(Bill bill) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Select Payment Method");
        builder.setMessage("How was this bill paid?");

        builder.setPositiveButton("Cash", (dialog, which) -> {
            updateBillStatus(bill, "Cash");
        });

        builder.setNegativeButton("Online", (dialog, which) -> {
            updateBillStatus(bill, "Online");
        });

        builder.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void updateBillStatus(Bill bill, String method) {
        bill.setStatus("Paid");
        bill.setPaymentMethod(method);

        db.collection("bills").document(bill.getId())
                .set(bill)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Bill marked as Paid via " + method, Toast.LENGTH_SHORT).show();
                    loadCustomerBills(); // Refresh list
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update bill", Toast.LENGTH_SHORT).show());
    }
}
