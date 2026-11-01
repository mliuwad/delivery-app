package com.example.udgh.repositories;

import com.example.udgh.models.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ShipperManagementRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }

    public void getShippers(Callback<List<User>> callback) {
        db.collection("USERS").whereEqualTo("role", "SHIPPER").get()
                .addOnSuccessListener(snapshot -> {
                    List<User> shippers = new ArrayList<>();
                    for (QueryDocumentSnapshot document : snapshot) {
                        User user = document.toObject(User.class);
                        user.setUserId(document.getId());
                        shippers.add(user);
                    }
                    callback.onSuccess(shippers);
                })
                .addOnFailureListener(callback::onError);
    }

    public void updateShipperStatus(String shipperId, String status, Callback<Void> callback) {
        if (shipperId == null || shipperId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Thiếu mã Shipper"));
            return;
        }
        db.collection("USERS").document(shipperId).update("status", status)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }
}
