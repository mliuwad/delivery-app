package com.example.udgh;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bumptech.glide.Glide;
import com.example.udgh.models.Product;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
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

public class AddEditProductActivity extends AppCompatActivity {

    // ================== KHAI BÁO ==================

    private MaterialToolbar toolbar;
    private ImageView imgProduct;
    private MaterialButton btnChooseImage, btnSave;
    private TextInputEditText edtProductName, edtDescription, edtWeight, edtPrice;
    private ScrollView scrollView;
    private View rootView;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Uri imageUri;
    private String imageUrl = "";
    private String productId = "";
    private boolean isEditMode = false;
    private String currentMode = ""; // "add", "edit", "view"

    // API Key ImgBB
    private static final String IMGBB_API_KEY = "dee690b746c3dc19370022b139ad6696";

    // ================== LAUNCHER CHỌN ẢNH ==================

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                imageUri = result.getData().getData();
                                if (imageUri != null) {
                                    Glide.with(AddEditProductActivity.this)
                                            .load(imageUri)
                                            .centerCrop()
                                            .into(imgProduct);
                                    Toast.makeText(AddEditProductActivity.this, "Đã chọn ảnh mới", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });

    // ================== ONCREATE ==================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_edit_product);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ view
        toolbar = findViewById(R.id.toolbar);
        scrollView = findViewById(R.id.scrollView);
        rootView = findViewById(android.R.id.content);
        imgProduct = findViewById(R.id.imgProduct);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnSave = findViewById(R.id.btnSave);
        edtProductName = findViewById(R.id.edtProductName);
        edtDescription = findViewById(R.id.edtDescription);
        edtWeight = findViewById(R.id.edtWeight);
        edtPrice = findViewById(R.id.edtPrice);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // ====== KIỂM TRA CHẾ ĐỘ ======
        Intent intent = getIntent();
        String mode = intent.getStringExtra("mode");

        if (intent.hasExtra("product")) {
            Product product = (Product) intent.getSerializableExtra("product");
            if (product != null) {
                productId = product.getProductId();
                isEditMode = true;
                loadProductData(product);

                // ✅ Chế độ XEM CHI TIẾT
                if ("view".equals(mode)) {
                    currentMode = "view";
                    getSupportActionBar().setTitle("Chi tiết sản phẩm");
                    btnSave.setVisibility(View.GONE);
                    btnChooseImage.setVisibility(View.GONE);
                    setFieldsReadOnly(true);
                }
                // ✅ Chế độ SỬA
                else {
                    currentMode = "edit";
                    getSupportActionBar().setTitle("Sửa sản phẩm");
                    btnSave.setText("Cập nhật");
                    btnSave.setVisibility(View.VISIBLE);
                    btnChooseImage.setVisibility(View.VISIBLE);
                    setFieldsReadOnly(false);
                }
            }
        } else {
            // ✅ Chế độ THÊM MỚI
            currentMode = "add";
            getSupportActionBar().setTitle("Thêm sản phẩm");
            btnSave.setText("Lưu thông tin");
            btnSave.setVisibility(View.VISIBLE);
            btnChooseImage.setVisibility(View.VISIBLE);
            setFieldsReadOnly(false);
        }

        btnChooseImage.setOnClickListener(v -> openImagePicker());

        btnSave.setOnClickListener(v -> {
            if (isEditMode) {
                updateProduct();
            } else {
                generateProductIdAndSave();
            }
        });

        // ✅ SETUP SCROLL KHI NHẬP
        setupScrollOnFocus();

        // ✅ LẮNG NGHE KHI BÀN PHÍM HIỆN
        setupKeyboardListener();
    }

    // ================== KHÓA/MỞ CÁC Ô NHẬP ==================

    private void setFieldsReadOnly(boolean readOnly) {
        if (readOnly) {
            // ✅ Chế độ chỉ xem - khóa tất cả, đổi màu nền
            edtProductName.setEnabled(false);
            edtDescription.setEnabled(false);
            edtWeight.setEnabled(false);
            edtPrice.setEnabled(false);

            edtProductName.setTextColor(getColor(R.color.black));
            edtDescription.setTextColor(getColor(R.color.black));
            edtWeight.setTextColor(getColor(R.color.black));
            edtPrice.setTextColor(getColor(R.color.black));

            // Đổi màu nền sang xám nhạt
            edtProductName.setBackgroundResource(android.R.drawable.editbox_background);
            edtDescription.setBackgroundResource(android.R.drawable.editbox_background);
            edtWeight.setBackgroundResource(android.R.drawable.editbox_background);
            edtPrice.setBackgroundResource(android.R.drawable.editbox_background);

        } else {
            // ✅ Chế độ thêm/sửa - mở khóa
            edtProductName.setEnabled(true);
            edtDescription.setEnabled(true);
            edtWeight.setEnabled(true);
            edtPrice.setEnabled(true);

            edtProductName.setTextColor(getColor(R.color.black));
            edtDescription.setTextColor(getColor(R.color.black));
            edtWeight.setTextColor(getColor(R.color.black));
            edtPrice.setTextColor(getColor(R.color.black));

            // Reset background về mặc định
            edtProductName.setBackgroundResource(android.R.drawable.edit_text);
            edtDescription.setBackgroundResource(android.R.drawable.edit_text);
            edtWeight.setBackgroundResource(android.R.drawable.edit_text);
            edtPrice.setBackgroundResource(android.R.drawable.edit_text);
        }
    }

    // ================== LẮNG NGHE BÀN PHÍM HIỆN ==================

    private void setupKeyboardListener() {
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > 100) {
                    View currentFocus = getCurrentFocus();
                    if (currentFocus != null) {
                        int[] location = new int[2];
                        currentFocus.getLocationInWindow(location);
                        int y = location[1];

                        int scrollY = y - 100;
                        scrollView.postDelayed(() -> {
                            scrollView.smoothScrollTo(0, Math.max(0, scrollY));
                        }, 200);
                    }
                }
            }
        });
    }

    // ================== SETUP SCROLL KHI FOCUS ==================

    private void setupScrollOnFocus() {
        edtProductName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollView.postDelayed(() -> {
                    int y = edtProductName.getTop() - 80;
                    scrollView.smoothScrollTo(0, Math.max(0, y));
                }, 300);
            }
        });

        edtDescription.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollView.postDelayed(() -> {
                    int y = edtDescription.getTop() - 80;
                    scrollView.smoothScrollTo(0, Math.max(0, y));
                }, 300);
            }
        });

        edtWeight.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollView.postDelayed(() -> {
                    int y = edtWeight.getTop() - 80;
                    scrollView.smoothScrollTo(0, Math.max(0, y));
                }, 300);
            }
        });

        edtPrice.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollView.postDelayed(() -> {
                    int y = edtPrice.getTop() - 80;
                    scrollView.smoothScrollTo(0, Math.max(0, y));
                }, 300);
            }
        });
    }

    // ================== TẠO MÃ SẢN PHẨM TỰ ĐỘNG ==================

    private void generateProductIdAndSave() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tạo mã sản phẩm...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        db.collection("PRODUCTS")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressDialog.dismiss();

                    int maxNumber = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String productId = document.getId();
                        if (productId != null && productId.startsWith("P")) {
                            try {
                                String numberStr = productId.substring(1);
                                int number = Integer.parseInt(numberStr);
                                if (number > maxNumber) {
                                    maxNumber = number;
                                }
                            } catch (NumberFormatException e) {
                                // Bỏ qua
                            }
                        }
                    }

                    int newNumber = maxNumber + 1;
                    String newProductId = "P" + String.format("%03d", newNumber);

                    saveProductWithProductId(newProductId);

                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Lỗi tạo mã: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ================== LƯU SẢN PHẨM VỚI DOCUMENT ID TỰ TẠO ==================

    private void saveProductWithProductId(String productId) {
        String productName = edtProductName.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();
        String weightStr = edtWeight.getText().toString().trim();
        String priceStr = edtPrice.getText().toString().trim();

        if (TextUtils.isEmpty(productName)) {
            edtProductName.setError("Vui lòng nhập tên sản phẩm");
            edtProductName.requestFocus();
            scrollView.postDelayed(() -> {
                int y = edtProductName.getTop() - 80;
                scrollView.smoothScrollTo(0, Math.max(0, y));
            }, 300);
            return;
        }

        if (TextUtils.isEmpty(description)) {
            edtDescription.setError("Vui lòng nhập mô tả");
            edtDescription.requestFocus();
            scrollView.postDelayed(() -> {
                int y = edtDescription.getTop() - 80;
                scrollView.smoothScrollTo(0, Math.max(0, y));
            }, 300);
            return;
        }

        if (TextUtils.isEmpty(weightStr)) {
            edtWeight.setError("Vui lòng nhập khối lượng");
            edtWeight.requestFocus();
            scrollView.postDelayed(() -> {
                int y = edtWeight.getTop() - 80;
                scrollView.smoothScrollTo(0, Math.max(0, y));
            }, 300);
            return;
        }

        if (TextUtils.isEmpty(priceStr)) {
            edtPrice.setError("Vui lòng nhập giá trị");
            edtPrice.requestFocus();
            scrollView.postDelayed(() -> {
                int y = edtPrice.getTop() - 80;
                scrollView.smoothScrollTo(0, Math.max(0, y));
            }, 300);
            return;
        }

        double weight = Double.parseDouble(weightStr);
        double price = Double.parseDouble(priceStr);

        if (imageUri == null && TextUtils.isEmpty(imageUrl)) {
            Toast.makeText(this, "Vui lòng chọn ảnh sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }

        String createdBy = mAuth.getCurrentUser() != null ?
                mAuth.getCurrentUser().getUid() : "unknown";

        if (imageUri != null) {
            uploadImageAndSave(productId, productName, description, weight, price, createdBy);
        } else {
            saveProductToFirestore(productId, productName, description, weight, price, createdBy, imageUrl);
        }
    }

    // ================== LOAD DỮ LIỆU KHI SỬA ==================

    private void loadProductData(Product product) {
        edtProductName.setText(product.getProductName());
        edtDescription.setText(product.getDescription());
        edtWeight.setText(String.valueOf(product.getWeight()));
        edtPrice.setText(String.valueOf((int) product.getPrice()));

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            imageUrl = product.getImageUrl();
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .into(imgProduct);
        }
    }

    // ================== MỞ THƯ VIỆN ẢNH ==================

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    // ================== CẬP NHẬT SẢN PHẨM ==================

    private void updateProduct() {
        String productName = edtProductName.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();
        String weightStr = edtWeight.getText().toString().trim();
        String priceStr = edtPrice.getText().toString().trim();

        if (TextUtils.isEmpty(productName)) {
            edtProductName.setError("Vui lòng nhập tên sản phẩm");
            edtProductName.requestFocus();
            scrollView.postDelayed(() -> {
                int y = edtProductName.getTop() - 80;
                scrollView.smoothScrollTo(0, Math.max(0, y));
            }, 300);
            return;
        }

        if (TextUtils.isEmpty(description)) {
            edtDescription.setError("Vui lòng nhập mô tả");
            edtDescription.requestFocus();
            scrollView.postDelayed(() -> {
                int y = edtDescription.getTop() - 80;
                scrollView.smoothScrollTo(0, Math.max(0, y));
            }, 300);
            return;
        }

        if (TextUtils.isEmpty(weightStr)) {
            edtWeight.setError("Vui lòng nhập khối lượng");
            edtWeight.requestFocus();
            scrollView.postDelayed(() -> {
                int y = edtWeight.getTop() - 80;
                scrollView.smoothScrollTo(0, Math.max(0, y));
            }, 300);
            return;
        }

        if (TextUtils.isEmpty(priceStr)) {
            edtPrice.setError("Vui lòng nhập giá trị");
            edtPrice.requestFocus();
            scrollView.postDelayed(() -> {
                int y = edtPrice.getTop() - 80;
                scrollView.smoothScrollTo(0, Math.max(0, y));
            }, 300);
            return;
        }

        double weight = Double.parseDouble(weightStr);
        double price = Double.parseDouble(priceStr);

        String createdBy = mAuth.getCurrentUser() != null ?
                mAuth.getCurrentUser().getUid() : "unknown";

        if (imageUri != null) {
            uploadImageAndUpdate(productName, description, weight, price, createdBy);
        } else {
            updateProductToFirestore(productName, description, weight, price, createdBy, imageUrl);
        }
    }

    // ================== UPLOAD ẢNH VÀ LƯU MỚI ==================

    private void uploadImageAndSave(String productId, String productName,
                                    String description, double weight,
                                    double price, String createdBy) {

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang upload ảnh...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
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
                        Toast.makeText(AddEditProductActivity.this,
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
                            boolean success = jsonObject.getBoolean("success");

                            if (success) {
                                JSONObject data = jsonObject.getJSONObject("data");
                                String imageUrl = data.getString("url");

                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    saveProductToFirestore(productId, productName, description,
                                            weight, price, createdBy, imageUrl);
                                });
                            } else {
                                String errorMsg = jsonObject.getString("error");
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(AddEditProductActivity.this,
                                            "Upload ảnh thất bại: " + errorMsg,
                                            Toast.LENGTH_LONG).show();
                                });
                            }

                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(AddEditProductActivity.this,
                                        "Lỗi xử lý ảnh: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(AddEditProductActivity.this,
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

    // ================== UPLOAD ẢNH VÀ CẬP NHẬT ==================

    private void uploadImageAndUpdate(String productName, String description,
                                      double weight, double price, String createdBy) {

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang upload ảnh mới...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
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
                        Toast.makeText(AddEditProductActivity.this,
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
                            boolean success = jsonObject.getBoolean("success");

                            if (success) {
                                JSONObject data = jsonObject.getJSONObject("data");
                                String imageUrl = data.getString("url");

                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    updateProductToFirestore(productName, description,
                                            weight, price, createdBy, imageUrl);
                                });
                            } else {
                                String errorMsg = jsonObject.getString("error");
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(AddEditProductActivity.this,
                                            "Upload ảnh thất bại: " + errorMsg,
                                            Toast.LENGTH_LONG).show();
                                });
                            }

                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(AddEditProductActivity.this,
                                        "Lỗi xử lý ảnh: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(AddEditProductActivity.this,
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

    // ================== LƯU SẢN PHẨM MỚI VÀO FIRESTORE ==================

    private void saveProductToFirestore(String productId, String productName,
                                        String description, double weight,
                                        double price, String createdBy,
                                        String imageUrl) {

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang lưu sản phẩm...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Map<String, Object> product = new HashMap<>();
        product.put("productName", productName);
        product.put("description", description);
        product.put("weight", weight);
        product.put("price", price);
        product.put("createdBy", createdBy);
        product.put("createdAt", Timestamp.now());
        product.put("imageUrl", imageUrl);

        db.collection("PRODUCTS")
                .document(productId)
                .set(product)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(AddEditProductActivity.this,
                            "Thêm sản phẩm thành công!\nMã: " + productId,
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(AddEditProductActivity.this,
                            "Lỗi lưu: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // ================== CẬP NHẬT SẢN PHẨM TRONG FIRESTORE ==================

    private void updateProductToFirestore(String productName, String description,
                                          double weight, double price,
                                          String createdBy, String imageUrl) {

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang cập nhật sản phẩm...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Map<String, Object> updates = new HashMap<>();
        updates.put("productName", productName);
        updates.put("description", description);
        updates.put("weight", weight);
        updates.put("price", price);
        updates.put("createdBy", createdBy);
        updates.put("imageUrl", imageUrl);

        db.collection("PRODUCTS")
                .document(productId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(AddEditProductActivity.this,
                            "Cập nhật sản phẩm thành công!",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(AddEditProductActivity.this,
                            "Lỗi cập nhật: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}