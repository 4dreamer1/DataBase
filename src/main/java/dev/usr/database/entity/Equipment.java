package dev.usr.database.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Equipment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "装备名称不能为空")
    @Column(nullable = false)
    private String name;
    
    @Column(unique = true)
    private String serialNumber;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    @NotNull(message = "分类不能为空")
    @JsonIgnoreProperties("equipments")
    private Category category;
    
    @Min(value = 1, message = "数量必须大于0")
    @Column(nullable = false)
    private Integer quantity = 1;
    
    @Min(value = 0, message = "可用数量不能为负数")
    @Column(nullable = false)
    private Integer availableQuantity = 1;
    
    private String manufacturer;
    
    private String model;
    
    @Column(name = "purchase_date")
    private LocalDateTime purchaseDate;
    
    @Column(name = "purchase_price")
    private BigDecimal purchasePrice;
    
    private String location;
    
    @NotBlank(message = "状态不能为空")
    private String status = "可用"; // 可用, 维修中, 报废
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 