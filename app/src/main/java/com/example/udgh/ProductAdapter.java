package com.example.udgh;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.udgh.models.Product;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Product> productList;
    private Context context;
    private OnProductDeleteListener deleteListener;

    // ✅ Interface để thông báo khi xóa thành công
    public interface OnProductDeleteListener {
        void onProductDeleted();
    }

    public ProductAdapter(List<Product> productList) {
        this.productList = productList;
    }

    // ✅ Set listener
    public void setOnProductDeleteListener(OnProductDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        // ✅ Mã SP + Tên sản phẩm chung 1 hàng
        holder.txtProductId.setText(product.getProductId());
        holder.txtProductName.setText(product.getProductName());
        holder.txtWeight.setText("Khối lượng: " + product.getWeight() + " kg");
        holder.txtPrice.setText(formatCurrency((int) product.getPrice()));

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

        // ✅ Xử lý nút Sửa - Mở trang ở chế độ SỬA
        holder.btnEdit.setOnClickListener(v -> {
            product.setDocumentId(product.getProductId());

            Intent intent = new Intent(context, AddEditProductActivity.class);
            intent.putExtra("product", product);
            intent.putExtra("mode", "edit");  // ✅ Thêm flag chế độ sửa
            context.startActivity(intent);
        });

        // ✅ Xử lý nút Xóa
        holder.btnDelete.setOnClickListener(v -> {
            showDeleteConfirmDialog(product, position);
        });

        // ✅ Click vào item - Mở trang ở chế độ XEM CHI TIẾT
        holder.itemView.setOnClickListener(v -> {
            product.setDocumentId(product.getProductId());

            Intent intent = new Intent(context, AddEditProductActivity.class);
            intent.putExtra("product", product);
            intent.putExtra("mode", "view");  // ✅ Thêm flag chế độ xem
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    // ================== HIỂN THỊ DIALOG XÓA ==================

    private void showDeleteConfirmDialog(Product product, int position) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Xóa sản phẩm")
                .setMessage("Bạn có chắc chắn muốn xóa sản phẩm \"" + product.getProductName() + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteProduct(product, position);
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    // ================== XÓA SẢN PHẨM TRONG FIRESTORE ==================

    private void deleteProduct(Product product, int position) {
        String documentId = product.getDocumentId();

        FirebaseFirestore.getInstance()
                .collection("PRODUCTS")
                .document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    productList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, productList.size());

                    Toast.makeText(context,
                            "Đã xóa sản phẩm: " + product.getProductName(),
                            Toast.LENGTH_SHORT).show();

                    if (deleteListener != null) {
                        deleteListener.onProductDeleted();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context,
                            "Lỗi xóa sản phẩm: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // ================== FORMAT TIỀN TỆ ==================

    private String formatCurrency(int price) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return format.format(price);
    }

    // ================== VIEW HOLDER ==================

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView txtProductId, txtProductName, txtWeight, txtPrice;
        ImageButton btnEdit, btnDelete;

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