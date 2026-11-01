package com.example.udgh.activities;

import com.example.udgh.R;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.udgh.viewmodels.LoginViewModel;
import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {

    private EditText edtUsername, edtPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private TextView tvGoToRegister, tvForgotPassword;
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        observeViewModel();

        btnLogin.setOnClickListener(v -> loginUser());
        tvGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, loading -> setLoadingState(Boolean.TRUE.equals(loading)));

        viewModel.getRole().observe(this, role -> {
            if (TextUtils.isEmpty(role)) return;

            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
            Class<?> destination;
            if ("ADMIN".equals(role)) {
                destination = MainActivity.class;
            } else if ("CREATOR".equals(role)) {
                destination = HomeCreatorActivity.class;
            } else if ("SHIPPER".equals(role)) {
                destination = HomeShipperActivity.class;
            } else {
                Toast.makeText(this, "Vai trò tài khoản không hợp lệ", Toast.LENGTH_LONG).show();
                return;
            }

            startActivity(new Intent(this, destination));
            finish();
        });

        viewModel.getMessage().observe(this, message -> {
            if (!TextUtils.isEmpty(message)) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loginUser() {
        String email = edtUsername.getText() == null ? "" : edtUsername.getText().toString().trim();
        String password = edtPassword.getText() == null ? "" : edtPassword.getText().toString();

        if (TextUtils.isEmpty(email)) {
            edtUsername.setError("Vui lòng nhập Email");
            edtUsername.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtUsername.setError("Email không hợp lệ");
            edtUsername.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Vui lòng nhập mật khẩu");
            edtPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            edtPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            edtPassword.requestFocus();
            return;
        }

        viewModel.login(email, password);
    }

    private void setLoadingState(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setText(isLoading ? "" : "Đăng nhập");
        btnLogin.setEnabled(!isLoading);
        edtUsername.setEnabled(!isLoading);
        edtPassword.setEnabled(!isLoading);
    }

    private void showForgotPasswordDialog() {
        EditText edtEmail = new EditText(this);
        edtEmail.setHint("Nhập Email của bạn");
        edtEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        edtEmail.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Khôi phục mật khẩu")
                .setMessage("Nhập địa chỉ Email đã đăng ký. Chúng tôi sẽ gửi một liên kết để đặt lại mật khẩu.")
                .setView(edtEmail)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String email = edtEmail.getText().toString().trim();
                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(this, "Vui lòng nhập Email!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(this, "Định dạng Email không hợp lệ!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.sendPasswordReset(email);
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
