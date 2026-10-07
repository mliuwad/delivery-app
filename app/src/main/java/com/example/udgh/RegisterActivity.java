package com.example.udgh;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
public class RegisterActivity extends AppCompatActivity {
    private ImageButton btnBack;

    private TextInputEditText edtFullName;
    private TextInputEditText edtEmail;
    private TextInputEditText edtPassword;
    private TextInputEditText edtConfirmPassword;

    private RadioGroup rgRole;

    private MaterialButton btnRegister;

    private TextView txtLogin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        /*
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        */
        // Ánh xạ
        btnBack = findViewById(R.id.btnBack);

        edtFullName = findViewById(R.id.edtFullName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);

        rgRole = findViewById(R.id.rgRole);

        btnRegister = findViewById(R.id.btnRegister);

        txtLogin = findViewById(R.id.txtLogin);

        // Nút quay lại
        btnBack.setOnClickListener(v -> finish());

        // Nút đăng ký
        btnRegister.setOnClickListener(v -> register());

    }
    private void register() {

        String fullName = edtFullName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        if (fullName.isEmpty()
                || email.isEmpty()
                || password.isEmpty()
                || confirmPassword.isEmpty()) {

            Toast.makeText(
                    this,
                    "Vui lòng nhập đầy đủ thông tin",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (!password.equals(confirmPassword)) {

            Toast.makeText(
                    this,
                    "Mật khẩu không khớp",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        int selectedRole = rgRole.getCheckedRadioButtonId();

        if (selectedRole == -1) {

            Toast.makeText(
                    this,
                    "Vui lòng chọn vai trò",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Toast.makeText(
                this,
                "Đăng ký thành công",
                Toast.LENGTH_SHORT
        ).show();
    }
}

