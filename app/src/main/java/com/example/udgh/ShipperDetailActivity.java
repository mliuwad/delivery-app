package com.example.udgh;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.example.udgh.models.ShipperDetail;
import com.example.udgh.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
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

public class ShipperDetailActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private ImageView imgAvatar;
    private TextView txtName, txtStatus, txtTotalOrders, txtSalary;
    private EditText edtFullName, edtPhone, edtEmail, edtCitizenId, edtLicensePlate, edtAddress;
    private MaterialButton btnEdit, btnLock, btnDelete;

    private FirebaseFirestore db;
    private String shipperId;
    private String shipperDetailId;

    private User currentUser;
    private ShipperDetail currentShipperDetail;
    private boolean isEditing = false;
    private boolean isViewMode = true;  // ✅ Chế độ xem (không có nút chức năng)

    private Uri selectedImageUri;
    private ProgressDialog progressDialog;

    private static final String IMGBB_API_KEY = "dee690b746c3dc19370022b139ad6696";

    // ==================== IMAGE PICKER ====================
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            selectedImageUri = result.getData().getData();
                            if (selectedImageUri != null) {
                                Glide.with(this)
                                        .load(selectedImageUri)
                                        .circleCrop()
                                        .placeholder(R.drawable.delivery_man)
                                        .error(R.drawable.delivery_man)
                                        .into(imgAvatar);
                                uploadAvatarToImgBB(selectedImageUri);
                            }
                        }
                    });

    // ==================== ONCREATE ====================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shipper_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        shipperId = getIntent().getStringExtra("shipperId");
        if (TextUtils.isEmpty(shipperId)) {
            Toast.makeText(this, "Không tìm thấy shipper", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ✅ Kiểm tra chế độ: nếu có "mode=edit" thì là chế độ sửa, ngược lại là xem chi tiết
        String mode = getIntent().getStringExtra("mode");
        isViewMode = !"edit".equals(mode);

        // Ánh xạ view
        toolbar = findViewById(R.id.toolbar);
        imgAvatar = findViewById(R.id.imgAvatar);
        txtName = findViewById(R.id.txtName);
        txtStatus = findViewById(R.id.txtStatus);
        txtTotalOrders = findViewById(R.id.txtTotalOrders);
        txtSalary = findViewById(R.id.txtSalary);
        edtFullName = findViewById(R.id.txtFullName);
        edtPhone = findViewById(R.id.txtPhone);
        edtEmail = findViewById(R.id.txtEmail);
        edtCitizenId = findViewById(R.id.txtCitizenId);
        edtLicensePlate = findViewById(R.id.txtLicensePlate);
        edtAddress = findViewById(R.id.txtAddress);
        btnEdit = findViewById(R.id.btnEdit);
        btnLock = findViewById(R.id.btnLock);
        btnDelete = findViewById(R.id.btnDelete);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // ✅ Cấu hình hiển thị theo chế độ
        if (isViewMode) {
            // Chế độ chi tiết: ẩn tất cả nút chức năng
            toolbar.setTitle("Chi tiết nhân viên");
            btnEdit.setVisibility(View.GONE);
            btnLock.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
            // Vô hiệu hóa chỉnh sửa
            setInputsEnabled(false);
            // Không cho click avatar
            imgAvatar.setClickable(false);
            imgAvatar.setEnabled(false);
        } else {
            // Chế độ sửa: hiển thị các nút
            toolbar.setTitle("Sửa nhân viên");
            btnEdit.setVisibility(View.VISIBLE);
            btnLock.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
            btnEdit.setText("Sửa");
            // Cho phép click avatar để đổi ảnh (khi đang edit)
            imgAvatar.setClickable(true);
            imgAvatar.setEnabled(true);
            // Vô hiệu hóa ban đầu, bấm Sửa mới enable
            setInputsEnabled(false);
            btnLock.setEnabled(true);
            btnDelete.setEnabled(true);
        }

        // Avatar click để đổi ảnh (chỉ khi đang edit và isEditing = true)
        imgAvatar.setOnClickListener(v -> {
            if (!isViewMode && isEditing) {
                openGallery();
            } else if (isViewMode) {
                Toast.makeText(this, "Đang ở chế độ xem", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bấm Sửa để đổi ảnh", Toast.LENGTH_SHORT).show();
            }
        });

        // Nút Sửa / Lưu (chỉ hiện khi không ở chế độ view)
        if (!isViewMode) {
            btnEdit.setOnClickListener(v -> {
                if (isEditing) {
                    saveProfile();
                } else {
                    enableEditing();
                }
            });
        }

        // Nút Khóa / Mở khóa
        btnLock.setOnClickListener(v -> toggleLock());

        // Nút Xóa
        btnDelete.setOnClickListener(v -> deleteShipper());

        loadProfile();
    }

    // ==================== OPEN GALLERY ====================
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    // ==================== UPLOAD AVATAR TO IMGBB ====================
    private void uploadAvatarToImgBB(Uri imageUri) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang upload ảnh...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                progressDialog.dismiss();
                Toast.makeText(this, "Không thể đọc file ảnh", Toast.LENGTH_SHORT).show();
                return;
            }
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
                        Toast.makeText(ShipperDetailActivity.this,
                                "Lỗi upload ảnh: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String jsonData = response.body().string();
                            JSONObject jsonObject = new JSONObject(jsonData);
                            boolean success = jsonObject.optBoolean("success", false);

                            if (success) {
                                JSONObject data = jsonObject.getJSONObject("data");
                                final String imageUrl = data.getString("url");

                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    updateAvatarUrl(imageUrl);
                                });
                            } else {
                                String errorMsg = jsonObject.optString("error", "Upload thất bại");
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(ShipperDetailActivity.this,
                                            "Upload ảnh thất bại: " + errorMsg,
                                            Toast.LENGTH_LONG).show();
                                });
                            }
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(ShipperDetailActivity.this,
                                        "Lỗi xử lý ảnh: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(ShipperDetailActivity.this,
                                    "Upload ảnh thất bại!",
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });
        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Lỗi đọc file ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ==================== UPDATE AVATAR URL TO FIRESTORE ====================
    private void updateAvatarUrl(String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("avatarUrl", imageUrl);

        db.collection("USERS")
                .document(shipperId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật ảnh đại diện thành công", Toast.LENGTH_SHORT).show();
                    if (currentUser != null) {
                        currentUser.setAvatarUrl(imageUrl);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi cập nhật avatar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ==================== LOAD PROFILE ====================
    private void loadProfile() {
        // Load User
        db.collection("USERS")
                .document(shipperId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUser = doc.toObject(User.class);
                        currentUser.setUserId(doc.getId());
                        displayUserInfo();

                        String avatarUrl = doc.getString("avatarUrl");
                        if (!TextUtils.isEmpty(avatarUrl)) {
                            Glide.with(this)
                                    .load(avatarUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.delivery_man)
                                    .error(R.drawable.delivery_man)
                                    .into(imgAvatar);
                        }
                    } else {
                        Toast.makeText(this, "Không tìm thấy shipper", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải user: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        // Load ShipperDetail
        db.collection("USERS")
                .document(shipperId)
                .collection("SHIPPER_DETAILS")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        shipperDetailId = doc.getId();
                        currentShipperDetail = doc.toObject(ShipperDetail.class);
                        displayShipperDetail();
                    } else {
                        shipperDetailId = shipperId;
                        currentShipperDetail = new ShipperDetail();
                        currentShipperDetail.setShipperId(shipperId);
                        Map<String, Object> data = new HashMap<>();
                        data.put("shipperId", shipperId);
                        db.collection("USERS")
                                .document(shipperId)
                                .collection("SHIPPER_DETAILS")
                                .document(shipperDetailId)
                                .set(data)
                                .addOnSuccessListener(aVoid -> {})
                                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tạo detail: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        displayShipperDetail();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải chi tiết: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        loadStatistics();
    }

    private void displayUserInfo() {
        if (currentUser == null) return;
        txtName.setText(currentUser.getFullName());
        edtFullName.setText(currentUser.getFullName());
        edtPhone.setText(currentUser.getPhone());
        edtEmail.setText(currentUser.getEmail());

        String status = currentUser.getStatus();
        if ("ACTIVE".equals(status)) {
            txtStatus.setText("Hoạt động");
            txtStatus.setTextColor(getColor(android.R.color.holo_green_dark));
            if (!isViewMode) btnLock.setText("Khóa");
        } else {
            txtStatus.setText("Đã khóa");
            txtStatus.setTextColor(getColor(android.R.color.holo_red_dark));
            if (!isViewMode) btnLock.setText("Mở khóa");
        }
    }

    private void displayShipperDetail() {
        if (currentShipperDetail == null) return;
        edtCitizenId.setText(currentShipperDetail.getCitizenId() != null ? currentShipperDetail.getCitizenId() : "");
        edtLicensePlate.setText(currentShipperDetail.getLicensePlate() != null ? currentShipperDetail.getLicensePlate() : "");
        edtAddress.setText(currentShipperDetail.getAddress() != null ? currentShipperDetail.getAddress() : "");
    }

    private void loadStatistics() {
        db.collection("ORDERS")
                .whereEqualTo("shipperId", shipperId)
                .whereEqualTo("status", "COMPLETED")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    txtTotalOrders.setText(count + " đơn");
                })
                .addOnFailureListener(e -> txtTotalOrders.setText("0 đơn"));

        txtSalary.setText("0đ"); // Tạm thời
    }

    // ==================== EDIT MODE ====================
    private void enableEditing() {
        isEditing = true;
        btnEdit.setText("Lưu");
        setInputsEnabled(true);
        // Cho phép click avatar để đổi ảnh
        imgAvatar.setClickable(true);
        imgAvatar.setEnabled(true);
        // Thêm viền nhận biết đang chỉnh sửa
        int padding = 8;
        edtFullName.setBackgroundResource(R.drawable.bg_edit_text);
        edtFullName.setPadding(padding, padding, padding, padding);
        edtPhone.setBackgroundResource(R.drawable.bg_edit_text);
        edtPhone.setPadding(padding, padding, padding, padding);
        edtEmail.setBackgroundResource(R.drawable.bg_edit_text);
        edtEmail.setPadding(padding, padding, padding, padding);
        edtCitizenId.setBackgroundResource(R.drawable.bg_edit_text);
        edtCitizenId.setPadding(padding, padding, padding, padding);
        edtLicensePlate.setBackgroundResource(R.drawable.bg_edit_text);
        edtLicensePlate.setPadding(padding, padding, padding, padding);
        edtAddress.setBackgroundResource(R.drawable.bg_edit_text);
        edtAddress.setPadding(padding, padding, padding, padding);
    }

    private void disableEditing() {
        isEditing = false;
        btnEdit.setText("Sửa");
        setInputsEnabled(false);
        imgAvatar.setClickable(false);
        imgAvatar.setEnabled(false);
        edtFullName.setBackgroundResource(0);
        edtPhone.setBackgroundResource(0);
        edtEmail.setBackgroundResource(0);
        edtCitizenId.setBackgroundResource(0);
        edtLicensePlate.setBackgroundResource(0);
        edtAddress.setBackgroundResource(0);
    }

    private void setInputsEnabled(boolean enabled) {
        edtFullName.setEnabled(enabled);
        edtPhone.setEnabled(enabled);
        edtEmail.setEnabled(enabled);
        edtCitizenId.setEnabled(enabled);
        edtLicensePlate.setEnabled(enabled);
        edtAddress.setEnabled(enabled);
    }

    // ==================== SAVE PROFILE ====================
    private void saveProfile() {
        String fullName = edtFullName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String citizenId = edtCitizenId.getText().toString().trim();
        String licensePlate = edtLicensePlate.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            edtFullName.setError("Nhập họ tên");
            edtFullName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(phone) || !phone.matches("^0[0-9]{9}$")) {
            edtPhone.setError("Số điện thoại không hợp lệ");
            edtPhone.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email không hợp lệ");
            edtEmail.requestFocus();
            return;
        }

        btnEdit.setEnabled(false);
        btnEdit.setText("Đang lưu...");

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("fullName", fullName);
        userUpdates.put("phone", phone);
        userUpdates.put("email", email);

        db.collection("USERS")
                .document(shipperId)
                .update(userUpdates)
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> detailUpdates = new HashMap<>();
                    detailUpdates.put("citizenId", citizenId);
                    detailUpdates.put("licensePlate", licensePlate);
                    detailUpdates.put("address", address);

                    db.collection("USERS")
                            .document(shipperId)
                            .collection("SHIPPER_DETAILS")
                            .document(shipperDetailId)
                            .set(detailUpdates, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                                currentUser.setFullName(fullName);
                                currentUser.setPhone(phone);
                                currentUser.setEmail(email);
                                displayUserInfo();
                                if (currentShipperDetail != null) {
                                    currentShipperDetail.setCitizenId(citizenId);
                                    currentShipperDetail.setLicensePlate(licensePlate);
                                    currentShipperDetail.setAddress(address);
                                    displayShipperDetail();
                                }
                                disableEditing();
                                btnEdit.setEnabled(true);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Lỗi cập nhật chi tiết: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                btnEdit.setEnabled(true);
                                btnEdit.setText("Lưu");
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnEdit.setEnabled(true);
                    btnEdit.setText("Lưu");
                });
    }

    // ==================== TOGGLE LOCK ====================
    private void toggleLock() {
        if (currentUser == null) return;
        String newStatus = "ACTIVE".equals(currentUser.getStatus()) ? "INACTIVE" : "ACTIVE";

        db.collection("USERS")
                .document(shipperId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    currentUser.setStatus(newStatus);
                    displayUserInfo();
                    Toast.makeText(this, "Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ==================== DELETE SHIPPER ====================
    private void deleteShipper() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa nhân viên")
                .setMessage("Bạn có chắc muốn xóa nhân viên này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    ProgressDialog progressDialog = new ProgressDialog(this);
                    progressDialog.setMessage("Đang xóa...");
                    progressDialog.show();

                    db.collection("USERS")
                            .document(shipperId)
                            .collection("SHIPPER_DETAILS")
                            .document(shipperDetailId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                db.collection("USERS")
                                        .document(shipperId)
                                        .delete()
                                        .addOnSuccessListener(aVoid2 -> {
                                            progressDialog.dismiss();
                                            Toast.makeText(this, "Đã xóa nhân viên", Toast.LENGTH_SHORT).show();
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            progressDialog.dismiss();
                                            Toast.makeText(this, "Lỗi xóa user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(this, "Lỗi xóa chi tiết: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ==================== NAVIGATION ====================
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }
}