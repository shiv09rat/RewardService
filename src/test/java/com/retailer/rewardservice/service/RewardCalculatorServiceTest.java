package com.retailer.rewardservice.service;

import com.retailer.rewardservice.dto.MonthlyRewardSummary;
import com.retailer.rewardservice.dto.RewardResponse;
import com.retailer.rewardservice.exception.CustomerNotFoundException;
import com.retailer.rewardservice.exception.InvalidRequestException;
import com.retailer.rewardservice.model.Customer;
import com.retailer.rewardservice.model.Transaction;
import com.retailer.rewardservice.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RewardCalculatorService}.
 *
 * <p>Covers:
 * <ul>
 *   <li>Point calculation edge cases (including cent-level amounts)</li>
 *   <li>Monthly and total aggregation with exact assertions</li>
 *   <li>Date-range resolution (startDate/endDate/months combinations)</li>
 *   <li>Exception handling for unknown customers and invalid parameters</li>
 *   <li>Transactions outside the requested time window are excluded</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class RewardCalculatorServiceTest {

    private static final DateTimeFormatter MONTH_FMT =
            DateTimeFormatter.ofPattern("MMMM yyyy");

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private RewardCalculatorService rewardCalculatorService;

    @BeforeEach
    void setUp() {
        // Inject configurable threshold values matching application.properties defaults
        ReflectionTestUtils.setField(rewardCalculatorService, "lowerThreshold", 50.0);
        ReflectionTestUtils.setField(rewardCalculatorService, "upperThreshold", 100.0);
        ReflectionTestUtils.setField(rewardCalculatorService, "lowerPoints",     1);
        ReflectionTestUtils.setField(rewardCalculatorService, "upperPoints",     2);
    }

    // =========================================================================
    // computePoints() – boundary and representative values
    // =========================================================================

    @Test
    @DisplayName("Amount below lower threshold ($30) earns 0 points")
    void computePoints_belowLowerThreshold_returnsZero() {
        assertEquals(0, rewardCalculatorService.computePoints(30.00));
    }

    @Test
    @DisplayName("Amount exactly at lower threshold ($50) earns 0 points")
    void computePoints_exactlyAtLowerThreshold_returnsZero() {
        assertEquals(0, rewardCalculatorService.computePoints(50.00));
    }

    @Test
    @DisplayName("$75 earns 25 points (1 pt per dollar above $50)")
    void computePoints_betweenThresholds_returnsCorrectPoints() {
        // $75 → (75 − 50) × 1 = 25
        assertEquals(25, rewardCalculatorService.computePoints(75.00));
    }

    @Test
    @DisplayName("$100 earns 50 points (upper boundary of lower band)")
    void computePoints_exactlyAtUpperThreshold_returnsCorrectPoints() {
        // $100 → (100 − 50) × 1 = 50
        assertEquals(50, rewardCalculatorService.computePoints(100.00));
    }

    @Test
    @DisplayName("$120 earns 90 points: (20×2) + (50×1) = 90 (requirement example)")
    void computePoints_120dollars_matchesRequirementExample() {
        // $120 → (20 × 2) + (50 × 1) = 40 + 50 = 90
        assertEquals(90, rewardCalculatorService.computePoints(120.00));
    }

    @Test
    @DisplayName("$500 earns 850 points: (400×2) + (50×1)")
    void computePoints_largePurchase_returnsCorrectPoints() {
        // $500 → (400 × 2) + (50 × 1) = 800 + 50 = 850
        assertEquals(850, rewardCalculatorService.computePoints(500.00));
    }

    @Test
    @DisplayName("$0 earns 0 points")
    void computePoints_zeroDollars_returnsZero() {
        assertEquals(0, rewardCalculatorService.computePoints(0.00));
    }

    @Test
    @DisplayName("$50.99 earns 0.99 points – cents above lower threshold are counted, not truncated")
    void computePoints_centsAboveLowerThreshold_countsCorrectly() {
        // $50.99 → round(0.99 * 1 * 100) / 100 = round(99) / 100 = 0.99  (was 0 with (int) cast)
        assertEquals(0.99, rewardCalculatorService.computePoints(50.99));
    }

    @Test
    @DisplayName("$100.99 earns 51.98 points – cents above upper threshold are counted, not truncated")
    void computePoints_centsAboveUpperThreshold_countsCorrectly() {
        // upper portion: round(0.99 * 2 * 100) / 100 = round(198) / 100 = 1.98
        // lower band:    round(50 * 1 * 100)   / 100 = 50.0
        // total = 51.98
        assertEquals(51.98, rewardCalculatorService.computePoints(100.99));
    }

    // =========================================================================
    // calculateRewards() – happy-path with exact assertions
    // =========================================================================

    @Test
    @DisplayName("Single $120 transaction 10 days ago earns exactly 90 points")
    void calculateRewards_singleTransaction_exactPoints() {
        String customerId = "C001";
        LocalDate txDate = LocalDate.now().minusDays(10);
        Customer customer = new Customer(customerId, "Alice", "Johnson",
                "alice@example.com",
                Collections.singletonList(
                        new Transaction("T1", customerId, 120.00, txDate)));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        RewardResponse response = rewardCalculatorService.calculateRewards(
                customerId, null, null, 3);

        assertNotNull(response);
        assertEquals(customerId, response.getCustomerId());
        assertEquals("Alice Johnson", response.getCustomerName());
        assertEquals(90.0, response.getTotalRewardPoints());
        assertEquals(1, response.getTotalTransactions());
        assertEquals(120.00, response.getTotalAmountSpent());

        // Monthly breakdown
        assertEquals(1, response.getMonthlyRewards().size());
        MonthlyRewardSummary summary = response.getMonthlyRewards().get(0);
        assertEquals(90.0, summary.getTotalPoints());
        assertEquals(1, summary.getTransactionCount());
        assertEquals(120.00, summary.getTotalSpent());
        assertEquals(YearMonth.from(txDate).format(MONTH_FMT), summary.getMonth());

        // Transaction detail
        assertEquals(1, summary.getTransactions().size());
        assertEquals("T1", summary.getTransactions().get(0).getTransactionId());
        assertEquals(90.0, summary.getTransactions().get(0).getPointsEarned());
    }

    @Test
    @DisplayName("Two transactions in different months accumulate correct per-month and total points")
    void calculateRewards_twoMonths_correctPerMonthAndTotal() {
        String customerId = "C001";
        // Pin to specific dates guaranteed to be in different calendar months
        // and within the last 3 months from today (July 3, 2026)
        LocalDate month1Tx = LocalDate.now().minusMonths(2).withDayOfMonth(15); // ~May 15
        LocalDate month2Tx = LocalDate.now().minusMonths(1).withDayOfMonth(15); // ~Jun 15

        // $120 in month2 → 90 pts; $75 in month1 → 25 pts; total 115
        Customer customer = new Customer(customerId, "Alice", "Johnson",
                "alice@example.com",
                Arrays.asList(
                        new Transaction("T1", customerId, 120.00, month2Tx),
                        new Transaction("T2", customerId,  75.00, month1Tx)));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        RewardResponse response = rewardCalculatorService.calculateRewards(
                customerId, null, null, 3);

        assertEquals(115.0, response.getTotalRewardPoints());
        assertEquals(2, response.getTotalTransactions());

        // Two monthly summaries sorted ascending by YearMonth
        assertEquals(2, response.getMonthlyRewards().size());

        MonthlyRewardSummary older = response.getMonthlyRewards().get(0);
        MonthlyRewardSummary newer = response.getMonthlyRewards().get(1);

        assertEquals(25.0,  older.getTotalPoints());
        assertEquals(75.00, older.getTotalSpent());

        assertEquals(90.0,   newer.getTotalPoints());
        assertEquals(120.00, newer.getTotalSpent());
    }

    @Test
    @DisplayName("Single month window with one $120 transaction returns 90 points")
    void calculateRewards_oneMonthWindow_exactPoints() {
        String customerId = "C004";
        LocalDate txDate = LocalDate.now().minusDays(5);
        Customer customer = new Customer(customerId, "David", "Lee",
                "david@example.com",
                Collections.singletonList(
                        new Transaction("R01", customerId, 120.00, txDate)));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        RewardResponse response = rewardCalculatorService.calculateRewards(
                customerId, null, null, 1);

        assertEquals(1, response.getMonthsCalculated());
        assertEquals(90.0, response.getTotalRewardPoints());
    }

    @Test
    @DisplayName("Explicit startDate + endDate window is respected and returns correct points")
    void calculateRewards_explicitDateRange_exactPoints() {
        String customerId = "C001";
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end   = LocalDate.of(2024, 1, 31);
        LocalDate txDate = LocalDate.of(2024, 1, 15);

        Customer customer = new Customer(customerId, "Alice", "Johnson",
                "alice@example.com",
                Arrays.asList(
                        new Transaction("T1", customerId, 120.00, txDate),
                        // outside window – should be excluded
                        new Transaction("T2", customerId, 200.00,
                                LocalDate.of(2024, 3, 1))));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        RewardResponse response = rewardCalculatorService.calculateRewards(
                customerId, start, end, null);

        assertEquals("2024-01-01", response.getPeriodStart());
        assertEquals("2024-01-31", response.getPeriodEnd());
        assertEquals(1, response.getTotalTransactions());
        assertEquals(90.0, response.getTotalRewardPoints());
    }

    @Test
    @DisplayName("startDate + months computes correct endDate and returns expected points")
    void calculateRewards_startDateAndMonths_exactPoints() {
        String customerId = "C001";
        LocalDate start = LocalDate.of(2024, 1, 1);
        // months=2 → endDate = 2024-02-29 (inclusive)
        LocalDate txJan = LocalDate.of(2024, 1, 15);
        LocalDate txFeb = LocalDate.of(2024, 2, 10);
        LocalDate txMar = LocalDate.of(2024, 3, 5); // outside – excluded

        Customer customer = new Customer(customerId, "Alice", "Johnson",
                "alice@example.com",
                Arrays.asList(
                        new Transaction("T1", customerId, 120.00, txJan),
                        new Transaction("T2", customerId, 120.00, txFeb),
                        new Transaction("T3", customerId, 120.00, txMar)));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        RewardResponse response = rewardCalculatorService.calculateRewards(
                customerId, start, null, 2);

        // Only T1 and T2 fall within Jan 1 → Feb 29
        assertEquals(2, response.getTotalTransactions());
        assertEquals(180.0, response.getTotalRewardPoints()); // 2 × 90
    }

    // =========================================================================
    // calculateRewards() – error cases
    // =========================================================================

    @Test
    @DisplayName("Unknown customer ID throws CustomerNotFoundException")
    void calculateRewards_unknownCustomer_throwsCustomerNotFoundException() {
        when(customerRepository.findById("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class,
                () -> rewardCalculatorService.calculateRewards("UNKNOWN", null, null, 3));

        verify(customerRepository).findById("UNKNOWN");
    }

    @Test
    @DisplayName("months=0 throws InvalidRequestException")
    void calculateRewards_zeroMonths_throwsInvalidRequestException() {
        assertThrows(InvalidRequestException.class,
                () -> rewardCalculatorService.calculateRewards("C001", null, null, 0));
    }

    @Test
    @DisplayName("months=25 throws InvalidRequestException")
    void calculateRewards_monthsTooLarge_throwsInvalidRequestException() {
        assertThrows(InvalidRequestException.class,
                () -> rewardCalculatorService.calculateRewards("C001", null, null, 25));
    }

    @Test
    @DisplayName("months=-1 throws InvalidRequestException")
    void calculateRewards_negativeMonths_throwsInvalidRequestException() {
        assertThrows(InvalidRequestException.class,
                () -> rewardCalculatorService.calculateRewards("C001", null, null, -1));
    }

    @Test
    @DisplayName("startDate after endDate throws InvalidRequestException")
    void calculateRewards_startAfterEnd_throwsInvalidRequestException() {
        assertThrows(InvalidRequestException.class,
                () -> rewardCalculatorService.calculateRewards(
                        "C001",
                        LocalDate.of(2024, 6, 1),
                        LocalDate.of(2024, 1, 1),
                        null));
    }

    // =========================================================================
    // calculateRewards() – edge cases
    // =========================================================================

    @Test
    @DisplayName("Transactions outside the requested period are excluded")
    void calculateRewards_oldTransactionsExcluded() {
        String customerId = "C002";
        Transaction oldTx = new Transaction("OLD", customerId, 200.00,
                LocalDate.now().minusYears(10));
        Customer customer = new Customer(customerId, "Bob", "Martinez",
                "bob@example.com", Collections.singletonList(oldTx));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        RewardResponse response = rewardCalculatorService.calculateRewards(
                customerId, null, null, 3);

        assertEquals(0.0, response.getTotalRewardPoints());
        assertEquals(0, response.getTotalTransactions());
        assertTrue(response.getMonthlyRewards().isEmpty());
    }

    @Test
    @DisplayName("Customer with no transactions returns zero points and empty monthly list")
    void calculateRewards_noTransactions_returnsZeroPoints() {
        String customerId = "C003";
        Customer customer = new Customer(customerId, "Carol", "Smith",
                "carol@example.com", Collections.emptyList());
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        RewardResponse response = rewardCalculatorService.calculateRewards(
                customerId, null, null, 3);

        assertEquals(0.0, response.getTotalRewardPoints());
        assertTrue(response.getMonthlyRewards().isEmpty());
    }

    // =========================================================================
    // calculateRewardsForAllCustomers()
    // =========================================================================

    @Test
    @DisplayName("calculateRewardsForAllCustomers returns one entry per customer with correct totals")
    void calculateRewardsForAllCustomers_returnsCorrectListWithExactPoints() {
        LocalDate txDate = LocalDate.now().minusDays(10);

        // C001: one $120 transaction → 90 pts
        Customer c1 = new Customer("C001", "Alice", "Johnson", "alice@example.com",
                Collections.singletonList(
                        new Transaction("TX1", "C001", 120.00, txDate)));

        // C002: one $75 transaction → 25 pts
        Customer c2 = new Customer("C002", "Bob", "Martinez", "bob@example.com",
                Collections.singletonList(
                        new Transaction("TX2", "C002", 75.00, txDate)));

        when(customerRepository.findAll()).thenReturn(Arrays.asList(c1, c2));

        List<RewardResponse> results =
                rewardCalculatorService.calculateRewardsForAllCustomers(null, null, 3);

        assertEquals(2, results.size());

        // Results sorted descending by totalRewardPoints
        assertEquals("C001", results.get(0).getCustomerId());
        assertEquals(90.0,   results.get(0).getTotalRewardPoints());

        assertEquals("C002", results.get(1).getCustomerId());
        assertEquals(25.0,   results.get(1).getTotalRewardPoints());
    }
}
