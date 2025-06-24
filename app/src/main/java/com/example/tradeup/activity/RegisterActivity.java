package com.example.tradeup.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.tradeup.R;
import com.example.tradeup.utils.FirebaseHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private EditText firstNameField, lastNameField, emailField, passwordField, confirmPasswordField;
    private Button registerBtn;
    private ConstraintLayout btnGoogleSignIn;
    private TextView loginLink;
    private ProgressBar progressBar;
    private ImageView togglePasswordVisibility, toggleConfirmPasswordVisibility;

    private FirebaseHelper firebaseHelper;
    private GoogleSignInClient mGoogleSignInClient;

    private static final String TAG = "RegisterActivity";
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseHelper = new FirebaseHelper();

        firstNameField = findViewById(R.id.firstNameField);
        lastNameField = findViewById(R.id.lastNameField);
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        confirmPasswordField = findViewById(R.id.confirmPasswordField);
        registerBtn = findViewById(R.id.btnSignUp);
        loginLink = findViewById(R.id.loginLink);
        progressBar = findViewById(R.id.progressBar);
        togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility);
        toggleConfirmPasswordVisibility = findViewById(R.id.toggleConfirmPasswordVisibility);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // --- Original logic for TextWatchers (kept as per request) ---
        firstNameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() < 1) {
                    firstNameField.setError("First name cannot be empty.");
                } else {
                    firstNameField.setError(null);
                }
                // Removed validateInputsAndUpdateButtonState() call as per request
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        lastNameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() < 1) {
                    lastNameField.setError("Last name cannot be empty.");
                } else {
                    lastNameField.setError(null);
                }
                // Removed validateInputsAndUpdateButtonState() call as per request
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        emailField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!Patterns.EMAIL_ADDRESS.matcher(s).matches()) {
                    emailField.setError("Invalid email address.");
                } else {
                    emailField.setError(null);
                }
                // Removed validateInputsAndUpdateButtonState() call as per request
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        passwordField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() < 6) {
                    passwordField.setError("Password must be at least 6 characters long.");
                } else {
                    passwordField.setError(null);
                }
                validatePasswords(); // Validate password match
                // Removed validateInputsAndUpdateButtonState() call as per request
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        confirmPasswordField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePasswords(); // Validate password match
                // Removed validateInputsAndUpdateButtonState() call as per request
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        // --- End of original TextWatcher logic ---

        togglePasswordVisibility.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible; // Toggle the state
            if (isPasswordVisible) {
                // If now visible, set transformation to show text
                passwordField.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                togglePasswordVisibility.setImageResource(R.drawable.ic_visibility_on);
            } else {
                // If now hidden, set transformation to hide text
                passwordField.setTransformationMethod(PasswordTransformationMethod.getInstance());
                togglePasswordVisibility.setImageResource(R.drawable.ic_visibility_off);
            }
            passwordField.setSelection(passwordField.getText().length()); // Keep cursor at end
        });

        // FIXED: Corrected the logic for toggleConfirmPasswordVisibility
        toggleConfirmPasswordVisibility.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible; // Toggle the state
            if (isConfirmPasswordVisible) {
                // If now visible, set transformation to show text
                confirmPasswordField.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                toggleConfirmPasswordVisibility.setImageResource(R.drawable.ic_visibility_on);
            } else {
                // If now hidden, set transformation to hide text
                confirmPasswordField.setTransformationMethod(PasswordTransformationMethod.getInstance());
                toggleConfirmPasswordVisibility.setImageResource(R.drawable.ic_visibility_off);
            }
            confirmPasswordField.setSelection(confirmPasswordField.getText().length()); // Keep cursor at end
        });

        registerBtn.setOnClickListener(v -> registerUser());

        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        // Removed the initial call to validateInputsAndUpdateButtonState() and related logic
        // registerBtn.setEnabled(false); and validateInputsAndUpdateButtonState(); are removed
    }

    private void validatePasswords() {
        String password = passwordField.getText().toString();
        String confirmPassword = confirmPasswordField.getText().toString();

        if (!password.equals(confirmPassword)) {
            confirmPasswordField.setError("Passwords do not match.");
        } else {
            confirmPasswordField.setError(null);
        }
    }

    // Removed this function as per request, as button will not be disabled based on input validation.
    /*
    private void validateInputsAndUpdateButtonState() {
        String firstName = firstNameField.getText().toString().trim();
        String lastName = lastNameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String confirmPassword = confirmPasswordField.getText().toString().trim();

        boolean isFirstNameValid = !firstName.isEmpty();
        boolean isLastNameValid = !lastName.isEmpty();
        boolean isEmailValid = !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
        boolean isPasswordValid = !password.isEmpty() && password.length() >= 6;
        boolean isConfirmPasswordValid = password.equals(confirmPassword);

        registerBtn.setEnabled(isFirstNameValid && isLastNameValid && isEmailValid && isPasswordValid && isConfirmPasswordValid);
    }
    */


    private void registerUser() {
        String firstName = firstNameField.getText().toString().trim();
        String lastName = lastNameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String confirmPassword = confirmPasswordField.getText().toString().trim();

        // Final validation before registration attempt
        // These checks also provide immediate feedback to the user
        if (firstName.isEmpty()) {
            firstNameField.setError("First name cannot be empty.");
            firstNameField.requestFocus();
            return;
        }
        if (lastName.isEmpty()) {
            lastNameField.setError("Last name cannot be empty.");
            lastNameField.requestFocus();
            return;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.setError("Please enter a valid email.");
            emailField.requestFocus();
            return;
        }
        if (password.isEmpty() || password.length() < 6) {
            passwordField.setError("Password must be at least 6 characters long.");
            passwordField.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordField.setError("Passwords do not match.");
            confirmPasswordField.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        registerBtn.setEnabled(false); // Disable button during processing

        firebaseHelper.registerUser(email, password, new FirebaseHelper.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                if (user != null) {
                    firebaseHelper.createOrUpdateUserProfile(user, firstName, lastName, new FirebaseHelper.DbWriteCallback() {
                        @Override
                        public void onSuccess() {
                            progressBar.setVisibility(View.GONE);
                            registerBtn.setEnabled(true); // Re-enable button
                            Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();

                            if (!user.isEmailVerified()) {
                                Intent intent = new Intent(RegisterActivity.this, VerifyEmailActivity.class);
                                startActivity(intent);
                            } else {
                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            }
                            finish();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            progressBar.setVisibility(View.GONE);
                            registerBtn.setEnabled(true); // Re-enable button
                            Log.e(TAG, "Failed to save user data after registration: " + errorMessage);
                            Toast.makeText(RegisterActivity.this, "Registration successful but failed to save data: " + errorMessage, Toast.LENGTH_LONG).show();
                            // Even if data save fails, allow to proceed if the user was created in Auth
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            finish();
                        }
                    });
                } else {
                    progressBar.setVisibility(View.GONE);
                    registerBtn.setEnabled(true); // Re-enable button
                    Toast.makeText(RegisterActivity.this, "Registration failed: Could not retrieve user information", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                registerBtn.setEnabled(true); // Re-enable button
                Log.e(TAG, "Registration failed: " + errorMessage);
                Toast.makeText(RegisterActivity.this, "Registration failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                progressBar.setVisibility(View.VISIBLE);
                firebaseHelper.firebaseAuthWithGoogle(account.getIdToken(), new FirebaseHelper.AuthCallback() {
                    @Override
                    public void onSuccess(FirebaseUser user) {
                        if (user != null) {
                            String googleFirstName = account.getGivenName() != null ? account.getGivenName() : "";
                            String googleLastName = account.getFamilyName() != null ? account.getFamilyName() : "";

                            firebaseHelper.createOrUpdateUserProfile(user, googleFirstName, googleLastName, new FirebaseHelper.DbWriteCallback() {
                                @Override
                                public void onSuccess() {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(RegisterActivity.this, "Google Sign-Up/Login successful", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                    finish();
                                }

                                @Override
                                public void onFailure(String errorMessage) {
                                    progressBar.setVisibility(View.GONE);
                                    Log.e(TAG, "Google Sign-In successful, but failed to save user data: " + errorMessage);
                                    Toast.makeText(RegisterActivity.this, "Google Sign-In successful but failed to save data: " + errorMessage, Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                    finish();
                                }
                            });
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Log.e(TAG, "FirebaseUser is null after Google Sign-In.");
                            Toast.makeText(RegisterActivity.this, "Error: Could not retrieve Google user information after login.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Google Sign-In failed via FirebaseHelper: " + errorMessage);
                        Toast.makeText(RegisterActivity.this, "Google Sign-In failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (ApiException e) {
                progressBar.setVisibility(View.GONE);
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(RegisterActivity.this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}