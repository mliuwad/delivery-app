package com.example.udgh.models;

import com.google.firebase.Timestamp;

public class Salary {
    private String salaryId;
    private String shipperId;

    private int month;
    private int year;

    private int totalOrders;

    private double salaryPerOrder;
    private double totalSalary;

    private String paymentStatus;
    private Timestamp paymentDate;

    public Salary(String salaryId, String shipperId, int month, int year, int totalOrders, double salaryPerOrder, double totalSalary, String paymentStatus, Timestamp paymentDate) {
        this.salaryId = salaryId;
        this.shipperId = shipperId;
        this.month = month;
        this.year = year;
        this.totalOrders = totalOrders;
        this.salaryPerOrder = salaryPerOrder;
        this.totalSalary = totalSalary;
        this.paymentStatus = paymentStatus;
        this.paymentDate = paymentDate;
    }

    public String getSalaryId() {
        return salaryId;
    }

    public void setSalaryId(String salaryId) {
        this.salaryId = salaryId;
    }

    public String getShipperId() {
        return shipperId;
    }

    public void setShipperId(String shipperId) {
        this.shipperId = shipperId;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }

    public double getSalaryPerOrder() {
        return salaryPerOrder;
    }

    public void setSalaryPerOrder(double salaryPerOrder) {
        this.salaryPerOrder = salaryPerOrder;
    }

    public double getTotalSalary() {
        return totalSalary;
    }

    public void setTotalSalary(double totalSalary) {
        this.totalSalary = totalSalary;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Timestamp getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Timestamp paymentDate) {
        this.paymentDate = paymentDate;
    }
}
