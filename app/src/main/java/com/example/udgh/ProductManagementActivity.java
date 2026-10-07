package com.example.udgh;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.udgh.models.Product;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProductManagementActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private RecyclerView rvProduct;
    private TextView txtTotalProduct;
    private SwipeRefreshLayout swipeRefreshLayout;
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private ProductAdapter productAdapter;
    private List<Product> productList;
    private List<Product> productListFull;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product_management);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        SharedPreferences sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        userId = sharedPref.getString("userId", "");
        if (TextUtils.isEmpty(userId) && mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        // Ánh xạ view
        toolbar = findViewById(R.id.toolbar);
        rvProduct = findViewById(R.id.rvProduct);
        txtTotalProduct = findViewById(R.id.txtTotalProduct);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        etSearch = findViewById(R.id.etSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Quản lý sản phẩm");
        toolbar.setNavigationOnClickListener(v -> finish());

        setupRecyclerView();
        setupSwipeRefresh();
        setupSearch();
        loadProductsFromFirestore();
    }

    // ================== SETUP RECYCLERVIEW ==================

    private void setupRecyclerView() {
        productList = new ArrayList<>();
        productListFull = new ArrayList<>();
        productAdapter = new ProductAdapter(productList);

        productAdapter.setOnProductDeleteListener(() -> {
            updateTotalCount();
            if (productList.isEmpty()) {
                Toast.makeText(this, "Không còn sản phẩm nào", Toast.LENGTH_SHORT).show();
            }
        });

        rvProduct.setLayoutManager(new LinearLayoutManager(this));
        rvProduct.setAdapter(productAdapter);
    }

    // ================== SETUP SWIPE REFRESH ==================

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadProductsFromFirestore();
        });
    }

    // ================== SETUP TÌM KIẾM ==================

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String keyword = s.toString();
                filterProducts(keyword);

                if (keyword.length() > 0) {
                    btnClearSearch.setVisibility(ImageButton.VISIBLE);
                } else {
                    btnClearSearch.setVisibility(ImageButton.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            btnClearSearch.setVisibility(ImageButton.GONE);
            etSearch.requestFocus();
        });
    }

    // ================== LOAD DỮ LIỆU TỪ FIRESTORE (LẤY TẤT CẢ SẢN PHẨM) ==================

    private void loadProductsFromFirestore() {
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "Không tìm thấy userId", Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        swipeRefreshLayout.setRefreshing(true);

        // ✅ Lấy TẤT CẢ sản phẩm (không lọc theo createdBy)
        db.collection("PRODUCTS")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    productList.clear();
                    productListFull.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String documentId = document.getId();
                        String productName = document.getString("productName");
                        String description = document.getString("description");
                        String imageUrl = document.getString("picture");
                        Double price = document.getDouble("price");
                        Double weight = document.getDouble("weight");
                        String createdBy = document.getString("createdBy");

                        Product product = new Product();
                        product.setDocumentId(documentId);
                        product.setProductId(documentId);
                        product.setProductName(productName);
                        product.setDescription(description);
                        product.setImageUrl(imageUrl);
                        product.setPrice(price != null ? price : 0);
                        product.setWeight(weight != null ? weight : 0);
                        product.setCreatedBy(createdBy);

                        productList.add(product);
                        productListFull.add(product);
                    }

                    productAdapter.notifyDataSetChanged();
                    updateTotalCount();

                    if (productList.isEmpty()) {
                        Toast.makeText(this, "Không có sản phẩm nào trong hệ thống", Toast.LENGTH_SHORT).show();
                    }

                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    // ================== LỌC SẢN PHẨM ==================

    private void filterProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            productList.clear();
            productList.addAll(productListFull);
        } else {
            String searchText = keyword.toLowerCase().trim();
            List<Product> filteredList = new ArrayList<>();

            for (Product product : productListFull) {
                if (product.getProductName().toLowerCase().contains(searchText) ||
                        product.getProductId().toLowerCase().contains(searchText)) {
                    filteredList.add(product);
                }
            }

            productList.clear();
            productList.addAll(filteredList);
        }

        productAdapter.notifyDataSetChanged();
        updateTotalCount();

        if (productList.isEmpty() && keyword != null && !keyword.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy sản phẩm nào", Toast.LENGTH_SHORT).show();
        }
    }

    // ================== CẬP NHẬT TỔNG SỐ ==================

    private void updateTotalCount() {
        String text = "Tổng số: " + productList.size() + " sản phẩm";
        if (productList.size() != productListFull.size() && !productListFull.isEmpty()) {
            text += " (Đã lọc)";
        }
        txtTotalProduct.setText(text);
    }

    // ================== XỬ LÝ MENU TOOLBAR ==================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.product_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            Intent intent = new Intent(ProductManagementActivity.this, AddEditProductActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProductsFromFirestore();
    }
}