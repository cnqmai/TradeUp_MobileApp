package com.example.tradeup.features.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.tradeup.MainActivity;
import com.example.tradeup.R;

public class LoginActivity extends AppCompatActivity {

    private boolean isPasswordVisible = false;
    private EditText emailField, passwordField;
    private Button btnLogin;
    private CheckBox rememberPasswordCheck;

    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "loginPrefs";
    private static final String PREF_EMAIL = "email";
    private static final String PREF_PASSWORD = "password";
    private static final String PREF_REMEMBER = "remember";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.login);

        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        btnLogin = findViewById(R.id.btnLogin);
        rememberPasswordCheck = findViewById(R.id.rememberPasswordCheck);
        ImageView togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        loadSavedCredentials();

        btnLogin.setEnabled(false);
        btnLogin.setAlpha(0.5f);

        TextWatcher inputWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkInputFields();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        emailField.addTextChangedListener(inputWatcher);
        passwordField.addTextChangedListener(inputWatcher);

        togglePasswordVisibility.setOnClickListener(view -> {
            if (isPasswordVisible) {
                passwordField.setTransformationMethod(PasswordTransformationMethod.getInstance());
                togglePasswordVisibility.setImageResource(R.drawable.eye_closed);
            } else {
                passwordField.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                togglePasswordVisibility.setImageResource(R.drawable.eye_open);
            }
            isPasswordVisible = !isPasswordVisible;
            passwordField.setSelection(passwordField.getText().length());
        });

        btnLogin.setOnClickListener(view -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (email.equals("admin@gmail.com") && password.equals("123456")) {
                Toast.makeText(LoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();

                if (rememberPasswordCheck.isChecked()) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(PREF_EMAIL, email);
                    editor.putString(PREF_PASSWORD, password);
                    editor.putBoolean(PREF_REMEMBER, true);
                    editor.apply();
                } else {
                    sharedPreferences.edit().clear().apply();
                }

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "Email hoặc mật khẩu không đúng", Toast.LENGTH_SHORT).show();
            }
        });

        ConstraintLayout googleBtn = findViewById(R.id.btnGoogleSignIn);
        googleBtn.setOnClickListener(view -> {
            Toast.makeText(LoginActivity.this, "Google Sign-In Clicked", Toast.LENGTH_SHORT).show();
        });
    }

    private void checkInputFields() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (!email.isEmpty() && !password.isEmpty()) {
            btnLogin.setEnabled(true);
            btnLogin.setAlpha(1f);
        } else {
            btnLogin.setEnabled(false);
            btnLogin.setAlpha(0.5f);
        }
    }

    private void loadSavedCredentials() {
        boolean remember = sharedPreferences.getBoolean(PREF_REMEMBER, false);
        if (remember) {
            String savedEmail = sharedPreferences.getString(PREF_EMAIL, "");
            String savedPassword = sharedPreferences.getString(PREF_PASSWORD, "");
            emailField.setText(savedEmail);
            passwordField.setText(savedPassword);
            rememberPasswordCheck.setChecked(true);
            checkInputFields(); // bật nút nếu có dữ liệu
        }
    }
}
