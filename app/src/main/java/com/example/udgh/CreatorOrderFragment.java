package com.example.udgh;

import android.os.Bundle;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.udgh.models.Order;
import com.example.udgh.viewmodels.OrderViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class CreatorOrderFragment extends Fragment {

    private OrderViewModel orderViewModel;
    private String userId;
    private TabLayout tabs;
    private EditText searchView;
    private RecyclerView rvOrders;
    private TextView txtTotal;
    private OrderAdapter adapter;
    private final List<Order> visibleOrders = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_creator_orders, container, false);

        userId = FirebaseAuth.getInstance().getUid();
        tabs = view.findViewById(R.id.tabsOrderStatus);
        searchView = view.findViewById(R.id.searchOrders);
        rvOrders = view.findViewById(R.id.rvCreatorOrders);
        txtTotal = view.findViewById(R.id.txtTotalCreatorOrders);

        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);

        adapter = new OrderAdapter(visibleOrders, CreatorOrderDetailActivity.class);
        rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOrders.setAdapter(adapter);

        observeViewModel();
        setupTabs();
        setupSearch();

        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
        } else {
            orderViewModel.loadCreatorOrders(userId);
        }

        return view;
    }

    private void observeViewModel() {
        orderViewModel.getOrders().observe(getViewLifecycleOwner(), orders -> {
            visibleOrders.clear();
            if (orders != null) visibleOrders.addAll(orders);
            adapter.notifyDataSetChanged();
            txtTotal.setText("Tổng số: " + visibleOrders.size() + " đơn hàng");
        });

        orderViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(requireContext(),
                        "Lỗi tải đơn hàng: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupTabs() {
        String[] titles = {"Tất cả", "Chờ nhận", "Đang giao", "Hoàn thành", "Đã hủy"};
        for (String title : titles) {
            tabs.addTab(tabs.newTab().setText(title));
        }

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String[] statuses = {"ALL", "PENDING", "DELIVERING", "COMPLETED", "CANCELLED"};
                orderViewModel.setStatusFilter(statuses[tab.getPosition()]);
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    private void setupSearch() {
        searchView.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                orderViewModel.searchOrders(s.toString());
            }

            @Override public void afterTextChanged(Editable s) { }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (orderViewModel != null && userId != null && !userId.trim().isEmpty()) {
            orderViewModel.loadCreatorOrders(userId);
        }
    }
}
