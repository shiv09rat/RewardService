package com.retailer.rewardservice.dto;

import java.util.List;

/**
 * DTO summarising reward points earned by a customer in a given calendar month.
 * Points are stored as {@code double} to preserve fractional values derived from
 * cent-level transaction amounts (e.g. $120.99 → 90.99 points).
 */
public class MonthlyRewardSummary {

    private String month;           // e.g. "JANUARY 2024"
    private double totalPoints;
    private int transactionCount;
    private double totalSpent;
    private List<TransactionDetail> transactions;

    public MonthlyRewardSummary() {}

    public MonthlyRewardSummary(String month, double totalPoints,
                                int transactionCount, double totalSpent,
                                List<TransactionDetail> transactions) {
        this.month = month;
        this.totalPoints = totalPoints;
        this.transactionCount = transactionCount;
        this.totalSpent = totalSpent;
        this.transactions = transactions;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public double getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(double totalPoints) {
        this.totalPoints = totalPoints;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public double getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(double totalSpent) {
        this.totalSpent = totalSpent;
    }

    public List<TransactionDetail> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionDetail> transactions) {
        this.transactions = transactions;
    }
}
