package dev.usr.database.payload.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BorrowRequest {
    @NotNull
    private Long equipmentId;
    
    @NotNull
    @Min(1)
    private Integer quantity;
    
    private LocalDateTime expectedReturnDate;
    
    private String purpose;
    
    private String remarks;
}