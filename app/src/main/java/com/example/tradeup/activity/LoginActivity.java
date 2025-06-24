package com.example.tradeup.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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

public class LoginActivity extends AppCompatActivity {

    private boolean isPasswordVisible = false;
    private EditText emailField, passwordField;
    private Button loginBtn;
    private ConstraintLayout btnGoogleSignIn;
    private TextView registerLink, forgotPasswordLink;
    private ProgressBar progressBar;
    private CheckBox rememberMeCheckbox;
    private ImageView togglePasswordVisibility;

    private FirebaseHelper firebaseHelper;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String PREF_REMEMBER_ME = "remember_me";
    private static final String PREF_EMAIL = "email";
    private static final String PREF_PASSWORD = "password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        firebaseHelper = new FirebaseHelper();

        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        loginBtn = findViewById(R.id.btnLogin);
        registerLink = findViewById(R.id.registerLink);
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink);
        progressBar = findViewById(R.id.progressBar);
        rememberMeCheckbox = findViewById(R.id.rememberPasswordCheck);
        togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        if (sharedPreferences.getBoolean(PREF_REMEMBER_ME, false)) {
            emailField.setText(sharedPreferences.getString(PREF_EMAIL, ""));
            passwordField.setText(sharedPreferences.getString(PREF_PASSWORD, ""));
            rememberMeCheckbox.setChecked(true);
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Disable login button initially
        loginBtn.setEnabled(false);

        // Add TextWatcher for emailField
        emailField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // When text changes, validate inputs and update button state
                validateInputsAndUpdateButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Add TextWatcher for passwordField
        passwordField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // When text changes, validate inputs and update button state
                validateInputsAndUpdateButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loginBtn.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            // Final validation before attempting login
            if (!isValidEmail(email)) {
                emailField.setError("Please enter a valid email.");
                emailField.requestFocus();
                return;
            }
            if (!isValidPassword(password)) {
                passwordField.setError("Password must be at least 6 characters long.");
                passwordField.requestFocus();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            loginBtn.setEnabled(false); // Disable button during processing

            firebaseHelper.loginUser(email, password, new FirebaseHelper.AuthCallback() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    // Authentication successful, now create/update user profile in DB
                    firebaseHelper.createOrUpdateUserProfile(user, null, null, new FirebaseHelper.DbWriteCallback() {
                        @Override
                        public void onSuccess() {
                            progressBar.setVisibility(View.GONE);
                            // Re-enable button after processing (if needed for other operations,
                            // or it will automatically disable if fields don't meet conditions)
                            validateInputsAndUpdateButtonState();
                            Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();

                            if (rememberMeCheckbox.isChecked()) {
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean(PREF_REMEMBER_ME, true);
                                editor.putString(PREF_EMAIL, email);
                                editor.putString(PREF_PASSWORD, password);
                                editor.apply();
                            } else {
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean(PREF_REMEMBER_ME, false);
                                editor.remove(PREF_EMAIL);
                                editor.remove(PREF_PASSWORD);
                                editor.apply();
                            }

                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            progressBar.setVisibility(View.GONE);
                            // Re-enable button after processing
                            validateInputsAndUpdateButtonState();
                            Toast.makeText(LoginActivity.this, "Login successful but failed to save data: " + errorMessage, Toast.LENGTH_SHORT).show();
                            // Even if data save fails, allow to proceed to MainActivity if login was successful
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    progressBar.setVisibility(View.GONE);
                    validateInputsAndUpdateButtonState(); // Re-enable button after failure
                    Toast.makeText(LoginActivity.this, "Login error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });

        registerLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        forgotPasswordLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        togglePasswordVisibility.setOnClickListener(v -> {
            if (isPasswordVisible) {
                // Currently visible, switch to hidden
                passwordField.setTransformationMethod(PasswordTransformationMethod.getInstance());
                togglePasswordVisibility.setImageResource(R.drawable.ic_visibility_off);
            } else {
                // Currently hidden, switch to visible
                passwordField.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                togglePasswordVisibility.setImageResource(R.drawable.ic_visibility_on);
            }
            isPasswordVisible = !isPasswordVisible; // Toggle the state
            passwordField.setSelection(passwordField.getText().length());
        });

        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        // Call initially to set the login button's initial state
        validateInputsAndUpdateButtonState();
    }

    // Function to validate email
    private boolean isValidEmail(String email) {
        return !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    // Function to validate password
    private boolean isValidPassword(String password) {
        return !password.isEmpty() && password.length() >= 6;
    }

    // New function to validate all inputs and update button state
    private void validateInputsAndUpdateButtonState() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        boolean isEmailValid = isValidEmail(email);
        boolean isPasswordValid = isValidPassword(password);

        // Only enable the button if both email and password are valid
        loginBtn.setEnabled(isEmailValid && isPasswordValid);
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
                        String firstName = account.getGivenName() != null ? account.getGivenName() : "";
                        String lastName = account.getFamilyName() != null ? account.getFamilyName() : "";

                        firebaseHelper.createOrUpdateUserProfile(user, firstName, lastName, new FirebaseHelper.DbWriteCallback() {
                            @Override
                            public void onSuccess() {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(LoginActivity.this, "Google login successful", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(LoginActivity.this, "Google login successful but failed to save data: " + errorMessage, Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Google Sign-In failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (ApiException e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}