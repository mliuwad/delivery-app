
package com.example.udgh.repositories;

import com.example.udgh.models.Product;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProductRepository {

    private final FirebaseFirestore db =
            FirebaseFirestore.getInstance();

    private static final String COLLECTION = "PRODUCTS";

    public interface ResultCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    /**
     * Lấy role của tài khoản đang đăng nhập.
     * Repository chịu trách nhiệm giao tiếp với Firebase Auth/Firestore.
     */
    public void getCurrentUserRole(ResultCallback<String> callback) {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (uid == null || uid.trim().isEmpty()) {
            callback.onError(new SecurityException("Vui lòng đăng nhập"));
            return;
        }

        db.collection("USERS").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onError(new SecurityException("Không tìm thấy thông tin tài khoản"));
                        return;
                    }

                    String role = document.getString("role");
                    if (role == null || role.trim().isEmpty()) {
                        callback.onError(new SecurityException("Tài khoản chưa được phân quyền"));
                        return;
                    }

                    callback.onSuccess(role);
                })
                .addOnFailureListener(callback::onError);
    }

    public String getCurrentUserUid() {
        return com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
    }

    // TẢI DANH SÁCH SẢN PHẨM
    public void getProducts(String ownerUid, boolean isAdmin, ResultCallback<List<Product>> callback) {
        com.google.firebase.firestore.Query query = db.collection(COLLECTION);
        if (!isAdmin) {
            if (ownerUid == null || ownerUid.trim().isEmpty()) {
                callback.onError(new IllegalStateException("Không xác định được chủ shop"));
                return;
            }
            query = query.whereEqualTo("createdBy", ownerUid);
        }
        query.get()
                .addOnSuccessListener(snapshot -> {

                    List<Product> products = new ArrayList<>();

                    for (QueryDocumentSnapshot document : snapshot) {

                        Boolean active = document.getBoolean("isActive");

                        if (Boolean.FALSE.equals(active)) {
                            continue;
                        }

                        Product product = new Product();

                        product.setProductId(document.getId());
                        product.setProductName(document.getString("productName"));
                        product.setDescription(document.getString("description"));
                        product.setImageUrl(document.getString("imageUrl"));
                        product.setCreatedBy(document.getString("createdBy"));
                        product.setCreatedAt(document.getTimestamp("createdAt"));

                        Double price = document.getDouble("price");
                        Double weight = document.getDouble("weight");

                        product.setPrice(price != null ? price : 0);
                        product.setWeight(weight != null ? weight : 0);
                        product.setActive(active == null || active);

                        products.add(product);
                    }

                    callback.onSuccess(products);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getProducts(ResultCallback<List<Product>> callback) {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        getProducts(uid, false, callback);
    }

    // THÊM SẢN PHẨM
    public void addProduct(
            Product product,
            ResultCallback<String> callback) {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (uid == null || !uid.equals(product.getCreatedBy())) {
            callback.onError(new SecurityException("Không xác định được chủ shop"));
            return;
        }

        // Firestore tự tạo ID duy nhất; không cần đọc toàn bộ PRODUCTS.
        com.google.firebase.firestore.DocumentReference newProductRef =
                db.collection(COLLECTION).document();
        String newId = newProductRef.getId();

        Map<String, Object> data = new HashMap<>();
        data.put("productId", newId);
        data.put("productName", product.getProductName());
        data.put("description", product.getDescription());
        data.put("weight", product.getWeight());
        data.put("price", product.getPrice());
        data.put("createdBy", uid);
        data.put("createdAt", Timestamp.now());
        data.put("imageUrl", product.getImageUrl());
        data.put("isActive", true);

        newProductRef.set(data)
                .addOnSuccessListener(unused -> callback.onSuccess(newId))
                .addOnFailureListener(callback::onError);
    }

    // CẬP NHẬT SẢN PHẨM
    public void updateProduct(
            Product product,
            ResultCallback<Void> callback) {

        Map<String, Object> updates = new HashMap<>();

        updates.put("productName", product.getProductName());
        updates.put("description", product.getDescription());
        updates.put("weight", product.getWeight());
        updates.put("price", product.getPrice());
        updates.put("imageUrl", product.getImageUrl());

        com.google.firebase.firestore.DocumentReference ref = db.collection(COLLECTION).document(product.getProductId());
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot snapshot = transaction.get(ref);
            if (uid == null || !snapshot.exists() || !uid.equals(snapshot.getString("createdBy"))) {
                throw new SecurityException("Bạn chỉ được sửa sản phẩm của shop mình");
            }
            transaction.update(ref, updates);
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess(null)).addOnFailureListener(callback::onError);
    }

    // NGỪNG KINH DOANH
    public void deactivateProduct(
            String productId,
            ResultCallback<Void> callback) {

        com.google.firebase.firestore.DocumentReference ref = db.collection(COLLECTION).document(productId);
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot snapshot = transaction.get(ref);
            if (uid == null || !snapshot.exists() || !uid.equals(snapshot.getString("createdBy"))) {
                throw new SecurityException("Bạn chỉ được ngừng kinh doanh sản phẩm của shop mình");
            }
            transaction.update(ref, "isActive", false);
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess(null)).addOnFailureListener(callback::onError);
    }


    public void getProductById(
            String productId,
            ResultCallback<Product> callback) {

        db.collection(COLLECTION)
                .document(productId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        callback.onError(
                                new Exception("Không tìm thấy sản phẩm")
                        );
                        return;
                    }

                    Product product = new Product();

                    product.setProductId(document.getId());
                    product.setProductName(
                            document.getString("productName")
                    );
                    product.setDescription(
                            document.getString("description")
                    );
                    product.setImageUrl(
                            document.getString("imageUrl")
                    );
                    product.setCreatedBy(
                            document.getString("createdBy")
                    );
                    product.setCreatedAt(
                            document.getTimestamp("createdAt")
                    );

                    Double price = document.getDouble("price");
                    Double weight = document.getDouble("weight");

                    product.setPrice(price != null ? price : 0);
                    product.setWeight(weight != null ? weight : 0);

                    Boolean active = document.getBoolean("isActive");
                    product.setActive(active == null || active);

                    callback.onSuccess(product);
                })
                .addOnFailureListener(callback::onError);
    }
}
