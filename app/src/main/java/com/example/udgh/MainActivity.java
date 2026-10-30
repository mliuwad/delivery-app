package com.example.udgh;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.udgh.activities.ProductManagementActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private MaterialCardView cardProduct, cardOrder, cardShipper, cardSalary, cardStatistics;
    private TextView txtPending, txtCompleted, txtShipFee;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Kiểm tra đăng nhập
        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Ánh xạ view
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Trang chủ Admin");

        cardProduct = findViewById(R.id.cardProduct);
        cardOrder = findViewById(R.id.cardOrder);
        cardShipper = findViewById(R.id.cardShipper);
        cardSalary = findViewById(R.id.cardSalary);
        cardStatistics = findViewById(R.id.cardStatistics);

        // Set sự kiện click cho các card
        cardProduct.setOnClickListener(v -> {
            Toast.makeText(this, "Đang mở Quản lý sản phẩm...", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, ProductManagementActivity.class));
        });

        cardOrder.setOnClickListener(v -> {
            Toast.makeText(this, "Đang mở Quản lý đơn hàng...", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, OrderManagementActivity.class));
        });

        cardShipper.setOnClickListener(v -> {
            Toast.makeText(this, "Đang mở Quản lý nhân viên giao hàng...", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, ShipperManagementActivity.class));
        });

        cardSalary.setOnClickListener(v -> {
            Toast.makeText(this, "Đang mở Quản lý thanh toán lương...", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, SalaryManagement.class));
        });

        cardStatistics.setOnClickListener(v -> {
            Toast.makeText(this, "Đang mở Thống kê hệ thống...", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, StatisticsActivity.class));
        });

        // Load dữ liệu thống kê
        loadStatistics();
    }

    // ================== LOAD THỐNG KÊ ==================

    private void loadStatistics() {
        // Tạm thời để dữ liệu mẫu
        // Sau này thay bằng code lấy từ Firestore
    }

    // ================== XỬ LÝ MENU TOOLBAR ==================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // Kiểm tra nếu menu item có id là logout
        if (id == R.id.menu_logout) {
            logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ================== ĐĂNG XUẤT ==================

    private void logout() {
        mAuth.signOut();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
    }
}