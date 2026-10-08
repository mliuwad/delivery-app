package com.example.udgh;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddEditShipperActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private ImageView imgAvatar;
    private MaterialButton btnChooseImage, btnSave;
    private TextInputEditText edtFullName, edtPhone, edtEmail, edtCitizenId, edtLicensePlate, edtAddress;
    private TextInputEditText edtPassword, edtConfirmPassword;
    private RadioGroup rgStatus;
    private RadioButton rbActive, rbLocked;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Uri selectedImageUri;
    private String avatarUrl = "";

    private static final String IMGBB_API_KEY = "dee690b746c3dc19370022b139ad6696";

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            selectedImageUri = result.getData().getData();
                            if (selectedImageUri != null) {
                                Glide.with(this)
                                        .load(selectedImageUri)
                                        .centerCrop()
                                        .into(imgAvatar);
                                Toast.makeText(this, "Đã chọn ảnh", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_edit_shipper);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        toolbar = findViewById(R.id.toolbar);
        imgAvatar = findViewById(R.id.imgAvatar);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnSave = findViewById(R.id.btnSave);
        edtFullName = findViewById(R.id.edtFullName);
        edtPhone = findViewById(R.id.edtPhone);
        edtEmail = findViewById(R.id.edtEmail);
        edtCitizenId = findViewById(R.id.edtCitizenId);
        edtLicensePlate = findViewById(R.id.edtLicensePlate);
        edtAddress = findViewById(R.id.edtAddress);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        rgStatus = findViewById(R.id.rgStatus);
        rbActive = findViewById(R.id.rbActive);
        rbLocked = findViewById(R.id.rbLocked);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        btnChooseImage.setOnClickListener(v -> openGallery());
        btnSave.setOnClickListener(v -> addShipper());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void addShipper() {
        String fullName = edtFullName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String citizenId = edtCitizenId.getText().toString().trim();
        String licensePlate = edtLicensePlate.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        // Validate
        if (TextUtils.isEmpty(fullName)) {
            edtFullName.setError("Vui lòng nhập họ tên");
            edtFullName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(phone) || !phone.matches("^0[0-9]{9}$")) {
            edtPhone.setError("Số điện thoại không hợp lệ (10 số, bắt đầu bằng 0)");
            edtPhone.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email không hợp lệ");
            edtEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            edtPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            edtPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError("Mật khẩu không khớp");
            edtConfirmPassword.requestFocus();
            return;
        }

        String status = rbActive.isChecked() ? "ACTIVE" : "INACTIVE";

        btnSave.setEnabled(false);
        btnSave.setText("Đang tạo...");

        // Nếu có ảnh, upload lên ImgBB trước
        if (selectedImageUri != null) {
            uploadAvatarToImgBB(fullName, phone, email, citizenId, licensePlate, address, password, status);
        } else {
            // Tạo tài khoản không có ảnh
            createShipperAccount(fullName, phone, email, citizenId, licensePlate, address, password, status, "");
        }
    }

    private void uploadAvatarToImgBB(String fullName, String phone, String email,
                                     String citizenId, String licensePlate, String address,
                                     String password, String status) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang upload ảnh...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            byte[] imageBytes = new byte[inputStream.available()];
            inputStream.read(imageBytes);
            inputStream.close();

            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            OkHttpClient client = new OkHttpClient();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("key", IMGBB_API_KEY)
                    .addFormDataPart("image", base64Image)
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.imgbb.com/1/upload")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(AddEditShipperActivity.this, "Lỗi upload ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnSave.setEnabled(true);
                        btnSave.setText("Lưu thông tin");
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String jsonData = response.body().string();
                            JSONObject jsonObject = new JSONObject(jsonData);
                            boolean success = jsonObject.getBoolean("success");

                            if (success) {
                                JSONObject data = jsonObject.getJSONObject("data");
                                String imageUrl = data.getString("url");

                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    createShipperAccount(fullName, phone, email, citizenId,
                                            licensePlate, address, password, status, imageUrl);
                                });
                            } else {
                                String errorMsg = jsonObject.getString("error");
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(AddEditShipperActivity.this, "Upload ảnh thất bại: " + errorMsg, Toast.LENGTH_LONG).show();
                                    btnSave.setEnabled(true);
                                    btnSave.setText("Lưu thông tin");
                                });
                            }
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(AddEditShipperActivity.this, "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                btnSave.setEnabled(true);
                                btnSave.setText("Lưu thông tin");
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(AddEditShipperActivity.this, "Upload ảnh thất bại!", Toast.LENGTH_LONG).show();
                            btnSave.setEnabled(true);
                            btnSave.setText("Lưu thông tin");
                        });
                    }
                }
            });
        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Lỗi đọc file ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
            btnSave.setEnabled(true);
            btnSave.setText("Lưu thông tin");
        }
    }

    private void createShipperAccount(String fullName, String phone, String email,
                                      String citizenId, String licensePlate, String address,
                                      String password, String status, String avatarUrl) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tạo tài khoản...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Tạo tài khoản Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        String userId = firebaseUser.getUid();

                        // Lưu thông tin User vào Firestore
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("fullName", fullName);
                        userData.put("phone", phone);
                        userData.put("email", email);
                        userData.put("role", "SHIPPER");
                        userData.put("status", status);
                        userData.put("avatarUrl", avatarUrl);
                        userData.put("createdAt", com.google.firebase.Timestamp.now());

                        db.collection("USERS")
                                .document(userId)
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    // Lưu ShipperDetail vào sub-collection
                                    saveShipperDetail(userId, citizenId, licensePlate, address, progressDialog);
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(this, "Lỗi lưu thông tin: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    btnSave.setEnabled(true);
                                    btnSave.setText("Lưu thông tin");
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Lỗi tạo tài khoản: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("Lưu thông tin");
                });
    }

    private void saveShipperDetail(String userId, String citizenId, String licensePlate,
                                   String address, ProgressDialog progressDialog) {
        Map<String, Object> detailData = new HashMap<>();
        detailData.put("shipperId", userId);
        detailData.put("citizenId", citizenId);
        detailData.put("licensePlate", licensePlate);
        detailData.put("address", address);
        detailData.put("joinDate", com.google.firebase.Timestamp.now());

        db.collection("USERS")
                .document(userId)
                .collection("SHIPPER_DETAILS")
                .document(userId)
                .set(detailData)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Thêm nhân viên thành công!", Toast.LENGTH_LONG).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("Lưu thông tin");
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Lỗi lưu chi tiết: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("Lưu thông tin");
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}