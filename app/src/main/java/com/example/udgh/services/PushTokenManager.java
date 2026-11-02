package com.example.udgh.services;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

/** Token is linked only to the authenticated account; never expose server credentials in the app. */
public final class PushTokenManager {
    private PushTokenManager() {}

    public static void sync() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token ->
            FirebaseFirestore.getInstance().collection("USERS").document(uid)
                .get().addOnSuccessListener(user -> {
                    if ("SHIPPER".equalsIgnoreCase(user.getString("role"))
                            && "ACTIVE".equalsIgnoreCase(user.getString("status"))) {
                        user.getReference().update("fcmToken", token);
                    }
                }));
    }

    public static void clear() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("USERS").document(uid)
            .update("fcmToken", com.google.firebase.firestore.FieldValue.delete());
        FirebaseMessaging.getInstance().deleteToken();
    }
}
