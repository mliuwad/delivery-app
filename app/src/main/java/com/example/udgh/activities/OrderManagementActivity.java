package com.example.udgh.activities;

import com.example.udgh.adapters.*;
import com.example.udgh.dialogs.*;
import com.example.udgh.utils.*;
import com.example.udgh.fragments.*;

import android.content.Intent;
import android.os.Bundle;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.udgh.R;
import com.example.udgh.adapters.OrderAdminAdapter;
import com.example.udgh.models.Order;
import com.example.udgh.viewmodels.OrderViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class OrderManagementActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private SearchView searchView;
    private RecyclerView rvOrder;
    private TextView txtTotalOrder;

    private OrderAdminAdapter orderAdapter;
    private OrderViewModel orderViewModel;

    private int totalOrders = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_order_management);

        RoleAccess.requireRole(this, "ADMIN", this::initializeAdminScreen);
    }

    private void initializeAdminScreen() {

        setupWindowInsets();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupViewModel();
        setupSearchView();
        setupTabs();

        // Tải danh sách đơn hàng ban đầu
        orderViewModel.loadOrders("ALL");
    
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
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

    private void setupViewModel() {
        orderViewModel = new ViewModelProvider(this)
                .get(OrderViewModel.class);

        observeViewModel();
    }

    private void observeViewModel() {

        // Quan sát danh sách đơn hàng
        orderViewModel.getOrders().observe(this, orders -> {
            if (orders == null) {
                orderAdapter.setOrders(null);
            } else {
                orderAdapter.setOrders(orders);
            }

            updateTotalCount(orders);
        });

        // Quan sát tổng số đơn hàng trước khi tìm kiếm
        orderViewModel.getTotalOrdersCount().observe(this, count -> {
            totalOrders = count != null ? count : 0;
        });

        // Quan sát trạng thái tải
        orderViewModel.getIsLoading().observe(this, isLoading -> {

        });

        // Quan sát lỗi
        orderViewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(
                        this,
                        "Lỗi tải đơn hàng: " + message,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void setupRecyclerView() {
        orderAdapter = new OrderAdminAdapter(this::openOrderDetail);

        rvOrder.setLayoutManager(new LinearLayoutManager(this));
        rvOrder.setAdapter(orderAdapter);
    }

    private void setupSearchView() {
        AutoCompleteTextView textView = searchView.findViewById(
                androidx.appcompat.R.id.search_src_text
        );

        if (textView != null) {
            textView.setTextColor(getColor(R.color.black));
            textView.setHintTextColor(getColor(R.color.gray));
        }

        ImageView searchIcon = searchView.findViewById(
                androidx.appcompat.R.id.search_button
        );

        if (searchIcon != null) {
            searchIcon.setColorFilter(
                    getColor(R.color.gray),
                    android.graphics.PorterDuff.Mode.SRC_IN
            );
        }

        ImageView closeIcon = searchView.findViewById(
                androidx.appcompat.R.id.search_close_btn
        );

        if (closeIcon != null) {
            closeIcon.setColorFilter(
                    getColor(R.color.gray),
                    android.graphics.PorterDuff.Mode.SRC_IN
            );
        }

        searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {

                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        orderViewModel.searchOrders(query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        orderViewModel.searchOrders(newText);
                        return true;
                    }
                }
        );
    }

    private void setupTabs() {
        // Chọn tab đầu tiên trước khi đăng ký listener
        if (tabLayout.getTabAt(0) != null) {
            tabLayout.getTabAt(0).select();
        }

        tabLayout.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {

                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        String status;

                        switch (tab.getPosition()) {
                            case 1:
                                status = "PENDING";
                                break;

                            case 2:
                                status = "DELIVERING";
                                break;

                            case 3:
                                status = "COMPLETED";
                                break;

                            case 4:
                                status = "CANCELLED";
                                break;

                            case 0:
                            default:
                                status = "ALL";
                                break;
                        }

                        orderViewModel.loadOrders(status);
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                    }
                }
        );
    }

    private void openOrderDetail(Order order) {
        if (order == null || order.getOrderId() == null) {
            Toast.makeText(
                    this,
                    "Không tìm thấy mã đơn hàng",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent = new Intent(
                this,
                OrderDetailActivity.class
        );

        intent.putExtra("orderId", order.getOrderId());

        startActivity(intent);
    }

    private void updateTotalCount(List<Order> orders) {
        int displayedCount = orders != null ? orders.size() : 0;

        String text = "Tổng số: " + displayedCount + " đơn hàng";

        if (displayedCount != totalOrders && totalOrders > 0) {
            text += " (Đã lọc)";
        }

        txtTotalOrder.setText(text);
    }
}