package com.example.udgh.repositories;

import com.example.udgh.models.Order;
import com.example.udgh.models.OrderDetail;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SalaryShipperRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    public interface Callback {
        void onSuccess(SalaryData data);
        void onError(Exception error);
    }
    public static class SalaryData {
        public final double totalSalary;
        public final long totalOrders;
        public final String paymentStatus;
        public final List<Order> orders;
        public SalaryData(double totalSalary, long totalOrders, String paymentStatus, List<Order> orders) {
            this.totalSalary = totalSalary;
            this.totalOrders = totalOrders;
            this.paymentStatus = paymentStatus;
            this.orders = orders;
        }
    }
    public void load(String shipperId, int month, int year, Callback callback) {
        if (shipperId == null || shipperId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Không xác định được tài khoản Shipper"));
            return;
        }
        db.collection("SALARIES").whereEqualTo("shipperID", shipperId)
                .whereEqualTo("month", month).whereEqualTo("year", year).get()
                .addOnSuccessListener(salaries -> {
                    double amount = 0d;
                    long count = 0L;
                    String status = "UNPAID";
                    if (!salaries.isEmpty()) {
                        QueryDocumentSnapshot salary = (QueryDocumentSnapshot) salaries.getDocuments().get(0);
                        Double savedAmount = salary.getDouble("totalSalary");
                        Long savedCount = salary.getLong("totalOrders");
                        String savedStatus = salary.getString("paymentStatus");
                        amount = savedAmount != null ? savedAmount : 0d;
                        count = savedCount != null ? savedCount : 0L;
                        status = savedStatus != null ? savedStatus : "UNPAID";
                    }
                    loadOrders(shipperId, month, year, amount, count, status, callback);
                }).addOnFailureListener(callback::onError);
    }
    private void loadOrders(String shipperId, int month, int year, double amount, long count,
                            String status, Callback callback) {
        Calendar start = Calendar.getInstance();
        start.clear();
        start.set(year, month - 1, 1, 0, 0, 0);
        Timestamp from = new Timestamp(start.getTime());
        Calendar end = Calendar.getInstance();
        end.clear();
        end.set(year, month, 1, 0, 0, 0);
        Timestamp until = new Timestamp(end.getTime());
        db.collection("ORDERS").whereEqualTo("shipperId", shipperId)
                .whereEqualTo("status", "COMPLETED").get()
                .addOnSuccessListener(snapshot -> {
                    List<Order> orders = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Order order = doc.toObject(Order.class);
                        order.setOrderId(doc.getId());
                        Timestamp delivered = order.getDeliveredAt();
                        if (delivered != null && delivered.compareTo(from) >= 0 && delivered.compareTo(until) < 0) {
                            orders.add(order);
                        }
                    }
                    orders.sort((a, b) -> {
                        if (a.getDeliveredAt() == null) return 1;
                        if (b.getDeliveredAt() == null) return -1;
                        return b.getDeliveredAt().compareTo(a.getDeliveredAt());
                    });
                    if (orders.isEmpty()) {
                        callback.onSuccess(new SalaryData(amount, count, status, orders));
                        return;
                    }
                    AtomicInteger done = new AtomicInteger();
                    for (Order order : orders) {
                        db.collection("ORDERS").document(order.getOrderId()).collection("ORDER_DETAILS").get()
                                .addOnSuccessListener(details -> {
                                    List<OrderDetail> items = new ArrayList<>();
                                    for (QueryDocumentSnapshot detail : details) items.add(detail.toObject(OrderDetail.class));
                                    order.setOrderDetails(items);
                                    finish(done, orders, amount, count, status, callback);
                                }).addOnFailureListener(error -> {
                                    order.setOrderDetails(new ArrayList<>());
                                    finish(done, orders, amount, count, status, callback);
                                });
                    }
                }).addOnFailureListener(callback::onError);
    }
    private void finish(AtomicInteger done, List<Order> orders, double amount, long count,
                       String status, Callback callback) {
        if (done.incrementAndGet() == orders.size()) callback.onSuccess(new SalaryData(amount, count, status, orders));
    }
}
