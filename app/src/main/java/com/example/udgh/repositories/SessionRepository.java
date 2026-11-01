package com.example.udgh.repositories;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SessionRepository {
    public interface Callback {
        void onSuccess(String role);
        void onError(String message);
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user == null ? null : user.getUid();
    }

    public void validateCurrentSession(Callback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onSuccess(null);
            return;
        }

        db.collection("USERS").document(currentUser.getUid()).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        auth.signOut();
                        callback.onSuccess(null);
                        return;
                    }
                    String role = document.getString("role");
                    String status = document.getString("status");
                    if (!"ACTIVE".equalsIgnoreCase(status)
                            || role == null || role.trim().isEmpty()) {
                        auth.signOut();
                        callback.onSuccess(null);
                        return;
                    }
                    String normalized = role.trim().toUpperCase(java.util.Locale.ROOT);
                    if (!"ADMIN".equals(normalized) && !"CREATOR".equals(normalized)
                            && !"SHIPPER".equals(normalized)) {
                        auth.signOut();
                        callback.onSuccess(null);
                        return;
                    }
                    callback.onSuccess(normalized);
                })
                .addOnFailureListener(error -> callback.onError(
                        "Không thể xác minh phiên đăng nhập. Vui lòng thử lại."));
    }

    public void signOut() {
        auth.signOut();
    }
}
