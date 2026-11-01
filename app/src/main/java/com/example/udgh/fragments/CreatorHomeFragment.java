package com.example.udgh.fragments;

import com.example.udgh.activities.CreateOrderActivity;
import com.example.udgh.R;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.udgh.repositories.DashboardRepository;
import com.example.udgh.viewmodels.DashboardViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.example.udgh.viewmodels.SessionViewModel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class CreatorHomeFragment extends Fragment {

    private String userId;
    private DashboardViewModel dashboardViewModel;
    private TextView txtCreatorName, txtTotalOrders, txtTotalProductValue;
    private TextView txtWaiting, txtDelivering, txtCompleted;
    private LineChart chartRevenue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_creator_home, container, false);
        userId = new ViewModelProvider(this).get(SessionViewModel.class).getCurrentUserId();

        txtCreatorName = view.findViewById(R.id.txtCreatorName);
        txtTotalOrders = view.findViewById(R.id.txtTotalOrders);
        txtTotalProductValue = view.findViewById(R.id.txtTotalProductValue);
        txtWaiting = view.findViewById(R.id.txtWaiting);
        txtDelivering = view.findViewById(R.id.txtDelivering);
        txtCompleted = view.findViewById(R.id.txtCompleted);
        chartRevenue = view.findViewById(R.id.chartCreatorRevenue);

        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        setupRevenueChart();
        observeDashboard();

        view.findViewById(R.id.btnCreateOrder).setOnClickListener(v ->
                startActivity(new android.content.Intent(requireContext(), CreateOrderActivity.class)));

        loadDashboard();
        return view;
    }

    private void observeDashboard() {
        dashboardViewModel.getCreatorDashboard().observe(getViewLifecycleOwner(), data -> {
            if (data == null) return;
            txtCreatorName.setText(TextUtils.isEmpty(data.creatorName)
                    ? "Cửa hàng của bạn" : data.creatorName);
            txtTotalOrders.setText(String.valueOf(data.totalOrders));
            txtTotalProductValue.setText(formatCurrency(data.completedProductRevenue));
            txtWaiting.setText(String.valueOf(data.waiting));
            txtDelivering.setText(String.valueOf(data.delivering));
            txtCompleted.setText(String.valueOf(data.completed));
            showRevenueChart(data.monthlyRevenue);
        });

        dashboardViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (!TextUtils.isEmpty(message) && isAdded()) {
                Toast.makeText(requireContext(), "Không thể tải thống kê: " + message,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDashboard() {
        if (dashboardViewModel != null && !TextUtils.isEmpty(userId)) {
            dashboardViewModel.loadCreatorDashboard(userId);
        }
    }

    private void setupRevenueChart() {
        chartRevenue.getDescription().setEnabled(false);
        chartRevenue.getAxisRight().setEnabled(false);
        chartRevenue.getLegend().setEnabled(false);
        chartRevenue.setNoDataText("Chưa có dữ liệu doanh thu");
        XAxis xAxis = chartRevenue.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        Calendar firstMonth = Calendar.getInstance();
        firstMonth.set(Calendar.DAY_OF_MONTH, 1);
        firstMonth.add(Calendar.MONTH, -11);
        final Calendar labelCalendar = (Calendar) firstMonth.clone();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                Calendar monthCalendar = (Calendar) labelCalendar.clone();
                monthCalendar.add(Calendar.MONTH, (int) value);
                return (monthCalendar.get(Calendar.MONTH) + 1) + "/"
                        + String.valueOf(monthCalendar.get(Calendar.YEAR)).substring(2);
            }
        });
    }

    private void showRevenueChart(double[] values) {
        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            entries.add(new Entry(i, (float) values[i]));
        }
        LineDataSet dataSet = new LineDataSet(entries, "Doanh thu tiền hàng");
        dataSet.setColor(requireContext().getColor(R.color.primary));
        dataSet.setCircleColor(requireContext().getColor(R.color.primary));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        chartRevenue.setData(new LineData(dataSet));
        chartRevenue.invalidate();
    }

    private String formatCurrency(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return format.format(amount);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboard();
    }
}
