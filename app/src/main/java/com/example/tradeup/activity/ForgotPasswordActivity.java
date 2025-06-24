package com.example.tradeup.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tradeup.R;
import com.example.tradeup.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseUser;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailField;
    private Button btnSendResetEmail;
    private ProgressBar progressBar;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        // Initialize FirebaseHelper
        firebaseHelper = new FirebaseHelper();

        // Initialize UI components
        emailField = findViewById(R.id.emailField);
        btnSendResetEmail = findViewById(R.id.btnSendResetEmail);
        progressBar = findViewById(R.id.progressBar);
        TextView backToLoginLink = findViewById(R.id.backToLoginLink);

        // Send reset email button click listener
        btnSendResetEmail.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailField.setError("Please enter a valid email address.");
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            btnSendResetEmail.setEnabled(false);

            firebaseHelper.sendPasswordResetEmail(email, new FirebaseHelper.AuthCallback() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    progressBar.setVisibility(View.GONE);
                    btnSendResetEmail.setEnabled(true);
                    Toast.makeText(ForgotPasswordActivity.this, "Password reset email sent.", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(String errorMessage) {
                    progressBar.setVisibility(View.GONE);
                    btnSendResetEmail.setEnabled(true);
                    Toast.makeText(ForgotPasswordActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Back to login link click listener
        backToLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
            finish();
        });
    }
}