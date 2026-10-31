package com.example.udgh.repository;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class OrderActionRepository {

    public interface RoleCallback {
        void onResult(String role, Exception error);
    }

    public interface ActionCallback {
        void onResult(boolean success, String newStatus, Exception error);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void loadUserRole(String userId, RoleCallback callback) {
        if (TextUtils.isEmpty(userId)) {
            callback.onResult(null, new IllegalStateException("Không xác định được tài khoản"));
            return;
        }

        db.collection("USERS").document(userId).get()
                .addOnSuccessListener(snapshot -> {
                    String role = snapshot.exists() ? snapshot.getString("role") : null;
                    if (TextUtils.isEmpty(role)) {
                        callback.onResult(null, new IllegalStateException("Tài khoản chưa được phân quyền"));
                    } else {
                        callback.onResult(role.trim().toUpperCase(Locale.ROOT), null);
                    }
                })
                .addOnFailureListener(error -> callback.onResult(null, error));
    }

    public void updateOrderStatus(String orderId, String userId,
                                  String requestedStatus, ActionCallback callback) {
        if (TextUtils.isEmpty(orderId) || TextUtils.isEmpty(userId)) {
            callback.onResult(false, null,
                    new IllegalArgumentException("Thiếu mã đơn hàng hoặc tài khoản"));
            return;
        }

        if (!"CANCELLED".equals(requestedStatus)
                && !"DELIVERING".equals(requestedStatus)
                && !"COMPLETED".equals(requestedStatus)) {
            callback.onResult(false, null,
                    new IllegalArgumentException("Trạng thái cập nhật không hợp lệ"));
            return;
        }

        if ("COMPLETED".equals(requestedStatus)) {
            prepareCompletion(orderId, userId, callback);
        } else {
            executeTransaction(orderId, userId, requestedStatus, null, null, callback);
        }
    }

    private void prepareCompletion(String orderId, String userId, ActionCallback callback) {
        DocumentReference orderRef = db.collection("ORDERS").document(orderId);

        // Đọc đơn hiện tại để xác định đúng bảng lương theo shipper/tháng/năm.
        // Transaction phía dưới sẽ đọc lại đơn và xác minh mọi điều kiện trước khi ghi.
        orderRef.get()
                .addOnSuccessListener(orderSnapshot -> {
                    if (!orderSnapshot.exists()) {
                        callback.onResult(false, null,
                                new IllegalStateException("Đơn hàng không tồn tại"));
                        return;
                    }

                    String shipperId = orderSnapshot.getString("shipperId");
                    if (TextUtils.isEmpty(shipperId)) {
                        callback.onResult(false, null,
                                new IllegalStateException("Đơn hàng chưa được gán shipper"));
                        return;
                    }

                    Timestamp completedAt = Timestamp.now();
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(completedAt.toDate());
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int year = calendar.get(Calendar.YEAR);

                    db.collection("SALARIES")
                            .whereEqualTo("shipperID", shipperId)
                            .whereEqualTo("month", month)
                            .whereEqualTo("year", year)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(salarySnapshots -> {
                                DocumentReference salaryRef;
                                if (!salarySnapshots.isEmpty()) {
                                    salaryRef = salarySnapshots.getDocuments().get(0).getReference();
                                } else {
                                    String salaryId = "SL_" + shipperId + "_" + year
                                            + String.format(Locale.US, "%02d", month);
                                    salaryRef = db.collection("SALARIES").document(salaryId);
                                }

                                executeTransaction(orderId, userId, "COMPLETED",
                                        completedAt, salaryRef, callback);
                            })
                            .addOnFailureListener(error ->
                                    callback.onResult(false, null, error));
                })
                .addOnFailureListener(error -> callback.onResult(false, null, error));
    }

    private void executeTransaction(String orderId, String userId, String requestedStatus,
                                    Timestamp completedAt, DocumentReference salaryRef,
                                    ActionCallback callback) {
        DocumentReference orderRef = db.collection("ORDERS").document(orderId);
        DocumentReference userRef = db.collection("USERS").document(userId);

        db.runTransaction(transaction -> {
            // Tất cả thao tác đọc phải hoàn tất trước thao tác ghi.
            DocumentSnapshot userSnapshot = transaction.get(userRef);
            DocumentSnapshot orderSnapshot = transaction.get(orderRef);

            if (!userSnapshot.exists() || !orderSnapshot.exists()) {
                throw new IllegalStateException("Không tìm thấy tài khoản hoặc đơn hàng");
            }

            String role = userSnapshot.getString("role");
            role = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);

            String currentStatus = orderSnapshot.getString("status");
            String creatorId = orderSnapshot.getString("creatorId");
            String assignedShipperId = orderSnapshot.getString("shipperId");

            if ("CANCELLED".equals(requestedStatus)) {
                if (!"CREATOR".equals(role) || !userId.equals(creatorId)) {
                    throw new IllegalStateException("Không có quyền hủy đơn hàng này");
                }
                if ("COMPLETED".equals(currentStatus) || "CANCELLED".equals(currentStatus)) {
                    throw new IllegalStateException("Đơn hàng đã kết thúc");
                }

                transaction.update(orderRef, "status", "CANCELLED");
                return "CANCELLED";
            }

            if ("DELIVERING".equals(requestedStatus)) {
                if (!"SHIPPER".equals(role)) {
                    throw new IllegalStateException("Tài khoản không có quyền nhận đơn");
                }
                if (!"PENDING".equals(currentStatus) && !"WAITING".equals(currentStatus)) {
                    throw new IllegalStateException("Đơn hàng không còn ở trạng thái chờ nhận");
                }
                // Hỗ trợ dữ liệu cũ: shipperId có thể là null, rỗng hoặc chưa tồn tại.
                if (!TextUtils.isEmpty(assignedShipperId)) {
                    throw new IllegalStateException("Đơn hàng đã có shipper nhận");
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("status", "DELIVERING");
                updates.put("shipperId", userId);
                transaction.update(orderRef, updates);
                return "DELIVERING";
            }

            if ("COMPLETED".equals(requestedStatus)) {
                if (!"SHIPPER".equals(role)) {
                    throw new IllegalStateException("Tài khoản không có quyền hoàn thành đơn");
                }
                if (!"DELIVERING".equals(currentStatus)) {
                    throw new IllegalStateException("Đơn hàng không ở trạng thái đang giao");
                }
                if (TextUtils.isEmpty(assignedShipperId) || !userId.equals(assignedShipperId)) {
                    throw new IllegalStateException("Đơn hàng không được giao cho shipper này");
                }
                if (completedAt == null || salaryRef == null) {
                    throw new IllegalStateException("Thiếu dữ liệu bảng lương");
                }

                boolean salaryAdded = Boolean.TRUE.equals(orderSnapshot.getBoolean("salaryAdded"));
                DocumentSnapshot salarySnapshot = null;
                if (!salaryAdded) {
                    salarySnapshot = transaction.get(salaryRef);
                }

                if (!salaryAdded) {
                    double deliveryFee = orderSnapshot.getDouble("deliveryFee") != null
                            ? orderSnapshot.getDouble("deliveryFee") : 0d;

                    if (salarySnapshot != null && salarySnapshot.exists()) {
                        Long oldOrders = salarySnapshot.getLong("totalOrders");
                        Double oldSalary = salarySnapshot.getDouble("totalSalary");

                        Map<String, Object> salaryUpdates = new HashMap<>();
                        salaryUpdates.put("totalOrders", (oldOrders != null ? oldOrders : 0L) + 1L);
                        salaryUpdates.put("totalSalary", (oldSalary != null ? oldSalary : 0d) + deliveryFee);
                        transaction.update(salaryRef, salaryUpdates);
                    } else {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(completedAt.toDate());

                        Map<String, Object> salaryData = new HashMap<>();
                        salaryData.put("shipperID", assignedShipperId);
                        salaryData.put("month", calendar.get(Calendar.MONTH) + 1);
                        salaryData.put("year", calendar.get(Calendar.YEAR));
                        salaryData.put("totalOrders", 1);
                        salaryData.put("totalSalary", deliveryFee);
                        salaryData.put("salaryPerOrder", 0);
                        salaryData.put("paymentStatus", "PENDING");
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
            }

            throw new IllegalStateException("Trạng thái cập nhật không hợp lệ");
        }).addOnSuccessListener(status ->
                callback.onResult(true, status, null))
          .addOnFailureListener(error ->
                callback.onResult(false, null, error));
    }
}
