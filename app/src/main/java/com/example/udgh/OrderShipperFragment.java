package com.example.udgh;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.udgh.models.Order;
import com.example.udgh.models.OrderDetail;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;

public class OrderShipperFragment extends Fragment {

    private TabLayout tabLayout;
    private EditText searchView;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_shipper, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        SharedPreferences sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE);
        userId = sharedPref.getString("userId", "");
        if (TextUtils.isEmpty(userId) && mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        // Ánh xạ view
        tabLayout = view.findViewById(R.id.tabLayout);
        searchView = view.findViewById(R.id.searchView);
        rvOrder = view.findViewById(R.id.rvOrder);
        txtTotalOrder = view.findViewById(R.id.txtTotalOrder);

        // Setup RecyclerView
        orderList = new ArrayList<>();
        orderListFull = new ArrayList<>();
        orderAdapter = new OrderAdapter(orderList, ShipperOrderDetailActivity.class);
        rvOrder.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOrder.setAdapter(orderAdapter);

        // Setup TabLayout
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                switch (position) {
                    case 0: currentFilter = "ALL"; break;
                    case 1: currentFilter = "PENDING"; break;
                    case 2: currentFilter = "DELIVERING"; break;
                    case 3: currentFilter = "COMPLETED"; break;
                    case 4: currentFilter = "CANCELLED"; break;
                }
                loadOrdersFromFirestore();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Setup SearchView
        searchView.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterOrders(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Mặc định tab Tất cả
        tabLayout.getTabAt(0).select();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadOrdersFromFirestore();
    }

    // ------------------ LOAD ĐƠN HÀNG CHO SHIPPER ------------------
    private void loadOrdersFromFirestore() {
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(getContext(), "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        // Nếu là tab "Tất cả" -> lấy cả đơn chờ nhận + đơn đã nhận
        if ("ALL".equals(currentFilter)) {
            fetchAllShipperOrders();
            return;
        }

        // Nếu là tab "Chờ giao" (PENDING) -> chỉ lấy đơn PENDING, shipperId rỗng
        if ("PENDING".equals(currentFilter)) {
            db.collection("ORDERS")
                    .whereIn("status", Arrays.asList("PENDING", "WAITING"))
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        List<Order> tempList = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            String assignedShipperId = doc.getString("shipperId");
                            // Dữ liệu cũ có thể shipperId rỗng, null hoặc chưa tồn tại.
                            if (TextUtils.isEmpty(assignedShipperId)) {
                                Order order = doc.toObject(Order.class);
                                order.setOrderId(doc.getId());
                                tempList.add(order);
                            }
                        }
                        processOrderList(tempList);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Lỗi tải đơn chờ nhận: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            return;
        }

        // Các tab còn lại (DELIVERING, COMPLETED, CANCELLED) -> lọc theo shipperId
        db.collection("ORDERS")
                .whereEqualTo("status", currentFilter)
                .whereEqualTo("shipperId", userId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Order> tempList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Order order = doc.toObject(Order.class);
                        order.setOrderId(doc.getId());
                        tempList.add(order);
                    }
                    processOrderList(tempList);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi tải đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Lấy tất cả đơn cho tab "Tất cả": PENDING chưa nhận + các đơn đã nhận của shipper
    private void fetchAllShipperOrders() {
        // Bước 1: Lấy đơn PENDING chưa nhận (shipperId rỗng)
        db.collection("ORDERS")
                .whereIn("status", Arrays.asList("PENDING", "WAITING"))
                .get()
                .addOnSuccessListener(pendingSnapshots -> {
                    List<Order> allOrders = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : pendingSnapshots) {
                        String assignedShipperId = doc.getString("shipperId");
                        if (TextUtils.isEmpty(assignedShipperId)) {
                            Order order = doc.toObject(Order.class);
                            order.setOrderId(doc.getId());
                            allOrders.add(order);
                        }
                    }

                    // Bước 2: Lấy đơn đã nhận của shipper này (tất cả status ngoại trừ PENDING)
                    db.collection("ORDERS")
                            .whereEqualTo("shipperId", userId)
                            .get()
                            .addOnSuccessListener(shipperSnapshots -> {
                                for (QueryDocumentSnapshot doc : shipperSnapshots) {
                                    String status = doc.getString("status");
                                    if (!"PENDING".equals(status) && !"WAITING".equals(status)) {
                                        Order order = doc.toObject(Order.class);
                                        order.setOrderId(doc.getId());
                                        allOrders.add(order);
                                    }
                                }
                                // Xử lý danh sách tổng hợp
                                processOrderList(allOrders);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Lỗi tải đơn đã nhận: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                // Vẫn xử lý danh sách đã có (chỉ pending)
                                processOrderList(allOrders);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi tải đơn chờ nhận: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Fallback: chỉ lấy đơn đã nhận của shipper
                    db.collection("ORDERS")
                            .whereEqualTo("shipperId", userId)
                            .get()
                            .addOnSuccessListener(shipperSnapshots -> {
                                List<Order> allOrders = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : shipperSnapshots) {
                                    Order order = doc.toObject(Order.class);
                                    order.setOrderId(doc.getId());
                                    allOrders.add(order);
                                }
                                processOrderList(allOrders);
                            })
                            .addOnFailureListener(ex ->
                                    Toast.makeText(getContext(), "Không thể tải đơn hàng", Toast.LENGTH_SHORT).show());
                });
    }

    // ------------------ XỬ LÝ DANH SÁCH (SẮP XẾP + LOAD DETAIL) ------------------
    private void processOrderList(List<Order> tempOrderList) {
        // Sắp xếp theo createdAt (mới nhất trước)
        Collections.sort(tempOrderList, (o1, o2) -> {
            if (o1.getCreatedAt() == null && o2.getCreatedAt() == null) return 0;
            if (o1.getCreatedAt() == null) return 1;
            if (o2.getCreatedAt() == null) return -1;
            return o2.getCreatedAt().compareTo(o1.getCreatedAt());
        });

        if (tempOrderList.isEmpty()) {
            orderList.clear();
            orderListFull.clear();
            orderAdapter.notifyDataSetChanged();
            updateTotalCount();
            Toast.makeText(getContext(), "Không có đơn hàng nào", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load OrderDetail cho từng đơn
        final int totalOrders = tempOrderList.size();
        final java.util.concurrent.atomic.AtomicInteger loadedCount = new java.util.concurrent.atomic.AtomicInteger(0);

        for (Order order : tempOrderList) {
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
                        int current = loadedCount.incrementAndGet();
                        if (current == totalOrders) {
                            // Cập nhật danh sách chính
                            orderList.clear();
                            orderList.addAll(tempOrderList);
                            orderListFull.clear();
                            orderListFull.addAll(tempOrderList);
                            if (!TextUtils.isEmpty(currentKeyword)) {
                                filterOrders(currentKeyword);
                            } else {
                                orderAdapter.notifyDataSetChanged();
                                updateTotalCount();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        order.setOrderDetails(new ArrayList<>());
                        int current = loadedCount.incrementAndGet();
                        if (current == totalOrders) {
                            orderList.clear();
                            orderList.addAll(tempOrderList);
                            orderListFull.clear();
                            orderListFull.addAll(tempOrderList);
                            if (!TextUtils.isEmpty(currentKeyword)) {
                                filterOrders(currentKeyword);
                            } else {
                                orderAdapter.notifyDataSetChanged();
                                updateTotalCount();
                            }
                        }
                    });
        }
    }

    // ------------------ TÌM KIẾM ------------------
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
            Toast.makeText(getContext(), "Không tìm thấy đơn hàng nào", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTotalCount() {
        String text = "Tổng số: " + orderList.size() + " đơn hàng";
        if (orderList.size() != orderListFull.size() && !orderListFull.isEmpty()) {
            text += " (Đã lọc)";
        }
        txtTotalOrder.setText(text);
    }
}