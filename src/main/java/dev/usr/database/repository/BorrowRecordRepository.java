package dev.usr.database.repository;

import dev.usr.database.entity.BorrowRecord;
import dev.usr.database.entity.BorrowStatus;
import dev.usr.database.entity.Equipment;
import dev.usr.database.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {
    List<BorrowRecord> findByBorrower(User borrower);
    
    List<BorrowRecord> findByApprover(User approver);
    
    List<BorrowRecord> findByEquipment(Equipment equipment);
    
    List<BorrowRecord> findByStatus(BorrowStatus status);
    
    @Query("SELECT br FROM BorrowRecord br WHERE br.status = 'BORROWED' AND br.expectedReturnDate < ?1")
    List<BorrowRecord> findOverdueRecords(LocalDateTime now);
    
    @Query("SELECT br FROM BorrowRecord br WHERE br.status = 'BORROWED' AND br.expectedReturnDate >= ?1 AND br.expectedReturnDate <= ?2")
    List<BorrowRecord> findExpiringRecords(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT br FROM BorrowRecord br WHERE br.borrower = ?1 AND (br.status = 'BORROWED' OR br.status = 'OVERDUE')")
    List<BorrowRecord> findActiveBorrowsByUser(User user);
    
    @Query("SELECT br FROM BorrowRecord br WHERE br.equipment.id = ?1 AND (br.status = 'BORROWED' OR br.status = 'APPROVED' OR br.status = 'OVERDUE')")
    List<BorrowRecord> findActiveBorrowsByEquipmentId(Long equipmentId);
    
    @Query("SELECT br FROM BorrowRecord br WHERE br.status = 'PENDING'")
    List<BorrowRecord> findPendingRequests();
    
    // 统计相关查询
    @Query("SELECT COUNT(br) FROM BorrowRecord br WHERE br.status = ?1")
    int countByStatus(BorrowStatus status);
    
    @Query("SELECT COUNT(br) FROM BorrowRecord br WHERE br.status = 'BORROWED' AND br.expectedReturnDate < CURRENT_TIMESTAMP")
    int countOverdueRecords();
    
    @Query("SELECT COUNT(br) FROM BorrowRecord br WHERE FUNCTION('DATE', br.borrowDate) = ?1")
    int countCreatedOnDate(LocalDate date);
    
    @Query("SELECT COUNT(br) FROM BorrowRecord br WHERE br.actualReturnDate IS NOT NULL AND FUNCTION('DATE', br.actualReturnDate) = ?1")
    int countReturnedOnDate(LocalDate date);
    
    @Query("SELECT e.id as id, e.name as name, COUNT(br) as borrowCount " +
           "FROM BorrowRecord br JOIN br.equipment e " +
           "GROUP BY e.id, e.name ORDER BY COUNT(br) DESC")
    List<Object[]> findEquipmentRanking();
    
    @Query("SELECT u.id as id, u.name as name, COUNT(br) as borrowCount " +
           "FROM BorrowRecord br JOIN br.borrower u " +
           "GROUP BY u.id, u.name ORDER BY COUNT(br) DESC")
    List<Object[]> findUserRanking();
} 