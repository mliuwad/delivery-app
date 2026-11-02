package com.example.udgh.repositories;

/**
 * Public self-registration is disabled.
 * Accounts must be provisioned by a trusted administrator/backend.
 */
public class RegistrationRepository {
    public interface Callback {
        void onSuccess();
        void onError(String message);
    }

    public void register(String fullName, String email, String password, String role,
                         Callback callback) {
        callback.onError("Ứng dụng không hỗ trợ tự đăng ký. Vui lòng liên hệ quản trị viên để được cấp tài khoản.");
    }
}
