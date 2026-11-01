
package com.example.udgh.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.udgh.models.Product;
import com.example.udgh.repositories.ProductImageRepository;
import com.example.udgh.repositories.ProductRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductViewModel extends ViewModel {

    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;

    // Danh sách sản phẩm gốc lấy từ Firestore
    private final List<Product> allProducts = new ArrayList<>();

    // Danh sách sản phẩm sau khi tìm kiếm
    private final MutableLiveData<List<Product>> products =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<Boolean> isLoading =
            new MutableLiveData<>(false);

    private final MutableLiveData<OperationResult> operationResult =
            new MutableLiveData<>();

    public ProductViewModel() {
        productRepository = new ProductRepository();
        imageRepository = new ProductImageRepository();
    }

    private final MutableLiveData<Boolean> adminAccount = new MutableLiveData<>(false);
    private final MutableLiveData<AccessResult> accessResult = new MutableLiveData<>();

    public LiveData<Boolean> getAdminAccount() {
        return adminAccount;
    }

    public LiveData<AccessResult> getAccessResult() {
        return accessResult;
    }

    public String getCurrentUserUid() {
        return productRepository.getCurrentUserUid();
    }

    /**
     * Xác định role và tải danh sách sản phẩm theo quyền.
     * Activity không cần tự đọc USERS trên Firestore.
     */
    public void loadProductsForCurrentUser() {
        isLoading.setValue(true);
        productRepository.getCurrentUserRole(new ProductRepository.ResultCallback<String>() {
            @Override
            public void onSuccess(String role) {
                boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
                boolean isCreator = "CREATOR".equalsIgnoreCase(role);

                if (!isAdmin && !isCreator) {
                    isLoading.postValue(false);
                    operationResult.postValue(new OperationResult(
                            false, "Tài khoản không có quyền quản lý sản phẩm"));
                    return;
                }

                adminAccount.postValue(isAdmin);
                loadProducts(productRepository.getCurrentUserUid(), isAdmin);
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                operationResult.postValue(new OperationResult(
                        false, "Không thể xác định quyền tài khoản: " + e.getMessage()));
            }
        });
    }

    public void verifyAddPermission() {
        productRepository.getCurrentUserRole(new ProductRepository.ResultCallback<String>() {
            @Override
            public void onSuccess(String role) {
                boolean allowed = "CREATOR".equalsIgnoreCase(role);
                accessResult.postValue(new AccessResult(
                        allowed, allowed, false,
                        allowed ? "" : "Chỉ Creator mới được thêm sản phẩm"));
            }

            @Override
            public void onError(Exception e) {
                accessResult.postValue(new AccessResult(
                        false, false, false,
                        "Không thể xác minh quyền tài khoản: " + e.getMessage()));
            }
        });
    }

    public void verifyProductPermission(Product product, String mode) {
        if (product == null) {
            accessResult.postValue(new AccessResult(
                    false, false, false, "Không tìm thấy sản phẩm"));
            return;
        }

        productRepository.getCurrentUserRole(new ProductRepository.ResultCallback<String>() {
            @Override
            public void onSuccess(String role) {
                String uid = productRepository.getCurrentUserUid();
                boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
                boolean isCreator = "CREATOR".equalsIgnoreCase(role);
                boolean isOwner = isCreator && uid != null && uid.equals(product.getCreatedBy());
                boolean allowed = isOwner || (isAdmin && "view".equals(mode));

                accessResult.postValue(new AccessResult(
                        allowed, isOwner, isAdmin,
                        allowed ? "" : "Bạn không có quyền truy cập sản phẩm này"));
            }

            @Override
            public void onError(Exception e) {
                accessResult.postValue(new AccessResult(
                        false, false, false,
                        "Không thể xác minh quyền truy cập: " + e.getMessage()));
            }
        });
    }

    public static class AccessResult {
        private final boolean allowed;
        private final boolean creator;
        private final boolean admin;
        private final String message;

        public AccessResult(boolean allowed, boolean creator, boolean admin, String message) {
            this.allowed = allowed;
            this.creator = creator;
            this.admin = admin;
            this.message = message;
        }

        public boolean isAllowed() { return allowed; }
        public boolean isCreator() { return creator; }
        public boolean isAdmin() { return admin; }
        public String getMessage() { return message; }
    }

    public LiveData<List<Product>> getProducts() {
        return products;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<OperationResult> getOperationResult() {
        return operationResult;
    }

    // ================== TẢI DANH SÁCH ==================

    public void loadProducts() {
        loadProducts(com.google.firebase.auth.FirebaseAuth.getInstance().getUid(), false);
    }

    public void loadProducts(String ownerUid, boolean isAdmin) {
        isLoading.setValue(true);

        productRepository.getProducts(ownerUid, isAdmin, new ProductRepository.ResultCallback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> result) {
                allProducts.clear();
                allProducts.addAll(result);

                products.postValue(new ArrayList<>(allProducts));
                isLoading.postValue(false);
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                operationResult.postValue(
                        new OperationResult(false,
                                "Không thể tải sản phẩm: " + e.getMessage())
                );
            }
        });
    }

    // ================== TÌM KIẾM ==================

    public void searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            products.setValue(new ArrayList<>(allProducts));
            return;
        }

        String searchText = keyword.trim().toLowerCase(Locale.ROOT);

        List<Product> filteredProducts = new ArrayList<>();

        for (Product product : allProducts) {
            String productName = product.getProductName() == null
                    ? ""
                    : product.getProductName().toLowerCase(Locale.ROOT);

            String productId = product.getProductId() == null
                    ? ""
                    : product.getProductId().toLowerCase(Locale.ROOT);

            if (productName.contains(searchText)
                    || productId.contains(searchText)) {
                filteredProducts.add(product);
            }
        }

        products.setValue(filteredProducts);
    }

    // ================== THÊM SẢN PHẨM ==================

    public void addProduct(Product product, byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            operationResult.setValue(
                    new OperationResult(false, "Vui lòng chọn ảnh sản phẩm")
            );
            return;
        }

        isLoading.setValue(true);

        imageRepository.uploadImage(imageBytes,
                new ProductImageRepository.ImageCallback() {
                    @Override
                    public void onSuccess(String imageUrl) {
                        product.setImageUrl(imageUrl);
                        saveNewProduct(product);
                    }

                    @Override
                    public void onError(Exception e) {
                        finishWithError("Lỗi upload ảnh: " + e.getMessage());
                    }
                });
    }

    private void saveNewProduct(Product product) {
        productRepository.addProduct(product,
                new ProductRepository.ResultCallback<String>() {
                    @Override
                    public void onSuccess(String productId) {
                        finishWithSuccess(
                                "Thêm sản phẩm thành công! Mã: " + productId
                        );
                    }

                    @Override
                    public void onError(Exception e) {
                        finishWithError(
                                "Lỗi thêm sản phẩm: " + e.getMessage()
                        );
                    }
                });
    }

    // ================== CẬP NHẬT SẢN PHẨM ==================

    public void updateProduct(Product product, byte[] newImageBytes) {
        isLoading.setValue(true);

        // Nếu người dùng chọn ảnh mới thì upload ảnh trước
        if (newImageBytes != null && newImageBytes.length > 0) {
            imageRepository.uploadImage(newImageBytes,
                    new ProductImageRepository.ImageCallback() {
                        @Override
                        public void onSuccess(String imageUrl) {
                            product.setImageUrl(imageUrl);
                            saveUpdatedProduct(product);
                        }

                        @Override
                        public void onError(Exception e) {
                            finishWithError(
                                    "Lỗi upload ảnh: " + e.getMessage()
                            );
                        }
                    });
        } else {
            // Không chọn ảnh mới thì giữ URL ảnh cũ
            saveUpdatedProduct(product);
        }
    }

    private void saveUpdatedProduct(Product product) {
        productRepository.updateProduct(product,
                new ProductRepository.ResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        finishWithSuccess("Cập nhật sản phẩm thành công");
                    }

                    @Override
                    public void onError(Exception e) {
                        finishWithError(
                                "Lỗi cập nhật sản phẩm: " + e.getMessage()
                        );
                    }
                });
    }

    // ================== NGỪNG KINH DOANH ==================

    public void deactivateProduct(String productId) {
        isLoading.setValue(true);

        productRepository.deactivateProduct(productId,
                new ProductRepository.ResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        finishWithSuccess(
                                "Đã ngừng kinh doanh sản phẩm " + productId
                        );
                    }

                    @Override
                    public void onError(Exception e) {
                        finishWithError(
                                "Lỗi ngừng kinh doanh: " + e.getMessage()
                        );
                    }
                });
    }

    // ================== XỬ LÝ KẾT QUẢ ==================

    private void finishWithSuccess(String message) {
        isLoading.postValue(false);
        operationResult.postValue(
                new OperationResult(true, message)
        );

        // Tải lại danh sách sau khi thêm, sửa hoặc ngừng kinh doanh
        loadProducts();
    }

    private void finishWithError(String message) {
        isLoading.postValue(false);
        operationResult.postValue(
                new OperationResult(false, message)
        );
    }

    // Đối tượng chứa kết quả thao tác
    public static class OperationResult {

        private final boolean success;
        private final String message;

        public OperationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }


    private final MutableLiveData<Product> selectedProduct =
            new MutableLiveData<>();

    public LiveData<Product> getSelectedProduct() {
        return selectedProduct;
    }
    public void loadProductById(String productId) {
        isLoading.setValue(true);

        productRepository.getProductById(
                productId,
                new ProductRepository.ResultCallback<Product>() {

                    @Override
                    public void onSuccess(Product product) {
                        selectedProduct.postValue(product);
                        isLoading.postValue(false);
                    }

                    @Override
                    public void onError(Exception e) {
                        isLoading.postValue(false);

                        operationResult.postValue(
                                new OperationResult(
                                        false,
                                        "Không thể tải sản phẩm: "
                                                + e.getMessage()
                                )
                        );
                    }
                }
        );
    }
}
