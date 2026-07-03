package com.retailer.rewardservice.controller;

import com.retailer.rewardservice.dto.RewardResponse;
import com.retailer.rewardservice.service.RewardCalculatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller exposing the single reward-calculation endpoint.
 *
 * <pre>
 * GET /api/rewards
 *   ?customerId=C001        (optional – omit to retrieve all customers)
 *   &amp;startDate=2024-01-01  (optional – ISO date yyyy-MM-dd)
 *   &amp;endDate=2024-03-31    (optional – ISO date yyyy-MM-dd)
 *   &amp;months=3             (optional – integer 1–24, default 3)
 * </pre>
 *
 * <p>Date-range resolution (applied in order):
 * <ol>
 *   <li>{@code startDate} + {@code endDate}  → use directly.</li>
 *   <li>{@code startDate} + {@code months}   → endDate = startDate + months.</li>
 *   <li>{@code months} only                  → window ending today.</li>
 *   <li>{@code endDate} only                 → 3-month window ending on endDate.</li>
 *   <li>Nothing supplied                     → last 3 months ending today.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/rewards")
public class RewardController {

    private static final Logger logger = LoggerFactory.getLogger(RewardController.class);

    private final RewardCalculatorService rewardCalculatorService;

    public RewardController(RewardCalculatorService rewardCalculatorService) {
        this.rewardCalculatorService = rewardCalculatorService;
    }

    /**
     * Retrieves reward points for one customer (if {@code customerId} is supplied) or for
     * all customers.
     *
     * @param customerId optional customer identifier; when absent returns all customers
     * @param startDate  optional start of the calculation window (ISO-8601 date)
     * @param endDate    optional end of the calculation window (ISO-8601 date)
     * @param months     optional number of months (1–24); used when dates are absent
     * @return single {@link RewardResponse} or a list of them, depending on {@code customerId}
     */
    @GetMapping
    public ResponseEntity<?> getRewards(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer months) {

        logger.info("GET /api/rewards customerId={} startDate={} endDate={} months={}",
                customerId, startDate, endDate, months);

        if (customerId != null && !customerId.trim().isEmpty()) {
            RewardResponse response =
                    rewardCalculatorService.calculateRewards(customerId, startDate, endDate, months);
            return ResponseEntity.ok(response);
        }

        List<RewardResponse> responses =
                rewardCalculatorService.calculateRewardsForAllCustomers(startDate, endDate, months);
        return ResponseEntity.ok(responses);
    }
}
