package com.example.udgh;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.udgh.models.Product;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ProductHomeAdapter extends RecyclerView.Adapter<ProductHomeAdapter.ViewHolder> {

    private static final String TAG = "ProductHomeAdapter";
    private List<Product> productList;

    public ProductHomeAdapter(List<Product> productList) {
        this.productList = productList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_horizontal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.tvProductName.setText(product.getProductName() != null ? product.getProductName() : "");
        holder.tvProductPrice.setText(formatCurrency((int) product.getPrice()));

        String imageUrl = product.getImageUrl();
        Log.d(TAG, "Product " + position + " imageUrl: " + imageUrl); // ✅ Log URL

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)  // ✅ Cache ảnh
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.box)
                            .error(R.drawable.box)
                            .centerCrop()
                            .timeout(10000)) // 10s timeout
                    .into(holder.imgProduct);
        } else {
            holder.imgProduct.setImageResource(R.drawable.box);
        }

        holder.itemView.setOnClickListener(v -> {
            // TODO: Mở ProductDetailActivity
        });
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    private String formatCurrency(int price) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return format.format(price);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView tvProductName, tvProductPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
        }
    }
}