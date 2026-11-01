package com.example.udgh.activities;

import com.example.udgh.R;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.udgh.databinding.ActivityListOrderBinding;
import com.google.android.material.tabs.TabLayout;

public class ListOrderActitvity extends AppCompatActivity {

    private ActivityListOrderBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityListOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initToolbar();
        initTabLayout();
    }

    private void initToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initTabLayout() {

        binding.tabOrder.addTab(
                binding.tabOrder.newTab()
                        .setText("Tất cả")
                        .setIcon(android.R.drawable.ic_menu_agenda)
        );

        binding.tabOrder.addTab(
                binding.tabOrder.newTab()
                        .setText("Chờ nhận")
                        .setIcon(android.R.drawable.ic_popup_sync)
        );

        binding.tabOrder.addTab(
                binding.tabOrder.newTab()
                        .setText("Đang giao")
                        .setIcon(android.R.drawable.ic_menu_directions)
        );

        binding.tabOrder.addTab(
                binding.tabOrder.newTab()
                        .setText("Đã giao")
                        .setIcon(android.R.drawable.checkbox_on_background)
        );

        binding.tabOrder.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                switch (tab.getPosition()) {

                    case 0:
                        loadOrders("ALL");
                        break;

                    case 1:
                        loadOrders("PENDING");
                        break;

                    case 2:
                        loadOrders("DELIVERING");
                        break;

                    case 3:
                        loadOrders("COMPLETED");
                        break;
                }

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void loadOrders(String status) {

        Toast.makeText(this, status, Toast.LENGTH_SHORT).show();

    }
}