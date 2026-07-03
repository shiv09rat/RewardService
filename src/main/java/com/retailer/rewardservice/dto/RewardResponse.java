package com.retailer.rewardservice.dto;

import java.util.List;

/**
 * Top-level DTO returned by the rewards API endpoint.
 * Contains full customer info, per-month breakdown and grand total.
 * Points are stored as {@code double} to preserve fractional values.
 */
public class RewardResponse {

    // Customer information
    private String customerId;
    private String customerName;
    private String email;

    // Period of calculation
    private String periodStart;
    private String periodEnd;
    private int monthsCalculated;

    // Reward breakdown
    private List<MonthlyRewardSummary> monthlyRewards;
    private double totalRewardPoints;

    // Transaction summary
    private int totalTransactions;
    private double totalAmountSpent;

    public RewardResponse() {}

    // --- Getters & Setters ---

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(String periodStart) {
        this.periodStart = periodStart;
    }

    public String getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(String periodEnd) {
        this.periodEnd = periodEnd;
    }

    public int getMonthsCalculated() {
        return monthsCalculated;
    }

    public void setMonthsCalculated(int monthsCalculated) {
        this.monthsCalculated = monthsCalculated;
    }

    public List<MonthlyRewardSummary> getMonthlyRewards() {
        return monthlyRewards;
    }

    public void setMonthlyRewards(List<MonthlyRewardSummary> monthlyRewards) {
        this.monthlyRewards = monthlyRewards;
    }

    public double getTotalRewardPoints() {
        return totalRewardPoints;
    }

    public void setTotalRewardPoints(double totalRewardPoints) {
        this.totalRewardPoints = totalRewardPoints;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(int totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public double getTotalAmountSpent() {
        return totalAmountSpent;
    }

    public void setTotalAmountSpent(double totalAmountSpent) {
        this.totalAmountSpent = totalAmountSpent;
    }
}
