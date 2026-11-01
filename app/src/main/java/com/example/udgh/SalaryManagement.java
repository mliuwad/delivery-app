package com.example.udgh;

import android.os.Bundle;
import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.udgh.repositories.SalaryManagementRepository;
import com.example.udgh.adapters.SalaryManagementAdapter;
import com.example.udgh.viewmodels.SalaryManagementViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SalaryManagement extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private Spinner spMonth;
    private TextView txtTotalSalary, txtTotalUnpaid;
    private EditText edtSearch;
    private ChipGroup chipGroupFilter;
    private RecyclerView rvSalary;
    private SwipeRefreshLayout swipeRefreshLayout;

    private SalaryManagementViewModel viewModel;
    private List<SalaryManagementRepository.SalaryRow> salaryList;
    private SalaryManagementAdapter adapter;
    private List<SalaryManagementRepository.SalaryRow> salaryListFull;

    private int selectedMonth;
    private int selectedYear;
    private String selectedFilter = "ALL";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_salary_management);

        RoleAccess.requireRole(this, "ADMIN", this::initializeAdminScreen);
    }

    private void initializeAdminScreen() {

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        toolbar = findViewById(R.id.toolbar);
        spMonth = findViewById(R.id.spMonth);
        txtTotalSalary = findViewById(R.id.txtTotalSalary);
        txtTotalUnpaid = findViewById(R.id.txtTotalUnpaid);
        edtSearch = findViewById(R.id.edtSearch);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        rvSalary = findViewById(R.id.rvSalary);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(SalaryManagementViewModel.class);
        salaryList = new ArrayList<>();
        salaryListFull = new ArrayList<>();

        setupMonthSpinner();
        adapter = new SalaryManagementAdapter(salaryList, (row) -> {
            Intent intent = new Intent(SalaryManagement.this, SalaryDetailActivity.class);
            intent.putExtra("shipperId", row.user.getUserId());
            intent.putExtra("month", selectedMonth);
            intent.putExtra("year", selectedYear);
            startActivity(intent);
        });
        observeViewModel();
        rvSalary.setLayoutManager(new LinearLayoutManager(this));
        rvSalary.setAdapter(adapter);

        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(this::loadData);

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterData();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedFilter = "ALL";
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chipAll) {
                    selectedFilter = "ALL";
                } else if (id == R.id.chipPaid) {
                    selectedFilter = "PAID";
                } else if (id == R.id.chipUnpaid) {
                    selectedFilter = "UNPAID";
                }
            }
            filterData();
        });

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
                loadData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadData() {
        viewModel.load(selectedMonth, selectedYear);
    }

    private void observeViewModel() {
        viewModel.getSalaries().observe(this, rows -> {
            salaryListFull.clear();
            if (rows != null) salaryListFull.addAll(rows);
            updateUI();
        });

        viewModel.getLoading().observe(this, loading -> {
            swipeRefreshLayout.setRefreshing(Boolean.TRUE.equals(loading));
        });

        viewModel.getError().observe(this, error -> {
            if (!TextUtils.isEmpty(error)) {
                Toast.makeText(this, "Lỗi tải dữ liệu lương: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        double totalSalary = 0;
        int totalUnpaid = 0;
        for (SalaryManagementRepository.SalaryRow item : salaryListFull) {
            totalSalary += item.totalSalary;
            if ("UNPAID".equals(item.paymentStatus)) {
                totalUnpaid++;
            }
        }

        txtTotalSalary.setText(formatCurrency((int) totalSalary));
        txtTotalUnpaid.setText(String.valueOf(totalUnpaid));

        filterData();
    }

    private void filterData() {
        String keyword = edtSearch.getText().toString().toLowerCase().trim();
        List<SalaryManagementRepository.SalaryRow> filtered = new ArrayList<>();

        for (SalaryManagementRepository.SalaryRow item : salaryListFull) {
            boolean matchSearch = true;
            if (!TextUtils.isEmpty(keyword)) {
                String name = item.user.getFullName() != null ? item.user.getFullName().toLowerCase() : "";
                matchSearch = name.contains(keyword);
            }

            boolean matchPayment = true;
            if ("PAID".equals(selectedFilter)) {
                matchPayment = "PAID".equals(item.paymentStatus);
            } else if ("UNPAID".equals(selectedFilter)) {
                matchPayment = "UNPAID".equals(item.paymentStatus);
            }

            if (matchSearch && matchPayment) {
                filtered.add(item);
            }
        }

        salaryList.clear();
        salaryList.addAll(filtered);
        adapter.notifyDataSetChanged();
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

}
