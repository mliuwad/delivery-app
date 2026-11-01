package com.example.udgh.repositories;

import com.google.firebase.firestore.FirebaseFirestore;

public class UserAvatarRepository {
    public interface Callback {
        void onSuccess(String avatarUrl);
        void onError(Exception error);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void loadAvatar(String userId, Callback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Không xác định được tài khoản"));
            return;
        }
        db.collection("USERS").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onError(new IllegalStateException("Không tìm thấy hồ sơ người dùng"));
                        return;
                    }
                    callback.onSuccess(document.getString("avatarUrl"));
                })
                .addOnFailureListener(callback::onError);
    }
}
