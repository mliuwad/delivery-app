package com.example.udgh.utils;

import com.example.udgh.activities.HomeCreatorActivity;
import com.example.udgh.activities.HomeShipperActivity;
import com.example.udgh.activities.LoginActivity;
import com.example.udgh.activities.MainActivity;
import com.example.udgh.R;

import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.udgh.repositories.SessionRepository;

/**
 * Kiểm tra phiên đăng nhập và vai trò trước khi khởi tạo màn hình.
 * Việc đọc USERS và xác thực trạng thái được giao cho SessionRepository.
 */
public final class RoleAccess {
    private RoleAccess() {}

    public static void requireRole(AppCompatActivity activity,
                                   String requiredRole,
                                   Runnable onAuthorized) {
        new SessionRepository().validateCurrentSession(new SessionRepository.Callback() {
            @Override
            public void onSuccess(String actualRole) {
                if (activity.isFinishing() || activity.isDestroyed()) return;

                if (actualRole == null) {
                    Toast.makeText(activity, "Vui lòng đăng nhập lại", Toast.LENGTH_LONG).show();
                    redirectToLogin(activity);
                    return;
                }

                if (!requiredRole.equalsIgnoreCase(actualRole)) {
                    Toast.makeText(activity, "Bạn không có quyền truy cập màn hình này",
                            Toast.LENGTH_LONG).show();
                    redirectToRoleHome(activity, actualRole);
                    return;
                }

                onAuthorized.run();
            }

            @Override
            public void onError(String message) {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                activity.finish();
            }
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
