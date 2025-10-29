package com.smsindia.app.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.smsindia.app.R;
import com.smsindia.app.worker.SmsWorker;

import java.util.List;

public class SMSFragment extends Fragment {

    private ActivityResultLauncher<String> smsPermissionLauncher;
    private ActivityResultLauncher<String> phonePermissionLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_sms, container, false);

        MaterialButton btnStart = v.findViewById(R.id.btn_start);
        MaterialButton btnStop = v.findViewById(R.id.btn_stop);

        smsPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!granted) Toast.makeText(requireContext(), "SMS permission required!", Toast.LENGTH_SHORT).show();
                });

        phonePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!granted) Toast.makeText(requireContext(), "Phone permission required!", Toast.LENGTH_SHORT).show();
                });

        btnStart.setOnClickListener(view -> startSending());
        btnStop.setOnClickListener(view -> stopSending());

        return v;
    }

    private void startSending() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS);
            return;
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE);
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        int simId = getSimId(); // automatically choose first SIM
        Data input = new Data.Builder()
                .putString("userId", user.getUid())
                .putInt("subscriptionId", simId)
                .putInt("maxPerRun", 10)
                .build();

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(SmsWorker.class)
                .setInputData(input)
                .build();

        WorkManager.getInstance(requireContext())
                .enqueueUniqueWork("sms_queue_worker_" + user.getUid(), ExistingWorkPolicy.REPLACE, req);

        Toast.makeText(requireContext(), "📤 Task started — sending SMS...", Toast.LENGTH_SHORT).show();
    }

    private void stopSending() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            WorkManager.getInstance(requireContext())
                    .cancelUniqueWork("sms_queue_worker_" + user.getUid());
            Toast.makeText(requireContext(), "⛔ Task stopped", Toast.LENGTH_SHORT).show();
        }
    }

    private int getSimId() {
        try {
            SubscriptionManager sm = (SubscriptionManager)
                    requireContext().getSystemService(android.content.Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            if (sm != null) {
                List<SubscriptionInfo> list = sm.getActiveSubscriptionInfoList();
                if (list != null && !list.isEmpty()) {
                    return list.get(0).getSubscriptionId(); // first SIM
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}