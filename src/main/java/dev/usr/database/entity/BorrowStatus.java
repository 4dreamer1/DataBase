package dev.usr.database.entity;

public enum BorrowStatus {
    PENDING,    // 待审批
    APPROVED,   // 已批准
    REJECTED,   // 已拒绝
    BORROWED,   // 已借出
    RETURNED,   // 已归还
    OVERDUE     // 已逾期
} 