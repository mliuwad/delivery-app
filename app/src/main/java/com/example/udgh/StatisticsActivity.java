package com.example.udgh;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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

import com.example.udgh.models.User;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private Spinner spMonth;
    private TextView txtTotalOrder, txtRevenue, txtCustomer, txtShipper;
    private PieChart pieChartStatus;
    private LineChart lineChartRevenue;
    private RecyclerView rvTopShipper;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseFirestore db;
    private int selectedMonth;
    private int selectedYear;
    private Timestamp startTimestamp, endTimestamp;

    private TopShipperAdapter topShipperAdapter;
    private List<TopShipperItem> topShipperList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_statistics);

        RoleAccess.requireRole(this, "ADMIN", this::initializeAdminScreen);
    }

    private void initializeAdminScreen() {

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        toolbar = findViewById(R.id.toolbar);
        spMonth = findViewById(R.id.spMonth);
        txtTotalOrder = findViewById(R.id.txtTotalOrder);
        txtRevenue = findViewById(R.id.txtRevenue);
        txtCustomer = findViewById(R.id.txtCustomer);
        txtShipper = findViewById(R.id.txtShipper);
        pieChartStatus = findViewById(R.id.pieChartStatus);
        lineChartRevenue = findViewById(R.id.lineChartRevenue);
        rvTopShipper = findViewById(R.id.rvTopShipper);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        setupMonthSpinner();

        topShipperList = new ArrayList<>();
        topShipperAdapter = new TopShipperAdapter(topShipperList);
        rvTopShipper.setLayoutManager(new LinearLayoutManager(this));
        rvTopShipper.setAdapter(topShipperAdapter);

        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(this::loadData);

        loadData();
    
    }

    private void setupMonthSpinner() {
        Calendar cal = Calendar.getInstance();
        selectedMonth = cal.get(Calendar.MONTH) + 1;
        selectedYear = cal.get(Calendar.YEAR);

        List<String> monthList = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            int month = i + 1;
            monthList.add("Tháng " + month + "/" + selectedYear);
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, monthList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMonth.setAdapter(spinnerAdapter);
        spMonth.setSelection(selectedMonth - 1);

        spMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonth = position + 1;
                updateTimeRange();
                loadData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        updateTimeRange();
    }

    private void updateTimeRange() {
        Calendar startCal = Calendar.getInstance();
        startCal.set(selectedYear, selectedMonth - 1, 1, 0, 0, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        startTimestamp = new Timestamp(startCal.getTime());

        Calendar endCal = Calendar.getInstance();
        endCal.set(selectedYear, selectedMonth - 1, startCal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        endCal.set(Calendar.MILLISECOND, 999);
        endTimestamp = new Timestamp(endCal.getTime());
    }

    private void loadData() {
        swipeRefreshLayout.setRefreshing(true);

        // 1. Thống kê từ SALARIES (tổng phí ship, đơn giao thành công, số shipper)
        loadStatisticsFromSalaries();

        // 2. Tổng đơn tất cả trạng thái
        loadTotalOrders();

        // 3. Biểu đồ trạng thái đơn
        loadPieChart();

        // 4. Biểu đồ doanh thu theo tháng
        loadLineChart();

        // 5. Top shipper
        loadTopShippers();

        swipeRefreshLayout.setRefreshing(false);
    }

    // ================== THỐNG KÊ TỪ SALARIES ==================
    private void loadStatisticsFromSalaries() {
        db.collection("SALARIES")
                .whereEqualTo("month", selectedMonth)
                .whereEqualTo("year", selectedYear)
                .get()
                .addOnSuccessListener(snapshots -> {
                    double totalShipFee = 0;
                    long totalCompletedOrders = 0;
                    int shipperCount = snapshots.size();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Double salary = doc.getDouble("totalSalary");
                        if (salary != null) totalShipFee += salary;
                        Long orders = doc.getLong("totalOrders");
                        if (orders != null) totalCompletedOrders += orders;
                    }

                    txtRevenue.setText(formatCurrency((int) totalShipFee));
                    txtCustomer.setText(String.valueOf(totalCompletedOrders));
                    txtShipper.setText(String.valueOf(shipperCount));
                })
                .addOnFailureListener(e -> {
                    txtRevenue.setText("0đ");
                    txtCustomer.setText("0");
                    txtShipper.setText("0");
                    Toast.makeText(this, "Lỗi tải SALARIES: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ================== TỔNG ĐƠN (TẤT CẢ TRẠNG THÁI) ==================
    private void loadTotalOrders() {
        // Lấy tất cả đơn, lọc thủ công theo tháng để tránh lỗi index
        db.collection("ORDERS")
                .get()
                .addOnSuccessListener(snapshots -> {
                    int count = 0;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Timestamp createdAt = doc.getTimestamp("createdAt");
                        if (createdAt != null && createdAt.compareTo(startTimestamp) >= 0 && createdAt.compareTo(endTimestamp) <= 0) {
                            count++;
                        }
                    }
                    txtTotalOrder.setText(String.valueOf(count));
                })
                .addOnFailureListener(e -> {
                    txtTotalOrder.setText("0");
                    Toast.makeText(this, "Lỗi tải tổng đơn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ================== BIỂU ĐỒ TRẠNG THÁI ĐƠN ==================
    private void loadPieChart() {
        // Lấy tất cả đơn, lọc thủ công theo tháng
        db.collection("ORDERS")
                .get()
                .addOnSuccessListener(snapshots -> {
                    Map<String, Integer> statusCount = new HashMap<>();
                    statusCount.put("PENDING", 0);
                    statusCount.put("DELIVERING", 0);
                    statusCount.put("COMPLETED", 0);
                    statusCount.put("CANCELLED", 0);

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Timestamp createdAt = doc.getTimestamp("createdAt");
                        if (createdAt != null && createdAt.compareTo(startTimestamp) >= 0 && createdAt.compareTo(endTimestamp) <= 0) {
                            String status = doc.getString("status");
                            if (status != null && statusCount.containsKey(status)) {
                                statusCount.put(status, statusCount.get(status) + 1);
                            }
                        }
                    }

                    List<PieEntry> entries = new ArrayList<>();
                    String[] labels = {"Chờ nhận", "Đang giao", "Hoàn thành", "Đã hủy"};
                    int[] colors = {ColorTemplate.rgb("#FF9800"), ColorTemplate.rgb("#2196F3"),
                            ColorTemplate.rgb("#4CAF50"), ColorTemplate.rgb("#F44336")};
                    int[] values = {statusCount.get("PENDING"), statusCount.get("DELIVERING"),
                            statusCount.get("COMPLETED"), statusCount.get("CANCELLED")};

                    for (int i = 0; i < labels.length; i++) {
                        if (values[i] > 0) {
                            entries.add(new PieEntry(values[i], labels[i]));
                        }
                    }

                    if (entries.isEmpty()) {
                        Toast.makeText(this, "Không có dữ liệu cho biểu đồ", Toast.LENGTH_SHORT).show();
                        pieChartStatus.clear();
                        return;
                    }

                    PieDataSet dataSet = new PieDataSet(entries, "Trạng thái đơn hàng");
                    dataSet.setColors(colors);
                    dataSet.setValueTextSize(14f);
                    dataSet.setValueTextColor(getColor(R.color.black));
                    dataSet.setValueFormatter(new PercentFormatter(pieChartStatus));

                    PieData data = new PieData(dataSet);
                    pieChartStatus.setData(data);
                    pieChartStatus.setUsePercentValues(true);
                    pieChartStatus.getDescription().setEnabled(false);
                    pieChartStatus.setEntryLabelTextSize(14f);
                    pieChartStatus.setEntryLabelColor(getColor(R.color.black));
                    pieChartStatus.animateY(1000);
                    pieChartStatus.invalidate();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải biểu đồ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ================== BIỂU ĐỒ DOANH THU THEO THÁNG (TỪ SALARIES) ==================
    private void loadLineChart() {
        Calendar cal = Calendar.getInstance();
        int currentMonth = cal.get(Calendar.MONTH) + 1;
        int currentYear = cal.get(Calendar.YEAR);

        List<String> monthLabels = new ArrayList<>();
        List<Float> revenueData = new ArrayList<>();

        int startMonth = currentMonth - 11;
        int startYear = currentYear;
        if (startMonth <= 0) {
            startMonth += 12;
            startYear--;
        }

        final int totalMonths = 12;
        final int[] loadedMonths = {0};

        for (int i = 0; i < 12; i++) {
            int month = (startMonth + i) % 12;
            if (month == 0) month = 12;
            int year = startYear + ((startMonth + i - 1) / 12);

            monthLabels.add("Tháng " + month);
            revenueData.add(0f);

            final int index = i;
            final int finalMonth = month;
            final int finalYear = year;

            db.collection("SALARIES")
                    .whereEqualTo("month", finalMonth)
                    .whereEqualTo("year", finalYear)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        double total = 0;
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Double salary = doc.getDouble("totalSalary");
                            if (salary != null) total += salary;
                        }
                        revenueData.set(index, (float) total);
                        loadedMonths[0]++;

                        if (loadedMonths[0] == totalMonths) {
                            updateLineChart(revenueData, monthLabels);
                        }
                    })
                    .addOnFailureListener(e -> {
                        loadedMonths[0]++;
                        if (loadedMonths[0] == totalMonths) {
                            updateLineChart(revenueData, monthLabels);
                        }
                    });
        }
    }

    private void updateLineChart(List<Float> revenueData, List<String> monthLabels) {
        List<Entry> entries = new ArrayList<>();
        for (int j = 0; j < revenueData.size(); j++) {
            entries.add(new Entry(j, revenueData.get(j)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Tiền ship");
        dataSet.setColor(getColor(R.color.primary));
        dataSet.setValueTextColor(getColor(R.color.black));
        dataSet.setValueTextSize(12f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return formatCurrency((int) value);
            }
        });

        LineData data = new LineData(dataSet);
        lineChartRevenue.setData(data);
        XAxis xAxis = lineChartRevenue.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < monthLabels.size()) {
                    return monthLabels.get(index);
                }
                return "";
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(monthLabels.size());

        lineChartRevenue.getDescription().setEnabled(false);
        lineChartRevenue.getAxisRight().setEnabled(false);
        lineChartRevenue.animateX(1000);
        lineChartRevenue.invalidate();
    }

    // ================== TOP 5 SHIPPER (THEO SALARIES) ==================
    private void loadTopShippers() {
        db.collection("SALARIES")
                .whereEqualTo("month", selectedMonth)
                .whereEqualTo("year", selectedYear)
                .get()
                .addOnSuccessListener(salarySnapshots -> {
                    topShipperList.clear();
                    List<TopShipperItem> tempList = new ArrayList<>();

                    for (QueryDocumentSnapshot salaryDoc : salarySnapshots) {
                        String shipperId = salaryDoc.getString("shipperID");
                        double totalSalary = salaryDoc.getDouble("totalSalary") != null ? salaryDoc.getDouble("totalSalary") : 0;
                        long totalOrders = salaryDoc.getLong("totalOrders") != null ? salaryDoc.getLong("totalOrders") : 0;

                        db.collection("USERS")
                                .document(shipperId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        User user = userDoc.toObject(User.class);
                                        user.setUserId(userDoc.getId());
                                        TopShipperItem item = new TopShipperItem();
                                        item.user = user;
                                        item.totalSalary = totalSalary;
                                        item.totalOrders = totalOrders;
                                        tempList.add(item);
                                    }
                                })
                                .addOnFailureListener(e -> {});
                    }

                    // Cập nhật sau khi đã load hết user
                    new android.os.Handler().postDelayed(() -> {
                        Collections.sort(tempList, (o1, o2) -> Double.compare(o2.totalSalary, o1.totalSalary));
                        int limit = Math.min(5, tempList.size());
                        topShipperList.clear();
                        topShipperList.addAll(tempList.subList(0, limit));
                        topShipperAdapter.notifyDataSetChanged();
                    }, 1500);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải top shipper", Toast.LENGTH_SHORT).show());
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

    // ================== MODEL TOP SHIPPER ==================
    private static class TopShipperItem {
        User user;
        double totalSalary;
        long totalOrders;
    }

    // ================== ADAPTER TOP SHIPPER ==================
    private class TopShipperAdapter extends RecyclerView.Adapter<TopShipperAdapter.ViewHolder> {

        private List<TopShipperItem> items;

        public TopShipperAdapter(List<TopShipperItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_top_shipper, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TopShipperItem item = items.get(position);
            User user = item.user;

            holder.txtRank.setText(String.valueOf(position + 1));
            holder.txtName.setText(user != null ? user.getFullName() : "Unknown");
            holder.txtCompleted.setText(item.totalOrders + " đơn hoàn thành");
            holder.txtIncome.setText(formatCurrency((int) item.totalSalary));
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtRank, txtName, txtCompleted, txtIncome;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                txtRank = itemView.findViewById(R.id.txtRank);
                txtName = itemView.findViewById(R.id.txtName);
                txtCompleted = itemView.findViewById(R.id.txtCompleted);
                txtIncome = itemView.findViewById(R.id.txtIncome);
            }
        }
    }
}