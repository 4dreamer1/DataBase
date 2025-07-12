package dev.usr.database.service.impl;

import dev.usr.database.config.AiConfig;
import dev.usr.database.payload.ai.ChatMessage;
import dev.usr.database.payload.ai.ChatRequest;
import dev.usr.database.service.AiChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {
    
    @Autowired
    private AiConfig aiConfig;
    
    @Autowired
    private RestTemplate restTemplate;
    
    private final List<ChatMessage> conversationHistory = Collections.synchronizedList(new ArrayList<>());
    
    public AiChatServiceImpl(AiConfig aiConfig) {
        // 初始化系统消息
        if (aiConfig.getSystemMessage() != null && !aiConfig.getSystemMessage().isEmpty()) {
            conversationHistory.add(new ChatMessage("system", aiConfig.getSystemMessage()));
        }
    }
    
    @Override
    public String processMessage(String message) {
        try {
            log.info("开始处理用户消息");
            
            // 添加用户消息到历史记录
            conversationHistory.add(new ChatMessage("user", message));
            
            // 准备请求
            ChatRequest request = new ChatRequest();
            request.setModel(aiConfig.getModel());
            request.setMessages(new ArrayList<>(conversationHistory));
            request.setTemperature(aiConfig.getTemperature());
            request.setMax_tokens(aiConfig.getMaxTokens());
            request.setStream(false);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + aiConfig.getApiKey());
            
            // 创建请求实体
            HttpEntity<ChatRequest> entity = new HttpEntity<>(request, headers);
            
            log.debug("发送请求到AI服务: {}", aiConfig.getApiUrl());
            
            // 发送请求
            ResponseEntity<Map> response = restTemplate.exchange(
                aiConfig.getApiUrl(),
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            if (response.getBody() != null && response.getBody().containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, String> message_obj = (Map<String, String>) choice.get("message");
                    String aiResponse = message_obj.get("content");
                    
                    // 添加AI响应到历史记录
                    conversationHistory.add(new ChatMessage("assistant", aiResponse));
                    
                    // 如果历史记录太长，删除最早的非系统消息
                    while (conversationHistory.size() > 10) {
                        int indexToRemove = 1; // 跳过系统消息
                        if (conversationHistory.get(indexToRemove) != null) {
                            conversationHistory.remove(indexToRemove);
                        }
                    }
                    
                    log.info("成功获取AI响应");
                    return aiResponse;
                }
            }
            
            throw new RuntimeException("无法获取AI响应");
            
        } catch (Exception e) {
            log.error("调用AI API时发生错误: {}", e.getMessage(), e);
            String errorMessage = e.getMessage();
            if (errorMessage.contains("402") || errorMessage.contains("Insufficient Balance")) {
                return "抱歉，AI服务暂时无法使用，请联系管理员检查API配额。";
            } else if (errorMessage.contains("401") || errorMessage.contains("Unauthorized")) {
                return "抱歉，AI服务认证失败，请联系管理员检查API密钥配置。";
            } else if (errorMessage.contains("429") || errorMessage.contains("Too Many Requests")) {
                return "抱歉，AI服务请求过于频繁，请稍后再试。";
            } else if (errorMessage.contains("500") || errorMessage.contains("503")) {
                return "抱歉，AI服务暂时不可用，请稍后再试。";
            }
            return "抱歉，我现在无法回答您的问题。请稍后再试或联系管理员。";
        }
    }
}