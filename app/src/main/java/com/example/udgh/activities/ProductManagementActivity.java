
package com.example.udgh.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.udgh.adapters.ProductAdapter;
import com.example.udgh.R;
import com.example.udgh.models.Product;
import com.example.udgh.viewmodels.ProductViewModel;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class ProductManagementActivity extends AppCompatActivity
        implements ProductAdapter.OnProductActionListener {

    private MaterialToolbar toolbar;
    private RecyclerView rvProduct;
    private TextView txtTotalProduct;
    private SwipeRefreshLayout swipeRefreshLayout;
    private EditText etSearch;
    private ImageButton btnClearSearch;

    private ProductAdapter productAdapter;
    private ProductViewModel productViewModel;

    private String currentKeyword = "";
    private String currentUserId;
    private boolean isAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product_management);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );
                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );
                    return insets;
                }
        );

        productViewModel = new ViewModelProvider(this)
                .get(ProductViewModel.class);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();
        setupSearch();
        observeViewModel();

        loadCurrentUserAndProducts();
    }

    private void loadCurrentUserAndProducts() {
        currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) { finish(); return; }
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("USERS").document(currentUserId).get()
                .addOnSuccessListener(user -> {
                    String role = user.getString("role");
                    isAdmin = "ADMIN".equalsIgnoreCase(role);
                    if (!isAdmin && !"CREATOR".equalsIgnoreCase(role)) {
                        Toast.makeText(this, "Tài khoản không có quyền quản lý sản phẩm", Toast.LENGTH_SHORT).show(); finish(); return;
                    }
                    productAdapter.setAdminViewOnly(isAdmin);
                    invalidateOptionsMenu();
                    productViewModel.loadProducts(currentUserId, isAdmin);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Không thể xác định quyền tài khoản", Toast.LENGTH_SHORT).show());
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvProduct = findViewById(R.id.rvProduct);
        txtTotalProduct = findViewById(R.id.txtTotalProduct);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        etSearch = findViewById(R.id.etSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Quản lý sản phẩm");
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        productAdapter = new ProductAdapter(
                new ArrayList<>(),
                this
        );

        rvProduct.setLayoutManager(
                new LinearLayoutManager(this)
        );
        rvProduct.setAdapter(productAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );

        swipeRefreshLayout.setOnRefreshListener(
                () -> loadCurrentUserAndProducts()
        );
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {

                currentKeyword = s.toString();
                productViewModel.searchProducts(currentKeyword);

                btnClearSearch.setVisibility(
                        currentKeyword.isEmpty()
                                ? ImageButton.GONE
                                : ImageButton.VISIBLE
                );
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            etSearch.requestFocus();
        });
    }

    private void observeViewModel() {

        productViewModel.getProducts().observe(this, products -> {
            productAdapter.setProducts(products);
            updateTotalCount(products);
        });

        productViewModel.getIsLoading().observe(this, loading -> {
            swipeRefreshLayout.setRefreshing(
                    loading != null && loading
            );
        });

        productViewModel.getOperationResult().observe(this, result -> {
            if (result == null) {
                return;
            }

            Toast.makeText(
                    this,
                    result.getMessage(),
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private void updateTotalCount(List<Product> products) {
        int count = products == null ? 0 : products.size();

        String text = "Tổng số: " + count + " sản phẩm";

        if (!currentKeyword.trim().isEmpty()) {
            text += " (Đã lọc)";
        }

        txtTotalProduct.setText(text);
    }

    // ================== SỰ KIỆN TỪ ADAPTER ==================

    @Override
    public void onEditProduct(Product product) {
        if (isAdmin || product == null || currentUserId == null || !currentUserId.equals(product.getCreatedBy())) { Toast.makeText(this, "Bạn không có quyền sửa sản phẩm này", Toast.LENGTH_SHORT).show(); return; }
        openProductScreen(product, "edit");
    }

    @Override
    public void onViewProduct(Product product) {
        openProductScreen(product, "view");
    }

    @Override
    public void onDeactivateProduct(Product product) {
        if (isAdmin || product == null || currentUserId == null || !currentUserId.equals(product.getCreatedBy())) { Toast.makeText(this, "Bạn không có quyền ngừng kinh doanh sản phẩm này", Toast.LENGTH_SHORT).show(); return; }
        productViewModel.deactivateProduct(
                product.getProductId()
        );
    }

    private void openProductScreen(Product product, String mode) {
        Intent intent = new Intent(
                this,
                AddEditProductActivity.class
        );

        intent.putExtra("productId", product.getProductId());
        intent.putExtra("mode", mode);

        startActivity(intent);
    }
    // ================== MENU TOOLBAR ==================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(
                R.menu.product_toolbar_menu,
                menu
        );
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem addItem = menu.findItem(R.id.action_add);
        if (addItem != null) addItem.setVisible(!isAdmin);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            if (isAdmin) { Toast.makeText(this, "Admin không tạo sản phẩm thay cho shop", Toast.LENGTH_SHORT).show(); return true; }
            Intent intent = new Intent(
                    this,
                    AddEditProductActivity.class
            );

            intent.putExtra("mode", "add");
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

        if (productViewModel != null) {
            loadCurrentUserAndProducts();
        }
    }
}
