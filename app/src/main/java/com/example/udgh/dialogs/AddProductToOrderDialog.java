package com.example.udgh.dialogs;

import com.example.udgh.adapters.ProductSelectionAdapter;
import com.example.udgh.R;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.udgh.models.OrderDetail;
import com.example.udgh.models.Product;
import com.example.udgh.viewmodels.ProductSelectionViewModel;
import com.google.android.material.button.MaterialButton;

public class AddProductToOrderDialog {
    private final Context context;
    private final OnProductSelectedListener listener;
    private ProductSelectionViewModel viewModel;
    private ProductSelectionAdapter adapter;
    private AlertDialog dialog;
    private Observer<java.util.List<Product>> productsObserver;
    private Observer<String> errorObserver;

    public interface OnProductSelectedListener {
        void onProductSelected(OrderDetail orderDetail);
    }

    public AddProductToOrderDialog(Context context, OnProductSelectedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void show() {
        if (!(context instanceof ViewModelStoreOwner) || !(context instanceof LifecycleOwner)) {
            Toast.makeText(context, "Không thể khởi tạo màn hình chọn sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_product_to_order, null);
        RecyclerView rvProducts = view.findViewById(R.id.rvProducts);
        EditText etSearch = view.findViewById(R.id.etSearch);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);

        adapter = new ProductSelectionAdapter(new java.util.ArrayList<>(), this::showQuantityDialog);
        rvProducts.setLayoutManager(new LinearLayoutManager(context));
        rvProducts.setAdapter(adapter);

        dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setTitle("Chọn sản phẩm")
                .setCancelable(true)
                .create();

        viewModel = new ViewModelProvider((ViewModelStoreOwner) context)
                .get(ProductSelectionViewModel.class);

        productsObserver = products -> adapter.updateData(products);
        errorObserver = message -> {
            if (message != null && !message.isEmpty())
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        };
        viewModel.getProducts().observe((LifecycleOwner) context, productsObserver);
        viewModel.getError().observe((LifecycleOwner) context, errorObserver);
        viewModel.loadProducts();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.search(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnDismissListener(unused -> removeObservers());
        dialog.show();
    }

    private void removeObservers() {
        if (viewModel == null || !(context instanceof LifecycleOwner)) return;
        if (productsObserver != null) viewModel.getProducts().removeObserver(productsObserver);
        if (errorObserver != null) viewModel.getError().removeObserver(errorObserver);
    }

    private void showQuantityDialog(Product product) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_quantity_input, null);
        EditText edtQuantity = view.findViewById(R.id.edtQuantity);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirm);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);

        AlertDialog quantityDialog = new AlertDialog.Builder(context)
                .setView(view)
                .setTitle("Nhập số lượng")
                .setCancelable(false)
                .create();

        btnConfirm.setOnClickListener(v -> {
            String value = edtQuantity.getText() == null ? "" : edtQuantity.getText().toString().trim();
            if (value.isEmpty()) {
                edtQuantity.setError("Vui lòng nhập số lượng");
                return;
            }

            int quantity;
            try {
                quantity = Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                edtQuantity.setError("Số lượng không hợp lệ");
                return;
            }
            if (quantity <= 0) {
                edtQuantity.setError("Số lượng phải lớn hơn 0");
                return;
            }

            OrderDetail detail = new OrderDetail(null, null, product.getProductId(),
                    product.getProductName(),
                    product.getDescription() == null ? "" : product.getDescription(),
                    product.getWeight(), product.getPrice(), quantity);
            detail.setImageUrl(product.getImageUrl());

            if (listener != null) listener.onProductSelected(detail);
            quantityDialog.dismiss();
            if (dialog != null) dialog.dismiss();
        });
        btnCancel.setOnClickListener(v -> quantityDialog.dismiss());
        quantityDialog.show();
    }
}
