package dev.usr.database.service.impl;

import dev.usr.database.entity.BorrowRecord;
import dev.usr.database.entity.BorrowStatus;
import dev.usr.database.entity.Equipment;
import dev.usr.database.entity.User;
import dev.usr.database.payload.request.BorrowRequest;
import dev.usr.database.repository.BorrowRecordRepository;
import dev.usr.database.repository.EquipmentRepository;
import dev.usr.database.repository.UserRepository;
import dev.usr.database.service.BorrowService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BorrowServiceImpl implements BorrowService {
    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Override
    public List<BorrowRecord> findAll() {
        return borrowRecordRepository.findAll();
    }

    @Override
    public BorrowRecord findById(Long id) {
        try {
            BorrowRecord record = borrowRecordRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("未找到借用记录: " + id));
            
            // 确保关联实体已加载
            if (record.getEquipment() != null) {
                try {
                    // 触发装备信息加载
                    String equipmentName = record.getEquipment().getName();
                    
                    // 记录加载了什么信息
                    System.out.println("成功加载装备信息: " + equipmentName);
                    
                    // 加载装备分类信息
                    if (record.getEquipment().getCategory() != null) {
                        String categoryName = record.getEquipment().getCategory().getName();
                        System.out.println("成功加载分类信息: " + categoryName);
                    }
                } catch (Exception e) {
                    System.err.println("加载装备相关信息失败: " + e.getMessage());
                    // 如果装备信息加载失败，我们尝试重新获取
                    Equipment equipment = equipmentRepository.findById(record.getEquipment().getId())
                            .orElse(null);
                    record.setEquipment(equipment);
                }
            } else {
                System.err.println("借用记录缺少装备信息，ID: " + id);
            }
            
            // 确保借用人信息已加载
            if (record.getBorrower() != null) {
                try {
                    String borrowerName = record.getBorrower().getName();
                    System.out.println("成功加载借用人信息: " + borrowerName);
                } catch (Exception e) {
                    System.err.println("加载借用人信息失败: " + e.getMessage());
                    // 如果借用人信息加载失败，我们尝试重新获取
                    User borrower = userRepository.findById(record.getBorrower().getId())
                            .orElse(null);
                    record.setBorrower(borrower);
                }
            } else {
                System.err.println("借用记录缺少借用人信息，ID: " + id);
            }
            
            // 确保审批人信息已加载
            if (record.getApprover() != null) {
                try {
                    String approverName = record.getApprover().getName();
                    System.out.println("成功加载审批人信息: " + approverName);
                } catch (Exception e) {
                    System.err.println("加载审批人信息失败: " + e.getMessage());
                    // 如果审批人信息加载失败，我们尝试重新获取
                    User approver = userRepository.findById(record.getApprover().getId())
                            .orElse(null);
                    record.setApprover(approver);
                }
            }
            
            return record;
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("获取借用记录失败，详细错误: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("获取借用记录失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BorrowRecord> findByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("未找到用户: " + userId));
        return borrowRecordRepository.findByBorrower(user);
    }

    @Override
    public List<BorrowRecord> findActiveByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("未找到用户: " + userId));
        return borrowRecordRepository.findActiveBorrowsByUser(user);
    }

    @Override
    public List<BorrowRecord> findActiveByEquipmentId(Long equipmentId) {
        try {
            // 验证装备是否存在
            equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new EntityNotFoundException("未找到装备: " + equipmentId));
            
            // 查询该装备的所有活跃借用记录
            return borrowRecordRepository.findActiveBorrowsByEquipmentId(equipmentId);
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("获取装备活跃借用记录失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BorrowRecord> findByStatus(BorrowStatus status) {
        return borrowRecordRepository.findByStatus(status);
    }

    @Override
    public List<BorrowRecord> findOverdue() {
        return borrowRecordRepository.findOverdueRecords(LocalDateTime.now());
    }

    @Override
    public List<BorrowRecord> findExpiring(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusDays(days);
        return borrowRecordRepository.findExpiringRecords(now, end);
    }

    @Override
    @Transactional
    public BorrowRecord createBorrowRequest(BorrowRequest borrowRequest, Long userId) {
        try {
            User borrower = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("未找到用户: " + userId));
            
            Equipment equipment = equipmentRepository.findById(borrowRequest.getEquipmentId())
                    .orElseThrow(() -> new EntityNotFoundException("未找到装备: " + borrowRequest.getEquipmentId()));
            
            // 检查装备状态
            if (!"可用".equals(equipment.getStatus())) {
                throw new IllegalStateException("该装备当前状态为：" + equipment.getStatus() + "，不可借用");
            }
            
            if (equipment.getAvailableQuantity() < borrowRequest.getQuantity()) {
                throw new IllegalStateException("可用数量不足，当前可用: " + equipment.getAvailableQuantity());
            }
            
            BorrowRecord borrowRecord = new BorrowRecord();
            borrowRecord.setEquipment(equipment);
            borrowRecord.setBorrower(borrower);
            borrowRecord.setBorrowDate(LocalDateTime.now());
            
            // 处理预计归还日期
            LocalDateTime expectedReturnDate = borrowRequest.getExpectedReturnDate();
            if (expectedReturnDate == null) {
                // 如果未提供预计归还日期，默认设置为7天后
                expectedReturnDate = LocalDateTime.now().plusDays(7);
            }
            borrowRecord.setExpectedReturnDate(expectedReturnDate);
            
            borrowRecord.setQuantity(borrowRequest.getQuantity());
            borrowRecord.setStatus(BorrowStatus.PENDING);
            borrowRecord.setPurpose(borrowRequest.getPurpose());
            borrowRecord.setRemarks(borrowRequest.getRemarks());
            
            return borrowRecordRepository.save(borrowRecord);
        } catch (EntityNotFoundException | IllegalStateException e) {
            // 直接抛出已知异常
            throw e;
        } catch (Exception e) {
            // 记录并包装其他异常
            throw new RuntimeException("创建借用记录失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public BorrowRecord approve(Long id, Long approverId) {
        BorrowRecord borrowRecord = findById(id);
        
        if (borrowRecord.getStatus() != BorrowStatus.PENDING) {
            throw new IllegalStateException("只能审批待处理的借用请求");
        }
        
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new EntityNotFoundException("未找到用户: " + approverId));
        
        Equipment equipment = borrowRecord.getEquipment();
        
        // 检查装备状态
        if (!"可用".equals(equipment.getStatus())) {
            throw new IllegalStateException("该装备当前状态为：" + equipment.getStatus() + "，不可借用");
        }
        
        if (equipment.getAvailableQuantity() < borrowRecord.getQuantity()) {
            throw new IllegalStateException("可用数量不足");
        }
        
        // 更新装备可用数量
        equipment.setAvailableQuantity(equipment.getAvailableQuantity() - borrowRecord.getQuantity());
        equipmentRepository.save(equipment);
        
        // 更新借用记录
        borrowRecord.setApprover(approver);
        borrowRecord.setStatus(BorrowStatus.BORROWED);
        
        // 记录最后更新时间
        borrowRecord.setUpdatedAt(LocalDateTime.now());
        
        return borrowRecordRepository.save(borrowRecord);
    }

    @Override
    @Transactional
    public BorrowRecord reject(Long id, Long approverId) {
        BorrowRecord borrowRecord = findById(id);
        
        if (borrowRecord.getStatus() != BorrowStatus.PENDING) {
            throw new IllegalStateException("只能拒绝待处理的借用请求");
        }
        
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new EntityNotFoundException("未找到用户: " + approverId));
        
        borrowRecord.setApprover(approver);
        borrowRecord.setStatus(BorrowStatus.REJECTED);
        
        // 记录最后更新时间
        borrowRecord.setUpdatedAt(LocalDateTime.now());
        
        return borrowRecordRepository.save(borrowRecord);
    }

    @Override
    @Transactional
    public BorrowRecord returnEquipment(Long id) {
        try {
            BorrowRecord borrowRecord = findById(id);
            
            if (borrowRecord.getStatus() != BorrowStatus.BORROWED && borrowRecord.getStatus() != BorrowStatus.OVERDUE) {
                throw new IllegalStateException("只能归还已借出或逾期的装备，当前状态: " + borrowRecord.getStatus());
            }
            
            Equipment equipment = borrowRecord.getEquipment();
            if (equipment == null) {
                throw new IllegalStateException("借用记录关联的装备不存在");
            }
            
            // 更新装备可用数量
            equipment.setAvailableQuantity(equipment.getAvailableQuantity() + borrowRecord.getQuantity());
            equipmentRepository.save(equipment);
            
            // 更新借用记录
            borrowRecord.setActualReturnDate(LocalDateTime.now());
            borrowRecord.setStatus(BorrowStatus.RETURNED);
            
            return borrowRecordRepository.save(borrowRecord);
        } catch (EntityNotFoundException | IllegalStateException e) {
            // 直接抛出已知异常
            throw e;
        } catch (Exception e) {
            // 记录并包装其他异常
            throw new RuntimeException("归还装备失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        BorrowRecord borrowRecord = findById(id);
        
        // 如果是已借出状态，需要归还装备库存
        if (borrowRecord.getStatus() == BorrowStatus.BORROWED || borrowRecord.getStatus() == BorrowStatus.OVERDUE) {
            Equipment equipment = borrowRecord.getEquipment();
            equipment.setAvailableQuantity(equipment.getAvailableQuantity() + borrowRecord.getQuantity());
            equipmentRepository.save(equipment);
        }
        
        borrowRecordRepository.delete(borrowRecord);
    }
    
    @Override
    @Transactional
    public BorrowRecord save(BorrowRecord borrowRecord) {
        return borrowRecordRepository.save(borrowRecord);
    }
    
    // 实现统计相关方法
    @Override
    public int countByStatus(BorrowStatus status) {
        return borrowRecordRepository.countByStatus(status);
    }
    
    @Override
    public int countAll() {
        return (int) borrowRecordRepository.count();
    }
    
    @Override
    public int countOverdue() {
        return borrowRecordRepository.countOverdueRecords();
    }
    
    @Override
    public int countCreatedOnDate(LocalDate date) {
        try {
            return borrowRecordRepository.countCreatedOnDate(date);
        } catch (Exception e) {
            // 如果查询失败，返回0
            return 0;
        }
    }
    
    @Override
    public int countReturnedOnDate(LocalDate date) {
        try {
            return borrowRecordRepository.countReturnedOnDate(date);
        } catch (Exception e) {
            // 如果查询失败，返回0
            return 0;
        }
    }
    
    @Override
    public List<Map<String, Object>> getEquipmentRanking() {
        List<Object[]> results = borrowRecordRepository.findEquipmentRanking();
        List<Map<String, Object>> rankingData = new ArrayList<>();
        
        for (Object[] result : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", result[0]);
            item.put("name", result[1]);
            item.put("borrowCount", result[2]);
            rankingData.add(item);
        }
        
        return rankingData;
    }
    
    @Override
    public List<Map<String, Object>> getUserRanking() {
        List<Object[]> results = borrowRecordRepository.findUserRanking();
        List<Map<String, Object>> rankingData = new ArrayList<>();
        
        for (Object[] result : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", result[0]);
            item.put("name", result[1]);
            item.put("borrowCount", result[2]);
            rankingData.add(item);
        }
        
        return rankingData;
    }
}