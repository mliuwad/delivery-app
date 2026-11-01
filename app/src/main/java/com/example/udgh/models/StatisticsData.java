package com.example.udgh.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsData {
    private int totalOrders;
    private double totalRevenue;
    private long completedOrders;
    private int shipperCount;
    private final Map<String, Integer> statusCounts = new HashMap<>();
    private final List<String> monthLabels = new ArrayList<>();
    private final List<Float> monthlyRevenue = new ArrayList<>();
    private final List<TopShipper> topShippers = new ArrayList<>();

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int value) { totalOrders = value; }
    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double value) { totalRevenue = value; }
    public long getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(long value) { completedOrders = value; }
    public int getShipperCount() { return shipperCount; }
    public void setShipperCount(int value) { shipperCount = value; }
    public Map<String, Integer> getStatusCounts() { return statusCounts; }
    public List<String> getMonthLabels() { return monthLabels; }
    public List<Float> getMonthlyRevenue() { return monthlyRevenue; }
    public List<TopShipper> getTopShippers() { return topShippers; }

    public static class TopShipper {
        private final User user;
        private final double totalSalary;
        private final long totalOrders;

        public TopShipper(User user, double totalSalary, long totalOrders) {
            this.user = user;
            this.totalSalary = totalSalary;
            this.totalOrders = totalOrders;
        }
        public User getUser() { return user; }
        public double getTotalSalary() { return totalSalary; }
        public long getTotalOrders() { return totalOrders; }
    }
}
