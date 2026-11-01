package com.example.udgh;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText edtUsername, edtPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private TextView tvGoToRegister, tvForgotPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Ánh xạ view
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        mAuth = FirebaseAuth.getInstance();

        // Sự kiện click
        btnLogin.setOnClickListener(v -> loginUser());

        tvGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );

        // Gọi hàm xử lý Quên mật khẩu
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    // ==========================================
    // 1. XỬ LÝ ĐĂNG NHẬP
    // ==========================================
    private void loginUser() {
        String username = edtUsername.getText() != null ? edtUsername.getText().toString().trim() : "";
        String password = edtPassword.getText() != null ? edtPassword.getText().toString().trim() : "";

        // Validate dữ liệu đầu vào
        if (TextUtils.isEmpty(username)) {
            edtUsername.setError("Vui lòng nhập Email hoặc Số điện thoại");
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

        // Bật trạng thái Loading
        setLoadingState(true);

        mAuth.signInWithEmailAndPassword(username, password)
                .addOnCompleteListener(this, task -> {
                    setLoadingState(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        String uid = mAuth.getCurrentUser() != null
                                ? mAuth.getCurrentUser().getUid() : null;
                        if (uid == null) {
                            Toast.makeText(LoginActivity.this, "Không xác định được tài khoản", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        FirebaseFirestore.getInstance().collection("USERS").document(uid).get()
                                .addOnSuccessListener(user -> {
                                    if (!user.exists()) {
                                        mAuth.signOut();
                                        Toast.makeText(LoginActivity.this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                    String status = user.getString("status");
                                    if (!"ACTIVE".equalsIgnoreCase(status)) {
                                        mAuth.signOut();
                                        Toast.makeText(LoginActivity.this, "Tài khoản đã bị khóa hoặc chưa được kích hoạt", Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                    String role = user.getString("role");
                                    Intent intent;
                                    if ("ADMIN".equalsIgnoreCase(role)) {
                                        intent = new Intent(LoginActivity.this, MainActivity.class);
                                    } else if ("CREATOR".equalsIgnoreCase(role)) {
                                        intent = new Intent(LoginActivity.this, HomeCreatorActivity.class);
                                    } else if ("SHIPPER".equalsIgnoreCase(role)) {
                                        intent = new Intent(LoginActivity.this, HomeShipperActivity.class);
                                    } else {
                                        mAuth.signOut();
                                        Toast.makeText(LoginActivity.this, "Vai trò tài khoản không hợp lệ", Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    mAuth.signOut();
                                    Toast.makeText(LoginActivity.this, "Không thể xác minh tài khoản. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                                });
                    } else {
                        // Phân tích lỗi chi tiết
                        String errorMessage = getErrorMessage(task.getException());
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Hàm dịch lỗi Firebase sang Tiếng Việt
    private String getErrorMessage(Exception exception) {
        if (exception instanceof FirebaseAuthInvalidUserException) {
            return "Tài khoản không tồn tại trên hệ thống.";
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            return "Mật khẩu không chính xác. Vui lòng thử lại.";
        } else if (exception.getMessage() != null && exception.getMessage().contains("network")) {
            return "Không có kết nối Internet. Vui lòng kiểm tra lại mạng.";
        } else if (exception.getMessage() != null && exception.getMessage().contains("too many")) {
            return "Bạn đã thử quá nhiều lần. Vui lòng thử lại sau ít phút.";
        }
        return "Đăng nhập thất bại. Vui lòng thử lại sau.";
    }

    // Quản lý trạng thái Loading
    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setText("");
            btnLogin.setEnabled(false);
            edtUsername.setEnabled(false);
            edtPassword.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setText("Đăng nhập");
            btnLogin.setEnabled(true);
            edtUsername.setEnabled(true);
            edtPassword.setEnabled(true);
        }
    }

    // ==========================================
    // 2. XỬ LÝ QUÊN MẬT KHẨU (HIỂN THỊ DIALOG)
    // ==========================================
    private void showForgotPasswordDialog() {
        // Tạo View cho Dialog
        View dialogView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null);

        // Tạo một EditText động để nhập Email
        EditText edtEmail = new EditText(this);
        edtEmail.setHint("Nhập Email của bạn");
        edtEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        // Padding cho EditText
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        edtEmail.setPadding(padding, padding, padding, padding);

        // Tạo AlertDialog
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

                    // Gọi Firebase gửi email khôi phục
                    sendPasswordResetEmail(email);
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Gửi email khôi phục mật khẩu qua Firebase
    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                "Đã gửi liên kết khôi phục vào Email: " + email + "\nVui lòng kiểm tra hộp thư!",
                                Toast.LENGTH_LONG).show();
                    } else {
                        String error = "Lỗi: " + (task.getException() != null ? task.getException().getMessage() : "Không xác định");
                        Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}