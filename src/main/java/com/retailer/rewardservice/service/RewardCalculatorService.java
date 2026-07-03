package com.retailer.rewardservice.service;

import com.retailer.rewardservice.dto.MonthlyRewardSummary;
import com.retailer.rewardservice.dto.RewardResponse;
import com.retailer.rewardservice.dto.TransactionDetail;
import com.retailer.rewardservice.exception.CustomerNotFoundException;
import com.retailer.rewardservice.exception.InvalidRequestException;
import com.retailer.rewardservice.model.Customer;
import com.retailer.rewardservice.model.Transaction;
import com.retailer.rewardservice.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Core service responsible for calculating reward points per customer.
 *
 * <p>Reward rules:
 * <ul>
 *   <li>2 points for every dollar spent OVER $100 in a single transaction</li>
 *   <li>1 point for every dollar spent between $50 and $100 (inclusive of neither boundary)</li>
 *   <li>No points for amounts at or below $50</li>
 * </ul>
 *
 * <p>Example: a $120 purchase = (2 × $20) + (1 × $50) = 90 points.
 *
 * <p>Date-range resolution rules (applied in order):
 * <ol>
 *   <li>If {@code startDate} and {@code endDate} are both supplied, use them directly.</li>
 *   <li>If {@code startDate} and {@code months} are supplied, compute
 *       {@code endDate = startDate + months}.</li>
 *   <li>If only {@code months} is supplied, compute a window ending today.</li>
 *   <li>If only {@code endDate} is supplied, default to 3 months ending on that date.</li>
 * </ol>
 */
@Service
public class RewardCalculatorService {

    private static final Logger logger = LoggerFactory.getLogger(RewardCalculatorService.class);

    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM yyyy");

    @Value("${reward.threshold.lower:50}")
    private double lowerThreshold;

    @Value("${reward.threshold.upper:100}")
    private double upperThreshold;

    @Value("${reward.points.lower:1}")
    private int lowerPoints;

    @Value("${reward.points.upper:2}")
    private int upperPoints;

    private final CustomerRepository customerRepository;

    public RewardCalculatorService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Calculates rewards for a specific customer using a resolved date range.
     *
     * @param customerId the unique customer identifier (may be {@code null} for all customers)
     * @param startDate  start of the period (inclusive); may be {@code null}
     * @param endDate    end of the period (inclusive); may be {@code null}
     * @param months     number of months to look back; used when startDate/endDate absent
     * @return fully populated {@link RewardResponse}
     * @throws CustomerNotFoundException if the customer does not exist
     * @throws InvalidRequestException   if the resolved date range is invalid
     */
    public RewardResponse calculateRewards(String customerId,
                                           LocalDate startDate,
                                           LocalDate endDate,
                                           Integer months) {

        LocalDate[] range = resolveDateRange(startDate, endDate, months);
        LocalDate resolvedStart = range[0];
        LocalDate resolvedEnd   = range[1];

        logger.info("Calculating rewards for customer '{}' from {} to {}",
                customerId, resolvedStart, resolvedEnd);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        return buildResponse(customer, resolvedStart, resolvedEnd, months);
    }

    /**
     * Returns reward summaries for ALL customers over the resolved date range.
     *
     * @param startDate start of the period (inclusive); may be {@code null}
     * @param endDate   end of the period (inclusive); may be {@code null}
     * @param months    number of months to look back; used when startDate/endDate absent
     * @return list of {@link RewardResponse} for every customer, sorted by total points desc
     * @throws InvalidRequestException if the resolved date range is invalid
     */
    public List<RewardResponse> calculateRewardsForAllCustomers(LocalDate startDate,
                                                                 LocalDate endDate,
                                                                 Integer months) {
        LocalDate[] range = resolveDateRange(startDate, endDate, months);
        LocalDate resolvedStart = range[0];
        LocalDate resolvedEnd   = range[1];

        logger.info("Calculating rewards for all customers from {} to {}",
                resolvedStart, resolvedEnd);

        return customerRepository.findAll().stream()
                .map(customer -> buildResponse(customer, resolvedStart, resolvedEnd, months))
                .sorted(Comparator.comparingDouble(RewardResponse::getTotalRewardPoints).reversed())
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Reward calculation logic
    // -------------------------------------------------------------------------

    /**
     * Computes reward points for a single transaction amount.
     *
     * <p>Uses {@link Math#round(double)} on each tier's dollar value so that
     * cents are counted rather than truncated.  For example:
     * <ul>
     *   <li>$50.99 → 1 point  (0.99 × 1, rounded = 1)</li>
     *   <li>$100.99 → 51.98 points  (0.99 × 2 = 1.98 above $100 band, plus 50 for $50–$100 band)</li>
     *   <li>$120.00 → 90 points  (unchanged whole-dollar example)</li>
     * </ul>
     *
     * @param amount purchase amount in dollars
     * @return reward points (never negative)
     */
    public double computePoints(double amount) {
        if (amount <= lowerThreshold) {
            return 0;
        }

        double points = 0;

        if (amount > upperThreshold) {
            // Points for the portion above $100 (rounded to nearest whole point)
            points += Math.round((amount - upperThreshold) * upperPoints * 100.0) / 100.0;
            // Points for the $50–$100 band (always a whole number when thresholds are integers)
            points += Math.round((upperThreshold - lowerThreshold) * lowerPoints * 100.0) / 100.0;
        } else {
            // Points only for the portion above $50 (rounded to nearest whole point)
            points += Math.round((amount - lowerThreshold) * lowerPoints * 100.0) / 100.0;
        }

        return points;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Builds the full {@link RewardResponse} for one customer over the given date range.
     */
    private RewardResponse buildResponse(Customer customer,
                                         LocalDate resolvedStart,
                                         LocalDate resolvedEnd,
                                         Integer months) {

        List<Transaction> periodTransactions = customer.getTransactions().stream()
                .filter(tx -> !tx.getTransactionDate().isBefore(resolvedStart)
                           && !tx.getTransactionDate().isAfter(resolvedEnd))
                .sorted(Comparator.comparing(Transaction::getTransactionDate))
                .collect(Collectors.toList());

        logger.debug("Found {} transaction(s) for customer '{}' between {} and {}",
                periodTransactions.size(), customer.getCustomerId(), resolvedStart, resolvedEnd);

        // Group transactions by calendar month (TreeMap keeps months sorted)
        Map<YearMonth, List<Transaction>> byMonth = periodTransactions.stream()
                .collect(Collectors.groupingBy(
                        tx -> YearMonth.from(tx.getTransactionDate()),
                        TreeMap::new,
                        Collectors.toList()
                ));

        List<MonthlyRewardSummary> monthlySummaries = new ArrayList<>();
        double grandTotal = 0;
        double totalSpent = 0.0;

        for (Map.Entry<YearMonth, List<Transaction>> entry : byMonth.entrySet()) {
            YearMonth ym = entry.getKey();
            List<Transaction> monthTx = entry.getValue();

            List<TransactionDetail> details = new ArrayList<>();
            double monthPoints = 0;
            double monthSpent = 0.0;

            for (Transaction tx : monthTx) {
                double points = computePoints(tx.getAmount());
                monthPoints += points;
                monthSpent += tx.getAmount();

                details.add(new TransactionDetail(
                        tx.getTransactionId(),
                        tx.getTransactionDate().toString(),
                        tx.getAmount(),
                        points
                ));
            }

            monthlySummaries.add(new MonthlyRewardSummary(
                    ym.format(MONTH_FORMATTER),
                    Math.round(monthPoints * 100.0) / 100.0,
                    monthTx.size(),
                    Math.round(monthSpent * 100.0) / 100.0,
                    details
            ));

            grandTotal += monthPoints;
            totalSpent += monthSpent;
        }

        // Derive monthsCalculated: if caller passed months use it, otherwise compute from range
        int monthsCalculated = (months != null)
                ? months
                : (int) java.time.temporal.ChronoUnit.MONTHS.between(
                        YearMonth.from(resolvedStart), YearMonth.from(resolvedEnd)) + 1;

        RewardResponse response = new RewardResponse();
        response.setCustomerId(customer.getCustomerId());
        response.setCustomerName(customer.getFullName());
        response.setEmail(customer.getEmail());
        response.setPeriodStart(resolvedStart.toString());
        response.setPeriodEnd(resolvedEnd.toString());
        response.setMonthsCalculated(monthsCalculated);
        response.setMonthlyRewards(monthlySummaries);
        response.setTotalRewardPoints(Math.round(grandTotal * 100.0) / 100.0);
        response.setTotalTransactions(periodTransactions.size());
        response.setTotalAmountSpent(Math.round(totalSpent * 100.0) / 100.0);

        logger.info("Customer '{}' earned {} total points from {} to {}",
                customer.getCustomerId(), grandTotal, resolvedStart, resolvedEnd);

        return response;
    }

    /**
     * Resolves the effective {@code [startDate, endDate]} window from the combination of
     * the three optional parameters.  At least one parameter must be non-null.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>{@code startDate} + {@code endDate}  → use as-is.</li>
     *   <li>{@code startDate} + {@code months}   → endDate = startDate + months (exclusive day).</li>
     *   <li>{@code months} only                  → endDate = today, startDate = today − months.</li>
     *   <li>{@code endDate} only                 → startDate = endDate − 3 months (default window).</li>
     * </ol>
     *
     * @return two-element array {@code [startDate, endDate]}
     * @throws InvalidRequestException if the resulting range is invalid
     */
    private LocalDate[] resolveDateRange(LocalDate startDate, LocalDate endDate, Integer months) {

        LocalDate resolvedStart;
        LocalDate resolvedEnd;

        if (startDate != null && endDate != null) {
            resolvedStart = startDate;
            resolvedEnd   = endDate;

        } else if (startDate != null && months != null) {
            validateMonths(months);
            resolvedStart = startDate;
            resolvedEnd   = startDate.plusMonths(months).minusDays(1);

        } else if (months != null) {
            validateMonths(months);
            resolvedEnd   = LocalDate.now();
            resolvedStart = resolvedEnd.minusMonths(months).withDayOfMonth(1);

        } else if (endDate != null) {
            // Default 3-month window ending on the supplied date
            resolvedEnd   = endDate;
            resolvedStart = endDate.minusMonths(3).withDayOfMonth(1);

        } else {
            // Nothing supplied — default to the last 3 months
            resolvedEnd   = LocalDate.now();
            resolvedStart = resolvedEnd.minusMonths(3).withDayOfMonth(1);
        }

        if (resolvedStart.isAfter(resolvedEnd)) {
            throw new InvalidRequestException(
                    "startDate (" + resolvedStart + ") must not be after endDate (" + resolvedEnd + ").");
        }

        return new LocalDate[]{resolvedStart, resolvedEnd};
    }

    /**
     * Validates that {@code months} is within the accepted range of 1–24.
     */
    private void validateMonths(int months) {
        if (months < 1 || months > 24) {
            throw new InvalidRequestException(
                    "Parameter 'months' must be between 1 and 24, but was: " + months);
        }
    }
}
