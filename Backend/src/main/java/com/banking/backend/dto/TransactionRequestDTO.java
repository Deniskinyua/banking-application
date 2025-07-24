package com.banking.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;


public class TransactionRequestDTO {
    @NotBlank(message = "From user ID cannot be blank")
    private String fromUserId;

    @NotBlank(message = "To user ID cannot be blank")
    private String toUserId;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero") // Minimum transaction amount
    private BigDecimal amount;
    // Description can be optional, no @NotBlank or @NotNull needed unless required.
    private String description;

    public @NotBlank(message = "From user ID cannot be blank") String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(@NotBlank(message = "From user ID cannot be blank") String fromUserId) {
        this.fromUserId = fromUserId;
    }

    public @NotBlank(message = "To user ID cannot be blank") String getToUserId() {
        return toUserId;
    }

    public void setToUserId(@NotBlank(message = "To user ID cannot be blank") String toUserId) {
        this.toUserId = toUserId;
    }

    public @NotNull(message = "Amount cannot be null") @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(@NotNull(message = "Amount cannot be null") @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}