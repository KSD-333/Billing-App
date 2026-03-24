package com.example.billingapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.billingapp.R;
import com.example.billingapp.models.Bill;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<Bill> bills = new ArrayList<>();
    private OnBillDeleteListener deleteListener;

    public interface OnBillDeleteListener {
        void onDelete(Bill bill);
    }

    public interface OnBillClickListener {
        void onBillClick(Bill bill);
    }

    public interface OnBillPaidListener {
        void onMarkAsPaid(Bill bill);
    }

    private OnBillClickListener clickListener;
    private OnBillPaidListener paidListener;

    public void setOnBillDeleteListener(OnBillDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setOnBillClickListener(OnBillClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnBillPaidListener(OnBillPaidListener listener) {
        this.paidListener = listener;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        Bill bill = bills.get(position);
        holder.bind(bill);
    }

    @Override
    public int getItemCount() {
        return bills.size();
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, amountText, dateText, statusText;
        ImageButton deleteBtn;
        android.widget.Button markPaidBtn;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_history_customer);
            amountText = itemView.findViewById(R.id.text_history_amount);
            dateText = itemView.findViewById(R.id.text_history_date);
            statusText = itemView.findViewById(R.id.text_history_status);
            deleteBtn = itemView.findViewById(R.id.btn_delete_bill);
            markPaidBtn = itemView.findViewById(R.id.btn_mark_paid);
        }

        public void bind(Bill bill) {
            nameText.setText(bill.getCustomerName());
            amountText.setText(String.format("₹%.2f", bill.getFinalAmount()));

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            dateText.setText(sdf.format(new Date(bill.getTimestamp())));

            String statusStr = "Status: " + bill.getStatus();
            if (!"Pending".equalsIgnoreCase(bill.getStatus())) {
                statusStr += " | " + bill.getPaymentMethod();
            }
            if (bill.isUpdated()) {
                statusStr += " (Updated)";
            }
            statusText.setText(statusStr);

            if ("Pending".equalsIgnoreCase(bill.getStatus())) {
                markPaidBtn.setVisibility(View.VISIBLE);
                markPaidBtn.setOnClickListener(v -> {
                    if (paidListener != null) {
                        paidListener.onMarkAsPaid(bill);
                    }
                });
            } else {
                markPaidBtn.setVisibility(View.GONE);
            }

            deleteBtn.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(bill);
                }
            });

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onBillClick(bill);
                }
            });
        }
    }
}
