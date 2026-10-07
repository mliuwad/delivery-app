package com.example.udgh;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
//import com.google.firebase.auth.FirebaseAuthTooManyRequestsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText edtEmail;
    private TextInputEditText edtPassword;

    private MaterialButton btnLogin;

    private TextView txtRegister;
    private TextView txtForgotPassword;

    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);

        btnLogin = findViewById(R.id.btnLogin);

        txtRegister = findViewById(R.id.txtRegister);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);

        progressBar = findViewById(R.id.progressBar);

        setupClearErrorOnType();

        // Chặn nút Back khi đang loading, tránh thoát app giữa chừng gọi mạng
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isLoading) {
                    Toast.makeText(LoginActivity.this,
                            "Vui lòng đợi, đang xử lý...", Toast.LENGTH_SHORT).show();
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        //checkExistingSession();

        btnLogin.setOnClickListener(v -> login());

        txtRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        txtForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    // ================== TỰ XOÁ LỖI KHI GÕ LẠI ==================

    private void setupClearErrorOnType() {
        edtEmail.addTextChangedListener(new SimpleTextWatcher(() -> edtEmail.setError(null)));
        edtPassword.addTextChangedListener(new SimpleTextWatcher(() -> edtPassword.setError(null)));
    }

    private static class SimpleTextWatcher implements TextWatcher {
        private final Runnable onChange;

        SimpleTextWatcher(Runnable onChange) {
            this.onChange = onChange;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            onChange.run();
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }

    // ================== KIỂM TRA SESSION ĐANG ĐĂNG NHẬP ==================

              
    private void checkExistingSession() {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) return;

        if (!isNetworkAvailable()) {
            // Có session cục bộ nhưng không có mạng để xác thực lại -> để người dùng đăng nhập thủ công
            return;
        }

        setLoading(true);

        // reload() để chắc chắn tài khoản chưa bị xoá/khoá phía server
        user.reload()
                .addOnSuccessListener(unused -> loadUser(user.getUid()))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    mAuth.signOut();
                });
    }



    // ================== KIỂM TRA KẾT NỐI MẠNG ==================

    private boolean isNetworkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return false;

        android.net.Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        if (capabilities == null) return false;

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    // ================== XỬ LÝ ĐĂNG NHẬP ==================

    private void login() {

        String email = edtEmail.getText() == null ? "" : edtEmail.getText().toString().trim();
        String password = edtPassword.getText() == null ? "" : edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Vui lòng nhập email");
            edtEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email không đúng định dạng");
            edtEmail.requestFocus();
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

        if (!isNetworkAvailable()) {
            Toast.makeText(this,
                    "Không có kết nối Internet. Vui lòng kiểm tra lại mạng.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {

                    FirebaseUser user = mAuth.getCurrentUser();

                    if (user == null) {
                        setLoading(false);
                        Toast.makeText(this, "Đăng nhập thất bại", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    loadUser(user.getUid());
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    setLoading(false);
                    Toast.makeText(LoginActivity.this,
                            getFirebaseErrorMessage(e), Toast.LENGTH_LONG).show();
                });
    }

    // ================== LẤY THÔNG TIN USER TỪ FIRESTORE ==================

    private void loadUser(String uid) {

        db.collection("USERS")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (isFinishing() || isDestroyed()) return;

                    if (!documentSnapshot.exists()) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this,
                                "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        return;
                    }

                    String role = documentSnapshot.getString("role");
                    String status = documentSnapshot.getString("status");

                    if (role == null) {
                        setLoading(false);
                        Toast.makeText(this,
                                "Không xác định quyền người dùng", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        return;
                    }

                    if (status == null || !"ACTIVE".equalsIgnoreCase(status)) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this,
                                "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.",
                                Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        return;
                    }

                    Intent intent;

                    if ("ADMIN".equalsIgnoreCase(role)) {
                        intent = new Intent(LoginActivity.this, MainActivity.class);
                    } else if ("CREATOR".equalsIgnoreCase(role)) {
                        intent = new Intent(LoginActivity.this, HomeCustomerActivity.class);
                    } else if ("SHIPPER".equalsIgnoreCase(role)) {
                        intent = new Intent(LoginActivity.this, HomeShipperActivity.class);
                    } else {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this,
                                "Vai trò không hợp lệ", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        return;
                    }

                    setLoading(false);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    setLoading(false);
                    Toast.makeText(LoginActivity.this,
                            "Lỗi tải thông tin người dùng: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // ================== QUÊN MẬT KHẨU ==================

    private void showForgotPasswordDialog() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        final EditText input = new EditText(this);
        input.setHint("Nhập email đã đăng ký");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        String currentEmail = edtEmail.getText() == null ? "" : edtEmail.getText().toString().trim();
        if (!TextUtils.isEmpty(currentEmail)) {
            input.setText(currentEmail);
            input.setSelection(currentEmail.length());
        }

        layout.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Quên mật khẩu")
                .setMessage("Nhập email của bạn, hệ thống sẽ gửi liên kết đặt lại mật khẩu.")
                .setView(layout)
                .setPositiveButton("Gửi", null)
                .setNegativeButton("Hủy", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {

            android.widget.Button btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            btnPositive.setOnClickListener(v -> {

                String resetEmail = input.getText().toString().trim();

                if (TextUtils.isEmpty(resetEmail)) {
                    input.setError("Vui lòng nhập email");
                    return;
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(resetEmail).matches()) {
                    input.setError("Email không đúng định dạng");
                    return;
                }

                if (!isNetworkAvailable()) {
                    Toast.makeText(LoginActivity.this,
                            "Không có kết nối Internet", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnPositive.setEnabled(false);
                setLoading(true);

                mAuth.sendPasswordResetEmail(resetEmail)
                        .addOnSuccessListener(unused -> {
                            if (isFinishing() || isDestroyed()) return;
                            setLoading(false);
                            Toast.makeText(LoginActivity.this,
                                    "Đã gửi email đặt lại mật khẩu. Vui lòng kiểm tra hộp thư.",
                                    Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            if (isFinishing() || isDestroyed()) return;
                            setLoading(false);
                            btnPositive.setEnabled(true);
                            Toast.makeText(LoginActivity.this,
                                    getFirebaseErrorMessage(e), Toast.LENGTH_LONG).show();
                        });
            });
        });

        dialog.show();
    }

    // ================== TIỆN ÍCH ==================

    private void setLoading(boolean loading) {
        isLoading = loading;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        setEnabled(!loading);
    }

    private void setEnabled(boolean enabled) {
        btnLogin.setEnabled(enabled);
        edtEmail.setEnabled(enabled);
        edtPassword.setEnabled(enabled);
        txtForgotPassword.setEnabled(enabled);
        txtRegister.setEnabled(enabled);
    }

    /**
     * Chuyển exception của Firebase thành thông báo tiếng Việt dễ hiểu cho người dùng.
     */
    private String getFirebaseErrorMessage(@NonNull Exception e) {

        if (e instanceof FirebaseAuthInvalidUserException) {
            return "Tài khoản không tồn tại hoặc đã bị vô hiệu hóa";
        }

        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return "Email hoặc mật khẩu không đúng";
        }
          /*
        if (e instanceof FirebaseAuthTooManyRequestsException) {
            return "Bạn đã thử đăng nhập quá nhiều lần. Vui lòng thử lại sau ít phút.";
        }
         */
        if (e instanceof FirebaseNetworkException) {
            return "Lỗi kết nối mạng. Vui lòng kiểm tra lại Internet.";
        }

        String msg = e.getMessage();
        return TextUtils.isEmpty(msg) ? "Đăng nhập thất bại. Vui lòng thử lại." : msg;
    }
}