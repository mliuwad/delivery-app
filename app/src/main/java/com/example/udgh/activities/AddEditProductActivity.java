
package com.example.udgh.activities;

import com.example.udgh.adapters.*;
import com.example.udgh.dialogs.*;
import com.example.udgh.utils.*;
import com.example.udgh.fragments.*;
import com.example.udgh.R;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.udgh.models.Product;
import com.example.udgh.viewmodels.ProductViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class AddEditProductActivity extends AppCompatActivity {



    private MaterialToolbar toolbar;
    private ImageView imgProduct;
    private MaterialButton btnChooseImage;
    private MaterialButton btnSave;

    private TextInputEditText edtProductName;
    private TextInputEditText edtDescription;
    private TextInputEditText edtWeight;
    private TextInputEditText edtPrice;

    private ScrollView scrollView;
    private View rootView;


    private ProductViewModel productViewModel;

    private Uri imageUri;
    private String imageUrl = "";
    private String productId = "";

    private boolean isEditMode = false;
    private String currentMode = "add";

    private ProgressDialog progressDialog;
    private boolean creatorPermissionGranted = false;
    private boolean productPermissionCheckStarted = false;



    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null) {

                            imageUri = result.getData().getData();

                            if (imageUri != null) {
                                Glide.with(this)
                                        .load(imageUri)
                                        .centerCrop()
                                        .into(imgProduct);

                                Toast.makeText(
                                        this,
                                        "Đã chọn ảnh mới",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                    }
            );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_edit_product);


        View mainView = findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
            );

            Insets ime = insets.getInsets(
                    WindowInsetsCompat.Type.ime()
            );

            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    0
            );

            scrollView.setPadding(
                    0,
                    0,
                    0,
                    Math.max(systemBars.bottom, ime.bottom)
            );

            if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                scrollView.post(() -> {
                    Rect rect = new Rect();
                    edtPrice.getDrawingRect(rect);

                    scrollView.offsetDescendantRectToMyCoords(
                            edtPrice, rect
                    );

                    int visibleBottom =
                            scrollView.getHeight() - scrollView.getPaddingBottom();

                    int distance = rect.bottom - visibleBottom + 32;

                    if (distance > 0) {
                        scrollView.smoothScrollBy(0, distance);
                    }
                });
            }

            return insets;
        });

        productViewModel = new ViewModelProvider(this)
                .get(ProductViewModel.class);

        initViews();
        setupToolbar();
        setupMode();
        setupListeners();
        observeViewModel();
    }

    private void initViews() {
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
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    // ================== CHẾ ĐỘ MÀN HÌNH ==================

    private void setupMode() {
        Intent intent = getIntent();

        String mode = intent.getStringExtra("mode");

        if (mode != null) {
            currentMode = mode;
        }

        productId = intent.getStringExtra("productId");

        if (productId != null && !productId.isEmpty()) {

            isEditMode = true;

            if ("view".equals(currentMode)) {
                toolbar.setTitle("Chi tiết sản phẩm");
                btnSave.setVisibility(View.GONE);
                btnChooseImage.setVisibility(View.GONE);
                setFieldsReadOnly(true);

            } else {
                currentMode = "edit";
                toolbar.setTitle("Sửa sản phẩm");
                btnSave.setText("Cập nhật");
                setFieldsReadOnly(false);
            }

            productViewModel.loadProductById(productId);

        } else {
            currentMode = "add";
            toolbar.setTitle("Thêm sản phẩm");
            btnSave.setText("Lưu thông tin");
            btnSave.setEnabled(false);
            setFieldsReadOnly(false);
            verifyCreatorForAdd();
        }



    }

    private void verifyCreatorForAdd() {
        productViewModel.verifyAddPermission();
    }

    private void loadProductData(Product product) {
        edtProductName.setText(product.getProductName());
        edtDescription.setText(product.getDescription());
        edtWeight.setText(String.valueOf(product.getWeight()));
        edtPrice.setText(String.valueOf((int) product.getPrice()));

        imageUrl = product.getImageUrl() == null
                ? ""
                : product.getImageUrl();

        if (!imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.box)
                    .error(R.drawable.box)
                    .centerCrop()
                    .into(imgProduct);
        }
    }

    private void setFieldsReadOnly(boolean readOnly) {
        edtProductName.setEnabled(!readOnly);
        edtDescription.setEnabled(!readOnly);
        edtWeight.setEnabled(!readOnly);
        edtPrice.setEnabled(!readOnly);

        int textColor = getColor(R.color.black);

        edtProductName.setTextColor(textColor);
        edtDescription.setTextColor(textColor);
        edtWeight.setTextColor(textColor);
        edtPrice.setTextColor(textColor);
    }


    private void setupListeners() {
        btnChooseImage.setOnClickListener(v -> openImagePicker());

        btnSave.setOnClickListener(v -> {
            if ("edit".equals(currentMode)) {
                updateProduct();
            } else if ("add".equals(currentMode)) {
                addProduct();
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        );

        imagePickerLauncher.launch(intent);
    }

    // THÊM SẢN PHẨM

    private void addProduct() {
        if (!creatorPermissionGranted) { Toast.makeText(this, "Tài khoản chưa được cấp quyền Creator", Toast.LENGTH_SHORT).show(); return; }
        if (!validateInput()) {
            return;
        }

        Product product = createProductFromInput();

        String currentUserId = productViewModel.getCurrentUserUid();
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        product.setCreatedBy(currentUserId);

        byte[] imageBytes = readSelectedImage();

        if (imageBytes == null || imageBytes.length == 0) {
            Toast.makeText(
                    this,
                    "Vui lòng chọn ảnh sản phẩm",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        productViewModel.addProduct(product, imageBytes);
    }

    //CẬP NHẬT SẢN PHẨM

    private void updateProduct() {
        if (!creatorPermissionGranted) { Toast.makeText(this, "Bạn không có quyền sửa sản phẩm", Toast.LENGTH_SHORT).show(); return; }
        if (!validateInput()) {
            return;
        }

        Product product = createProductFromInput();

        product.setProductId(productId);
        product.setImageUrl(imageUrl);

        // Giữ nguyên createdBy của sản phẩm; không chuyển quyền sở hữu khi sửa.

        byte[] newImageBytes = null;

        if (imageUri != null) {
            newImageBytes = readSelectedImage();

            if (newImageBytes == null || newImageBytes.length == 0) {
                Toast.makeText(
                        this,
                        "Không thể đọc ảnh đã chọn",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
        }

        productViewModel.updateProduct(product, newImageBytes);
    }

    private Product createProductFromInput() {
        Product product = new Product();

        product.setProductName(
                edtProductName.getText().toString().trim()
        );

        product.setDescription(
                edtDescription.getText().toString().trim()
        );

        product.setWeight(
                Double.parseDouble(
                        edtWeight.getText().toString().trim()
                )
        );

        product.setPrice(
                Double.parseDouble(
                        edtPrice.getText().toString().trim()
                )
        );

        return product;
    }

    // KIỂM TRA DỮ LIỆU

    private boolean validateInput() {
        String productName =
                edtProductName.getText().toString().trim();

        String description =
                edtDescription.getText().toString().trim();

        String weightText =
                edtWeight.getText().toString().trim();

        String priceText =
                edtPrice.getText().toString().trim();

        if (TextUtils.isEmpty(productName)) {
            edtProductName.setError("Vui lòng nhập tên sản phẩm");
            edtProductName.requestFocus();
            scrollToView(edtProductName);
            return false;
        }

        if (TextUtils.isEmpty(description)) {
            edtDescription.setError("Vui lòng nhập mô tả");
            edtDescription.requestFocus();
            scrollToView(edtDescription);
            return false;
        }

        if (TextUtils.isEmpty(weightText)) {
            edtWeight.setError("Vui lòng nhập khối lượng");
            edtWeight.requestFocus();
            scrollToView(edtWeight);
            return false;
        }

        if (TextUtils.isEmpty(priceText)) {
            edtPrice.setError("Vui lòng nhập giá trị");
            edtPrice.requestFocus();
            scrollToView(edtPrice);
            return false;
        }

        try {
            double weight = Double.parseDouble(weightText);
            double price = Double.parseDouble(priceText);

            if (weight <= 0) {
                edtWeight.setError("Khối lượng phải lớn hơn 0");
                edtWeight.requestFocus();
                return false;
            }

            if (price < 0) {
                edtPrice.setError("Giá trị không được âm");
                edtPrice.requestFocus();
                return false;
            }

        } catch (NumberFormatException e) {
            Toast.makeText(
                    this,
                    "Khối lượng hoặc giá trị không hợp lệ",
                    Toast.LENGTH_SHORT
            ).show();
            return false;
        }

        return true;
    }

    // ĐỌC ẢNH
    private byte[] readSelectedImage() {
        if (imageUri == null) {
            return null;
        }

        try (InputStream inputStream =
                     getContentResolver().openInputStream(imageUri);
             ByteArrayOutputStream outputStream =
                     new ByteArrayOutputStream()) {

            if (inputStream == null) {
                return null;
            }

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return outputStream.toByteArray();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Lỗi đọc ảnh: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            return null;
        }
    }

    // QUAN SÁT VIEWMODEL

    private void observeViewModel() {
        productViewModel.getIsLoading().observe(this, loading -> {
            if (loading == null) {
                return;
            }

            if (loading) {
                showProgressDialog();
            } else {
                hideProgressDialog();
            }
        });
        productViewModel.getSelectedProduct().observe(this, product -> {
            if (product != null && !productPermissionCheckStarted) {
                productPermissionCheckStarted = true;
                verifyPermissionForExistingProduct(product);
            }
        });

        productViewModel.getAccessResult().observe(this, access -> {
            if (access == null) return;

            if (!access.isAllowed()) {
                Toast.makeText(this, access.getMessage(), Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            creatorPermissionGranted = access.isCreator();

            if (currentMode.equals("add")) {
                btnSave.setEnabled(true);
                return;
            }

            if (access.isAdmin()) {
                btnSave.setVisibility(View.GONE);
                btnChooseImage.setVisibility(View.GONE);
                setFieldsReadOnly(true);
            }

            Product selected = productViewModel.getSelectedProduct().getValue();
            if (selected != null) {
                loadProductData(selected);
            }
        });

        productViewModel.getOperationResult().observe(this, result -> {
            if (result == null) {
                return;
            }

            Toast.makeText(
                    this,
                    result.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            if (result.isSuccess()) {
                finish();
            }
        });
    }

    private void verifyPermissionForExistingProduct(Product product) {
        productViewModel.verifyProductPermission(product, currentMode);
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }

        if (!progressDialog.isShowing()) {
            progressDialog.setMessage(
                    "Đang xử lý dữ liệu sản phẩm..."
            );
            progressDialog.show();
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        hideProgressDialog();
        super.onDestroy();
    }

    //CUỘN MÀN HÌNH

    private void scrollToView(View view) {
        scrollView.postDelayed(() -> {
            int y = view.getTop() - 80;
            scrollView.smoothScrollTo(0, Math.max(0, y));
        }, 300);
    }


}
