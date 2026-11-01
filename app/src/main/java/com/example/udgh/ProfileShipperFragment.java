package com.example.udgh;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.example.udgh.models.ShipperDetail;
import com.example.udgh.models.User;
import com.example.udgh.viewmodels.ShipperProfileViewModel;
import com.example.udgh.repositories.ShipperProfileRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

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

public class ProfileShipperFragment extends Fragment {

    private ImageView imgAvatar;
    private TextView txtName;
    private Chip chipStatus;
    private TextInputEditText edtFullName, edtPhone, edtEmail, edtCitizenId, edtLicensePlate, edtAddress;
    private MaterialButton btnEdit;

    private FirebaseAuth mAuth;
    private ShipperProfileViewModel profileViewModel;
    private String userId;
    private String shipperDetailId;
    private Uri selectedImageUri;

    private User currentUser;
    private ShipperDetail currentShipperDetail;
    private boolean isEditing = false;

    private static final String IMGBB_API_KEY = "dee690b746c3dc19370022b139ad6696";

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            selectedImageUri = result.getData().getData();
                            if (selectedImageUri != null) {
                                imgAvatar.setImageURI(selectedImageUri);
                                uploadAvatarToImgBB(selectedImageUri);
                            }
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shipper_information, container, false);

        mAuth = FirebaseAuth.getInstance();
        profileViewModel = new ViewModelProvider(this).get(ShipperProfileViewModel.class);

        SharedPreferences sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE);
        userId = sharedPref.getString("userId", "");
        if (TextUtils.isEmpty(userId) && mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        imgAvatar = view.findViewById(R.id.imgAvatar);
        txtName = view.findViewById(R.id.txtName);
        chipStatus = view.findViewById(R.id.chipStatus);
        edtFullName = view.findViewById(R.id.txtFullName);
        edtPhone = view.findViewById(R.id.txtPhone);
        edtEmail = view.findViewById(R.id.txtEmail);
        edtCitizenId = view.findViewById(R.id.txtCitizenId);
        edtLicensePlate = view.findViewById(R.id.txtLicensePlate);
        edtAddress = view.findViewById(R.id.txtAddress);
        btnEdit = view.findViewById(R.id.btnEdit);

        setEditMode(false);
        observeProfileViewModel();

        imgAvatar.setOnClickListener(v -> openGallery());

        btnEdit.setOnClickListener(v -> {
            if (isEditing) {
                saveProfile();
            } else {
                enableEditing();
            }
        });

        loadProfile();
        return view;
    }

    private void openGallery() {
        if (!isEditing) {
            Toast.makeText(getContext(), "Bấm 'Cập nhật thông tin' trước khi đổi ảnh", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void uploadAvatarToImgBB(Uri imageUri) {
        if (imageUri == null || TextUtils.isEmpty(userId)) return;

        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Đang upload ảnh...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        try {
            InputStream inputStream = requireActivity().getContentResolver().openInputStream(imageUri);
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
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Lỗi upload: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

                                requireActivity().runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    saveAvatarUrlToFirestore(imageUrl);
                                });
                            } else {
                                String errorMsg = jsonObject.getString("error");
                                requireActivity().runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(getContext(), "Upload thất bại: " + errorMsg, Toast.LENGTH_LONG).show();
                                });
                            }
                        } catch (Exception e) {
                            requireActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(getContext(), "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                        }
                    } else {
                        requireActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(getContext(), "Upload ảnh thất bại!", Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });
        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(getContext(), "Lỗi đọc file ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveAvatarUrlToFirestore(String url) {
        if (profileViewModel != null) {
            profileViewModel.updateAvatar(userId, url);
        }
    }

    private void loadProfile() {
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(getContext(), "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        if (profileViewModel != null) profileViewModel.loadProfile(userId);
    }

    private void observeProfileViewModel() {
        profileViewModel.getProfile().observe(getViewLifecycleOwner(), data -> {
            if (data == null) return;
            currentUser = data.user;
            currentShipperDetail = data.shipperDetail;
            shipperDetailId = data.shipperDetailId;
            displayUserInfo();
            displayShipperDetailInfo();

            String avatarUrl = currentUser != null ? currentUser.getAvatarUrl() : null;
            if (!TextUtils.isEmpty(avatarUrl) && isAdded() && getContext() != null) {
                Glide.with(this).load(avatarUrl).circleCrop().into(imgAvatar);
            }
        });

        profileViewModel.getOperationMessage().observe(getViewLifecycleOwner(), message -> {
            if (!TextUtils.isEmpty(message) && isAdded()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        profileViewModel.getOperationSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success == null) return;
            if (btnEdit != null && isAdded()) {
                btnEdit.setEnabled(true);
                if (!success && isEditing) btnEdit.setText("LƯU");
            }
        });

        profileViewModel.getProfileSaved().observe(getViewLifecycleOwner(), saved -> {
            if (saved == null || !saved || !isAdded()) return;
            isEditing = false;
            btnEdit.setText("Cập nhật thông tin");
            setEditMode(false);
            loadProfile();
        });
    }

    private void displayUserInfo() {
        if (currentUser == null) return;
        txtName.setText(currentUser.getFullName());
        edtFullName.setText(currentUser.getFullName());
        edtPhone.setText(currentUser.getPhone());
        edtEmail.setText(currentUser.getEmail());

        String status = currentUser.getStatus();
        if ("ACTIVE".equals(status)) {
            chipStatus.setText("Hoạt động");
            chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_dark);
        } else {
            chipStatus.setText("Không hoạt động");
            chipStatus.setChipBackgroundColorResource(android.R.color.darker_gray);
        }
    }

    private void displayShipperDetailInfo() {
        if (currentShipperDetail == null) return;
        edtCitizenId.setText(currentShipperDetail.getCitizenId() != null ? currentShipperDetail.getCitizenId() : "");
        edtLicensePlate.setText(currentShipperDetail.getLicensePlate() != null ? currentShipperDetail.getLicensePlate() : "");
        edtAddress.setText(currentShipperDetail.getAddress() != null ? currentShipperDetail.getAddress() : "");
    }

    private void enableEditing() {
        isEditing = true;
        btnEdit.setText("LƯU");
        setEditMode(true);
    }

    private void saveProfile() {
        String fullName = edtFullName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String citizenId = edtCitizenId.getText().toString().trim();
        String licensePlate = edtLicensePlate.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            edtFullName.setError("Vui lòng nhập họ tên");
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
        if (TextUtils.isEmpty(shipperDetailId)) {
            Toast.makeText(getContext(), "Chưa tải được thông tin chi tiết Shipper", Toast.LENGTH_SHORT).show();
            return;
        }

        btnEdit.setEnabled(false);
        btnEdit.setText("Đang lưu...");
        profileViewModel.saveProfile(userId, shipperDetailId, fullName, phone, email,
                citizenId, licensePlate, address);
    }

    private void setEditMode(boolean editing) {
        edtFullName.setEnabled(editing);
        edtPhone.setEnabled(editing);
        edtEmail.setEnabled(editing);
        edtCitizenId.setEnabled(editing);
        edtLicensePlate.setEnabled(editing);
        edtAddress.setEnabled(editing);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Hủy các tác vụ Glide nếu cần, nhưng Glide tự quản lý
    }
}