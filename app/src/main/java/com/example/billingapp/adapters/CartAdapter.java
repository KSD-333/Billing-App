package com.example.billingapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.billingapp.R;
import com.example.billingapp.models.CartItem;
import java.util.ArrayList;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<CartItem> cartList = new ArrayList<>();
    private OnCartActionListener listener;

    public interface OnCartActionListener {
        void onRemove(CartItem item);

        void onEdit(CartItem item);
    }

    public void setOnCartActionListener(OnCartActionListener listener) {
        this.listener = listener;
    }

    public void setCartItems(List<CartItem> items) {
        this.cartList = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return cartList.size();
    }

    class CartViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, quantityText, totalText;
        ImageButton removeBtn;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_cart_product_name);
            quantityText = itemView.findViewById(R.id.text_cart_quantity);
            totalText = itemView.findViewById(R.id.text_cart_total_price);
            removeBtn = itemView.findViewById(R.id.btn_remove_cart);
        }

        public void bind(CartItem item) {
            nameText.setText(item.getName());
            quantityText.setText(
                    String.format("%.2f %s x ₹%.2f", item.getQuantity(), item.getUnitType(), item.getPricePerUnit()));
            totalText.setText(String.format("₹%.2f", item.getTotalPrice()));

            removeBtn.setOnClickListener(v -> {
                if (listener != null)
                    listener.onRemove(item);
            });

            itemView.setOnClickListener(v -> {
                if (listener != null)
                    listener.onEdit(item);
            });
        }
    }
}
