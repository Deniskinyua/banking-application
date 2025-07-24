package com.banking.backend.repository;

import com.banking.backend.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    // Best practice: Name query methods clearly. findByUserId is fine.
    Optional<Account> findByCustomerId(String customerId); // Changed from fromUserId for generality
    //findByUserId
}