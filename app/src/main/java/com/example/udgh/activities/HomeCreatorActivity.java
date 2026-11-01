package com.example.udgh.activities;

import com.example.udgh.fragments.CreatorHomeFragment;
import com.example.udgh.fragments.CreatorOrderFragment;
import com.example.udgh.fragments.CreatorProductFragment;
import com.example.udgh.utils.RoleAccess;
import com.example.udgh.R;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.udgh.viewmodels.SessionViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeCreatorActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigation;
    private SessionViewModel sessionViewModel;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_creator);
        RoleAccess.requireRole(this, "CREATOR",
                () -> initializeCreatorScreen(savedInstanceState == null));
    }

    private void initializeCreatorScreen(boolean isFirstCreation) {
        sessionViewModel = new ViewModelProvider(this).get(SessionViewModel.class);
        toolbar = findViewById(R.id.toolbar);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        setSupportActionBar(toolbar);

        if (isFirstCreation) {
            bottomNavigation.setSelectedItemId(R.id.nav_creator_home);
            showFragment(new CreatorHomeFragment(), "Trang chủ");
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment;
            String title;
            int id = item.getItemId();
            if (id == R.id.nav_creator_home) {
                fragment = new CreatorHomeFragment();
                title = "Tổng quan";
            } else if (id == R.id.nav_creator_orders) {
                fragment = new CreatorOrderFragment();
                title = "Đơn hàng";
            } else if (id == R.id.nav_creator_products) {
                fragment = new CreatorProductFragment();
                title = "Sản phẩm";
            } else return false;
            showFragment(fragment, title);
            return true;
        });
    }

    private void showFragment(Fragment fragment, String title) {
        toolbar.setTitle(title);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment).commit();
    }

    public void openOrdersTab() {
        bottomNavigation.setSelectedItemId(R.id.nav_creator_orders);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.creator_home_toolbar_menu, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_logout) {
            sessionViewModel.signOut();
            getSharedPreferences("MyAppPrefs", MODE_PRIVATE).edit().clear().apply();
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
