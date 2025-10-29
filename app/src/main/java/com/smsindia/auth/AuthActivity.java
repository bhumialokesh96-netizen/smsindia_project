package com.rupeedesk7.smsapp.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.rupeedesk7.smsapp.MainActivity;

public class AuthActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            // already signed in -> go to main
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Attach fragments (simple container switches). Use login fragment by default
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.auth_container, new LoginFragment())
                .commit();
        }
    }
}