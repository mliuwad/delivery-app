package com.example.udgh;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.udgh.activities.AddEditProductActivity;
import com.example.udgh.adapters.ProductAdapter;
import com.example.udgh.models.Product;
import com.example.udgh.viewmodels.ProductViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class CreatorProductFragment extends Fragment implements ProductAdapter.OnProductActionListener {
    private ProductViewModel viewModel;
    private ProductAdapter adapter;
    private String userId;
    private EditText etSearch;
    private ImageButton btnClear;
    private TextView txtCount;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_creator_products, container, false);
        userId = FirebaseAuth.getInstance().getUid();
        viewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        RecyclerView rv = view.findViewById(R.id.rvCreatorProducts);
        etSearch = view.findViewById(R.id.etSearchCreatorProducts);
        btnClear = view.findViewById(R.id.btnClearCreatorSearch);
        txtCount = view.findViewById(R.id.txtCreatorProductCount);
        swipe = view.findViewById(R.id.swipeCreatorProducts);
        adapter = new ProductAdapter(new ArrayList<>(), this);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        view.findViewById(R.id.btnAddCreatorProduct).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddEditProductActivity.class);
            intent.putExtra("mode", "add");
            startActivity(intent);
        });
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.searchProducts(s.toString());
                btnClear.setVisibility(s.length() == 0 ? View.GONE : View.VISIBLE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        btnClear.setOnClickListener(v -> etSearch.setText(""));
        swipe.setOnRefreshListener(this::loadProducts);

        viewModel.getProducts().observe(getViewLifecycleOwner(), products -> {
            adapter.setProducts(products);
            txtCount.setText("Tổng số: " + (products == null ? 0 : products.size()) + " sản phẩm");
        });
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> swipe.setRefreshing(loading != null && loading));
        viewModel.getOperationResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && isAdded()) Toast.makeText(requireContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
        });
        loadProducts();
        return view;
    }

    private void loadProducts() {
        if (userId != null) viewModel.loadProducts(userId, false);
    }

    @Override public void onEditProduct(Product product) {
        if (product == null || !userId.equals(product.getCreatedBy())) return;
        Intent intent = new Intent(requireContext(), AddEditProductActivity.class);
        intent.putExtra("productId", product.getProductId());
        intent.putExtra("mode", "edit");
        startActivity(intent);
    }
    @Override public void onViewProduct(Product product) {
        Intent intent = new Intent(requireContext(), AddEditProductActivity.class);
        intent.putExtra("productId", product.getProductId());
        intent.putExtra("mode", "view");
        startActivity(intent);
    }
    @Override public void onDeactivateProduct(Product product) {
        if (product != null && userId.equals(product.getCreatedBy())) viewModel.deactivateProduct(product.getProductId());
    }
    @Override public void onResume() { super.onResume(); if (viewModel != null) loadProducts(); }
}