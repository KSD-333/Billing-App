package com.example.billingapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.billingapp.adapters.CartAdapter;
import com.example.billingapp.models.Bill;
import com.example.billingapp.models.CartItem;
import com.example.billingapp.models.Product;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.graphics.pdf.PdfDocument;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Canvas;
import android.view.Menu;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.content.SharedPreferences;
import android.widget.BaseAdapter;
import android.widget.ListView;

public class BillingActivity extends AppCompatActivity {

    private AutoCompleteTextView productSearch;
    private RecyclerView cartRecycler;
    private CartAdapter cartAdapter;
    private TextView totalText;
    private AutoCompleteTextView customerPhoneInput;
    private AutoCompleteTextView customerNameInput;
    private Button generateBtn;

    private FirebaseFirestore db;
    private List<Product> allProducts = new ArrayList<>();
    private List<CartItem> cartItems = new ArrayList<>();
    private List<String> productNames = new ArrayList<>();

    // Customer Data Caching
    private java.util.Map<String, String> phoneToNameMap = new java.util.HashMap<>();
    private java.util.Map<String, String> nameToPhoneMap = new java.util.HashMap<>();

    private String currentBillId = null;
    private String currentInvoiceId = null;
    private String currentBillStatus = "Pending";
    private String currentBillPaymentMethod = "Cash";

    private static final int PERMISSION_REQUEST_READ_CONTACTS = 100;

    private final androidx.activity.result.ActivityResultLauncher<Intent> contactPickerLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                    Uri contactUri = result.getData().getData();
                    getContactDetails(contactUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billing);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupCart();

        loadProducts();
        loadCustomers();

        checkForEditMode();
    }

    private void checkForEditMode() {
        if (getIntent().getBooleanExtra("EDIT_MODE", false)) {
            String name = getIntent().getStringExtra("CUST_NAME");
            String phone = getIntent().getStringExtra("CUST_PHONE");
            String billId = getIntent().getStringExtra("BILL_ID");

            customerNameInput.setText(name);
            customerPhoneInput.setText(phone);

            // To be truly useful, we should ideally fetch items for this billId
            // and populate cartItems.
            if (billId != null) {
                currentBillId = billId; // Store ID
                db.collection("bills").document(billId).get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Bill bill = doc.toObject(Bill.class);
                        if (bill != null && bill.getItems() != null) {
                            cartItems.addAll(bill.getItems());
                            cartAdapter.setCartItems(cartItems);
                            calculateTotal();

                            // Capture Invoice ID if exists
                            currentInvoiceId = bill.getInvoiceId();

                            // Capture Status and Payment Method
                            if (bill.getStatus() != null)
                                currentBillStatus = bill.getStatus();
                            if (bill.getPaymentMethod() != null)
                                currentBillPaymentMethod = bill.getPaymentMethod();
                        }
                    }
                });
            }
        }
    }

    private void initViews() {
        productSearch = findViewById(R.id.autocomplete_product);
        cartRecycler = findViewById(R.id.recycler_cart);
        totalText = findViewById(R.id.text_total_amount);
        customerPhoneInput = findViewById(R.id.input_customer_phone);
        customerNameInput = findViewById(R.id.input_customer_name);
        customerNameInput = findViewById(R.id.input_customer_name);
        generateBtn = findViewById(R.id.btn_generate_bill);

        // Restrict phone input to 10 digits
        customerPhoneInput.setFilters(new android.text.InputFilter[] {
                new android.text.InputFilter.LengthFilter(10)
        });
        customerPhoneInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);

        View parent = (View) findViewById(R.id.input_customer_phone).getParent();
        com.google.android.material.textfield.TextInputLayout phoneLayout = null;

        if (parent != null && parent.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
            phoneLayout = (com.google.android.material.textfield.TextInputLayout) parent.getParent();
        }

        if (phoneLayout != null) {
            phoneLayout.setEndIconOnClickListener(v -> pickContact());
        }

        // Product Search View All Listener
        View productParent = (View) findViewById(R.id.autocomplete_product).getParent();
        if (productParent != null
                && productParent.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
            com.google.android.material.textfield.TextInputLayout productLayout = (com.google.android.material.textfield.TextInputLayout) productParent
                    .getParent();
            productLayout.setEndIconOnClickListener(v -> showProductSelectionSheet());
        }

        // Search Customer by Phone
        customerPhoneInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 10) {
                    searchCustomer(s.toString());
                }
            }
        });

        generateBtn.setOnClickListener(v -> showCheckoutDialog());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_billing, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showProductSelectionSheet() {
        if (allProducts.isEmpty()) {
            Toast.makeText(this, "No products available", Toast.LENGTH_SHORT).show();
            return;
        }

        com.google.android.material.bottomsheet.BottomSheetDialog sheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(
                this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_product_selection_sheet, null);
        sheetDialog.setContentView(sheetView);

        RecyclerView recycler = sheetView.findViewById(R.id.recycler_product_selection);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        com.example.billingapp.adapters.ProductSelectionAdapter adapter = new com.example.billingapp.adapters.ProductSelectionAdapter(
                product -> {
                    addProductToCart(product.getName());
                    sheetDialog.dismiss();
                    hideKeyboard();
                });

        adapter.setProducts(allProducts);
        recycler.setAdapter(adapter);

        sheetDialog.show();
    }

    private void calculateTotal() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getTotalPrice();
        }
        totalText.setText(String.format("₹%.2f", total));

        // Update Item Count
        TextView countText = findViewById(R.id.text_cart_count);
        if (countText != null) {
            countText.setText(cartItems.size() + " Items");
        }
    }

    private void pickContact() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[] { android.Manifest.permission.READ_CONTACTS }, PERMISSION_REQUEST_READ_CONTACTS);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            contactPickerLauncher.launch(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                pickContact();
            } else {
                Toast.makeText(this, "Permission denied to read contacts", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getContactDetails(Uri uri) {
        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER);
                int nameIndex = cursor
                        .getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);

                String number = cursor.getString(numberIndex);
                String name = cursor.getString(nameIndex);

                // Clean up number
                if (number != null) {
                    number = number.replaceAll("\\s+", "").replaceAll("-", "");
                    if (number.length() > 10) {
                        // Simple extraction of last 10 digits if possible, or leave as is
                        // This is simplistic, but fits common use cases
                        number = number.substring(number.length() - 10);
                    }
                    customerPhoneInput.setText(number);
                }
                if (name != null) {
                    customerNameInput.setText(name);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to get contact", Toast.LENGTH_SHORT).show();
        }
    }

    // ... (rest of methods like showCheckoutDialog)

    private void showCheckoutDialog() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        com.google.android.material.bottomsheet.BottomSheetDialog sheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(
                this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_checkout_bottom_sheet, null);
        sheetDialog.setContentView(sheetView);
        sheetDialog.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);

        TextView sheetTotalText = sheetView.findViewById(R.id.sheet_text_total);
        TextView sheetFinalText = sheetView.findViewById(R.id.sheet_text_final);
        com.google.android.material.textfield.TextInputEditText sheetDiscountInput = sheetView
                .findViewById(R.id.sheet_input_discount);
        android.widget.RadioGroup sheetPaymentGroup = sheetView.findViewById(R.id.sheet_group_payment);
        android.widget.RadioGroup sheetStatusGroup = sheetView.findViewById(R.id.sheet_group_status);

        Button btnSaveOnly = sheetView.findViewById(R.id.sheet_btn_save_only);
        Button btnSaveShare = sheetView.findViewById(R.id.sheet_btn_confirm);

        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getTotalPrice();
        }
        sheetTotalText.setText(String.format("%.2f", total));

        updateSheetFinalAmount(total, (EditText) sheetDiscountInput, sheetFinalText);
        final double finalTotalBase = total;

        sheetDiscountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateSheetFinalAmount(finalTotalBase, sheetDiscountInput, sheetFinalText);
            }
        });

        View.OnClickListener confirmListener = v -> {
            boolean share = (v.getId() == R.id.sheet_btn_confirm);
            double discount = 0;
            try {
                discount = Double.parseDouble(sheetDiscountInput.getText().toString());
            } catch (Exception e) {
            }

            double finalAmt = finalTotalBase - discount;
            if (finalAmt < 0)
                finalAmt = 0;

            String paymentMethod = "Cash";
            int selectedPaymentId = sheetPaymentGroup.getCheckedRadioButtonId();
            if (selectedPaymentId == R.id.sheet_radio_online)
                paymentMethod = "Online";

            String status = "Paid";
            int selectedStatusId = sheetStatusGroup.getCheckedRadioButtonId();
            if (selectedStatusId == R.id.sheet_radio_pending)
                status = "Pending";

            // Update current state variables in case they save but don't finish
            currentBillPaymentMethod = paymentMethod;
            currentBillStatus = status;

            generateBill(finalTotalBase, discount, finalAmt, paymentMethod, status, share);
            sheetDialog.dismiss();
        };

        btnSaveOnly.setOnClickListener(confirmListener);
        btnSaveShare.setOnClickListener(confirmListener);

        // Pre-select based on current/edited bill state
        if ("Online".equalsIgnoreCase(currentBillPaymentMethod)) {
            sheetPaymentGroup.check(R.id.sheet_radio_online);
        } else {
            sheetPaymentGroup.check(R.id.sheet_radio_cash);
        }

        if ("Pending".equalsIgnoreCase(currentBillStatus)) {
            sheetStatusGroup.check(R.id.sheet_radio_pending);
            disablePaymentMethod(sheetPaymentGroup);
        } else {
            sheetStatusGroup.check(R.id.sheet_radio_paid);
            enablePaymentMethod(sheetPaymentGroup);
        }

        sheetStatusGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sheet_radio_pending) {
                disablePaymentMethod(sheetPaymentGroup);
            } else {
                enablePaymentMethod(sheetPaymentGroup);
            }
        });

        sheetDialog.show();
    }

    private void updateSheetFinalAmount(double total, EditText discountInput, TextView finalText) {
        double discount = 0;
        try {
            discount = Double.parseDouble(discountInput.getText().toString());
        } catch (Exception e) {
        }

        double finalAmount = total - discount;
        if (finalAmount < 0)
            finalAmount = 0;
        finalText.setText(String.format("₹%.2f", finalAmount));
    }

    private void disablePaymentMethod(android.widget.RadioGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            group.getChildAt(i).setEnabled(false);
        }
        group.setAlpha(0.5f);
    }

    private void enablePaymentMethod(android.widget.RadioGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            group.getChildAt(i).setEnabled(true);
        }
        group.setAlpha(1.0f);
    }

    // ... calculateTotal etc

    private void generateBill(double total, double discount, double finalAmt, String paymentMethod, String status,
            boolean share) {
        String phone = customerPhoneInput.getText().toString();
        String name = customerNameInput.getText().toString();

        long timestamp = System.currentTimeMillis();
        // Use existing invoice ID or generate new one
        if (currentInvoiceId == null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd-HHmm",
                    java.util.Locale.getDefault());
            currentInvoiceId = "INV-" + sdf.format(new java.util.Date());
        }

        // Pass currentBillId to constructor so it's saved in the object too
        Bill bill = new Bill(currentBillId, null, name, phone, cartItems, total, discount, finalAmt, timestamp,
                status, paymentMethod);
        bill.setInvoiceId(currentInvoiceId);

        if (currentBillId != null) {
            // Update existing
            bill.setUpdated(true);
            db.collection("bills").document(currentBillId).set(bill)
                    .addOnSuccessListener(aVoid -> {
                        onBillSaved(bill, share);
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error updating bill", Toast.LENGTH_SHORT).show());
        } else {
            // Create new
            db.collection("bills").add(bill)
                    .addOnSuccessListener(ref -> {
                        bill.setId(ref.getId()); // Update local object with new ID
                        onBillSaved(bill, share);
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error saving bill", Toast.LENGTH_SHORT).show());
        }
    }

    private void onBillSaved(Bill bill, boolean share) {
        Toast.makeText(this, "Bill Saved", Toast.LENGTH_SHORT).show();
        File pdfFile = createPdf(bill);
        clearBillingData();
        if (share && pdfFile != null) {
            sharePdfToWhatsApp(pdfFile, bill.getCustomerNumber());
        }
    }

    private void clearBillingData() {
        cartItems.clear();
        cartAdapter.setCartItems(cartItems);
        calculateTotal();

        customerNameInput.setText("");
        customerPhoneInput.setText("");
        productSearch.setText("");

        // Reset edit mode if active
        getIntent().removeExtra("EDIT_MODE");
        currentBillId = null;
        currentInvoiceId = null;
        currentBillStatus = "Pending";
        currentBillPaymentMethod = "Cash";
    }

    private void setupCart() {
        cartAdapter = new CartAdapter();
        cartRecycler.setLayoutManager(new LinearLayoutManager(this));
        cartRecycler.setAdapter(cartAdapter);
        cartAdapter.setOnCartActionListener(new CartAdapter.OnCartActionListener() {
            @Override
            public void onRemove(CartItem item) {
                cartItems.remove(item);
                cartAdapter.setCartItems(cartItems);
                calculateTotal();
            }

            @Override
            public void onEdit(CartItem item) {
                showQuantityDialog(item, item, false);
            }
        });
    }

    private void loadProducts() {
        db.collection("products").get().addOnSuccessListener(snapshots -> {
            allProducts.clear();
            productNames.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                Product p = doc.toObject(Product.class);
                p.setId(doc.getId());
                allProducts.add(p);
                productNames.add(p.getName());
            }

            // Sort Alphabetically
            java.util.Collections.sort(allProducts, (p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));

            // Use custom layout for dropdown
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_dropdown_suggestion, productNames);
            productSearch.setAdapter(adapter);

            productSearch.setOnItemClickListener((parent, view, position, id) -> {
                String selectedName = (String) parent.getItemAtPosition(position);
                hideKeyboard();
                addProductToCart(selectedName);
                productSearch.setText("");
            });
        });

    }

    private void loadCustomers() {
        db.collection("bills").get().addOnSuccessListener(snapshots -> {
            phoneToNameMap.clear();
            nameToPhoneMap.clear();

            for (QueryDocumentSnapshot doc : snapshots) {
                String name = doc.getString("customerName");
                String phone = doc.getString("customerNumber");

                if (name != null && !name.trim().isEmpty() && phone != null && !phone.trim().isEmpty()) {
                    name = name.trim();
                    phone = phone.trim();

                    // Store latest mapping (overwrites old ones if any)
                    phoneToNameMap.put(phone, name);
                    nameToPhoneMap.put(name, phone);
                }
            }

            // Setup AutoComplete for Name
            List<String> customerList = new ArrayList<>(nameToPhoneMap.keySet());
            ArrayAdapter<String> nameAdapter = new ArrayAdapter<>(this, R.layout.item_dropdown_suggestion,
                    customerList);
            customerNameInput.setAdapter(nameAdapter);

            // Setup AutoComplete for Phone
            List<String> phoneList = new ArrayList<>(phoneToNameMap.keySet());
            ArrayAdapter<String> phoneAdapter = new ArrayAdapter<>(this, R.layout.item_dropdown_suggestion, phoneList);
            customerPhoneInput.setAdapter(phoneAdapter);

            // Interaction Logic: When Name Selected -> Fill Phone
            customerNameInput.setOnItemClickListener((parent, view, position, id) -> {
                hideKeyboard();
                String selectedName = (String) parent.getItemAtPosition(position);
                String associatedPhone = nameToPhoneMap.get(selectedName);
                if (associatedPhone != null) {
                    customerPhoneInput.setText(associatedPhone);
                }
            });

            // Interaction Logic: When Phone Selected -> Fill Name
            customerPhoneInput.setOnItemClickListener((parent, view, position, id) -> {
                hideKeyboard();
                String selectedPhone = (String) parent.getItemAtPosition(position);
                String associatedName = phoneToNameMap.get(selectedPhone);
                if (associatedName != null) {
                    customerNameInput.setText(associatedName);
                }
            });
        });
    }

    private void searchCustomer(String phone) {
        db.collection("bills")
                .whereEqualTo("customerNumber", phone)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        Bill lastBill = snapshots.getDocuments().get(0).toObject(Bill.class);
                        if (lastBill != null && lastBill.getCustomerName() != null) {
                            customerNameInput.setText(lastBill.getCustomerName());

                        }
                    }
                });
    }

    private void addProductToCart(String productName) {
        Product selected = null;
        for (Product p : allProducts) {
            if (p.getName().equals(productName)) {
                selected = p;
                break;
            }
        }

        if (selected != null) {
            CartItem existingItem = null;
            for (CartItem item : cartItems) {
                // Fixed: use getId instead of getProductId
                if (item.getId().equals(selected.getId())) {
                    existingItem = item;
                    break;
                }
            }

            // if existingItem != null, we are in "Merge" mode (isMerge = true)
            // if existingItem == null, we are in "Add New" mode
            // We'll pass existingItem in both cases. Distinction is:
            // If existingItem is passed from here, we usually mean merge.
            // But wait, "Edit" button also passes existingItem.
            // So we need a boolean `isMerge`.

            showQuantityDialog(selected, existingItem, existingItem != null);
        }
    }

    private void showQuantityDialog(Product product, CartItem itemToEdit, boolean isMerge) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_quantity, null);
        builder.setView(view);

        TextView title = view.findViewById(R.id.dialog_title);
        TextView subtitle = view.findViewById(R.id.dialog_subtitle);
        TextView infoCurrent = view.findViewById(R.id.dialog_info_current);
        TextView infoTotal = view.findViewById(R.id.dialog_info_total);
        EditText input = view.findViewById(R.id.input_quantity);
        Button btnAdd = view.findViewById(R.id.btn_add);
        Button btnCancel = view.findViewById(R.id.btn_cancel);

        subtitle.setText("Enter quantity for " + product.getName() + " (" + product.getUnitType() + ")");

        if (itemToEdit != null) {
            if (isMerge) {
                // MERGE MODE (Add to existing)
                title.setText("Add More Items");
                btnAdd.setText("Add to Cart");

                infoCurrent.setVisibility(View.VISIBLE);
                infoTotal.setVisibility(View.VISIBLE);

                double currentQty = itemToEdit.getQuantity();
                infoCurrent.setText("Current in Cart: " + currentQty + " " + product.getUnitType());
                infoTotal.setText("Total will be: " + currentQty + " " + product.getUnitType());

                input.setText(""); // Start empty for user to add new amount
                input.setHint("Qty to Add");

                input.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        try {
                            double addQty = s.toString().isEmpty() ? 0 : Double.parseDouble(s.toString());
                            double total = currentQty + addQty;
                            infoTotal.setText("Total will be: " + total + " " + product.getUnitType());
                        } catch (NumberFormatException e) {
                            infoTotal.setText("Total will be: " + currentQty);
                        }
                    }
                });

            } else {
                // EDIT MODE (Replace quantity)
                title.setText("Edit Quantity");
                btnAdd.setText("Update");
                input.setText(String.valueOf(itemToEdit.getQuantity()));
            }
        } else {
            // NEW MODE
            title.setText("Add to Cart");
            btnAdd.setText("Add");
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
        }

        btnAdd.setOnClickListener(v -> {
            String qtyStr = input.getText().toString();
            if (!qtyStr.isEmpty()) {
                double qty = Double.parseDouble(qtyStr);

                if (itemToEdit != null) {
                    if (isMerge) {
                        // Add to existing
                        double newTotal = itemToEdit.getQuantity() + qty;
                        itemToEdit.setQuantity(newTotal);
                    } else {
                        // Replace existing
                        itemToEdit.setQuantity(qty);
                    }
                } else {
                    cartItems.add(new CartItem(product, qty));
                }

                cartAdapter.setCartItems(cartItems);
                calculateTotal();
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private File createPdf(Bill bill) {
        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size approx
        PdfDocument.Page page = doc.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        // Load settings
        SharedPreferences prefs = getSharedPreferences("StorePrefs", MODE_PRIVATE);
        String storeName = prefs.getString("store_name", "My Store");
        String storeMobile = prefs.getString("store_mobile", "1234567890");
        String storeGst = prefs.getString("store_gst", "GSTIN4754545448544855");
        String storeAddress = prefs.getString("store_address", "A/p Burli Dist:Sangli");

        int x = 40, y = 50;

        // Store Details
        paint.setColor(Color.BLACK);
        paint.setTextSize(24);
        paint.setFakeBoldText(true);
        canvas.drawText(storeName, x, y, paint);
        y += 30;

        paint.setTextSize(14);
        paint.setFakeBoldText(false);
        if (!storeAddress.isEmpty()) {
            for (String line : storeAddress.split("\n")) {
                canvas.drawText(line, x, y, paint);
                y += 20;
            }
        }

        String contactInfo = "Mobile: " + storeMobile + (storeGst.isEmpty() ? "" : " | GST: " + storeGst);
        canvas.drawText(contactInfo, x, y, paint);
        y += 30;

        // Line Separator
        paint.setStrokeWidth(1);
        canvas.drawLine(x, y, 555, y, paint);
        y += 30;

        // Customer Details Section
        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        canvas.drawText("Customer Details:", x, y, paint);

        // Date aligned to right on same header line
        paint.setFakeBoldText(false);
        String dateStr = "Date: "
                + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date(bill.getTimestamp()));
        canvas.drawText(dateStr, 350, y, paint);
        y += 25;

        // Name
        canvas.drawText("Name: " + bill.getCustomerName(), x, y, paint);
        y += 20;

        // Mobile (on new line to avoid overlap)
        canvas.drawText("Mobile: " + bill.getCustomerNumber(), x, y, paint);
        y += 30; // Extra spacing before table header

        // Table Header
        paint.setFakeBoldText(true);
        canvas.drawText("Item Name", x, y, paint);
        canvas.drawText("Qty", 300, y, paint);
        canvas.drawText("Price", 380, y, paint);
        canvas.drawText("Total", 480, y, paint);
        y += 10;
        canvas.drawLine(x, y, 555, y, paint);
        y += 25;

        paint.setFakeBoldText(false);

        // Items
        for (CartItem item : bill.getItems()) {
            canvas.drawText(item.getName(), x, y, paint);
            canvas.drawText(item.getQuantity() + " " + item.getUnitType(), 300, y, paint);
            canvas.drawText(String.format("₹%.2f", item.getPricePerUnit()), 380, y, paint);
            canvas.drawText(String.format("₹%.2f", item.getTotalPrice()), 480, y, paint);
            y += 20;
        }

        y += 10;
        canvas.drawLine(x, y, 555, y, paint);
        y += 30;

        // Totals
        // Align to right
        int valueX = 480;
        int labelX = 380;

        canvas.drawText("Subtotal:", labelX, y, paint);
        canvas.drawText(String.format("₹%.2f", bill.getTotalAmount()), valueX, y, paint);
        y += 20;

        canvas.drawText("Discount:", labelX, y, paint);
        canvas.drawText(String.format("- ₹%.2f", bill.getDiscount()), valueX, y, paint);
        y += 20;

        paint.setFakeBoldText(true);
        paint.setTextSize(16);
        canvas.drawText("Final Payable:", labelX - 20, y, paint);
        canvas.drawText(String.format("₹%.2f", bill.getFinalAmount()), valueX, y, paint);

        doc.finishPage(page);

        File file = new File(getExternalCacheDir(), "Bill_" + System.currentTimeMillis() + ".pdf");
        try {
            doc.writeTo(new FileOutputStream(file));
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            doc.close();
        }
    }

    private void hideKeyboard() {
        android.view.View view = this.getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(
                    android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void sharePdfToWhatsApp(File file, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Customer phone number not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure proper format for WhatsApp (defaults to India +91 if missing)
        String formattedNumber = phoneNumber.replace("+", "").replace(" ", "").trim();
        if (formattedNumber.length() == 10) {
            formattedNumber = "91" + formattedNumber;
        }

        Uri uri = FileProvider.getUriForFile(this, "com.example.billingapp.fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // This 'jid' extra is the key for direct message
        intent.putExtra("jid", formattedNumber + "@s.whatsapp.net");
        intent.setPackage("com.whatsapp");

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp not installed or error", Toast.LENGTH_SHORT).show();
            // Fallback to general share
            intent.setPackage(null);
            intent.removeExtra("jid"); // Remove specific target for general share
            startActivity(Intent.createChooser(intent, "Share Bill"));
        }
    }
}
