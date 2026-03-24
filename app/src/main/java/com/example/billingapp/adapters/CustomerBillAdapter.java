package com.example.billingapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class CustomerBillAdapter extends RecyclerView.Adapter<CustomerBillAdapter.BillViewHolder> {

    private List<Bill> bills = new ArrayList<>();
    private OnShareClickListener listener;
    private OnBillPaidListener paidListener;

    public interface OnShareClickListener {
        void onShareClick(Bill bill);
    }

    public interface OnBillPaidListener {
        void onMarkAsPaid(Bill bill);
    }

    public CustomerBillAdapter(OnShareClickListener listener, OnBillPaidListener paidListener) {
        this.listener = listener;
        this.paidListener = paidListener;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_customer_bill, parent, false);
        return new BillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BillViewHolder holder, int position) {
        Bill bill = bills.get(position);
        holder.bind(bill);
    }

    @Override
    public int getItemCount() {
        return bills.size();
    }

    class BillViewHolder extends RecyclerView.ViewHolder {
        TextView dateText, statusText, amountText;
        Button shareBtn, markPaidBtn;

        public BillViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.text_bill_date);
            statusText = itemView.findViewById(R.id.text_bill_status);
            amountText = itemView.findViewById(R.id.text_bill_amount);
            shareBtn = itemView.findViewById(R.id.btn_share_whatsapp);
            markPaidBtn = itemView.findViewById(R.id.btn_mark_paid_customer);
        }

        public void bind(Bill bill) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            dateText.setText(sdf.format(new Date(bill.getTimestamp())));

            amountText.setText(String.format("₹%.2f", bill.getFinalAmount()));
            statusText.setText(bill.getStatus());

            if ("Pending".equalsIgnoreCase(bill.getStatus())) {
                statusText.setBackgroundResource(R.drawable.bg_status_pending);
                markPaidBtn.setVisibility(View.VISIBLE);
                markPaidBtn.setOnClickListener(v -> {
                    if (paidListener != null)
                        paidListener.onMarkAsPaid(bill);
                });
            } else {
                statusText.setBackgroundResource(R.drawable.bg_status_paid);
                markPaidBtn.setVisibility(View.GONE);
            }

            shareBtn.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onShareClick(bill);
                }
            });
        }
    }
}
