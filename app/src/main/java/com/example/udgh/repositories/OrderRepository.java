
package com.example.udgh.repositories;

import com.example.udgh.models.Order;
import com.example.udgh.models.OrderDetail;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
