package com.example.udgh.activities;

import com.example.udgh.fragments.HomeShipperFragment;
import com.example.udgh.fragments.OrderShipperFragment;
import com.example.udgh.fragments.ProfileShipperFragment;
import com.example.udgh.fragments.SalaryShipperFragment;
import com.example.udgh.utils.RoleAccess;
import com.example.udgh.R;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.udgh.viewmodels.UserAvatarViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.udgh.viewmodels.SessionViewModel;

public class HomeShipperActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigation;
    private FragmentManager fragmentManager;
    private SessionViewModel sessionViewModel;
    private String userId;
    private String avatarUrl;
    private UserAvatarViewModel avatarViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_shipper);
        RoleAccess.requireRole(this, "SHIPPER",
                () -> initializeShipperScreen(savedInstanceState == null));
    }

    private void initializeShipperScreen(boolean isFirstCreation) {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        sessionViewModel = new ViewModelProvider(this).get(SessionViewModel.class);
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", "");
        if (TextUtils.isEmpty(userId)) userId = sessionViewModel.getCurrentUserId();

        toolbar = findViewById(R.id.toolbar);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        fragmentManager = getSupportFragmentManager();
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Trang chủ");

        avatarViewModel = new ViewModelProvider(this).get(UserAvatarViewModel.class);
        avatarViewModel.getAvatarUrl().observe(this, url -> {
            avatarUrl = url;
            invalidateOptionsMenu();
        });
        avatarViewModel.loadAvatar(userId);

        if (isFirstCreation) loadFragment(new HomeShipperFragment());

        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment fragment = null;
            if (id == R.id.nav_home) {
                fragment = new HomeShipperFragment();
                toolbar.setTitle("Trang chủ");
            } else if (id == R.id.nav_order) {
                fragment = new OrderShipperFragment();
                toolbar.setTitle("Đơn hàng");
            } else if (id == R.id.nav_salary) {
                fragment = new SalaryShipperFragment();
                toolbar.setTitle("Thống kê lương");
            } else if (id == R.id.nav_profile) {
                fragment = new ProfileShipperFragment();
                toolbar.setTitle("Cá nhân");
            }
            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    public void openOrdersTab() {
        if (bottomNavigation != null) bottomNavigation.setSelectedItemId(R.id.nav_order);
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.commit();
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.shipper_home_toolbar_menu, menu);
        MenuItem profileItem = menu.findItem(R.id.menuProfile);
        if (!TextUtils.isEmpty(avatarUrl)) loadAvatarToMenuItem(profileItem);
        else profileItem.setIcon(R.drawable.delivery_man);
        return true;
    }

    private void loadAvatarToMenuItem(MenuItem menuItem) {
        Glide.with(this).asBitmap().load(avatarUrl).circleCrop()
                .into(new CustomTarget<Bitmap>() {
                    @Override public void onResourceReady(@NonNull Bitmap resource,
                            @Nullable Transition<? super Bitmap> transition) {
                        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(
                                getResources(), resource);
                        drawable.setCircular(true);
                        menuItem.setIcon(drawable);
                    }
                    @Override public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menuLogout) {
            logout();
            return true;
        } else if (id == R.id.menuProfile) {
            bottomNavigation.setSelectedItemId(R.id.nav_profile);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        sessionViewModel.signOut();
        getSharedPreferences("MyAppPrefs", MODE_PRIVATE).edit().clear().apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
    }

    @Override protected void onResume() {
        super.onResume();
        if (avatarViewModel != null) avatarViewModel.loadAvatar(userId);
    }
}
