package com.example.udgh.repositories;

import android.text.TextUtils;

import com.example.udgh.models.Order;
import com.example.udgh.models.OrderDetail;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DashboardRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(Exception error);
    }

    public static class CreatorDashboard {
        public String creatorName = "Cửa hàng của bạn";
        public int totalOrders;
        public int waiting;
        public int delivering;
        public int completed;
        public double completedProductRevenue;
        public final double[] monthlyRevenue = new double[12];
    }

    public static class ShipperDashboard {
        public String shipperName = "Người dùng";
        public int waiting;
        public int delivering;
        public int completed;
        public double income;
        public final List<Order> waitingOrders = new ArrayList<>();
    }

    public void loadCreatorDashboard(String userId, Callback<CreatorDashboard> callback) {
        if (TextUtils.isEmpty(userId)) {
            callback.onError(new IllegalArgumentException("Không xác định được tài khoản Creator"));
            return;
        }

        CreatorDashboard result = new CreatorDashboard();
        db.collection("USERS").document(userId).get()
                .addOnSuccessListener(user -> {
                    if (user.exists()) {
                        String name = user.getString("fullName");
                        if (!TextUtils.isEmpty(name)) result.creatorName = name;
                    }

                    db.collection("ORDERS").whereEqualTo("creatorId", userId).get()
                            .addOnSuccessListener(snapshot -> {
                                Calendar now = Calendar.getInstance();
                                int currentMonth = now.get(Calendar.MONTH);
                                int currentYear = now.get(Calendar.YEAR);
                                result.totalOrders = snapshot.size();

                                for (QueryDocumentSnapshot doc : snapshot) {
                                    String status = doc.getString("status");
                                    if ("PENDING".equals(status) || "WAITING".equals(status)) {
                                        result.waiting++;
                                    } else if ("DELIVERING".equals(status)) {
                                        result.delivering++;
                                    } else if ("COMPLETED".equals(status)) {
                                        result.completed++;
                                        Number amount = (Number) doc.get("totalProductValue");
                                        double value = amount == null ? 0d : amount.doubleValue();
                                        result.completedProductRevenue += value;

                                        Timestamp createdAt = doc.getTimestamp("createdAt");
                                        if (createdAt != null) {
                                            Calendar orderDate = Calendar.getInstance();
                                            orderDate.setTime(createdAt.toDate());
                                            int offset = (currentYear - orderDate.get(Calendar.YEAR)) * 12
                                                    + currentMonth - orderDate.get(Calendar.MONTH);
                                            if (offset >= 0 && offset < 12) {
                                                result.monthlyRevenue[11 - offset] += value;
                                            }
                                        }
                                    }
                                }
                                callback.onSuccess(result);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void loadShipperDashboard(String userId, Callback<ShipperDashboard> callback) {
        if (TextUtils.isEmpty(userId)) {
            callback.onError(new IllegalArgumentException("Không xác định được tài khoản Shipper"));
            return;
        }

        ShipperDashboard result = new ShipperDashboard();
        db.collection("USERS").document(userId).get()
                .addOnSuccessListener(user -> {
                    if (user.exists()) {
                        String name = user.getString("fullName");
                        if (!TextUtils.isEmpty(name)) result.shipperName = name;
                    }

                    db.collection("ORDERS")
                            .whereIn("status", java.util.Arrays.asList("PENDING", "WAITING"))
                            .get()
                            .addOnSuccessListener(waitingSnapshot -> {
                                List<Order> availableOrders = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : waitingSnapshot) {
                                    if (TextUtils.isEmpty(doc.getString("shipperId"))) {
                                        Order order = doc.toObject(Order.class);
                                        order.setOrderId(doc.getId());
                                        availableOrders.add(order);
                                    }
                                }
                                result.waiting = availableOrders.size();
                                Collections.shuffle(availableOrders);
                                int count = Math.min(2, availableOrders.size());
                                result.waitingOrders.addAll(availableOrders.subList(0, count));

                                db.collection("ORDERS").whereEqualTo("shipperId", userId).get()
                                        .addOnSuccessListener(assignedSnapshot -> {
                                            for (QueryDocumentSnapshot doc : assignedSnapshot) {
                                                String status = doc.getString("status");
                                                if ("DELIVERING".equals(status)) {
                                                    result.delivering++;
                                                } else if ("COMPLETED".equals(status)) {
                                                    result.completed++;
                                                    Number fee = (Number) doc.get("deliveryFee");
                                                    if (fee != null) result.income += fee.doubleValue();
                                                }
                                            }
                                            loadWaitingOrderDetails(result.waitingOrders, callback, result);
                                        })
                                        .addOnFailureListener(callback::onError);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    private void loadWaitingOrderDetails(List<Order> orders,
                                         Callback<ShipperDashboard> callback,
                                         ShipperDashboard result) {
        if (orders.isEmpty()) {
            callback.onSuccess(result);
            return;
        }

        AtomicInteger completed = new AtomicInteger(0);
        for (Order order : orders) {
            db.collection("ORDERS").document(order.getOrderId())
                    .collection("ORDER_DETAILS").get()
                    .addOnSuccessListener(snapshot -> {
                        List<OrderDetail> details = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshot) {
                            details.add(doc.toObject(OrderDetail.class));
                        }
                        order.setOrderDetails(details);
                        if (completed.incrementAndGet() == orders.size()) {
                            callback.onSuccess(result);
                        }
                    })
                    .addOnFailureListener(error -> {
                        order.setOrderDetails(new ArrayList<>());
                        if (completed.incrementAndGet() == orders.size()) {
                            callback.onSuccess(result);
                        }
                    });
        }
    }
}
