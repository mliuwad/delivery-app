package com.example.udgh.repositories;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegistrationRepository {
    public interface Callback {
        void onSuccess();
        void onError(String message);
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void register(String fullName, String email, String password, String role,
                         Callback callback) {
        if (!"CREATOR".equals(role) && !"SHIPPER".equals(role)) {
            callback.onError("Vai trò đăng ký không hợp lệ");
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        auth.signOut();
                        callback.onError("Không thể tạo tài khoản");
                        return;
                    }

                    Map<String, Object> profile = new HashMap<>();
                    profile.put("fullName", fullName);
                    profile.put("email", email);
                    profile.put("phone", "");
                    profile.put("role", role);
                    profile.put("status", "ACTIVE");
                    profile.put("avatarUrl", "");
                    profile.put("createdAt", Timestamp.now());

                    db.collection("USERS").document(user.getUid()).set(profile)
                            .addOnSuccessListener(unused -> {
                                auth.signOut();
                                callback.onSuccess();
                            })
                            .addOnFailureListener(error -> user.delete()
                                    .addOnCompleteListener(task -> {
                                        auth.signOut();
                                        callback.onError("Không thể lưu hồ sơ tài khoản. Vui lòng thử lại.");
                                    }));
                })
                .addOnFailureListener(error -> {
                    auth.signOut();
                    String message = error.getMessage();
                    callback.onError(message == null
                            ? "Đăng ký thất bại. Vui lòng thử lại." : message);
                });
    }
}
