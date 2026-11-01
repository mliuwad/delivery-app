package com.example.udgh;

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
import java.util.concurrent.atomic.AtomicInteger;

public class CreatorOrderFragment extends Fragment {
    private FirebaseFirestore db;
    private String userId;
    private TabLayout tabs;
    private EditText searchView;
    private RecyclerView rvOrders;
    private TextView txtTotal;
    private OrderAdapter adapter;
    private final List<Order> allOrders = new ArrayList<>();
    private final List<Order> visibleOrders = new ArrayList<>();
    private String currentStatus = "ALL";
    private String keyword = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_creator_orders, container, false);
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();
        tabs = view.findViewById(R.id.tabsOrderStatus);
        searchView = view.findViewById(R.id.searchOrders);
        rvOrders = view.findViewById(R.id.rvCreatorOrders);
        txtTotal = view.findViewById(R.id.txtTotalCreatorOrders);

        adapter = new OrderAdapter(visibleOrders, CreatorOrderDetailActivity.class);
        rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOrders.setAdapter(adapter);

        String[] titles = {"Tất cả", "Chờ nhận", "Đang giao", "Hoàn thành", "Đã hủy"};
        for (String title : titles) tabs.addTab(tabs.newTab().setText(title));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                String[] statuses = {"ALL", "PENDING", "DELIVERING", "COMPLETED", "CANCELLED"};
                currentStatus = statuses[tab.getPosition()];
                applyFilters();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        searchView.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                keyword = s.toString();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        loadOrders();
        return view;
    }

    private void loadOrders() {
        if (TextUtils.isEmpty(userId)) return;
        db.collection("ORDERS").whereEqualTo("creatorId", userId).get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;
                    List<Order> loaded = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Order order = doc.toObject(Order.class);
                        order.setOrderId(doc.getId());
                        loaded.add(order);
                    }
                    Collections.sort(loaded, (a, b) -> {
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });
                    if (loaded.isEmpty()) {
                        allOrders.clear();
                        applyFilters();
                        return;
                    }
                    AtomicInteger count = new AtomicInteger();
                    for (Order order : loaded) {
                        db.collection("ORDERS").document(order.getOrderId())
                                .collection("ORDER_DETAILS").get()
                                .addOnSuccessListener(details -> {
                                    List<OrderDetail> list = new ArrayList<>();
                                    for (QueryDocumentSnapshot detail : details) list.add(detail.toObject(OrderDetail.class));
                                    order.setOrderDetails(list);
                                    if (count.incrementAndGet() == loaded.size()) finishLoading(loaded);
                                })
                                .addOnFailureListener(e -> {
                                    order.setOrderDetails(new ArrayList<>());
                                    if (count.incrementAndGet() == loaded.size()) finishLoading(loaded);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(requireContext(), "Lỗi tải đơn hàng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void finishLoading(List<Order> loaded) {
        if (!isAdded()) return;
        allOrders.clear();
        allOrders.addAll(loaded);
        applyFilters();
    }

    private void applyFilters() {
        if (!isAdded() || adapter == null) return;
        visibleOrders.clear();
        String q = keyword == null ? "" : keyword.trim().toLowerCase();
        for (Order order : allOrders) {
            boolean statusMatches = "ALL".equals(currentStatus)
                    || ("PENDING".equals(currentStatus) && ("PENDING".equals(order.getStatus()) || "WAITING".equals(order.getStatus())))
                    || currentStatus.equals(order.getStatus());
            String id = order.getOrderId() == null ? "" : order.getOrderId().toLowerCase();
            String receiver = order.getReceiverName() == null ? "" : order.getReceiverName().toLowerCase();
            if (statusMatches && (q.isEmpty() || id.contains(q) || receiver.contains(q))) visibleOrders.add(order);
        }
        adapter.notifyDataSetChanged();
        txtTotal.setText("Tổng số: " + visibleOrders.size() + " đơn hàng");
    }

    @Override public void onResume() { super.onResume(); loadOrders(); }
}