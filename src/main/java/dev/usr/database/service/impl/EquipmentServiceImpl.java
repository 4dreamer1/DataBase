package dev.usr.database.service.impl;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import dev.usr.database.entity.Category;
import dev.usr.database.entity.Equipment;
import dev.usr.database.exception.ResourceNotFoundException;
import dev.usr.database.payload.request.EquipmentRequest;
import dev.usr.database.payload.response.EquipmentStatisticsResponse;
import dev.usr.database.repository.CategoryRepository;
import dev.usr.database.repository.EquipmentRepository;
import dev.usr.database.service.EquipmentService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 装备管理服务实现类
 */
@Slf4j
@Service
public class EquipmentServiceImpl implements EquipmentService {
    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private CategoryRepository categoryRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Equipment> findAll() {
        return equipmentRepository.findAll();
    }
    
    @Override
    public Page<Equipment> findAll(Pageable pageable) {
        return equipmentRepository.findAll(pageable);
    }

    @Override
    public Equipment findById(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("装备", "ID", id));
    }

    @Override
    public List<Equipment> findByCategory(Long categoryId) {
        // 使用直接查询方法，避免额外查询category实体
        return equipmentRepository.findByCategoryId(categoryId);
    }

    @Override
    public List<Equipment> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return equipmentRepository.findAll();
        }
        return equipmentRepository.findByNameContainingIgnoreCase(keyword.trim());
    }
    
    @Override
    public Page<Equipment> search(String keyword, Long categoryId, String status, Pageable pageable) {
        return equipmentRepository.search(keyword, categoryId, status, pageable);
    }

    @Override
    public List<Equipment> findAvailable() {
        log.info("获取可用装备列表");
        List<Equipment> availableEquipments = equipmentRepository.findAvailableEquipments();
        log.info("找到{}件可用装备", availableEquipments.size());
        
        // 记录日志，便于排查问题
        for (Equipment equipment : availableEquipments) {
            log.debug("可用装备: ID={}, 名称={}, 状态={}, 可用数量={}", 
                equipment.getId(), equipment.getName(), 
                equipment.getStatus(), equipment.getAvailableQuantity());
        }
        
        return availableEquipments;
    }

    @Override
    public List<Equipment> findLowStock() {
        return equipmentRepository.findLowStockEquipments();
    }

    @Override
    @Transactional
    public Equipment save(EquipmentRequest request) {
        // 检查序列号是否存在
        if (request.getSerialNumber() != null && !request.getSerialNumber().trim().isEmpty() 
            && equipmentRepository.existsBySerialNumber(request.getSerialNumber().trim())) {
            throw new IllegalArgumentException("序列号已存在: " + request.getSerialNumber());
        }
        
        // 查找分类
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("分类", "ID", request.getCategoryId()));
        
        // 创建装备实体
        Equipment equipment = new Equipment();
        equipment.setName(request.getName().trim());
        equipment.setSerialNumber(request.getSerialNumber() != null ? request.getSerialNumber().trim() : null);
        equipment.setDescription(request.getDescription());
        equipment.setCategory(category);
        equipment.setQuantity(request.getQuantity());
        
        // 如果没有设置可用数量，默认等于总数量
        equipment.setAvailableQuantity(request.getAvailableQuantity() != null 
                ? request.getAvailableQuantity() 
                : request.getQuantity());
        
        equipment.setManufacturer(request.getManufacturer());
        equipment.setModel(request.getModel());
        equipment.setPurchaseDate(request.getPurchaseDate());
        equipment.setPurchasePrice(request.getPurchasePrice());
        equipment.setLocation(request.getLocation());
        equipment.setStatus(request.getStatus());
        
        log.info("创建新装备: {}, 分类: {}", equipment.getName(), category.getName());
        Equipment savedEquipment = equipmentRepository.save(equipment);
        
        // 确保缓存同步
        entityManager.flush();
        
        return savedEquipment;
    }

    @Override
    @Transactional
    public Equipment update(Long id, EquipmentRequest request) {
        // 获取现有装备
        Equipment existingEquipment = findById(id);
        
        // 检查序列号是否存在（排除当前装备）
        if (request.getSerialNumber() != null && !request.getSerialNumber().trim().isEmpty() 
            && !request.getSerialNumber().equals(existingEquipment.getSerialNumber())
            && equipmentRepository.existsBySerialNumber(request.getSerialNumber().trim())) {
            throw new IllegalArgumentException("序列号已存在: " + request.getSerialNumber());
        }
        
        // 查找分类
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("分类", "ID", request.getCategoryId()));
        
        // 更新基本信息
        existingEquipment.setName(request.getName().trim());
        existingEquipment.setSerialNumber(request.getSerialNumber() != null ? request.getSerialNumber().trim() : null);
        existingEquipment.setDescription(request.getDescription());
        existingEquipment.setCategory(category);
        existingEquipment.setManufacturer(request.getManufacturer());
        existingEquipment.setModel(request.getModel());
        existingEquipment.setPurchaseDate(request.getPurchaseDate());
        existingEquipment.setPurchasePrice(request.getPurchasePrice());
        existingEquipment.setLocation(request.getLocation());
        existingEquipment.setStatus(request.getStatus());
        
        // 如果数量发生变化，需要更新可用数量
        if (!existingEquipment.getQuantity().equals(request.getQuantity())) {
            int diff = request.getQuantity() - existingEquipment.getQuantity();
            existingEquipment.setQuantity(request.getQuantity());
            
            // 如果请求中明确指定了可用数量，则使用请求中的值，否则根据差值更新
            if (request.getAvailableQuantity() != null) {
                existingEquipment.setAvailableQuantity(request.getAvailableQuantity());
            } else {
                // 增加可用数量，但不能小于0
                int newAvailable = Math.max(0, existingEquipment.getAvailableQuantity() + diff);
                existingEquipment.setAvailableQuantity(newAvailable);
            }
        } else if (request.getAvailableQuantity() != null) {
            // 如果总数量没变但明确指定了可用数量
            existingEquipment.setAvailableQuantity(request.getAvailableQuantity());
        }
        
        log.info("更新装备 ID {}: {}, 分类: {}", id, existingEquipment.getName(), category.getName());
        Equipment updatedEquipment = equipmentRepository.save(existingEquipment);
        
        // 确保缓存同步
        entityManager.flush();
        
        return updatedEquipment;
    }
    
    @Override
    @Transactional
    public Equipment updateStatus(Long id, String status) {
        Equipment equipment = findById(id);
        equipment.setStatus(status);
        return equipmentRepository.save(equipment);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Equipment equipment = findById(id);
        log.info("删除装备 ID {}: {}", id, equipment.getName());
        equipmentRepository.delete(equipment);
    }
    
    @Override
    @Transactional
    public void deleteAll(List<Long> ids) {
        log.info("批量删除装备, IDs: {}", ids);
        List<Equipment> equipmentsToDelete = new ArrayList<>();
        for (Long id : ids) {
            try {
                Equipment equipment = findById(id);
                equipmentsToDelete.add(equipment);
            } catch (ResourceNotFoundException e) {
                // 记录错误但继续处理其他ID
                log.warn("删除装备时未找到ID: {}", id);
            }
        }
        if (!equipmentsToDelete.isEmpty()) {
            equipmentRepository.deleteAll(equipmentsToDelete);
        }
    }
    
    @Override
    public long countAll() {
        // 使用新的方法计算总数量和而不是行数
        return equipmentRepository.sumTotalQuantity();
    }
    
    @Override
    public long countAvailable() {
        // 使用新的方法计算可用数量和而不是行数
        return equipmentRepository.sumAvailableQuantity();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getCategoryDistribution() {
        // 使用JPQL查询，按分类统计装备
        Query query = entityManager.createQuery(
            "SELECT c.id as categoryId, c.name as categoryName, " +
            "COUNT(e) as equipmentCount, SUM(e.quantity) as totalQuantity " +
            "FROM Equipment e JOIN e.category c " +
            "GROUP BY c.id, c.name " +
            "ORDER BY COUNT(e) DESC"
        );
        
        List<Object[]> results = query.getResultList();
        List<Map<String, Object>> distribution = new ArrayList<>();
        
        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("categoryId", row[0]);
            item.put("categoryName", row[1]);
            item.put("equipmentCount", row[2]);
            item.put("totalQuantity", row[3]);
            distribution.add(item);
        }
        
        return distribution;
    }
    
    @Override
    public EquipmentStatisticsResponse getStatistics() {
        EquipmentStatisticsResponse stats = new EquipmentStatisticsResponse();
        
        // 装备总数
        stats.setTotalCount(countAll());
        
        // 可用装备数
        stats.setAvailableCount(countAvailable());
        
        // 维修中装备数
        stats.setRepairingCount(equipmentRepository.sumQuantityByStatus("维修中"));
        
        // 报废装备数
        stats.setScrapCount(equipmentRepository.sumQuantityByStatus("报废"));
        
        // 分类分布
        stats.setCategoryDistribution(getCategoryDistribution());
        
        // 状态分布
        List<Map<String, Object>> statusStats = new ArrayList<>();
        Map<String, Object> availableStatus = new HashMap<>();
        availableStatus.put("status", "可用");
        availableStatus.put("count", equipmentRepository.sumQuantityByStatus("可用"));
        statusStats.add(availableStatus);
        
        Map<String, Object> repairingStatus = new HashMap<>();
        repairingStatus.put("status", "维修中");
        repairingStatus.put("count", equipmentRepository.sumQuantityByStatus("维修中"));
        statusStats.add(repairingStatus);
        
        Map<String, Object> scrapStatus = new HashMap<>();
        scrapStatus.put("status", "报废");
        scrapStatus.put("count", equipmentRepository.sumQuantityByStatus("报废"));
        statusStats.add(scrapStatus);
        
        stats.setStatusDistribution(statusStats);
        
        return stats;
    }
    
    @Override
    public boolean isSerialNumberExists(String serialNumber) {
        if (serialNumber == null || serialNumber.trim().isEmpty()) {
            return false;
        }
        return equipmentRepository.existsBySerialNumber(serialNumber.trim());
    }
    
    @Override
    public boolean isSerialNumberExists(String serialNumber, Long excludeId) {
        if (serialNumber == null || serialNumber.trim().isEmpty()) {
            return false;
        }
        
        // 查找具有该序列号的装备
        return equipmentRepository.findBySerialNumber(serialNumber.trim())
                .map(e -> !e.getId().equals(excludeId)) // 如果找到了，检查ID是否不等于excludeId
                .orElse(false); // 如果没找到，则返回false
    }
    
    @Override
    public void exportEquipment(String keyword, Long categoryId, String status, String format, OutputStream outputStream) throws IOException {
        // 获取数据
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "id"));
        Page<Equipment> equipmentPage = search(keyword, categoryId, status, pageable);
        List<Equipment> equipments = equipmentPage.getContent();
        
        // 定义表头
        String[] headers = {
            "ID", "名称", "序列号", "分类", "数量", "可用数量", "状态", 
            "品牌", "型号", "购买日期", "购买价格", "位置", "描述"
        };
        
        // 根据格式导出
        if ("excel".equalsIgnoreCase(format)) {
            exportToExcel(equipments, headers, outputStream);
        } else {
            exportToCsv(equipments, headers, outputStream);
        }
    }
    
    @Override
    public void generateImportTemplate(String format, OutputStream outputStream) throws IOException {
        // 定义模板表头
        String[] headers = {
            "name", "serialNumber", "categoryId", "quantity", "availableQuantity", "status",
            "manufacturer", "model", "purchaseDate", "purchasePrice", "location", "description"
        };
        
        // 创建示例数据
        List<String[]> exampleData = new ArrayList<>();
        exampleData.add(new String[] {
            "示例装备", "SN123456", "1", "10", "8", "可用",
            "示例品牌", "X-100", "2023-01-01", "2000.00", "A区-01架", "这是一个装备导入模板示例"
        });
        
        // 获取所有分类，用于备注
        List<Category> categories = categoryRepository.findAll();
        
        // 根据格式生成模板
        if ("excel".equalsIgnoreCase(format)) {
            generateExcelTemplate(headers, exampleData, categories, outputStream);
        } else {
            generateCsvTemplate(headers, exampleData, outputStream);
        }
    }
    
    /**
     * 生成装备导入模板（包含分类列表）
     * 
     * @param categories 分类列表
     * @param isCsv 是否生成CSV格式（true为CSV，false为Excel）
     * @return 模板文件的字节数组
     * @throws IOException 如果生成过程中发生IO异常
     */
    @Override
    public byte[] generateImportTemplate(List<Category> categories, boolean isCsv) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        String[] headers = {"name", "serialNumber", "categoryId", "quantity", "availableQuantity", "status",
                          "manufacturer", "model", "purchaseDate", "purchasePrice", "location", "description"};
        
        // 创建示例数据
        List<String[]> exampleData = new ArrayList<>();
        // 不再需要这里的示例数据，因为我们在各自的方法中创建
        
        if (isCsv) {
            generateCsvTemplate(headers, exampleData, outputStream);
        } else {
            generateExcelTemplate(headers, exampleData, categories, outputStream);
        }
        
        return outputStream.toByteArray();
    }
    
    @Override
    @Transactional
    public Map<String, Object> importEquipment(org.springframework.web.multipart.MultipartFile file, boolean updateExisting, boolean skipErrors) throws Exception {
        log.info("开始导入装备数据，文件名: {}", file.getOriginalFilename());
        
        // 结果统计
        Map<String, Object> result = new HashMap<>();
        result.put("imported", 0);
        result.put("updated", 0);
        List<String> errors = new ArrayList<>();
        result.put("errors", errors);
        
        try {
            // 根据文件类型读取数据
            List<Map<String, String>> dataList;
            String filename = file.getOriginalFilename();
            
            if (filename != null && filename.endsWith(".xlsx")) {
                dataList = readExcelData(file);
            } else {
                dataList = readCsvData(file);
            }
            
            log.info("读取到 {} 条记录准备导入", dataList.size());
            
            int imported = 0;
            int updated = 0;
            int rowNum = 1; // 从1开始，表示Excel或CSV的行号（有表头）
            
            for (Map<String, String> data : dataList) {
                rowNum++;
                try {
                    // 验证必填字段
                    validateRequiredFields(data, rowNum);
                    
                    // 构建装备请求对象
                    EquipmentRequest request = createEquipmentRequest(data);
                    
                    // 判断是更新还是新增
                    String serialNumber = data.get("serialNumber");
                    if (serialNumber != null && !serialNumber.trim().isEmpty()) {
                        Equipment existingEquipment = equipmentRepository.findBySerialNumber(serialNumber.trim())
                                .orElse(null);
                        
                        if (existingEquipment != null) {
                            if (updateExisting) {
                                // 更新已有记录
                                update(existingEquipment.getId(), request);
                                updated++;
                            } else {
                                // 记录错误
                                errors.add(String.format("第%d行: 序列号 '%s' 已存在", rowNum, serialNumber));
                                if (!skipErrors) {
                                    throw new IllegalArgumentException("序列号已存在: " + serialNumber);
                                }
                                continue;
                            }
                        } else {
                            // 新增记录
                            save(request);
                            imported++;
                        }
                    } else {
                        // 新增记录
                        save(request);
                        imported++;
                    }
                } catch (Exception e) {
                    String errorMsg = String.format("第%d行导入失败: %s", rowNum, e.getMessage());
                    log.error(errorMsg, e);
                    errors.add(errorMsg);
                    
                    if (!skipErrors) {
                        throw new Exception("导入中止，行 " + rowNum + ": " + e.getMessage());
                    }
                }
            }
            
            result.put("imported", imported);
            result.put("updated", updated);
            
            log.info("导入完成: 新增 {} 条，更新 {} 条，错误 {} 条", imported, updated, errors.size());
        } catch (IOException | CsvValidationException e) {
            log.error("解析导入文件失败", e);
            throw new Exception("解析导入文件失败: " + e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * 导出为CSV格式
     */
    private void exportToCsv(List<Equipment> equipments, String[] headers, OutputStream outputStream) throws IOException {
        // 自定义CSV写入配置
        CSVWriter writer = new CSVWriter(
            new OutputStreamWriter(outputStream, StandardCharsets.UTF_8),
            ',',                 // 分隔符
            '"',                 // 引号字符
            '"',                 // 转义字符
            "\r\n"               // 行结束符
        );
        
        try {
            // 写入BOM标记，确保Excel正确识别UTF-8编码
            outputStream.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
            
            // 写入标题行
            writer.writeNext(headers);
            
            // 写入数据行
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            for (Equipment equipment : equipments) {
                String[] data = new String[] {
                    String.valueOf(equipment.getId()),
                    equipment.getName() != null ? equipment.getName() : "",
                    equipment.getSerialNumber() != null ? equipment.getSerialNumber() : "",
                    equipment.getCategory() != null ? equipment.getCategory().getName() : "未分类",
                    equipment.getDescription() != null ? equipment.getDescription() : "",
                    String.valueOf(equipment.getQuantity()),
                    String.valueOf(equipment.getAvailableQuantity()),
                    equipment.getStatus() != null ? equipment.getStatus() : "",
                    equipment.getManufacturer() != null ? equipment.getManufacturer() : "",
                    equipment.getModel() != null ? equipment.getModel() : "",
                    equipment.getPurchaseDate() != null ? dateFormat.format(equipment.getPurchaseDate()) : "",
                    equipment.getPurchasePrice() != null ? String.format("%.2f", equipment.getPurchasePrice()) : "",
                    equipment.getLocation() != null ? equipment.getLocation() : ""
                };
                writer.writeNext(data);
            }
            
            // 确保数据写入
            writer.flush();
        } finally {
            writer.close();
        }
    }
    
    /**
     * 导出为Excel格式
     */
    private void exportToExcel(List<Equipment> equipments, String[] headers, OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            // 创建工作表
            Sheet sheet = workbook.createSheet("装备列表");
            
            // 设置列宽
            sheet.setColumnWidth(0, 2000);  // ID
            sheet.setColumnWidth(1, 6000);  // 名称
            sheet.setColumnWidth(2, 5000);  // 序列号
            sheet.setColumnWidth(3, 4000);  // 分类
            sheet.setColumnWidth(4, 10000); // 描述
            sheet.setColumnWidth(5, 2500);  // 总数量
            sheet.setColumnWidth(6, 2500);  // 可用数量
            sheet.setColumnWidth(7, 3000);  // 状态
            sheet.setColumnWidth(8, 5000);  // 制造商
            sheet.setColumnWidth(9, 4000);  // 型号
            sheet.setColumnWidth(10, 4000); // 购买日期
            sheet.setColumnWidth(11, 4000); // 购买价格
            sheet.setColumnWidth(12, 6000); // 存放位置
            
            // 创建标题行样式
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            
            // 创建标题行
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(20); // 设置行高
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 创建数据行通用样式
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            
            // 创建不同类型的数据样式
            // 文本样式 - 左对齐
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.cloneStyleFrom(dataStyle);
            textStyle.setAlignment(HorizontalAlignment.LEFT);
            textStyle.setWrapText(true); // 允许文本换行
            
            // 数字样式 - 右对齐
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(dataStyle);
            numberStyle.setAlignment(HorizontalAlignment.RIGHT);
            
            // 居中样式 - 用于ID、状态等
            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.cloneStyleFrom(dataStyle);
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            
            // 日期格式
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.cloneStyleFrom(dataStyle);
            dateStyle.setAlignment(HorizontalAlignment.CENTER);
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));
            
            // 货币格式
            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.cloneStyleFrom(numberStyle);
            currencyStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));
            
            // 状态样式 - 可用、不可用、维修中
            CellStyle availableStyle = workbook.createCellStyle();
            availableStyle.cloneStyleFrom(centerStyle);
            Font availableFont = workbook.createFont();
            availableFont.setColor(IndexedColors.DARK_GREEN.getIndex());
            availableStyle.setFont(availableFont);
            
            CellStyle unavailableStyle = workbook.createCellStyle();
            unavailableStyle.cloneStyleFrom(centerStyle);
            Font unavailableFont = workbook.createFont();
            unavailableFont.setColor(IndexedColors.RED.getIndex());
            unavailableStyle.setFont(unavailableFont);
            
            CellStyle maintenanceStyle = workbook.createCellStyle();
            maintenanceStyle.cloneStyleFrom(centerStyle);
            Font maintenanceFont = workbook.createFont();
            maintenanceFont.setColor(IndexedColors.ORANGE.getIndex());
            maintenanceStyle.setFont(maintenanceFont);
            
            // 写入数据行
            int rowNum = 1;
            for (Equipment equipment : equipments) {
                Row row = sheet.createRow(rowNum++);
                row.setHeightInPoints(16); // 设置行高
                
                // ID
                Cell idCell = row.createCell(0);
                idCell.setCellValue(equipment.getId());
                idCell.setCellStyle(centerStyle);
                
                // 名称
                Cell nameCell = row.createCell(1);
                nameCell.setCellValue(equipment.getName());
                nameCell.setCellStyle(textStyle);
                
                // 序列号
                Cell serialCell = row.createCell(2);
                serialCell.setCellValue(equipment.getSerialNumber() != null ? equipment.getSerialNumber() : "");
                serialCell.setCellStyle(centerStyle);
                
                // 分类
                Cell categoryCell = row.createCell(3);
                categoryCell.setCellValue(equipment.getCategory() != null ? equipment.getCategory().getName() : "未分类");
                categoryCell.setCellStyle(centerStyle);
                
                // 描述
                Cell descCell = row.createCell(4);
                descCell.setCellValue(equipment.getDescription() != null ? equipment.getDescription() : "");
                descCell.setCellStyle(textStyle);
                
                // 总数量
                Cell quantityCell = row.createCell(5);
                quantityCell.setCellValue(equipment.getQuantity());
                quantityCell.setCellStyle(numberStyle);
                
                // 可用数量
                Cell availableCell = row.createCell(6);
                availableCell.setCellValue(equipment.getAvailableQuantity());
                availableCell.setCellStyle(numberStyle);
                
                // 状态
                Cell statusCell = row.createCell(7);
                String status = equipment.getStatus() != null ? equipment.getStatus() : "";
                statusCell.setCellValue(status);
                
                // 根据状态设置不同样式
                if ("可用".equals(status)) {
                    statusCell.setCellStyle(availableStyle);
                } else if ("不可用".equals(status)) {
                    statusCell.setCellStyle(unavailableStyle);
                } else if ("维修中".equals(status)) {
                    statusCell.setCellStyle(maintenanceStyle);
                } else {
                    statusCell.setCellStyle(centerStyle);
                }
                
                // 制造商
                Cell manufacturerCell = row.createCell(8);
                manufacturerCell.setCellValue(equipment.getManufacturer() != null ? equipment.getManufacturer() : "");
                manufacturerCell.setCellStyle(textStyle);
                
                // 型号
                Cell modelCell = row.createCell(9);
                modelCell.setCellValue(equipment.getModel() != null ? equipment.getModel() : "");
                modelCell.setCellStyle(textStyle);
                
                // 日期单独处理
                Cell dateCell = row.createCell(10);
                if (equipment.getPurchaseDate() != null) {
                    dateCell.setCellValue(equipment.getPurchaseDate());
                    dateCell.setCellStyle(dateStyle);
                } else {
                    dateCell.setCellValue("");
                    dateCell.setCellStyle(centerStyle);
                }
                
                // 价格
                Cell priceCell = row.createCell(11);
                if (equipment.getPurchasePrice() != null) {
                    priceCell.setCellValue(equipment.getPurchasePrice().doubleValue());
                    priceCell.setCellStyle(currencyStyle);
                } else {
                    priceCell.setCellValue("");
                    priceCell.setCellStyle(numberStyle);
                }
                
                // 位置
                Cell locationCell = row.createCell(12);
                locationCell.setCellValue(equipment.getLocation() != null ? equipment.getLocation() : "");
                locationCell.setCellStyle(textStyle);
            }
            
            // 创建一个冻结窗格，冻结第一行
            sheet.createFreezePane(0, 1);
            
            // 写入输出流
            workbook.write(outputStream);
        }
    }
    
    /**
     * 生成CSV模板
     */
    private void generateCsvTemplate(String[] headers, List<String[]> exampleData, OutputStream outputStream) throws IOException {
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            // 创建中文列标题
            String[] chineseHeaders = {
                "名称", "序列号", "分类", "数量", "可用数量", "状态",
                "品牌", "型号", "购买日期", "购买价格", "位置", "描述"
            };
            
            // 写入中文表头
            writer.writeNext(chineseHeaders);
            
            // 获取所有分类，用于示例数据
            List<Category> categories = categoryRepository.findAll();
            Category exampleCategory = categories.isEmpty() ? null : categories.get(0);
            
            // 写入示例数据
            String categoryInfo = exampleCategory != null ? 
                exampleCategory.getName() : "请填入有效分类";
            
            writer.writeNext(new String[] {
                "示例装备", "SN123456", categoryInfo, "10", "8", "可用",
                "示例品牌", "X-100", "2023-01-01", "2000.00", "A区-01架", "这是一个导入示例"
            });
            
            // 写入说明
            writer.writeNext(new String[] {"# 以下是系统中已有的分类列表，请使用这些分类名称或ID进行导入："});
            
            for (Category category : categories) {
                writer.writeNext(new String[] {
                    String.format("# ID: %d - 分类名称: %s - 描述: %s", 
                        category.getId(), category.getName(), 
                        category.getDescription() != null ? category.getDescription() : "")
                });
            }
            
            writer.writeNext(new String[] {"# ----- 导入说明 -----"});
            writer.writeNext(new String[] {"# 名称: 装备名称（必填）"});
            writer.writeNext(new String[] {"# 序列号: 序列号（必填，唯一标识）"});
            writer.writeNext(new String[] {"# 分类: 分类名称或ID（必填，必须使用系统中已有的分类）"});
            writer.writeNext(new String[] {"# 数量: 总数量（必填，正整数）"});
            writer.writeNext(new String[] {"# 可用数量: 可用数量（必填，不大于总数量）"});
            writer.writeNext(new String[] {"# 状态: 状态（必填，可选值：可用、维修中、不可用）"});
            writer.writeNext(new String[] {"# 品牌、型号、购买日期、购买价格、位置、描述: 这些字段为选填项"});
            writer.writeNext(new String[] {"# 购买日期格式: yyyy-MM-dd，例如2023-01-01"});
        }
    }
    
    /**
     * 生成Excel模板
     */
    private void generateExcelTemplate(String[] headers, List<String[]> exampleData, List<Category> categories, OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            // 创建数据工作表
            Sheet sheet = workbook.createSheet("装备导入模板");
            
            // 创建样式
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            CellStyle noteStyle = workbook.createCellStyle();
            noteStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            noteStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // 写入中文表头
            String[] chineseHeaders = {
                "名称", "序列号", "分类", "数量", "可用数量", "状态",
                "品牌", "型号", "购买日期", "购买价格", "位置", "描述"
            };
            
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < chineseHeaders.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(chineseHeaders[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }
            
            // 获取示例分类
            Category exampleCategory = categories.isEmpty() ? null : categories.get(0);
            
            // 写入示例数据
            Row exampleRow = sheet.createRow(1);
            String categoryInfo = exampleCategory != null ? 
                exampleCategory.getName() : "请填入有效分类";
            
            exampleRow.createCell(0).setCellValue("示例装备");
            exampleRow.createCell(1).setCellValue("SN123456");
            exampleRow.createCell(2).setCellValue(categoryInfo); // 使用分类名称而不是ID
            exampleRow.createCell(3).setCellValue(10);
            exampleRow.createCell(4).setCellValue(8);
            exampleRow.createCell(5).setCellValue("可用");
            exampleRow.createCell(6).setCellValue("示例品牌");
            exampleRow.createCell(7).setCellValue("X-100");
            exampleRow.createCell(8).setCellValue("2023-01-01");
            exampleRow.createCell(9).setCellValue(2000.00);
            exampleRow.createCell(10).setCellValue("A区-01架");
            exampleRow.createCell(11).setCellValue("这是一个导入示例");
            
            // 添加说明
            int rowNum = 3;
            Row noteRow = sheet.createRow(rowNum++);
            Cell noteCell = noteRow.createCell(0);
            noteCell.setCellValue("导入说明：");
            noteCell.setCellStyle(noteStyle);
            
            sheet.createRow(rowNum++).createCell(0).setCellValue("1. 名称: 装备名称（必填）");
            sheet.createRow(rowNum++).createCell(0).setCellValue("2. 序列号: 序列号（必填，唯一标识）");
            sheet.createRow(rowNum++).createCell(0).setCellValue("3. 分类: 分类名称或ID（必填，必须使用系统中已有的分类）");
            sheet.createRow(rowNum++).createCell(0).setCellValue("4. 数量: 总数量（必填，正整数）");
            sheet.createRow(rowNum++).createCell(0).setCellValue("5. 可用数量: 可用数量（必填，不大于总数量）");
            sheet.createRow(rowNum++).createCell(0).setCellValue("6. 状态: 状态（必填，可选值：可用、维修中、不可用）");
            sheet.createRow(rowNum++).createCell(0).setCellValue("7. 品牌、型号、购买日期、购买价格、位置、描述: 这些字段为选填项");
            sheet.createRow(rowNum++).createCell(0).setCellValue("8. 购买日期格式: yyyy-MM-dd，例如2023-01-01");
            
            // 添加分类参考
            rowNum += 2;
            Row catRow = sheet.createRow(rowNum++);
            Cell catCell = catRow.createCell(0);
            catCell.setCellValue("系统中的分类列表（请使用这些分类名称或ID）：");
            catCell.setCellStyle(noteStyle);
            
            for (Category category : categories) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(String.format("ID: %d", category.getId()));
                row.createCell(1).setCellValue(category.getName());
                if (category.getDescription() != null) {
                    row.createCell(2).setCellValue(category.getDescription());
                }
            }
            
            // 创建分类工作表
            Sheet categorySheet = workbook.createSheet("分类列表");
            
            // 写入分类表头
            Row catHeaderRow = categorySheet.createRow(0);
            String[] catHeaders = {"ID", "分类名称", "描述"};
            for (int i = 0; i < catHeaders.length; i++) {
                Cell cell = catHeaderRow.createCell(i);
                cell.setCellValue(catHeaders[i]);
                cell.setCellStyle(headerStyle);
                categorySheet.setColumnWidth(i, 5000);
            }
            
            // 写入所有分类数据
            int catRowNum = 1;
            for (Category category : categories) {
                Row row = categorySheet.createRow(catRowNum++);
                row.createCell(0).setCellValue(category.getId());
                row.createCell(1).setCellValue(category.getName());
                if (category.getDescription() != null) {
                    row.createCell(2).setCellValue(category.getDescription());
                }
            }
            
            // 创建说明工作表
            Sheet infoSheet = workbook.createSheet("使用说明");
            rowNum = 0;
            Row infoTitleRow = infoSheet.createRow(rowNum++);
            Cell infoTitleCell = infoTitleRow.createCell(0);
            infoTitleCell.setCellValue("装备导入模板使用说明");
            
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setBold(true);
            titleStyle.setFont(titleFont);
            infoTitleCell.setCellStyle(titleStyle);
            
            rowNum++;
            infoSheet.createRow(rowNum++).createCell(0).setCellValue("1. 必填字段: 名称, 序列号, 分类, 数量, 可用数量, 状态");
            infoSheet.createRow(rowNum++).createCell(0).setCellValue("2. 序列号是唯一标识，如果系统中已存在相同序列号，将根据导入选项决定是更新还是报错");
            infoSheet.createRow(rowNum++).createCell(0).setCellValue("3. 分类必须是系统中已存在的分类名称或ID，请参考\"分类列表\"工作表");
            infoSheet.createRow(rowNum++).createCell(0).setCellValue("4. 状态必须是以下值之一: 可用、维修中、不可用");
            infoSheet.createRow(rowNum++).createCell(0).setCellValue("5. 日期格式: yyyy-MM-dd，例如2023-01-01");
            infoSheet.createRow(rowNum++).createCell(0).setCellValue("6. 导入时，系统会尝试从\"分类\"列识别分类，支持使用分类名称或ID");
            
            // 保存工作簿
            workbook.write(outputStream);
        }
    }
    
    /**
     * 从Excel文件导入数据
     */
    private List<Map<String, String>> readExcelData(org.springframework.web.multipart.MultipartFile file) throws IOException {
        List<Map<String, String>> dataList = new ArrayList<>();
        
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0); // 获取第一个工作表
            
            // 获取标题行
            Row headerRow = sheet.getRow(0);
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
                Cell cell = headerRow.getCell(i);
                headers.add(cell != null ? cell.getStringCellValue() : "");
            }
            
            // 读取数据行
            int numOfRows = sheet.getPhysicalNumberOfRows();
            for (int i = 1; i < numOfRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                // 跳过空行或者注释行（以#开头的行）
                Cell firstCell = row.getCell(0);
                if (firstCell == null || 
                    firstCell.getCellType() == CellType.BLANK ||
                    (firstCell.getCellType() == CellType.STRING && 
                     firstCell.getStringCellValue().trim().startsWith("#"))) {
                    continue;
                }
                
                Map<String, String> rowData = new HashMap<>();
                
                // 添加英文键和中文键的映射
                Map<String, String> columnMapping = new HashMap<>();
                columnMapping.put("名称", "name");
                columnMapping.put("序列号", "serialNumber");
                columnMapping.put("分类", "categoryId");
                columnMapping.put("数量", "quantity");
                columnMapping.put("可用数量", "availableQuantity");
                columnMapping.put("状态", "status");
                columnMapping.put("品牌", "manufacturer");
                columnMapping.put("型号", "model");
                columnMapping.put("购买日期", "purchaseDate");
                columnMapping.put("购买价格", "purchasePrice");
                columnMapping.put("位置", "location");
                columnMapping.put("描述", "description");
                
                for (int j = 0; j < headers.size() && j < row.getPhysicalNumberOfCells(); j++) {
                    Cell cell = row.getCell(j);
                    String header = headers.get(j);
                    String value = "";
                    
                    if (cell != null) {
                        // 根据单元格类型获取值
                        switch (cell.getCellType()) {
                            case STRING:
                                value = cell.getStringCellValue();
                                break;
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    value = new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
                                } else {
                                    // 确保数字不会被格式化为科学计数法
                                    value = String.valueOf(Double.valueOf(cell.getNumericCellValue()).longValue());
                                }
                                break;
                            case BOOLEAN:
                                value = String.valueOf(cell.getBooleanCellValue());
                                break;
                            default:
                                value = "";
                        }
                    }
                    
                    // 使用英文键和中文键
                    rowData.put(header, value);
                    // 如果是中文标题，添加对应的英文键值
                    if (columnMapping.containsKey(header)) {
                        rowData.put(columnMapping.get(header), value);
                    }
                }
                
                dataList.add(rowData);
            }
        }
        
        return dataList;
    }
    
    /**
     * 从CSV文件读取数据
     */
    private List<Map<String, String>> readCsvData(org.springframework.web.multipart.MultipartFile file) throws IOException, CsvValidationException {
        List<Map<String, String>> dataList = new ArrayList<>();
        
        // 使用OpenCSV读取文件
        try (CSVReader reader = new CSVReader(new java.io.InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String[] headers = reader.readNext();
            if (headers == null) {
                throw new IllegalArgumentException("CSV文件格式错误：缺少表头行");
            }
            
            // 验证必要的列
            for (int i = 0; i < 6; i++) {
                if (i >= headers.length || headers[i] == null || headers[i].isEmpty()) {
                    throw new IllegalArgumentException("CSV文件格式错误：缺少必要的列（前6列）");
                }
            }
            
            String[] line;
            while ((line = reader.readNext()) != null) {
                // 跳过空行或注释行
                if (line.length == 0 || (line.length > 0 && line[0].startsWith("#"))) {
                    continue;
                }
                
                Map<String, String> rowData = new HashMap<>();
                for (int i = 0; i < headers.length && i < line.length; i++) {
                    if (headers[i] != null && !headers[i].isEmpty() && !headers[i].startsWith("#")) {
                        rowData.put(headers[i], line[i]);
                    }
                }
                
                dataList.add(rowData);
            }
        }
        
        return dataList;
    }
    
    /**
     * 验证必填字段
     */
    private void validateRequiredFields(Map<String, String> data, int rowNum) {
        // 检查必填字段
        String name = data.get("name");
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("装备名称不能为空");
        }
        
        String serialNumber = data.get("serialNumber");
        if (serialNumber == null || serialNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("序列号不能为空");
        }
        
        // 检查分类 - 支持通过ID或名称查找
        String categoryIdStr = data.get("categoryId");
        String categoryName = data.get("分类"); // 支持中文列名
        
        if ((categoryIdStr == null || categoryIdStr.trim().isEmpty()) && (categoryName == null || categoryName.trim().isEmpty())) {
            throw new IllegalArgumentException("分类不能为空 (需要提供分类ID或分类名称)");
        }
        
        // 验证分类存在
        if (categoryIdStr != null && !categoryIdStr.trim().isEmpty()) {
            try {
                Long categoryId = Long.parseLong(categoryIdStr.trim());
                if (!categoryRepository.existsById(categoryId)) {
                    throw new IllegalArgumentException("分类ID不存在: " + categoryId);
                }
            } catch (NumberFormatException e) {
                // 如果不是数字，尝试按名称查找
                if (categoryRepository.findByName(categoryIdStr.trim()).isEmpty()) {
                    throw new IllegalArgumentException("分类名称不存在: " + categoryIdStr);
                }
            }
        } else if (categoryName != null && !categoryName.trim().isEmpty()) {
            if (categoryRepository.findByName(categoryName.trim()).isEmpty()) {
                throw new IllegalArgumentException("分类名称不存在: " + categoryName);
            }
        }
        
        // 验证数量
        String quantityStr = data.get("quantity");
        if (quantityStr == null || quantityStr.trim().isEmpty()) {
            // 尝试获取中文列名
            quantityStr = data.get("数量");
            if (quantityStr == null || quantityStr.trim().isEmpty()) {
                throw new IllegalArgumentException("总数量不能为空");
            }
        }
        try {
            int quantity = Integer.parseInt(quantityStr.trim());
            if (quantity <= 0) {
                throw new IllegalArgumentException("总数量必须大于0");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("总数量必须是整数: " + quantityStr);
        }
        
        // 验证可用数量
        String availableStr = data.get("availableQuantity");
        if (availableStr == null || availableStr.trim().isEmpty()) {
            // 尝试获取中文列名
            availableStr = data.get("可用数量");
            if (availableStr == null || availableStr.trim().isEmpty()) {
                throw new IllegalArgumentException("可用数量不能为空");
            }
        }
        try {
            int available = Integer.parseInt(availableStr.trim());
            int quantity = Integer.parseInt(quantityStr.trim());
            if (available < 0) {
                throw new IllegalArgumentException("可用数量不能小于0");
            }
            if (available > quantity) {
                throw new IllegalArgumentException("可用数量不能大于总数量");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("可用数量必须是整数: " + availableStr);
        }
        
        // 验证状态
        String status = data.get("status");
        if (status == null || status.trim().isEmpty()) {
            // 尝试获取中文列名
            status = data.get("状态");
            if (status == null || status.trim().isEmpty()) {
                throw new IllegalArgumentException("状态不能为空");
            }
        }
        status = status.trim();
        if (!status.equals("可用") && !status.equals("维修中") && !status.equals("不可用")) {
            throw new IllegalArgumentException("状态值无效，必须是：可用、维修中、不可用");
        }
    }
    
    /**
     * 创建装备请求对象
     */
    private EquipmentRequest createEquipmentRequest(Map<String, String> data) {
        EquipmentRequest request = new EquipmentRequest();
        
        // 装备名称 - 支持中文列名
        String name = data.get("name");
        if (name == null || name.trim().isEmpty()) {
            name = data.get("名称");
        }
        request.setName(name.trim());
        
        // 序列号 - 支持中文列名
        String serialNumber = data.get("serialNumber");
        if (serialNumber == null || serialNumber.trim().isEmpty()) {
            serialNumber = data.get("序列号");
        }
        request.setSerialNumber(serialNumber.trim());
        
        // 处理分类ID - 支持通过名称或ID查找
        Long categoryId = null;
        String categoryIdStr = data.get("categoryId");
        String categoryName = data.get("分类");
        
        if (categoryIdStr != null && !categoryIdStr.trim().isEmpty()) {
            try {
                categoryId = Long.parseLong(categoryIdStr.trim());
                // 确认ID存在
                if (!categoryRepository.existsById(categoryId)) {
                    // 如果ID不存在，尝试按名称查找
                    Optional<Category> category = categoryRepository.findByName(categoryIdStr.trim());
                    if (category.isPresent()) {
                        categoryId = category.get().getId();
                    } else {
                        throw new IllegalArgumentException("分类ID不存在: " + categoryIdStr);
                    }
                }
            } catch (NumberFormatException e) {
                // 如果不是数字，尝试按名称查找
                Optional<Category> category = categoryRepository.findByName(categoryIdStr.trim());
                if (category.isPresent()) {
                    categoryId = category.get().getId();
                } else {
                    throw new IllegalArgumentException("分类名称不存在: " + categoryIdStr);
                }
            }
        } else if (categoryName != null && !categoryName.trim().isEmpty()) {
            // 通过名称查找分类
            Optional<Category> category = categoryRepository.findByName(categoryName.trim());
            if (category.isPresent()) {
                categoryId = category.get().getId();
            } else {
                throw new IllegalArgumentException("分类名称不存在: " + categoryName);
            }
        } else {
            throw new IllegalArgumentException("分类不能为空");
        }
        
        request.setCategoryId(categoryId);
        
        // 数量 - 支持中文列名
        String quantityStr = data.get("quantity");
        if (quantityStr == null || quantityStr.trim().isEmpty()) {
            quantityStr = data.get("数量");
        }
        request.setQuantity(Integer.parseInt(quantityStr.trim()));
        
        // 可用数量 - 支持中文列名
        String availableStr = data.get("availableQuantity");
        if (availableStr == null || availableStr.trim().isEmpty()) {
            availableStr = data.get("可用数量");
        }
        request.setAvailableQuantity(Integer.parseInt(availableStr.trim()));
        
        // 状态 - 支持中文列名
        String status = data.get("status");
        if (status == null || status.trim().isEmpty()) {
            status = data.get("状态");
        }
        request.setStatus(status.trim());
        
        // 设置可选字段，支持中文列名
        if (data.containsKey("description") && data.get("description") != null) {
            request.setDescription(data.get("description"));
        } else if (data.containsKey("描述") && data.get("描述") != null) {
            request.setDescription(data.get("描述"));
        }
        
        if (data.containsKey("manufacturer") && data.get("manufacturer") != null) {
            request.setManufacturer(data.get("manufacturer"));
        } else if (data.containsKey("品牌") && data.get("品牌") != null) {
            request.setManufacturer(data.get("品牌"));
        }
        
        if (data.containsKey("model") && data.get("model") != null) {
            request.setModel(data.get("model"));
        } else if (data.containsKey("型号") && data.get("型号") != null) {
            request.setModel(data.get("型号"));
        }
        
        if (data.containsKey("location") && data.get("location") != null) {
            request.setLocation(data.get("location"));
        } else if (data.containsKey("位置") && data.get("位置") != null) {
            request.setLocation(data.get("位置"));
        }
        
        // 购买价格 - 支持中文列名
        if (data.containsKey("purchasePrice") && data.get("purchasePrice") != null && !data.get("purchasePrice").trim().isEmpty()) {
            try {
                request.setPurchasePrice(new java.math.BigDecimal(data.get("purchasePrice").trim()));
            } catch (NumberFormatException ignored) {
                // 忽略无效价格
            }
        } else if (data.containsKey("购买价格") && data.get("购买价格") != null && !data.get("购买价格").trim().isEmpty()) {
            try {
                request.setPurchasePrice(new java.math.BigDecimal(data.get("购买价格").trim()));
            } catch (NumberFormatException ignored) {
                // 忽略无效价格
            }
        }
        
        // 购买日期 - 支持中文列名
        if (data.containsKey("purchaseDate") && data.get("purchaseDate") != null && !data.get("purchaseDate").trim().isEmpty()) {
            try {
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
                java.util.Date parsedDate = dateFormat.parse(data.get("purchaseDate").trim());
                request.setPurchaseDate(java.time.LocalDateTime.ofInstant(parsedDate.toInstant(), java.time.ZoneId.systemDefault()));
            } catch (java.text.ParseException ignored) {
                // 忽略无效日期
            }
        } else if (data.containsKey("购买日期") && data.get("购买日期") != null && !data.get("购买日期").trim().isEmpty()) {
            try {
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
                java.util.Date parsedDate = dateFormat.parse(data.get("购买日期").trim());
                request.setPurchaseDate(java.time.LocalDateTime.ofInstant(parsedDate.toInstant(), java.time.ZoneId.systemDefault()));
            } catch (java.text.ParseException ignored) {
                // 忽略无效日期
            }
        }
        
        return request;
    }
    
    /**
     * 判断是否为空行
     */
    private boolean isEmptyRow(Row row) {
        if (row == null) return true;
        
        boolean isEmpty = true;
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellValueAsString(cell).trim().isEmpty()) {
                isEmpty = false;
                break;
            }
        }
        return isEmpty;
    }
    
    /**
     * 判断是否为注释行
     */
    private boolean isCommentRow(Row row) {
        if (row == null || row.getCell(0) == null) return false;
        
        String firstCellValue = getCellValueAsString(row.getCell(0));
        return firstCellValue.startsWith("#");
    }
    
    /**
     * 获取单元格值为字符串
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
                } else {
                    // 处理科学计数法问题
                    return String.valueOf((long)cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception ex) {
                        return "";
                    }
                }
            default:
                return "";
        }
    }
} 