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

public class OrderDetailCreatorActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private SearchView searchView;
    private RecyclerView rvOrder;
    private TextView txtTotalOrder;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    private OrderAdapter orderAdapter;
    private List<Order> orderList;
    private List<Order> orderListFull;
    private String currentFilter = "ALL";
    private String currentKeyword = "";

    // Cờ để tránh gọi nhiều lần
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_order_management);

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

        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Ánh xạ view
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        searchView = findViewById(R.id.searchView);
        rvOrder = findViewById(R.id.rvOrder);
        txtTotalOrder = findViewById(R.id.txtTotalOrder);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Đơn hàng của tôi");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        orderList = new ArrayList<>();
        orderListFull = new ArrayList<>();
        orderAdapter = new OrderAdapter(orderList);
        rvOrder.setLayoutManager(new LinearLayoutManager(this));
        rvOrder.setAdapter(orderAdapter);

        // Tùy chỉnh SearchView
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

        // Tab Layout
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentFilter = "ALL"; break;
                    case 1: currentFilter = "PENDING"; break;
                    case 2: currentFilter = "DELIVERING"; break;
                    case 3: currentFilter = "COMPLETED"; break;
                    case 4: currentFilter = "CANCELLED"; break;
                    default: currentFilter = "ALL";
                }
                loadOrders();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // SearchView listener
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

        // Chọn tab mặc định
        if (tabLayout.getTabAt(0) != null) {
            tabLayout.getTabAt(0).select();
        }
        loadOrders();
    }

    /**
     * Tải danh sách đơn hàng của người dùng hiện tại (creatorId == userId)
     */
    private void loadOrders() {
        if (TextUtils.isEmpty(userId) || isLoading) return;

        isLoading = true;

        // Query chỉ lấy đơn hàng của creator này
        com.google.firebase.firestore.Query query = db.collection("ORDERS")
                .whereEqualTo("creatorId", userId);

        if (!"ALL".equals(currentFilter)) {
            query = query.whereEqualTo("status", currentFilter);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Order> tempOrders = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        order.setOrderId(doc.getId());
                        tempOrders.add(order);
                    }

                    // Sắp xếp theo thời gian tạo (mới nhất trước)
                    Collections.sort(tempOrders, (o1, o2) -> {
                        if (o1.getCreatedAt() == null && o2.getCreatedAt() == null) return 0;
                        if (o1.getCreatedAt() == null) return 1;
                        if (o2.getCreatedAt() == null) return -1;
                        return o2.getCreatedAt().compareTo(o1.getCreatedAt());
                    });

                    if (tempOrders.isEmpty()) {
                        // Cập nhật danh sách rỗng
                        orderListFull.clear();
                        orderList.clear();
                        orderAdapter.notifyDataSetChanged();
                        updateTotalCount();
                        String statusName = getStatusName(currentFilter);
                        Toast.makeText(this, "Không có đơn hàng nào ở trạng thái '" + statusName + "'", Toast.LENGTH_SHORT).show();
                        isLoading = false;
                        return;
                    }

                    // Load chi tiết từng đơn (sub-collection ORDER_DETAILS)
                    final int total = tempOrders.size();
                    final AtomicInteger loaded = new AtomicInteger(0);

                    for (Order order : tempOrders) {
                        db.collection("ORDERS")
                                .document(order.getOrderId())
                                .collection("ORDER_DETAILS")
                                .get()
                                .addOnSuccessListener(detailSnapshots -> {
                                    List<OrderDetail> details = new ArrayList<>();
                                    for (QueryDocumentSnapshot detailDoc : detailSnapshots) {
                                        OrderDetail detail = detailDoc.toObject(OrderDetail.class);
                                        details.add(detail);
                                    }
                                    order.setOrderDetails(details);
                                    if (loaded.incrementAndGet() == total) {
                                        updateUI(tempOrders);
                                        isLoading = false;
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    // Nếu lỗi vẫn hiển thị đơn nhưng không có chi tiết
                                    order.setOrderDetails(new ArrayList<>());
                                    if (loaded.incrementAndGet() == total) {
                                        updateUI(tempOrders);
                                        isLoading = false;
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    Toast.makeText(this, "Lỗi tải đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI(List<Order> orders) {
        // Cập nhật dữ liệu mới
        orderListFull.clear();
        orderListFull.addAll(orders);

        // Áp dụng filter nếu có
        if (!TextUtils.isEmpty(currentKeyword)) {
            // Gọi filter nhưng KHÔNG notify ở đây
            applyFilter();
        } else {
            // Cập nhật trực tiếp
            orderList.clear();
            orderList.addAll(orderListFull);
        }

        // CHỈ GỌI NOTIFY 1 LẦN DUY NHẤT
        orderAdapter.notifyDataSetChanged();
        updateTotalCount();

        if (orderList.isEmpty()) {
            String statusName = getStatusName(currentFilter);
            Toast.makeText(this, "Không có đơn hàng nào ở trạng thái '" + statusName + "'", Toast.LENGTH_SHORT).show();
        }
    }

    // Tách riêng filter để không gọi notify
    private void applyFilter() {
        if (TextUtils.isEmpty(currentKeyword)) {
            orderList.clear();
            orderList.addAll(orderListFull);
            return;
        }

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

        // CHỈ GỌI NOTIFY 1 LẦN
        orderAdapter.notifyDataSetChanged();
        updateTotalCount();

        if (orderList.isEmpty() && !TextUtils.isEmpty(currentKeyword)) {
            Toast.makeText(this, "Không tìm thấy đơn hàng", Toast.LENGTH_SHORT).show();
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
        loadOrders();
    }
}