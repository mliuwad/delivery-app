package com.example.udgh;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.udgh.models.Order;
import com.example.udgh.models.OrderDetail;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
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

public class SalaryShipperFragment extends Fragment {

    private Spinner spMonth;
    private TextView txtSalaryPeriod, txtSalary, txtCompletedOrder, txtSalaryStatus, txtViewAll;
    private RecyclerView rvCompletedOrder;
    private SwipeRefreshLayout swipeRefreshLayout;
    private BarChart barChart;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;
    private String shipperId;

    private SalaryOrderAdapter adapter;
    private List<Order> orderList;

    private int currentMonth;
    private int currentYear;

    private Context context; // Lưu context để dùng trong callback

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_salary_statistics, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        SharedPreferences sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE);
        userId = sharedPref.getString("userId", "");
        if (TextUtils.isEmpty(userId) && mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }
        shipperId = userId;

        // Ánh xạ
        spMonth = view.findViewById(R.id.spMonth);
        txtSalaryPeriod = view.findViewById(R.id.txtSalaryPeriod);
        txtSalary = view.findViewById(R.id.txtSalary);
        txtCompletedOrder = view.findViewById(R.id.txtCompletedOrder);
        txtSalaryStatus = view.findViewById(R.id.txtSalaryStatus);
        txtViewAll = view.findViewById(R.id.txtViewAll);
        rvCompletedOrder = view.findViewById(R.id.rvCompletedOrder);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        barChart = view.findViewById(R.id.barChart);

        // Setup SwipeRefresh
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(this::loadData);

        // Setup RecyclerView
        orderList = new ArrayList<>();
        adapter = new SalaryOrderAdapter(orderList);
        rvCompletedOrder.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCompletedOrder.setAdapter(adapter);

        // Setup Spinner tháng CUSTOM
        setupSpinner();

        txtViewAll.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), OrderManagementActivity.class);
            intent.putExtra("filter", "COMPLETED");
            startActivity(intent);
        });

        loadData();
        return view;
    }

    private void setupSpinner() {
        String[] months = new String[12];
        for (int i = 0; i < 12; i++) {
            months[i] = "Tháng " + (i + 1);
        }
        // Sử dụng custom layout
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_spinner_month,
                months
        );
        monthAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spMonth.setAdapter(monthAdapter);

        // Chọn tháng hiện tại
        Calendar cal = Calendar.getInstance();
        currentMonth = cal.get(Calendar.MONTH) + 1;
        currentYear = cal.get(Calendar.YEAR);
        spMonth.setSelection(currentMonth - 1);

        spMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentMonth = position + 1;
                loadData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadData() {
        if (TextUtils.isEmpty(shipperId) || getContext() == null) {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getContext(), "Chưa đăng nhập hoặc context null", Toast.LENGTH_SHORT).show();
            return;
        }

        swipeRefreshLayout.setRefreshing(true);

        // Hiển thị kỳ lương
        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth - 1, 1);
        int firstDay = cal.getActualMinimum(Calendar.DAY_OF_MONTH);
        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        cal.set(currentYear, currentMonth - 1, firstDay);
        String startDate = sdf.format(cal.getTime());
        cal.set(currentYear, currentMonth - 1, lastDay);
        String endDate = sdf.format(cal.getTime());
        txtSalaryPeriod.setText("Kỳ lương: " + startDate + " - " + endDate);

        // 1. Lấy dữ liệu lương từ SALARIES
        db.collection("SALARIES")
                .whereEqualTo("shipperID", shipperId)
                .whereEqualTo("month", currentMonth)
                .whereEqualTo("year", currentYear)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        Double totalSalary = doc.getDouble("totalSalary");
                        Long totalOrders = doc.getLong("totalOrders");
                        String paymentStatus = doc.getString("paymentStatus");

                        txtSalary.setText(formatCurrency(totalSalary != null ? totalSalary.intValue() : 0));
                        txtCompletedOrder.setText(String.valueOf(totalOrders != null ? totalOrders : 0));

                        if ("PAID".equals(paymentStatus)) {
                            txtSalaryStatus.setText("ĐÃ NHẬN");
                            txtSalaryStatus.setTextColor(getColorSafe(R.color.success));
                        } else {
                            txtSalaryStatus.setText("CHƯA NHẬN");
                            txtSalaryStatus.setTextColor(getColorSafe(R.color.warning));
                        }
                    } else {
                        txtSalary.setText("0đ");
                        txtCompletedOrder.setText("0");
                        txtSalaryStatus.setText("CHƯA CÓ");
                        txtSalaryStatus.setTextColor(getColorSafe(R.color.gray));
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi tải lương: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        // 2. Lấy danh sách đơn hàng COMPLETED của shipper (lọc thủ công theo tháng để tránh lỗi index)
        db.collection("ORDERS")
                .whereEqualTo("shipperId", shipperId)
                .whereEqualTo("status", "COMPLETED")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || getContext() == null) {
                        swipeRefreshLayout.setRefreshing(false);
                        return;
                    }

                    List<Order> tempList = new ArrayList<>();
                    Calendar startCal = Calendar.getInstance();
                    startCal.set(currentYear, currentMonth - 1, 1, 0, 0, 0);
                    startCal.set(Calendar.MILLISECOND, 0);
                    Timestamp startTimestamp = new Timestamp(startCal.getTime());

                    Calendar endCal = Calendar.getInstance();
                    endCal.set(currentYear, currentMonth - 1, startCal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
                    endCal.set(Calendar.MILLISECOND, 999);
                    Timestamp endTimestamp = new Timestamp(endCal.getTime());

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        order.setOrderId(doc.getId());
                        Timestamp deliveredAt = order.getDeliveredAt();
                        if (deliveredAt != null) {
                            if (deliveredAt.compareTo(startTimestamp) >= 0 && deliveredAt.compareTo(endTimestamp) <= 0) {
                                tempList.add(order);
                            }
                        }
                    }

                    // Sắp xếp theo deliveredAt mới nhất
                    tempList.sort((o1, o2) -> {
                        if (o1.getDeliveredAt() == null && o2.getDeliveredAt() == null) return 0;
                        if (o1.getDeliveredAt() == null) return 1;
                        if (o2.getDeliveredAt() == null) return -1;
                        return o2.getDeliveredAt().compareTo(o1.getDeliveredAt());
                    });

                    orderList.clear();
                    orderList.addAll(tempList);
                    adapter.notifyDataSetChanged();

                    // Tải OrderDetail cho từng đơn
                    loadOrderDetailsForOrders(tempList);

                    // Vẽ biểu đồ theo tuần (kiểm tra context)
                    if (isAdded() && getContext() != null) {
                        drawWeeklyChart(tempList);
                    }

                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Lỗi tải đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    // Hàm lấy màu an toàn
    private int getColorSafe(int colorRes) {
        if (getContext() != null) {
            return getContext().getColor(colorRes);
        } else {
            return android.graphics.Color.GRAY;
        }
    }

    // ================== TẢI ORDER DETAIL ==================
    private void loadOrderDetailsForOrders(List<Order> orders) {
        if (orders == null || orders.isEmpty()) return;

        for (Order order : orders) {
            db.collection("ORDERS")
                    .document(order.getOrderId())
                    .collection("ORDER_DETAILS")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!isAdded()) return;
                        List<OrderDetail> details = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            OrderDetail detail = doc.toObject(OrderDetail.class);
                            details.add(detail);
                        }
                        order.setOrderDetails(details);
                        adapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        order.setOrderDetails(new ArrayList<>());
                        adapter.notifyDataSetChanged();
                    });
        }
    }

    // ================== VẼ BIỂU ĐỒ CỘT THEO TUẦN ==================
    private void drawWeeklyChart(List<Order> orders) {
        if (getContext() == null) return;

        if (orders == null || orders.isEmpty()) {
            barChart.clear();
            barChart.setNoDataText("Không có dữ liệu");
            barChart.invalidate();
            return;
        }

        // Nhóm theo tuần (dùng DAY_OF_MONTH để tính)
        Map<Integer, Double> weeklySalary = new HashMap<>();
        for (Order order : orders) {
            if (order.getDeliveredAt() == null) continue;
            Calendar cal = Calendar.getInstance();
            cal.setTime(order.getDeliveredAt().toDate());
            int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
            // Tuần 1: 1-7, Tuần 2: 8-14, Tuần 3: 15-21, Tuần 4: 22-28, Tuần 5: 29-31
            int weekOfMonth = (dayOfMonth - 1) / 7 + 1;
            double fee = order.getDeliveryFee();
            weeklySalary.put(weekOfMonth, weeklySalary.getOrDefault(weekOfMonth, 0.0) + fee);
        }

        // Xác định số tuần cần hiển thị (ít nhất 4, nhiều nhất 5)
        int numWeeks = 0;
        for (int week : weeklySalary.keySet()) {
            if (week > numWeeks) numWeeks = week;
        }
        // Đảm bảo ít nhất 4 tuần (tháng thường có 4 tuần)
        numWeeks = Math.max(numWeeks, 4);
        // Nếu có tuần 5 thì hiển thị đến tuần 5
        if (numWeeks > 5) numWeeks = 5; // tháng có tối đa 5 tuần

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 1; i <= numWeeks; i++) {
            double value = weeklySalary.getOrDefault(i, 0.0);
            entries.add(new BarEntry(i - 1, (float) value));
            labels.add("Tuần " + i);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Thu nhập (đ)");
        dataSet.setColor(getColorSafe(R.color.primary));
        dataSet.setValueTextColor(android.graphics.Color.BLACK);
        dataSet.setValueTextSize(12f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return formatCurrency((int) value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        barChart.setData(barData);
        barChart.setFitBars(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(labels.size());

        barChart.getDescription().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    // ================== FORMAT TIỀN ==================
    private String formatCurrency(int price) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return format.format(price);
    }

    // ================== ADAPTER NỘI BỘ ==================
    private class SalaryOrderAdapter extends RecyclerView.Adapter<SalaryOrderAdapter.ViewHolder> {

        private List<Order> orderList;

        public SalaryOrderAdapter(List<Order> orderList) {
            this.orderList = orderList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_salary_order, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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
                OrderDetail detail = details.get(0);
                String productName = detail.getProductName();
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

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), CreateOrderActivity.class);
                intent.putExtra("orderId", order.getOrderId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return orderList != null ? orderList.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtOrderId, txtShipFee, txtProductName, txtCompletedDate;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                txtOrderId = itemView.findViewById(R.id.txtOrderId);
                txtShipFee = itemView.findViewById(R.id.txtShipFee);
                txtProductName = itemView.findViewById(R.id.txtProductName);
                txtCompletedDate = itemView.findViewById(R.id.txtCompletedDate);
            }
        }
    }
}