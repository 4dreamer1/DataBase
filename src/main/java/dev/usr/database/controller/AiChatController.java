package dev.usr.database.controller;

import dev.usr.database.payload.request.ChatRequest;
import dev.usr.database.payload.response.MessageResponse;
import dev.usr.database.security.services.UserDetailsImpl;
import dev.usr.database.service.AiChatService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/chat")
public class AiChatController {
    
    @Autowired
    private AiChatService aiChatService;

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ChatRequest chatRequest) {
        
        log.info("用户 {} 发送消息", userDetails.getUsername());
        
        try {
            String response = aiChatService.processMessage(chatRequest.getMessage());
            return ResponseEntity.ok(new MessageResponse(response));
        } catch (Exception e) {
            log.error("处理消息时发生错误: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("处理消息失败，请稍后重试"));
        }
    }
}