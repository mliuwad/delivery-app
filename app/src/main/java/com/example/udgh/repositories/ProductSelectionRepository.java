package com.example.udgh.repositories;

import com.example.udgh.models.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProductSelectionRepository {
    public interface Callback {
        void onSuccess(List<Product> products);
        void onError(Exception error);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void loadProductsForCurrentCreator(Callback callback) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || uid.trim().isEmpty()) {
            callback.onError(new IllegalStateException("Vui lòng đăng nhập"));
            return;
        }

        db.collection("PRODUCTS")
                .whereEqualTo("createdBy", uid)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Product> products = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Product product = new Product();
                        product.setProductId(doc.getId());
                        product.setCreatedBy(doc.getString("createdBy"));
                        product.setProductName(doc.getString("productName"));
                        product.setPrice(doc.getDouble("price") == null ? 0 : doc.getDouble("price"));
                        product.setImageUrl(doc.getString("imageUrl"));
                        product.setDescription(doc.getString("description"));
                        product.setWeight(doc.getDouble("weight") == null ? 0 : doc.getDouble("weight"));
                        products.add(product);
                    }
                    callback.onSuccess(products);
                })
                .addOnFailureListener(callback::onError);
    }
}
