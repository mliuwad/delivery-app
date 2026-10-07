package com.example.udgh;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {

            startActivity(new Intent(
                    SplashActivity.this,
                    LoginActivity.class
            ));

            finish();

            return;
        }

        loadUser(currentUser.getUid());
    }

    private void loadUser(String uid) {

        db.collection("USERS")
                .document(uid)
                .get()

                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) {

                        mAuth.signOut();

                        startActivity(new Intent(
                                SplashActivity.this,
                                LoginActivity.class
                        ));

                        finish();

                        return;
                    }

                    String role = documentSnapshot.getString("role");
                    String status = documentSnapshot.getString("status");

                    if (!"ACTIVE".equals(status)) {

                        mAuth.signOut();

                        Toast.makeText(
                                this,
                                "Tài khoản đã bị khóa",
                                Toast.LENGTH_SHORT
                        ).show();

                        startActivity(new Intent(
                                SplashActivity.this,
                                LoginActivity.class
                        ));

                        finish();

                        return;
                    }

                    if ("ADMIN".equals(role)) {

                        startActivity(new Intent(
                                SplashActivity.this,
                                MainActivity.class
                        ));

                    }

                    else if ("CREATOR".equals(role)) {

                        startActivity(new Intent(
                                SplashActivity.this,
                                HomeCustomerActivity.class
                        ));

                    }
                    else if ("SHIPPER".equals(role)) {

                        startActivity(new Intent(
                                SplashActivity.this,
                                HomeShipperActivity.class
                        ));

                    }
                    else {

                        mAuth.signOut();

                        startActivity(new Intent(
                                SplashActivity.this,
                                LoginActivity.class
                        ));

                    }

                    finish();

                })

                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();

                    startActivity(new Intent(
                            SplashActivity.this,
                            LoginActivity.class
                    ));

                    finish();

                });

    }

}