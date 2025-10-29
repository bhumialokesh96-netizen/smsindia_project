package com.smsindia.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.smsindia.app.R;
import com.smsindia.app.ui.SMSFragment;
import com.smsindia.app.ui.TaskFragment;
import com.smsindia.app.ui.ProfileFragment;
import com.smsindia.app.ui.WithdrawFragment;

public class MainActivity extends AppCompatActivity {
    private ActivityResultLauncher<String> smsPermissionLauncher;
    private ActivityResultLauncher<String> phoneStateLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Register permission launchers
        smsPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                result -> {}
        );
        phoneStateLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {}
        );

        // ✅ Request permissions if not already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            phoneStateLauncher.launch(Manifest.permission.READ_PHONE_STATE);
        }

        // ✅ Setup bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected;

            int id = item.getItemId(); // 👈 prevents "constant expression" compile error
            if (id == R.id.nav_sms) {
                selected = new SMSFragment();
            } else if (id == R.id.nav_task) {
                selected = new TaskFragment();
            } else if (id == R.id.nav_withdraw) {
                selected = new WithdrawFragment();
            } else if (id == R.id.nav_profile) {
                selected = new ProfileFragment();
            } else {
                selected = new SMSFragment();
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, selected)
                    .commit();

            return true;
        });

        // ✅ Default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SMSFragment())
                    .commit();
        }
    }
}