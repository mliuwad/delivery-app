package com.example.udgh.fragments;

import com.example.udgh.activities.CreateOrderActivity;
import com.example.udgh.R;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.udgh.activities.OrderManagementActivity;
import com.example.udgh.adapters.SalaryOrderAdapter;
import com.example.udgh.models.Order;
import com.example.udgh.viewmodels.SalaryShipperViewModel;
import com.example.udgh.viewmodels.SessionViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

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
    private String shipperId;
    private SalaryShipperViewModel viewModel;
    private SalaryOrderAdapter adapter;
    private final List<Order> orderList = new ArrayList<>();
    private int currentMonth;
    private int currentYear;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_salary_statistics, container, false);

        SharedPreferences preferences = requireActivity()
                .getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE);
        shipperId = preferences.getString("userId", "");
        if (TextUtils.isEmpty(shipperId)) {
            shipperId = new ViewModelProvider(this).get(SessionViewModel.class).getCurrentUserId();
        }

        spMonth = view.findViewById(R.id.spMonth);
        txtSalaryPeriod = view.findViewById(R.id.txtSalaryPeriod);
        txtSalary = view.findViewById(R.id.txtSalary);
        txtCompletedOrder = view.findViewById(R.id.txtCompletedOrder);
        txtSalaryStatus = view.findViewById(R.id.txtSalaryStatus);
        txtViewAll = view.findViewById(R.id.txtViewAll);
        rvCompletedOrder = view.findViewById(R.id.rvCompletedOrder);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        barChart = view.findViewById(R.id.barChart);

        viewModel = new ViewModelProvider(this).get(SalaryShipperViewModel.class);
        adapter = new SalaryOrderAdapter(orderList, order -> {
            Intent intent = new Intent(requireContext(), CreateOrderActivity.class);
            intent.putExtra("orderId", order.getOrderId());
            startActivity(intent);
        });
        rvCompletedOrder.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCompletedOrder.setAdapter(adapter);

        observeViewModel();
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(this::loadData);
        txtViewAll.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), OrderManagementActivity.class);
            intent.putExtra("filter", "COMPLETED");
            startActivity(intent);
        });

        setupSpinner();
        return view;
    }

    private void observeViewModel() {
        viewModel.getSalary().observe(getViewLifecycleOwner(), data -> {
            if (data == null) return;
            txtSalary.setText(formatCurrency(data.totalSalary));
            txtCompletedOrder.setText(String.valueOf(data.totalOrders));

            if ("PAID".equals(data.paymentStatus)) {
                txtSalaryStatus.setText("ĐÃ NHẬN");
                txtSalaryStatus.setTextColor(requireContext().getColor(R.color.success));
            } else {
                boolean hasSalary = data.totalOrders > 0;
                txtSalaryStatus.setText(hasSalary ? "CHƯA NHẬN" : "CHƯA CÓ");
                txtSalaryStatus.setTextColor(requireContext().getColor(
                        hasSalary ? R.color.warning : R.color.gray));
            }

            orderList.clear();
            orderList.addAll(data.orders);
            adapter.notifyDataSetChanged();
            drawWeeklyChart(data.orders);
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                swipeRefreshLayout.setRefreshing(Boolean.TRUE.equals(loading)));

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (!TextUtils.isEmpty(error)) {
                Toast.makeText(requireContext(), "Lỗi tải lương: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSpinner() {
        String[] months = new String[12];
        for (int i = 0; i < 12; i++) months[i] = "Tháng " + (i + 1);
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.item_spinner_month, months);
        monthAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spMonth.setAdapter(monthAdapter);

        Calendar calendar = Calendar.getInstance();
        currentMonth = calendar.get(Calendar.MONTH) + 1;
        currentYear = calendar.get(Calendar.YEAR);
        spMonth.setSelection(currentMonth - 1);
        spMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentMonth = position + 1;
                loadData();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void loadData() {
        if (TextUtils.isEmpty(shipperId)) {
            Toast.makeText(requireContext(), "Không xác định được tài khoản Shipper", Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        Calendar start = Calendar.getInstance();
        start.clear();
        start.set(currentYear, currentMonth - 1, 1);
        Calendar end = (Calendar) start.clone();
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        txtSalaryPeriod.setText("Kỳ lương: " + dateFormat.format(start.getTime())
                + " - " + dateFormat.format(end.getTime()));

        viewModel.load(shipperId, currentMonth, currentYear);
    }

    private void drawWeeklyChart(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            barChart.clear();
            barChart.setNoDataText("Không có dữ liệu");
            barChart.invalidate();
            return;
        }

        Map<Integer, Double> weeklySalary = new HashMap<>();
        for (Order order : orders) {
            if (order.getDeliveredAt() == null) continue;
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(order.getDeliveredAt().toDate());
            int week = (calendar.get(Calendar.DAY_OF_MONTH) - 1) / 7 + 1;
            weeklySalary.put(week, weeklySalary.getOrDefault(week, 0d) + order.getDeliveryFee());
        }

        int weekCount = 4;
        for (Integer week : weeklySalary.keySet()) weekCount = Math.max(weekCount, week);
        weekCount = Math.min(weekCount, 5);

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int week = 1; week <= weekCount; week++) {
            entries.add(new BarEntry(week - 1, weeklySalary.getOrDefault(week, 0d).floatValue()));
            labels.add("Tuần " + week);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Thu nhập (đ)");
        dataSet.setColor(requireContext().getColor(R.color.primary));
        dataSet.setValueTextColor(android.graphics.Color.BLACK);
        dataSet.setValueTextSize(12f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                return formatCurrency(value);
            }
        });

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        barChart.setData(data);
        barChart.setFitBars(true);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(labels.size());
        barChart.getDescription().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.animateY(600);
        barChart.invalidate();
    }

    private String formatCurrency(double amount) {
        return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(amount);
    }
}
