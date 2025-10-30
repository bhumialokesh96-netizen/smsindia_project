package com.smsindia.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.smsindia.app.R;

public class TaskFragment extends Fragment {

    private TextView txtTaskInfo;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_task, container, false);

        txtTaskInfo = v.findViewById(R.id.txt_task_info);
        progressBar = v.findViewById(R.id.task_progress);
        db = FirebaseFirestore.getInstance();

        loadTasks();

        return v;
    }

    private void loadTasks() {
        progressBar.setVisibility(View.VISIBLE);
        txtTaskInfo.setText("");

        db.collection("tasks").get()
                .addOnSuccessListener(this::handleSuccess)
                .addOnFailureListener(this::handleFailure);
    }

    private void handleSuccess(QuerySnapshot snap) {
        progressBar.setVisibility(View.GONE);

        if (snap == null || snap.isEmpty()) {
            txtTaskInfo.setText("📭 No tasks available right now.");
        } else {
            txtTaskInfo.setText("✅ Total tasks available: " + snap.size());
        }
    }

    private void handleFailure(@NonNull Exception e) {
        progressBar.setVisibility(View.GONE);
        String msg = (e instanceof FirebaseFirestoreException)
                ? ((FirebaseFirestoreException) e).getMessage()
                : e.getMessage();
        txtTaskInfo.setText("⚠️ Error loading tasks: " + msg);
    }
}