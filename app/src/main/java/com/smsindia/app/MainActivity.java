package com.smsindia.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
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

        // ✅ Register permission launchers (no auto-launch yet)
        smsPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!granted) {
                        Toast.makeText(this, "SMS permission required to earn.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        phoneStateLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!granted) {
                        Toast.makeText(this, "Phone permission required for account verification.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // ✅ Bottom navigation setup
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected;
            int id = item.getItemId();

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

    // 🔹 Called by fragments (e.g., TaskFragment when "Start" clicked)
    public void requestSmsAndPhonePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            phoneStateLauncher.launch(Manifest.permission.READ_PHONE_STATE);
        }
    }
}