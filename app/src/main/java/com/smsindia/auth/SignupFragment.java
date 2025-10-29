package com.smsindia.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.button.MaterialButton;

import com.smsindia.app.MainActivity;
import com.smsindia.app.R;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SignupFragment extends Fragment {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_signup, container, false);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        TextInputEditText etName = v.findViewById(R.id.et_name);
        TextInputEditText etEmail = v.findViewById(R.id.et_email);
        TextInputEditText etPassword = v.findViewById(R.id.et_password);
        TextInputEditText etReferral = v.findViewById(R.id.et_referral);
        MaterialButton btnSignup = v.findViewById(R.id.btn_signup);

        btnSignup.setOnClickListener(view -> {
            String name = etName.getText() == null ? "" : etName.getText().toString().trim();
            String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
            String pass = etPassword.getText() == null ? "" : etPassword.getText().toString().trim();
            String refCode = etReferral.getText() == null ? "" : etReferral.getText().toString().trim();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
                Toast.makeText(requireContext(), "All fields required", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(authResult -> {
                        String uid = mAuth.getCurrentUser().getUid();
                        String myReferral = generateReferralCode();
                        Map<String, Object> udoc = new HashMap<>();
                        udoc.put("name", name);
                        udoc.put("email", email);
                        udoc.put("balance", 0.0);
                        udoc.put("dailySent", 0L);
                        udoc.put("spins", 0L);
                        udoc.put("simId", -1L);
                        udoc.put("referralCode", myReferral);
                        udoc.put("referredBy", TextUtils.isEmpty(refCode) ? null : refCode);

                        db.collection("users").document(uid).set(udoc)
                                .addOnSuccessListener(aVoid -> {
                                    // If used a referrer code, credit the referrer with signup bonus (e.g., ₹5)
                                    if (!TextUtils.isEmpty(refCode)) {
                                        db.collection("users").whereEqualTo("referralCode", refCode).limit(1).get()
                                                .addOnSuccessListener(query -> {
                                                    if (!query.isEmpty()) {
                                                        String refUid = query.getDocuments().get(0).getId();
                                                        db.collection("users").document(refUid)
                                                                .update("balance", com.google.firebase.firestore.FieldValue.increment(5.0))
                                                                .addOnSuccessListener(__ ->
                                                                        Toast.makeText(requireContext(), "Referral bonus credited", Toast.LENGTH_SHORT).show());
                                                    }
                                                });
                                    }

                                    startActivity(new Intent(requireContext(), MainActivity.class));
                                    requireActivity().finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(requireContext(), "Failed to create user doc: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Signup failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        return v;
    }

    private String generateReferralCode() {
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // avoid ambiguous chars
        for (int i = 0; i < 6; i++) sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
        return sb.toString().toUpperCase(Locale.US);
    }
}