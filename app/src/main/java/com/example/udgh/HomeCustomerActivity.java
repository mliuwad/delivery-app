package com.example.udgh;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
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
import com.example.udgh.models.Order;
import com.example.udgh.models.OrderDetail;
import com.example.udgh.models.Product;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeCustomerActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView txtUserName;
    private MaterialButton btnCreateOrder;
    private TextView txtWaiting, txtDelivering, txtCompleted;
    private TextView txtViewAllOrders, txtViewAllProducts;
    private RecyclerView rvOrders, rvProducts;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    private OrderAdapter orderAdapter;
    private ProductHorizontalAdapter productAdapter;
    private List<Order> orderList;
    private List<Product> productList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_customer);

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
        txtUserName = findViewById(R.id.txtUserName);
        btnCreateOrder = findViewById(R.id.btnCreateOrder);
        txtWaiting = findViewById(R.id.txtWaiting);
        txtDelivering = findViewById(R.id.txtDelivering);
        txtCompleted = findViewById(R.id.txtCompleted);
        txtViewAllOrders = findViewById(R.id.txtViewAllOrders);
        txtViewAllProducts = findViewById(R.id.txtViewAllProducts);
        rvOrders = findViewById(R.id.rvOrders);
        rvProducts = findViewById(R.id.rvProducts);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Setup Toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Trang chủ");

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(this::refreshAllData);

        // Setup RecyclerView Orders
        orderList = new ArrayList<>();
        orderAdapter = new OrderAdapter(orderList);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(orderAdapter);

        // Setup RecyclerView Products (ngang)
        productList = new ArrayList<>();
        productAdapter = new ProductHorizontalAdapter(productList);
        rvProducts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvProducts.setAdapter(productAdapter);

        // Sự kiện
        btnCreateOrder.setOnClickListener(v ->
                startActivity(new Intent(HomeCustomerActivity.this, CreateOrderActivity.class)));

        txtViewAllOrders.setOnClickListener(v ->
                startActivity(new Intent(HomeCustomerActivity.this, OrderDetailCreatorActivity.class)));

        txtViewAllProducts.setOnClickListener(v ->
                startActivity(new Intent(HomeCustomerActivity.this, ProductManagementActivity.class)));

        // Load dữ liệu lần đầu
        loadAllData();
    }

    private void loadAllData() {
        loadUserInfo();
        loadStatistics();
        loadRecentOrders();
        loadProducts();
    }

    private void refreshAllData() {
        loadAllData();
        swipeRefreshLayout.setRefreshing(false);
        Toast.makeText(this, "Đã làm mới", Toast.LENGTH_SHORT).show();
    }

    // ================== LẤY TÊN NGƯỜI DÙNG ==================

    private void loadUserInfo() {

        if (TextUtils.isEmpty(userId)) {
            txtUserName.setText("Người tạo đơn");
            return;
        }

        db.collection("USERS")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("fullName");
                        txtUserName.setText(TextUtils.isEmpty(name) ? "Người tạo đơn" : name);
                    }
                })
                .addOnFailureListener(e -> txtUserName.setText("Người tạo đơn"));
    }

    // ================== THỐNG KÊ ĐƠN HÀNG ==================

    private void loadStatistics() {
        if (TextUtils.isEmpty(userId)) return;

        db.collection("ORDERS")
                .whereEqualTo("creatorId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int waiting = 0, delivering = 0, completed = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String status = doc.getString("status");
                        if (status != null) {
                            switch (status) {
                                case "PENDING":
                                case "WAITING":
                                    waiting++;
                                    break;
                                case "DELIVERING":
                                    delivering++;
                                    break;
                                case "COMPLETED":
                                    completed++;
                                    break;
                            }
                        }
                    }
                    txtWaiting.setText(String.valueOf(waiting));
                    txtDelivering.setText(String.valueOf(delivering));
                    txtCompleted.setText(String.valueOf(completed));
                })
                .addOnFailureListener(e -> {
                    txtWaiting.setText("0");
                    txtDelivering.setText("0");
                    txtCompleted.setText("0");
                });
    }

    // ================== LẤY 3 ĐƠN HÀNG GẦN NHẤT (CÓ TẤT CẢ SẢN PHẨM) ==================

    private void loadRecentOrders() {
        if (TextUtils.isEmpty(userId)) return;

        db.collection("ORDERS")
                .whereEqualTo("creatorId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    orderList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        order.setOrderId(doc.getId());

                        // Lấy tất cả OrderDetail từ sub-collection
                        db.collection("ORDERS")
                                .document(doc.getId())
                                .collection("ORDER_DETAILS")
                                .get()
                                .addOnSuccessListener(detailSnapshots -> {
                                    List<OrderDetail> details = new ArrayList<>();
                                    for (QueryDocumentSnapshot detailDoc : detailSnapshots) {
                                        OrderDetail detail = detailDoc.toObject(OrderDetail.class);
                                        details.add(detail);
                                    }
                                    order.setOrderDetails(details);
                                    orderAdapter.notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> {
                                    order.setOrderDetails(new ArrayList<>());
                                    orderAdapter.notifyDataSetChanged();
                                });

                        orderList.add(order);
                    }

                    orderAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show());

    }

    // ================== LẤY SẢN PHẨM CỦA USER ==================

    // ================== LẤY TẤT CẢ SẢN PHẨM (KHÔNG LỌC) ==================

    private void loadProducts() {
        if (TextUtils.isEmpty(userId)) return;

        // ✅ Lấy tất cả sản phẩm (không lọc theo createdBy)
        db.collection("PRODUCTS")
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    productList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Product product = new Product();
                        product.setProductId(doc.getId());
                        product.setProductName(doc.getString("productName"));
                        product.setDescription(doc.getString("description"));
                        product.setWeight(doc.getDouble("weight") != null ? doc.getDouble("weight") : 0);
                        product.setPrice(doc.getDouble("price") != null ? doc.getDouble("price") : 0);
                        product.setCreatedBy(doc.getString("createdBy"));
                        product.setImageUrl(doc.getString("picture"));
                        productList.add(product);
                    }
                    productAdapter.notifyDataSetChanged();

                    if (productList.isEmpty()) {
                        Toast.makeText(this, "Không có sản phẩm nào", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải sản phẩm: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ================== MENU TOOLBAR ==================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.creator_home_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        mAuth.signOut();
        SharedPreferences sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        sharedPref.edit().clear().apply();

        startActivity(new Intent(HomeCustomerActivity.this, LoginActivity.class));
        finish();
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllData();
    }
}