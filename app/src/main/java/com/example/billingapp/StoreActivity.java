package com.example.billingapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.billingapp.adapters.ProductAdapter;
import com.example.billingapp.models.Product;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class StoreActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private FirebaseFirestore db;
    private List<Product> productList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        db = FirebaseFirestore.getInstance();
        productList = new ArrayList<>();

        recyclerView = findViewById(R.id.recycler_products);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter();
        recyclerView.setAdapter(adapter);

        adapter.setOnProductActionListener(new ProductAdapter.OnProductActionListener() {
            @Override
            public void onEdit(Product product) {
                showAddEditDialog(product);
            }

            @Override
            public void onDelete(Product product) {
                deleteProduct(product);
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab_add_product);
        fab.setOnClickListener(v -> showAddEditDialog(null));

        loadProducts();
    }

    private void loadProducts() {
        db.collection("products").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    productList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Product p = document.toObject(Product.class);
                        p.setId(document.getId());
                        productList.add(p);
                    }
                    adapter.setProducts(productList);
                })
                .addOnFailureListener(
                        e -> Toast.makeText(StoreActivity.this, "Error loading products", Toast.LENGTH_SHORT).show());
    }

    private void showAddEditDialog(Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null);
        builder.setView(view);

        TextView title = view.findViewById(R.id.text_dialog_title);
        EditText nameInput = view.findViewById(R.id.input_product_name);
        EditText priceInput = view.findViewById(R.id.input_product_price);
        RadioGroup unitGroup = view.findViewById(R.id.group_unit_type);
        RadioButton kgRadio = view.findViewById(R.id.radio_kg);
        RadioButton literRadio = view.findViewById(R.id.radio_liter);

        // Initialize custom buttons
        android.widget.Button btnSave = view.findViewById(R.id.btn_save);
        android.widget.Button btnCancel = view.findViewById(R.id.btn_cancel);

        if (product != null) {
            title.setText("Edit Product");
            nameInput.setText(product.getName());
            priceInput.setText(String.valueOf(product.getPricePerUnit()));
            if ("L".equals(product.getUnitType())) {
                literRadio.setChecked(true);
            } else {
                kgRadio.setChecked(true);
            }
            btnSave.setText("Update Product");
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
        }

        btnSave.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String priceStr = priceInput.getText().toString().trim();
            String unit = kgRadio.isChecked() ? "KG" : "L";

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(priceStr)) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double price = 0;
            try {
                price = Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show();
                return;
            }

            if (product == null) {
                // Add
                Product newProduct = new Product(null, name, price, unit);
                saveProduct(newProduct);
            } else {
                // Update
                product.setName(name);
                product.setPricePerUnit(price);
                product.setUnitType(unit);
                updateProduct(product);
            }
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void saveProduct(Product product) {
        db.collection("products").add(product)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Product Added", Toast.LENGTH_SHORT).show();
                    loadProducts();
                })
                .addOnFailureListener(e -> Toast
                        .makeText(this, "Error adding product: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateProduct(Product product) {
        db.collection("products").document(product.getId()).set(product)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Product Updated", Toast.LENGTH_SHORT).show();
                    loadProducts();
                })
                .addOnFailureListener(e -> Toast
                        .makeText(this, "Error updating product: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void deleteProduct(Product product) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete " + product.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("products").document(product.getId()).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Product Deleted", Toast.LENGTH_SHORT).show();
                                loadProducts();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
