package com.swornima.petcare;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.swornima.petcare.db.SessionManager;

/**
 * App launcher entry point. If the user already has an active session it
 * forwards straight to the Home dashboard (MainActivity); otherwise it shows
 * a simple welcome screen with Login / Sign Up buttons instead of forcing
 * the user directly into the login form.
 */
public class WelcomeActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        if (sessionManager.isLoggedIn()) {
            goToHome();
            return;
        }

        setContentView(R.layout.activity_welcome);

        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        MaterialButton btnSignUp = findViewById(R.id.btnSignUp);

        btnLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
        btnSignUp.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (sessionManager.isLoggedIn()) {
            goToHome();
        }
    }

    private void goToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
