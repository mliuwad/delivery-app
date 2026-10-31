package com.example.udgh;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.udgh.activities.OrderManagementActivity;
import com.example.udgh.models.Order;
import com.example.udgh.models.OrderDetail;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class HomeShipperFragment extends Fragment {

    private TextView txtShipperName, txtWaiting, txtDelivering, txtCompleted, txtIncome;
    private TextView txtViewAllWaiting;
    private RecyclerView rvWaitingOrders;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    private OrderAdapter waitingOrderAdapter;
    private List<Order> waitingOrderList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_shipper, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        SharedPreferences sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE);
        userId = sharedPref.getString("userId", "");
        if (TextUtils.isEmpty(userId) && mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        // Ánh xạ view
        txtShipperName = view.findViewById(R.id.txtShipperName);
        txtWaiting = view.findViewById(R.id.txtWaiting);
        txtDelivering = view.findViewById(R.id.txtDelivering);
        txtCompleted = view.findViewById(R.id.txtCompleted);
        txtIncome = view.findViewById(R.id.txtIncome);
        txtViewAllWaiting = view.findViewById(R.id.txtViewAllWaiting);
        rvWaitingOrders = view.findViewById(R.id.rvWaitingOrders);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadData();
        });

        // Setup RecyclerView
        waitingOrderList = new ArrayList<>();
        waitingOrderAdapter = new OrderAdapter(waitingOrderList);
        rvWaitingOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        rvWaitingOrders.setAdapter(waitingOrderAdapter);

        // Xem tất cả đơn chờ nhận
        // Khi bấm "Xem tất cả", chỉ mở OrderManagementActivity, không gửi filter
        txtViewAllWaiting.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), OrderManagementActivity.class);
            startActivity(intent);
        });

        loadData();
        return view;
    }

    private void loadData() {
        // Bắt đầu hiệu ứng refresh (nếu đang kéo)
        swipeRefreshLayout.setRefreshing(true);

        loadShipperInfo();
        loadStatistics();
        loadWaitingOrders();
    }

    private void loadShipperInfo() {
        if (TextUtils.isEmpty(userId)) {
            txtShipperName.setText("Người dùng");
            return;
        }
        db.collection("USERS")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("fullName");
                        txtShipperName.setText(TextUtils.isEmpty(name) ? "Người dùng" : name);
                    }
                })
                .addOnFailureListener(e -> txtShipperName.setText("Người dùng"));
    }

    private void loadStatistics() {
        if (TextUtils.isEmpty(userId)) return;

        // Đếm đơn chờ nhận (PENDING, shipperId rỗng)
        db.collection("ORDERS")
                .whereEqualTo("status", "PENDING")
                .whereEqualTo("shipperId", "")
                .get()
                .addOnSuccessListener(query -> {
                    txtWaiting.setText(String.valueOf(query.size()));
                })
                .addOnFailureListener(e -> txtWaiting.setText("0"));

        // Đếm đơn đang giao của shipper này
        db.collection("ORDERS")
                .whereEqualTo("status", "DELIVERING")
                .whereEqualTo("shipperId", userId)
                .get()
                .addOnSuccessListener(query -> {
                    txtDelivering.setText(String.valueOf(query.size()));
                })
                .addOnFailureListener(e -> txtDelivering.setText("0"));

        // Đếm đơn hoàn thành + tính thu nhập
        db.collection("ORDERS")
                .whereEqualTo("status", "COMPLETED")
                .whereEqualTo("shipperId", userId)
                .get()
                .addOnSuccessListener(query -> {
                    int count = query.size();
                    txtCompleted.setText(String.valueOf(count));
                    double totalIncome = 0;
                    for (QueryDocumentSnapshot doc : query) {
                        Double fee = doc.getDouble("deliveryFee");
                        if (fee != null) totalIncome += fee;
                    }
                    txtIncome.setText(formatCurrency((int) totalIncome));
                })
                .addOnFailureListener(e -> {
                    txtCompleted.setText("0");
                    txtIncome.setText("0đ");
                });
    }

    // ================== LOAD 2 ĐƠN CHỜ NHẬN NGẪU NHIÊN ==================
    private void loadWaitingOrders() {
        if (TextUtils.isEmpty(userId)) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        // Lấy TẤT CẢ đơn PENDING, shipperId rỗng (không orderBy, không limit)
        db.collection("ORDERS")
                .whereEqualTo("status", "PENDING")
                .whereEqualTo("shipperId", "")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Chuyển toàn bộ kết quả thành List<Order>
                    List<Order> allOrders = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        order.setOrderId(doc.getId());
                        allOrders.add(order);
                    }

                    // Xáo trộn ngẫu nhiên
                    Collections.shuffle(allOrders);

                    // Chọn 2 đơn đầu (hoặc ít hơn nếu tổng số < 2)
                    int limit = Math.min(2, allOrders.size());
                    List<Order> randomOrders = allOrders.subList(0, limit);

                    waitingOrderList.clear();
                    waitingOrderList.addAll(randomOrders);
                    waitingOrderAdapter.notifyDataSetChanged();

                    // Load OrderDetail cho từng đơn được chọn
                    for (Order order : waitingOrderList) {
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
                                    waitingOrderAdapter.notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> {
                                    order.setOrderDetails(new ArrayList<>());
                                    waitingOrderAdapter.notifyDataSetChanged();
                                });
                    }

                    // Dừng hiệu ứng refresh sau khi tải thành công
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi tải đơn chờ nhận: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private String formatCurrency(int price) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return format.format(price);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

}