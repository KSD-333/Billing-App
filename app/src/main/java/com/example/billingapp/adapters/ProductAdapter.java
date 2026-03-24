package com.example.billingapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.billingapp.R;
import com.example.billingapp.models.Product;
import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Product> productList = new ArrayList<>();
    private OnProductActionListener listener;

    public interface OnProductActionListener {
        void onEdit(Product product);

        void onDelete(Product product);
    }

    public void setOnProductActionListener(OnProductActionListener listener) {
        this.listener = listener;
    }

    public void setProducts(List<Product> products) {
        this.productList = products;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product);

        // Animation
        android.view.animation.Animation animation = android.view.animation.AnimationUtils
                .loadAnimation(holder.itemView.getContext(), R.anim.item_animation_fall_down);
        holder.itemView.startAnimation(animation);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView priceText;
        ImageButton editBtn;
        ImageButton deleteBtn;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_product_name);
            priceText = itemView.findViewById(R.id.text_product_price);
            editBtn = itemView.findViewById(R.id.btn_edit);
            deleteBtn = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(Product product) {
            nameText.setText(product.getName());
            priceText.setText(String.format("₹%.2f per %s", product.getPricePerUnit(), product.getUnitType()));

            editBtn.setOnClickListener(v -> {
                if (listener != null)
                    listener.onEdit(product);
            });

            deleteBtn.setOnClickListener(v -> {
                if (listener != null)
                    listener.onDelete(product);
            });
        }
    }
}
