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
import androidx.appcompat.app.AlertDialog; // Import AlertDialog
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.tradeup.R;
import com.example.tradeup.model.User; // Import User model
import com.example.tradeup.utils.FirebaseHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap; // Import HashMap
import java.util.Map; // Import Map

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
                    if (user != null) {
                        // Authentication successful, now check account status in DB
                        firebaseHelper.getUserProfile(user.getUid(), new FirebaseHelper.DbReadCallback<User>() {
                            @Override
                            public void onSuccess(User userData) {
                                progressBar.setVisibility(View.GONE);
                                validateInputsAndUpdateButtonState(); // Re-enable button

                                if (userData != null && "inactive".equals(userData.getAccount_status())) {
                                    // Account is deactivated, show reactivate dialog
                                    showReactivateAccountDialog(user.getUid());
                                } else {
                                    // Account is active or status not found (assume active), proceed to create/update profile and then Main Activity
                                    handleSuccessfulLogin(user);
                                }
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                // In case of failure to read user profile, still proceed to create/update profile.
                                // It might be a new user, or a temporary database issue.
                                progressBar.setVisibility(View.GONE);
                                validateInputsAndUpdateButtonState();
                                Toast.makeText(LoginActivity.this, "Login successful, but failed to read profile status: " + errorMessage, Toast.LENGTH_LONG).show();
                                handleSuccessfulLogin(user); // Proceed as if active
                            }
                        });
                    }
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

    // New method to handle logic after successful Firebase Auth login
    private void handleSuccessfulLogin(FirebaseUser user) {
        // Authentication successful, now create/update user profile in DB
        firebaseHelper.createOrUpdateUserProfile(user, null, null, new FirebaseHelper.DbWriteCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                if (rememberMeCheckbox.isChecked()) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(PREF_REMEMBER_ME, true);
                    editor.putString(PREF_EMAIL, emailField.getText().toString().trim());
                    editor.putString(PREF_PASSWORD, passwordField.getText().toString().trim());
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
                Toast.makeText(LoginActivity.this, "Login successful but failed to save/update user data: " + errorMessage, Toast.LENGTH_LONG).show();
                // Even if data save fails, allow to proceed to MainActivity if login was successful
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
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
        progressBar.setVisibility(View.VISIBLE); // Show progress bar
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

                firebaseHelper.firebaseAuthWithGoogle(account.getIdToken(), new FirebaseHelper.AuthCallback() {
                    @Override
                    public void onSuccess(FirebaseUser user) {
                        if (user != null) {
                            // Google Auth successful, now check account status in DB
                            firebaseHelper.getUserProfile(user.getUid(), new FirebaseHelper.DbReadCallback<User>() {
                                @Override
                                public void onSuccess(User userData) {
                                    progressBar.setVisibility(View.GONE); // Hide progress bar
                                    if (userData != null && "inactive".equals(userData.getAccount_status())) {
                                        // Account is deactivated, show reactivate dialog
                                        showReactivateAccountDialog(user.getUid());
                                    } else {
                                        // Account is active or status not found (assume active), proceed to create/update profile and then Main Activity
                                        String firstName = account.getGivenName() != null ? account.getGivenName() : "";
                                        String lastName = account.getFamilyName() != null ? account.getFamilyName() : "";
                                        handleSuccessfulGoogleLogin(user, firstName, lastName);
                                    }
                                }

                                @Override
                                public void onFailure(String errorMessage) {
                                    // In case of failure to read user profile, still proceed to create/update profile.
                                    progressBar.setVisibility(View.GONE); // Hide progress bar
                                    Toast.makeText(LoginActivity.this, "Google login successful, but failed to read profile status: " + errorMessage, Toast.LENGTH_LONG).show();
                                    String firstName = account.getGivenName() != null ? account.getGivenName() : "";
                                    String lastName = account.getFamilyName() != null ? account.getFamilyName() : "";
                                    handleSuccessfulGoogleLogin(user, firstName, lastName); // Proceed as if active
                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        progressBar.setVisibility(View.GONE); // Hide progress bar
                        Toast.makeText(LoginActivity.this, "Google Sign-In failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (ApiException e) {
                progressBar.setVisibility(View.GONE); // Hide progress bar
                Toast.makeText(LoginActivity.this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // New method for handling successful Google sign-in and profile update
    private void handleSuccessfulGoogleLogin(FirebaseUser user, String firstName, String lastName) {
        firebaseHelper.createOrUpdateUserProfile(user, firstName, lastName, new FirebaseHelper.DbWriteCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, "Google login successful!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, "Google login successful but failed to save/update user data: " + errorMessage, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }


    // New method to show dialog for reactivating account
    private void showReactivateAccountDialog(String userId) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.account_deactivated_title))
                .setMessage(getString(R.string.account_deactivated_message))
                .setPositiveButton(getString(R.string.reactivate_button), (dialog, which) -> {
                    reactivateAccount(userId);
                })
                .setNegativeButton(getString(R.string.cancel_button), (dialog, which) -> {
                    firebaseHelper.signOut(); // Ensure user is signed out if they choose not to reactivate
                    Toast.makeText(this, getString(R.string.account_remains_deactivated), Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false) // Prevent dialog from being dismissed by tapping outside
                .show();
    }

    // New method to reactivate account
    private void reactivateAccount(String userId) {
        progressBar.setVisibility(View.VISIBLE); // Show progress bar during reactivation
        Map<String, Object> updates = new HashMap<>();
        updates.put("account_status", "active");
        firebaseHelper.updateUserProfileFields(userId, updates, new FirebaseHelper.DbWriteCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, getString(R.string.account_reactivated_success), Toast.LENGTH_SHORT).show();
                // After reactivation, proceed to MainActivity
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, getString(R.string.failed_to_reactivate_account) + errorMessage, Toast.LENGTH_LONG).show();
                firebaseHelper.signOut(); // Ensure user is signed out if reactivation fails
            }
        });
    }
}