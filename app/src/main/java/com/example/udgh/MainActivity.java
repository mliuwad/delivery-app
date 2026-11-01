package com.example.udgh;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.example.udgh.activities.OrderManagementActivity;
import com.example.udgh.activities.ProductManagementActivity;
import com.example.udgh.viewmodels.SessionViewModel;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private SessionViewModel sessionViewModel;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RoleAccess.requireRole(this, "ADMIN", this::initializeAdminScreen);
    }

    private void initializeAdminScreen() {
        sessionViewModel = new ViewModelProvider(this).get(SessionViewModel.class);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Trang chủ Admin");

        MaterialCardView cardProduct = findViewById(R.id.cardProduct);
        MaterialCardView cardOrder = findViewById(R.id.cardOrder);
        MaterialCardView cardShipper = findViewById(R.id.cardShipper);
        MaterialCardView cardSalary = findViewById(R.id.cardSalary);
        MaterialCardView cardStatistics = findViewById(R.id.cardStatistics);

        cardProduct.setOnClickListener(v -> open(ProductManagementActivity.class,
                "Đang mở Quản lý sản phẩm..."));
        cardOrder.setOnClickListener(v -> open(OrderManagementActivity.class,
                "Đang mở Quản lý đơn hàng..."));
        cardShipper.setOnClickListener(v -> open(ShipperManagementActivity.class,
                "Đang mở Quản lý nhân viên giao hàng..."));
        cardSalary.setOnClickListener(v -> open(SalaryManagement.class,
                "Đang mở Quản lý thanh toán lương..."));
        cardStatistics.setOnClickListener(v -> open(StatisticsActivity.class,
                "Đang mở Thống kê hệ thống..."));
    }

    private void open(Class<?> destination, String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, destination));
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_toolbar_menu, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_logout) {
            sessionViewModel.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
