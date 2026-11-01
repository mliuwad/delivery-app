package com.example.udgh;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.udgh.viewmodels.RegistrationViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {
    private ImageButton btnBack;
    private TextInputEditText edtFullName, edtEmail, edtPassword, edtConfirmPassword;
    private RadioGroup rgRole;
    private MaterialButton btnRegister;
    private TextView txtLogin;
    private RegistrationViewModel viewModel;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        btnBack = findViewById(R.id.btnBack);
        edtFullName = findViewById(R.id.edtFullName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        rgRole = findViewById(R.id.rgRole);
        btnRegister = findViewById(R.id.btnRegister);
        txtLogin = findViewById(R.id.txtLogin);

        viewModel = new ViewModelProvider(this).get(RegistrationViewModel.class);
        observeViewModel();

        btnBack.setOnClickListener(v -> finish());
        txtLogin.setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> register());
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            btnRegister.setEnabled(!isLoading);
            btnRegister.setText(isLoading ? "Đang đăng ký..." : "Đăng ký");
            edtFullName.setEnabled(!isLoading);
            edtEmail.setEnabled(!isLoading);
            edtPassword.setEnabled(!isLoading);
            edtConfirmPassword.setEnabled(!isLoading);
            rgRole.setEnabled(!isLoading);
        });
        viewModel.getError().observe(this, message -> {
            if (!TextUtils.isEmpty(message))
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
        viewModel.getRegistered().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "Đăng ký thành công. Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void register() {
        String fullName = textOf(edtFullName);
        String email = textOf(edtEmail);
        String password = textOf(edtPassword);
        String confirmPassword = textOf(edtConfirmPassword);

        if (fullName.isEmpty()) {
            edtFullName.setError("Vui lòng nhập họ và tên");
            edtFullName.requestFocus();
            return;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email không hợp lệ");
            edtEmail.requestFocus();
            return;
        }
        if (password.length() < 6) {
            edtPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            edtPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            edtConfirmPassword.requestFocus();
            return;
        }

        int selected = rgRole.getCheckedRadioButtonId();
        String role;
        if (selected == R.id.rbCreator) role = "CREATOR";
        else if (selected == R.id.rbShipper) role = "SHIPPER";
        else {
            Toast.makeText(this, "Vui lòng chọn vai trò", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.register(fullName, email, password, role);
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
