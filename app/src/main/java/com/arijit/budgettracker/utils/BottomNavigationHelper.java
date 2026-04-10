package com.arijit.budgettracker.utils;

import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BottomNavigationHelper {

    private final AppCompatActivity activity;
    private final BottomNavigationView bottomNavigationView;
    public BottomNavigationHelper(AppCompatActivity activity, View navigationView) {
        this.activity = activity;
        this.bottomNavigationView = null;
    }

    public void setup(int containerId, Fragment homeFrag, Fragment historyFrag, Fragment statsFrag, Fragment profileFrag) {
        // Deprecated helper: navigation is handled in MainActivity now.
    }
    // Hàm bổ trợ chuyển Fragment
    private void replaceFragment(int containerId, Fragment fragment) {
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(containerId, fragment)
                .commit();
    }
}