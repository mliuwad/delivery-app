package com.example.udgh.repositories;

import com.example.udgh.models.ShipperDetail;
import com.example.udgh.models.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ShipperDetailRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback<T> {
        void onSuccess(T value);
        void onError(Exception error);
    }

    public static class DetailData {
        public final User user;
        public final ShipperDetail detail;
        public final String detailId;
        public final int completedOrders;

        public DetailData(User user, ShipperDetail detail, String detailId, int completedOrders) {
            this.user = user;
            this.detail = detail;
            this.detailId = detailId;
            this.completedOrders = completedOrders;
        }
    }

    public void load(String shipperId, Callback<DetailData> callback) {
        db.collection("USERS").document(shipperId).get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) {
                        callback.onError(new IllegalStateException("Không tìm thấy shipper"));
                        return;
                    }
                    User user = userDoc.toObject(User.class);
                    if (user == null) {
                        callback.onError(new IllegalStateException("Dữ liệu shipper không hợp lệ"));
                        return;
                    }
                    user.setUserId(userDoc.getId());

                    db.collection("USERS").document(shipperId)
                            .collection("SHIPPER_DETAILS").get()
                            .addOnSuccessListener(details -> {
                                ShipperDetail detail;
                                String detailId;
                                if (details.isEmpty()) {
                                    detailId = shipperId;
                                    detail = new ShipperDetail();
                                    detail.setShipperId(shipperId);
                                    Map<String, Object> initial = new HashMap<>();
                                    initial.put("shipperId", shipperId);
                                    db.collection("USERS").document(shipperId)
                                            .collection("SHIPPER_DETAILS").document(detailId)
                                            .set(initial)
                                            .addOnFailureListener(callback::onError);
                                } else {
                                    DocumentSnapshot detailDoc = details.getDocuments().get(0);
                                    detailId = detailDoc.getId();
                                    detail = detailDoc.toObject(ShipperDetail.class);
                                    if (detail == null) detail = new ShipperDetail();
                                }

                                final ShipperDetail finalDetail = detail;
                                final String finalDetailId = detailId;
                                db.collection("ORDERS").whereEqualTo("shipperId", shipperId)
                                        .whereEqualTo("status", "COMPLETED").get()
                                        .addOnSuccessListener(orders -> callback.onSuccess(
                                                new DetailData(user, finalDetail, finalDetailId, orders.size())))
                                        .addOnFailureListener(callback::onError);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void save(String shipperId, String detailId, String fullName, String phone,
                     String email, String citizenId, String licensePlate, String address,
                     Callback<Void> callback) {
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("fullName", fullName);
        userUpdates.put("phone", phone);
        userUpdates.put("email", email);

        Map<String, Object> detailUpdates = new HashMap<>();
        detailUpdates.put("citizenId", citizenId);
        detailUpdates.put("licensePlate", licensePlate);
        detailUpdates.put("address", address);

        db.runBatch(batch -> {
            batch.update(db.collection("USERS").document(shipperId), userUpdates);
            batch.set(db.collection("USERS").document(shipperId)
                    .collection("SHIPPER_DETAILS").document(detailId), detailUpdates, SetOptions.merge());
        }).addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void updateAvatar(String shipperId, String avatarUrl, Callback<Void> callback) {
        db.collection("USERS").document(shipperId).update("avatarUrl", avatarUrl)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void updateStatus(String shipperId, String status, Callback<Void> callback) {
        db.collection("USERS").document(shipperId).update("status", status)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void deleteProfile(String shipperId, String detailId, Callback<Void> callback) {
        db.collection("USERS").document(shipperId).collection("SHIPPER_DETAILS")
                .document(detailId).delete()
                .addOnSuccessListener(unused -> db.collection("USERS").document(shipperId).delete()
                        .addOnSuccessListener(done -> callback.onSuccess(null))
                        .addOnFailureListener(callback::onError))
                .addOnFailureListener(callback::onError);
    }
}
