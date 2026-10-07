package com.example.udgh.models;

import com.google.firebase.Timestamp;
import java.io.Serializable;

public class Product implements Serializable {

    private String documentId;      // ID của document trong Firestore
    private String productId;       // Mã sản phẩm (SP001, SP002...)
    private String productName;     // Tên sản phẩm
    private String description;     // Mô tả sản phẩm
    private double weight;          // Cân nặng (kg)
    private double price;           // Giá sản phẩm
    private String createdBy;       // Người tạo (UID)
    private Timestamp createdAt;    // Thời gian tạo
    private String imageUrl;        // URL ảnh (tương ứng field "picture" trong Firestore)

    // ================== CONSTRUCTOR ==================

    public Product() {
        // Constructor rỗng bắt buộc cho Firestore
    }

    public Product(String productId, String productName,
                   String description, double weight,
                   double price, String createdBy,
                   Timestamp createdAt, String imageUrl) {
        this.productId = productId;
        this.productName = productName;
        this.description = description;
        this.weight = weight;
        this.price = price;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.imageUrl = imageUrl;
    }

    // ================== GETTER & SETTER ==================

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    // ================== TO STRING ==================

    @Override
    public String toString() {
        return "Product{" +
                "documentId='" + documentId + '\'' +
                ", productId='" + productId + '\'' +
                ", productName='" + productName + '\'' +
                ", description='" + description + '\'' +
                ", weight=" + weight +
                ", price=" + price +
                ", createdBy='" + createdBy + '\'' +
                ", createdAt=" + createdAt +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}