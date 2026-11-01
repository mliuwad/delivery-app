package com.example.udgh.adapters;

import com.example.udgh.R;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.udgh.models.Order;
import com.example.udgh.models.OrderDetail;
import com.google.android.material.chip.Chip;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Order> orderList;
    private final Class<?> detailActivity;

    public OrderAdapter(List<Order> orderList, Class<?> detailActivity) {
        this.orderList = orderList;
        this.detailActivity = detailActivity;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);

        // Mã đơn
        String orderId = order.getOrderId();
        if (orderId != null && orderId.startsWith("OD")) {
            holder.tvOrderId.setText("DH" + orderId.substring(2));
        } else {
            holder.tvOrderId.setText(orderId != null ? orderId : "DH---");
        }

        // Ngày tạo
        if (order.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.tvCreatedAt.setText(sdf.format(order.getCreatedAt().toDate()));
        } else {
            holder.tvCreatedAt.setText("");
        }

        // Người nhận
        holder.tvReceiver.setText(order.getReceiverName() != null ? order.getReceiverName() : "");

        // === HIỂN THỊ TẤT CẢ SẢN PHẨM KÈM SỐ LƯỢNG ===
        holder.llProductList.removeAllViews();

        List<OrderDetail> details = order.getOrderDetails();
        if (details != null && !details.isEmpty()) {
            for (OrderDetail detail : details) {
                TextView tvProduct = new TextView(holder.itemView.getContext());
                tvProduct.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));
                tvProduct.setTextSize(14);
                tvProduct.setTextColor(holder.itemView.getContext().getColor(R.color.black));
                tvProduct.setText("• " + detail.getProductName() + " x" + detail.getQuantity());
                holder.llProductList.addView(tvProduct);
            }
        } else {
            TextView tvProduct = new TextView(holder.itemView.getContext());
            tvProduct.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            tvProduct.setTextSize(14);
            tvProduct.setTextColor(holder.itemView.getContext().getColor(R.color.gray));
            tvProduct.setText("Không có sản phẩm");
            holder.llProductList.addView(tvProduct);
        }

        // Tiền hàng
        holder.tvProductPrice.setText(formatCurrency((int) order.getTotalProductValue()));

        // Phí ship
        holder.tvShippingFee.setText(formatCurrency((int) order.getDeliveryFee()));

        // Trạng thái
        String status = order.getStatus();
        if (status != null) {
            switch (status) {
                case "PENDING":
                case "WAITING":
                    holder.chipStatus.setText("Chờ nhận");
                    holder.chipStatus.setChipBackgroundColorResource(R.color.warning);
                    break;
                case "DELIVERING":
                    holder.chipStatus.setText("Đang giao");
                    holder.chipStatus.setChipBackgroundColorResource(R.color.secondary);
                    break;
                case "COMPLETED":
                    holder.chipStatus.setText("Hoàn thành");
                    holder.chipStatus.setChipBackgroundColorResource(R.color.success);
                    break;
                case "CANCELLED":
                    holder.chipStatus.setText("Đã hủy");
                    holder.chipStatus.setChipBackgroundColorResource(R.color.error);
                    break;
                default:
                    holder.chipStatus.setText(status);
                    holder.chipStatus.setChipBackgroundColorResource(R.color.gray);
                    break;
            }
        } else {
            holder.chipStatus.setText("Không xác định");
            holder.chipStatus.setChipBackgroundColorResource(R.color.gray);
        }

        // ================== CLICK ITEM -> MỞ CHI TIẾT ĐƠN HÀNG ==================
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, detailActivity);
            intent.putExtra("orderId", order.getOrderId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    private String formatCurrency(int price) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return format.format(price);
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvCreatedAt, tvReceiver, tvProductPrice, tvShippingFee;
        LinearLayout llProductList;
        Chip chipStatus;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.txtOrderId);
            tvCreatedAt = itemView.findViewById(R.id.txtCreatedAt);
            tvReceiver = itemView.findViewById(R.id.txtReceiver);
            llProductList = itemView.findViewById(R.id.llProductList);
            tvProductPrice = itemView.findViewById(R.id.txtProductPrice);
            tvShippingFee = itemView.findViewById(R.id.txtShippingFee);
            chipStatus = itemView.findViewById(R.id.chipStatus);
        }
    }
}