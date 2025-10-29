package com.smsindia.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smsindia.app.R;

public class ProfileFragment extends Fragment {
    private TextView txtBalance, txtEmail;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);
        txtBalance = v.findViewById(R.id.txt_balance);
        txtEmail = v.findViewById(R.id.txt_email);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            txtEmail.setText("Not logged in");
            txtBalance.setText("₹0.00");
            return v;
        }

        txtEmail.setText(auth.getCurrentUser().getEmail());

        db.collection("users").document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Object balObj = doc.get("balance");
                        double bal = 0.0;
                        if (balObj instanceof Number) bal = ((Number) balObj).doubleValue();
                        txtBalance.setText("₹" + String.format("%.2f", bal));
                    } else {
                        txtBalance.setText("₹0.00");
                    }
                })
                .addOnFailureListener(e -> txtBalance.setText("Error"));
        return v;
    }
}