package dev.usr.database.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 装备统计响应数据模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentStatisticsResponse {
    // 装备总数量
    private long totalCount = 0;
    
    // 可用装备数量
    private long availableCount = 0;
    
    // 维修中装备数量
    private long repairingCount = 0;
    
    // 报废装备数量
    private long scrapCount = 0;
    
    // 分类分布
    private List<Map<String, Object>> categoryDistribution = new ArrayList<>();
    
    // 状态分布
    private List<Map<String, Object>> statusDistribution = new ArrayList<>();
} 