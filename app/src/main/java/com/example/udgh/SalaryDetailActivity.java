package com.example.udgh;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.lifecycle.ViewModelProvider;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.udgh.models.Order;
import com.example.udgh.models.OrderDetail;
import com.example.udgh.models.User;
import com.example.udgh.repositories.SalaryDetailRepository;
import com.example.udgh.viewmodels.SalaryDetailViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SalaryDetailActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private ImageView imgAvatar;
    private TextView txtName, txtPhone, txtStatus;
    private TextView txtCompletedOrders, txtTotalSalary;
    private TextView txtShippingRevenue, txtSalaryFinal;
    private RecyclerView rvOrders;
    private MaterialButton btnConfirmSalary;
    private SwipeRefreshLayout swipeRefreshLayout;

    private SalaryDetailViewModel viewModel;
    private String shipperId;
    private int month, year;
    private String salaryDocumentId;
    private double totalSalary = 0;
    private long totalOrders = 0;
    private String paymentStatus = "UNPAID";

    private OrderAdapter orderAdapter;
    private List<Order> orderList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_salary_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        shipperId = getIntent().getStringExtra("shipperId");
        month = getIntent().getIntExtra("month", Calendar.getInstance().get(Calendar.MONTH) + 1);
        year = getIntent().getIntExtra("year", Calendar.getInstance().get(Calendar.YEAR));

        if (TextUtils.isEmpty(shipperId)) {
            Toast.makeText(this, "Không tìm thấy shipper", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        toolbar = findViewById(R.id.toolbar);
        imgAvatar = findViewById(R.id.imgAvatar);
        txtName = findViewById(R.id.txtName);
        txtPhone = findViewById(R.id.txtPhone);
        txtStatus = findViewById(R.id.txtStatus);
        txtCompletedOrders = findViewById(R.id.txtCompletedOrders);
        txtTotalSalary = findViewById(R.id.txtTotalSalary);
        txtShippingRevenue = findViewById(R.id.txtShippingRevenue);
        txtSalaryFinal = findViewById(R.id.txtSalaryFinal);
        rvOrders = findViewById(R.id.rvOrders);
        btnConfirmSalary = findViewById(R.id.btnConfirmSalary);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(SalaryDetailViewModel.class);
        orderList = new ArrayList<>();
        orderAdapter = new OrderAdapter(orderList);
        observeViewModel();
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(orderAdapter);

        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(this::loadData);

        btnConfirmSalary.setOnClickListener(v -> confirmPayment());

        loadData();
    }

    private void loadData() {
        viewModel.load(shipperId, month, year);
    }

    private void observeViewModel() {
        viewModel.getDetail().observe(this, data -> {
            if (data == null) return;
            User user = data.user;
            txtName.setText(user.getFullName() != null ? user.getFullName() : "Không có tên");
            txtPhone.setText(user.getPhone() != null ? user.getPhone() : "--");
            if (!TextUtils.isEmpty(user.getAvatarUrl())) {
                Glide.with(this).load(user.getAvatarUrl()).circleCrop()
                        .placeholder(R.drawable.delivery_man)
                        .error(R.drawable.delivery_man).into(imgAvatar);
            } else {
                imgAvatar.setImageResource(R.drawable.delivery_man);
            }

            totalSalary = data.totalSalary;
            totalOrders = data.totalOrders;
            paymentStatus = data.paymentStatus;
            salaryDocumentId = data.salaryDocumentId;

            txtCompletedOrders.setText(String.valueOf(totalOrders));
            txtTotalSalary.setText(formatCurrency((int) totalSalary));
            txtShippingRevenue.setText("Tổng phí giao hàng: " + formatCurrency((int) totalSalary));
            txtSalaryFinal.setText(formatCurrency((int) totalSalary));

            boolean paid = "PAID".equals(paymentStatus);
            txtStatus.setText(paid ? "Đã thanh toán" : "Chưa thanh toán");
            txtStatus.setTextColor(getColor(paid ? R.color.success : R.color.warning));
            btnConfirmSalary.setVisibility(paid || salaryDocumentId == null ? View.GONE : View.VISIBLE);
            btnConfirmSalary.setEnabled(true);
            btnConfirmSalary.setText("XÁC NHẬN ĐÃ THANH TOÁN");

            orderList.clear();
            orderList.addAll(data.orders);
            orderAdapter.notifyDataSetChanged();
        });

        viewModel.getLoading().observe(this, loading ->
                swipeRefreshLayout.setRefreshing(Boolean.TRUE.equals(loading)));

        viewModel.getMessage().observe(this, message -> {
            if (!TextUtils.isEmpty(message)) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getPaymentSuccess().observe(this, success -> {
            if (success == null) return;
            btnConfirmSalary.setEnabled(true);
            btnConfirmSalary.setText("XÁC NHẬN ĐÃ THANH TOÁN");
            if (success) {
                paymentStatus = "PAID";
                txtStatus.setText("Đã thanh toán");
                txtStatus.setTextColor(getColor(R.color.success));
                btnConfirmSalary.setVisibility(View.GONE);
            }
        });
    }

    private void confirmPayment() {
        btnConfirmSalary.setEnabled(false);
        btnConfirmSalary.setText("Đang cập nhật...");
        viewModel.confirmPayment();
    }

    private String formatCurrency(int price) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return format.format(price);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ================== ADAPTER CHO DANH SÁCH ĐƠN HÀNG ==================
    private class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

        private List<Order> orderList;

        public OrderAdapter(List<Order> orderList) {
            this.orderList = orderList;
        }

        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_salary_order, parent, false);
            return new OrderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            Order order = orderList.get(position);

            String orderId = order.getOrderId();
            if (orderId != null && orderId.startsWith("OD")) {
                holder.txtOrderId.setText("DH" + orderId.substring(2));
            } else {
                holder.txtOrderId.setText(orderId != null ? orderId : "DH---");
            }

            double fee = order.getDeliveryFee();
            holder.txtShipFee.setText("+ " + formatCurrency((int) fee));

            List<OrderDetail> details = order.getOrderDetails();
            if (details != null && !details.isEmpty()) {
                String productName = details.get(0).getProductName();
                if (details.size() > 1) {
                    productName += " (+" + (details.size() - 1) + " sản phẩm khác)";
                }
                holder.txtProductName.setText(productName);
            } else {
                holder.txtProductName.setText("Đang tải...");
            }

            if (order.getDeliveredAt() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                holder.txtCompletedDate.setText("Hoàn thành: " + sdf.format(order.getDeliveredAt().toDate()));
            } else {
                holder.txtCompletedDate.setText("Hoàn thành: --");
            }
        }

        @Override
        public int getItemCount() {
            return orderList != null ? orderList.size() : 0;
        }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView txtOrderId, txtShipFee, txtProductName, txtCompletedDate;

            public OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                txtOrderId = itemView.findViewById(R.id.txtOrderId);
                txtShipFee = itemView.findViewById(R.id.txtShipFee);
                txtProductName = itemView.findViewById(R.id.txtProductName);
                txtCompletedDate = itemView.findViewById(R.id.txtCompletedDate);
            }
        }
    }
}