package dev.usr.database.service;

import dev.usr.database.entity.BorrowRecord;
import dev.usr.database.entity.BorrowStatus;
import dev.usr.database.payload.request.BorrowRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface BorrowService {
    List<BorrowRecord> findAll();
    
    BorrowRecord findById(Long id);
    
    List<BorrowRecord> findByUserId(Long userId);
    
    List<BorrowRecord> findActiveByUserId(Long userId);
    
    List<BorrowRecord> findActiveByEquipmentId(Long equipmentId);
    
    List<BorrowRecord> findByStatus(BorrowStatus status);
    
    List<BorrowRecord> findOverdue();
    
    List<BorrowRecord> findExpiring(int days);
    
    BorrowRecord createBorrowRequest(BorrowRequest borrowRequest, Long userId);
    
    BorrowRecord approve(Long id, Long approverId);
    
    BorrowRecord reject(Long id, Long approverId);
    
    BorrowRecord returnEquipment(Long id);
    
    BorrowRecord save(BorrowRecord borrowRecord);
    
    void delete(Long id);
    
    int countByStatus(BorrowStatus status);
    
    int countAll();
    
    int countOverdue();
    
    int countCreatedOnDate(LocalDate date);
    
    int countReturnedOnDate(LocalDate date);
    
    List<Map<String, Object>> getEquipmentRanking();
    
    List<Map<String, Object>> getUserRanking();
} 