package com.example.udgh.adapters;

import com.example.udgh.activities.*;
import com.example.udgh.fragments.*;
import com.example.udgh.utils.*;
import com.example.udgh.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.udgh.R;
import com.example.udgh.models.Order;
import com.example.udgh.models.OrderDetail;
import com.google.android.material.chip.Chip;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderAdminAdapter
        extends RecyclerView.Adapter<OrderAdminAdapter.OrderViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    private final List<Order> orderList = new ArrayList<>();
    private final OnOrderClickListener listener;

    public OrderAdminAdapter(OnOrderClickListener listener) {
        this.listener = listener;
    }

    public void setOrders(List<Order> orders) {
        orderList.clear();

        if (orders != null) {
            orderList.addAll(orders);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);

        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull OrderViewHolder holder,
            int position
    ) {
        Order order = orderList.get(position);

        // Mã đơn hàng
        String orderId = order.getOrderId();

        if (orderId != null && orderId.startsWith("OD")) {
            holder.tvOrderId.setText("DH" + orderId.substring(2));
        } else {
            holder.tvOrderId.setText(
                    orderId != null ? orderId : "DH---"
            );
        }

        // Ngày tạo
        if (order.getCreatedAt() != null) {
            SimpleDateFormat dateFormat =
                    new SimpleDateFormat(
                            "dd/MM/yyyy HH:mm",
                            Locale.getDefault()
                    );

            holder.tvCreatedAt.setText(
                    dateFormat.format(order.getCreatedAt().toDate())
            );
        } else {
            holder.tvCreatedAt.setText("");
        }

        // Người nhận
        holder.tvReceiver.setText(
                order.getReceiverName() != null
                        ? order.getReceiverName()
                        : ""
        );

        // Danh sách sản phẩm
        holder.llProductList.removeAllViews();

        List<OrderDetail> details = order.getOrderDetails();

        if (details != null && !details.isEmpty()) {
            for (OrderDetail detail : details) {
                TextView tvProduct = new TextView(holder.itemView.getContext());

                tvProduct.setLayoutParams(
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                );

                tvProduct.setTextSize(14);
                tvProduct.setTextColor(
                        holder.itemView.getContext().getColor(R.color.black)
                );

                String productName = detail.getProductName() != null
                        ? detail.getProductName()
                        : "Sản phẩm";

                tvProduct.setText(
                        "• " + productName + " x" + detail.getQuantity()
                );

                holder.llProductList.addView(tvProduct);
            }
        } else {
            TextView tvProduct = new TextView(holder.itemView.getContext());

            tvProduct.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );

            tvProduct.setTextSize(14);
            tvProduct.setTextColor(
                    holder.itemView.getContext().getColor(R.color.gray)
            );
            tvProduct.setText("Không có sản phẩm");

            holder.llProductList.addView(tvProduct);
        }

        // Tiền hàng
        holder.tvProductPrice.setText(
                formatCurrency(order.getTotalProductValue())
        );

        // Phí giao hàng
        holder.tvShippingFee.setText(
                formatCurrency(order.getDeliveryFee())
        );

        // Trạng thái
        bindStatus(holder, order.getStatus());

        // Chuyển sự kiện click về Activity
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOrderClick(order);
            }
        });
    }

    private void bindStatus(OrderViewHolder holder, String status) {
        if (status == null) {
            holder.chipStatus.setText("Không xác định");
            holder.chipStatus.setChipBackgroundColorResource(R.color.gray);
            return;
        }

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
    }

    private String formatCurrency(double amount) {
        NumberFormat format =
                NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        return format.format(amount);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {

        TextView tvOrderId;
        TextView tvCreatedAt;
        TextView tvReceiver;
        TextView tvProductPrice;
        TextView tvShippingFee;

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