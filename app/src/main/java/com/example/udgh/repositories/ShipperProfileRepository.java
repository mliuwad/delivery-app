package com.example.udgh.repositories;

import com.example.udgh.models.ShipperDetail;
import com.example.udgh.models.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ShipperProfileRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }

    public static class ProfileData {
        public final User user;
        public final ShipperDetail shipperDetail;
        public final String shipperDetailId;

        public ProfileData(User user, ShipperDetail detail, String detailId) {
            this.user = user;
            this.shipperDetail = detail;
            this.shipperDetailId = detailId;
        }
    }

    public void loadProfile(String userId, Callback<ProfileData> callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Không xác định được tài khoản"));
            return;
        }

        db.collection("USERS").document(userId).get()
                .addOnSuccessListener(userDocument -> {
                    if (!userDocument.exists()) {
                        callback.onError(new IllegalStateException("Không tìm thấy thông tin người dùng"));
                        return;
                    }

                    User user = userDocument.toObject(User.class);
                    if (user == null) {
                        callback.onError(new IllegalStateException("Thông tin người dùng không hợp lệ"));
                        return;
                    }
                    user.setUserId(userDocument.getId());

                    db.collection("USERS").document(userId)
                            .collection("SHIPPER_DETAILS").get()
                            .addOnSuccessListener(details -> {
                                if (!details.isEmpty()) {
                                    DocumentSnapshot detailDocument = details.getDocuments().get(0);
                                    ShipperDetail detail = detailDocument.toObject(ShipperDetail.class);
                                    callback.onSuccess(new ProfileData(
                                            user, detail, detailDocument.getId()));
                                    return;
                                }

                                ShipperDetail detail = new ShipperDetail();
                                detail.setShipperId(userId);
                                db.collection("USERS").document(userId)
                                        .collection("SHIPPER_DETAILS").document(userId)
                                        .set(java.util.Collections.singletonMap("shipperId", userId))
                                        .addOnSuccessListener(unused ->
                                                callback.onSuccess(new ProfileData(user, detail, userId)))
                                        .addOnFailureListener(callback::onError);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void saveProfile(String userId, String detailId,
                            String fullName, String phone, String email,
                            String citizenId, String licensePlate, String address,
                            Callback<Void> callback) {
        if (userId == null || userId.trim().isEmpty()
                || detailId == null || detailId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Thiếu thông tin tài khoản"));
            return;
        }

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("fullName", fullName);
        userUpdates.put("phone", phone);
        userUpdates.put("email", email);

        Map<String, Object> detailUpdates = new HashMap<>();
        detailUpdates.put("citizenId", citizenId);
        detailUpdates.put("licensePlate", licensePlate);
        detailUpdates.put("address", address);

        com.google.firebase.firestore.WriteBatch batch = db.batch();
        batch.update(db.collection("USERS").document(userId), userUpdates);
        batch.set(db.collection("USERS").document(userId)
                .collection("SHIPPER_DETAILS").document(detailId),
                detailUpdates, SetOptions.merge());

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void updateAvatar(String userId, String avatarUrl, Callback<Void> callback) {
        if (userId == null || userId.trim().isEmpty() || avatarUrl == null || avatarUrl.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Thiếu thông tin ảnh đại diện"));
            return;
        }

        db.collection("USERS").document(userId)
                .update("avatarUrl", avatarUrl)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }
}
