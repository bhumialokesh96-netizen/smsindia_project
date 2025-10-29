package com.smsindia.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smsindia.app.R;

public class WithdrawFragment extends Fragment {
    private TextView txtBalance;
    private Button btnWithdraw;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private double currentBalance = 0.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_withdraw, container, false);
        txtBalance = v.findViewById(R.id.txt_withdraw_balance);
        btnWithdraw = v.findViewById(R.id.btn_withdraw);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            txtBalance.setText("₹0.00");
            btnWithdraw.setEnabled(false);
            return v;
        }

        // Load balance
        db.collection("users").document(auth.getCurrentUser().getUid())
                .get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Object balObj = doc.get("balance");
                        if (balObj instanceof Number)
                            currentBalance = ((Number) balObj).doubleValue();
                        txtBalance.setText("₹" + String.format("%.2f", currentBalance));
                    }
                });

        btnWithdraw.setOnClickListener(view -> {
            if (currentBalance < 100) {
                Toast.makeText(requireContext(), "Minimum withdrawal ₹100", Toast.LENGTH_SHORT).show();
                return;
            }

            // Example: Create a withdrawal request
            db.collection("withdraw_requests").add(new WithdrawalRequest(auth.getCurrentUser().getUid(), currentBalance))
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(requireContext(), "Withdrawal request submitted!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        return v;
    }

    public static class WithdrawalRequest {
        public String userId;
        public double amount;
        public long timestamp;

        public WithdrawalRequest() {} // Firestore requires empty constructor
        public WithdrawalRequest(String userId, double amount) {
            this.userId = userId;
            this.amount = amount;
            this.timestamp = System.currentTimeMillis();
        }
    }
}