package com.arijit.budgettracker.utils;

import android.content.Intent;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.arijit.budgettracker.R;
import com.arijit.budgettracker.AddExpenseActivity; // Activity thêm chi tiêu của bạn
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class BottomNavigationHelper {

    private final AppCompatActivity activity;
    private final BottomNavigationView bottomNavigationView;
    private final FloatingActionButton fabAdd;
    public BottomNavigationHelper(AppCompatActivity activity, View navigationView) {
        this.activity = activity;
        this.bottomNavigationView = navigationView.findViewById(R.id.bottomNavigationView);
        this.fabAdd = navigationView.findViewById(R.id.fab_add);
    }

    public void setup(int containerId, Fragment homeFrag, Fragment chartFrag, Fragment statsFrag, Fragment profileFrag) {
        // 1. Xử lý các icon trên thanh menu
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                replaceFragment(containerId, homeFrag);
            } else if (id == R.id.nav_chart) {
                replaceFragment(containerId, chartFrag);
            } else if (id == R.id.nav_stats) {
                replaceFragment(containerId, statsFrag);
            } else if (id == R.id.nav_profile) {
                replaceFragment(containerId, profileFrag);
            }
            return true;
        });
        // 2. Xử lý nút cộng (+) ở giữa
        fabAdd.setOnClickListener(v -> {
            // Mở màn hình thêm mới
            Intent intent = new Intent(activity, AddExpenseActivity.class);
            activity.startActivity(intent);
        });
    }
    // Hàm bổ trợ chuyển Fragment
    private void replaceFragment(int containerId, Fragment fragment) {
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(containerId, fragment)
                .commit();
    }
}