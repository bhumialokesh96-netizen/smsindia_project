package com.smsindia.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.firestore.FirebaseFirestore;
import com.smsindia.app.MainActivity;
import com.smsindia.app.R;
import com.smsindia.app.worker.SmsWorker;

public class TaskFragment extends Fragment {

    private TextView txtTaskInfo;
    private ProgressBar progressBar;
    private Button btnStart;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_task, container, false);

        txtTaskInfo = v.findViewById(R.id.txt_task_info);
        progressBar = v.findViewById(R.id.task_progress);
        btnStart = v.findViewById(R.id.btn_start_task); // make sure this button exists in XML
        db = FirebaseFirestore.getInstance();

        // 🔹 Load tasks info
        progressBar.setVisibility(View.VISIBLE);
        db.collection("tasks").get().addOnSuccessListener(snap -> {
            progressBar.setVisibility(View.GONE);
            if (snap.isEmpty()) {
                txtTaskInfo.setText("No tasks available right now.");
            } else {
                txtTaskInfo.setText("Total tasks available: " + snap.size());
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            txtTaskInfo.setText("Error loading tasks: " + e.getMessage());
        });

        // 🔹 Start button click — only here permissions are requested
        btnStart.setOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).requestSmsAndPhonePermissions();
            }

            // 🔹 Start SMS background job
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SmsWorker.class).build();
            WorkManager.getInstance(requireContext()).enqueue(request);

            Toast.makeText(requireContext(), "Task started ✅", Toast.LENGTH_SHORT).show();
        });

        return v;
    }
}