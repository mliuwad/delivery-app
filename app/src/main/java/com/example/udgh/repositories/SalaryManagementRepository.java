package com.example.udgh.repositories;

import com.example.udgh.models.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SalaryManagementRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(Exception error);
    }

    public static class SalaryRow {
        public final User user;
        public final double totalSalary;
        public final long totalOrders;
        public final String paymentStatus;

        public SalaryRow(User user, double totalSalary, long totalOrders, String paymentStatus) {
            this.user = user;
            this.totalSalary = totalSalary;
            this.totalOrders = totalOrders;
            this.paymentStatus = paymentStatus;
        }
    }

    public void load(int month, int year, Callback<List<SalaryRow>> callback) {
        db.collection("SALARIES")
                .whereEqualTo("month", month)
                .whereEqualTo("year", year)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }

                    List<SalaryRow> rows = Collections.synchronizedList(new ArrayList<>());
                    int total = snapshots.size();
                    int[] completed = {0};

                    for (QueryDocumentSnapshot salaryDoc : snapshots) {
                        String shipperId = salaryDoc.getString("shipperID");
                        Double salaryValue = salaryDoc.getDouble("totalSalary");
                        Long orderValue = salaryDoc.getLong("totalOrders");
                        String paymentStatus = salaryDoc.getString("paymentStatus");
                        double totalSalary = salaryValue != null ? salaryValue : 0d;
                        long totalOrders = orderValue != null ? orderValue : 0L;
                        String finalStatus = paymentStatus != null ? paymentStatus : "UNPAID";

                        if (shipperId == null || shipperId.trim().isEmpty()) {
                            User missing = new User();
                            missing.setFullName("Shipper không xác định");
                            rows.add(new SalaryRow(missing, totalSalary, totalOrders, finalStatus));
                            finishOne(rows, total, completed, callback);
                            continue;
                        }

                        db.collection("USERS").document(shipperId).get()
                                .addOnSuccessListener(userDoc -> {
                                    User user = userDoc.exists() ? userDoc.toObject(User.class) : null;
                                    if (user == null) {
                                        user = new User();
                                        user.setFullName("Shipper đã xóa");
                                    }
                                    user.setUserId(shipperId);
                                    rows.add(new SalaryRow(user, totalSalary, totalOrders, finalStatus));
                                    finishOne(rows, total, completed, callback);
                                })
                                .addOnFailureListener(error -> {
                                    User user = new User();
                                    user.setFullName("Không tải được thông tin");
                                    user.setUserId(shipperId);
                                    rows.add(new SalaryRow(user, totalSalary, totalOrders, finalStatus));
                                    finishOne(rows, total, completed, callback);
                                });
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    private void finishOne(List<SalaryRow> rows, int total, int[] completed,
                           Callback<List<SalaryRow>> callback) {
        boolean finished;
        synchronized (completed) {
            completed[0]++;
            finished = completed[0] == total;
        }
        if (finished) {
            List<SalaryRow> result = new ArrayList<>(rows);
            result.sort((a, b) -> Double.compare(b.totalSalary, a.totalSalary));
            callback.onSuccess(result);
        }
    }
}
