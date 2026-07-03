package com.retailer.rewardservice.repository;

import com.retailer.rewardservice.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Customer} entities.
 * Replaces the previous in-memory data store; H2 is used as the runtime database.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
}
