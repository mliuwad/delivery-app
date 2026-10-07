package com.example.udgh.models;

import com.google.firebase.Timestamp;

import java.sql.Time;

public class DeliveryHistory {
    private String historyId;
    private String orderId;
    private String shipperId;

    private String status;
    private String note;

    private Timestamp updatedAt;

    public DeliveryHistory(String historyId, String orderId, String shipperId, String status, String note, Timestamp updatedAt) {
        this.historyId = historyId;
        this.orderId = orderId;
        this.shipperId = shipperId;
        this.status = status;
        this.note = note;
        this.updatedAt = updatedAt;
    }

    public String getHistoryId() {
        return historyId;
    }

    public void setHistoryId(String historyId) {
        this.historyId = historyId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getShipperId() {
        return shipperId;
    }

    public void setShipperId(String shipperId) {
        this.shipperId = shipperId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
