package com.example.udgh;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.udgh.R;
import com.example.udgh.ProductSelectionAdapter;
import com.example.udgh.models.OrderDetail;
import com.example.udgh.models.Product;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AddProductToOrderDialog {

    private Context context;
    private FirebaseFirestore db;
    private List<Product> productList;
    private ProductSelectionAdapter adapter;
    private OnProductSelectedListener listener;
    private AlertDialog dialog;

    public interface OnProductSelectedListener {
        void onProductSelected(OrderDetail orderDetail);
    }

    public AddProductToOrderDialog(Context context, OnProductSelectedListener listener) {
        this.context = context;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
        this.productList = new ArrayList<>();
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_product_to_order, null);

        RecyclerView rvProducts = view.findViewById(R.id.rvProducts);
        EditText etSearch = view.findViewById(R.id.etSearch);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);

        adapter = new ProductSelectionAdapter(productList, product -> {
            showQuantityDialog(product);
        });
        rvProducts.setLayoutManager(new LinearLayoutManager(context));
        rvProducts.setAdapter(adapter);

        loadProducts();

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        dialog = builder.setView(view)
                .setTitle("Chọn sản phẩm")
                .setCancelable(true)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void loadProducts() {
        db.collection("PRODUCTS")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    productList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Product product = new Product();
                        product.setProductId(doc.getId());
                        product.setProductName(doc.getString("productName"));
                        product.setPrice(doc.getDouble("price") != null ? doc.getDouble("price") : 0);
                        product.setImageUrl(doc.getString("imageUrl"));
                        product.setDescription(doc.getString("description"));
                        productList.add(product);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Lỗi tải sản phẩm", Toast.LENGTH_SHORT).show();
                });
    }

    private void filterProducts(String keyword) {
        // TODO: Lọc sản phẩm theo từ khóa
    }

    private void showQuantityDialog(Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_quantity_input, null);

        EditText edtQuantity = view.findViewById(R.id.edtQuantity);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirm);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);

        AlertDialog quantityDialog = builder.setView(view)
                .setTitle("Nhập số lượng")
                .setCancelable(false)
                .create();

        btnConfirm.setOnClickListener(v -> {
            String quantityStr = edtQuantity.getText().toString();
            if (quantityStr.isEmpty()) {
                Toast.makeText(context, "Vui lòng nhập số lượng", Toast.LENGTH_SHORT).show();
                return;
            }

            int quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                Toast.makeText(context, "Số lượng phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Tạo OrderDetail trực tiếp (không cần OrderProduct)
            OrderDetail orderDetail = new OrderDetail(
                    null,                           // detailId (tự tạo)
                    null,                           // orderId (sẽ set sau)
                    product.getProductId(),
                    product.getProductName(),
                    product.getDescription() != null ? product.getDescription() : "",
                    product.getWeight(),                              // weight (sẽ lấy sau)
                    product.getPrice(),
                    quantity
            );
            orderDetail.setImageUrl(product.getImageUrl()); // Thêm field này nếu cần

            if (listener != null) {
                listener.onProductSelected(orderDetail);
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
            quantityDialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> quantityDialog.dismiss());

        quantityDialog.show();
    }
}