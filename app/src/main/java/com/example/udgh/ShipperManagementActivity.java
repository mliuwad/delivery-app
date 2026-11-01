package com.example.udgh;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.udgh.models.User;
import com.example.udgh.viewmodels.ShipperManagementViewModel;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class ShipperManagementActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private SearchView searchView;
    private RecyclerView rvShipper;
    private TextView txtTotal;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ShipperManagementViewModel viewModel;
    private List<User> shipperList;
    private ShipperAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shipper_management);

        RoleAccess.requireRole(this, "ADMIN", this::initializeAdminScreen);
    }

    private void initializeAdminScreen() {

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        toolbar = findViewById(R.id.toolbar);
        searchView = findViewById(R.id.searchView);
        rvShipper = findViewById(R.id.rvShipper);
        txtTotal = findViewById(R.id.txtTotal);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());


        // Cấu hình màu sắc cho SearchView
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

        // Setup RecyclerView
        shipperList = new ArrayList<>();
        adapter = new ShipperAdapter(
                this, // ✅ Truyền context
                shipperList,
                new ShipperAdapter.OnShipperActionListener() {
                    @Override
                    public void onLock(User shipper) {
                        toggleShipperStatus(shipper);
                    }
                }
        );

        rvShipper.setLayoutManager(new LinearLayoutManager(this));
        rvShipper.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ShipperManagementViewModel.class);
        observeViewModel();

        // SwipeRefreshLayout
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(() -> loadShippers());

        // SearchView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterShippers(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterShippers(newText);
                return true;
            }
        });

        loadShippers();
    
    }

    private void observeViewModel() {
        viewModel.getShippers().observe(this, result -> {
            shipperList.clear();
            if (result != null) shipperList.addAll(result);
            adapter.notifyDataSetChanged();
            updateTotalCount();
            swipeRefreshLayout.setRefreshing(false);
        });

        viewModel.getMessage().observe(this, message -> {
            if (!TextUtils.isEmpty(message)) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void loadShippers() {
        swipeRefreshLayout.setRefreshing(true);
        viewModel.loadShippers();
    }

    private void filterShippers(String keyword) {
        viewModel.search(keyword);
    }

    private void updateTotalCount() {
        txtTotal.setText("Tổng số nhân viên: " + shipperList.size());
    }

    private void toggleShipperStatus(User shipper) {
        viewModel.toggleStatus(shipper);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadShippers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.shipper_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add) {
            Intent intent = new Intent(this, AddEditShipperActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}