package com.swornima.petcare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.swornima.petcare.db.AppDatabase;
import com.swornima.petcare.db.PasswordHasher;
import com.swornima.petcare.db.SessionManager;
import com.swornima.petcare.db.UserEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User login screen. Part of the core "User registration and login"
 * requirement - authenticates an existing PetCare account against the local
 * Room database.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvRegister;
    private ProgressBar progressBar;

    private AppDatabase db;
    private SessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onStart() {
        super.onStart();
        if (sessionManager.isLoggedIn()) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = AppDatabase.getInstance(this);
        sessionManager = new SessionManager(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        progressBar = findViewById(R.id.progressBar);

        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                String email = String.valueOf(etEmail.getText()).trim();
                String password = String.valueOf(etPassword.getText());

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(LoginActivity.this, "Enter email", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(LoginActivity.this, "Enter password", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                attemptLogin(email, password);
            }
        });
    }

    private void attemptLogin(String email, String password) {
        executor.execute(() -> {
            UserEntity user = db.userDao().findByEmail(email);
            boolean userExists = user != null;
            boolean success = userExists && PasswordHasher.matches(password, user.passwordHash);

            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                if (success) {
                    etEmail.setError(null);
                    etPassword.setError(null);
                    sessionManager.setCurrentUserId(user.uid);
                    Toast.makeText(LoginActivity.this, "Login Successful.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                } else if (!userExists) {
                    etEmail.setError("User does not exist");
                    Toast.makeText(LoginActivity.this, "User does not exist.", Toast.LENGTH_SHORT).show();
                } else {
                    etPassword.setError("Wrong username or password");
                    Toast.makeText(LoginActivity.this, "Wrong username or password.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
