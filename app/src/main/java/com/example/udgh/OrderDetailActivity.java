package com.example.udgh;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.lifecycle.ViewModelProvider;
import com.example.udgh.viewmodels.OrderViewModel;

import com.example.udgh.models.Order;
import com.example.udgh.models.OrderDetail;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {

    // Views
    private MaterialToolbar toolbar;

    // Order info
    private TextView tvOrderId, tvStatus, tvCreatedAt, tvDeliveredAt;
    private LinearLayout layoutDeliveredAt;

    // Sender
    private TextInputEditText edtSenderName, edtSenderPhone, edtSenderAddress;

    // Receiver
    private TextInputEditText edtReceiverName, edtReceiverPhone, edtReceiverAddress;

    // Products
    private RecyclerView rvSelectedProducts;
    private SelectedProductAdapter productAdapter;
    private List<OrderDetail> productList;
    private TextView txtNoProduct; // Thêm TextView thông báo

    // Note
    private TextInputEditText edtNote;

    // Payment
    private TextView txtProductPrice, txtShippingFee, txtTotalPrice;

    // Firebase
    private FirebaseAuth mAuth;
    private String userId;
    private OrderViewModel orderViewModel;

    // Data
    private String orderId;
    private Order currentOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_order_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        SharedPreferences sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        userId = sharedPref.getString("userId", "");
        if (TextUtils.isEmpty(userId) && mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        orderId = getIntent().getStringExtra("orderId");
        if (TextUtils.isEmpty(orderId)) {
            Toast.makeText(this, "Không có mã đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupRecyclerView();
        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);
        observeOrderViewModel();
        hideCreateModeViews();
        loadOrderDetails();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);

        tvOrderId = findViewById(R.id.tvOrderId);
        tvStatus = findViewById(R.id.tvStatus);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvDeliveredAt = findViewById(R.id.tvDeliveredAt);
        layoutDeliveredAt = findViewById(R.id.layoutDeliveredAt);

        edtSenderName = findViewById(R.id.edtSenderName);
        edtSenderPhone = findViewById(R.id.edtSenderPhone);
        edtSenderAddress = findViewById(R.id.edtSenderAddress);

        edtReceiverName = findViewById(R.id.edtReceiverName);
        edtReceiverPhone = findViewById(R.id.edtReceiverPhone);
        edtReceiverAddress = findViewById(R.id.edtReceiverAddress);

        rvSelectedProducts = findViewById(R.id.rvSelectedProducts);
        productList = new ArrayList<>();
        txtNoProduct = findViewById(R.id.txtNoProduct); // Ánh xạ

        edtNote = findViewById(R.id.edtNote);

        txtProductPrice = findViewById(R.id.txtProductPrice);
        txtShippingFee = findViewById(R.id.txtShippingFee);
        txtTotalPrice = findViewById(R.id.txtTotalPrice);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Chi tiết đơn hàng");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        // Truyền isViewMode = true để ẩn nút xóa
        productAdapter = new SelectedProductAdapter(productList, null, true);
        rvSelectedProducts.setLayoutManager(new LinearLayoutManager(this));
        rvSelectedProducts.setAdapter(productAdapter);
    }

    private void hideCreateModeViews() {
        // Ẩn các nút tạo và chọn
        //findViewById(R.id.btnCreateOrder).setVisibility(View.GONE);
        findViewById(R.id.btnChooseProduct).setVisibility(View.GONE);
        findViewById(R.id.txtAddNewProduct).setVisibility(View.GONE);
        findViewById(R.id.btnChooseSenderLocation).setVisibility(View.GONE);
        findViewById(R.id.btnChooseReceiverLocation).setVisibility(View.GONE);
        findViewById(R.id.cbCOD).setVisibility(View.GONE);
        findViewById(R.id.layoutCOD).setVisibility(View.GONE);
        // KHÔNG ẨN txtNoProduct (để hiển thị khi rỗng)

        // Disable input
        edtSenderName.setEnabled(false);
        edtSenderPhone.setEnabled(false);
        edtSenderAddress.setEnabled(false);
        edtReceiverName.setEnabled(false);
        edtReceiverPhone.setEnabled(false);
        edtReceiverAddress.setEnabled(false);
        edtNote.setEnabled(false);
        edtNote.setFocusable(false);
        edtNote.setFocusableInTouchMode(false);

        findViewById(R.id.orderInfoLayout).setVisibility(View.VISIBLE);
    }

    private void observeOrderViewModel() {
        orderViewModel.getSelectedOrder().observe(this, order -> {
            if (order == null) return;
            currentOrder = order;
            productList.clear();
            if (order.getOrderDetails() != null) {
                productList.addAll(order.getOrderDetails());
            }
            displayOrderInfo();
            productAdapter.notifyDataSetChanged();
            updateProductUI();
            updateTotalPrice();
        });

        orderViewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(this, "Lỗi tải đơn: " + message, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void loadOrderDetails() {
        orderViewModel.loadOrderById(orderId);
    }

    // ================== CẬP NHẬT UI SẢN PHẨM ==================
    private void updateProductUI() {
        if (productList.isEmpty()) {
            txtNoProduct.setVisibility(View.VISIBLE);
            rvSelectedProducts.setVisibility(View.GONE);
        } else {
            txtNoProduct.setVisibility(View.GONE);
            rvSelectedProducts.setVisibility(View.VISIBLE);
        }
    }

    private void displayOrderInfo() {
        if (currentOrder == null) return;

        tvOrderId.setText("Đơn hàng: " + currentOrder.getOrderId());

        String status = currentOrder.getStatus();
        tvStatus.setText(getStatusText(status));
        tvStatus.setTextColor(getStatusColor(status));

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        if (currentOrder.getCreatedAt() != null) {
            tvCreatedAt.setText(sdf.format(currentOrder.getCreatedAt().toDate()));
        } else {
            tvCreatedAt.setText("--");
        }

        if (currentOrder.getDeliveredAt() != null) {
            tvDeliveredAt.setText(sdf.format(currentOrder.getDeliveredAt().toDate()));
            layoutDeliveredAt.setVisibility(View.VISIBLE);
        } else {
            layoutDeliveredAt.setVisibility(View.GONE);
        }

        edtSenderName.setText(currentOrder.getSenderName());
        edtSenderPhone.setText(currentOrder.getSenderPhone());
        edtSenderAddress.setText(currentOrder.getSenderAddress());

        edtReceiverName.setText(currentOrder.getReceiverName());
        edtReceiverPhone.setText(currentOrder.getReceiverPhone());
        edtReceiverAddress.setText(currentOrder.getDeliveryAddress());

        edtNote.setText(currentOrder.getNote());

        txtShippingFee.setText(formatCurrency((int) currentOrder.getDeliveryFee()));
    }

    private void updateTotalPrice() {
        if (currentOrder == null) return;
        double totalProductPrice = 0;
        for (OrderDetail detail : productList) {
            totalProductPrice += detail.getPrice() * detail.getQuantity();
        }
        txtProductPrice.setText(formatCurrency((int) totalProductPrice));
        double total = totalProductPrice + currentOrder.getDeliveryFee();
        txtTotalPrice.setText(formatCurrency((int) total));
    }

    private String getStatusText(String status) {
        if (status == null) return "Không xác định";
        switch (status) {
            case "PENDING": return "Chờ nhận";
            case "WAITING": return "Chờ nhận";
            case "DELIVERING": return "Đang giao";
            case "COMPLETED": return "Hoàn thành";
            case "CANCELLED": return "Đã hủy";
            default: return status;
        }
    }

    private int getStatusColor(String status) {
        if (status == null) return getColor(R.color.gray);
        switch (status) {
            case "PENDING":
            case "WAITING":
                return getColor(R.color.warning);
            case "DELIVERING":
                return getColor(R.color.secondary);
            case "COMPLETED":
                return getColor(R.color.success);
            case "CANCELLED":
                return getColor(R.color.error);
            default:
                return getColor(R.color.gray);
        }
    }

    private String formatCurrency(int price) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return format.format(price);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Không cần reload nếu đã load trong onCreate
    }
}