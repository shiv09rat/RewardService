package com.retailer.rewardservice.dto;

/**
 * DTO representing a single transaction alongside its computed reward points.
 * Points are stored as {@code double} to preserve fractional values from cent-level
 * amounts (e.g. $120.99 earns 90.99 points, not 90).
 */
public class TransactionDetail {

    private String transactionId;
    private String transactionDate;
    private double amount;
    private double pointsEarned;

    public TransactionDetail() {}

    public TransactionDetail(String transactionId, String transactionDate,
                             double amount, double pointsEarned) {
        this.transactionId = transactionId;
        this.transactionDate = transactionDate;
        this.amount = amount;
        this.pointsEarned = pointsEarned;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(double pointsEarned) {
        this.pointsEarned = pointsEarned;
    }
}
