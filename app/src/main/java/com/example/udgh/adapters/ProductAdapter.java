
package com.example.udgh.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.udgh.R;
import com.example.udgh.models.Product;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter
        extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final List<Product> productList = new ArrayList<>();
    private final OnProductActionListener listener;
    private boolean adminViewOnly = false;

    public interface OnProductActionListener {
        void onEditProduct(Product product);
        void onViewProduct(Product product);
        void onDeactivateProduct(Product product);
    }

    public ProductAdapter(
            List<Product> products,
            OnProductActionListener listener) {

        if (products != null) {
            productList.addAll(products);
        }

        this.listener = listener;
    }

    public void setAdminViewOnly(boolean adminViewOnly) {
        this.adminViewOnly = adminViewOnly;
        notifyDataSetChanged();
    }

    public void setProducts(List<Product> products) {
        productList.clear();

        if (products != null) {
            productList.addAll(products);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);

        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ProductViewHolder holder,
            int position) {

        Product product = productList.get(position);

        holder.txtProductId.setText(product.getProductId());
        holder.txtProductName.setText(product.getProductName());

        holder.txtWeight.setText(
                "Khối lượng: " + product.getWeight() + " kg"
        );

        holder.txtPrice.setText(
                formatCurrency(product.getPrice())
        );

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

        holder.btnEdit.setVisibility(adminViewOnly ? View.GONE : View.VISIBLE);
        holder.btnDelete.setVisibility(adminViewOnly ? View.GONE : View.VISIBLE);

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditProduct(product);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            showDeactivateDialog(holder, product);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewProduct(product);
            }
        });
    }

    private void showDeactivateDialog(
            ProductViewHolder holder,
            Product product) {

        new MaterialAlertDialogBuilder(holder.itemView.getContext())
                .setTitle("Ngừng kinh doanh sản phẩm")
                .setMessage(
                        "Bạn có chắc chắn muốn ngừng kinh doanh sản phẩm \""
                                + product.getProductName() + "\"?"
                )
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    if (listener != null) {
                        listener.onDeactivateProduct(product);
                    }
                })
                .setNegativeButton("Hủy", (dialog, which) ->
                        dialog.dismiss()
                )
                .show();
    }

    private String formatCurrency(double price) {
        NumberFormat format =
                NumberFormat.getCurrencyInstance(
                        new Locale("vi", "VN")
                );

        return format.format(price);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder
            extends RecyclerView.ViewHolder {

        ImageView imgProduct;
        TextView txtProductId;
        TextView txtProductName;
        TextView txtWeight;
        TextView txtPrice;
        ImageButton btnEdit;
        ImageButton btnDelete;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);

            imgProduct = itemView.findViewById(R.id.imgProduct);
            txtProductId = itemView.findViewById(R.id.txtProductId);
            txtProductName = itemView.findViewById(R.id.txtProductName);
            txtWeight = itemView.findViewById(R.id.txtWeight);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
