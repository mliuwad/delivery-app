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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.udgh.models.Order;
import com.example.udgh.viewmodels.OrderViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.example.udgh.viewmodels.SessionViewModel;

import java.util.ArrayList;
import java.util.List;

public class OrderDetailCreatorActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private SearchView searchView;
    private RecyclerView rvOrder;
    private TextView txtTotalOrder;

    private String userId;
    private String currentFilter = "ALL";
    private OrderViewModel orderViewModel;
    private SessionViewModel sessionViewModel;
    private OrderAdapter orderAdapter;
    private final List<Order> orderList = new ArrayList<>();

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

        SharedPreferences preferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        userId = preferences.getString("userId", "");
        if (TextUtils.isEmpty(userId)) {
            sessionViewModel = new ViewModelProvider(this).get(SessionViewModel.class);
            userId = sessionViewModel.getCurrentUserId();
        }

        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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

        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);
        orderAdapter = new OrderAdapter(orderList, CreatorOrderDetailActivity.class);
        rvOrder.setLayoutManager(new LinearLayoutManager(this));
        rvOrder.setAdapter(orderAdapter);

        observeViewModel();
        setupSearchView();
        setupTabs();
        loadOrders();
    }

    private void observeViewModel() {
        orderViewModel.getOrders().observe(this, orders -> {
            orderList.clear();
            if (orders != null) orderList.addAll(orders);
            orderAdapter.notifyDataSetChanged();
            txtTotalOrder.setText("Tổng số: " + orderList.size() + " đơn hàng");
        });

        orderViewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(this, "Lỗi tải đơn hàng: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupSearchView() {
        AutoCompleteTextView input = searchView.findViewById(
                androidx.appcompat.R.id.search_src_text);
        if (input != null) {
            input.setTextColor(getColor(R.color.black));
            input.setHintTextColor(getColor(R.color.gray));
        }

        ImageView searchIcon = searchView.findViewById(
                androidx.appcompat.R.id.search_button);
        if (searchIcon != null) {
            searchIcon.setColorFilter(getColor(R.color.gray),
                    android.graphics.PorterDuff.Mode.SRC_IN);
        }

        ImageView closeIcon = searchView.findViewById(
                androidx.appcompat.R.id.search_close_btn);
        if (closeIcon != null) {
            closeIcon.setColorFilter(getColor(R.color.gray),
                    android.graphics.PorterDuff.Mode.SRC_IN);
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
        });
    }

    private void setupTabs() {
        if (tabLayout.getTabAt(0) != null) tabLayout.getTabAt(0).select();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String[] statuses = {"ALL", "PENDING", "DELIVERING", "COMPLETED", "CANCELLED"};
                int position = tab.getPosition();
                if (position >= 0 && position < statuses.length) {
                    currentFilter = statuses[position];
                    orderViewModel.setStatusFilter(currentFilter);
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    private void loadOrders() {
        orderViewModel.loadCreatorOrders(userId);
        orderViewModel.setStatusFilter(currentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (orderViewModel != null && !TextUtils.isEmpty(userId)) loadOrders();
    }
}
