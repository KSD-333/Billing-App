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
import java.util.Map;

public class MonthlySalesAdapter extends RecyclerView.Adapter<MonthlySalesAdapter.ViewHolder> {

    private List<Map.Entry<String, Double>> salesList = new ArrayList<>();

    public void setSalesList(List<Map.Entry<String, Double>> salesList) {
        this.salesList = salesList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_monthly_sales, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map.Entry<String, Double> entry = salesList.get(position);
        holder.monthText.setText(entry.getKey());
        holder.amountText.setText(String.format("₹%.2f", entry.getValue()));
    }

    @Override
    public int getItemCount() {
        return salesList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView monthText, amountText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            monthText = itemView.findViewById(R.id.text_month_name);
            amountText = itemView.findViewById(R.id.text_month_amount);
        }
    }
}
