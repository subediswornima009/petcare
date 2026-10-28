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
import com.google.android.material.textfield.TextInputLayout;
import com.swornima.petcare.db.AppDatabase;
import com.swornima.petcare.db.PasswordHasher;
import com.swornima.petcare.db.UserEntity;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * New account registration screen. Creates a locally stored user account
 * (Room) with a salted password hash - replaces Firebase Authentication +
 * the Firestore users/{uid} profile document.
 */
public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etFirstName, etLastName, etEmail, etPassword, etConfirmPassword;
    private TextInputLayout firstNameLayout, lastNameLayout, emailLayout, passwordLayout, confirmPasswordLayout;
    private MaterialButton btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = AppDatabase.getInstance(this);

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        firstNameLayout = findViewById(R.id.tilFirstName);
        lastNameLayout = findViewById(R.id.tilLastName);
        emailLayout = findViewById(R.id.tilEmail);
        passwordLayout = findViewById(R.id.tilPassword);
        confirmPasswordLayout = findViewById(R.id.tilConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);

        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String firstName = String.valueOf(etFirstName.getText()).trim();
                String lastName = String.valueOf(etLastName.getText()).trim();
                String email = String.valueOf(etEmail.getText()).trim();
                String password = String.valueOf(etPassword.getText());
                String confirmPassword = String.valueOf(etConfirmPassword.getText());

                clearValidationErrors();
                if (!validateRegistrationInput(firstName, lastName, email, password, confirmPassword)) return;

                progressBar.setVisibility(View.VISIBLE);
                registerUser(firstName, lastName, email, password);
            }
        });
    }

    private boolean validateRegistrationInput(String firstName, String lastName, String email,
                                              String password, String confirmPassword) {
        if (TextUtils.isEmpty(firstName)) {
            showValidationMessage(firstNameLayout, "Enter first name");
            etFirstName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(lastName)) {
            showValidationMessage(lastNameLayout, "Enter last name");
            etLastName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            showValidationMessage(emailLayout, "Enter email");
            etEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            showValidationMessage(passwordLayout, "Enter password");
            etPassword.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            showValidationMessage(confirmPasswordLayout, "Confirm your password");
            etConfirmPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            showValidationMessage(confirmPasswordLayout, "Passwords do not match");
            etConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void showValidationMessage(TextInputLayout field, String message) {
        progressBar.setVisibility(View.GONE);
        field.setError(message);
    }

    private void clearValidationErrors() {
        firstNameLayout.setError(null);
        lastNameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        confirmPasswordLayout.setError(null);
    }

    private void registerUser(String firstName, String lastName, String email, String password) {
        executor.execute(() -> {
            if (db.userDao().countByEmail(email) > 0) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(RegisterActivity.this, "An account with this email already exists.", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            UserEntity user = new UserEntity();
            user.uid = UUID.randomUUID().toString();
            user.email = email;
            user.passwordHash = PasswordHasher.hash(password);
            user.firstName = firstName;
            user.lastName = lastName;
            user.fullName = firstName + " " + lastName;
            db.userDao().insert(user);

            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(RegisterActivity.this, "Account Created. Please Login.", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            });
        });
    }
}
