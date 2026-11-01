package com.example.udgh;

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

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class CreatorHomeFragment extends Fragment {
    private FirebaseFirestore db;
    private String userId;
    private TextView txtCreatorName, txtTotalOrders, txtTotalProductValue;
    private TextView txtWaiting, txtDelivering, txtCompleted;
    private LineChart chartRevenue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_creator_home, container, false);
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        txtCreatorName = view.findViewById(R.id.txtCreatorName);
        txtTotalOrders = view.findViewById(R.id.txtTotalOrders);
        txtTotalProductValue = view.findViewById(R.id.txtTotalProductValue);
        txtWaiting = view.findViewById(R.id.txtWaiting);
        txtDelivering = view.findViewById(R.id.txtDelivering);
        txtCompleted = view.findViewById(R.id.txtCompleted);
        chartRevenue = view.findViewById(R.id.chartCreatorRevenue);
        setupRevenueChart();

        view.findViewById(R.id.btnCreateOrder).setOnClickListener(v ->
                startActivity(new android.content.Intent(requireContext(), CreateOrderActivity.class)));
        view.findViewById(R.id.btnViewOrders).setOnClickListener(v -> {
            if (getActivity() instanceof HomeCreatorActivity) {
                ((HomeCreatorActivity) getActivity()).openOrdersTab();
            }
        });

        loadCreatorInfo();
        loadStatistics();
        return view;
    }

    private void loadCreatorInfo() {
        if (TextUtils.isEmpty(userId)) return;
        db.collection("USERS").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (isAdded() && doc.exists()) {
                        String name = doc.getString("fullName");
                        txtCreatorName.setText(TextUtils.isEmpty(name) ? "Cửa hàng của bạn" : name);
                    }
                });
    }

    private void setupRevenueChart() {
        chartRevenue.getDescription().setEnabled(false);
        chartRevenue.getAxisRight().setEnabled(false);
        chartRevenue.getLegend().setEnabled(false);
        chartRevenue.setNoDataText("Chưa có dữ liệu doanh thu");
        XAxis xAxis = chartRevenue.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                int month = ((int) value % 12) + 1;
                return "T" + month;
            }
        });
    }

    private void loadStatistics() {
        if (TextUtils.isEmpty(userId)) return;
        db.collection("ORDERS").whereEqualTo("creatorId", userId).get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;
                    int waiting = 0, delivering = 0, completed = 0;
                    double revenue = 0;
                    Calendar now = Calendar.getInstance();
                    int currentMonth = now.get(Calendar.MONTH);
                    int currentYear = now.get(Calendar.YEAR);
                    double[] monthlyRevenue = new double[12];

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String status = doc.getString("status");
                        if ("PENDING".equals(status) || "WAITING".equals(status)) waiting++;
                        else if ("DELIVERING".equals(status)) delivering++;
                        else if ("COMPLETED".equals(status)) completed++;

                        Number amount = (Number) doc.get("totalProductValue");
                        double productValue = amount == null ? 0 : amount.doubleValue();
                        revenue += productValue;

                        Timestamp createdAt = doc.getTimestamp("createdAt");
                        if (createdAt != null) {
                            Calendar orderDate = Calendar.getInstance();
                            orderDate.setTime(createdAt.toDate());
                            int monthOffset = (currentYear - orderDate.get(Calendar.YEAR)) * 12
                                    + currentMonth - orderDate.get(Calendar.MONTH);
                            if (monthOffset >= 0 && monthOffset < 12) {
                                int chartIndex = 11 - monthOffset;
                                monthlyRevenue[chartIndex] += productValue;
                            }
                        }
                    }

                    txtTotalOrders.setText(String.valueOf(snapshot.size()));
                    txtTotalProductValue.setText(formatCurrency(revenue));
                    txtWaiting.setText(String.valueOf(waiting));
                    txtDelivering.setText(String.valueOf(delivering));
                    txtCompleted.setText(String.valueOf(completed));
                    showRevenueChart(monthlyRevenue);
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(requireContext(),
                            "Không thể tải thống kê: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showRevenueChart(double[] values) {
        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < values.length; i++) entries.add(new Entry(i, (float) values[i]));
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

    @Override public void onResume() {
        super.onResume();
        loadStatistics();
    }
}