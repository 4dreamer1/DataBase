package dev.usr.database.controller;

import dev.usr.database.entity.BorrowRecord;
import dev.usr.database.entity.BorrowStatus;
import dev.usr.database.payload.request.BorrowRequest;
import dev.usr.database.payload.response.MessageResponse;
import dev.usr.database.security.services.UserDetailsImpl;
import dev.usr.database.service.BorrowService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/borrow")
public class BorrowController {
    private static final Logger logger = LoggerFactory.getLogger(BorrowController.class);
    
    @Autowired
    private BorrowService borrowService;

    @GetMapping
    public ResponseEntity<?> getAllBorrows() {
        try {
            List<BorrowRecord> borrows = borrowService.findAll();
            
            // 包装为前端期望的分页格式
            Map<String, Object> response = new HashMap<>();
            response.put("content", borrows);
            response.put("totalElements", borrows.size());
            response.put("totalPages", 1);
            response.put("number", 0);
            response.put("size", borrows.size());
            response.put("last", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取借用记录失败", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("获取借用记录失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBorrowById(@PathVariable Long id) {
        try {
            logger.info("获取借用记录详情, ID: {}", id);
            BorrowRecord borrow = borrowService.findById(id);
            
            if (borrow == null) {
                logger.warn("未找到借用记录, ID: {}", id);
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new MessageResponse("未找到借用记录: " + id));
            }
            
            // 确保装备和用户信息已加载
            if (borrow.getEquipment() != null) {
                logger.info("借用记录关联的装备: {}", borrow.getEquipment().getName());
            } else {
                logger.warn("借用记录缺少装备信息, ID: {}", id);
            }
            
            if (borrow.getBorrower() != null) {
                logger.info("借用记录关联的借用人: {}", borrow.getBorrower().getName());
            } else {
                logger.warn("借用记录缺少借用人信息, ID: {}", id);
            }
            
            logger.info("成功获取借用记录详情, ID: {}", id);
            return ResponseEntity.ok(borrow);
        } catch (Exception e) {
            logger.error("获取借用记录详情失败, ID: " + id, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("获取借用记录详情失败: " + e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyBorrows(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            List<BorrowRecord> borrows = borrowService.findByUserId(userDetails.getId());
            return ResponseEntity.ok(borrows);
        } catch (Exception e) {
            logger.error("获取用户借用记录失败, 用户ID: " + userDetails.getId(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("获取借用记录失败: " + e.getMessage()));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<?> getMyActiveBorrows(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            List<BorrowRecord> borrows = borrowService.findActiveByUserId(userDetails.getId());
            return ResponseEntity.ok(borrows);
        } catch (Exception e) {
            logger.error("获取用户活跃借用记录失败, 用户ID: " + userDetails.getId(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("获取活跃借用记录失败: " + e.getMessage()));
        }
    }

    @GetMapping("/active/{equipmentId}")
    public ResponseEntity<?> getActiveByEquipmentId(@PathVariable Long equipmentId) {
        try {
            logger.info("获取装备的活跃借用记录, 装备ID: {}", equipmentId);
            List<BorrowRecord> borrows = borrowService.findActiveByEquipmentId(equipmentId);
            return ResponseEntity.ok(borrows);
        } catch (Exception e) {
            logger.error("获取装备活跃借用记录失败, 装备ID: " + equipmentId, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("获取装备活跃借用记录失败: " + e.getMessage()));
        }
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingBorrows() {
        try {
            List<BorrowRecord> borrows = borrowService.findByStatus(BorrowStatus.PENDING);
            return ResponseEntity.ok(borrows);
        } catch (Exception e) {
            logger.error("获取待审批借用记录失败", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("获取待审批借用记录失败: " + e.getMessage()));
        }
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getOverdueBorrows() {
        try {
            List<BorrowRecord> borrows = borrowService.findOverdue();
            return ResponseEntity.ok(borrows);
        } catch (Exception e) {
            logger.error("获取逾期借用记录失败", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("获取逾期借用记录失败: " + e.getMessage()));
        }
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getExpiringBorrows(@RequestParam(defaultValue = "3") int days) {
        try {
            List<BorrowRecord> borrows = borrowService.findExpiring(days);
            return ResponseEntity.ok(borrows);
        } catch (Exception e) {
            logger.error("获取即将到期借用记录失败", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("获取即将到期借用记录失败: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createBorrowRequest(@Valid @RequestBody BorrowRequest borrowRequest, 
                                               @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            logger.info("创建借用请求: 用户ID={}, 装备ID={}, 数量={}", 
                userDetails.getId(), borrowRequest.getEquipmentId(), borrowRequest.getQuantity());
                
            BorrowRecord borrowRecord = borrowService.createBorrowRequest(borrowRequest, userDetails.getId());
            return ResponseEntity.ok(borrowRecord);
        } catch (Exception e) {
            logger.error("创建借用记录失败", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("创建借用记录失败: " + e.getMessage()));
        }
    }

    @PostMapping("/apply")
    public ResponseEntity<?> applyForBorrow(@Valid @RequestBody BorrowRequest borrowRequest, 
                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            logger.info("提交借用申请: 用户ID={}, 装备ID={}, 数量={}", 
                userDetails.getId(), borrowRequest.getEquipmentId(), borrowRequest.getQuantity());
                
            // 验证用户是否已登录
            if (userDetails == null || userDetails.getId() == null) {
                logger.warn("未授权的借用申请");
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(new MessageResponse("您需要先登录才能提交借用申请"));
            }
                
            BorrowRecord borrowRecord = borrowService.createBorrowRequest(borrowRequest, userDetails.getId());
            return ResponseEntity.ok(borrowRecord);
        } catch (EntityNotFoundException e) {
            logger.error("提交借用申请时找不到资源", e);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            logger.error("提交借用申请时状态错误", e);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("提交借用申请失败", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("提交借用申请失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> request, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // 记录批准备注 (如果有)
            String note = request != null && request.containsKey("note") ? (String) request.get("note") : null;
            logger.info("批准借用申请: ID={}, 批准人={}, 备注={}", id, userDetails.getUsername(), note);
            
            BorrowRecord borrow = borrowService.approve(id, userDetails.getId());
            return ResponseEntity.ok(borrow);
        } catch (Exception e) {
            logger.error("审批借用记录失败, ID: " + id, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("审批借用记录失败: " + e.getMessage()));
        }
    }

    @PutMapping("/batch-approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> batchApprove(@RequestBody Map<String, Object> request, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            List<Long> ids = (List<Long>) request.get("ids");
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("未提供要批准的借用ID列表"));
            }
            
            List<BorrowRecord> approvedRecords = new ArrayList<>();
            for (Long id : ids) {
                try {
                    BorrowRecord borrow = borrowService.approve(id, userDetails.getId());
                    approvedRecords.add(borrow);
                } catch (Exception e) {
                    logger.error("批量审批时单条记录失败, ID: " + id, e);
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "成功批准" + approvedRecords.size() + "条记录",
                "approvedCount", approvedRecords.size(),
                "totalCount", ids.size()
            ));
        } catch (Exception e) {
            logger.error("批量审批借用记录失败", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("批量审批借用记录失败: " + e.getMessage()));
        }
    }
    
    @PutMapping("/batch-reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> batchReject(@RequestBody Map<String, Object> request, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            List<Long> ids = (List<Long>) request.get("ids");
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("未提供要拒绝的借用ID列表"));
            }
            
            List<BorrowRecord> rejectedRecords = new ArrayList<>();
            for (Long id : ids) {
                try {
                    BorrowRecord borrow = borrowService.reject(id, userDetails.getId());
                    rejectedRecords.add(borrow);
                } catch (Exception e) {
                    logger.error("批量拒绝时单条记录失败, ID: " + id, e);
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "成功拒绝" + rejectedRecords.size() + "条记录",
                "rejectedCount", rejectedRecords.size(),
                "totalCount", ids.size()
            ));
        } catch (Exception e) {
            logger.error("批量拒绝借用记录失败", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("批量拒绝借用记录失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectBorrow(@PathVariable Long id, @RequestBody Map<String, Object> request, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // 获取拒绝原因
            String reason = request.containsKey("reason") ? (String) request.get("reason") : null;
            logger.info("拒绝借用申请: ID={}, 拒绝人={}, 原因={}", id, userDetails.getUsername(), reason);
            
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("拒绝原因不能为空"));
            }
            
            BorrowRecord borrowRecord = borrowService.reject(id, userDetails.getId());
            return ResponseEntity.ok(borrowRecord);
        } catch (Exception e) {
            logger.error("拒绝借用记录失败, ID: " + id, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("拒绝借用记录失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/return")
    public ResponseEntity<?> returnEquipment(@PathVariable Long id) {
        try {
            BorrowRecord borrowRecord = borrowService.returnEquipment(id);
            return ResponseEntity.ok(borrowRecord);
        } catch (Exception e) {
            logger.error("归还装备失败, ID: " + id, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("归还装备失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteBorrow(@PathVariable Long id) {
        try {
            borrowService.delete(id);
            return ResponseEntity.ok(new MessageResponse("借用记录已成功删除"));
        } catch (Exception e) {
            logger.error("删除借用记录失败, ID: " + id, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("删除借用记录失败: " + e.getMessage()));
        }
    }
    
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // 获取各种状态的借用记录数量
            statistics.put("totalBorrows", borrowService.countAll());
            statistics.put("pendingCount", borrowService.countByStatus(BorrowStatus.PENDING));
            statistics.put("borrowedCount", borrowService.countByStatus(BorrowStatus.BORROWED));
            statistics.put("returnedCount", borrowService.countByStatus(BorrowStatus.RETURNED));
            statistics.put("rejectedCount", borrowService.countByStatus(BorrowStatus.REJECTED));
            statistics.put("overdueCount", borrowService.countOverdue());
            
            // 获取今日借出和归还数量
            LocalDate today = LocalDate.now();
            statistics.put("todayBorrows", borrowService.countCreatedOnDate(today));
            statistics.put("todayReturns", borrowService.countReturnedOnDate(today));
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("获取统计数据失败", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("获取统计数据失败: " + e.getMessage()));
        }
    }

    @GetMapping("/trends")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getTrendData(@RequestParam(defaultValue = "week") String period) {
        try {
            logger.info("获取借用趋势数据, 时间范围: {}", period);
            
            LocalDate endDate = LocalDate.now();
            LocalDate startDate;
            
            // 根据请求的时间范围设置开始日期
            switch (period) {
                case "week":
                    startDate = endDate.minusDays(6); // 最近7天
                    break;
                case "month":
                    startDate = endDate.minusDays(29); // 最近30天
                    break;
                case "quarter":
                    startDate = endDate.minusDays(89); // 最近90天
                    break;
                case "year":
                    startDate = endDate.minusDays(364); // 最近365天
                    break;
                default:
                    startDate = endDate.minusDays(6); // 默认最近7天
            }
            
            // 获取趋势数据
            List<Map<String, Object>> trendData = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
            
            // 为每一天创建数据点
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                Map<String, Object> dataPoint = new HashMap<>();
                String formattedDate = currentDate.format(formatter);
                
                dataPoint.put("date", formattedDate);
                dataPoint.put("borrowCount", borrowService.countCreatedOnDate(currentDate));
                dataPoint.put("returnCount", borrowService.countReturnedOnDate(currentDate));
                
                trendData.add(dataPoint);
                currentDate = currentDate.plusDays(1);
            }
            
            logger.info("借用趋势数据获取成功, 数据点数量: {}", trendData.size());
            return ResponseEntity.ok(trendData);
        } catch (Exception e) {
            logger.error("获取借用趋势数据失败", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("获取借用趋势数据失败: " + e.getMessage()));
        }
    }

    @GetMapping("/equipment-ranking")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getEquipmentRanking() {
        try {
            logger.info("获取装备借用排行数据");
            
            List<Map<String, Object>> rankingData = borrowService.getEquipmentRanking();
            logger.info("装备借用排行数据获取成功, 数量: {}", rankingData.size());
            
            return ResponseEntity.ok(rankingData);
        } catch (Exception e) {
            logger.error("获取装备借用排行数据失败", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("获取装备借用排行数据失败: " + e.getMessage()));
        }
    }

    @GetMapping("/user-ranking")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserRanking() {
        try {
            logger.info("获取用户借用排行数据");
            
            List<Map<String, Object>> rankingData = borrowService.getUserRanking();
            logger.info("用户借用排行数据获取成功, 数量: {}", rankingData.size());
            
            return ResponseEntity.ok(rankingData);
        } catch (Exception e) {
            logger.error("获取用户借用排行数据失败", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("获取用户借用排行数据失败: " + e.getMessage()));
        }
    }

    @GetMapping("/my/stats")
    public ResponseEntity<?> getMyBorrowStats(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            logger.info("获取用户个人借用统计数据, 用户ID: {}", userDetails.getId());
            
            // 查询该用户的所有借用记录
            List<BorrowRecord> userBorrows = borrowService.findByUserId(userDetails.getId());
            
            // 统计各种状态的借用记录数量
            int pendingCount = 0;
            int activeCount = 0;
            int overdueCount = 0;
            int returnedCount = 0;
            
            LocalDateTime now = LocalDateTime.now();
            
            for (BorrowRecord borrow : userBorrows) {
                if (borrow.getStatus() == BorrowStatus.PENDING) {
                    pendingCount++;
                } else if (borrow.getStatus() == BorrowStatus.APPROVED || borrow.getStatus() == BorrowStatus.BORROWED) {
                    if (borrow.getExpectedReturnDate() != null && borrow.getExpectedReturnDate().isBefore(now)) {
                        overdueCount++;
                    } else {
                        activeCount++;
                    }
                } else if (borrow.getStatus() == BorrowStatus.RETURNED) {
                    returnedCount++;
                }
            }
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("pending", pendingCount);
            stats.put("active", activeCount);
            stats.put("overdue", overdueCount);
            stats.put("returned", returnedCount);
            
            logger.info("用户借用统计数据: 用户ID={}, 待审批={}, 使用中={}, 已逾期={}, 已归还={}", 
                userDetails.getId(), pendingCount, activeCount, overdueCount, returnedCount);
                    
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("获取用户借用统计数据失败, 用户ID: " + userDetails.getId(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("获取用户借用统计数据失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/send-reminder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sendReminder(@PathVariable Long id, @RequestBody(required = false) Map<String, String> request) {
        try {
            String message = request != null && request.containsKey("message") ? request.get("message") : null;
            BorrowRecord record = borrowService.findById(id);
            
            if (record == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 在实际应用中，这里应该发送邮件或短信提醒
            logger.info("发送提醒给用户: {}, 借用ID: {}, 消息: {}", 
                record.getBorrower().getUsername(), id, message);
            
            // 标记为已提醒
            record.setReminderSent(true);
            record.setReminderDate(LocalDateTime.now());
            record.setReminderMessage(message);
            borrowService.save(record);
            
            return ResponseEntity.ok(new MessageResponse("提醒已发送"));
        } catch (Exception e) {
            logger.error("发送提醒失败, ID: " + id, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("发送提醒失败: " + e.getMessage()));
        }
    }
    
    @PutMapping("/batch-send-reminders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sendBatchReminders(@RequestBody Map<String, Object> request) {
        try {
            List<Long> ids = (List<Long>) request.get("ids");
            String message = request.containsKey("message") ? (String) request.get("message") : null;
            
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("未提供要发送提醒的借用ID列表"));
            }
            
            int successCount = 0;
            for (Long id : ids) {
                try {
                    BorrowRecord record = borrowService.findById(id);
                    if (record != null) {
                        // 在实际应用中，这里应该发送邮件或短信提醒
                        logger.info("批量发送提醒给用户: {}, 借用ID: {}, 消息: {}", 
                            record.getBorrower().getUsername(), id, message);
                        
                        // 标记为已提醒
                        record.setReminderSent(true);
                        record.setReminderDate(LocalDateTime.now());
                        record.setReminderMessage(message);
                        borrowService.save(record);
                        successCount++;
                    }
                } catch (Exception e) {
                    logger.error("批量发送提醒时单条记录失败, ID: " + id, e);
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "成功发送" + successCount + "条提醒",
                "successCount", successCount,
                "totalCount", ids.size()
            ));
        } catch (Exception e) {
            logger.error("批量发送提醒失败", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("批量发送提醒失败: " + e.getMessage()));
        }
    }
} 