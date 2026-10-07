package com.example.udgh;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.udgh.models.OrderDetail;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class SelectedProductAdapter extends RecyclerView.Adapter<SelectedProductAdapter.ViewHolder> {

    private List<OrderDetail> productList;
    private OnProductRemoveListener removeListener;
    private boolean isViewMode; // Thêm biến để biết chế độ xem

    public interface OnProductRemoveListener {
        void onRemove(OrderDetail product);
    }

    // Constructor cũ (giữ tương thích, mặc định isViewMode = false)
    public SelectedProductAdapter(List<OrderDetail> productList, OnProductRemoveListener removeListener) {
        this(productList, removeListener, false);
    }

    // Constructor mới có tham số isViewMode
    public SelectedProductAdapter(List<OrderDetail> productList, OnProductRemoveListener removeListener, boolean isViewMode) {
        this.productList = productList;
        this.removeListener = removeListener;
        this.isViewMode = isViewMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderDetail product = productList.get(position);

        holder.tvProductName.setText(product.getProductName());
        holder.tvPrice.setText(formatCurrency((int) product.getPrice()));
        holder.tvQuantity.setText("x" + product.getQuantity());

        // Load ảnh
        String imageUrl = product.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.box)
                    .error(R.drawable.box)
                    .centerCrop()
                    .into(holder.imgProduct);
        } else {
            holder.imgProduct.setImageResource(R.drawable.box);
        }

        // ✅ Nếu là chế độ xem chi tiết => ẩn nút xóa
        if (isViewMode) {
            holder.btnRemove.setVisibility(View.GONE);
        } else {
            holder.btnRemove.setVisibility(View.VISIBLE);
            holder.btnRemove.setOnClickListener(v -> {
                if (removeListener != null) {
                    removeListener.onRemove(product);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    private String formatCurrency(int price) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return format.format(price);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView tvProductName, tvPrice, tvQuantity;
        ImageButton btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}