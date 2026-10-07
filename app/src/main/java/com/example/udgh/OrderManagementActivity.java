package com.example.udgh;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.udgh.models.Order;
import com.example.udgh.models.OrderDetail;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Activity dành cho ADMIN – xem tất cả đơn hàng.
 * Không lọc theo creatorId hay shipperId.
 */
public class OrderManagementActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private SearchView searchView;
    private RecyclerView rvOrder;
    private TextView txtTotalOrder;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private OrderAdminAdapter orderAdapter;
    private final List<Order> orderList = new ArrayList<>();
    private final List<Order> orderListFull = new ArrayList<>();

    private String currentFilter = "ALL";
    private String currentKeyword = "";

    private boolean isLoadingOrders = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_order_management);

        setupWindowInsets();
        initFirebase();
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearchView();
        setupTabs();

        // ADMIN: tải tất cả đơn hàng không cần role
        loadOrdersForAdmin();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        searchView = findViewById(R.id.searchView);
        rvOrder = findViewById(R.id.rvOrder);
        txtTotalOrder = findViewById(R.id.txtTotalOrder);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Quản lý đơn hàng");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        orderAdapter = new OrderAdminAdapter(orderList);
        rvOrder.setLayoutManager(new LinearLayoutManager(this));
        rvOrder.setAdapter(orderAdapter);
    }

    private void setupSearchView() {
        AutoCompleteTextView textView = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (textView != null) {
            textView.setTextColor(getColor(R.color.black));
            textView.setHintTextColor(getColor(R.color.gray));
        }

        ImageView searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_button);
        if (searchIcon != null) {
            searchIcon.setColorFilter(getColor(R.color.gray), android.graphics.PorterDuff.Mode.SRC_IN);
        }

        ImageView closeIcon = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        if (closeIcon != null) {
            closeIcon.setColorFilter(getColor(R.color.gray), android.graphics.PorterDuff.Mode.SRC_IN);
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterOrders(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterOrders(newText);
                return true;
            }
        });
    }

    private void setupTabs() {
        tabLayout.getTabAt(0).select();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentFilter = "ALL"; break;
                    case 1: currentFilter = "PENDING"; break;
                    case 2: currentFilter = "DELIVERING"; break;
                    case 3: currentFilter = "COMPLETED"; break;
                    case 4: currentFilter = "CANCELLED"; break;
                }
                loadOrdersForAdmin();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    /**
     * ADMIN: load tất cả đơn hàng (không lọc theo userId)
     */
    private void loadOrdersForAdmin() {
        if (isLoadingOrders) return;
        isLoadingOrders = true;

        com.google.firebase.firestore.Query query = db.collection("ORDERS");

        if (!"ALL".equals(currentFilter)) {
            query = query.whereEqualTo("status", currentFilter);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Order> tempOrderList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        order.setOrderId(doc.getId());
                        tempOrderList.add(order);
                    }

                    sortOrders(tempOrderList);

                    if (tempOrderList.isEmpty()) {
                        showEmptyOrderList();
                        isLoadingOrders = false;
                        return;
                    }

                    loadOrderDetails(tempOrderList, () -> {
                        updateOrderLists(tempOrderList);
                        isLoadingOrders = false;
                    });
                })
                .addOnFailureListener(e -> {
                    isLoadingOrders = false;
                    Toast.makeText(this, "Lỗi tải: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sortOrders(List<Order> orders) {
        Collections.sort(orders, (o1, o2) -> {
            if (o1.getCreatedAt() == null && o2.getCreatedAt() == null) return 0;
            if (o1.getCreatedAt() == null) return 1;
            if (o2.getCreatedAt() == null) return -1;
            return o2.getCreatedAt().compareTo(o1.getCreatedAt());
        });
    }

    private void loadOrderDetails(List<Order> orders, Runnable onComplete) {
        final int total = orders.size();
        final AtomicInteger loaded = new AtomicInteger(0);

        for (Order order : orders) {
            db.collection("ORDERS")
                    .document(order.getOrderId())
                    .collection("ORDER_DETAILS")
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        List<OrderDetail> details = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            details.add(doc.toObject(OrderDetail.class));
                        }
                        order.setOrderDetails(details);
                        if (loaded.incrementAndGet() == total) onComplete.run();
                    })
                    .addOnFailureListener(e -> {
                        order.setOrderDetails(new ArrayList<>());
                        if (loaded.incrementAndGet() == total) onComplete.run();
                    });
        }
    }

    private void updateOrderLists(List<Order> orders) {
        orderListFull.clear();
        orderListFull.addAll(orders);

        if (!TextUtils.isEmpty(currentKeyword)) {
            filterOrders(currentKeyword);
        } else {
            orderList.clear();
            orderList.addAll(orderListFull);
            orderAdapter.notifyDataSetChanged();
            updateTotalCount();
        }
    }

    private void showEmptyOrderList() {
        orderList.clear();
        orderListFull.clear();
        orderAdapter.notifyDataSetChanged();
        updateTotalCount();
        Toast.makeText(this, "Không có đơn hàng nào", Toast.LENGTH_SHORT).show();
    }

    private void filterOrders(String keyword) {
        currentKeyword = keyword != null ? keyword.trim() : "";
        if (TextUtils.isEmpty(currentKeyword)) {
            orderList.clear();
            orderList.addAll(orderListFull);
        } else {
            String searchText = currentKeyword.toLowerCase();
            List<Order> filtered = new ArrayList<>();
            for (Order order : orderListFull) {
                String orderId = order.getOrderId() != null ? order.getOrderId().toLowerCase() : "";
                String receiverName = order.getReceiverName() != null ? order.getReceiverName().toLowerCase() : "";
                String senderName = order.getSenderName() != null ? order.getSenderName().toLowerCase() : "";
                if (orderId.contains(searchText) ||
                        receiverName.contains(searchText) ||
                        senderName.contains(searchText)) {
                    filtered.add(order);
                }
            }
            orderList.clear();
            orderList.addAll(filtered);
        }
        orderAdapter.notifyDataSetChanged();
        updateTotalCount();
        if (orderList.isEmpty() && !TextUtils.isEmpty(currentKeyword)) {
            Toast.makeText(this, "Không tìm thấy", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTotalCount() {
        String text = "Tổng số: " + orderList.size() + " đơn hàng";
        if (orderList.size() != orderListFull.size() && !orderListFull.isEmpty()) {
            text += " (Đã lọc)";
        }
        txtTotalOrder.setText(text);
    }

    private String getStatusName(String status) {
        switch (status) {
            case "PENDING": return "Chờ giao";
            case "DELIVERING": return "Đang giao";
            case "COMPLETED": return "Hoàn thành";
            case "CANCELLED": return "Đã hủy";
            default: return "Tất cả";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Có thể refresh nếu cần
    }
}