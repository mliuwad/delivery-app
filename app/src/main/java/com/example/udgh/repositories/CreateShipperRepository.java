package com.example.udgh.repositories;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

public class CreateShipperRepository {
    public interface Callback {
        void onSuccess();
        void onError(Exception error);
    }

    public void create(Context context, String fullName, String phone, String email, String password,
                       String status, String avatarUrl, String citizenId, String licensePlate,
                       String address, Callback callback) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            callback.onError(new SecurityException("Vui lòng đăng nhập bằng tài khoản Admin"));
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("fullName", fullName);
        data.put("phone", phone);
        data.put("email", email);
        data.put("password", password);
        data.put("status", status);
        data.put("avatarUrl", avatarUrl == null ? "" : avatarUrl);
        data.put("citizenId", citizenId);
        data.put("licensePlate", licensePlate);
        data.put("address", address);
        com.google.firebase.functions.FirebaseFunctions.getInstance()
                .getHttpsCallable("createShipper").call(data)
                .addOnSuccessListener(result -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

}
