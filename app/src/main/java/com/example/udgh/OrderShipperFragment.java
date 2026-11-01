package com.example.udgh;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
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
import com.example.udgh.viewmodels.SessionViewModel;

import java.util.ArrayList;
import java.util.List;

public class OrderShipperFragment extends Fragment {

    private OrderViewModel orderViewModel;
    private TabLayout tabLayout;
    private EditText searchView;
    private RecyclerView rvOrder;
    private TextView txtTotalOrder;
    private String userId;
    private String currentFilter = "ALL";

    private OrderAdapter orderAdapter;
    private final List<Order> orderList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_shipper, container, false);

        SharedPreferences sharedPref = requireActivity().getSharedPreferences(
                "MyAppPrefs", android.content.Context.MODE_PRIVATE);
        userId = sharedPref.getString("userId", "");
        if (TextUtils.isEmpty(userId)) userId = new ViewModelProvider(this).get(SessionViewModel.class).getCurrentUserId();

        tabLayout = view.findViewById(R.id.tabLayout);
        searchView = view.findViewById(R.id.searchView);
        rvOrder = view.findViewById(R.id.rvOrder);
        txtTotalOrder = view.findViewById(R.id.txtTotalOrder);

        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);

        orderAdapter = new OrderAdapter(orderList, ShipperOrderDetailActivity.class);
        rvOrder.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOrder.setAdapter(orderAdapter);

        observeViewModel();
        setupTabs();
        setupSearch();

        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(requireContext(), "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
        } else {
            loadOrders();
        }

        return view;
    }

    private void observeViewModel() {
        orderViewModel.getOrders().observe(getViewLifecycleOwner(), orders -> {
            orderList.clear();
            if (orders != null) orderList.addAll(orders);
            orderAdapter.notifyDataSetChanged();
            txtTotalOrder.setText("Tổng số: " + orderList.size() + " đơn hàng");
        });

        orderViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(requireContext(),
                        "Lỗi tải đơn hàng: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String[] statuses = {"ALL", "PENDING", "DELIVERING", "COMPLETED", "CANCELLED"};
                int position = tab.getPosition();
                if (position >= 0 && position < statuses.length) {
                    currentFilter = statuses[position];
                    loadOrders();
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });

        if (tabLayout.getTabAt(0) != null) {
            tabLayout.getTabAt(0).select();
        }
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

    private void loadOrders() {
        if (orderViewModel == null || TextUtils.isEmpty(userId)) return;
        orderViewModel.loadShipperOrders(userId, currentFilter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadOrders();
    }
}
