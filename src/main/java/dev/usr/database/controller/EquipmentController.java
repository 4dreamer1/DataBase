package dev.usr.database.controller;

import dev.usr.database.entity.Equipment;
import dev.usr.database.payload.request.EquipmentRequest;
import dev.usr.database.payload.request.StatusUpdateRequest;
import dev.usr.database.payload.response.ApiResponse;
import dev.usr.database.payload.response.EquipmentStatisticsResponse;
import dev.usr.database.payload.response.PagedResponse;
import dev.usr.database.service.EquipmentService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import dev.usr.database.entity.Category;
import dev.usr.database.repository.CategoryRepository;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;

/**
 * 装备管理控制器
 */
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {
    @Autowired
    private EquipmentService equipmentService;
    @Autowired
    private CategoryRepository categoryRepository;
    
    /**
     * 获取装备列表（支持搜索、分页、排序）
     */
    @GetMapping
    public ResponseEntity<PagedResponse<Equipment>> getAllEquipments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {
        
        log.info("获取装备列表 - 页码: {}, 大小: {}, 关键词: {}, 分类: {}, 状态: {}", 
                page, size, keyword, categoryId, status);
        
        // 构建排序
        String sortField = sort[0];
        String sortDirection = sort.length > 1 ? sort[1] : "asc";
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sortObj = Sort.by(direction, sortField);
        
        // 构建分页
        Pageable pageable = PageRequest.of(page, size, sortObj);
        
        // 执行搜索
        Page<Equipment> pageResult = equipmentService.search(keyword, categoryId, status, pageable);
        
        // 构建响应
        PagedResponse<Equipment> response = new PagedResponse<>(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isLast()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * 导出装备数据（支持CSV和Excel格式）
     */
    @GetMapping("/export")
    public void exportEquipment(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "csv") String format,
            HttpServletResponse response) throws IOException {
        
        log.info("导出装备数据 - 格式: {}, 关键词: {}, 分类: {}", format, keyword, categoryId);
        
        try {
            // 简化文件名生成逻辑
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String filename = "equipment_" + timestamp;
            
            // 设置响应头
            response.setContentType(
                "excel".equalsIgnoreCase(format) 
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" 
                : "text/csv;charset=UTF-8"
            );
            
            String ext = "excel".equalsIgnoreCase(format) ? ".xlsx" : ".csv";
            String encodedFilename = URLEncoder.encode(filename + ext, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
            
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename*=UTF-8''" + encodedFilename);
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            
            // 执行导出
            equipmentService.exportEquipment(keyword, categoryId, status, format, response.getOutputStream());
        } catch (Exception e) {
            log.error("导出装备数据失败", e);
            response.reset();
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"message\":\"导出失败: " + e.getMessage() + "\"}");
        }
    }

    /**
     * 根据ID获取装备详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Equipment> getEquipmentById(@PathVariable Long id) {
        log.info("获取装备详情 - ID: {}", id);
        Equipment equipment = equipmentService.findById(id);
        return ResponseEntity.ok(equipment);
    }

    /**
     * 根据分类获取装备
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Equipment>> getEquipmentsByCategory(@PathVariable Long categoryId) {
        log.info("获取分类下装备 - 分类ID: {}", categoryId);
        List<Equipment> equipments = equipmentService.findByCategory(categoryId);
        return ResponseEntity.ok(equipments);
    }

    /**
     * 获取可用装备
     */
    @GetMapping("/available")
    public ResponseEntity<List<Equipment>> getAvailableEquipments() {
        log.info("获取可用装备");
        List<Equipment> equipments = equipmentService.findAvailable();
        return ResponseEntity.ok(equipments);
    }

    /**
     * 获取库存不足的装备
     */
    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Equipment>> getLowStockEquipments() {
        log.info("获取库存不足装备");
        List<Equipment> equipments = equipmentService.findLowStock();
        return ResponseEntity.ok(equipments);
    }
    
    /**
     * 获取装备统计数据
     */
    @GetMapping("/statistics")
    public ResponseEntity<EquipmentStatisticsResponse> getStatistics() {
        log.info("获取装备统计数据");
        EquipmentStatisticsResponse statistics = equipmentService.getStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * 检查序列号是否已存在
     */
    @GetMapping("/check-serial/{serialNumber}")
    public ResponseEntity<Boolean> checkSerialNumber(
            @PathVariable String serialNumber,
            @RequestParam(required = false) Long excludeId) {
        log.info("检查序列号 - 序列号: {}, 排除ID: {}", serialNumber, excludeId);
        boolean exists = (excludeId != null) 
                ? equipmentService.isSerialNumberExists(serialNumber, excludeId)
                : equipmentService.isSerialNumberExists(serialNumber);
        return ResponseEntity.ok(exists);
    }

    /**
     * 创建新装备
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Equipment> createEquipment(@Valid @RequestBody EquipmentRequest request) {
        log.info("创建新装备 - 名称: {}", request.getName());
        Equipment savedEquipment = equipmentService.save(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedEquipment);
    }

    /**
     * 更新装备
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Equipment> updateEquipment(
            @PathVariable Long id, 
            @Valid @RequestBody EquipmentRequest request) {
        log.info("更新装备 - ID: {}, 名称: {}", id, request.getName());
        Equipment updatedEquipment = equipmentService.update(id, request);
        return ResponseEntity.ok(updatedEquipment);
    }
    
    /**
     * 更新装备状态
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Equipment> updateEquipmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        log.info("更新装备状态 - ID: {}, 状态: {}", id, request.getStatus());
        Equipment updatedEquipment = equipmentService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(updatedEquipment);
    }

    /**
     * 删除装备
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteEquipment(@PathVariable Long id) {
        log.info("删除装备 - ID: {}", id);
        equipmentService.delete(id);
        return ResponseEntity.ok(new ApiResponse(true, "装备已成功删除"));
    }
    
    /**
     * 批量删除装备
     */
    @PostMapping("/bulk-delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> bulkDeleteEquipment(@RequestBody List<Long> ids) {
        log.info("批量删除装备 - IDs: {}", ids);
        equipmentService.deleteAll(ids);
        return ResponseEntity.ok(new ApiResponse(true, "已成功删除所选装备"));
    }
    
    /**
     * 获取导入模板
     */
    @GetMapping("/import/template")
    @PreAuthorize("hasRole('ADMIN')")
    public void getImportTemplate(
            @RequestParam(defaultValue = "csv") String format,
            HttpServletResponse response) throws IOException {
        
        log.info("获取导入模板 - 格式: {}", format);
        
        try {
            // 生成文件名
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String filename = "equipment_template_" + timestamp;
            
            // 设置响应头
            response.setContentType(
                "excel".equalsIgnoreCase(format) 
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" 
                : "text/csv;charset=UTF-8"
            );
            
            String ext = "excel".equalsIgnoreCase(format) ? ".xlsx" : ".csv";
            String encodedFilename = URLEncoder.encode(filename + ext, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
            
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename*=UTF-8''" + encodedFilename);
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            
            // 生成模板
            equipmentService.generateImportTemplate(format, response.getOutputStream());
        } catch (Exception e) {
            log.error("生成导入模板失败", e);
            response.reset();
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"message\":\"生成导入模板失败: " + e.getMessage() + "\"}");
        }
    }
    
    /**
     * 导入装备数据
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importEquipment(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "true") boolean updateExisting,
            @RequestParam(defaultValue = "false") boolean skipErrors) {
        
        log.info("导入装备数据 - 文件名: {}, 更新已有: {}, 跳过错误: {}", 
                file.getOriginalFilename(), updateExisting, skipErrors);
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "请选择要导入的文件"));
        }
        
        // 验证文件类型
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".csv") && !filename.endsWith(".xlsx"))) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "仅支持.csv和.xlsx格式的文件"));
        }
        
        try {
            // 执行导入
            Map<String, Object> result = equipmentService.importEquipment(file, updateExisting, skipErrors);
            
            // 返回导入结果
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("成功导入 %d 条记录", result.get("imported")));
            response.put("imported", result.get("imported"));
            response.put("updated", result.get("updated"));
            
            if (result.containsKey("errors") && ((List<String>)result.get("errors")).size() > 0) {
                response.put("hasErrors", true);
                response.put("errors", result.get("errors"));
                response.put("message", String.format("部分导入成功（%d 条），存在 %d 条错误记录", 
                        result.get("imported"), ((List<String>)result.get("errors")).size()));
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("导入装备数据失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "导入失败: " + e.getMessage()));
        }
    }
    
    /**
     * 生成导入模板
     */
    @GetMapping("/template")
    public ResponseEntity<Resource> generateTemplate(
            @RequestParam(value = "type", defaultValue = "excel") String type
    ) {
        try {
            // 获取所有分类
            List<Category> categories = categoryRepository.findAll();
            ByteArrayResource resource;
            String filename;
            String contentType;

            if ("csv".equalsIgnoreCase(type)) {
                resource = new ByteArrayResource(equipmentService.generateImportTemplate(categories, true));
                filename = "equipment_import_template.csv";
                contentType = "text/csv";
            } else {
                resource = new ByteArrayResource(equipmentService.generateImportTemplate(categories, false));
                filename = "equipment_import_template.xlsx";
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            log.error("生成导入模板失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * 检查认证状态
     * 此端点用于前端验证用户认证状态是否有效
     */
    @GetMapping("/check-auth")
    public ResponseEntity<ApiResponse> checkAuthentication() {
        log.debug("检查认证状态");
        return ResponseEntity.ok(new ApiResponse(true, "认证有效"));
    }
}