
package com.example.udgh.repositories;

import com.example.udgh.models.Order;
import com.example.udgh.models.OrderDetail;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class OrderRepository {

    private final FirebaseFirestore db;
    private static final String COLLECTION = "ORDERS";

    public interface ResultCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public OrderRepository() {
        db = FirebaseFirestore.getInstance();
    }


    public interface RoleCallback {
        void onResult(String role, Exception error);
    }

    public interface ActionCallback {
        void onResult(boolean success, String newStatus, Exception error);
    }

    public void loadUserRole(String userId, RoleCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onResult(null, new IllegalStateException("Không xác định được tài khoản"));
            return;
        }

        db.collection("USERS").document(userId).get()
                .addOnSuccessListener(snapshot -> {
                    String role = snapshot.exists() ? snapshot.getString("role") : null;
                    callback.onResult(role == null ? null : role.trim().toUpperCase(Locale.ROOT),
                            role == null ? new IllegalStateException("Không tìm thấy quyền tài khoản") : null);
                })
                .addOnFailureListener(error -> callback.onResult(null, error));
    }

    public void updateOrderStatus(String orderId, String userId,
                                  String requestedStatus, ActionCallback callback) {
        if (orderId == null || orderId.trim().isEmpty()
                || userId == null || userId.trim().isEmpty()) {
            callback.onResult(false, null,
                    new IllegalArgumentException("Thiếu mã đơn hàng hoặc tài khoản"));
            return;
        }

        if ("COMPLETED".equals(requestedStatus)) {
            prepareCompletion(orderId, userId, callback);
        } else if ("CANCELLED".equals(requestedStatus) || "DELIVERING".equals(requestedStatus)) {
            executeOrderAction(orderId, userId, requestedStatus, null, null, callback);
        } else {
            callback.onResult(false, null, new IllegalArgumentException("Trạng thái không hợp lệ"));
        }
    }

    private void prepareCompletion(String orderId, String userId, ActionCallback callback) {
        db.collection(COLLECTION).document(orderId).get()
                .addOnSuccessListener(order -> {
                    if (!order.exists()) {
                        callback.onResult(false, null, new IllegalStateException("Không tìm thấy đơn hàng"));
                        return;
                    }

                    String shipperId = order.getString("shipperId");
                    if (shipperId == null || shipperId.trim().isEmpty() || !userId.equals(shipperId)) {
                        callback.onResult(false, null, new IllegalStateException("Đơn hàng không thuộc shipper này"));
                        return;
                    }

                    com.google.firebase.Timestamp completedAt = com.google.firebase.Timestamp.now();
                    java.util.Calendar calendar = java.util.Calendar.getInstance();
                    calendar.setTime(completedAt.toDate());
                    int month = calendar.get(java.util.Calendar.MONTH) + 1;
                    int year = calendar.get(java.util.Calendar.YEAR);

                    db.collection("SALARIES")
                            .whereEqualTo("shipperID", shipperId)
                            .whereEqualTo("month", month)
                            .whereEqualTo("year", year)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(salaries -> {
                                com.google.firebase.firestore.DocumentReference salaryRef;
                                if (!salaries.isEmpty()) {
                                    salaryRef = salaries.getDocuments().get(0).getReference();
                                } else {
                                    String salaryId = "SL_" + shipperId + "_" + year
                                            + String.format(java.util.Locale.US, "%02d", month);
                                    salaryRef = db.collection("SALARIES").document(salaryId);
                                }
                                executeOrderAction(orderId, userId, "COMPLETED",
                                        completedAt, salaryRef, callback);
                            })
                            .addOnFailureListener(error -> callback.onResult(false, null, error));
                })
                .addOnFailureListener(error -> callback.onResult(false, null, error));
    }

    private void executeOrderAction(String orderId, String userId, String requestedStatus,
                                    com.google.firebase.Timestamp completedAt,
                                    com.google.firebase.firestore.DocumentReference salaryRef,
                                    ActionCallback callback) {
        com.google.firebase.firestore.DocumentReference orderRef =
                db.collection(COLLECTION).document(orderId);
        com.google.firebase.firestore.DocumentReference userRef =
                db.collection("USERS").document(userId);

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot user = transaction.get(userRef);
            com.google.firebase.firestore.DocumentSnapshot order = transaction.get(orderRef);
            if (!user.exists() || !order.exists()) {
                throw new IllegalStateException("Không tìm thấy tài khoản hoặc đơn hàng");
            }

            String role = user.getString("role");
            role = role == null ? "" : role.trim().toUpperCase(java.util.Locale.ROOT);
            String status = order.getString("status");
            String creatorId = order.getString("creatorId");
            String shipperId = order.getString("shipperId");

            if ("CANCELLED".equals(requestedStatus)) {
                if (!"CREATOR".equals(role) || !userId.equals(creatorId)) {
                    throw new IllegalStateException("Bạn không có quyền hủy đơn này");
                }
                if (!"PENDING".equals(status) && !"WAITING".equals(status)) {
                    throw new IllegalStateException("Chỉ có thể hủy đơn khi đơn đang chờ nhận");
                }
                transaction.update(orderRef, "status", "CANCELLED");
                return "CANCELLED";
            }

            if ("DELIVERING".equals(requestedStatus)) {
                if (!"SHIPPER".equals(role)) {
                    throw new IllegalStateException("Tài khoản không có quyền nhận đơn");
                }
                if (!"PENDING".equals(status) && !"WAITING".equals(status)) {
                    throw new IllegalStateException("Đơn hàng không còn chờ nhận");
                }
                if (shipperId != null && !shipperId.trim().isEmpty()) {
                    throw new IllegalStateException("Đơn hàng đã có shipper nhận");
                }
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", "DELIVERING");
                updates.put("shipperId", userId);
                transaction.update(orderRef, updates);
                return "DELIVERING";
            }

            if (!"SHIPPER".equals(role)
                    || !"DELIVERING".equals(status)
                    || shipperId == null
                    || !userId.equals(shipperId)) {
                throw new IllegalStateException("Bạn không có quyền hoàn thành đơn này");
            }
            if (completedAt == null || salaryRef == null) {
                throw new IllegalStateException("Thiếu dữ liệu tính lương");
            }

            boolean salaryAdded = Boolean.TRUE.equals(order.getBoolean("salaryAdded"));
            com.google.firebase.firestore.DocumentSnapshot salary = null;
            if (!salaryAdded) salary = transaction.get(salaryRef);

            if (!salaryAdded) {
                Double feeValue = order.getDouble("deliveryFee");
                double fee = feeValue != null ? feeValue : 0d;

                if (salary != null && salary.exists()) {
                    Long oldOrders = salary.getLong("totalOrders");
                    Double oldSalary = salary.getDouble("totalSalary");
                    Map<String, Object> salaryUpdates = new HashMap<>();
                    salaryUpdates.put("totalOrders", (oldOrders != null ? oldOrders : 0L) + 1L);
                    salaryUpdates.put("totalSalary", (oldSalary != null ? oldSalary : 0d) + fee);
                    transaction.update(salaryRef, salaryUpdates);
                } else {
                    java.util.Calendar calendar = java.util.Calendar.getInstance();
                    calendar.setTime(completedAt.toDate());
                    Map<String, Object> salaryData = new HashMap<>();
                    salaryData.put("shipperID", shipperId);
                    salaryData.put("month", calendar.get(java.util.Calendar.MONTH) + 1);
                    salaryData.put("year", calendar.get(java.util.Calendar.YEAR));
                    salaryData.put("totalOrders", 1);
                    salaryData.put("totalSalary", fee);
                    salaryData.put("salaryPerOrder", 0);
                    salaryData.put("paymentStatus", "UNPAID");
                    salaryData.put("paymentDate", null);
                    transaction.set(salaryRef, salaryData);
                }
            }

            Map<String, Object> orderUpdates = new HashMap<>();
            orderUpdates.put("status", "COMPLETED");
            orderUpdates.put("deliveredAt", completedAt);
            orderUpdates.put("salaryAdded", true);
            transaction.update(orderRef, orderUpdates);
            return "COMPLETED";
        }).addOnSuccessListener(status ->
                callback.onResult(true, status, null))
          .addOnFailureListener(error ->
                callback.onResult(false, null, error));
    }

    /**
     * Tải một đơn hàng và các dòng chi tiết của đơn.
     */
    public void getOrderById(String orderId, ResultCallback<Order> callback) {
        if (orderId == null || orderId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Thiếu mã đơn hàng"));
            return;
        }

        db.collection(COLLECTION).document(orderId).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onError(new IllegalStateException("Không tìm thấy đơn hàng"));
                        return;
                    }
                    Order order = document.toObject(Order.class);
                    if (order == null) {
                        callback.onError(new IllegalStateException("Dữ liệu đơn hàng không hợp lệ"));
                        return;
                    }
                    order.setOrderId(document.getId());
                    document.getReference().collection("ORDER_DETAILS").get()
                            .addOnSuccessListener(snapshot -> {
                                List<OrderDetail> details = new ArrayList<>();
                                for (QueryDocumentSnapshot detailDoc : snapshot) {
                                    details.add(detailDoc.toObject(OrderDetail.class));
                                }
                                order.setOrderDetails(details);
                                callback.onSuccess(order);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getOrdersForShipper(String shipperId, String status,
                                    ResultCallback<List<Order>> callback) {
        if (shipperId == null || shipperId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Không xác định được tài khoản Shipper"));
            return;
        }

        if ("PENDING".equals(status)) {
            db.collection(COLLECTION)
                    .whereIn("status", java.util.Arrays.asList("PENDING", "WAITING"))
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        List<Order> orders = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshot) {
                            String assignedShipperId = doc.getString("shipperId");
                            if (assignedShipperId == null || assignedShipperId.trim().isEmpty()) {
                                Order order = doc.toObject(Order.class);
                                order.setOrderId(doc.getId());
                                orders.add(order);
                            }
                        }
                        sortOrders(orders);
                        if (orders.isEmpty()) callback.onSuccess(orders);
                        else loadOrderDetails(orders, callback);
                    })
                    .addOnFailureListener(callback::onError);
            return;
        }

        if (!"ALL".equals(status)) {
            db.collection(COLLECTION)
                    .whereEqualTo("status", status)
                    .whereEqualTo("shipperId", shipperId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        List<Order> orders = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshot) {
                            Order order = doc.toObject(Order.class);
                            order.setOrderId(doc.getId());
                            orders.add(order);
                        }
                        sortOrders(orders);
                        if (orders.isEmpty()) callback.onSuccess(orders);
                        else loadOrderDetails(orders, callback);
                    })
                    .addOnFailureListener(callback::onError);
            return;
        }

        final List<Order> combined = java.util.Collections.synchronizedList(new ArrayList<>());
        final AtomicInteger completedQueries = new AtomicInteger(0);
        final AtomicInteger failedQueries = new AtomicInteger(0);

        db.collection(COLLECTION)
                .whereIn("status", java.util.Arrays.asList("PENDING", "WAITING"))
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String assignedShipperId = doc.getString("shipperId");
                        if (assignedShipperId == null || assignedShipperId.trim().isEmpty()) {
                            Order order = doc.toObject(Order.class);
                            order.setOrderId(doc.getId());
                            combined.add(order);
                        }
                    }
                    finishShipperQueries(completedQueries, failedQueries, combined, callback);
                })
                .addOnFailureListener(error -> {
                    failedQueries.incrementAndGet();
                    finishShipperQueries(completedQueries, failedQueries, combined, callback);
                });

        db.collection(COLLECTION)
                .whereEqualTo("shipperId", shipperId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String orderStatus = doc.getString("status");
                        if (!"PENDING".equals(orderStatus) && !"WAITING".equals(orderStatus)) {
                            Order order = doc.toObject(Order.class);
                            order.setOrderId(doc.getId());
                            combined.add(order);
                        }
                    }
                    finishShipperQueries(completedQueries, failedQueries, combined, callback);
                })
                .addOnFailureListener(error -> {
                    failedQueries.incrementAndGet();
                    finishShipperQueries(completedQueries, failedQueries, combined, callback);
                });
    }

    private void finishShipperQueries(AtomicInteger completedQueries,
                                      AtomicInteger failedQueries,
                                      List<Order> orders,
                                      ResultCallback<List<Order>> callback) {
        if (completedQueries.incrementAndGet() != 2) return;

        if (orders.isEmpty() && failedQueries.get() > 0) {
            callback.onError(new IllegalStateException("Không thể tải danh sách đơn hàng"));
            return;
        }

        sortOrders(orders);
        if (orders.isEmpty()) callback.onSuccess(orders);
        else loadOrderDetails(orders, callback);
    }

    public void getOrdersByCreator(String creatorId, ResultCallback<List<Order>> callback) {
        if (creatorId == null || creatorId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Không xác định được tài khoản Creator"));
            return;
        }

        db.collection(COLLECTION).whereEqualTo("creatorId", creatorId).get()
                .addOnSuccessListener(snapshot -> {
                    List<Order> orders = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Order order = doc.toObject(Order.class);
                        order.setOrderId(doc.getId());
                        orders.add(order);
                    }
                    sortOrders(orders);
                    if (orders.isEmpty()) {
                        callback.onSuccess(orders);
                    } else {
                        loadOrderDetails(orders, callback);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    // Lấy danh sách đơn hàng theo trạng thái
    public void getOrders(String status, ResultCallback<List<Order>> callback) {

        Query query = db.collection(COLLECTION);

        if (status != null && !"ALL".equals(status)) {
            query = query.whereEqualTo("status", status);
        }

        query.get()
                .addOnSuccessListener(snapshot -> {

                    List<Order> orders = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        Order order = doc.toObject(Order.class);
                        order.setOrderId(doc.getId());
                        orders.add(order);
                    }

                    sortOrders(orders);

                    if (orders.isEmpty()) {
                        callback.onSuccess(orders);
                        return;
                    }

                    loadOrderDetails(orders, callback);
                })
                .addOnFailureListener(callback::onError);
    }

    // Tải chi tiết sản phẩm của từng đơn hàng
    private void loadOrderDetails(
            List<Order> orders,
            ResultCallback<List<Order>> callback
    ) {
        int total = orders.size();
        AtomicInteger completed = new AtomicInteger(0);

        for (Order order : orders) {

            db.collection(COLLECTION)
                    .document(order.getOrderId())
                    .collection("ORDER_DETAILS")
                    .get()
                    .addOnSuccessListener(snapshot -> {

                        List<OrderDetail> details = new ArrayList<>();

                        for (QueryDocumentSnapshot doc : snapshot) {
                            OrderDetail detail = doc.toObject(OrderDetail.class);
                            details.add(detail);
                        }

                        order.setOrderDetails(details);

                        if (completed.incrementAndGet() == total) {
                            callback.onSuccess(orders);
                        }
                    })
                    .addOnFailureListener(e -> {

                        // Giữ hành vi cũ:
                        // Nếu không tải được chi tiết thì đơn vẫn được hiển thị.
                        order.setOrderDetails(new ArrayList<>());

                        if (completed.incrementAndGet() == total) {
                            callback.onSuccess(orders);
                        }
                    });
        }
    }

    // Sắp xếp đơn mới nhất lên đầu
    private void sortOrders(List<Order> orders) {
        Collections.sort(orders, (o1, o2) -> {

            if (o1.getCreatedAt() == null
                    && o2.getCreatedAt() == null) {
                return 0;
            }

            if (o1.getCreatedAt() == null) return 1;
            if (o2.getCreatedAt() == null) return -1;

            return o2.getCreatedAt().compareTo(o1.getCreatedAt());
        });
    }
}
