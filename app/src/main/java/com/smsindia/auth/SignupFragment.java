package com.smsindia.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smsindia.app.MainActivity;
import com.smsindia.app.R;

import java.util.HashMap;
import java.util.Map;

public class SignupFragment extends Fragment {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_signup, container, false);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        TextInputEditText etEmail = v.findViewById(R.id.et_email);
        TextInputEditText etPassword = v.findViewById(R.id.et_password);
        TextInputEditText etName = v.findViewById(R.id.et_name);
        MaterialButton btnSignup = v.findViewById(R.id.btn_signup);
        MaterialButton btnGotoLogin = v.findViewById(R.id.btn_goto_login);

        btnSignup.setOnClickListener(view -> {
            String name = etName.getText() == null ? "" : etName.getText().toString().trim();
            String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
            String pass = etPassword.getText() == null ? "" : etPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Create user in Firebase Authentication
            mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("name", name);
                    userData.put("email", email);
                    userData.put("balance", 0.0);
                    userData.put("dailySent", 0);
                    userData.put("sendingRunning", false);

                    // ✅ Save profile in Firestore
                    db.collection("users").document(uid)
                        .set(userData)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(requireContext(), "Signup successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(requireContext(), MainActivity.class));
                            requireActivity().finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(requireContext(), "Error saving profile", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                    Toast.makeText(requireContext(), "Signup failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        btnGotoLogin.setOnClickListener(view ->
            requireActivity().getSupportFragmentManager().popBackStack()
        );

        return v;
    }
}