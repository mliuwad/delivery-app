package com.example.udgh;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SalaryDetailActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private ImageView imgAvatar;
    private TextView txtName, txtPhone, txtStatus;
    private TextView txtCompletedOrders, txtTotalSalary;
    private TextView txtShippingRevenue, txtSalaryFinal;
    private RecyclerView rvOrders;
    private MaterialButton btnConfirmSalary;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseFirestore db;
    private String shipperId;
    private int month, year;
    private String shipperName;
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

        db = FirebaseFirestore.getInstance();

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

        orderList = new ArrayList<>();
        orderAdapter = new OrderAdapter(orderList);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(orderAdapter);

        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(this::loadData);

        btnConfirmSalary.setOnClickListener(v -> confirmPayment());

        loadData();
    }

    private void loadData() {
        swipeRefreshLayout.setRefreshing(true);

        // 1. Lấy thông tin User
        db.collection("USERS")
                .document(shipperId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        User user = userDoc.toObject(User.class);
                        shipperName = user.getFullName();
                        txtName.setText(shipperName);
                        txtPhone.setText(user.getPhone());

                        String avatarUrl = user.getAvatarUrl();
                        if (!TextUtils.isEmpty(avatarUrl)) {
                            Glide.with(this)
                                    .load(avatarUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.delivery_man)
                                    .error(R.drawable.delivery_man)
                                    .into(imgAvatar);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải user", Toast.LENGTH_SHORT).show());

        // 2. Lấy dữ liệu tổng hợp từ SALARIES
        db.collection("SALARIES")
                .whereEqualTo("shipperID", shipperId)
                .whereEqualTo("month", month)
                .whereEqualTo("year", year)
                .get()
                .addOnSuccessListener(salarySnapshots -> {
                    if (!salarySnapshots.isEmpty()) {
                        DocumentSnapshot doc = salarySnapshots.getDocuments().get(0);
                        totalOrders = doc.getLong("totalOrders") != null ? doc.getLong("totalOrders") : 0;
                        totalSalary = doc.getDouble("totalSalary") != null ? doc.getDouble("totalSalary") : 0;
                        paymentStatus = doc.getString("paymentStatus") != null ? doc.getString("paymentStatus") : "UNPAID";
                    } else {
                        totalOrders = 0;
                        totalSalary = 0;
                        paymentStatus = "UNPAID";
                    }

                    // Cập nhật UI thống kê
                    txtCompletedOrders.setText(String.valueOf(totalOrders));
                    txtTotalSalary.setText(formatCurrency((int) totalSalary));
                    txtShippingRevenue.setText("Tổng phí giao hàng: " + formatCurrency((int) totalSalary));
                    txtSalaryFinal.setText(formatCurrency((int) totalSalary));

                    if ("PAID".equals(paymentStatus)) {
                        txtStatus.setText("✅ Đã thanh toán");
                        txtStatus.setTextColor(getColor(R.color.success));
                        btnConfirmSalary.setVisibility(View.GONE);
                    } else {
                        txtStatus.setText("🟠 Chưa thanh toán");
                        txtStatus.setTextColor(getColor(R.color.warning));
                        btnConfirmSalary.setVisibility(View.VISIBLE);
                    }

                    // 3. Lấy danh sách đơn hàng để hiển thị (chỉ để hiển thị, không tính toán lại)
                    loadOrders();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải dữ liệu lương: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private void loadOrders() {
        // Truy vấn lấy tất cả đơn COMPLETED của shipper
        db.collection("ORDERS")
                .whereEqualTo("shipperId", shipperId)
                .whereEqualTo("status", "COMPLETED")
                .get()
                .addOnSuccessListener(orderSnapshots -> {
                    orderList.clear();

                    // Lọc thủ công theo tháng
                    Calendar startCal = Calendar.getInstance();
                    startCal.set(year, month - 1, 1, 0, 0, 0);
                    startCal.set(Calendar.MILLISECOND, 0);
                    Timestamp startTimestamp = new Timestamp(startCal.getTime());

                    Calendar endCal = Calendar.getInstance();
                    endCal.set(year, month - 1, startCal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
                    endCal.set(Calendar.MILLISECOND, 999);
                    Timestamp endTimestamp = new Timestamp(endCal.getTime());

                    for (QueryDocumentSnapshot doc : orderSnapshots) {
                        Order order = doc.toObject(Order.class);
                        order.setOrderId(doc.getId());
                        Timestamp deliveredAt = order.getDeliveredAt();
                        if (deliveredAt != null && deliveredAt.compareTo(startTimestamp) >= 0 && deliveredAt.compareTo(endTimestamp) <= 0) {
                            orderList.add(order);
                        }
                    }

                    // Sắp xếp theo deliveredAt mới nhất
                    orderList.sort((o1, o2) -> {
                        if (o1.getDeliveredAt() == null && o2.getDeliveredAt() == null) return 0;
                        if (o1.getDeliveredAt() == null) return 1;
                        if (o2.getDeliveredAt() == null) return -1;
                        return o2.getDeliveredAt().compareTo(o1.getDeliveredAt());
                    });

                    orderAdapter.notifyDataSetChanged();

                    // Load OrderDetail cho từng đơn
                    loadOrderDetails();

                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private void loadOrderDetails() {
        for (Order order : orderList) {
            db.collection("ORDERS")
                    .document(order.getOrderId())
                    .collection("ORDER_DETAILS")
                    .get()
                    .addOnSuccessListener(detailSnapshots -> {
                        List<OrderDetail> details = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : detailSnapshots) {
                            OrderDetail detail = doc.toObject(OrderDetail.class);
                            details.add(detail);
                        }
                        order.setOrderDetails(details);
                        orderAdapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        order.setOrderDetails(new ArrayList<>());
                        orderAdapter.notifyDataSetChanged();
                    });
        }
    }

    private void confirmPayment() {
        btnConfirmSalary.setEnabled(false);
        btnConfirmSalary.setText("Đang cập nhật...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("paymentStatus", "PAID");

        db.collection("SALARIES")
                .whereEqualTo("shipperID", shipperId)
                .whereEqualTo("month", month)
                .whereEqualTo("year", year)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        String docId = snapshots.getDocuments().get(0).getId();
                        db.collection("SALARIES")
                                .document(docId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Đã xác nhận thanh toán!", Toast.LENGTH_SHORT).show();
                                    paymentStatus = "PAID";
                                    txtStatus.setText("✅ Đã thanh toán");
                                    txtStatus.setTextColor(getColor(R.color.success));
                                    btnConfirmSalary.setVisibility(View.GONE);
                                    btnConfirmSalary.setEnabled(true);
                                    btnConfirmSalary.setText("XÁC NHẬN ĐÃ THANH TOÁN");
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    btnConfirmSalary.setEnabled(true);
                                    btnConfirmSalary.setText("XÁC NHẬN ĐÃ THANH TOÁN");
                                });
                    } else {
                        Toast.makeText(this, "Không tìm thấy dữ liệu lương", Toast.LENGTH_SHORT).show();
                        btnConfirmSalary.setEnabled(true);
                        btnConfirmSalary.setText("XÁC NHẬN ĐÃ THANH TOÁN");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnConfirmSalary.setEnabled(true);
                    btnConfirmSalary.setText("XÁC NHẬN ĐÃ THANH TOÁN");
                });
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