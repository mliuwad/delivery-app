package com.example.udgh.models;

public class OrderDetail {
    private String detailId;
    private String orderId;
    private String productId;

    private String productName;
    private String description;

    private double weight;
    private double price;

    private int quantity;
    private String imageUrl;  // ✅ Thêm field này

    public OrderDetail() {}

    public OrderDetail(String detailId, String orderId, String productId,
                       String productName, String description,
                       double weight, double price, int quantity) {
        this.detailId = detailId;
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.description = description;
        this.weight = weight;
        this.price = price;
        this.quantity = quantity;
    }

    // Getter & Setter
    public String getDetailId() { return detailId; }
    public void setDetailId(String detailId) { this.detailId = detailId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}