package com.example.billingapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.billingapp.R;
import java.util.ArrayList;
import java.util.List;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder> {
    // Adapter for Customer List

    private List<CustomerSummary> customers = new ArrayList<>();
    private OnCustomerClickListener listener;

    public interface OnCustomerClickListener {
        void onCustomerClick(CustomerSummary customer);
    }

    public CustomerAdapter(OnCustomerClickListener listener) {
        this.listener = listener;
    }

    public void setCustomers(List<CustomerSummary> customers) {
        this.customers = customers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CustomerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_customer, parent, false);
        return new CustomerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerViewHolder holder, int position) {
        CustomerSummary customer = customers.get(position);
        holder.bind(customer);
    }

    @Override
    public int getItemCount() {
        return customers.size();
    }

    class CustomerViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, phoneText, pendingText;

        public CustomerViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_customer_name);
            phoneText = itemView.findViewById(R.id.text_customer_phone);
            pendingText = itemView.findViewById(R.id.text_customer_pending);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onCustomerClick(customers.get(getAdapterPosition()));
                }
            });
        }

        public void bind(CustomerSummary customer) {
            nameText.setText(customer.name);
            phoneText.setText(customer.phoneNumber);
            pendingText.setText(String.format("Pending: ₹%.2f", customer.totalPending));

            if (customer.totalPending > 0) {
                pendingText.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
            } else {
                pendingText
                        .setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                pendingText.setText("No Pending");
            }
        }
    }

    public static class CustomerSummary {
        public String name;
        public String phoneNumber;
        public double totalPending;
        public double totalPaid;

        public CustomerSummary(String name, String phoneNumber, double totalPending, double totalPaid) {
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.totalPending = totalPending;
            this.totalPaid = totalPaid;
        }
    }
}
