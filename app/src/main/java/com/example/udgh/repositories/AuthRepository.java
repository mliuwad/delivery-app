package com.example.udgh.repositories;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthRepository {

    public interface Callback<T> {
        void onSuccess(T value);
        void onError(String message);
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void signIn(String email, String password, Callback<String> callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    if (result.getUser() == null) {
                        auth.signOut();
                        callback.onError("Không xác định được tài khoản");
                        return;
                    }

                    String uid = result.getUser().getUid();
                    db.collection("USERS").document(uid).get()
                            .addOnSuccessListener(document -> {
                                if (!document.exists()) {
                                    auth.signOut();
                                    callback.onError("Không tìm thấy thông tin người dùng");
                                    return;
                                }

                                String status = document.getString("status");
                                if (!"ACTIVE".equalsIgnoreCase(status)) {
                                    auth.signOut();
                                    callback.onError("Tài khoản đã bị khóa hoặc chưa được kích hoạt");
                                    return;
                                }

                                String role = document.getString("role");
                                if (role == null) {
                                    auth.signOut();
                                    callback.onError("Vai trò tài khoản không hợp lệ");
                                    return;
                                }

                                String normalizedRole = role.trim().toUpperCase(java.util.Locale.ROOT);
                                if (!"ADMIN".equals(normalizedRole)
                                        && !"CREATOR".equals(normalizedRole)
                                        && !"SHIPPER".equals(normalizedRole)) {
                                    auth.signOut();
                                    callback.onError("Vai trò tài khoản không hợp lệ");
                                    return;
                                }

                                callback.onSuccess(normalizedRole);
                            })
                            .addOnFailureListener(error -> {
                                auth.signOut();
                                callback.onError("Không thể xác minh tài khoản. Vui lòng thử lại.");
                            });
                })
                .addOnFailureListener(error -> callback.onError(getSignInError(error)));
    }

    public void sendPasswordReset(String email, Callback<Void> callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(error ->
                        callback.onError(error.getMessage() == null
                                ? "Không thể gửi email khôi phục mật khẩu"
                                : error.getMessage()));
    }

    private String getSignInError(Exception error) {
        if (error instanceof FirebaseAuthInvalidUserException) {
            return "Tài khoản không tồn tại trên hệ thống.";
        }
        if (error instanceof FirebaseAuthInvalidCredentialsException) {
            return "Email hoặc mật khẩu không chính xác. Vui lòng thử lại.";
        }
        String message = error.getMessage();
        if (message != null && message.toLowerCase(java.util.Locale.ROOT).contains("network")) {
            return "Không có kết nối Internet. Vui lòng kiểm tra lại mạng.";
        }
        if (message != null && message.toLowerCase(java.util.Locale.ROOT).contains("too many")) {
            return "Bạn đã thử quá nhiều lần. Vui lòng thử lại sau ít phút.";
        }
        return "Đăng nhập thất bại. Vui lòng thử lại sau.";
    }
}
