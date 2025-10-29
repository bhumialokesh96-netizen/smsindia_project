package com.smsindia.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.activity.result.ActivityResultCallback;
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

        smsPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                new ActivityResultCallback<Boolean>() { public void onActivityResult(Boolean result) {} });
        phoneStateLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                granted -> {});

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            phoneStateLauncher.launch(Manifest.permission.READ_PHONE_STATE);
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            switch (item.getItemId()) {
                case R.id.nav_sms: selected = new SMSFragment(); break;
                case R.id.nav_task: selected = new TaskFragment(); break;
                case R.id.nav_withdraw: selected = new WithdrawFragment(); break;
                case R.id.nav_profile: selected = new ProfileFragment(); break;
                default: selected = new SMSFragment();
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selected).commit();
            return true;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SMSFragment()).commit();
        }
    }
}
