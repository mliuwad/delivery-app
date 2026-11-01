package com.example.udgh.repositories;

import com.example.udgh.models.StatisticsData;
import com.example.udgh.models.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class StatisticsRepository {
    public interface Callback {
        void onSuccess(StatisticsData data);
        void onError(Exception error);
    }

    private final FirebaseFirestore db;

    public StatisticsRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public void load(int selectedMonth, int selectedYear, Callback callback) {
        Calendar start = Calendar.getInstance();
        start.set(selectedYear, selectedMonth - 1, 1, 0, 0, 0);
        start.set(Calendar.MILLISECOND, 0);
        Timestamp startTime = new Timestamp(start.getTime());

        Calendar end = (Calendar) start.clone();
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);
        Timestamp endTime = new Timestamp(end.getTime());

        Task<QuerySnapshot> ordersTask = db.collection("ORDERS")
                .whereGreaterThanOrEqualTo("createdAt", startTime)
                .whereLessThanOrEqualTo("createdAt", endTime)
                .get();
        List<Task<QuerySnapshot>> salaryTasks = new ArrayList<>();
        List<String> monthLabels = new ArrayList<>();
        int selectedIndex = -1;

        Calendar current = Calendar.getInstance();
        current.set(Calendar.DAY_OF_MONTH, 1);
        current.add(Calendar.MONTH, -11);
        for (int i = 0; i < 12; i++) {
            int month = current.get(Calendar.MONTH) + 1;
            int year = current.get(Calendar.YEAR);
            monthLabels.add("Tháng " + month);
            if (month == selectedMonth && year == selectedYear) selectedIndex = i;
            salaryTasks.add(db.collection("SALARIES")
                    .whereEqualTo("month", month)
                    .whereEqualTo("year", year).get());
            current.add(Calendar.MONTH, 1);
        }

        final int selectedSalaryIndex = selectedIndex;
        Tasks.whenAllComplete(ordersTask, Tasks.whenAllComplete(salaryTasks))
                .addOnSuccessListener(ignored -> {
                    if (!ordersTask.isSuccessful()) {
                        callback.onError(ordersTask.getException() != null
                                ? ordersTask.getException() : new Exception("Không thể tải đơn hàng"));
                        return;
                    }

                    for (Task<QuerySnapshot> task : salaryTasks) {
                        if (!task.isSuccessful()) {
                            callback.onError(task.getException() != null
                                    ? task.getException() : new Exception("Không thể tải dữ liệu lương"));
                            return;
                        }
                    }

                    StatisticsData data = new StatisticsData();
                    data.getStatusCounts().put("PENDING", 0);
                    data.getStatusCounts().put("WAITING", 0);
                    data.getStatusCounts().put("DELIVERING", 0);
                    data.getStatusCounts().put("COMPLETED", 0);
                    data.getStatusCounts().put("CANCELLED", 0);

                    for (QueryDocumentSnapshot doc : ordersTask.getResult()) {
                        Timestamp createdAt = doc.getTimestamp("createdAt");
                        if (createdAt == null || createdAt.compareTo(startTime) < 0
                                || createdAt.compareTo(endTime) > 0) continue;

                        data.setTotalOrders(data.getTotalOrders() + 1);
                        String status = doc.getString("status");
                        if (status != null && data.getStatusCounts().containsKey(status)) {
                            data.getStatusCounts().put(status,
                                    data.getStatusCounts().get(status) + 1);
                        }
                    }

                    List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();
                    List<Double> salaries = new ArrayList<>();
                    List<Long> orderCounts = new ArrayList<>();

                    for (int i = 0; i < salaryTasks.size(); i++) {
                        QuerySnapshot snapshot = salaryTasks.get(i).getResult();
                        double total = 0;
                        for (QueryDocumentSnapshot doc : snapshot) {
                            Double salary = doc.getDouble("totalSalary");
                            if (salary != null) total += salary;
                        }
                        data.getMonthLabels().add(monthLabels.get(i));
                        data.getMonthlyRevenue().add((float) total);

                        if (i == selectedSalaryIndex) {
                            data.setTotalRevenue(total);
                            data.setShipperCount(snapshot.size());
                            long completed = 0;
                            for (QueryDocumentSnapshot doc : snapshot) {
                                Long count = doc.getLong("totalOrders");
                                if (count != null) completed += count;

                                String shipperId = doc.getString("shipperID");
                                if (shipperId != null && !shipperId.trim().isEmpty()) {
                                    userTasks.add(db.collection("USERS").document(shipperId).get());
                                    salaries.add(doc.getDouble("totalSalary") == null
                                            ? 0d : doc.getDouble("totalSalary"));
                                    orderCounts.add(count == null ? 0L : count);
                                }
                            }
                            data.setCompletedOrders(completed);
                        }
                    }

                    if (userTasks.isEmpty()) {
                        callback.onSuccess(data);
                        return;
                    }

                    Tasks.whenAllComplete(userTasks).addOnSuccessListener(userResults -> {
                        for (int i = 0; i < userTasks.size(); i++) {
                            Task<DocumentSnapshot> task = userTasks.get(i);
                            if (!task.isSuccessful() || task.getResult() == null
                                    || !task.getResult().exists()) continue;
                            DocumentSnapshot doc = task.getResult();
                            User user = doc.toObject(User.class);
                            if (user == null) continue;
                            user.setUserId(doc.getId());
                            data.getTopShippers().add(new StatisticsData.TopShipper(
                                    user, salaries.get(i), orderCounts.get(i)));
                        }
                        Collections.sort(data.getTopShippers(),
                                (a, b) -> Double.compare(b.getTotalSalary(), a.getTotalSalary()));
                        if (data.getTopShippers().size() > 5) {
                            data.getTopShippers().subList(5, data.getTopShippers().size()).clear();
                        }
                        callback.onSuccess(data);
                    }).addOnFailureListener(callback::onError);
                }).addOnFailureListener(callback::onError);
    }
}
