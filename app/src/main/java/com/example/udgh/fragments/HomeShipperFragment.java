package com.example.udgh.fragments;

import com.example.udgh.activities.HomeShipperActivity;
import com.example.udgh.activities.ShipperOrderDetailActivity;
import com.example.udgh.adapters.OrderAdapter;
import com.example.udgh.R;

import android.content.Context;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.udgh.repositories.DashboardRepository;
import com.example.udgh.viewmodels.DashboardViewModel;
import com.example.udgh.viewmodels.SessionViewModel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeShipperFragment extends Fragment {

    private TextView txtShipperName, txtWaiting, txtDelivering, txtCompleted, txtIncome;
    private TextView txtViewAllWaiting;
    private RecyclerView rvWaitingOrders;
    private SwipeRefreshLayout swipeRefreshLayout;

    private String userId;
    private DashboardViewModel dashboardViewModel;
    private OrderAdapter waitingOrderAdapter;
    private final List<com.example.udgh.models.Order> waitingOrderList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_shipper, container, false);

        SharedPreferences preferences = requireActivity().getSharedPreferences(
                "MyAppPrefs", Context.MODE_PRIVATE);
        userId = preferences.getString("userId", "");
        if (TextUtils.isEmpty(userId)) userId = new ViewModelProvider(this).get(SessionViewModel.class).getCurrentUserId();

        txtShipperName = view.findViewById(R.id.txtShipperName);
        txtWaiting = view.findViewById(R.id.txtWaiting);
        txtDelivering = view.findViewById(R.id.txtDelivering);
        txtCompleted = view.findViewById(R.id.txtCompleted);
        txtIncome = view.findViewById(R.id.txtIncome);
        txtViewAllWaiting = view.findViewById(R.id.txtViewAllWaiting);
        rvWaitingOrders = view.findViewById(R.id.rvWaitingOrders);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(this::loadDashboard);

        waitingOrderAdapter = new OrderAdapter(waitingOrderList, ShipperOrderDetailActivity.class);
        rvWaitingOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvWaitingOrders.setAdapter(waitingOrderAdapter);

        txtViewAllWaiting.setOnClickListener(v -> {
            if (getActivity() instanceof HomeShipperActivity) {
                ((HomeShipperActivity) getActivity()).openOrdersTab();
            }
        });

        observeDashboard();
        loadDashboard();
        return view;
    }

    private void observeDashboard() {
        dashboardViewModel.getShipperDashboard().observe(getViewLifecycleOwner(), data -> {
            if (data == null) return;
            txtShipperName.setText(TextUtils.isEmpty(data.shipperName) ? "Người dùng" : data.shipperName);
            txtWaiting.setText(String.valueOf(data.waiting));
            txtDelivering.setText(String.valueOf(data.delivering));
            txtCompleted.setText(String.valueOf(data.completed));
            txtIncome.setText(formatCurrency(data.income));
            waitingOrderList.clear();
            waitingOrderList.addAll(data.waitingOrders);
            waitingOrderAdapter.notifyDataSetChanged();
            swipeRefreshLayout.setRefreshing(false);
        });

        dashboardViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (!TextUtils.isEmpty(message) && isAdded()) {
                Toast.makeText(requireContext(), "Không thể tải dashboard: " + message,
                        Toast.LENGTH_SHORT).show();
            }
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void loadDashboard() {
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);
        if (dashboardViewModel != null && !TextUtils.isEmpty(userId)) {
            dashboardViewModel.loadShipperDashboard(userId);
        } else if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private String formatCurrency(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return format.format((long) amount);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboard();
    }
}
