package com.smsindia.app.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.smsindia.app.R;

public class SMSFragment extends Fragment {

    private static final int SMS_PERMISSION_CODE = 100;
    private Button btnStart, btnStop;
    private TextView tvStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_sms, container, false);

        btnStart = v.findViewById(R.id.btn_start);
        btnStop = v.findViewById(R.id.btn_stop);
        tvStatus = v.findViewById(R.id.tv_status);

        btnStart.setOnClickListener(view -> checkAndSendSMS());
        btnStop.setOnClickListener(view -> stopSending());

        return v;
    }

    private void checkAndSendSMS() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_CODE);
        } else {
            sendSampleSMS();
        }
    }

    private void sendSampleSMS() {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage("9999999999", null,
                    "This is a test message from SMSIndia", null, null);
            tvStatus.setText("✅ SMS sent successfully");
            Toast.makeText(requireContext(), "SMS sent successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            tvStatus.setText("❌ Error sending SMS: " + e.getMessage());
        }
    }

    private void stopSending() {
        tvStatus.setText("⏸ SMS sending stopped");
        Toast.makeText(requireContext(), "Stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendSampleSMS();
            } else {
                Toast.makeText(requireContext(), "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}