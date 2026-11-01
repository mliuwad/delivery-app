package com.example.udgh.activities;

import com.example.udgh.R;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.udgh.viewmodels.SessionViewModel;

public class SplashActivity extends AppCompatActivity {
    private SessionViewModel viewModel;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        viewModel = new ViewModelProvider(this).get(SessionViewModel.class);
        viewModel.getRole().observe(this, this::navigateByRole);
        viewModel.getError().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                navigateToLogin();
            }
        });
        viewModel.validateCurrentSession();
    }

    private void navigateByRole(String role) {
        if (role == null) {
            navigateToLogin();
            return;
        }
        Class<?> destination;
        switch (role) {
            case "ADMIN": destination = MainActivity.class; break;
            case "CREATOR": destination = HomeCreatorActivity.class; break;
            case "SHIPPER": destination = HomeShipperActivity.class; break;
            default:
                viewModel.signOut();
                navigateToLogin();
                return;
        }
        startActivity(new Intent(this, destination));
        finish();
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
