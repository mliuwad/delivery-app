package com.example.udgh.adapters;

import com.example.udgh.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.udgh.models.Product;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductSelectionAdapter extends RecyclerView.Adapter<ProductSelectionAdapter.ViewHolder> {
    private final List<Product> productList = new ArrayList<>();
    private final OnProductClickListener listener;

    public interface OnProductClickListener { void onProductClick(Product product); }

    public ProductSelectionAdapter(List<Product> products, OnProductClickListener listener) {
        if (products != null) productList.addAll(products);
        this.listener = listener;
    }

    public void updateData(List<Product> products) {
        productList.clear();
        if (products != null) productList.addAll(products);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.tvProductName.setText(product.getProductName());
        holder.tvProductPrice.setText(NumberFormat.getCurrencyInstance(new Locale("vi", "VN"))
                .format((int) product.getPrice()));
        String imageUrl = product.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext()).load(imageUrl)
                    .placeholder(R.drawable.box).error(R.drawable.box)
                    .centerCrop().into(holder.imgProduct);
        } else {
            holder.imgProduct.setImageResource(R.drawable.box);
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProductClick(product);
        });
    }

    @Override public int getItemCount() { return productList.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgProduct;
        final TextView tvProductName, tvProductPrice;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
        }
    }
}
