package dev.usr.database.repository;

import dev.usr.database.entity.Category;
import dev.usr.database.entity.Equipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long>, JpaSpecificationExecutor<Equipment> {
    /**
     * 根据分类查找装备
     */
    List<Equipment> findByCategory(Category category);
    
    /**
     * 根据分类ID查找装备
     */
    @Query("SELECT e FROM Equipment e WHERE e.category.id = :categoryId")
    List<Equipment> findByCategoryId(@Param("categoryId") Long categoryId);
    
    /**
     * 根据序列号查找装备
     */
    Optional<Equipment> findBySerialNumber(String serialNumber);
    
    /**
     * 检查序列号是否存在
     */
    boolean existsBySerialNumber(String serialNumber);
    
    /**
     * 根据名称查找装备（模糊查询，忽略大小写）
     */
    List<Equipment> findByNameContainingIgnoreCase(String name);
    /**
     * 获取所有可用装备（可用数量大于0且状态为"可用"）
     */
    @Query("SELECT e FROM Equipment e WHERE e.availableQuantity > 0 AND e.status = '可用'")
    List<Equipment> findAvailableEquipments();
    
    /**
     * 获取库存不足的装备（可用数量少于总数的20%）
     */
    @Query("SELECT e FROM Equipment e WHERE e.availableQuantity < e.quantity * 0.2")
    List<Equipment> findLowStockEquipments();
    
    /**
     * 统计可用装备数量
     */
    long countByAvailableQuantityGreaterThan(int quantity);
    
    /**
     * 统计特定状态的装备数量
     */
    long countByStatus(String status);
    
    /**
     * 多条件搜索装备
     */
    @Query("SELECT e FROM Equipment e WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.serialNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:categoryId IS NULL OR e.category.id = :categoryId) AND " +
           "(:status IS NULL OR :status = '' OR e.status = :status)")
    Page<Equipment> search(
        @Param("keyword") String keyword, 
        @Param("categoryId") Long categoryId, 
        @Param("status") String status, 
        Pageable pageable
    );
    
    /**
     * 根据状态查找装备
     */
    List<Equipment> findByStatus(String status);
    
    /**
     * 获取特定分类下的装备数量
     */
    @Query("SELECT COUNT(e) FROM Equipment e WHERE e.category.id = :categoryId")
    long countByCategoryId(@Param("categoryId") Long categoryId);
    
    /**
     * 统计可用装备数量（可用数量大于指定值且状态为指定值）
     */
    long countByAvailableQuantityGreaterThanAndStatus(int quantity, String status);
    
    /**
     * 计算所有装备的数量总和
     */
    @Query("SELECT COALESCE(SUM(e.quantity), 0) FROM Equipment e")
    long sumTotalQuantity();
    
    /**
     * 计算所有可用装备的可用数量总和
     */
    @Query("SELECT COALESCE(SUM(e.availableQuantity), 0) FROM Equipment e WHERE e.availableQuantity > 0")
    long sumAvailableQuantity();
    
    /**
     * 计算特定状态装备的数量总和
     */
    @Query("SELECT COALESCE(SUM(e.quantity), 0) FROM Equipment e WHERE e.status = :status")
    long sumQuantityByStatus(@Param("status") String status);
} 