package com.example.udgh;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
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

import com.bumptech.glide.Glide;
import com.example.udgh.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
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

public class SalaryManagement extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private Spinner spMonth;
    private TextView txtTotalSalary, txtTotalUnpaid;
    private EditText edtSearch;
    private ChipGroup chipGroupFilter;
    private RecyclerView rvSalary;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseFirestore db;
    private List<SalaryItem> salaryList;
    private SalaryAdapter adapter;
    private List<SalaryItem> salaryListFull;

    private int selectedMonth;
    private int selectedYear;
    private String selectedFilter = "ALL";

    // Thêm flag để tránh load đồng thời
    private boolean isLoading = false;

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

        db = FirebaseFirestore.getInstance();

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

        setupMonthSpinner();

        salaryList = new ArrayList<>();
        salaryListFull = new ArrayList<>();
        adapter = new SalaryAdapter(salaryList);
        rvSalary.setLayoutManager(new LinearLayoutManager(this));
        rvSalary.setAdapter(adapter);

        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (!isLoading) {
                loadData();
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

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
                if (!isLoading) {
                    loadData();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ================== LOAD DỮ LIỆU (CÓ XỬ LÝ TRÙNG LẶP) ==================
    private void loadData() {
        // Ngăn gọi đồng thời
        if (isLoading) {
            return;
        }
        isLoading = true;
        swipeRefreshLayout.setRefreshing(true);

        // Dùng Map để gom nhóm theo shipperId, tránh trùng lặp
        Map<String, SalaryItem> salaryMap = new HashMap<>();

        db.collection("SALARIES")
                .whereEqualTo("month", selectedMonth)
                .whereEqualTo("year", selectedYear)
                .get()
                .addOnSuccessListener(salarySnapshots -> {
                    if (salarySnapshots.isEmpty()) {
                        salaryList.clear();
                        salaryListFull.clear();
                        updateUI();
                        swipeRefreshLayout.setRefreshing(false);
                        isLoading = false;
                        Toast.makeText(this, "Không có dữ liệu lương cho tháng này", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    final int total = salarySnapshots.size();
                    final int[] remaining = {total};

                    for (QueryDocumentSnapshot salaryDoc : salarySnapshots) {
                        String shipperId = salaryDoc.getString("shipperID");
                        double totalSalary = salaryDoc.getDouble("totalSalary") != null ? salaryDoc.getDouble("totalSalary") : 0;
                        long totalOrders = salaryDoc.getLong("totalOrders") != null ? salaryDoc.getLong("totalOrders") : 0;
                        String paymentStatus = salaryDoc.getString("paymentStatus") != null ? salaryDoc.getString("paymentStatus") : "UNPAID";

                        // Nếu đã có trong map thì bỏ qua (tránh trùng)
                        if (salaryMap.containsKey(shipperId)) {
                            remaining[0]--;
                            if (remaining[0] == 0) {
                                updateListFromMap(salaryMap);
                            }
                            continue;
                        }

                        db.collection("USERS")
                                .document(shipperId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    User user;
                                    if (userDoc.exists()) {
                                        user = userDoc.toObject(User.class);
                                        if (user != null) {
                                            user.setUserId(userDoc.getId());
                                        }
                                    } else {
                                        user = new User();
                                        user.setFullName("Shipper đã xóa");
                                        user.setUserId(shipperId);
                                    }

                                    SalaryItem item = new SalaryItem();
                                    item.user = user;
                                    item.totalSalary = totalSalary;
                                    item.totalOrders = totalOrders;
                                    item.paymentStatus = paymentStatus;
                                    salaryMap.put(shipperId, item);

                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        updateListFromMap(salaryMap);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    User user = new User();
                                    user.setFullName("Không tìm thấy");
                                    user.setUserId(shipperId);
                                    SalaryItem item = new SalaryItem();
                                    item.user = user;
                                    item.totalSalary = totalSalary;
                                    item.totalOrders = totalOrders;
                                    item.paymentStatus = paymentStatus;
                                    salaryMap.put(shipperId, item);

                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        updateListFromMap(salaryMap);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải dữ liệu lương: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                    isLoading = false;
                });
    }

    // Hàm cập nhật danh sách từ Map
    private void updateListFromMap(Map<String, SalaryItem> salaryMap) {
        salaryList.clear();
        salaryListFull.clear();
        salaryListFull.addAll(salaryMap.values());
        // Sắp xếp theo totalSalary giảm dần
        Collections.sort(salaryListFull, (o1, o2) -> Double.compare(o2.totalSalary, o1.totalSalary));
        salaryList.addAll(salaryListFull);
        updateUI();
        swipeRefreshLayout.setRefreshing(false);
        isLoading = false;
    }

    private void updateUI() {
        double totalSalary = 0;
        int totalUnpaid = 0;
        for (SalaryItem item : salaryListFull) {
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
        List<SalaryItem> filtered = new ArrayList<>();

        for (SalaryItem item : salaryListFull) {
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

    // ================== MODEL ==================
    private static class SalaryItem {
        User user;
        double totalSalary;
        long totalOrders;
        String paymentStatus;
    }

    // ================== ADAPTER ==================
    private class SalaryAdapter extends RecyclerView.Adapter<SalaryAdapter.ViewHolder> {

        private List<SalaryItem> items;

        public SalaryAdapter(List<SalaryItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_salary_shipper, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SalaryItem item = items.get(position);
            User user = item.user;

            holder.txtName.setText(user.getFullName() != null ? user.getFullName() : "Không tên");
            holder.txtCompleted.setText("Đơn hoàn thành: " + item.totalOrders);
            holder.txtSalary.setText(formatCurrency((int) item.totalSalary));

            if ("PAID".equals(item.paymentStatus)) {
                holder.txtPaymentStatus.setText("Đã thanh toán");
                holder.txtPaymentStatus.setTextColor(getColor(R.color.success));
            } else {
                holder.txtPaymentStatus.setText("Chưa thanh toán");
                holder.txtPaymentStatus.setTextColor(getColor(R.color.warning));
            }

            // Load avatar
            String avatarUrl = user.getAvatarUrl();
            if (!TextUtils.isEmpty(avatarUrl)) {
                Glide.with(SalaryManagement.this)
                        .load(avatarUrl)
                        .circleCrop()
                        .placeholder(R.drawable.delivery_man)
                        .error(R.drawable.delivery_man)
                        .into(holder.imgAvatar);
            } else {
                holder.imgAvatar.setImageResource(R.drawable.delivery_man);
            }

            holder.btnDetail.setOnClickListener(v -> {
                Intent intent = new Intent(SalaryManagement.this, SalaryDetailActivity.class);
                intent.putExtra("shipperId", user.getUserId());
                intent.putExtra("month", selectedMonth);
                intent.putExtra("year", selectedYear);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgAvatar;
            TextView txtName, txtCompleted, txtSalary, txtPaymentStatus;
            MaterialButton btnDetail;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                imgAvatar = itemView.findViewById(R.id.imgAvatar);
                txtName = itemView.findViewById(R.id.txtName);
                txtCompleted = itemView.findViewById(R.id.txtCompleted);
                txtSalary = itemView.findViewById(R.id.txtSalary);
                txtPaymentStatus = itemView.findViewById(R.id.txtPaymentStatus);
                btnDetail = itemView.findViewById(R.id.btnDetail);
            }
        }
    }
}