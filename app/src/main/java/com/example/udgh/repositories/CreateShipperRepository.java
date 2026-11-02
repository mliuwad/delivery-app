package com.example.udgh.repositories;

import android.content.Context;

/**
 * Account provisioning is handled by the project administrator in Firebase Console.
 * The client must never assign itself a privileged role.
 */
public class CreateShipperRepository {
    public interface Callback {
        void onSuccess();
        void onError(Exception error);
    }

    public void create(Context context, String fullName, String phone, String email, String password,
                       String status, String avatarUrl, String citizenId, String licensePlate,
                       String address, Callback callback) {
        callback.onError(new UnsupportedOperationException(
                "Tài khoản Shipper chỉ được cấp bởi quản trị dự án qua Firebase Console."));
    }
}
