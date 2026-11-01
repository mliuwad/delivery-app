package com.example.udgh;

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
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.udgh.HomeShipperFragment;
import com.example.udgh.OrderShipperFragment;
import com.example.udgh.ProfileShipperFragment;
import com.example.udgh.SalaryShipperFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeShipperActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigation;
    private FragmentManager fragmentManager;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;
    private String avatarUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_shipper);

        RoleAccess.requireRole(this, "SHIPPER", this::initializeShipperScreen);
    }

    private void initializeShipperScreen() {

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", "");
        if (TextUtils.isEmpty(userId) && mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        toolbar = findViewById(R.id.toolbar);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        fragmentManager = getSupportFragmentManager();

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Trang chủ");

        // Load avatar từ Firestore
        loadAvatarUrl();

        // Mặc định hiển thị HomeShipperFragment
        if (savedInstanceState == null) {
            loadFragment(new HomeShipperFragment());
        }

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

    // Chuyển sang tab danh sách đơn hàng dành riêng cho Shipper.
    public void openOrdersTab() {
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_order);
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.commit();
    }

    // ================== LOAD AVATAR TỪ FIRESTORE ==================

    private void loadAvatarUrl() {
        if (TextUtils.isEmpty(userId)) return;

        db.collection("USERS")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String url = doc.getString("avatarUrl");
                        if (!TextUtils.isEmpty(url)) {
                            avatarUrl = url;
                            // Cập nhật menu sau khi có avatar
                            invalidateOptionsMenu();
                        }
                    }
                })
                .addOnFailureListener(e -> {});
    }

    // ================== MENU TOOLBAR ==================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.shipper_home_toolbar_menu, menu);

        MenuItem profileItem = menu.findItem(R.id.menuProfile);
        if (!TextUtils.isEmpty(avatarUrl)) {
            loadAvatarToMenuItem(profileItem);
        } else {
            // Nếu chưa có avatar, dùng icon mặc định
            profileItem.setIcon(R.drawable.delivery_man);
        }

        return true;
    }

    private void loadAvatarToMenuItem(MenuItem menuItem) {
        Glide.with(this)
                .asBitmap()
                .load(avatarUrl)
                .circleCrop()
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        // Chuyển Bitmap thành Drawable tròn
                        RoundedBitmapDrawable roundedDrawable = RoundedBitmapDrawableFactory.create(
                                getResources(), resource
                        );
                        roundedDrawable.setCircular(true);
                        menuItem.setIcon(roundedDrawable);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Không làm gì
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menuLogout) {
            logout();
            return true;
        } else if (id == R.id.menuProfile) {
            // Chuyển đến tab Profile
            bottomNavigation.setSelectedItemId(R.id.nav_profile);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ================== HÀM ĐĂNG XUẤT ==================

    private void logout() {
        mAuth.signOut();

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh avatar khi quay lại (trong trường hợp đã cập nhật ảnh từ Profile)
        loadAvatarUrl();
    }
}