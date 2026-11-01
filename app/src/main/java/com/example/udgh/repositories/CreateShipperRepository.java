package com.example.udgh.repositories;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateShipperRepository {
    public interface Callback {
        void onSuccess();
        void onError(Exception error);
    }

    public void create(Context context, String fullName, String phone, String email, String password,
                       String status, String avatarUrl, String citizenId, String licensePlate,
                       String address, Callback callback) {
        FirebaseApp defaultApp = FirebaseApp.getInstance();
        String appName = "shipper-registration-" + UUID.randomUUID();
        FirebaseApp secondaryApp = FirebaseApp.initializeApp(
                context.getApplicationContext(), defaultApp.getOptions(), appName);
        FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);
        FirebaseFirestore secondaryDb = FirebaseFirestore.getInstance(secondaryApp);

        secondaryAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser newUser = result.getUser();
                    if (newUser == null) {
                        cleanup(secondaryAuth, secondaryApp);
                        callback.onError(new IllegalStateException("Không tạo được tài khoản"));
                        return;
                    }

                    String uid = newUser.getUid();
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("fullName", fullName);
                    userData.put("phone", phone);
                    userData.put("email", email);
                    userData.put("role", "SHIPPER");
                    userData.put("status", status);
                    userData.put("avatarUrl", avatarUrl == null ? "" : avatarUrl);
                    userData.put("createdAt", com.google.firebase.Timestamp.now());

                    Map<String, Object> detailData = new HashMap<>();
                    detailData.put("shipperId", uid);
                    detailData.put("citizenId", citizenId);
                    detailData.put("licensePlate", licensePlate);
                    detailData.put("address", address);
                    detailData.put("joinDate", com.google.firebase.Timestamp.now());

                    WriteBatch batch = secondaryDb.batch();
                    batch.set(secondaryDb.collection("USERS").document(uid), userData);
                    batch.set(secondaryDb.collection("USERS").document(uid)
                            .collection("SHIPPER_DETAILS").document(uid), detailData);

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                cleanup(secondaryAuth, secondaryApp);
                                callback.onSuccess();
                            })
                            .addOnFailureListener(error -> newUser.delete()
                                    .addOnCompleteListener(task -> {
                                        cleanup(secondaryAuth, secondaryApp);
                                        callback.onError(error);
                                    }));
                })
                .addOnFailureListener(error -> {
                    cleanup(secondaryAuth, secondaryApp);
                    callback.onError(error);
                });
    }

    private void cleanup(FirebaseAuth auth, FirebaseApp app) {
        auth.signOut();
        app.delete();
    }
}
