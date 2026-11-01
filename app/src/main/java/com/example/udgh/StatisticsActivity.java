package com.example.udgh;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.udgh.models.StatisticsData;
import com.example.udgh.models.User;
import com.example.udgh.viewmodels.StatisticsViewModel;
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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StatisticsActivity extends AppCompatActivity {
    private Spinner spMonth;
    private TextView txtTotalOrder, txtRevenue, txtCustomer, txtShipper;
    private PieChart pieChartStatus;
    private LineChart lineChartRevenue;
    private RecyclerView rvTopShipper;
    private SwipeRefreshLayout swipeRefreshLayout;
    private StatisticsViewModel viewModel;
    private TopShipperAdapter adapter;
    private int selectedMonth;
    private final int selectedYear = Calendar.getInstance().get(Calendar.YEAR);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_statistics);
        RoleAccess.requireRole(this, "ADMIN", this::initializeAdminScreen);
    }

    private void initializeAdminScreen() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        spMonth = findViewById(R.id.spMonth);
        txtTotalOrder = findViewById(R.id.txtTotalOrder);
        txtRevenue = findViewById(R.id.txtRevenue);
        txtCustomer = findViewById(R.id.txtCustomer);
        txtShipper = findViewById(R.id.txtShipper);
        pieChartStatus = findViewById(R.id.pieChartStatus);
        lineChartRevenue = findViewById(R.id.lineChartRevenue);
        rvTopShipper = findViewById(R.id.rvTopShipper);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);
        adapter = new TopShipperAdapter();
        rvTopShipper.setLayoutManager(new LinearLayoutManager(this));
        rvTopShipper.setAdapter(adapter);
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(this::loadData);

        setupMonthSpinner();
        observeViewModel();
    }

    private void setupMonthSpinner() {
        Calendar now = Calendar.getInstance();
        selectedMonth = now.get(Calendar.MONTH) + 1;
        List<String> months = new ArrayList<>();
        for (int i = 1; i <= 12; i++) months.add("Tháng " + i + "/" + selectedYear);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, months);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMonth.setAdapter(spinnerAdapter);
        spMonth.setSelection(selectedMonth - 1);
        spMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonth = position + 1;
                loadData();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, loading ->
                swipeRefreshLayout.setRefreshing(Boolean.TRUE.equals(loading)));
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty())
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        });
        viewModel.getData().observe(this, this::renderStatistics);
    }

    private void loadData() {
        viewModel.load(selectedMonth, selectedYear);
    }

    private void renderStatistics(StatisticsData data) {
        if (data == null) return;
        txtTotalOrder.setText(String.valueOf(data.getTotalOrders()));
        txtRevenue.setText(formatCurrency((int) data.getTotalRevenue()));
        txtCustomer.setText(String.valueOf(data.getCompletedOrders()));
        txtShipper.setText(String.valueOf(data.getShipperCount()));
        adapter.submit(data.getTopShippers());
        renderPieChart(data);
        renderLineChart(data);
    }

    private void renderPieChart(StatisticsData data) {
        String[] keys = {"PENDING", "DELIVERING", "COMPLETED", "CANCELLED"};
        String[] labels = {"Chờ nhận", "Đang giao", "Hoàn thành", "Đã hủy"};
        int[] colors = {ColorTemplate.rgb("#FF9800"), ColorTemplate.rgb("#2196F3"),
                ColorTemplate.rgb("#4CAF50"), ColorTemplate.rgb("#F44336")};
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> usedColors = new ArrayList<>();
        for (int i = 0; i < keys.length; i++) {
            int count = data.getStatusCounts().getOrDefault(keys[i], 0);
            if (count > 0) {
                entries.add(new PieEntry(count, labels[i]));
                usedColors.add(colors[i]);
            }
        }
        if (entries.isEmpty()) {
            pieChartStatus.clear();
            return;
        }
        int[] chartColors = new int[usedColors.size()];
        for (int i = 0; i < usedColors.size(); i++) chartColors[i] = usedColors.get(i);
        PieDataSet set = new PieDataSet(entries, "Trạng thái đơn hàng");
        set.setColors(chartColors);
        set.setValueTextSize(14f);
        set.setValueTextColor(getColor(R.color.black));
        set.setValueFormatter(new PercentFormatter(pieChartStatus));
        pieChartStatus.setData(new PieData(set));
        pieChartStatus.setUsePercentValues(true);
        pieChartStatus.getDescription().setEnabled(false);
        pieChartStatus.setEntryLabelTextSize(14f);
        pieChartStatus.setEntryLabelColor(getColor(R.color.black));
        pieChartStatus.animateY(700);
        pieChartStatus.invalidate();
    }

    private void renderLineChart(StatisticsData data) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < data.getMonthlyRevenue().size(); i++)
            entries.add(new Entry(i, data.getMonthlyRevenue().get(i)));

        LineDataSet set = new LineDataSet(entries, "Tiền ship");
        set.setColor(getColor(R.color.primary));
        set.setValueTextColor(getColor(R.color.black));
        set.setValueTextSize(12f);
        set.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                return formatCurrency((int) value);
            }
        });
        lineChartRevenue.setData(new LineData(set));
        XAxis axis = lineChartRevenue.getXAxis();
        axis.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                int index = (int) value;
                return index >= 0 && index < data.getMonthLabels().size()
                        ? data.getMonthLabels().get(index) : "";
            }
        });
        axis.setPosition(XAxis.XAxisPosition.BOTTOM);
        axis.setGranularity(1f);
        axis.setLabelCount(data.getMonthLabels().size());
        lineChartRevenue.getDescription().setEnabled(false);
        lineChartRevenue.getAxisRight().setEnabled(false);
        lineChartRevenue.animateX(700);
        lineChartRevenue.invalidate();
    }

    private String formatCurrency(int amount) {
        return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(amount);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class TopShipperAdapter extends RecyclerView.Adapter<TopShipperAdapter.Holder> {
        private final List<StatisticsData.TopShipper> items = new ArrayList<>();

        void submit(List<StatisticsData.TopShipper> values) {
            items.clear();
            if (values != null) items.addAll(values);
            notifyDataSetChanged();
        }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_top_shipper, parent, false);
            return new Holder(view);
        }

        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            StatisticsData.TopShipper item = items.get(position);
            User user = item.getUser();
            holder.txtRank.setText(String.valueOf(position + 1));
            holder.txtName.setText(user != null && user.getFullName() != null
                    ? user.getFullName() : "Không rõ");
            holder.txtCompleted.setText(item.getTotalOrders() + " đơn hoàn thành");
            holder.txtIncome.setText(formatCurrency((int) item.getTotalSalary()));
        }

        @Override public int getItemCount() { return items.size(); }

        class Holder extends RecyclerView.ViewHolder {
            final TextView txtRank, txtName, txtCompleted, txtIncome;
            Holder(@NonNull View itemView) {
                super(itemView);
                txtRank = itemView.findViewById(R.id.txtRank);
                txtName = itemView.findViewById(R.id.txtName);
                txtCompleted = itemView.findViewById(R.id.txtCompleted);
                txtIncome = itemView.findViewById(R.id.txtIncome);
            }
        }
    }
}
