package com.example.udgh.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.udgh.R;
import com.example.udgh.models.Order;
import com.example.udgh.models.OrderDetail;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class SalaryOrderAdapter extends RecyclerView.Adapter<SalaryOrderAdapter.OrderViewHolder> {
    public interface OnOrderClickListener { void onOrderClick(Order order); }
    private final List<Order> orders;
    private final OnOrderClickListener listener;
    public SalaryOrderAdapter(List<Order> orders, OnOrderClickListener listener) {
        this.orders = orders;
        this.listener = listener;
    }
    @NonNull @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_salary_order, parent, false);
        return new OrderViewHolder(view);
    }
    @Override public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        String id = order.getOrderId();
        holder.txtOrderId.setText(id != null && id.startsWith("OD") ? "DH" + id.substring(2) : (id == null ? "DH---" : id));
        holder.txtShipFee.setText("+ " + formatCurrency(order.getDeliveryFee()));
        List<OrderDetail> details = order.getOrderDetails();
        if (details != null && !details.isEmpty()) {
            String name = details.get(0).getProductName();
            if (details.size() > 1) name += " (+" + (details.size() - 1) + " sản phẩm khác)";
            holder.txtProductName.setText(name);
        } else holder.txtProductName.setText("Chưa có thông tin sản phẩm");
        if (order.getDeliveredAt() != null) {
            holder.txtCompletedDate.setText("Hoàn thành: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(order.getDeliveredAt().toDate()));
        } else holder.txtCompletedDate.setText("Hoàn thành: --");
        holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onOrderClick(order); });
    }
    private String formatCurrency(double amount) {
        return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(amount);
    }
    @Override public int getItemCount() { return orders == null ? 0 : orders.size(); }
    static class OrderViewHolder extends RecyclerView.ViewHolder {
        final TextView txtOrderId, txtShipFee, txtProductName, txtCompletedDate;
        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            txtOrderId = itemView.findViewById(R.id.txtOrderId);
            txtShipFee = itemView.findViewById(R.id.txtShipFee);
            txtProductName = itemView.findViewById(R.id.txtProductName);
            txtCompletedDate = itemView.findViewById(R.id.txtCompletedDate);
        }
    }
}
