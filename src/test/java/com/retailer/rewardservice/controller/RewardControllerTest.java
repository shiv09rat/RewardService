package com.retailer.rewardservice.controller;

import com.retailer.rewardservice.dto.MonthlyRewardSummary;
import com.retailer.rewardservice.dto.RewardResponse;
import com.retailer.rewardservice.exception.CustomerNotFoundException;
import com.retailer.rewardservice.exception.GlobalExceptionHandler;
import com.retailer.rewardservice.service.RewardCalculatorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice tests for {@link RewardController}.
 *
 * <p>Uses MockMvc to verify HTTP response codes, content type, and JSON structure.
 * All service stubs use exact parameter values – no wildcard matchers – so that
 * incorrect routing (wrong customerId, wrong date, wrong months) causes a test failure
 * rather than silently passing.
 */
@WebMvcTest(RewardController.class)
@Import(GlobalExceptionHandler.class)
class RewardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RewardCalculatorService rewardCalculatorService;

    // =========================================================================
    // GET /api/rewards?customerId=…
    // =========================================================================

    @Test
    @DisplayName("GET /api/rewards?customerId=C001 returns 200 with correct reward data")
    void getRewards_withCustomerId_returns200() throws Exception {
        RewardResponse response = buildSampleResponse("C001", "Alice Johnson", 90.0);
        when(rewardCalculatorService.calculateRewards("C001", null, null, null))
                .thenReturn(response);

        mockMvc.perform(get("/api/rewards")
                        .param("customerId", "C001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.customerId").value("C001"))
                .andExpect(jsonPath("$.customerName").value("Alice Johnson"))
                .andExpect(jsonPath("$.totalRewardPoints").value(90.0))
                .andExpect(jsonPath("$.monthsCalculated").value(3));
    }

    @Test
    @DisplayName("GET /api/rewards?customerId=C001&months=3 passes exact months to service")
    void getRewards_withCustomerIdAndMonths_passesExactMonths() throws Exception {
        RewardResponse response = buildSampleResponse("C001", "Alice Johnson", 90.0);
        when(rewardCalculatorService.calculateRewards("C001", null, null, 3))
                .thenReturn(response);

        mockMvc.perform(get("/api/rewards")
                        .param("customerId", "C001")
                        .param("months", "3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("C001"))
                .andExpect(jsonPath("$.totalRewardPoints").value(90.0));
    }

    @Test
    @DisplayName("GET /api/rewards?customerId=C001&startDate=…&endDate=… passes exact dates to service")
    void getRewards_withCustomerIdAndDateRange_passesExactDates() throws Exception {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end   = LocalDate.of(2024, 3, 31);
        RewardResponse response = buildSampleResponse("C001", "Alice Johnson", 150.0);
        when(rewardCalculatorService.calculateRewards("C001", start, end, null))
                .thenReturn(response);

        mockMvc.perform(get("/api/rewards")
                        .param("customerId", "C001")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-03-31")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("C001"))
                .andExpect(jsonPath("$.totalRewardPoints").value(150.0));
    }

    @Test
    @DisplayName("GET /api/rewards?customerId=C001&startDate=…&months=3 passes exact start+months to service")
    void getRewards_withCustomerIdAndStartDateAndMonths_passesExactValues() throws Exception {
        LocalDate start = LocalDate.of(2024, 1, 1);
        RewardResponse response = buildSampleResponse("C001", "Alice Johnson", 120.0);
        when(rewardCalculatorService.calculateRewards("C001", start, null, 3))
                .thenReturn(response);

        mockMvc.perform(get("/api/rewards")
                        .param("customerId", "C001")
                        .param("startDate", "2024-01-01")
                        .param("months", "3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("C001"))
                .andExpect(jsonPath("$.totalRewardPoints").value(120.0));
    }

    @Test
    @DisplayName("GET /api/rewards?customerId=UNKNOWN returns 404")
    void getRewards_unknownCustomerId_returns404() throws Exception {
        when(rewardCalculatorService.calculateRewards("UNKNOWN", null, null, null))
                .thenThrow(new CustomerNotFoundException("UNKNOWN"));

        mockMvc.perform(get("/api/rewards")
                        .param("customerId", "UNKNOWN")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("GET /api/rewards?customerId=C001&months=abc returns 400 for non-integer months")
    void getRewards_invalidMonthsType_returns400() throws Exception {
        mockMvc.perform(get("/api/rewards")
                        .param("customerId", "C001")
                        .param("months", "abc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // GET /api/rewards  (all customers)
    // =========================================================================

    @Test
    @DisplayName("GET /api/rewards with no customerId returns list for all customers")
    void getRewards_noCustomerId_returnsAllCustomers() throws Exception {
        RewardResponse r1 = buildSampleResponse("C001", "Alice Johnson", 250.0);
        RewardResponse r2 = buildSampleResponse("C002", "Bob Martinez",  180.0);
        List<RewardResponse> all = Arrays.asList(r1, r2);
        when(rewardCalculatorService.calculateRewardsForAllCustomers(null, null, null))
                .thenReturn(all);

        mockMvc.perform(get("/api/rewards")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].customerId").value("C001"))
                .andExpect(jsonPath("$[1].customerId").value("C002"));
    }

    @Test
    @DisplayName("GET /api/rewards?months=6 passes exact months=6 to service for all customers")
    void getRewards_allCustomers_sixMonths_passesExactMonths() throws Exception {
        RewardResponse r1 = buildSampleResponse("C001", "Alice Johnson", 500.0);
        when(rewardCalculatorService.calculateRewardsForAllCustomers(null, null, 6))
                .thenReturn(Collections.singletonList(r1));

        mockMvc.perform(get("/api/rewards")
                        .param("months", "6")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalRewardPoints").value(500.0));
    }

    @Test
    @DisplayName("GET /api/rewards?startDate=…&endDate=… passes exact dates to service for all customers")
    void getRewards_allCustomers_withDateRange_passesExactDates() throws Exception {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end   = LocalDate.of(2024, 3, 31);
        RewardResponse r1 = buildSampleResponse("C001", "Alice Johnson", 300.0);
        when(rewardCalculatorService.calculateRewardsForAllCustomers(start, end, null))
                .thenReturn(Collections.singletonList(r1));

        mockMvc.perform(get("/api/rewards")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-03-31")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalRewardPoints").value(300.0));
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private RewardResponse buildSampleResponse(String id, String name, double points) {
        RewardResponse r = new RewardResponse();
        r.setCustomerId(id);
        r.setCustomerName(name);
        r.setEmail("test@example.com");
        r.setPeriodStart("2024-01-01");
        r.setPeriodEnd("2024-03-31");
        r.setMonthsCalculated(3);
        r.setTotalRewardPoints(points);
        r.setTotalTransactions(3);
        r.setTotalAmountSpent(395.00);
        r.setMonthlyRewards(Collections.singletonList(
                new MonthlyRewardSummary("JANUARY 2024", points, 3, 395.00,
                        Collections.emptyList())
        ));
        return r;
    }
}
