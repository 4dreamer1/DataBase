package dev.usr.database.payload.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 装备请求数据模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentRequest {
    @NotBlank(message = "装备名称不能为空")
    private String name;
    
    private String serialNumber;
    
    private String description;
    
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;
    
    @Min(value = 1, message = "数量必须大于0")
    private Integer quantity = 1;
    
    @Min(value = 0, message = "可用数量不能为负数")
    private Integer availableQuantity;
    
    private String manufacturer;
    
    private String model;
    
    private LocalDateTime purchaseDate;
    
    private BigDecimal purchasePrice;
    
    private String location;
    
    @NotBlank(message = "状态不能为空")
    private String status = "可用";
} 