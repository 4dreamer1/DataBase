package dev.usr.database.service;

import dev.usr.database.entity.Equipment;
import dev.usr.database.entity.Category;
import dev.usr.database.payload.request.EquipmentRequest;
import dev.usr.database.payload.response.EquipmentStatisticsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 装备管理服务接口
 */
public interface EquipmentService {
    /**
     * 获取所有装备
     */
    List<Equipment> findAll();
    
    /**
     * 分页获取所有装备
     */
    Page<Equipment> findAll(Pageable pageable);
    
    /**
     * 根据ID获取装备
     */
    Equipment findById(Long id);
    
    /**
     * 根据分类获取装备
     */
    List<Equipment> findByCategory(Long categoryId);
    
    /**
     * 根据关键词搜索装备
     */
    List<Equipment> search(String keyword);
    
    /**
     * 多条件搜索装备（分页）
     */
    Page<Equipment> search(String keyword, Long categoryId, String status, Pageable pageable);
    
    /**
     * 获取可用装备（可用数量大于0）
     */
    List<Equipment> findAvailable();
    
    /**
     * 获取库存不足的装备
     */
    List<Equipment> findLowStock();
    
    /**
     * 创建新装备
     */
    Equipment save(EquipmentRequest equipmentRequest);
    
    /**
     * 更新装备信息
     */
    Equipment update(Long id, EquipmentRequest equipmentRequest);
    
    /**
     * 更新装备状态
     */
    Equipment updateStatus(Long id, String status);
    
    /**
     * 删除装备
     */
    void delete(Long id);
    
    /**
     * 批量删除装备
     */
    void deleteAll(List<Long> ids);
    
    /**
     * 统计装备总数
     */
    long countAll();
    
    /**
     * 统计可用装备数量
     */
    long countAvailable();
    
    /**
     * 获取各分类的装备分布
     */
    List<Map<String, Object>> getCategoryDistribution();
    
    /**
     * 获取装备统计信息
     */
    EquipmentStatisticsResponse getStatistics();
    
    /**
     * 检查序列号是否已存在
     */
    boolean isSerialNumberExists(String serialNumber);
    
    /**
     * 检查序列号是否已存在（排除指定ID）
     */
    boolean isSerialNumberExists(String serialNumber, Long excludeId);
    
    /**
     * 导出装备数据
     * 
     * @param keyword 搜索关键词
     * @param categoryId 分类ID
     * @param status 状态
     * @param format 导出格式 (csv/excel)
     * @param outputStream 输出流
     * @throws IOException 如果导出过程中发生IO异常
     */
    void exportEquipment(String keyword, Long categoryId, String status, String format, OutputStream outputStream) throws IOException;
    
    /**
     * 生成装备导入模板
     * 
     * @param format 模板格式 (csv/excel)
     * @param outputStream 输出流
     * @throws IOException 如果生成过程中发生IO异常
     */
    void generateImportTemplate(String format, OutputStream outputStream) throws IOException;
    
    /**
     * 生成装备导入模板（包含分类列表）
     * 
     * @param categories 分类列表
     * @param isCsv 是否生成CSV格式（true为CSV，false为Excel）
     * @return 模板文件的字节数组
     * @throws IOException 如果生成过程中发生IO异常
     */
    byte[] generateImportTemplate(List<Category> categories, boolean isCsv) throws IOException;
    
    /**
     * 导入装备数据
     * 
     * @param file 上传的文件（CSV或Excel）
     * @param updateExisting 是否更新已存在的记录（根据序列号匹配）
     * @param skipErrors 是否跳过错误记录
     * @return 导入结果统计
     * @throws Exception 如果导入过程中发生异常
     */
    Map<String, Object> importEquipment(org.springframework.web.multipart.MultipartFile file, boolean updateExisting, boolean skipErrors) throws Exception;
} 