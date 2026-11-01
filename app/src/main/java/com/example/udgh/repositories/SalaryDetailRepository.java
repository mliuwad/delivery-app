package com.example.udgh.repositories;

import com.example.udgh.models.Order;
import com.example.udgh.models.OrderDetail;
import com.example.udgh.models.User;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SalaryDetailRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback<T> {
        void onSuccess(T value);
        void onError(Exception error);
    }

    public static class DetailData {
        public final User user;
        public final List<Order> orders;
        public final double totalSalary;
        public final long totalOrders;
        public final String paymentStatus;
        public final String salaryDocumentId;

        public DetailData(User user, List<Order> orders, double totalSalary, long totalOrders,
                          String paymentStatus, String salaryDocumentId) {
            this.user = user;
            this.orders = orders;
            this.totalSalary = totalSalary;
            this.totalOrders = totalOrders;
            this.paymentStatus = paymentStatus;
            this.salaryDocumentId = salaryDocumentId;
        }
    }

    public void load(String shipperId, int month, int year, Callback<DetailData> callback) {
        db.collection("USERS").document(shipperId).get()
                .addOnSuccessListener(userDoc -> {
                    User user = userDoc.exists() ? userDoc.toObject(User.class) : null;
                    if (user == null) {
                        callback.onError(new IllegalStateException("Không tìm thấy thông tin Shipper"));
                        return;
                    }
                    user.setUserId(userDoc.getId());

                    db.collection("SALARIES")
                            .whereEqualTo("shipperID", shipperId)
                            .whereEqualTo("month", month)
                            .whereEqualTo("year", year)
                            .get()
                            .addOnSuccessListener(salarySnapshots -> {
                                double totalSalary = 0d;
                                long totalOrders = 0L;
                                String paymentStatus = "UNPAID";
                                String salaryDocumentId = null;
                                if (!salarySnapshots.isEmpty()) {
                                    DocumentSnapshot salaryDoc = salarySnapshots.getDocuments().get(0);
                                    Double salaryValue = salaryDoc.getDouble("totalSalary");
                                    Long orderValue = salaryDoc.getLong("totalOrders");
                                    totalSalary = salaryValue != null ? salaryValue : 0d;
                                    totalOrders = orderValue != null ? orderValue : 0L;
                                    String savedStatus = salaryDoc.getString("paymentStatus");
                                    paymentStatus = savedStatus != null ? savedStatus : "UNPAID";
                                    salaryDocumentId = salaryDoc.getId();
                                }
                                loadCompletedOrders(shipperId, month, year, user, totalSalary,
                                        totalOrders, paymentStatus, salaryDocumentId, callback);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    private void loadCompletedOrders(String shipperId, int month, int year, User user,
                                     double totalSalary, long totalOrders, String paymentStatus,
                                     String salaryDocumentId, Callback<DetailData> callback) {
        Calendar start = Calendar.getInstance();
        start.clear();
        start.set(year, month - 1, 1, 0, 0, 0);
        Timestamp startAt = new Timestamp(start.getTime());

        Calendar end = Calendar.getInstance();
        end.clear();
        end.set(year, month, 1, 0, 0, 0);
        Timestamp endAt = new Timestamp(end.getTime());

        db.collection("ORDERS")
                .whereEqualTo("shipperId", shipperId)
                .whereEqualTo("status", "COMPLETED")
                .whereGreaterThanOrEqualTo("deliveredAt", startAt)
                .whereLessThan("deliveredAt", endAt)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Order> orders = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Order order = doc.toObject(Order.class);
                        order.setOrderId(doc.getId());
                        orders.add(order);
                    }
                    orders.sort((a, b) -> {
                        if (a.getDeliveredAt() == null && b.getDeliveredAt() == null) return 0;
                        if (a.getDeliveredAt() == null) return 1;
                        if (b.getDeliveredAt() == null) return -1;
                        return b.getDeliveredAt().compareTo(a.getDeliveredAt());
                    });

                    if (orders.isEmpty()) {
                        callback.onSuccess(new DetailData(user, orders, totalSalary, totalOrders,
                                paymentStatus, salaryDocumentId));
                        return;
                    }

                    AtomicInteger completed = new AtomicInteger(0);
                    for (Order order : orders) {
                        db.collection("ORDERS").document(order.getOrderId())
                                .collection("ORDER_DETAILS").get()
                                .addOnSuccessListener(details -> {
                                    List<OrderDetail> orderDetails = new ArrayList<>();
                                    for (QueryDocumentSnapshot detailDoc : details) {
                                        orderDetails.add(detailDoc.toObject(OrderDetail.class));
                                    }
                                    order.setOrderDetails(orderDetails);
                                    finishOrderLoad(completed, orders, user, totalSalary,
                                            totalOrders, paymentStatus, salaryDocumentId, callback);
                                })
                                .addOnFailureListener(error -> {
                                    order.setOrderDetails(new ArrayList<>());
                                    finishOrderLoad(completed, orders, user, totalSalary,
                                            totalOrders, paymentStatus, salaryDocumentId, callback);
                                });
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    private void finishOrderLoad(AtomicInteger completed, List<Order> orders, User user,
                                double totalSalary, long totalOrders, String paymentStatus,
                                String salaryDocumentId, Callback<DetailData> callback) {
        if (completed.incrementAndGet() == orders.size()) {
            callback.onSuccess(new DetailData(user, orders, totalSalary, totalOrders,
                    paymentStatus, salaryDocumentId));
        }
    }

    public void confirmPayment(String salaryDocumentId, Callback<Void> callback) {
        if (salaryDocumentId == null || salaryDocumentId.trim().isEmpty()) {
            callback.onError(new IllegalStateException("Không tìm thấy bản ghi lương để xác nhận"));
            return;
        }
        db.collection("SALARIES").document(salaryDocumentId)
                .update("paymentStatus", "PAID", "paymentDate", FieldValue.serverTimestamp())
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }
}
