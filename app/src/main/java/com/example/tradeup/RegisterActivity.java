package com.example.tradeup;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.constraintlayout.widget.ConstraintLayout;

public class RegisterActivity extends AppCompatActivity {

    private boolean isPasswordVisible = false;
    private EditText firstNameField, lastNameField, emailField, passwordField;
    private Button btnSignUp;
    private TextView signUpLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.register);

        // Khởi tạo các view
        firstNameField = findViewById(R.id.editTextText);
        lastNameField = findViewById(R.id.editTextText2);
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        btnSignUp = findViewById(R.id.btnLogin); // Nút đăng ký
        ImageView togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility);
        signUpLink = findViewById(R.id.signUpLink);
        ConstraintLayout googleBtn = findViewById(R.id.btnGoogleSignIn);

        // Mặc định disable nút đăng ký
        btnSignUp.setEnabled(false);
        btnSignUp.setAlpha(0.5f); // Làm mờ nút

        // Theo dõi thay đổi trong các trường nhập liệu
        TextWatcher inputWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkInputFields();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        firstNameField.addTextChangedListener(inputWatcher);
        lastNameField.addTextChangedListener(inputWatcher);
        emailField.addTextChangedListener(inputWatcher);
        passwordField.addTextChangedListener(inputWatcher);

        // Toggle hiện/ẩn mật khẩu
        togglePasswordVisibility.setOnClickListener(view -> {
            if (isPasswordVisible) {
                passwordField.setTransformationMethod(PasswordTransformationMethod.getInstance());
                togglePasswordVisibility.setImageResource(R.drawable.eye_closed);
            } else {
                passwordField.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                togglePasswordVisibility.setImageResource(R.drawable.eye_open);
            }
            isPasswordVisible = !isPasswordVisible;
            passwordField.setSelection(passwordField.getText().length()); // Giữ con trỏ ở cuối
        });

        // Xử lý nút đăng ký
        btnSignUp.setOnClickListener(view -> {
            String firstName = firstNameField.getText().toString().trim();
            String lastName = lastNameField.getText().toString().trim();
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            // Giả lập kiểm tra đăng ký (thay bằng API thật sau này)
            if (!firstName.isEmpty() && !lastName.isEmpty() && isValidEmail(email) && password.length() >= 6) {
                Toast.makeText(RegisterActivity.this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                startActivity(intent);
                finish(); // Đóng màn hình đăng ký
            } else {
                String errorMessage = "";
                if (firstName.isEmpty() || lastName.isEmpty()) {
                    errorMessage = "Vui lòng nhập đầy đủ họ và tên";
                } else if (!isValidEmail(email)) {
                    errorMessage = "Email không hợp lệ";
                } else if (password.length() < 6) {
                    errorMessage = "Mật khẩu phải có ít nhất 6 ký tự";
                }
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        // Xử lý nút Google Sign-In
        googleBtn.setOnClickListener(view -> {
            // TODO: Thêm logic đăng ký Google ở đây
            Toast.makeText(RegisterActivity.this, "Google Sign-Up Clicked", Toast.LENGTH_SHORT).show();
        });

        // Xử lý liên kết chuyển đến màn hình đăng nhập
        signUpLink.setOnClickListener(view -> {
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    private void checkInputFields() {
        String firstName = firstNameField.getText().toString().trim();
        String lastName = lastNameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (!firstName.isEmpty() && !lastName.isEmpty() && !email.isEmpty() && !password.isEmpty()) {
            btnSignUp.setEnabled(true);
            btnSignUp.setAlpha(1f); // Bỏ làm mờ
        } else {
            btnSignUp.setEnabled(false);
            btnSignUp.setAlpha(0.5f); // Làm mờ
        }
    }

    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        return email.matches(emailPattern);
    }
}