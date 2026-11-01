package com.example.udgh;

import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Xác thực phiên đăng nhập, trạng thái tài khoản và vai trò trước khi
 * khởi tạo màn hình dành riêng cho từng nhóm người dùng.
 */
public final class RoleAccess {

    private RoleAccess() {}

    public static void requireRole(
            AppCompatActivity activity,
            String requiredRole,
            Runnable onAuthorized
    ) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            redirectToLogin(activity);
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("USERS")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (activity.isFinishing() || activity.isDestroyed()) return;

                    String actualRole = document.getString("role");
                    String status = document.getString("status");

                    if (!document.exists() || !"ACTIVE".equalsIgnoreCase(status)) {
                        auth.signOut();
                        Toast.makeText(activity,
                                "Tài khoản không tồn tại hoặc đã bị khóa",
                                Toast.LENGTH_LONG).show();
                        redirectToLogin(activity);
                        return;
                    }

                    if (actualRole == null || !requiredRole.equalsIgnoreCase(actualRole)) {
                        Toast.makeText(activity,
                                "Bạn không có quyền truy cập màn hình này",
                                Toast.LENGTH_LONG).show();
                        redirectToRoleHome(activity, actualRole);
                        return;
                    }

                    onAuthorized.run();
                })
                .addOnFailureListener(error -> {
                    if (activity.isFinishing() || activity.isDestroyed()) return;
                    Toast.makeText(activity,
                            "Không thể xác minh quyền truy cập. Vui lòng kiểm tra kết nối và thử lại.",
                            Toast.LENGTH_LONG).show();
                    activity.finish();
                });
    }

    private static void redirectToLogin(AppCompatActivity activity) {
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    private static void redirectToRoleHome(AppCompatActivity activity, String role) {
        Class<?> destination;
        if ("ADMIN".equalsIgnoreCase(role)) {
            destination = MainActivity.class;
        } else if ("CREATOR".equalsIgnoreCase(role)) {
            destination = HomeCreatorActivity.class;
        } else if ("SHIPPER".equalsIgnoreCase(role)) {
            destination = HomeShipperActivity.class;
        } else {
            redirectToLogin(activity);
            return;
        }

        Intent intent = new Intent(activity, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
