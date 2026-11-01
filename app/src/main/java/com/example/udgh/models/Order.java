package com.example.udgh.models;

import com.google.firebase.Timestamp;
import java.util.List;

public class Order {
    private String orderId;
    private String creatorId;
    private String shipperId;
    private String senderName;
    private String senderPhone;
    private String senderAddress;
    private double senderLat;
    private double senderLng;
    private String receiverName;
    private String receiverPhone;
    private String deliveryAddress;
    private double receiverLat;
    private double receiverLng;
    private double deliveryFee;
    private boolean isCOD;
    private double codAmount;
    private double totalProductValue;
    private String status;
    private String note;
    private Timestamp createdAt;
    private Timestamp deliveredAt;

    // ✅ Danh sách OrderDetail (tạm, không lưu Firestore)
    private List<OrderDetail> orderDetails;

    public Order() {}

    // Getter & Setter
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }

    public String getShipperId() { return shipperId; }
    public void setShipperId(String shipperId) { this.shipperId = shipperId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderPhone() { return senderPhone; }
    public void setSenderPhone(String senderPhone) { this.senderPhone = senderPhone; }

    public String getSenderAddress() { return senderAddress; }
    public void setSenderAddress(String senderAddress) { this.senderAddress = senderAddress; }

    public double getSenderLat() { return senderLat; }
    public void setSenderLat(double senderLat) { this.senderLat = senderLat; }

    public double getSenderLng() { return senderLng; }
    public void setSenderLng(double senderLng) { this.senderLng = senderLng; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public double getReceiverLat() { return receiverLat; }
    public void setReceiverLat(double receiverLat) { this.receiverLat = receiverLat; }

    public double getReceiverLng() { return receiverLng; }
    public void setReceiverLng(double receiverLng) { this.receiverLng = receiverLng; }

    public double getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(double deliveryFee) { this.deliveryFee = deliveryFee; }

    public boolean isCOD() { return isCOD; }
    public void setCOD(boolean COD) { isCOD = COD; }

    public double getCodAmount() { return codAmount; }
    public void setCodAmount(double codAmount) { this.codAmount = codAmount; }

    public double getTotalProductValue() { return totalProductValue; }
    public void setTotalProductValue(double totalProductValue) { this.totalProductValue = totalProductValue; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Timestamp deliveredAt) { this.deliveredAt = deliveredAt; }

    public List<OrderDetail> getOrderDetails() { return orderDetails; }
    public void setOrderDetails(List<OrderDetail> orderDetails) { this.orderDetails = orderDetails; }
}