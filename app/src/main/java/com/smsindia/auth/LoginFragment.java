package com.smsindia.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.smsindia.R;
import com.smsindia.app.MainActivity;

public class LoginFragment extends Fragment {
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_login, container, false);
        mAuth = FirebaseAuth.getInstance();

        TextInputEditText etEmail = v.findViewById(R.id.et_email);
        TextInputEditText etPassword = v.findViewById(R.id.et_password);
        MaterialButton btnLogin = v.findViewById(R.id.btn_login);
        MaterialButton btnGotoSignup = v.findViewById(R.id.btn_goto_signup);

        btnLogin.setOnClickListener(view -> {
            String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
            String pass = etPassword.getText() == null ? "" : etPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(requireContext(), "Enter email & password", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Login with Firebase
            mAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> {
                    startActivity(new Intent(requireContext(), MainActivity.class));
                    requireActivity().finish();
                })
                .addOnFailureListener(e ->
                    Toast.makeText(requireContext(), "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        // ✅ Open Signup Fragment
        btnGotoSignup.setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.auth_container, new SignupFragment())
                .addToBackStack(null)
                .commit();
        });

        return v;
    }
}