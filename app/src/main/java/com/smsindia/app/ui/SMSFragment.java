package com.smsindia.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smsindia.app.R;
import com.smsindia.app.worker.SmsWorker;

import java.util.ArrayList;
import java.util.List;

public class SMSFragment extends Fragment {
    private SubscriptionManager subscriptionManager;
    private Spinner simSpinner;
    private EditText targetNumber, messageText;
    private Button startButton, stopButton, singleSendButton;
    private int selectedSubId = -1;
    private WorkManager workManager;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_sms, container, false);
        subscriptionManager = (SubscriptionManager) requireContext().getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);

        simSpinner = v.findViewById(R.id.sim_spinner);
        targetNumber = v.findViewById(R.id.edit_target);
        messageText = v.findViewById(R.id.edit_message);
        startButton = v.findViewById(R.id.btn_start);
        stopButton = v.findViewById(R.id.btn_stop);
        singleSendButton = v.findViewById(R.id.btn_send_once);

        workManager = WorkManager.getInstance(requireContext());
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) currentUserId = auth.getCurrentUser().getUid();

        loadSims();

        startButton.setOnClickListener(view -> {
            String target = targetNumber.getText().toString().trim();
            String msg = messageText.getText().toString();
            if (target.isEmpty()) {
                Toast.makeText(requireContext(), "Enter target number", Toast.LENGTH_SHORT).show();
                return;
            }
            Data input = new Data.Builder()
                    .putString("target", target)
                    .putString("message", msg)
                    .putInt("subId", selectedSubId)
                    .putString("userId", currentUserId != null ? currentUserId : "anonymous")
                    .build();

            OneTimeWorkRequest wr = new OneTimeWorkRequest.Builder(SmsWorker.class)
                    .setInputData(input)
                    .build();
            workManager.enqueue(wr);
            Toast.makeText(requireContext(), "SMS worker scheduled", Toast.LENGTH_SHORT).show();
        });

        stopButton.setOnClickListener(view -> {
            workManager.cancelAllWorkByTag("continuous_sms");
            Toast.makeText(requireContext(), "Stopped SMS worker", Toast.LENGTH_SHORT).show();
        });

        singleSendButton.setOnClickListener(view -> {
            String target = targetNumber.getText().toString().trim();
            String msg = messageText.getText().toString();
            if (target.isEmpty()) { Toast.makeText(requireContext(), "Enter target", Toast.LENGTH_SHORT).show(); return; }
            Data data = new Data.Builder()
                    .putString("target", target)
                    .putString("message", msg)
                    .putInt("subId", selectedSubId)
                    .putString("userId", currentUserId != null ? currentUserId : "anonymous")
                    .build();
            OneTimeWorkRequest one = new OneTimeWorkRequest.Builder(SmsWorker.class)
                    .setInputData(data).build();
            workManager.enqueue(one);
            Toast.makeText(requireContext(), "Scheduled single send", Toast.LENGTH_SHORT).show();
        });

        return v;
    }

    private void loadSims() {
        List<SubscriptionInfo> list = subscriptionManager.getActiveSubscriptionInfoList();
        if (list == null || list.isEmpty()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("SIM Info")
                    .setMessage("No SIM info available or permission not granted.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        List<String> labels = new ArrayList<>();
        for (SubscriptionInfo s : list) {
            String label = s.getCarrierName() + " (" + s.getNumber() + ")";
            labels.add(label);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        simSpinner.setAdapter(adapter);
        simSpinner.setSelection(0);
        selectedSubId = list.get(0).getSubscriptionId();
        simSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedSubId = list.get(i).getSubscriptionId();
            }
            @Override public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }
}
