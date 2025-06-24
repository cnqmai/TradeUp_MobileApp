package com.example.tradeup.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tradeup.R;
import com.example.tradeup.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VerifyEmailActivity extends AppCompatActivity {

    private Button btnCheckVerification;
    private ProgressBar progressBar;
    private TextView mailAccTxt, resendTxt, backToLoginLink;
    private FirebaseHelper firebaseHelper;
    private FirebaseAuth mAuth; // Still need FirebaseAuth to get the current user

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);

        firebaseHelper = new FirebaseHelper();
        mAuth = FirebaseAuth.getInstance();

        btnCheckVerification = findViewById(R.id.btnSendVerification); // Using existing ID
        progressBar = findViewById(R.id.progressBar);
        mailAccTxt = findViewById(R.id.mailAccTxt);
        resendTxt = findViewById(R.id.resendTxt);
        backToLoginLink = findViewById(R.id.backToLoginLink);

        // Display user's email if passed via Intent
        String userEmail = getIntent().getStringExtra("userEmail");
        if (userEmail != null && !userEmail.isEmpty()) {
            mailAccTxt.setText(userEmail);
        } else {
            // Fallback: if email not passed, try to get from current FirebaseUser
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                mailAccTxt.setText(currentUser.getEmail());
            } else {
                mailAccTxt.setText("Email not found.");
            }
        }

        btnCheckVerification.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            btnCheckVerification.setEnabled(false);

            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                user.reload().addOnCompleteListener(task -> { // Reload user to get latest verification status
                    progressBar.setVisibility(View.GONE);
                    btnCheckVerification.setEnabled(true);

                    if (task.isSuccessful() && user.isEmailVerified()) {
                        // Email is verified in Firebase Auth, now update Realtime Database
                        // Using DbWriteCallback here
                        firebaseHelper.updateEmailVerificationStatus(user.getUid(), true, new FirebaseHelper.DbWriteCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(VerifyEmailActivity.this, "Email successfully verified! Redirecting...", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(VerifyEmailActivity.this, MainActivity.class));
                                finish();
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                // Email verified in Auth, but failed to update DB. Log and proceed.
                                Toast.makeText(VerifyEmailActivity.this, "Email verified successfully, but failed to update database: " + errorMessage, Toast.LENGTH_LONG).show();
                                startActivity(new Intent(VerifyEmailActivity.this, MainActivity.class));
                                finish();
                            }
                        });
                    } else {
                        Toast.makeText(VerifyEmailActivity.this, "Email not yet verified. Please check your inbox.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                progressBar.setVisibility(View.GONE);
                btnCheckVerification.setEnabled(true);
                Toast.makeText(VerifyEmailActivity.this, "Error: No user logged in. Please log in again.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(VerifyEmailActivity.this, LoginActivity.class));
                finish();
            }
        });

        resendTxt.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            resendTxt.setEnabled(false);

            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                firebaseHelper.sendEmailVerification(user, new FirebaseHelper.AuthCallback() { // Still using AuthCallback
                    @Override
                    public void onSuccess(FirebaseUser user) {
                        progressBar.setVisibility(View.GONE);
                        resendTxt.setEnabled(true);
                        Toast.makeText(VerifyEmailActivity.this, "Verification email has been re-sent.", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        progressBar.setVisibility(View.GONE);
                        resendTxt.setEnabled(true);
                        Toast.makeText(VerifyEmailActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                progressBar.setVisibility(View.GONE);
                resendTxt.setEnabled(true);
                Toast.makeText(VerifyEmailActivity.this, "Error: No user logged in. Please log in again.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(VerifyEmailActivity.this, LoginActivity.class));
                finish();
            }
        });

        backToLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(VerifyEmailActivity.this, LoginActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = mAuth.getCurrentUser();
        // It's good practice to reload the user here to check for verification status
        // if the user returns to this activity after verifying email in their mail app.
        if (user != null) {
            user.reload();
        }
    }
}