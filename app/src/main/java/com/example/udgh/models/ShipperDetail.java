package com.example.udgh.models;

import com.google.firebase.Timestamp;

public class ShipperDetail {

    private String shipperId;
    private String citizenId;
    private String licensePlate;
    private String address;
    private Timestamp joinDate;

    public ShipperDetail() {
    }

    public ShipperDetail(String shipperId, String citizenId,
                         String licensePlate, String address,
                         Timestamp joinDate) {

        this.shipperId = shipperId;
        this.citizenId = citizenId;
        this.licensePlate = licensePlate;
        this.address = address;
        this.joinDate = joinDate;
    }

    public String getShipperId() {
        return shipperId;
    }

    public void setShipperId(String shipperId) {
        this.shipperId = shipperId;
    }

    public String getCitizenId() {
        return citizenId;
    }

    public void setCitizenId(String citizenId) {
        this.citizenId = citizenId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Timestamp getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(Timestamp joinDate) {
        this.joinDate = joinDate;
    }
}